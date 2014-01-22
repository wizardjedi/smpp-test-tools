package com.a1systems.http.adapter.message;

import com.cloudhopper.commons.util.HexUtil;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class MessagePart implements Delayed{
    protected Long id;
    protected String source;
    protected String destination;
    @JsonIgnore
    protected byte[] shortMessage;
    protected byte sourceTon;
    protected byte sourceNpi;
    protected byte destinationTon;
    protected byte destinationNpi;
    @JsonIgnore
    protected Message message;
    protected String smscId;
    protected PartState state;
    protected byte tryCount = 0;
    protected byte sendTryCount = 0;
    protected int error = 0;

    public int getError() {
        return error;
    }

    public void setError(int error) {
        this.error = error;
    }
    
    public String getSmscId() {
        return smscId;
    }

    public synchronized void setSmscId(String smscId) {
        this.smscId = smscId;
    }

    public Message getMessage() {
        return message;
    }

    public synchronized void setMessage(Message message) {
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public synchronized void setId(Long id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public synchronized void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public synchronized void setDestination(String destination) {
        this.destination = destination;
    }

    public byte[] getShortMessage() {
        return shortMessage;
    }

    public synchronized void setShortMessage(byte[] shortMessage) {
        this.shortMessage = shortMessage;
    }

    public byte getSourceTon() {
        return sourceTon;
    }

    public synchronized void setSourceTon(byte sourceTon) {
        this.sourceTon = sourceTon;
    }

    public byte getSourceNpi() {
        return sourceNpi;
    }

    public synchronized void setSourceNpi(byte sourceNpi) {
        this.sourceNpi = sourceNpi;
    }

    public byte getDestinationTon() {
        return destinationTon;
    }

    public synchronized void setDestinationTon(byte destinationTon) {
        this.destinationTon = destinationTon;
    }

    public byte getDestinationNpi() {
        return destinationNpi;
    }

    public synchronized void setDestinationNpi(byte destinationNpi) {
        this.destinationNpi = destinationNpi;
    }

    public PartState getState() {
        return state;
    }

    public synchronized void setState(PartState state) {
        this.state = state;
    }

    public int getTryCount() {
        return tryCount;
    }

    public synchronized void setTryCount(byte tryCount) {
        this.tryCount = tryCount;
    }

    public synchronized void setSendTryCount(byte sendTryCount) {
        this.sendTryCount = sendTryCount;
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
