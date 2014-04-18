package com.a1systems.smpp.multiplexer.server;

import com.a1systems.smpp.multiplexer.client.RouteInfo;
import static com.a1systems.smpp.multiplexer.server.SmppServerSessionHandler.logger;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
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
            if (pdu instanceof PduResponse) {
                serverSession.sendResponsePdu((PduResponse) pdu);
            } else {
                serverSession.sendRequestPdu((PduRequest) pdu, TimeUnit.SECONDS.toMillis(300), false);
            }
        } catch (Exception ex) {
            logger.error("{}", ex);
        }
    }

}
