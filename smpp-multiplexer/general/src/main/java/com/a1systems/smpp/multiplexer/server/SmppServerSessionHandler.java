package com.a1systems.smpp.multiplexer.server;

import com.a1systems.plugin.Authorizer;
import com.a1systems.smpp.multiplexer.Application;
import com.a1systems.smpp.multiplexer.client.Client;
import com.a1systems.smpp.multiplexer.client.ClientSessionHandler;
import com.a1systems.smpp.multiplexer.client.RouteInfo;
import com.cloudhopper.commons.gsm.GsmUtil;
import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppConstants;
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
import com.cloudhopper.smpp.pdu.QuerySm;
import com.cloudhopper.smpp.pdu.QuerySmResp;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.pdu.Unbind;
import com.cloudhopper.smpp.tlv.TlvConvertException;
import com.cloudhopper.smpp.type.LoggingOptions;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.util.SmppUtil;
import com.cloudhopper.smpp.util.TlvUtil;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.RateLimiter;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmppServerSessionHandler extends DefaultSmppSessionHandler {

    /**
     * Maximum timeout for binding all clients
     */
    public static final long MAX_CLIENT_BINDING_TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    public static final Logger logger = LoggerFactory.getLogger(SmppServerSessionHandler.class);

    protected SmppSession session;

    protected ThreadPoolExecutor pool;

    protected RateLimiter elinkLimiter = RateLimiter.create(1);

    protected ScheduledExecutorService asyncPool;

    protected SmppServerHandlerImpl handler;

    protected long clientsBindingTimeout = 0;

    protected List<Client> clients;

    protected AtomicLong index = new AtomicLong(0);

    protected ConcurrentHashMap<String, MsgRoute> msgMap = new ConcurrentHashMap<String, MsgRoute>();
    protected String systemId;
    protected String password;
    protected ScheduledFuture<?> cleanUpFuture = null;

    protected CopyOnWriteArrayList<Client> aliveClients = new CopyOnWriteArrayList<Client>();

    private ScheduledFuture<?> elinkTaskFuture;

    protected volatile long lastInputMillis;
    
    protected long elinkPeriod = 30;
    
    public CopyOnWriteArrayList<Client> getAliveClients() {
        return aliveClients;
    }

    public void setAliveClients(CopyOnWriteArrayList<Client> aliveClients) {
        this.aliveClients = aliveClients;
    }

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

        if (failLogin != null && failLogin.plusMinutes(1).isAfterNow()) {
            logger.error("{} Login failed by time", sessionConfiguration.getName());

            throw new MultiplexerBindException();
        }

        ServiceLoader<Authorizer> authorizers = handler.getApp().getAuthorizers();

        if (authorizers.iterator().hasNext()) {
            boolean auth = false;

            Authorizer authorizer;

            for (Authorizer a:authorizers) {
                if (a.auth(systemId, password)) {
                    auth = true;

                    logger.info("{} Login granted by authorizer {}", session.getConfiguration().getName(), a);

                    break;
                }
            }

            if (!auth) {
                logger.error("{} Login failed by authorizers", session.getConfiguration().getName());

                session.destroy();

                throw new MultiplexerBindException();
            }
        }

        for (Application.ConnectionEndpoint c : handler.endPoints) {
            AtomicReference<DateTime> unreachableRef = c.getUnreachableSince();
            
            DateTime unreachableSince = unreachableRef.get();
            
            if (
                unreachableSince != null 
                && unreachableSince.plusSeconds(60).isAfterNow()
            ) {
                logger
                    .error(
                        "Enpoint:{} server session:{} with creds:{} / {} is unreachable", 
                        c,
                        sessionConfiguration.getName(),
                        systemId,
                        password
                    );
                
                continue;
            }
            
            SmppSessionConfiguration cfg = new SmppSessionConfiguration();
            cfg.setHost(c.getHost());
            cfg.setPort(c.getPort());
            cfg.setSystemId(systemId);
            cfg.setPassword(password);
            cfg.setRequestExpiryTimeout(TimeUnit.SECONDS.toMillis(60));
            cfg.setWindowMonitorInterval(TimeUnit.SECONDS.toMillis(30));
            cfg.setConnectTimeout(TimeUnit.SECONDS.toMillis(5));
            cfg.setType(sessionConfiguration.getType());

            LoggingOptions lo = new LoggingOptions();
            lo.setLogBytes(false);
            lo.setLogPdu(false);
            cfg.setLoggingOptions(lo);

            // don't set big values
            cfg.setWindowSize(1000);
            cfg.setName(c.getHost() + ":" + c.getPort() + ":" + systemId + ":" + password);

            Client client = new Client(cfg, handler.getSmppClient(), c);

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

            } while ((!binded) && ((System.currentTimeMillis() - start) < 11000));

            if (binded) {
                cleanUpFuture =
                    this
                        .asyncPool
                        .scheduleAtFixedRate(
                            new CleanupTask(
                                msgMap,
                                sessionConfiguration.getName()
                            ),
                            60,
                            60,
                            TimeUnit.SECONDS
                        );

                logger.info("{} Create server session",sessionConfiguration.getName());
            } else {
                logger.error("{} Timeout", sessionConfiguration.getName());

                String failedLoginId = systemId+"/"+password;
                
                if (
                    !handler.failedLogins.containsKey(failedLoginId)
                    || handler.failedLogins.get(failedLoginId).plusMinutes(1).isBeforeNow()
                ) {
                    handler.failedLogins.put(failedLoginId, DateTime.now());
                }
                
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

        if (pduRequest instanceof Unbind) {

            Unbind unbind = (Unbind) pduRequest;

            logger.info("{} Got unbind:{}", session.getConfiguration().getName(), unbind);

            try {
                getSession().sendResponsePdu(unbind.createResponse());
            } catch (RecoverablePduException ex) {
                logger.error("{}", ex);
            } catch (UnrecoverablePduException ex) {
                logger.error("{}", ex);
            } catch (SmppChannelException ex) {
                logger.error("{}", ex);
            } catch (InterruptedException ex) {
                logger.error("{}", ex);
            }

            session.destroy();
            session.close();
        }

        if (pduRequest instanceof EnquireLink) {
            if (!elinkLimiter.tryAcquire()) {
                logger.error("{} Too much elinks. Close channel.", session.getConfiguration().getName());

                session.destroy();
                session.close();

                return null;
            } else {
                logger.info("{} Got elink response with elink_resp", session.getConfiguration().getName());

                return pduRequest.createResponse();
            }
        }

        if (pduRequest instanceof QuerySm) {
            return processQuerySm((QuerySm)pduRequest);
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
        logger.info("Channel unexpectedly closed {}", session.getConfiguration().getName());

        try {
            SmppServerSession serverSession = (SmppServerSession) session;

            if (
                elinkTaskFuture != null
                && !elinkTaskFuture.isCancelled()
            ) {
                elinkTaskFuture.cancel(true);
                elinkTaskFuture = null;
            }

            logger.info("Server session sess.name:{} unexpectedly closed", serverSession.getConfiguration().getName());

            if (
                clients != null
                && !clients.isEmpty()
            ) {
                for (Client client : clients) {
                    client.stop();
                }

                clients.clear();
                clients = null;
            }

            if (aliveClients != null) {
                aliveClients.clear();
                aliveClients = null;
            }

            if (
                handler !=null
                && handler.getMetricsRegistry() != null
            ) {
                handler.getMetricsRegistry().remove(session.getConfiguration().getName() + "_ssm");
                handler.getMetricsRegistry().remove(session.getConfiguration().getName() + "_dsm");
                handler.getMetricsRegistry().remove(session.getConfiguration().getName() + "_ssmr");
                handler.getMetricsRegistry().remove(session.getConfiguration().getName() + "_dsmr");
            }

            if (msgMap != null) {
                msgMap.clear();
                msgMap = null;
            }

            if (cleanUpFuture != null) {
                cleanUpFuture.cancel(true);
            }

            if (serverSession != null) {
                serverSession.destroy();
                serverSession.close();
                serverSession = null;
            }

            logger.info("Firechannel closed completed");
        } catch (Exception e) {
            logger.error("Exception in channel closed handler", e);
        }
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

            session.destroy();
            session.close();
        }
    }

    public void processSubmitSm(SubmitSm ssm) {
        SmppServerSession serverSession = (SmppServerSession) session;

        DefaultSmppSession defaultSmppSession = (DefaultSmppSession) session;

        InetSocketAddress remoteAddress = (InetSocketAddress)defaultSmppSession.getChannel().remoteAddress();
        InetSocketAddress localAddress = (InetSocketAddress)defaultSmppSession.getChannel().localAddress();

        this.lastInputMillis = System.currentTimeMillis();
        
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
            long incrementedIndex = index.incrementAndGet();
            
            long totalWeight = 0;
            
            for (Client c:aliveClients) {
                totalWeight += c.getEndpoint().getWeight();
            }
            
            long modulus = Math.abs(incrementedIndex) % totalWeight;

            long clientTotalWeight = 0;
            
            Client c = aliveClients.get(0);
            
            for (Client client:aliveClients) {
                if (
                    modulus >= clientTotalWeight 
                    && modulus < clientTotalWeight + client.getEndpoint().getWeight()
                ) {
                    c = client;
                }

                clientTotalWeight += client.getEndpoint().getWeight();
            }
            
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

            MetricRegistry registry = handler.getMetricsRegistry();

            pool.submit(new OutputSender(c, (SmppServerSession) session, ssm, System.currentTimeMillis(), registry));

            MetricsHelper.poolQueueSize(registry, pool.getQueue().size());
        }
    }    
    
    public PduResponse processQuerySm(QuerySm qsm) {
        SmppServerSession serverSession = (SmppServerSession) session;

        DefaultSmppSession defaultSmppSession = (DefaultSmppSession) session;

        InetSocketAddress remoteAddress = (InetSocketAddress)defaultSmppSession.getChannel().remoteAddress();
        InetSocketAddress localAddress = (InetSocketAddress)defaultSmppSession.getChannel().localAddress();

        this.lastInputMillis = System.currentTimeMillis();
        
        String adrInfo =
            remoteAddress.getHostString()
                + ":"
                + remoteAddress.getPort();

        try {
            qsm.setOptionalParameter(TlvUtil.createNullTerminatedStringTlv((byte)0x4123, adrInfo));

            qsm.calculateAndSetCommandLength();
        } catch (TlvConvertException ex) {
            logger.error("Can't pack TLV:{}", adrInfo);
        }

        if (aliveClients.size() > 0) {
            String messageId = qsm.getMessageId();
            
            for (Client c:aliveClients) {
                if (messageId.endsWith(c.getEndpoint().getNodeId())) {
                    if (c.isBound()) {
                        try {
                            RouteInfo ri = new RouteInfo();

                            ri.setInputSequenceNumber(qsm.getSequenceNumber());
                            ri.setCreateDate(DateTime.now());
                            ri.setClient(c);
                            
                            qsm.setReferenceObject(ri);
                            
                            qsm.removeSequenceNumber();
                            
                            logger
                                .info(
                                        "sess.name:{} got qsm.seq_num:{} qsm.msg_id:{} -> {} ssm.seq_num:{} [{}]",
                                        serverSession.getConfiguration().getName(),
                                        qsm.getSequenceNumber(),
                                        qsm.getMessageId(),
                                        c.toStringShortConnectionParams(),
                                        ((DefaultSmppSession) c.getSession()).getSequenceNumber().peek(),
                                        adrInfo
                                );
                            
                            MetricRegistry registry = handler.getMetricsRegistry();            
                                        
                            pool
                                .submit(
                                        new OutputSender(
                                            c, 
                                            (SmppServerSession) session, 
                                            qsm, 
                                            System.currentTimeMillis(), 
                                            registry
                                        )
                                );
                            
                            MetricsHelper.poolQueueSize(registry, pool.getQueue().size());
                            
                            return null;
                        } catch (Exception ex) {
                            logger.error("Error processing query_sm", ex);
                        }
                    }
                }
            }
        }
        
        QuerySmResp resp = qsm.createResponse();

        resp.setErrorCode((byte)SmppConstants.STATUS_SYSERR);

        return resp;
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

        pool.submit(new InputSender(serverSession, submitSmResp, System.currentTimeMillis(), metricsRegistry));

        int size = pool.getQueue().size();

        MetricsHelper.poolQueueSize(metricsRegistry, pool.getQueue().size());
    }

    public void processDeliverSm(DeliverSm deliverSm) {
        SmppServerSession serverSession = (SmppServerSession) session;

        RouteInfo ri = (RouteInfo) deliverSm.getReferenceObject();

        UUID uid = UUID.randomUUID();
        
        logger
                .info(
                        "uid:{} {} dsm.seq_num:{} dsm.dest:{} dsm.src:{} -> {}",
                        uid,
                        ri.getClient().toStringShortConnectionParams(),
                        deliverSm.getSequenceNumber(),
                        deliverSm.getDestAddress().getAddress(),
                        deliverSm.getSourceAddress().getAddress(),
                        serverSession.getConfiguration().getName()
                );

        deliverSm.removeSequenceNumber();

        MetricRegistry metricsRegistry = handler.getMetricsRegistry();

        pool.submit(new InputSender(uid, (SmppServerSession) session, deliverSm, System.currentTimeMillis(), metricsRegistry));

        int size = pool.getQueue().size();

        MetricsHelper.poolQueueSize(metricsRegistry, size);
    }

    public void processDeliverSmResp(DeliverSmResp deliverSmResp) {
        MetricRegistry metricsRegistry = handler.getMetricsRegistry();

        this.lastInputMillis = System.currentTimeMillis();
        
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

        pool.submit(new OutputSender(c, serverSession, deliverSmResp, System.currentTimeMillis(), metricsRegistry));

        MetricsHelper.poolQueueSize(metricsRegistry, pool.getQueue().size());
    }

    public void clientBinding(Client client) {
        if (!client.isHidden() && aliveClients.contains(client)) {
            aliveClients.remove(client);
        }

        if (clientsBindingTimeout == 0) {
            clientsBindingTimeout = System.currentTimeMillis();
        }

        if (
            System.currentTimeMillis() - clientsBindingTimeout > MAX_CLIENT_BINDING_TIMEOUT
            && aliveClients.isEmpty()
        ) {
            logger.error("No clients. Destroy session.");

            session.destroy();
            session.close();
        }
    }

    public void clientBound(Client client) {
        if (!client.isHidden()) {
            aliveClients.add(client);

            clientsBindingTimeout = 0;
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
        elinkTaskFuture = 
                asyncPool
                    .scheduleAtFixedRate(
                        new ServerElinkTask(smppServerSessionHandler), 
                        getElinkPeriod(), 
                        getElinkPeriod(), 
                        TimeUnit.SECONDS
                    );
    }

    public long getLastInputMillis() {
        return lastInputMillis;
    }

    public void setLastInputMillis(long lastInputMillis) {
        this.lastInputMillis = lastInputMillis;
    }

    public long getElinkPeriod() {
        return elinkPeriod;
    }

    public void setElinkPeriod(long elinkPeriod) {
        this.elinkPeriod = elinkPeriod;
    }

    public void processQuerySmResp(PduRequest req, QuerySmResp querySmResp) {
        RouteInfo ri = (RouteInfo) req.getReferenceObject();

        int sequenceNumber = querySmResp.getSequenceNumber();

        querySmResp.setSequenceNumber((int) ri.getInputSequenceNumber());        

        SmppServerSession serverSession = (SmppServerSession) session;

        logger
            .info(
                "{} qsmr.seq_num:{} status:{} msg_id:{} msg.state:{} -> {} qsmr.seq_num:{}",
                ri.getClient().toStringConnectionParams(),
                sequenceNumber,
                querySmResp.getCommandStatus(),
                querySmResp.getMessageId(),
                querySmResp.getMessageState(),
                serverSession.getConfiguration().getName(),
                querySmResp.getSequenceNumber()
            );
        
        MetricRegistry metricsRegistry = handler.getMetricsRegistry();

        pool.submit(new InputSender(serverSession, querySmResp, System.currentTimeMillis(), metricsRegistry));

        int size = pool.getQueue().size();

        MetricsHelper.poolQueueSize(metricsRegistry, pool.getQueue().size());
    }
}
