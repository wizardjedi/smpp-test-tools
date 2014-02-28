package com.a1systems.smpp.multiplexer.client;

import com.a1systems.smpp.multiplexer.task.SenderRespTask;
import com.a1systems.smpp.multiplexer.task.SenderTask;
import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientSessionHandler extends DefaultSmppSessionHandler {

    protected Client client;
    
    public ClientSessionHandler(Client client) {
        this.client = client;
    }
    
    @Override
    public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
        if (pduAsyncResponse.getResponse() instanceof SubmitSmResp) {
            ExecutorService pool = client.getPool();

            pool.submit(new SenderRespTask((SubmitSmResp)pduAsyncResponse.getResponse(), client.getServerSession()));
        }
    }

    @Override
    public PduResponse firePduRequestReceived(PduRequest pduRequest) {
        if (pduRequest instanceof DeliverSm) {
            ExecutorService pool = client.getPool();

            pool.submit(new SenderRespTask(pduRequest, client.getServerSession()));
            
            return null;
        } else {
            return super.firePduRequestReceived(pduRequest);
        }
    }

    @Override
    public void fireChannelUnexpectedlyClosed() {
        client.bind();
    }
}
