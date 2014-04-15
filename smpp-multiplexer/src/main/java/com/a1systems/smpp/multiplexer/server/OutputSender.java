package com.a1systems.smpp.multiplexer.server;

import com.a1systems.smpp.multiplexer.client.Client;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
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
        if (client.isBound()) {
            try {
                if (pdu instanceof PduRequest) {
                    client.sendRequestPdu((PduRequest)pdu, TimeUnit.SECONDS.toMillis(60), false);
                } else {
                    DeliverSmResp dsmr = (DeliverSmResp)pdu;

                    client.sendResponsePdu((PduResponse)pdu);
                }
            } catch (Exception ex) {
                logger.error("{}", ex);
            }
        }
    }

}
