package com.a1systems.smpp.multiplexer.server;

import static com.a1systems.smpp.multiplexer.client.ElinkTask.log;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.EnquireLinkResp;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerElinkTask implements Runnable {
    public static final Logger logger = LoggerFactory.getLogger(ServerElinkTask.class);
    
    protected volatile SmppServerSessionHandler smppServerSessionHandler;
    
    protected String sessionName;
    
    public ServerElinkTask(SmppServerSessionHandler smppServerSessionHandler) {
        this.smppServerSessionHandler = smppServerSessionHandler;
        
        sessionName = smppServerSessionHandler.getSession().getConfiguration().getName();
    }

    @Override
    public void run() {
        
        SmppServerSession session = (SmppServerSession) smppServerSessionHandler.getSession();
        
        try {
            if (
                session!=null 
                && session.isBound()
            ) {
                if (
                    System.currentTimeMillis() - smppServerSessionHandler.getLastInputMillis() > TimeUnit.SECONDS.toMillis(smppServerSessionHandler.getElinkPeriod())
                ) {
                    session.sendRequestPdu(new EnquireLink(), TimeUnit.SECONDS.toMillis(10), false);
                }
            } else {
                log.error("No session for {}", sessionName);
            }
        } catch (RecoverablePduException ex) {
            log.debug("{}", ex);
        } catch (UnrecoverablePduException ex) {
            log.debug("{}", ex);
        } catch (SmppTimeoutException ex) {
            log.debug("{}", ex);
        } catch (SmppChannelException ex) {
            log.debug("{}", ex);
        } catch (InterruptedException ex) {
            log.debug("{}", ex);
        }
    }
}
