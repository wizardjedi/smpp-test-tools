package com.a1systems.smpp.simulator;

import com.cloudhopper.smpp.PduAsyncResponse;
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
import javax.script.ScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionHandler extends DefaultSmppSessionHandler {

    public static final Logger logger = LoggerFactory.getLogger(SessionHandler.class);

    protected ScheduledExecutorService pool;

    protected Application app;

    protected WeakReference<SmppSession> sessionRef;

    protected AtomicLong counter = new AtomicLong(1);

    protected SimulatorSession simulatorSession;

    public SessionHandler(Application app,SmppSession session, SimulatorSession simSession, ScheduledExecutorService pool) {
        this.sessionRef = new WeakReference<SmppSession>(session);
        this.app = app;
        this.pool = pool;

        this.simulatorSession = simSession;
    }

    @Override
    public PduResponse firePduRequestReceived(PduRequest pduRequest) {
        SmppSession session = sessionRef.get();

        if (app.getInvocableEngine() != null) {
            try {
                Object result = app.getInvocableEngine().invokeFunction(ScriptConstants.HANDLER_ON_PDU_REQUEST, simulatorSession, pduRequest);

                return (PduResponse) result;
            } catch (ScriptException ex) {
                logger.error("firePduRequestReceived {}", ex.getMessage());
            } catch (NoSuchMethodException ex) {
                /* */
            }

            return pduRequest.createResponse();
        } else {
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

    @Override
    public void fireChannelUnexpectedlyClosed() {
        logger.error("Channel unexpectedly closed");

        if (app.getInvocableEngine() != null) {
            try {
                app.getInvocableEngine().invokeFunction(ScriptConstants.HANDLER_ON_CHANNEL_CLOSED, simulatorSession);
            } catch (ScriptException ex) {
                logger.error("fireChannelUnexpectedlyClosed {}", ex.getMessage());
            } catch (NoSuchMethodException ex) {
                /* */
            }
        }

        super.fireChannelUnexpectedlyClosed();
    }

    @Override
    public void firePduRequestExpired(PduRequest pduRequest) {
        super.firePduRequestExpired(pduRequest);
    }

    @Override
    public void fireUnexpectedPduResponseReceived(PduResponse pduResponse) {
        super.fireUnexpectedPduResponseReceived(pduResponse);
    }

    @Override
    public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
        super.fireExpectedPduResponseReceived(pduAsyncResponse);
    }




}
