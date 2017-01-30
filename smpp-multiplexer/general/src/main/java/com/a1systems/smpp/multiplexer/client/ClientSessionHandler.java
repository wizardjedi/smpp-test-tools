package com.a1systems.smpp.multiplexer.client;

import com.a1systems.smpp.multiplexer.Application;
import com.a1systems.smpp.multiplexer.server.SmppServerSessionHandler;
import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.EnquireLinkResp;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.QuerySmResp;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import io.netty.channel.ConnectTimeoutException;
import java.net.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientSessionHandler extends DefaultSmppSessionHandler {
    public static final Logger logger = LoggerFactory.getLogger(ClientSessionHandler.class);

    protected SmppServerSessionHandler serverHandler;

    protected Client client;

    public ClientSessionHandler(SmppServerSessionHandler serverHandler, Client client) {
        this.serverHandler = serverHandler;
        this.client = client;
    }

    @Override
    public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
        PduResponse response = pduAsyncResponse.getResponse();

        if (response instanceof SubmitSmResp) {
            PduRequest req = pduAsyncResponse.getRequest();

            serverHandler.processSubmitSmResp(req, (SubmitSmResp)response);

            return ;
        }
        
        if (response instanceof QuerySmResp) {
            PduRequest req = pduAsyncResponse.getRequest();

            serverHandler.processQuerySmResp(req, (QuerySmResp)response);

            return ;
        }

        if (response instanceof EnquireLinkResp) {
            logger.error("{} elink_resp", client.toStringConnectionParams());

            return ;
        }

        super.fireExpectedPduResponseReceived(pduAsyncResponse);
    }

    @Override
    public PduResponse firePduRequestReceived(PduRequest pduRequest) {
        if (pduRequest instanceof DeliverSm) {
            RouteInfo ri = new RouteInfo();
            ri.setClient(client);
            ri.setOutputSequenceNumber(pduRequest.getSequenceNumber());

            pduRequest.setReferenceObject(ri);

            if (client.isActive()) {
                serverHandler.processDeliverSm((DeliverSm)pduRequest);
            } else {
                client.addToQueue(pduRequest);
            }

            return null;
        }

        return super.firePduRequestReceived(pduRequest);
    }

    @Override
    public void firePduRequestExpired(PduRequest pduRequest) {
        if (pduRequest instanceof EnquireLink) {
            logger.error("{} No response to elink. Session dropped.", client.toStringConnectionParams());

            fireChannelUnexpectedlyClosed();
        } else {
            logger
                .error(
                    "{} no resp for pdu.seq_num:{}",
                    client.toStringConnectionParams(),
                    pduRequest.getSequenceNumber()
                );
        }
    }



    @Override
    public void fireChannelUnexpectedlyClosed() {
        logger.error("{} Session unexpectedly closed.", client.toStringConnectionParams());

        SmppSession session = client.getSession();
        
        if (session != null) {
            session.destroy();
        }
        
        client.bind();
    }

    @Override
    public void fireUnknownThrowable(Throwable t) {
        if (t instanceof ConnectTimeoutException) {
            Application.ConnectionEndpoint endpoint = client.getEndpoint();
            
            if (endpoint != null) {
                endpoint.markLastFailedConnection();
                
                logger
                    .error(
                        "{} Couldnot connect with timeout. Mark endpoint {} with failed connection.", 
                        client.toStringConnectionParams(),
                        endpoint
                    );
            }
        } else {
            if (t instanceof ConnectException) {
                Application.ConnectionEndpoint endpoint = client.getEndpoint();
                
                logger
                    .error(
                        "{} Connection problem. Mark endpoint {} is unreachable.",
                        client.toStringConnectionParams(),
                        endpoint
                    );
                
                endpoint.markUnreachable();
            } else {
                super.fireUnknownThrowable(t);
            }
        }
    }
}
