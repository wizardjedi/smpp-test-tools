package com.a1systems.smpp.multiplexer.server;

import static com.a1systems.smpp.multiplexer.server.OutputSender.logger;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import org.slf4j.LoggerFactory;

class InputSender implements Runnable {

    protected SmppServerSession serverSession;
    protected Pdu pdu;

    public static org.slf4j.Logger logger = LoggerFactory.getLogger(InputSender.class);

    public InputSender(SmppServerSession session, Pdu pdu) {
        this.serverSession = session;
        this.pdu = pdu;
    }

    @Override
    public void run() {
        try {
            long start = System.currentTimeMillis();
            
            if (pdu instanceof PduResponse) {
                serverSession.sendResponsePdu((PduResponse) pdu);
            } else {
                serverSession.sendRequestPdu((PduRequest) pdu, TimeUnit.SECONDS.toMillis(300), false);
            }
            
            logger
                .info(
                    "Processed {} input pdu.seq_num:{} type:{} processing took:{}", 
                    serverSession.getConfiguration().getName(),
                    pdu.getSequenceNumber(), 
                    (pdu.isRequest() ? "req" : "resp"),
                    System.currentTimeMillis() - start
                );
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            
            PrintWriter pw = new PrintWriter(sw, true);
            
            ex.printStackTrace(pw);
            
            logger.error("inputsender.error pdu:{} {} {} {}", pdu, ex.getMessage(), ex.getCause(), sw.toString());
            
            pw.close();
            try {
                sw.close();
            } catch (IOException e) {
                
            }
            
        }
    }

}
