package com.a1systems.http.adapter.dao;

import com.a1systems.http.adapter.message.Message;
import com.a1systems.http.adapter.message.MessagePart;

public class NullDao implements Dao {

    @Override
    public MessagePart getMessagePartById(Long id) {
        return null;
    }

    @Override
    public MessagePart getMessagePartBySMSCId(String id) {
        return null;
    }

    @Override
    public Message getMessageById() {
        return null;
    }

    @Override
    public Message saveMessage(Message message) {
        return message;
    }

    @Override
    public MessagePart saveMessagePart(MessagePart messagePart) {
        return messagePart;
    }

    @Override
    public MessagePart saveSMSCId(MessagePart messagePart) {
        return messagePart;
    }

}
