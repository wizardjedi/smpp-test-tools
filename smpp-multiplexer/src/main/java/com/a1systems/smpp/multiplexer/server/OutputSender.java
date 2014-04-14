package com.a1systems.smpp.multiplexer.server;

import com.a1systems.smpp.multiplexer.client.Client;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OutputSender implements Runnable {
    protected Client client;
    protected SmppServerSession serverSession;
    protected Pdu pdu;

    public static Logger logger = LoggerFactory.getLogger(OutputSender.class);

    public OutputSender(Client c, SmppServerSession session, Pdu ssm) {
        this.client = c;
        this.serverSession = session;
        this.pdu = ssm;

    }

    @Override
    public void run() {
        SmppSession session = client.getSession();

        // Todo: check is it needed?
        // pdu.removeSequenceNumber();

        if (session != null && session.isBound()) {
            try {
                if (pdu instanceof PduRequest) {
                    session.sendRequestPdu((PduRequest)pdu, TimeUnit.SECONDS.toMillis(60), false);
                } else {
                    DeliverSmResp dsmr = (DeliverSmResp)pdu;

                    session.sendResponsePdu((PduResponse)pdu);
                }
            } catch (Exception ex) {
                logger.error("{}", ex);
            }
        }
    }

}
