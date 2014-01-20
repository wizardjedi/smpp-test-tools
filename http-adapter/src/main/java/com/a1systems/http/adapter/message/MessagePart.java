package com.a1systems.http.adapter.message;

import com.cloudhopper.commons.util.HexUtil;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessagePart implements Delayed{
    protected Long id;
    protected String source;
    protected String destination;
    protected byte[] shortMessage;
    protected byte sourceTon;
    protected byte sourceNpi;
    protected byte destinationTon;
    protected byte destinationNpi;
    protected Message message;
    protected String smscId;
    protected PartState state;
    protected int tryCount = 0;

    public String getSmscId() {
        return smscId;
    }

    public void setSmscId(String smscId) {
        this.smscId = smscId;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
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

    public byte[] getShortMessage() {
        return shortMessage;
    }

    public void setShortMessage(byte[] shortMessage) {
        this.shortMessage = shortMessage;
    }

    public byte getSourceTon() {
        return sourceTon;
    }

    public void setSourceTon(byte sourceTon) {
        this.sourceTon = sourceTon;
    }

    public byte getSourceNpi() {
        return sourceNpi;
    }

    public void setSourceNpi(byte sourceNpi) {
        this.sourceNpi = sourceNpi;
    }

    public byte getDestinationTon() {
        return destinationTon;
    }

    public void setDestinationTon(byte destinationTon) {
        this.destinationTon = destinationTon;
    }

    public byte getDestinationNpi() {
        return destinationNpi;
    }

    public void setDestinationNpi(byte destinationNpi) {
        this.destinationNpi = destinationNpi;
    }

    public SubmitSm createSubmitSm() throws SmppInvalidArgumentException {
        SubmitSm submitSm = new SubmitSm();

        submitSm.setShortMessage(shortMessage);
        submitSm.setSourceAddress(new Address(sourceTon, sourceNpi, source));
        submitSm.setDestAddress(new Address(destinationTon, destinationNpi, destination));

        submitSm.setRegisteredDelivery((byte)1);

        submitSm.setReferenceObject(this);

        return submitSm;
    }

    @Override
    public String toString() {
        try {
            return "MessagePart{" + "id=" + id + ", smscId=" + smscId + ", source=" + source + ", destination=" + destination + ", shortMessage=" + HexUtil.toHexString(shortMessage) + ", sourceTon=" + sourceTon + ", sourceNpi=" + sourceNpi + ", destinationTon=" + destinationTon + ", destinationNpi=" + destinationNpi + ", message.id=" + message.getId() + ", ssm["+this.createSubmitSm().toString()+"]" + '}';
        } catch (SmppInvalidArgumentException ex) {
            return ex.toString();
        }
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return -1;
    }

    @Override
    public int compareTo(Delayed o) {
        return -1;
    }
}
