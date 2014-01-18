package com.a1systems.http.adapter;

import com.a1systems.http.adapter.message.Message;
import com.a1systems.http.adapter.message.MessagePart;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Application {
    protected ConcurrentHashMap<Long, Message> messages = new ConcurrentHashMap<Long, Message>();

    protected ConcurrentHashMap<Long, MessagePart> messageParts = new ConcurrentHashMap<Long, MessagePart>();

    protected ConcurrentHashMap<String, Long> linkIds = new ConcurrentHashMap<String, Long>();

    protected ExecutorService sendPool;

    protected IdGenerator messageIdGenerator;
    protected IdGenerator partIdGenerator;

    public Application() {
        this.sendPool = Executors.newFixedThreadPool(10);

        this.messageIdGenerator = new IdGenerator(null, 10L);
        this.partIdGenerator = new IdGenerator(null, 10L);
    }

    public String sendMessage(String source, String destination, String message, String encoding) {
        Message msg = new Message(partIdGenerator, source, destination, message, encoding);

        msg.setId(messageIdGenerator.generate());

        return msg.toString();
    }

}
