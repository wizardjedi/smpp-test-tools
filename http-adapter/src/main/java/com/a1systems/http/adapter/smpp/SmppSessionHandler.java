package com.a1systems.http.adapter.smpp;

import com.a1systems.http.adapter.Application;
import com.a1systems.http.adapter.message.MessagePart;
import com.a1systems.http.adapter.message.PartState;
import com.a1systems.http.adapter.smpp.client.Client;
import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.tlv.TlvConvertException;
import com.cloudhopper.smpp.util.DeliveryReceipt;
import com.cloudhopper.smpp.util.DeliveryReceiptException;
import java.util.logging.Level;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class SmppSessionHandler extends DefaultSmppSessionHandler {

    public static Logger log = LoggerFactory.getLogger(SmppSessionHandler.class);

    protected Client client;

    @Autowired
    protected Application application;

    public SmppSessionHandler(Client client) {
        this.client = client;
    }

    @Override
    public PduResponse firePduRequestReceived(PduRequest pduRequest) {
        if (pduRequest.isRequest()
                && pduRequest.getClass() == DeliverSm.class) {
            log.debug("Got DELIVER_SM");

            DeliverSm dlr = (DeliverSm) pduRequest;

            Tlv msgIdTlv = dlr.getOptionalParameter(SmppConstants.TAG_RECEIPTED_MSG_ID);
            Tlv stateTlv = dlr.getOptionalParameter(SmppConstants.TAG_MSG_STATE);

            log.error("{} {}", msgIdTlv, stateTlv);
            
            String deliveryReceiptString = CharsetUtil.decode(dlr.getShortMessage(), CharsetUtil.CHARSET_GSM8);
            
            log.error("{}", deliveryReceiptString);
            
            DeliveryReceipt deliveryReceipt = new DeliveryReceipt();
            try {
                deliveryReceipt = DeliveryReceipt.parseShortMessage(deliveryReceiptString, DateTimeZone.forOffsetHours(4), true);
            } catch (DeliveryReceiptException ex) {
                log.error("{}", ex);
            }

            String smscMsgId;

            log.error("{}", deliveryReceipt);
            
                //smscMsgId = msgIdTlv.getValueAsString();
            MessagePart part = application.getLinkIds().get(deliveryReceipt.getMessageId());

            log.error("{} {}", deliveryReceipt.getMessageId(), part);
            
            if (part != null) {
                part.setError(deliveryReceipt.getErrorCode());
                part.setState(PartState.valueOf(deliveryReceipt.getState()));
            }

            return pduRequest.createResponse();
        }

        return super.firePduRequestReceived(pduRequest);
    }

    @Override
    public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
        if (pduAsyncResponse.getResponse().getClass() == SubmitSmResp.class) {
            SubmitSm req = (SubmitSm) pduAsyncResponse.getRequest();

            MessagePart part = (MessagePart) req.getReferenceObject();

            log.debug("Got response for part={}", part.getId());

            SubmitSmResp ssmr = (SubmitSmResp) pduAsyncResponse.getResponse();

            log.error("{}", ssmr.getCommandStatus());

            if (ssmr.getCommandStatus() == 0) {
                application.setSmscId(ssmr.getMessageId(), part);
            }
        }
    }

    @Override
    public void fireChannelUnexpectedlyClosed() {
        client.bind();
    }

    @Override
    public void firePduRequestExpired(PduRequest pduRequest) {
        if (pduRequest instanceof EnquireLink) {
            super.fireChannelUnexpectedlyClosed();
            return;
        }

        super.firePduRequestExpired(pduRequest);
    }

}
