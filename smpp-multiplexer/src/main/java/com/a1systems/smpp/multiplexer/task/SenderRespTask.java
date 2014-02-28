package com.a1systems.smpp.multiplexer.task;

import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SenderRespTask implements Runnable {
    public static final Logger logger = LoggerFactory.getLogger(SenderRespTask.class);
    
    protected Pdu pdu;
    protected WeakReference<SmppServerSession> sessionRef;
    
    
    
    public SenderRespTask(Pdu ssmr, SmppServerSession session) {
        this.pdu = ssmr;
        this.sessionRef = new WeakReference<>(session);
    }

    @Override
    public void run() {
        if (sessionRef.get() != null) {
            SmppServerSession session = sessionRef.get();
            
            pdu.removeSequenceNumber();
            
            try {
                if (pdu.isResponse()) {
                    session.sendResponsePdu((PduResponse)pdu);
                } else {
                    session.sendRequestPdu((PduRequest)pdu, TimeUnit.SECONDS.toMillis(60), false);
                }
            } catch (RecoverablePduException ex) {
                java.util.logging.Logger.getLogger(SenderRespTask.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnrecoverablePduException ex) {
                java.util.logging.Logger.getLogger(SenderRespTask.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SmppChannelException ex) {
                java.util.logging.Logger.getLogger(SenderRespTask.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(SenderRespTask.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SmppTimeoutException ex) {
                java.util.logging.Logger.getLogger(SenderRespTask.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
}
