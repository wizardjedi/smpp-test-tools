package com.a1systems.smpp.multiplexer.server;

import com.a1systems.smpp.multiplexer.Application;
import com.a1systems.smpp.multiplexer.client.Client;
import com.a1systems.smpp.multiplexer.client.ClientSessionHandler;
import com.a1systems.smpp.multiplexer.client.RouteInfo;
import static com.a1systems.smpp.multiplexer.server.SmppServerHandlerImpl.logger;
import com.cloudhopper.commons.gsm.GsmUtil;
import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.type.LoggingOptions;
import com.cloudhopper.smpp.util.SmppUtil;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmppServerSessionHandler extends DefaultSmppSessionHandler {

    public static final Logger logger = LoggerFactory.getLogger(SmppServerSessionHandler.class);

    private WeakReference<SmppSession> sessionRef;

    protected ThreadPoolExecutor pool;

    protected ScheduledExecutorService asyncPool;

    protected SmppServerHandlerImpl handler;

    protected List<Client> clients;

    protected AtomicLong index = new AtomicLong(0);

    protected ConcurrentHashMap<String, MsgRoute> msgMap = new ConcurrentHashMap<String, MsgRoute>();
    protected String systemId;
    protected String password;

    public List<Client> getClients() {
        return clients;
    }

    public void setClients(List<Client> clients) {
        this.clients = clients;
    }

    public SmppServerSessionHandler(String systemId, String password, SmppSession session, ExecutorService pool, SmppServerHandlerImpl handler) throws Exception {
        this.sessionRef = new WeakReference<SmppSession>(session);

        this.handler = handler;

        this.pool = (ThreadPoolExecutor) pool;

        this.asyncPool = Executors.newScheduledThreadPool(2);

        this.asyncPool.scheduleAtFixedRate(new CleanupTask(msgMap), 60, 60, TimeUnit.SECONDS);

        this.systemId = systemId;
        this.password = password;

        clients = new ArrayList<>();

        for (Application.ConnectionEndpoint c : handler.endPoints) {
            SmppSessionConfiguration cfg = new SmppSessionConfiguration();
            cfg.setHost(c.getHost());
            cfg.setPort(c.getPort());
            cfg.setSystemId(systemId);
            cfg.setPassword(password);

            LoggingOptions lo = new LoggingOptions();
            lo.setLogBytes(false);
            lo.setLogPdu(false);
            cfg.setLoggingOptions(lo);

            Client client = new Client(cfg);

            client.setSessionHandler(new ClientSessionHandler(this, client));

            client.setPool(pool);
            client.start();

            clients.add(client);
        }

        for (int i = 0; i < clients.size(); i++) {
            clients.get(i).setServerSession((SmppServerSession) session);
        }

        boolean binded = false;

        long start = System.currentTimeMillis();

        try {
            do {
                TimeUnit.MILLISECONDS.sleep(500);

                for (int i = 0; i < clients.size(); i++) {
                    binded |= (clients.get(i).getSession() != null);
                }
            } while ((!binded) || (System.currentTimeMillis() - start) > 30_000);

            if (binded) {
                logger.info("Create server session");
            } else {
                logger.error("Timeout");

                session.destroy();

                throw new Exception();
            }
        } catch (InterruptedException e) {
            logger.error("{}", e);
        }

    }

    @Override
    public PduResponse firePduRequestReceived(PduRequest pduRequest) {

        if (pduRequest instanceof EnquireLink) {
            return pduRequest.createResponse();
        }

        if (pduRequest instanceof SubmitSm) {
            processSubmitSm((SubmitSm) pduRequest);
        }

        return null;
    }

    @Override
    public void fireExpectedPduResponseReceived(PduAsyncResponse pdu) {
        PduResponse pduResponse = pdu.getResponse();

        if (pduResponse instanceof DeliverSmResp) {
            RouteInfo ri = (RouteInfo) pdu.getRequest().getReferenceObject();

            pduResponse.setReferenceObject(ri);

            processDeliverSmResp((DeliverSmResp) pduResponse);
        }
    }

    @Override
    public void fireChannelUnexpectedlyClosed() {
        logger.info("Server session destroyed");

        SmppServerSession serverSession = (SmppServerSession) sessionRef.get();

        for (int i=0;i<clients.size();i++) {
            clients.get(i).stop();
        }

        msgMap = null;

        asyncPool.shutdownNow();

        serverSession.close();
        serverSession.destroy();
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
                    MsgRoute route = msgMap.get(key);

                    c = route.getClient();
                } else {
                    MsgRoute route = new MsgRoute(c);

                    msgMap.put(key, route);
                }

            }

            logger.info("{}", c);

            ri.setClient(c);

            ssm.removeSequenceNumber();

            logger.info("Threads:{} task count:{} active:{}", pool.getPoolSize(), pool.getTaskCount(), pool.getActiveCount());

            pool.submit(new OutputSender(c, (SmppServerSession) sessionRef.get(), ssm));
        }
    }

    public void processSubmitSmResp(SubmitSmResp submitSmResp) {
        logger.info("Threads:{} task count:{} active:{}", pool.getPoolSize(), pool.getTaskCount(), pool.getActiveCount());

        pool.submit(new InputSender((SmppServerSession) sessionRef.get(), submitSmResp));
    }

    public void processDeliverSm(DeliverSm deliverSm) {
        deliverSm.removeSequenceNumber();

        logger.info("Threads:{} task count:{} active:{}", pool.getPoolSize(), pool.getTaskCount(), pool.getActiveCount());

        pool.submit(new InputSender((SmppServerSession) sessionRef.get(), deliverSm));
    }

    public void processDeliverSmResp(DeliverSmResp deliverSmResp) {
        RouteInfo ri = (RouteInfo)deliverSmResp. getReferenceObject();

        logger.info("{}", ri);

        Client c = ri.getClient();

        deliverSmResp.setSequenceNumber((int) ri.getOutputSequenceNumber());

        logger.info("Threads:{} task count:{} active:{}", pool.getPoolSize(), pool.getTaskCount(), pool.getActiveCount());

        pool.submit(new OutputSender(c, (SmppServerSession) sessionRef.get(), deliverSmResp));
    }



}
