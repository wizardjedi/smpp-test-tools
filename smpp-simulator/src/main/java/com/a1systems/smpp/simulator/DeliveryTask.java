package com.a1systems.smpp.simulator;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.commons.gsm.GsmUtil;
import com.cloudhopper.commons.util.ByteArrayUtil;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.util.DeliveryReceipt;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeliveryTask implements Runnable {

    protected static final Logger logger = LoggerFactory.getLogger(DeliveryTask.class);

    protected SmppSession session;

    protected SubmitSm req;

    protected long id;

    public DeliveryTask(SmppSession session, SubmitSm req, long id) {
        this.session = session;
        this.req = req;
        this.id = id;
    }


    @Override
    public void run() {
        try {
            DeliverSm dsm = new DeliverSm();

            DeliveryReceipt receipt = new DeliveryReceipt();

            receipt.setMessageId(String.valueOf(id));

            dsm.setSourceAddress(req.getDestAddress());
            dsm.setDestAddress(req.getSourceAddress());
            receipt.setSubmitDate(DateTime.now().minusSeconds(10));
            receipt.setDoneDate(DateTime.now());

            dsm.setEsmClass(SmppConstants.ESM_CLASS_MT_SMSC_DELIVERY_RECEIPT);
            byte receiptState = (byte)((DateTime.now().getSecondOfMinute() % 7)+1);

            receipt.setState(receiptState);

            receipt.setSubmitCount(1);
            receipt.setDeliveredCount(0);
            receipt.setErrorCode(7);
            
            String str = receipt.toShortMessage();

            byte[] msgBuffer = CharsetUtil.encode(str, CharsetUtil.CHARSET_GSM8);

            dsm.setShortMessage(msgBuffer);

//            dsm.addOptionalParameter(new Tlv(SmppConstants.TAG_RECEIPTED_MSG_ID, ByteArrayUtil.toByteArray((int)id)));
//            dsm.addOptionalParameter(new Tlv(SmppConstants.TAG_MSG_STATE, ByteArrayUtil.toByteArray(receiptState)));

            session.sendRequestPdu(dsm, TimeUnit.SECONDS.toMillis(60), false);
        } catch (Exception ex) {
            logger.error("{}", ex);
        }

    }

}
