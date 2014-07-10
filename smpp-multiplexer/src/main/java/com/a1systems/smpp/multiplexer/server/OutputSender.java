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
                long start = System.currentTimeMillis();
                
                if (pdu instanceof PduRequest) {
                    client.sendRequestPdu((PduRequest)pdu, TimeUnit.SECONDS.toMillis(300), false);
                } else {
                    DeliverSmResp dsmr = (DeliverSmResp)pdu;

                    client.sendResponsePdu((PduResponse)pdu);
                }
                
                logger
                    .info(
                        "Processed by {} output pdu.seq_num:{} type:{} processing took:{}", 
                        client.getName(),
                        pdu.getSequenceNumber(),
                        (pdu.isRequest() ? "req" : "resp"),
                        System.currentTimeMillis() - start
                    );
            } catch (Exception ex) {
                logger.error("{}", ex);
            }
        } else {
            logger
                .error(
                    "{} no session to send pdu.seq_num:{}", 
                    client.toStringConnectionParams(), pdu.getSequenceNumber()
                );
        }
    }

}
