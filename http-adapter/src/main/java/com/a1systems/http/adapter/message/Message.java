package com.a1systems.http.adapter.message;

import com.a1systems.http.adapter.IdGenerator;
import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.commons.gsm.GsmUtil;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Message {
    protected Long id;
    protected String source;
    protected String destination;
    protected String encoding;
    protected Set<MessagePart> parts = new HashSet<MessagePart>();
    protected int partsCount;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public Set<MessagePart> getParts() {
        return parts;
    }

    public void setParts(Set<MessagePart> parts) {
        this.parts = parts;
    }

    public Message(IdGenerator partIdGenerator, String source, String destination, String message, String encoding) {
        this.source = source;
        this.destination = destination;
        this.encoding = encoding;

        byte[] encodedText = CharsetUtil.encode(message, this.encoding);

        byte[][] msgParts;

        if (encodedText.length > 140) {
            Random random = new Random();

            msgParts = GsmUtil.createConcatenatedBinaryShortMessages(encodedText, (byte)12);
        } else {
            msgParts = new byte[][]{encodedText};
        }
        this.partsCount = msgParts.length;

        for (int i=0;i<msgParts.length;i++) {
            MessagePart part = new MessagePart();

            part.setId(partIdGenerator.generate());

            part.setShortMessage(msgParts[i]);

            part.setDestination(this.destination);
            part.setDestinationTon((byte)1);
            part.setDestinationNpi((byte)1);

            part.setSource(this.source);
            part.setSourceTon((byte)1);
            part.setSourceNpi((byte)1);

            part.setMessage(this);

            this.parts.add(part);
        }
    }

    @Override
    public String toString() {
        return "Message{" + "id=" + id + ", source=" + source + ", destination=" + destination + ", encoding=" + encoding + ", parts=" + parts + ", partsCount=" + partsCount + '}';
    }

}
