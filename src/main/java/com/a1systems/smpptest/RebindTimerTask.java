package com.a1systems.smpptest;

import com.cloudhopper.smpp.SmppClient;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.type.SmppBindException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import java.util.TimerTask;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

public class RebindTimerTask extends TimerTask {
	protected static final Logger logger = LoggerFactory.getLogger(RebindTimerTask.class);
	protected SessionSupTask sessionTask;

	public RebindTimerTask(SessionSupTask sessionTask) {
		this.sessionTask = sessionTask;
	}

	@Override
	public void run() {
		SmppClient client = sessionTask.getClient();

		if (!sessionTask.isBound()) {
			try {
				logger.debug("Binding");

				SmppSession session = client.bind(sessionTask.getSmppSessionConfiguration(), sessionTask.getSmppSessionHandler());

				sessionTask.setSession(session);

				logger.debug("Bound");
			} catch (SmppTimeoutException ex) {
				logger.error("{}", ex);
			} catch (SmppChannelException ex) {
				logger.error("{}", ex);
			} catch (SmppBindException ex) {
				logger.error("{}", ex);
			} catch (UnrecoverablePduException ex) {
				logger.error("{}", ex);
			} catch (InterruptedException ex) {
				logger.error("{}", ex);
			}
		}
	}

}
