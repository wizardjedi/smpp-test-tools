package com.a1systems.smpp.multiplexer.server;

import com.a1systems.smpp.multiplexer.client.Client;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.codahale.metrics.MetricRegistry;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputSender implements Runnable {

    protected Client client;
    protected SmppServerSession serverSession;
    protected Pdu pdu;
    protected long processingStartMillis;
    protected MetricRegistry registry;
    
    public static Logger logger = LoggerFactory.getLogger(OutputSender.class);

    public OutputSender(Client c, SmppServerSession session, Pdu ssm, long start, MetricRegistry registry) {
        this.client = c;
        this.serverSession = session;
        this.pdu = ssm;
        this.processingStartMillis = start;
        this.registry = registry;
    }

    @Override
    public void run() {
        long curMillis = System.currentTimeMillis();

        if (
            (
                curMillis - processingStartMillis < 20000
                && pdu instanceof PduRequest
            )
            || pdu instanceof PduResponse
        ) {
            if (client.isBound()) {
                try {
                    if (pdu instanceof PduRequest) {
                        if (client.getSession().getSendWindow().getFreeSize() > 0) {
                            client.sendRequestPdu((PduRequest) pdu, TimeUnit.SECONDS.toMillis(20), false);
                        } else {
                            logger
                                .info(
                                    "Window full for {} discard output pdu.seq_num:{} type:{}",
                                    client.getName(),
                                    pdu.getSequenceNumber(),
                                    (pdu.isRequest() ? "req" : "resp")
                                );
                        }
                    } else {
                        DeliverSmResp dsmr = (DeliverSmResp) pdu;

                        client.sendResponsePdu((PduResponse) pdu);
                    }

                    logger
                        .info(
                            "Processed by {} output pdu.seq_num:{} type:{} processing took:{}",
                            client.getName(),
                            pdu.getSequenceNumber(),
                            (pdu.isRequest() ? "req" : "resp"),
                            System.currentTimeMillis() - processingStartMillis
                        );
                } catch (Exception ex) {
                    StringWriter sw = new StringWriter();

                    PrintWriter pw = new PrintWriter(sw, true);

                    ex.printStackTrace(pw);

                    logger
                        .error(
                            "outputsender.error pdu:{} {} {} {}", 
                            pdu, 
                            ex.getMessage(), 
                            ex.getCause(), 
                            sw.toString()
                        );

                    pw.close();
                    try {
                        sw.close();
                    } catch (IOException e) {
                        logger.error("IOException on get stack trace");
                    }
                    
                    MetricsHelper.outputSenderException(registry);
                }
            } else {
                logger
                    .error(
                        "{} no session to send pdu.seq_num:{}",
                        client.toStringConnectionParams(), pdu.getSequenceNumber()
                    );
                
                MetricsHelper.outputSenderNoSession(registry);
            }
        } else {
            logger
                .error(
                    "outputsender {} processing took too long:{} to process pdu.seq_num:{}",
                    client.toStringConnectionParams(), curMillis - processingStartMillis, pdu.getSequenceNumber()
                );
            
            MetricsHelper.outputSenderProcessingTooLong(registry);
        }
    }

}
