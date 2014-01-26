package com.a1systems.http.adapter.dao;

import com.a1systems.http.adapter.message.Message;
import com.a1systems.http.adapter.message.MessagePart;

public interface Dao {
    public MessagePart getMessagePartById(Long id);
    public MessagePart getMessagePartBySMSCId(String id);
    public Message getMessageById();

    public Message saveMessage(Message message);
    public MessagePart saveMessagePart(MessagePart messagePart);
    public MessagePart saveSMSCId(MessagePart messagePart);
}
