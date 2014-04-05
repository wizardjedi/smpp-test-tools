package com.a1systems.smpp.multiplexer;

import com.a1systems.smpp.multiplexer.server.SmppServerHandlerImpl;
import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.type.SmppChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    public static final Logger logger = LoggerFactory.getLogger(Application.class);

    protected ExecutorService pool;

    public static class ConnectionEndpoint {
        protected String host;
        protected int port;

        public static ConnectionEndpoint create(String host, int port) {
            ConnectionEndpoint c = new ConnectionEndpoint();

            c.setHost(host);
            c.setPort(port);

            return c;
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
            return "ConnectionEndpoint{" + "host=" + host + ", port=" + port + '}';
        }
    }

    public ExecutorService getPool() {
        return pool;
    }

    public void setPool(ExecutorService pool) {
        this.pool = pool;
    }

    public void run(CliConfig config) throws SmppChannelException {
        logger.info("Application starting");

        pool = Executors.newCachedThreadPool();

        SmppServerConfiguration serverConfig = new SmppServerConfiguration();
        serverConfig.setPort(config.getPort());
        serverConfig.setNonBlockingSocketsEnabled(true);

        List<ConnectionEndpoint> endPoints = new ArrayList<ConnectionEndpoint>();

        String[] configEndPoints = config.getEndPoints().split(",");

        for (int i=0;i<configEndPoints.length;i++) {
            String endPoint = configEndPoints[i];

            String[] parts = endPoint.split(":");

            ConnectionEndpoint c = ConnectionEndpoint.create(parts[0], Integer.parseInt(parts[1]));

            logger.info("Use end point:{}", c);

            endPoints.add(c);
        }

        DefaultSmppServer server = new DefaultSmppServer(serverConfig, new SmppServerHandlerImpl(pool, endPoints));

        logger.info("Smpp server starting");

        server.start();

        logger.info("Smpp server started");
    }
}
