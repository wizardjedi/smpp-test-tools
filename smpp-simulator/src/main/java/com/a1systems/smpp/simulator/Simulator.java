package com.a1systems.smpp.simulator;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.commons.gsm.GsmUtil;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.util.DeliveryReceipt;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Simulator {

    public static final Logger logger = LoggerFactory.getLogger(Simulator.class);

    protected static Random randomizer;

    static {
        randomizer = new Random();
    }

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
                    @Override
                    public void run() {
                        try {
                            session.sendRequestPdu(deliverSm, TimeUnit.SECONDS.toMillis(60), false);
                        } catch (RecoverablePduException ex) {
                            logger.error("{}", ex);
                        } catch (UnrecoverablePduException ex) {
                            logger.error("{}", ex);
                        } catch (SmppTimeoutException ex) {
                            logger.error("{}", ex);
                        } catch (SmppChannelException ex) {
                            logger.error("{}", ex);
                        } catch (InterruptedException ex) {
                            logger.error("{}", ex);
                        }
                    }
            },
            delayMillis,
            TimeUnit.MILLISECONDS
        );
    }

    public byte[] encode(String string, String encoding) {
        return CharsetUtil.encode(string, encoding);
    }

    public String decode(byte[] sequence, String encoding) {
        return CharsetUtil.decode(sequence, encoding);
    }

    public SubmitSm[] createSubmitSmForConcatinatedMessage(Address source, Address destination, String message, String encoding) throws SmppInvalidArgumentException {
        byte[] encodedString = CharsetUtil.encode(message, encoding);

        if (encodedString.length > 140) {
            int nextInt = randomizer.nextInt();
            byte[][] parts = GsmUtil.createConcatenatedBinaryShortMessages(encodedString, (byte)nextInt);

            SubmitSm[] list = new SubmitSm[parts.length];

            for (int i=0;i<parts.length;i++) {
                SubmitSm sm = new SubmitSm();
                sm.setSourceAddress(source);
                sm.setDestAddress(destination);
                sm.setShortMessage(parts[i]);

                sm.setEsmClass(SmppConstants.ESM_CLASS_UDHI_MASK);

                list[i] = sm;
            }

            return list;
        } else {
            SubmitSm sm = new SubmitSm();
            sm.setSourceAddress(source);
            sm.setDestAddress(destination);
            sm.setShortMessage(encodedString);

            return new SubmitSm[] {sm};
        }
    }
}
