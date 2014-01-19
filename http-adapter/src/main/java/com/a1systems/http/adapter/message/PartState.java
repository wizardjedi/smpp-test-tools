package com.a1systems.http.adapter.message;

/*    public static final byte STATE_ENROUTE = (byte) 0x01;
    public static final byte STATE_DELIVERED = (byte) 0x02;
    public static final byte STATE_EXPIRED = (byte) 0x03;
    public static final byte STATE_DELETED = (byte) 0x04;
    public static final byte STATE_UNDELIVERABLE = (byte) 0x05;
    public static final byte STATE_ACCEPTED = (byte) 0x06;
    public static final byte STATE_UNKNOWN = (byte) 0x07;
    public static final byte STATE_REJECTED = (byte) 0x08;
*/
public enum PartState {
    QUEUED, NOT_SENT, ENROUTE, DELIVERED, EXPIRED, DELETED, UNDELIVERABLE, ACCEPTED, UNKNOWN, REJECTED;
}

