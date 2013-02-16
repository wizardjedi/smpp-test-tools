package com.a1systems.smpptest;

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EnquireLinkTask extends TimerTask {
	protected SessionSupTask sessionTask;
	protected static final Logger logger = LoggerFactory.getLogger(EnquireLinkTask.class);
	protected long lastElinkTimeMillis = 0;
	
	public EnquireLinkTask(SessionSupTask sessionTask) {
		this.sessionTask = sessionTask;
	}

	@Override
	public void run() {
		Config config = sessionTask.getConfig();
		
		long currentTimeMillis = System.currentTimeMillis();
		
		if (
			config.isElinkNoTransmit() 
			&& lastElinkTimeMillis < sessionTask.getLastPacketTimeMillis()
		) {
			lastElinkTimeMillis = sessionTask.getLastPacketTimeMillis();
		}
		
		if (
			sessionTask.isBound()
			&& currentTimeMillis - lastElinkTimeMillis >= config.getEnquireLinkPeriod() * 1000
		) {
			SmppSession smppSession = sessionTask.getSession();

			try {
				smppSession.enquireLink(new EnquireLink(), TimeUnit.SECONDS.toMillis(60));
				
				lastElinkTimeMillis = System.currentTimeMillis();
			} catch (RecoverablePduException ex) {
				logger.error("{}", ex);
			} catch (UnrecoverablePduException ex) {
				logger.error("{}", ex);
			} catch (SmppTimeoutException ex) {
				sessionTask.channelClosed();
			} catch (SmppChannelException ex) {
				logger.error("{}", ex);
			} catch (InterruptedException ex) {
				logger.error("{}", ex);
			}
		}
	}
}
