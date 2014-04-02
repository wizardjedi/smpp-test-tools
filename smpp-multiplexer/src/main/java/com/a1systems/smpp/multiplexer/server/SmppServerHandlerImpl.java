package com.a1systems.smpp.multiplexer.server;

import com.a1systems.smpp.multiplexer.Application;
import com.a1systems.smpp.multiplexer.client.Client;
import com.a1systems.smpp.multiplexer.client.ClientSessionHandler;
import com.a1systems.smpp.multiplexer.client.RouteInfo;
import com.cloudhopper.commons.gsm.GsmUtil;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.type.LoggingOptions;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppProcessingException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.util.SmppUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmppServerHandlerImpl implements SmppServerHandler {

    public static final Logger logger = LoggerFactory.getLogger(SmppServerHandlerImpl.class);

    protected SmppServerSession session;

    protected ExecutorService pool;

    List<Application.ConnectionEndpoint> endPoints;

    protected ConcurrentHashMap<Long, SmppServerSessionHandler> handlers = new ConcurrentHashMap<Long, SmppServerSessionHandler>();

    public SmppServerHandlerImpl(ExecutorService pool, List<Application.ConnectionEndpoint> endPoints) {
        this.pool = pool;

        this.endPoints = endPoints;
    }

    @Override
    public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration, BaseBind bindRequest) throws SmppProcessingException {
        String systemId = sessionConfiguration.getSystemId();
        String password = sessionConfiguration.getPassword();

        logger.debug("Bind request with {}:{}", systemId, password);
    }

    @Override
    public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse) throws SmppProcessingException {
        this.session = session;

        SmppSessionConfiguration sessionConfiguration = session.getConfiguration();

        String systemId = sessionConfiguration.getSystemId();
        String password = sessionConfiguration.getPassword();

        try {
            SmppServerSessionHandler smppServerSessionHandler = new SmppServerSessionHandler(systemId, password, session, pool, this);

            handlers.put(sessionId, smppServerSessionHandler);

            logger.debug("{}", smppServerSessionHandler);

            session.serverReady(smppServerSessionHandler);
        } catch (Exception ex) {
            logger.error("{}", ex);

            throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL);
        }
    }

    @Override
    public void sessionDestroyed(Long sessionId, SmppServerSession session) {
        logger.debug("Session destroy");

        handlers.get(sessionId).fireChannelUnexpectedlyClosed();

        session.close();
        session.destroy();
    }

}
