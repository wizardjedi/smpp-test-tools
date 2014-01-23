package com.a1systems.http.adapter.smpp.client;

import com.a1systems.http.adapter.message.MessagePart;
import com.cloudhopper.smpp.SmppClient;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.codahale.metrics.CachedGauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.RateLimiter;
import com.codahale.metrics.Gauge;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class Client {

    @Autowired
    protected MetricRegistry metricRegistry;

    public static final Logger log = LoggerFactory.getLogger(Client.class);

    protected SmppSessionConfiguration cfg;
    protected SmppSessionHandler sessionHandler;
    protected ClientState state;

    protected volatile SmppSession session;

    protected SmppClient smppClient;

    protected ScheduledExecutorService timer;

    protected ScheduledFuture<?> elinkTask;
    protected ScheduledFuture<?> rebindTask;

    protected long rebindPeriod = 5;
    protected long elinkPeriod = 5;

    protected int speed = 30;
    protected RateLimiter rateLimiter;

    protected DelayQueue<MessagePart> queue;

    protected DateTimeZone timeZone = DateTimeZone.getDefault();

    public Client(SmppSessionConfiguration cfg) {
        this.cfg = cfg;

        this.state = ClientState.IDLE;

        this.queue = new DelayQueue<MessagePart>();
    }

    public void start() {
        log.debug("Starting client");

        this.smppClient = new DefaultSmppClient();

        this.rateLimiter = RateLimiter.create(this.speed);

        if (this.timer == null) {
            this.timer = Executors.newScheduledThreadPool(2);
        }

        Gauge<Integer> g = new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return queue.size();
            }

        };

        metricRegistry.register("gauge", (Metric)g);

        this.bind();
    }

    public MessagePart poll() {
        return queue.poll();
    }

    public int getQueueSize() {
        return this.queue.size();
    }

    private void runRebindTask() {
        this.rebindTask = this.timer.scheduleAtFixedRate(new RebindTask(this), 0, getRebindPeriod(), TimeUnit.SECONDS);
    }

    private void runElinkTask() {
        this.elinkTask = this.timer.scheduleAtFixedRate(new ElinkTask(this), getElinkPeriod(), getElinkPeriod(), TimeUnit.SECONDS);
    }

    public void bind() {
        if (this.state == ClientState.BOUND
                || this.state == ClientState.IDLE) {
            log.debug("Binding state");

            if (this.session != null
                    && this.session.isBound()) {
                this.session.close();
                this.session.destroy();
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

        this.elinkTask.cancel(true);
        this.rebindTask.cancel(true);
        this.timer.shutdown();

        this.timer = null;
    }

    // getters and setters
    public long getRebindPeriod() {
        return rebindPeriod;
    }

    public void setRebindPeriod(long rebindPeriod) {
        this.rebindPeriod = rebindPeriod;
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

    public void addToQueue(MessagePart part) {
        this.queue.add(part);
    }

}
