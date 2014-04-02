package com.a1systems.smpp.multiplexer.client;

import com.a1systems.smpp.multiplexer.server.SmppServerHandlerImpl;
import com.a1systems.smpp.multiplexer.server.SmppServerSessionHandler;
import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSmResp;

public class ClientSessionHandler extends DefaultSmppSessionHandler {
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

            RouteInfo ri = (RouteInfo)req.getReferenceObject();

            response.setSequenceNumber((int)ri.getInputSequenceNumber());

            serverHandler.processSubmitSmResp((SubmitSmResp)response);

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

            serverHandler.processDeliverSm((DeliverSm)pduRequest);

            return null;
        }

        return super.firePduRequestReceived(pduRequest);
    }

    @Override
    public void fireChannelUnexpectedlyClosed() {
        client.bind();
    }
}
