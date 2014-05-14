package com.a1systems.smpp.simulator;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.util.DeliveryReceipt;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joda.time.DateTime;

public class Simulator {

    protected ScheduledExecutorService scheduledExecutor;

    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    public void setScheduledExecutor(ScheduledExecutorService scheduledExecutor) {
        this.scheduledExecutor = scheduledExecutor;
    }

    public DeliverSm createDeliveryReceipt(SubmitSm sm) {
        DeliverSm dsm = new DeliverSm();

        dsm.setEsmClass(SmppConstants.ESM_CLASS_MT_SMSC_DELIVERY_RECEIPT);
        dsm.setDestAddress(sm.getSourceAddress());
        dsm.setSourceAddress(sm.getDestAddress());

        return dsm;
    }

    public DeliverSm setUpDeliveryReceipt(DeliverSm dsm, String messageId, String status, String sendDate, String deliveryDate, int errorCode) {
        return setUpDeliveryReceipt(dsm, messageId, status, DateTime.parse(sendDate), DateTime.parse(deliveryDate), errorCode);
    }
    
    public DeliverSm setUpDeliveryReceipt(DeliverSm dsm, String messageId, String status, DateTime sendDate, DateTime deliveryDate, int errorCode) {
        DeliveryReceipt dr = new DeliveryReceipt();

        dr.setMessageId(messageId);
        dr.setState(DeliveryReceipt.toState(status));
        dr.setDeliveredCount(1);
        dr.setSubmitCount(1);
        dr.setErrorCode(errorCode);
        dr.setSubmitDate(sendDate);
        dr.setDoneDate(deliveryDate);

        try {
            dsm.setShortMessage(dr.toShortMessage().getBytes());
        } catch (SmppInvalidArgumentException ex) {
            /* */
        }

        return dsm;
    }

    public void scheduleDeliverySm(final DeliverSm deliverSm, final SmppSession session, final long delayMillis) {
        scheduledExecutor
            .schedule(
                new Runnable() {
                    public void run() {
                        try {
                            session.sendRequestPdu(deliverSm, TimeUnit.SECONDS.toMillis(60), false);
                        } catch (RecoverablePduException ex) {
                            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (UnrecoverablePduException ex) {
                            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (SmppTimeoutException ex) {
                            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (SmppChannelException ex) {
                            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
            },
            delayMillis,
            TimeUnit.MILLISECONDS
        );
    }
}
