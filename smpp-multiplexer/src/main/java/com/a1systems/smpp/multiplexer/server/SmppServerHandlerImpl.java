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
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppProcessingException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.util.SmppUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmppServerHandlerImpl implements SmppServerHandler {

    public static final Logger logger = LoggerFactory.getLogger(SmppServerHandlerImpl.class);

    protected SmppServerSession session;

    protected ExecutorService pool;

    List<Application.ConnectionEndpoint> endPoints;

    protected List<Client> clients;

    protected AtomicLong index = new AtomicLong(0);

    protected ConcurrentHashMap<String, Client> msgMap = new ConcurrentHashMap<String, Client>();

    public SmppServerHandlerImpl(ExecutorService pool, List<Application.ConnectionEndpoint> endPoints) {
        this.pool = pool;

        this.endPoints = endPoints;
    }

    public List<Client> getClients() {
        return clients;
    }

    public void setClients(List<Client> clients) {
        this.clients = clients;
    }

    @Override
    public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration, BaseBind bindRequest) throws SmppProcessingException {
        String systemId = sessionConfiguration.getSystemId();
        String password = sessionConfiguration.getPassword();

        logger.debug("Bind request with {}:{}", systemId, password);

        clients = new ArrayList<>();

        for (Application.ConnectionEndpoint c:endPoints) {
            SmppSessionConfiguration cfg = new SmppSessionConfiguration();
            cfg.setHost(c.getHost());
            cfg.setPort(c.getPort());
            cfg.setSystemId(systemId);
            cfg.setPassword(password);

            Client client = new Client(cfg);

            client.setSessionHandler(new ClientSessionHandler(this, client));

            client.setPool(pool);
            client.start();

            clients.add(client);
        }
    }

    @Override
    public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse) throws SmppProcessingException {
        this.session = session;

        for (int i=0;i<clients.size();i++) {
            clients.get(i).setServerSession(session);
        }

        session.serverReady(new SmppServerSessionHandler(session, pool, this));
    }

    @Override
    public void sessionDestroyed(Long sessionId, SmppServerSession session) {
        logger.debug("Session destroy");

        for (int i=0;i<clients.size();i++) {
            clients.get(i).stop();
        }

        session.destroy();
    }

    public void processSubmitSm(SubmitSm ssm) {
        List<Client> aliveClients = new ArrayList<>(clients.size());

        for (int i=0;i<clients.size();i++) {
            if (clients.get(i).getSession() != null) {
                aliveClients.add(clients.get(i));
            }
        }

        if (aliveClients.size() > 0) {
            long id = Math.abs(index.incrementAndGet()) % aliveClients.size();

            Client c = aliveClients.get((int)id);

            logger.info("Choosed client {}", c);

            RouteInfo ri = new RouteInfo();

            ri.setInputSequenceNumber(ssm.getSequenceNumber());

            ssm.setReferenceObject(ri);

            if (SmppUtil.isUserDataHeaderIndicatorEnabled(ssm.getEsmClass())) {
                byte[] shortMessage = ssm.getShortMessage();

                byte[] udh = GsmUtil.getShortMessageUserDataHeader(shortMessage);

                long smsId = ServerUtil.getSmsId(udh);
                long parts = ServerUtil.getParts(udh);

                String key =
                    ssm.getDestAddress().getAddress()
                        +"_"
                        +ssm.getSourceAddress().getAddress()
                        +"_"
                        +smsId
                        +"_"
                        +parts;

                logger.info("{}", key);

                if (msgMap.containsKey(key)) {
                    c = msgMap.get(key);
                } else {
                    msgMap.put(key, c);
                }

            }

            logger.info("{}", c);

            ri.setClient(c);
            pool.submit(new OutputSender(c, session, ssm));
        }
    }

    public void processSubmitSmResp(SubmitSmResp submitSmResp) {
        pool.submit(new InputSender(session, submitSmResp));
    }

    public void processDeliverSm(DeliverSm deliverSm) {
        pool.submit(new InputSender(session, deliverSm));
    }

    public void processDeliverSmResp(DeliverSmResp deliverSmResp) {
        RouteInfo ri = (RouteInfo)deliverSmResp. getReferenceObject();

        logger.info("{}", ri);

        Client c = ri.getClient();

        pool.submit(new OutputSender(c, session, deliverSmResp));
    }
}
