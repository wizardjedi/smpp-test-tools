package com.a1systems.smpp.multiplexer.server;

import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.codahale.metrics.MetricRegistry;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import org.slf4j.LoggerFactory;

class InputSender implements Runnable {

    protected SmppServerSession serverSession;
    protected Pdu pdu;
    protected long processingStartMillis;
    protected MetricRegistry registry;

    public static org.slf4j.Logger logger = LoggerFactory.getLogger(InputSender.class);

    public InputSender(SmppServerSession session, Pdu pdu, long start, MetricRegistry registry) {
        this.serverSession = session;
        this.pdu = pdu;
        this.processingStartMillis = start;
        this.registry = registry;
    }

    @Override
    public void run() {
        long curMillis = System.currentTimeMillis();

        if ((curMillis - processingStartMillis < 20000
            && pdu instanceof PduRequest)
            || pdu instanceof PduResponse) {
            try {
                long start = System.currentTimeMillis();

                if (serverSession != null) {
                    if (pdu instanceof PduResponse) {
                        serverSession.sendResponsePdu((PduResponse) pdu);
                        
                        MetricsHelper.inputSenderResponseSuccess(registry);
                    } else {
                        if (serverSession.getSendWindow().getFreeSize() > 0) {
                            serverSession.sendRequestPdu((PduRequest) pdu, TimeUnit.SECONDS.toMillis(20), false);
                            
                            MetricsHelper.inputSenderRequestSuccess(registry);
                        } else {
                            logger
                                .info(
                                    "Window full for {} discard input pdu.seq_num:{} type:{}",
                                    serverSession.getConfiguration().getName(),
                                    pdu.getSequenceNumber(),
                                    (pdu.isRequest() ? "req" : "resp")
                                );
                            
                            MetricsHelper.inputSenderQueueFull(registry);
                        }
                    }

                    logger
                        .info(
                            "Processed {} input pdu.seq_num:{} type:{} processing took:{}",
                            serverSession.getConfiguration().getName(),
                            pdu.getSequenceNumber(),
                            (pdu.isRequest() ? "req" : "resp"),
                            System.currentTimeMillis() - start
                        );
                } else {
                    logger
                        .error(
                            "Session:{} Input sender error. Server session is null. Could not send pdu.seq_num:{}",
                            serverSession.getConfiguration().getName(),
                            pdu.getSequenceNumber()
                        );
                    
                    MetricsHelper.inputSenderNoSession(registry);
                }
            } catch (Exception ex) {
                StringWriter sw = new StringWriter();

                PrintWriter pw = new PrintWriter(sw, true);

                ex.printStackTrace(pw);

                logger.error("inputsender.error pdu:{} {} {} {}", pdu, ex.getMessage(), ex.getCause(), sw.toString());

                pw.close();
                try {
                    sw.close();
                } catch (IOException e) {
                    logger.error("IOException on get stack trace");
                }
                
                MetricsHelper.inputSenderException(registry);
            }
        } else {
            logger
                .error(
                    "inputsender {} processing took too long:{} to process pdu.seq_num:{}",
                    serverSession.getConfiguration().getName(),
                    curMillis - processingStartMillis,
                    pdu.getSequenceNumber()
                );
            
            MetricsHelper.inputSenderProcessingTooLong(registry);
        }
    }

}
