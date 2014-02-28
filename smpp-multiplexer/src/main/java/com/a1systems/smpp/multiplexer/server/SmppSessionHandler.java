package com.a1systems.smpp.multiplexer.server;

import com.a1systems.smpp.multiplexer.task.SenderTask;
import com.cloudhopper.commons.gsm.GsmUtil;
import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.util.SmppUtil;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmppSessionHandler extends DefaultSmppSessionHandler {

    public static final Logger logger = LoggerFactory.getLogger(SmppSessionHandler.class);
    
    private WeakReference<SmppSession> sessionRef;

    protected ExecutorService pool;
    
    protected SmppServerHandlerImpl handler;
    
    protected ConcurrentHashMap<String, ConcatinatedMessageLinkRoutingInfo> concatinatedMessagesRouting = new ConcurrentHashMap<String, ConcatinatedMessageLinkRoutingInfo>();
    
    public SmppSessionHandler(SmppSession session, ExecutorService pool, SmppServerHandlerImpl handler) {
        this.sessionRef = new WeakReference<SmppSession>(session);
        
        this.handler = handler;
        
        this.pool = pool;
    }

    @Override
    public PduResponse firePduRequestReceived(PduRequest pduRequest) {

        if (pduRequest instanceof EnquireLink) {
            return pduRequest.createResponse();
        }

        if (pduRequest instanceof SubmitSm) {
            SubmitSm ssm = (SubmitSm) pduRequest;
            
            SmppSession clientSession = null;
            
            if (SmppUtil.isUserDataHeaderIndicatorEnabled(ssm.getEsmClass())) {
                byte[] udh = GsmUtil.getShortMessageUserDataHeader(ssm.getShortMessage());

                logger.debug("UDH:{}", udh);
                
                long smsId = ServerUtil.getSmsId(udh);
                
                logger.trace("SMSID:{}", smsId);
                
                long parts = ServerUtil.getParts(udh);
                
                logger.trace("PARTS:{}", parts);
                
                String key = ConcatinatedMessageLinkRoutingInfo.createKey(ssm.getDestAddress().getAddress(), ssm.getSourceAddress().getAddress(), smsId, parts);
                
                logger.debug("Concat msg key:{}", key);
                
                ConcatinatedMessageLinkRoutingInfo routingInfo;
                
                if (concatinatedMessagesRouting.containsKey(key)) {
                    routingInfo = concatinatedMessagesRouting.get(key);
                } else {
                    ConcatinatedMessageLinkRoutingInfo info = new ConcatinatedMessageLinkRoutingInfo();
                    info.setCreateDate(DateTime.now());
                    info.setSession(null);
                    
                    routingInfo = concatinatedMessagesRouting.putIfAbsent(key, info);
                }
                
                logger.debug("Concat map:{}", concatinatedMessagesRouting);
                
                //clientSession = routingInfo.getSession();
            }
            
            pool.submit(new SenderTask(ssm, handler.getClients().get(0)));
        }

        return null;
    }

    @Override
    public void fireExpectedPduResponseReceived(PduAsyncResponse pdu) {
        if (pdu instanceof DeliverSmResp) {
            pool.submit(new SenderTask(pdu.getResponse(), handler.getClients().get(0)));
        }
    }

    
    
}
