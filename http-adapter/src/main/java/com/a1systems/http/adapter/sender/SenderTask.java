package com.a1systems.http.adapter.sender;

import com.a1systems.http.adapter.Application;
import com.a1systems.http.adapter.message.MessagePart;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

public class SenderTask implements Callable<Object> {
    protected static final Logger logger = LoggerFactory.getLogger(SenderTask.class);

    protected Application application;

    public SenderTask(Application application) {
        this.application = application;
    }

    @Override
    public Object call() throws Exception {
        while (true) {
            SessionHolder holder = application.getMessagePart();

            if (holder != null) {
                try {
                    SubmitSm ssm = holder.getPart().createSubmitSm();

                    logger.error("Prepare to send {}", ssm);

                    holder.getPart().setTryCount((byte) (holder.getPart().getTryCount()+1));

                    holder.getSession().sendRequestPdu(ssm, TimeUnit.SECONDS.toMillis(60), false);
                } catch (RecoverablePduException | UnrecoverablePduException | SmppTimeoutException | SmppChannelException | InterruptedException ex) {
                    logger.error("{}", ex);
                }
            }
        }
    }
}
