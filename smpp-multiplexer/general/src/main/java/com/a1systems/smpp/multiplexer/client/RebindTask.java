package com.a1systems.smpp.multiplexer.client;

import com.a1systems.smpp.multiplexer.Application;
import com.cloudhopper.smpp.SmppClient;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.type.SmppBindException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RebindTask implements Runnable {

    public static final Logger log = LoggerFactory.getLogger(RebindTask.class);

    protected Client client;

    public RebindTask(Client client) {
        this.client = client;
    }

    @Override
    public void run() {
        if (client.isBinding()) {
            
            client.onBindEvent();
            
            Application.ConnectionEndpoint endpoint = client.getEndpoint();
            
            if (
                endpoint != null
                && endpoint.getLastFailedConnection() != null
                && endpoint.getLastFailedConnection().plusSeconds(60).isAfterNow()
            ) {
                log.debug("{} Endpoint failed since {}", client.getName(), endpoint.getLastFailedConnection());
                
                return ;
            }
            
            SmppClient smppClient = client.getSmppClient();

            SmppSession session = null;

            try {
                SmppSessionConfiguration cfg = client.getCfg();

                log.debug("{} Try to bind host:[{}:{}] Credentials:[{}]:[{}]", client.getName(), cfg.getHost(), cfg.getPort(), cfg.getSystemId(), cfg.getPassword());

                session = smppClient.bind(client.getCfg(), client.getSessionHandler());

                client.bound(session);
            } catch (SmppTimeoutException ex) {
                log.error(String.format("SmppTimeoutException %s %s", client.getName(), ex.getMessage()), ex);
                             
                closeSession(session);
            } catch (SmppChannelException ex) {
                log.error(String.format("SmppChannelException %s %s", client.getName(), ex.getMessage()), ex);
                
                Throwable cause = ex.getCause();
                
                if (cause != null) {
                    SmppSessionHandler sessionHandler = client.getSessionHandler();
                
                    sessionHandler.fireUnknownThrowable(cause);
                }
                
                closeSession(session);
            } catch (SmppBindException ex) {
                log.error(String.format("SmppBindException %s %s", client.getName(), ex.getMessage()),ex);
                
                closeSession(session);
            } catch (UnrecoverablePduException ex) {
                log.error(String.format("UnrecoverablePduException %s %s", client.getName(), ex.getMessage()), ex);
                
                closeSession(session);
            } catch (InterruptedException ex) {
                log.error(String.format("InterruptedException %s %s", client.getName(), ex.getMessage()), ex);
                
                closeSession(session);
            }
        }
    }

    protected void closeSession(SmppSession session) {
        if (session != null) {
            log.info("Destroy session after unsuccessfull bind for {}", client.getName());
            
            session.destroy();
        }
    }

}
