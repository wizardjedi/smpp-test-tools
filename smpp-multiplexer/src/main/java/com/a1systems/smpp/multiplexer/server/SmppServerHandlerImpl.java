package com.a1systems.smpp.multiplexer.server;

import com.a1systems.smpp.multiplexer.client.Client;
import com.a1systems.smpp.multiplexer.client.ClientSessionHandler;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.type.SmppProcessingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmppServerHandlerImpl implements SmppServerHandler {

    public static final Logger logger = LoggerFactory.getLogger(SmppServerHandlerImpl.class);

    protected SmppServerSession session;

    protected ExecutorService pool;
    
    protected List<Client> clients;
    
    public SmppServerHandlerImpl(ExecutorService pool) {
        this.pool = pool;
    }

    public List<Client> getClients() {
        return clients;
    }

    public void setClients(List<Client> clients) {
        this.clients = clients;
    }
    
    @Override
    public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration, BaseBind bindRequest) throws SmppProcessingException {
        String systemId = sessionConfiguration.getSystemId();
        String password = sessionConfiguration.getPassword();

        logger.debug("Bind request with {}:{}", systemId, password);
        
        if (!"test1".equals(systemId) || !"test2".equals(password)) {
            logger.error("Bind request failed {} {}", systemId, password);
            
            throw new SmppProcessingException(SmppConstants.STATUS_INVPASWD, "CHipotl");
        }
        
        clients = new ArrayList<>();
        
        SmppSessionConfiguration cfg = new SmppSessionConfiguration();
        cfg.setHost("127.0.0.1");
        cfg.setPort(2775);
        cfg.setSystemId("smppclient1");
        cfg.setPassword("password");
        
        Client c = new Client(cfg);
        c.setSessionHandler(new ClientSessionHandler(c));
        c.setPool(pool);
        c.start();
            
        clients.add(c);
        
        SmppSessionConfiguration cfg2 = new SmppSessionConfiguration();
        cfg2.setHost("127.0.0.1");
        cfg2.setPort(2775);
        cfg2.setSystemId("smppclient1");
        cfg2.setPassword("password");
        
        Client c2 = new Client(cfg2);
        c2.setSessionHandler(new ClientSessionHandler(c2));
        c2.setPool(pool);
        c2.start();
            
        clients.add(c2);
    }

    @Override
    public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse) throws SmppProcessingException {
        this.session = session;
        
        for (int i=0;i<clients.size();i++) {
            clients.get(i).setServerSession(session);
        }
        
        session.serverReady(new SmppSessionHandler(session, pool, this));
    }

    @Override
    public void sessionDestroyed(Long sessionId, SmppServerSession session) {
        logger.debug("Session destroy");
        
        for (int i=0;i<clients.size();i++) {
            clients.get(i).stop();
        }
        
        session.destroy();
    }

}
