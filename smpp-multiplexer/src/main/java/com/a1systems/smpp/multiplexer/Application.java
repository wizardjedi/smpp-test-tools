package com.a1systems.smpp.multiplexer;

import com.a1systems.smpp.multiplexer.server.SmppServerHandlerImpl;
import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.type.SmppChannelException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    public static final Logger logger = LoggerFactory.getLogger(Application.class);
    
    protected ExecutorService pool;

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
        
        DefaultSmppServer server = new DefaultSmppServer(serverConfig, new SmppServerHandlerImpl(pool));

        logger.info("Smpp server starting");
        
        server.start();
        
        logger.info("Smpp server started");
    }
}
