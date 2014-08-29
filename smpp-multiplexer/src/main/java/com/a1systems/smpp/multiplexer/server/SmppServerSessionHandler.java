package com.a1systems.smpp.multiplexer.server;

import com.a1systems.smpp.multiplexer.Application;
import com.a1systems.smpp.multiplexer.client.Client;
import com.a1systems.smpp.multiplexer.client.ClientSessionHandler;
import com.a1systems.smpp.multiplexer.client.RouteInfo;
import com.cloudhopper.commons.gsm.GsmUtil;
import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.EnquireLinkResp;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.tlv.TlvConvertException;
import com.cloudhopper.smpp.type.LoggingOptions;
import com.cloudhopper.smpp.util.SmppUtil;
import com.cloudhopper.smpp.util.TlvUtil;
import com.codahale.metrics.MetricRegistry;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmppServerSessionHandler extends DefaultSmppSessionHandler {

    public static final Logger logger = LoggerFactory.getLogger(SmppServerSessionHandler.class);

    protected SmppSession session;

    protected ThreadPoolExecutor pool;

    protected ScheduledExecutorService asyncPool;

    protected SmppServerHandlerImpl handler;

    protected List<Client> clients;

    protected AtomicLong index = new AtomicLong(0);

    protected ConcurrentHashMap<String, MsgRoute> msgMap = new ConcurrentHashMap<String, MsgRoute>();
    protected String systemId;
    protected String password;
    protected ScheduledFuture<?> cleanUpFuture = null;

    protected CopyOnWriteArrayList<Client> aliveClients = new CopyOnWriteArrayList<Client>();
    private ScheduledFuture<?> elinkTaskFuture;

    public SmppSession getSession() {
        return session;
    }
    
    public List<Client> getClients() {
        return clients;
    }

    public void setClients(List<Client> clients) {
        this.clients = clients;
    }

    public SmppServerSessionHandler(String systemId, String password, SmppSessionConfiguration sessionConfiguration, ExecutorService pool, SmppServerHandlerImpl handler) throws Exception {
        //this.session = session;

        this.handler = handler;

        this.pool = (ThreadPoolExecutor) pool;

        this.asyncPool = handler.getAsyncPool();

        this.systemId = systemId;
        this.password = password;

        clients = new ArrayList<Client>();

        DateTime failLogin = handler.failedLogins.get(systemId+"/"+password);
        
        if (failLogin != null && failLogin.plusMinutes(10).isAfterNow()) {
            logger.error("{} Login failed by time", sessionConfiguration.getName());
            
            throw new MultiplexerBindException();
        }
        
        for (Application.ConnectionEndpoint c : handler.endPoints) {
            SmppSessionConfiguration cfg = new SmppSessionConfiguration();
            cfg.setHost(c.getHost());
            cfg.setPort(c.getPort());
            cfg.setSystemId(systemId);
            cfg.setPassword(password);
            cfg.setRequestExpiryTimeout(TimeUnit.SECONDS.toMillis(60));
            cfg.setWindowMonitorInterval(TimeUnit.SECONDS.toMillis(60));
            
            cfg.setType(sessionConfiguration.getType());

            LoggingOptions lo = new LoggingOptions();
            lo.setLogBytes(false);
            lo.setLogPdu(false);
            cfg.setLoggingOptions(lo);

            // don't set big values
            cfg.setWindowSize(1000);
            cfg.setName(c.getHost() + ":" + c.getPort() + ":" + systemId + ":" + password);

            Client client = new Client(cfg, handler.getSmppClient());

            client.setHidden(c.isHidden());

            client.setServerSessionHandler(this);
            client.setSessionHandler(new ClientSessionHandler(this, client));

            client.setElinkPeriod(30);

            client.setTimer(asyncPool);
            client.setPool(pool);
            client.start();

            clients.add(client);
        }

        boolean binded = false;

        long start = System.currentTimeMillis();

        try {
            do {
                TimeUnit.MILLISECONDS.sleep(500);

                for (Client client : clients) {
                    binded |= client.isBound();
                }

            } while ((!binded) && ((System.currentTimeMillis() - start) < 4000));

            if (binded) {
                cleanUpFuture = this.asyncPool.scheduleAtFixedRate(new CleanupTask(msgMap), 60, 60, TimeUnit.SECONDS);

                logger.info("{} Create server session",sessionConfiguration.getName());
            } else {
                logger.error("{} Timeout", sessionConfiguration.getName());

                handler.failedLogins.put(systemId+"/"+password, DateTime.now());
                
                for (Client client : clients) {
                    client.stop();
                }

                throw new MultiplexerBindException();
            }
        } catch (InterruptedException e) {
            logger.error("{}", e);
        }

    }

    @Override
    public PduResponse firePduRequestReceived(PduRequest pduRequest) {

        if (pduRequest instanceof EnquireLink) {
            logger.info("{} Got elink response with elink_resp", session.getConfiguration().getName());
            
            return pduRequest.createResponse();
        }

        if (pduRequest instanceof SubmitSm) {
            MetricRegistry metricsRegistry = handler.getMetricsRegistry();
            
            metricsRegistry.meter(session.getConfiguration().getName() + "_ssm").mark();
            metricsRegistry.meter("total_ssm").mark();

            processSubmitSm((SubmitSm) pduRequest);
        }

        return null;
    }

    @Override
    public void fireExpectedPduResponseReceived(PduAsyncResponse pdu) {
        PduResponse pduResponse = pdu.getResponse();

        if (pduResponse instanceof DeliverSmResp) {
            MetricRegistry metricsRegistry = handler.getMetricsRegistry();
            
            metricsRegistry.meter(session.getConfiguration().getName() + "_dsm").mark();
            metricsRegistry.meter("total_dsm").mark();

            RouteInfo ri = (RouteInfo) pdu.getRequest().getReferenceObject();

            pduResponse.setReferenceObject(ri);

            processDeliverSmResp((DeliverSmResp) pduResponse);
        }
        
        if (pduResponse instanceof EnquireLinkResp) {
            logger
                .info(
                    "{} elink success", 
                    getSession().getConfiguration().getName()
                );
        }
    }

    @Override
    public void fireChannelUnexpectedlyClosed() {
        SmppServerSession serverSession = (SmppServerSession) session;

        if (
            elinkTaskFuture!=null 
            && !elinkTaskFuture.isCancelled()
        ) {
            elinkTaskFuture.cancel(true);
            elinkTaskFuture = null;
        }
          
        logger.info("Server session sess.name:{} unexpectedly closed", serverSession.getConfiguration().getName());

        for (Client client : clients) {
            client.stop();
        }
        
        clients.clear();
        clients = null;
        aliveClients.clear();
        aliveClients = null;
        
        handler.getMetricsRegistry().remove(session.getConfiguration().getName() + "_ssm");
        handler.getMetricsRegistry().remove(session.getConfiguration().getName() + "_dsm");
        handler.getMetricsRegistry().remove(session.getConfiguration().getName() + "_ssmr");
        handler.getMetricsRegistry().remove(session.getConfiguration().getName() + "_dsmr");

        msgMap.clear();
        msgMap = null;

        if (cleanUpFuture != null) {
            cleanUpFuture.cancel(true);
        }
   
        serverSession.destroy();
        serverSession.close();
    }

    @Override
    public void firePduRequestExpired(PduRequest pduRequest) {   
        logger
            .error(
                "{} expired pdu.type:{} pdu.seq_num:{}",
                ((SmppSession)session).getConfiguration().getName(),
                pduRequest.getClass().toString(),
                pduRequest.getSequenceNumber()
            );
        
        if (pduRequest instanceof EnquireLink) {
            logger
                .error(
                    "{} enquire link expired. Close server session.", 
                    ((SmppSession)session).getConfiguration().getName()
                );
            
            getSession().destroy();
        }
    }

    public void processSubmitSm(SubmitSm ssm) {
        SmppServerSession serverSession = (SmppServerSession) session;

        DefaultSmppSession defaultSmppSession = (DefaultSmppSession) session;
        
        InetSocketAddress remoteAddress = (InetSocketAddress)defaultSmppSession.getChannel().remoteAddress();
        InetSocketAddress localAddress = (InetSocketAddress)defaultSmppSession.getChannel().localAddress();
        
        String adrInfo = 
            remoteAddress.getHostString()
                + ":"
                + remoteAddress.getPort();
        
        try {
            ssm.setOptionalParameter(TlvUtil.createNullTerminatedStringTlv((byte)0x4123, adrInfo));
            
            ssm.calculateAndSetCommandLength();
        } catch (TlvConvertException ex) {
            logger.error("Can't pack TLV:{}", adrInfo);
        }
        
        
        if (aliveClients.size() > 0) {
            long id = Math.abs(index.incrementAndGet()) % aliveClients.size();

            Client c = aliveClients.get((int) id);

            RouteInfo ri = new RouteInfo();

            ri.setInputSequenceNumber(ssm.getSequenceNumber());

            ssm.setReferenceObject(ri);

            if (SmppUtil.isUserDataHeaderIndicatorEnabled(ssm.getEsmClass())) {
                byte[] shortMessage = ssm.getShortMessage();

                byte[] udh = GsmUtil.getShortMessageUserDataHeader(shortMessage);

                long smsId = ServerUtil.getSmsId(udh);
                long parts = ServerUtil.getParts(udh);

                String key
                        = ssm.getDestAddress().getAddress()
                        + "_"
                        + ssm.getSourceAddress().getAddress()
                        + "_"
                        + smsId
                        + "_"
                        + parts;

                logger
                    .info(
                        "sess.name:{} got ssm.seq_num:{} multipart message key:{}",
                        serverSession.getConfiguration().getName(),
                        ssm.getSequenceNumber(),
                        key
                    );

                if (msgMap.containsKey(key)) {
                    MsgRoute route = msgMap.get(key);

                    if (route.getClient().isBound()
                            && route.getClient().getSession() != null) {
                        c = route.getClient();
                    }
                } else {
                    MsgRoute route = new MsgRoute(c);

                    msgMap.put(key, route);
                }

            }

            logger
                    .info(
                            "sess.name:{} got ssm.seq_num:{} ssm.dest:{} ssm.src:{} -> {} ssm.seq_num:{} [{}]",
                            serverSession.getConfiguration().getName(),
                            ssm.getSequenceNumber(),
                            ssm.getDestAddress().getAddress(),
                            ssm.getSourceAddress().getAddress(),
                            c.toStringShortConnectionParams(),
                            ((DefaultSmppSession) c.getSession()).getSequenceNumber().peek(),
                            adrInfo
                    );

            ri.setClient(c);

            ssm.removeSequenceNumber();

            pool.submit(new OutputSender(c, (SmppServerSession) session, ssm));
        }
    }

    public void processSubmitSmResp(PduRequest req, SubmitSmResp submitSmResp) {
        RouteInfo ri = (RouteInfo) req.getReferenceObject();

        int sequenceNumber = submitSmResp.getSequenceNumber();

        submitSmResp.setSequenceNumber((int) ri.getInputSequenceNumber());

        SmppServerSession serverSession = (SmppServerSession) session;

        logger
            .info(
                "{} ssmr.seq_num:{} status:{} msg_id:{} -> {} ssmr.seq_num:{}",
                ri.getClient().toStringConnectionParams(),
                sequenceNumber,
                submitSmResp.getCommandStatus(),
                submitSmResp.getMessageId(),
                serverSession.getConfiguration().getName(),
                submitSmResp.getSequenceNumber()
            );

        MetricRegistry metricsRegistry = handler.getMetricsRegistry();
        metricsRegistry.meter(session.getConfiguration().getName() + "_ssmr").mark();
        metricsRegistry.meter("total_ssmr").mark();
        
        pool.submit(new InputSender(serverSession, submitSmResp));
    }

    public void processDeliverSm(DeliverSm deliverSm) {
        SmppServerSession serverSession = (SmppServerSession) session;

        RouteInfo ri = (RouteInfo) deliverSm.getReferenceObject();

        logger
                .info(
                        "{} dsm.seq_num:{} dsm.dest:{} dsm.src:{} -> {}",
                        ri.getClient().toStringShortConnectionParams(),
                        deliverSm.getSequenceNumber(),
                        deliverSm.getDestAddress().getAddress(),
                        deliverSm.getSourceAddress().getAddress(),
                        serverSession.getConfiguration().getName()
                );

        deliverSm.removeSequenceNumber();

        pool.submit(new InputSender((SmppServerSession) session, deliverSm));
    }

    public void processDeliverSmResp(DeliverSmResp deliverSmResp) {
        MetricRegistry metricsRegistry = handler.getMetricsRegistry();
            
        metricsRegistry.meter(session.getConfiguration().getName() + "_dsmr").mark();
        metricsRegistry.meter("total_dsmr").mark();

        RouteInfo ri = (RouteInfo) deliverSmResp.getReferenceObject();

        Client c = ri.getClient();

        int oldSequenceNumber = deliverSmResp.getSequenceNumber();

        deliverSmResp.setSequenceNumber((int) ri.getOutputSequenceNumber());

        SmppServerSession serverSession = (SmppServerSession) session;

        logger
                .info(
                        "sess:{} dsmr.seq_num:{} status:{} -> {} dsm.seq_num:{}",
                        serverSession.getConfiguration().getName(),
                        oldSequenceNumber,
                        deliverSmResp.getCommandStatus(),
                        c.toStringConnectionParams(),
                        deliverSmResp.getSequenceNumber()
                );

        pool.submit(new OutputSender(c, serverSession, deliverSmResp));
    }

    public void clientBinding(Client client) {
        if (!client.isHidden()) {
            aliveClients.remove(client);
        }
    }

    public void clientBound(Client client) {
        if (!client.isHidden()) {
            aliveClients.add(client);
        }
    }

    public void processQueuedRequests() {
        for (Client c:clients) {
            c.setActive(true);
        }

        for (Client c:clients) {
            PduRequest r = c.getFromQueue();

            while (r != null) {
                processDeliverSm((DeliverSm)r);

                r = c.getFromQueue();
            }
        }
    }

    public void useServerSession(SmppServerSession session) {
        this.session = session;
        
        for (Client client : clients) {
            client.setServerSession((SmppServerSession) session);
        }
    }

    public void startElinkTask(SmppServerSessionHandler smppServerSessionHandler) {
        elinkTaskFuture = asyncPool.scheduleAtFixedRate(new ServerElinkTask(smppServerSessionHandler), 30, 30, TimeUnit.SECONDS);
    }

}
