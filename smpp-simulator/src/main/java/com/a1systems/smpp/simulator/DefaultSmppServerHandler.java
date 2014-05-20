package com.a1systems.smpp.simulator;

import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.type.SmppProcessingException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.script.ScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSmppServerHandler implements SmppServerHandler {

    protected static final Logger logger = LoggerFactory.getLogger(DefaultSmppServerHandler.class);

    protected ScheduledExecutorService pool;

    protected Application app;

    protected ConcurrentHashMap<Long, SessionHandler> sessionHandlers = new ConcurrentHashMap<Long, SessionHandler>();

    public DefaultSmppServerHandler(Application app, ScheduledExecutorService pool) {
        this.pool = pool;
        this.app = app;
    }

    @Override
    public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration, final BaseBind bindRequest) throws SmppProcessingException {
        sessionConfiguration.setName("Application.SMPP." + sessionConfiguration.getSystemId());

        if (app.getInvocableEngine() != null) {
            try {
                Object result = app.getInvocableEngine().invokeFunction(ScriptConstants.HANDLER_ON_BIND_REQUEST, sessionConfiguration, bindRequest);

                if (result != null) {
                    Integer intResult = (Integer)result;

                    if (intResult != 0) {
                        throw new SmppProcessingException(intResult);
                    }
                }
            } catch (ScriptException ex) {
                logger.error("sessionBindRequested() error:{}", ex.getMessage());
            } catch (NoSuchMethodException ex) {
                /* */
            }
        }
    }

    @Override
    public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse) throws SmppProcessingException {
        logger.info("Session created: {}", session);

        SimulatorSession simSession = new SimulatorSession();

        simSession.setSession(session);

        simSession.setSimulator(app.getSimulator());

        if (app.getInvocableEngine() != null) {
            try {
                app.getInvocableEngine().invokeFunction(ScriptConstants.HANDLER_ON_SESSION_CREATED, simSession, preparedBindResponse);
            } catch (ScriptException ex) {
                logger.error("sessionCreated() error:{}", ex.getMessage());
            } catch (NoSuchMethodException ex) {
                /* */
            }
        }

        SessionHandler sessionHandler = new SessionHandler(app, session, simSession, pool);

        sessionHandlers.put(sessionId, sessionHandler);

        session.serverReady(sessionHandler);
    }

    @Override
    public void sessionDestroyed(Long sessionId, SmppServerSession session) {
        logger.info("Session destroyed: {}", session);

        sessionHandlers.get(sessionId).fireChannelUnexpectedlyClosed();

        if (app.getInvocableEngine() != null) {
            try {
                app.getInvocableEngine().invokeFunction(ScriptConstants.HANDLER_ON_SESSION_DESTROYED, session);
            } catch (ScriptException ex) {
                logger.error("sessionBindRequested() error:{}", ex.getMessage());
            } catch (NoSuchMethodException ex) {
                /* */
            }
        }

        sessionHandlers.remove(sessionId);

        session.destroy();
    }

}
