package com.a1systems.smpp.multiplexer.server;

import com.a1systems.smpp.multiplexer.Application;
import com.cloudhopper.smpp.SmppClient;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.type.LoggingOptions;
import com.cloudhopper.smpp.type.SmppProcessingException;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import io.netty.channel.nio.NioEventLoopGroup;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmppServerHandlerImpl implements SmppServerHandler {

    public static final Logger logger = LoggerFactory.getLogger(SmppServerHandlerImpl.class);

    protected SmppServerSession session;

    protected SmppClient smppClient;

    protected ExecutorService pool;

    protected ScheduledExecutorService asyncPool;

    List<Application.ConnectionEndpoint> endPoints;

    protected ConcurrentHashMap<Long, SmppServerSessionHandler> handlers = new ConcurrentHashMap<Long, SmppServerSessionHandler>();
    protected MetricRegistry metricsRegistry;

    public SmppServerHandlerImpl(ExecutorService pool, List<Application.ConnectionEndpoint> endPoints) {
        this.pool = pool;

        asyncPool = Executors.newScheduledThreadPool(5);

        metricsRegistry = new MetricRegistry();

        final JmxReporter reporter = JmxReporter.forRegistry(metricsRegistry).build();
        reporter.start();

        NioEventLoopGroup nelg = new NioEventLoopGroup();

        this.smppClient = new DefaultSmppClient(nelg, asyncPool);

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

        LoggingOptions lo = new LoggingOptions();
        lo.setLogBytes(false);
        lo.setLogPdu(false);

        session.getConfiguration().setLoggingOptions(lo);

        session.getConfiguration().setWriteTimeout(TimeUnit.MILLISECONDS.toMillis(200));

        try {
            SmppServerSessionHandler smppServerSessionHandler = new SmppServerSessionHandler(systemId, password, session, pool, this);

            handlers.put(sessionId, smppServerSessionHandler);

            String sessionName = systemId + "_" + password + "_" + sessionId;

            logger.info("Created session sess.id:{} and sess.name:{}", sessionId, sessionName);

            session.getConfiguration().setName(sessionName);

            session.serverReady(smppServerSessionHandler);

            smppServerSessionHandler.processQueuedRequests();
        } catch (Exception ex) {
            logger.error("{}", ex);

            throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL);
        }
    }

    @Override
    public void sessionDestroyed(Long sessionId, SmppServerSession session) {
        logger.debug("Session sess.id:{} sess.name:{} destroy", sessionId, session.getConfiguration().getName());

        handlers.get(sessionId).fireChannelUnexpectedlyClosed();

        session.close();
        session.destroy();
    }

    public SmppClient getSmppClient() {
        return this.smppClient;
    }

    public ExecutorService getPool() {
        return pool;
    }

    public void setPool(ExecutorService pool) {
        this.pool = pool;
    }

    public ScheduledExecutorService getAsyncPool() {
        return asyncPool;
    }

    public void setAsyncPool(ScheduledExecutorService asyncPool) {
        this.asyncPool = asyncPool;
    }

    public MetricRegistry getMetricsRegistry() {
        return metricsRegistry;
    }

    public void setMetricsRegistry(MetricRegistry metricsRegistry) {
        this.metricsRegistry = metricsRegistry;
    }
}
