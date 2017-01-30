package com.a1systems.smpp.multiplexer.client;

import com.a1systems.smpp.multiplexer.Application;
import com.a1systems.smpp.multiplexer.server.SmppServerSessionHandler;
import com.cloudhopper.smpp.SmppClient;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.Unbind;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.google.common.util.concurrent.RateLimiter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {

    protected static AtomicLong clientNum;

    static {
        clientNum = new AtomicLong(0);
    }

    public static final Logger log = LoggerFactory.getLogger(Client.class);

    protected SmppSessionConfiguration cfg;
    protected SmppSessionHandler sessionHandler;
    protected ClientState state;

    protected volatile SmppSession session;

    protected volatile boolean active = false;

    protected ArrayBlockingQueue<PduRequest> requests = new ArrayBlockingQueue<PduRequest>(1000);

    protected volatile long lastSendMillis;

    protected SmppServerSession serverSession;

    protected boolean hidden = false;

    protected SmppClient smppClient;

    protected Application.ConnectionEndpoint endpoint;
    
    protected ScheduledExecutorService timer;

    protected ScheduledFuture<?> elinkTask;
    protected ScheduledFuture<?> rebindTask;

    protected long rebindPeriod = 5;
    protected long elinkPeriod = 5;
    protected long resendPeriod = 60;

    protected ExecutorService pool;

    protected int speed = 3000;
    protected RateLimiter rateLimiter;

    protected DateTimeZone timeZone = DateTimeZone.getDefault();

    protected String connectionString;
    protected String shortConnectonString;
    protected SmppServerSessionHandler serverSessionHandler;

    protected String name;

    public Client(SmppSessionConfiguration cfg) {
        this(cfg, null, null);
    }

    public Client(SmppSessionConfiguration cfg, SmppClient smppClient, Application.ConnectionEndpoint endpoint) {
        this.cfg = cfg;

        this.endpoint = endpoint;
        
        this.smppClient = smppClient;
     
        this.state = ClientState.IDLE;

        connectionString = cfg.getHost()+":"+cfg.getPort()+":["+cfg.getSystemId()+":"+cfg.getPassword()+"]";
        shortConnectonString = cfg.getHost()+":"+cfg.getPort();

        name = "c<"+clientNum.incrementAndGet()+">";
    }

    public void start() {
        log.debug("Starting client {}", this.toStringConnectionParams());

        if (smppClient == null) {
            this.smppClient = new DefaultSmppClient();
        }

        if (this.timer == null) {
            this.timer = Executors.newScheduledThreadPool(2);
        }

        this.bind();
    }

    private void runRebindTask() {
        this.rebindTask = this.timer.scheduleAtFixedRate(new RebindTask(this), 0, getRebindPeriod(), TimeUnit.SECONDS);
    }

    private void runElinkTask() {
        this.elinkTask = this.timer.scheduleAtFixedRate(new ElinkTask(this), getElinkPeriod(), getElinkPeriod(), TimeUnit.SECONDS);
    }

    public void bind() {
        if (
            this.state == ClientState.BOUND
            || this.state == ClientState.IDLE
        ) {
            log.debug("{} Binding state", toStringConnectionParams());

            if (
                this.session != null
                && this.session.isBound()
            ) {
                this.session.close();
                this.session.destroy();
                this.session = null;
            }

            this.state = ClientState.BINDING;

            if (serverSessionHandler != null) {
                serverSessionHandler.clientBinding(this);
            }

            if (elinkTask != null) {
                this.elinkTask.cancel(true);
            }
            runRebindTask();
        }
    }

    /**
     * Fired on processing BIND event
     */
    public void onBindEvent() {
        log.debug("{} Fired onBindEvent()", toStringConnectionParams());
        
        if (this.state == ClientState.BINDING) {
            if (serverSessionHandler != null) {
                serverSessionHandler.clientBinding(this);
            }
        }
    }
    
    public void bound(SmppSession session) {
        if (this.state == ClientState.BINDING) {
            log.debug("{} {} Bound state", getName(), toStringConnectionParams());

            this.state = ClientState.BOUND;

            SmppSessionConfiguration cfg2 = session.getConfiguration();

            if (!cfg2.getHost().equals(this.cfg.getHost()) || !cfg2.getPassword().equals(this.cfg.getPassword())) {
                System.exit(12);
                throw new RuntimeException("Ahtung!!!");
            }

            this.session = session;

            if (rebindTask != null) {
                this.rebindTask.cancel(true);
            }
            runElinkTask();

            if (serverSessionHandler != null) {
                serverSessionHandler.clientBound(this);
            }
        }
    }

    public void stop() {
        log.debug("{} Stopping", toStringConnectionParams());

        this.state = ClientState.STOPPING;

        if (this.elinkTask != null) {
            this.elinkTask.cancel(true);
            
            elinkTask = null;
        }

        if (this.rebindTask != null) {
            this.rebindTask.cancel(true);
            rebindTask = null;
        }

        if (session != null) {
            session.unbind(TimeUnit.SECONDS.toMillis(30));
            session.destroy();
            session.close();
            session = null;
        }
        
        if (requests != null) {
            requests.clear();
            requests = null;
        }
        
        if (serverSessionHandler != null) {
            serverSessionHandler = null;
        }
    }

    public void addToQueue(PduRequest pdu) {
        this.requests.add(pdu);
    }

    public Application.ConnectionEndpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Application.ConnectionEndpoint endpoint) {
        this.endpoint = endpoint;
    }
    
    public PduRequest getFromQueue() {
        return this.requests.poll();
    }

    public ClientState getState() {
        return state;
    }

    public boolean isIdle() {
        return state == ClientState.IDLE;
    }

    public boolean isBinding() {
        return state == ClientState.BINDING;
    }

    public boolean isBound() {
        return state == ClientState.BOUND;
    }

    public boolean isStopping() {
        return state == ClientState.STOPPING;
    }

    public boolean isStopped() {
        return state == ClientState.STOPPED;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public SmppServerSession getServerSession() {
        return serverSession;
    }

    public void setServerSession(SmppServerSession serverSession) {
        this.serverSession = serverSession;
    }

    // getters and setters
    public long getRebindPeriod() {
        return rebindPeriod;
    }

    public void setRebindPeriod(long rebindPeriod) {
        this.rebindPeriod = rebindPeriod;
    }

    public ExecutorService getPool() {
        return pool;
    }

    public void setPool(ExecutorService pool) {
        this.pool = pool;
    }

    public DateTimeZone getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(DateTimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public long getElinkPeriod() {
        return elinkPeriod;
    }

    public void setElinkPeriod(long elinkPeriod) {
        this.elinkPeriod = elinkPeriod;
    }

    public long getResendPeriod() {
        return resendPeriod;
    }

    public void setResendPeriod(long resendPeriod) {
        this.resendPeriod = resendPeriod;
    }

    public SmppClient getSmppClient() {
        return smppClient;
    }

    public void setSmppClient(SmppClient smppClient) {
        this.smppClient = smppClient;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public void setRateLimiter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    public SmppSessionConfiguration getCfg() {
        return cfg;
    }

    public void setCfg(SmppSessionConfiguration cfg) {
        this.cfg = cfg;
    }

    public SmppSessionHandler getSessionHandler() {
        return sessionHandler;
    }

    public void setSessionHandler(SmppSessionHandler sessionHandler) {
        this.sessionHandler = sessionHandler;
    }

    public SmppSession getSession() {
        return session;
    }

    public void setSession(SmppSession session) {
        this.session = session;
    }

    public ScheduledExecutorService getTimer() {
        return timer;
    }

    public void setTimer(ScheduledExecutorService timer) {
        this.timer = timer;
    }

    public String toStringConnectionParams() {
        return connectionString;
    }

    public String toStringShortConnectionParams() {
        return shortConnectonString;
    }

    public long getLastSendMillis() {
        return lastSendMillis;
    }

    public void setLastSendMillis(long lastSendMillis) {
        this.lastSendMillis = lastSendMillis;
    }

    public void sendRequestPdu(PduRequest pduRequest, long toMillis, boolean async) throws RecoverablePduException, UnrecoverablePduException, SmppTimeoutException, SmppChannelException, InterruptedException {
        //session.get

        session.sendRequestPdu(pduRequest, toMillis, async);

        this.lastSendMillis = System.currentTimeMillis();
    }

    public void sendResponsePdu(PduResponse pduResponse) throws RecoverablePduException, UnrecoverablePduException, SmppChannelException, InterruptedException {
        session.sendResponsePdu(pduResponse);

        this.lastSendMillis = System.currentTimeMillis();
    }

    public void setServerSessionHandler(SmppServerSessionHandler handler) {
        this.serverSessionHandler = handler;
    }

    public String getName() {
        return name;
    }
}
