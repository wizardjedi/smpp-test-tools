package com.a1systems.smpp.multiplexer.client;

import org.joda.time.DateTime;

public class RouteInfo {
    protected long inputSequenceNumber;
    protected long outputSequenceNumber;

    protected Client client;

    protected String msgId;

    protected DateTime createDate;

    public long getInputSequenceNumber() {
        return inputSequenceNumber;
    }

    public void setInputSequenceNumber(long inputSequenceNumber) {
        this.inputSequenceNumber = inputSequenceNumber;
    }

    public long getOutputSequenceNumber() {
        return outputSequenceNumber;
    }

    public void setOutputSequenceNumber(long outputSequenceNumber) {
        this.outputSequenceNumber = outputSequenceNumber;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public DateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(DateTime createDate) {
        this.createDate = createDate;
    }

    @Override
    public String toString() {
        return "RouteInfo{" + "inputSequenceNumber=" + inputSequenceNumber + ", outputSequenceNumber=" + outputSequenceNumber + ", client=" + client + ", msgId=" + msgId + ", createDate=" + createDate + '}';
    }

}
