package com.a1systems.smpp.multiplexer.server;

import com.cloudhopper.smpp.SmppSession;
import com.google.common.base.Objects;
import org.joda.time.DateTime;

class ConcatinatedMessageLinkRoutingInfo {
    protected SmppSession session;
    
    protected DateTime createDate;

    public static String createKey(String abonent, String sender, long smscId, long partsCount) {
        return abonent+"_"+sender+"_"+smscId+"_"+partsCount;
    }
    
    public SmppSession getSession() {
        return session;
    }

    public void setSession(SmppSession session) {
        this.session = session;
    }

    public DateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(DateTime createDate) {
        this.createDate = createDate;
    }
    
    public String toString() {
        return 
            Objects
                .toStringHelper(this)
                    .add("session", session)
                    .add("createDate", createDate)
                    .toString();
    }
    
    
}
