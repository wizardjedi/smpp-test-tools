package com.a1systems.smpp.multiplexer.server;

import com.a1systems.smpp.multiplexer.client.RouteInfo;
import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmppServerSessionHandler extends DefaultSmppSessionHandler {

    public static final Logger logger = LoggerFactory.getLogger(SmppServerSessionHandler.class);

    private WeakReference<SmppSession> sessionRef;

    protected ExecutorService pool;

    protected SmppServerHandlerImpl handler;

    public SmppServerSessionHandler(SmppSession session, ExecutorService pool, SmppServerHandlerImpl handler) {
        this.sessionRef = new WeakReference<SmppSession>(session);

        this.handler = handler;

        this.pool = pool;
    }

    @Override
    public PduResponse firePduRequestReceived(PduRequest pduRequest) {

        if (pduRequest instanceof EnquireLink) {
            return pduRequest.createResponse();
        }

        if (pduRequest instanceof SubmitSm) {
            handler.processSubmitSm((SubmitSm)pduRequest);
        }

        return null;
    }

    @Override
    public void fireExpectedPduResponseReceived(PduAsyncResponse pdu) {
        PduResponse pduResponse = pdu.getResponse();

        if (pduResponse instanceof DeliverSmResp) {
            RouteInfo ri = (RouteInfo)pdu.getRequest().getReferenceObject();

            pduResponse.setReferenceObject(ri);

            handler.processDeliverSmResp((DeliverSmResp)pduResponse);
        }
    }

}
