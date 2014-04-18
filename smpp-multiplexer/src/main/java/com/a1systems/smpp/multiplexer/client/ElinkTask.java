package com.a1systems.smpp.multiplexer.client;

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import java.util.concurrent.TimeUnit;
import org.slf4j.LoggerFactory;

public class ElinkTask implements Runnable {

    public static final org.slf4j.Logger log = LoggerFactory.getLogger(ElinkTask.class);

    protected Client client;

    public ElinkTask(Client client) {
        this.client = client;
    }

    @Override
    public void run() {
        if (
            client.isBound()
            && System.currentTimeMillis() - client.getLastSendMillis() > TimeUnit.SECONDS.toMillis(client.getElinkPeriod())
        ) {
            SmppSession session = client.getSession();

            log.debug("{} Send elink", client.toStringConnectionParams());
            try {
                session.sendRequestPdu(new EnquireLink(), TimeUnit.SECONDS.toMillis(10), false);
            } catch (RecoverablePduException ex) {
                log.debug("{}", ex);
            } catch (UnrecoverablePduException ex) {
                log.debug("{}", ex);
            } catch (SmppTimeoutException ex) {
                log.debug("{}", ex);
            } catch (SmppChannelException ex) {
                log.debug("{}", ex);
            } catch (InterruptedException ex) {
                log.debug("{}", ex);
            }

        }
    }

}
