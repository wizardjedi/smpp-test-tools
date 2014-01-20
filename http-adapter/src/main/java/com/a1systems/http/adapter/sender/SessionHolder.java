package com.a1systems.http.adapter.sender;

import com.a1systems.http.adapter.message.MessagePart;
import com.cloudhopper.smpp.SmppSession;

public class SessionHolder {
    protected SmppSession session;
    protected MessagePart part;

    public SessionHolder(SmppSession session, MessagePart part) {
        this.session = session;
        this.part = part;
    }

    public SmppSession getSession() {
        return session;
    }

    public MessagePart getPart() {
        return part;
    }
}
