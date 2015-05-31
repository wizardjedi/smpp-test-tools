package com.a1systems.smpp.multiplexer;

import com.a1systems.plugin.Authorizer;
import com.a1systems.smpp.multiplexer.server.MetricsHelper;
import com.a1systems.smpp.multiplexer.server.PoolActiveSizeGauge;
import com.a1systems.smpp.multiplexer.server.PoolQueueSizeGauge;
import com.a1systems.smpp.multiplexer.server.SmppServerHandlerImpl;
import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.codahale.metrics.MetricRegistry;
import io.netty.channel.nio.NioEventLoopGroup;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.joda.time.DateTime;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

    public static final Logger logger = LoggerFactory.getLogger(Application.class);

    protected ExecutorService pool;
    protected ScheduledExecutorService asyncPool;
    protected ScheduledExecutorService monitorPool;
    private NioEventLoopGroup clientGroup;
    private NioEventLoopGroup nioGroup;
    private DefaultSmppServer server;
    private SmppServerHandlerImpl serverHandler;

    protected ServiceLoader<Authorizer> authorizers;

    public static class ConnectionEndpoint {

        protected String host;
        protected int port;
        protected boolean hidden = false;
        protected volatile DateTime lastFailedConnection = null;
        
        public static ConnectionEndpoint create(String host, int port, boolean hidden) {
            ConnectionEndpoint c = new ConnectionEndpoint();

            c.setHost(host);
            c.setPort(port);
            c.setHidden(hidden);

            return c;
        }

        public void markLastFailedConnection() {
            this.lastFailedConnection = DateTime.now();
        }
        
        public DateTime getLastFailedConnection() {
            return lastFailedConnection;
        }

        public void setLastFailedConnection(DateTime lastFailedConnection) {
            this.lastFailedConnection = lastFailedConnection;
        }
        
        public boolean isHidden() {
            return hidden;
        }

        public void setHidden(boolean hidden) {
            this.hidden = hidden;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        @Override
        public String toString() {
            return "ConnectionEndpoint{" + "host=" + host + ", port=" + port + ", hidden=" + hidden + ", lastFailedConnection=" + lastFailedConnection + '}';
        }
    }

    public ExecutorService getPool() {
        return pool;
    }

    public void setPool(ExecutorService pool) {
        this.pool = pool;
    }

    
    
    public ScheduledExecutorService getAsyncPool() {
        return asyncPool;
    }

    public void setAsyncPool(ScheduledExecutorService asyncPool) {
        this.asyncPool = asyncPool;
    }

    public ScheduledExecutorService getMonitorPool() {
        return monitorPool;
    }

    public void setMonitorPool(ScheduledExecutorService monitorPool) {
        this.monitorPool = monitorPool;
    }

    public SmppServerHandlerImpl getServerHandler() {
        return serverHandler;
    }

    public void setServerHandler(SmppServerHandlerImpl serverHandler) {
        this.serverHandler = serverHandler;
    }

    public DefaultSmppServer getServer() {
        return server;
    }

    public void setServer(DefaultSmppServer server) {
        this.server = server;
    }

    public NioEventLoopGroup getClientGroup() {
        return clientGroup;
    }

    public void setClientGroup(NioEventLoopGroup clientGroup) {
        this.clientGroup = clientGroup;
    }

    public NioEventLoopGroup getNioGroup() {
        return nioGroup;
    }

    public void setNioGroup(NioEventLoopGroup nioGroup) {
        this.nioGroup = nioGroup;
    }
    
    public void run(CliConfig config) throws SmppChannelException {
        String applicationVersion = "undefined";

        try {
            Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                URL elem = resources.nextElement();

                Manifest manifest = new Manifest(elem.openStream());

                Attributes attrs = manifest.getMainAttributes();

                if (
                    attrs.getValue("Implementation-Build") != null
                    && attrs.getValue("Main-Class").equals("com.a1systems.smpp.multiplexer.App")
                ) {
                    applicationVersion = attrs.getValue("Implementation-Build");
                    break;
                };
            }
        } catch (IOException e) {
            logger.error("Couldnot open manifest");
        }

        logger.info("\n\nApplication version:{} starting\n\n", applicationVersion);

        File pluginsDirectory = new File("plugins");

        List<URL> jarsUrlsList = new ArrayList<URL>();

        if (
            pluginsDirectory.exists()
            && pluginsDirectory.canExecute()
        ) {
            for (File file:pluginsDirectory.listFiles()) {
                if (
                    file.canRead()
                    && file.isFile()
                    && file.canExecute()
                    && file.getName().endsWith(".jar")
                ) {
                    try {
                        jarsUrlsList.add(file.toURI().toURL());

                        logger.info("Found plugin-jar:{}", file);
                    } catch (MalformedURLException ex) {
                        logger.error("Malformed URL for file {}", file);
                    }
                }
            }
        }

        URLClassLoader cl = new URLClassLoader(jarsUrlsList.toArray(new URL[]{}));

        authorizers = ServiceLoader.load(Authorizer.class, cl);

        logger.info("Loading auth plugins {}", authorizers);

        for (Authorizer a:authorizers) {
            logger.info("Loading plugin {}", a);

            a.load();
            a.start();
        }

        pool = Executors.newFixedThreadPool(20);

        ScheduledExecutorService reportPool = Executors.newScheduledThreadPool(1);
        asyncPool = Executors.newScheduledThreadPool(15);
        monitorPool = Executors.newScheduledThreadPool(15);
        
        SmppServerConfiguration serverConfig = new SmppServerConfiguration();
        serverConfig.setPort(config.getPort());
        serverConfig.setNonBlockingSocketsEnabled(true);
        serverConfig.setBindTimeout(TimeUnit.SECONDS.toMillis(20));
        
        List<ConnectionEndpoint> endPoints = new ArrayList<ConnectionEndpoint>();

        String[] configEndPoints = config.getEndPoints().split(",");

        for (String endPoint : configEndPoints) {
            String[] parts = endPoint.split(":");

            ConnectionEndpoint c;

            if (parts.length == 3
                    && parts[0].toLowerCase().equals("h")) {
                c = ConnectionEndpoint.create(parts[1], Integer.parseInt(parts[2]), true);
            } else {
                c = ConnectionEndpoint.create(parts[0], Integer.parseInt(parts[1]), false);
            }

            logger.info("Use end point:{}", c);

            endPoints.add(c);
        }

        serverConfig.setSystemId("SMPP-MUX");

        serverConfig.setMaxConnectionSize(300);
        // don't set big values
        serverConfig.setDefaultWindowSize(2500);
        serverConfig.setDefaultRequestExpiryTimeout(TimeUnit.SECONDS.toMillis(60));
        serverConfig.setDefaultWindowMonitorInterval(TimeUnit.SECONDS.toMillis(15));
        
        nioGroup = new NioEventLoopGroup();
        clientGroup = new NioEventLoopGroup();
        
        MetricRegistry metricsRegistry = new MetricRegistry();
        
        metricsRegistry.register(MetricsHelper.JMX_GAUGE_ASYNC_POOL_QUEUE_SIZE, new PoolQueueSizeGauge((ThreadPoolExecutor) asyncPool));
        metricsRegistry.register(MetricsHelper.JMX_GAUGE_ACTIVE_ASYNC_POOL_SIZE, new PoolActiveSizeGauge((ThreadPoolExecutor) asyncPool));
        
        metricsRegistry.register(MetricsHelper.JMX_GAUGE_MONITOR_POOL_QUEUE_SIZE, new PoolQueueSizeGauge((ThreadPoolExecutor) monitorPool));
        metricsRegistry.register(MetricsHelper.JMX_GAUGE_ACTIVE_MONITOR_POOL_SIZE, new PoolActiveSizeGauge((ThreadPoolExecutor) monitorPool));
        
        metricsRegistry.register(MetricsHelper.JMX_GAUGE_POOL_QUEUE_SIZE, new PoolQueueSizeGauge((ThreadPoolExecutor) pool));
        metricsRegistry.register(MetricsHelper.JMX_GAUGE_ACTIVE_POOL_SIZE, new PoolActiveSizeGauge((ThreadPoolExecutor) pool));
        
        serverHandler = new SmppServerHandlerImpl(clientGroup, pool, endPoints, asyncPool, monitorPool, metricsRegistry);
        serverHandler.setApp(this);

        server = new DefaultSmppServer(serverConfig, serverHandler, monitorPool, nioGroup, nioGroup);

        logger.info("Smpp server starting");

        server.start();

        logger.info("Smpp server started");
        
        reportPool.scheduleAtFixedRate(new ReportTask(this), 1, 1, TimeUnit.MINUTES);
    }

    public ServiceLoader<Authorizer> getAuthorizers() {
        return authorizers;
    }

    public void setAuthorizers(ServiceLoader<Authorizer> authorizers) {
        this.authorizers = authorizers;
    }

}
