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
    }

    public ExecutorService getPool() {
        return pool;
    }

    public void setPool(ExecutorService pool) {
        this.pool = pool;
    }

    public void run() throws SmppChannelException {
        logger.info("Application starting");

        pool = Executors.newFixedThreadPool(20);

        SmppServerConfiguration serverConfig = new SmppServerConfiguration();
        serverConfig.setPort(3712);
        serverConfig.setNonBlockingSocketsEnabled(true);

        List<ConnectionEndpoint> endPoints = new ArrayList<ConnectionEndpoint>();

        endPoints.add(ConnectionEndpoint.create("127.0.0.1", 2775));
        endPoints.add(ConnectionEndpoint.create("127.0.0.1", 2775));

        DefaultSmppServer server = new DefaultSmppServer(serverConfig, new SmppServerHandlerImpl(pool, endPoints));

        logger.info("Smpp server starting");

        server.start();

        logger.info("Smpp server started");
    }
}
