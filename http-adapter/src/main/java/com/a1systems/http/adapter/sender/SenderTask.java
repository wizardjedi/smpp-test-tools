package com.a1systems.http.adapter.sender;

import com.a1systems.http.adapter.Application;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
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
        logger.trace("Starting sender task");

        try {
            while (true) {
                SessionHolder holder = application.getMessagePart();

                if (holder != null) {
                    try {
                        SubmitSm ssm = holder.getPart().createSubmitSm();

                        logger.info("DU:{} Prepare to send {}", holder.getPart().getId(), ssm);

                        holder.getPart().setTryCount((byte) (holder.getPart().getTryCount()+1));

                        if (
                            holder.getSession() != null
                            && holder.getSession().isBound()
                        ) {
                            holder.getSession().sendRequestPdu(ssm, TimeUnit.SECONDS.toMillis(60), false);
                        } else {
                            logger.error("DU:{} Session is not in BOUND state", holder.getPart().getId());

                            if (holder.getPart().getTryCount() < 10) {
                                logger.error("DU:{} Retry message in {} seconds", holder.getPart().getId(), holder.getClient().getResendPeriod());

                                DateTime newSendDate = DateTime.now().plusSeconds((int)holder.getClient().getResendPeriod());

                                holder.getPart().setSendDate(newSendDate);

                                holder.getClient().addToQueue(holder.getPart());
                            } else {
                                logger.error("DU:{} No more resends", holder.getPart().getId());
                            }
                        }
                    } catch (RecoverablePduException | UnrecoverablePduException | SmppTimeoutException | SmppChannelException | InterruptedException ex) {
                        logger.error("{}", ex);
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("{}", ex);
        }

        return null;
    }
}
