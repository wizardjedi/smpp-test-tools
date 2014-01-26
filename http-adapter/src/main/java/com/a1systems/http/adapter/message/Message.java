package com.a1systems.http.adapter.message;

import com.a1systems.http.adapter.IdGenerator;
import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.commons.gsm.GsmUtil;
import com.cloudhopper.commons.util.RandomUtil;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.joda.time.DateTime;

public class Message {
    protected Long id;
    protected String source;
    protected String destination;
    protected String encoding;
    protected Set<MessagePart> parts = new HashSet<MessagePart>();
    protected int partsCount;
    protected DateTime createDate;
    protected DateTime sendDate;
    protected DateTime deliveryReceiptDate;
    protected String message;

    public DateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(DateTime createDate) {
        this.createDate = createDate;
    }

    public DateTime getSendDate() {
        return sendDate;
    }

    public void setSendDate(DateTime sendDate) {
        this.sendDate = sendDate;
    }

    public DateTime getDeliveryReceiptDate() {
        return deliveryReceiptDate;
    }

    public void setDeliveryReceiptDate(DateTime deliveryReceiptDate) {
        this.deliveryReceiptDate = deliveryReceiptDate;
    }

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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Message(IdGenerator partIdGenerator,  String source, String destination, String message, String encoding) {
        this.source = source;
        this.destination = destination;
        this.encoding = encoding;
        this.message = message;

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
