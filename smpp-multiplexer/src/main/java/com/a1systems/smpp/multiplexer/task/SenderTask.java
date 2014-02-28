package com.a1systems.smpp.multiplexer.task;

import com.a1systems.smpp.multiplexer.client.Client;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SenderTask implements Runnable {
    public static final Logger logger = LoggerFactory.getLogger(SenderTask.class);
    
    protected Pdu pdu;
    protected WeakReference<Client> clientRef;
    
    
    
    public SenderTask(Pdu req, Client client) {
        this.pdu = req;
        this.clientRef = new WeakReference<>(client);
    }

    @Override
    public void run() {
        if (clientRef.get() != null) {
            Client client = clientRef.get();
            
            pdu.removeSequenceNumber();
            
            try {
                if (pdu.isRequest()) {
                    client.getSession().sendRequestPdu((PduRequest)pdu, TimeUnit.SECONDS.toMillis(60), false);
                } else {
                    client.getSession().sendResponsePdu((PduResponse)pdu);
                }
            } catch (RecoverablePduException ex) {
                logger.debug("{}", ex);
            } catch (UnrecoverablePduException ex) {
                logger.debug("{}", ex);
            } catch (SmppTimeoutException ex) {
                logger.debug("{}", ex);
            } catch (SmppChannelException ex) {
                logger.debug("{}", ex);
            } catch (InterruptedException ex) {
                logger.debug("{}", ex);
            }
        }
    }
    
}
