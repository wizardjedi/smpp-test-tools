package com.a1systems.http.adapter.message;

public enum PartState {
    ENROUTE(1), 
    DELIVERED(2), 
    EXPIRED(3), 
    DELETED(4), 
    UNDELIVERABLE(5), 
    ACCEPTED(6), 
    UNKNOWN(7), 
    REJECTED(8),
    QUEUED(9), 
    NOT_SENT(10), 
    SENDING(11);

    protected byte state;
    
    PartState(int i) {
        state = (byte)state;
    }
    
    public static PartState valueOf(byte state) {
        return PartState.values()[state-1];
    }
}

