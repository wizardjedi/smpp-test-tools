package com.a1systems.http.adapter;

import com.a1systems.http.adapter.message.Message;
import com.a1systems.http.adapter.message.MessagePart;
import com.a1systems.http.adapter.smpp.client.Client;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Application {

    protected ConcurrentHashMap<Long, Message> messages = new ConcurrentHashMap<Long, Message>();

    protected ConcurrentHashMap<Long, MessagePart> messageParts = new ConcurrentHashMap<Long, MessagePart>();

    protected ConcurrentHashMap<String, MessagePart> linkIds = new ConcurrentHashMap<String, MessagePart>();

    protected ConcurrentHashMap<String, Client> smppLinks = new ConcurrentHashMap<String, Client>();

    protected ExecutorService sendPool;

    protected ScheduledExecutorService asyncTaskPool;

    protected IdGenerator messageIdGenerator;
    protected IdGenerator partIdGenerator;

    public Application() {
        this.sendPool = Executors.newFixedThreadPool(10);

        this.messageIdGenerator = new IdGenerator(null, 10L);
        this.partIdGenerator = new IdGenerator(null, 10L);
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

    public String sendMessage(String link, String source, String destination, String message, String encoding) {
        Message msg = new Message(partIdGenerator, source, destination, message, encoding);

        msg.setId(messageIdGenerator.generate());

        Client client = this.getSmppLinks().get(link);

        SmppSession session = client.getSession();

        this.messages.put(msg.getId(), msg);

        try {
            for (MessagePart part : msg.getParts()) {
                this.messageParts.put(part.getId(), part);

                session.sendRequestPdu(part.createSubmitSm(), TimeUnit.SECONDS.toMillis(60), false);
            }
        } catch (RecoverablePduException | UnrecoverablePduException | SmppTimeoutException | SmppChannelException | InterruptedException ex) {
            return ex.toString();
        }

        return msg.toString();
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

    public void setSmscId(String messageId, MessagePart part) {
        part.setSmscId(messageId);

        this.linkIds.put(messageId, part);
    }

}
