package com.a1systems.http.adapter;

import com.a1systems.http.adapter.dao.Dao;
import com.a1systems.http.adapter.message.Message;
import com.a1systems.http.adapter.message.MessagePart;
import com.a1systems.http.adapter.sender.SenderTask;
import com.a1systems.http.adapter.sender.SessionHolder;
import com.a1systems.http.adapter.smpp.client.Client;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class Application {
    protected static final Logger logger = LoggerFactory.getLogger(Application.class);

    protected ConcurrentHashMap<Long, Message> messages = new ConcurrentHashMap<Long, Message>();

    protected ConcurrentHashMap<Long, MessagePart> messageParts = new ConcurrentHashMap<Long, MessagePart>();

    protected ConcurrentHashMap<String, MessagePart> linkIds = new ConcurrentHashMap<String, MessagePart>();

    protected ConcurrentHashMap<String, Client> smppLinks = new ConcurrentHashMap<String, Client>();

    protected ExecutorService sendPool;

    protected ScheduledExecutorService asyncTaskPool;

    protected IdGenerator messageIdGenerator;
    protected IdGenerator partIdGenerator;

    protected AtomicInteger linkNum = new AtomicInteger(0);

    protected List<Client> clients = new ArrayList<Client>();

    protected RateLimiter inputLimiter;

    protected int inputSpeed = 100;

    protected RateLimiter dbSendLimiter;

    @Autowired
    protected Dao dao;

    @Autowired
    protected MetricRegistry metricRegistry;

    @Autowired
    protected ObjectMapper objectMapper;

    public Application() {
        this.sendPool = Executors.newFixedThreadPool(10);

        this.messageIdGenerator = new IdGenerator(null, 10L);
        this.partIdGenerator = new IdGenerator(null, 10L);
    }

    public Dao getDao() {
        return dao;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public RateLimiter getInputLimiter() {
        return inputLimiter;
    }

    public void setInputLimiter(RateLimiter inputLimiter) {
        this.inputLimiter = inputLimiter;
    }

    public ExecutorService getSendPool() {
        return sendPool;
    }

    public void setSendPool(ExecutorService sendPool) {
        this.sendPool = sendPool;
    }

    public ScheduledExecutorService getAsyncTaskPool() {
        return asyncTaskPool;
    }

    public void setAsyncTaskPool(ScheduledExecutorService asyncTaskPool) {
        this.asyncTaskPool = asyncTaskPool;
    }

    public ConcurrentHashMap<String, Client> getSmppLinks() {
        return smppLinks;
    }

    public void setSmppLinks(ConcurrentHashMap<String, Client> smppLinks) {
        this.smppLinks = smppLinks;
    }

    public int getInputSpeed() {
        return inputSpeed;
    }

    public void setInputSpeed(int inputSpeed) {
        this.inputSpeed = inputSpeed;
    }

    public void start() {
        this.sendPool = Executors.newCachedThreadPool();

        int corePoolSize = 10;

        for (int i=0;i<corePoolSize;i++) {
            this.sendPool.submit(new SenderTask(this));
        }

        for (Client client:this.smppLinks.values()) {
            this.clients.add(client);
        }

        this.inputLimiter = RateLimiter.create(this.inputSpeed);

        this.dbSendLimiter = RateLimiter.create(100);

        final JmxReporter reporter = JmxReporter.forRegistry(metricRegistry).build();
        reporter.start();
    }

    public SessionHolder getMessagePart() {
        if (this.smppLinks.size() > 0) {
            int num = this.linkNum.incrementAndGet();

            int linkNumber = num % this.smppLinks.size();

            try {
                Client client = this.clients.get(linkNumber);

                if (client.getRateLimiter().tryAcquire()) {
                    MessagePart part = client.poll();

                    if (part != null) {
                        return new SessionHolder(client, client.getSession(), part);
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                /* */
            }
        }

        return null;
    }

    public String sendMessage(String link, String source, String destination, String message, String encoding) throws JsonProcessingException {
        Message msg = new Message(partIdGenerator, source, destination, message, encoding);

        msg.setCreateDate(new DateTime());

        if (this.inputLimiter.tryAcquire(msg.getParts().size())) {
            msg.setId(messageIdGenerator.generate());

            Client client = this.getSmppLinks().get(link);

            this.messages.put(msg.getId(), msg);

            this.dao.saveMessage(msg);

            for (MessagePart part : msg.getParts()) {
                part.setCreateDate(new DateTime());

                this.messageParts.put(part.getId(), part);

                client.addToQueue(part);
            }

            return objectMapper.writeValueAsString(msg);
        } else {
            return "Too much requests try again later";
        }
    }

    public ConcurrentHashMap<Long, Message> getMessages() {
        return messages;
    }

    public ConcurrentHashMap<Long, MessagePart> getMessageParts() {
        return messageParts;
    }

    public ConcurrentHashMap<String, MessagePart> getLinkIds() {
        return linkIds;
    }

    public synchronized void setSmscId(String messageId, MessagePart part) {
        part.setSmscId(messageId);

        part.setSendDate(new DateTime());

        this.linkIds.put(messageId, part);
    }

}
