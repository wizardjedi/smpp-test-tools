package com.a1systems.smpp.multiplexer.client;

import com.a1systems.smpp.multiplexer.client.ClientState;
import com.cloudhopper.smpp.SmppClient;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.google.common.util.concurrent.RateLimiter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {

    public static final Logger log = LoggerFactory.getLogger(Client.class);

    protected SmppSessionConfiguration cfg;
    protected SmppSessionHandler sessionHandler;
    protected ClientState state;

    protected volatile SmppSession session;

    protected SmppServerSession serverSession;

    protected boolean hidden = false;

    protected SmppClient smppClient;

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

    public Client(SmppSessionConfiguration cfg) {
        this(cfg, null);
    }

    public Client(SmppSessionConfiguration cfg, SmppClient smppClient) {
        this.cfg = cfg;

        this.smppClient = smppClient;

        this.state = ClientState.IDLE;
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
        //this.elinkTask = this.timer.scheduleAtFixedRate(new ElinkTask(this), getElinkPeriod(), getElinkPeriod(), TimeUnit.SECONDS);
    }

    public void bind() {
        if (this.state == ClientState.BOUND
                || this.state == ClientState.IDLE) {
            log.debug("Binding state");

            if (this.session != null
                    && this.session.isBound()) {
                this.session.close();
                this.session.destroy();
                this.session = null;
            }

            this.state = ClientState.BINDING;

            if (elinkTask != null) {
                this.elinkTask.cancel(true);
            }
            runRebindTask();
        }
    }

    public void bound(SmppSession session) {
        if (this.state == ClientState.BINDING) {
            log.debug("Bound state");

            this.state = ClientState.BOUND;

            this.session = session;

            if (rebindTask != null) {
                this.rebindTask.cancel(true);
            }
            runElinkTask();
        }
    }

    public void stop() {
        log.debug("Stopping");

        this.state = ClientState.STOPPING;

        if (this.elinkTask != null) {
            this.elinkTask.cancel(true);
        }

        if (this.rebindTask != null) {
            this.rebindTask.cancel(true);
        }

        if (session != null) {
            session.unbind(TimeUnit.SECONDS.toMillis(30));
            session.close();
        }
        //session.destroy();

        /*
        if (timer != null){
            this.timer.shutdown();

            this.timer = null;
        }

        if (this.smppClient != null) {
            this.smppClient.destroy();
        }*/
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
        return cfg.getHost()+":"+cfg.getPort()+":["+cfg.getSystemId()+":"+cfg.getPassword()+"]";
    }

    public String toStringShortConnectionParams() {
        return cfg.getHost()+":"+cfg.getPort();
    }
}
