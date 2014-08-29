package com.a1systems.smpp.multiplexer.server;

import com.a1systems.smpp.multiplexer.Application;
import com.cloudhopper.smpp.SmppClient;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.type.LoggingOptions;
import com.cloudhopper.smpp.type.SmppProcessingException;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import io.netty.channel.nio.NioEventLoopGroup;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmppServerHandlerImpl implements SmppServerHandler {

    public static final Logger logger = LoggerFactory.getLogger(SmppServerHandlerImpl.class);

    protected SmppClient smppClient;

    protected ExecutorService pool;

    protected Application app;

    protected ScheduledExecutorService asyncPool;

    protected List<Application.ConnectionEndpoint> endPoints;

    protected ConcurrentHashMap<Long, SmppServerSessionHandler> handlers = new ConcurrentHashMap<Long, SmppServerSessionHandler>();
    protected MetricRegistry metricsRegistry;

    protected ConcurrentHashMap<String,DateTime> failedLogins = new ConcurrentHashMap<String, DateTime>();

    public SmppServerHandlerImpl(NioEventLoopGroup group, ExecutorService pool, List<Application.ConnectionEndpoint> endPoints, Application app) {
        this.pool = pool;

        this.app = app;

        asyncPool = Executors.newScheduledThreadPool(5);

        metricsRegistry = new MetricRegistry();

        final JmxReporter reporter = JmxReporter.forRegistry(metricsRegistry).build();
        reporter.start();

        this.smppClient = new DefaultSmppClient(group, asyncPool);

        this.endPoints = endPoints;
    }

    @Override
    public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration, BaseBind bindRequest) throws SmppProcessingException {
        String systemId = sessionConfiguration.getSystemId();
        String password = sessionConfiguration.getPassword();

        String sessionName = systemId + "_" + password + "_" + sessionId;
        
        sessionConfiguration.setName(sessionName);
        
        logger.debug("{} Bind request with {}:{}", sessionName, systemId, password);

        try {
            SmppServerSessionHandler smppServerSessionHandler = new SmppServerSessionHandler(systemId, password, sessionConfiguration, pool, this);

            handlers.put(sessionId, smppServerSessionHandler);
        } catch (MultiplexerBindException e) {
            logger.error("{} Multiplexer bind exception", sessionName);
            
            throw new SmppProcessingException(SmppConstants.STATUS_INVSYSID);
        } catch (Exception e) {
            logger.error("{} Exception while bind request {}", sessionName, e);
            
            throw new SmppProcessingException(SmppConstants.STATUS_INVSYSID);
        }
    }

    @Override
    public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse) throws SmppProcessingException {
        SmppSessionConfiguration sessionConfiguration = session.getConfiguration();

        String systemId = sessionConfiguration.getSystemId();
        String password = sessionConfiguration.getPassword();

        LoggingOptions lo = new LoggingOptions();
        lo.setLogBytes(false);
        lo.setLogPdu(false);

        session.getConfiguration().setLoggingOptions(lo);

        session.getConfiguration().setWriteTimeout(TimeUnit.MILLISECONDS.toMillis(200));

        SmppServerSessionHandler smppServerSessionHandler = handlers.get(sessionId);

        smppServerSessionHandler.useServerSession(session);
        
        String sessionName = systemId + "_" + password + "_" + sessionId;

        DefaultSmppSession defaultSmppSession = (DefaultSmppSession)session;
        
        InetSocketAddress inetRemoteAddr = (InetSocketAddress)defaultSmppSession.getChannel().remoteAddress();
        
        String adr = inetRemoteAddr.getHostString()+":"+inetRemoteAddr.getPort();
        
        logger.info("Created session sess.id:{} and sess.name:{} for {}", sessionId, sessionName, adr);

        session.getConfiguration().setName(sessionName);

        session.serverReady(smppServerSessionHandler);

        smppServerSessionHandler.startElinkTask(smppServerSessionHandler);
        
        smppServerSessionHandler.processQueuedRequests();        
    }

    @Override
    public void sessionDestroyed(Long sessionId, SmppServerSession session) {
        DefaultSmppSession defaultSmppSession = (DefaultSmppSession)session;
        
        InetSocketAddress inetRemoteAddr = (InetSocketAddress)defaultSmppSession.getChannel().remoteAddress();
        
        String adr = inetRemoteAddr.getHostString()+":"+inetRemoteAddr.getPort();
        
        logger.debug("Session sess.id:{} sess.name:{} destroy for {}", sessionId, session.getConfiguration().getName(), adr);

        SmppServerSessionHandler handler = handlers.get(sessionId);

        handler.fireChannelUnexpectedlyClosed();

        handlers.remove(sessionId);

        session.close();
        session.destroy();
    }

    public SmppClient getSmppClient() {
        return this.smppClient;
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

    public MetricRegistry getMetricsRegistry() {
        return metricsRegistry;
    }

    public void setMetricsRegistry(MetricRegistry metricsRegistry) {
        this.metricsRegistry = metricsRegistry;
    }

    public Application getApp() {
        return app;
    }

    public void setApp(Application app) {
        this.app = app;
    }
}
