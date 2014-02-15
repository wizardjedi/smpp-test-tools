package com.a1systems.smpp.simulator;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.commons.gsm.GsmUtil;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.SubmitSm;
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

            receipt.setMessageId(id);

            dsm.setSourceAddress(req.getDestAddress());
            dsm.setDestAddress(req.getSourceAddress());
            receipt.setSubmitDate(DateTime.now().minusSeconds(10));
            receipt.setDoneDate(DateTime.now());

            Random r = new Random(DateTime.now().getMillis());

            receipt.setState((byte)(r.nextInt(7)+1));

            String str = receipt.toShortMessage();

            byte[] msgBuffer = CharsetUtil.encode(str, CharsetUtil.CHARSET_GSM8);

            dsm.setShortMessage(msgBuffer);

            session.sendRequestPdu(dsm, TimeUnit.SECONDS.toMillis(60), false);
        } catch (Exception ex) {
            logger.error("{}", ex);
        }

    }

}
