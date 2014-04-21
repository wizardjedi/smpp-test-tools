package com.a1systems.smpp.simulator;

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import java.lang.ref.WeakReference;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class SessionHandler extends DefaultSmppSessionHandler {

    public ScheduledExecutorService pool;

    private WeakReference<SmppSession> sessionRef;

    protected AtomicLong counter = new AtomicLong(1);

    public SessionHandler(SmppSession session, ScheduledExecutorService pool) {
        this.sessionRef = new WeakReference<SmppSession>(session);
        this.pool = pool;
    }

    @Override
    public PduResponse firePduRequestReceived(PduRequest pduRequest) {
        SmppSession session = sessionRef.get();

        if (pduRequest instanceof SubmitSm) {
            SubmitSmResp resp = (SubmitSmResp)pduRequest.createResponse();
            long id = counter.getAndIncrement();

            resp.setMessageId(String.valueOf(id));

            pool.schedule(new DeliveryTask(session, (SubmitSm)pduRequest, id), 1, TimeUnit.SECONDS);

            return resp;
        } else {
            return pduRequest.createResponse();
        }
    }
}
