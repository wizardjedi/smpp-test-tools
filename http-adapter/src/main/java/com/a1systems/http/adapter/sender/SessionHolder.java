package com.a1systems.http.adapter.sender;

import com.a1systems.http.adapter.message.MessagePart;
import com.a1systems.http.adapter.smpp.client.Client;
import com.cloudhopper.smpp.SmppSession;

public class SessionHolder {
    protected SmppSession session;
    protected MessagePart part;
    protected Client client;

    public SessionHolder(Client client, SmppSession session, MessagePart part) {
        this.session = session;
        this.part = part;
        this.client = client;
    }

    public Client getClient() {
        return client;
    }

    public SmppSession getSession() {
        return session;
    }

    public MessagePart getPart() {
        return part;
    }
}
