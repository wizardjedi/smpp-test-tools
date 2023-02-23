package com.a1systems.smpptest;

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.pdu.Unbind;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ShutdownHook extends Thread{
	private static final Logger log = LoggerFactory.getLogger(ShutdownHook.class);

	protected SmppSession session;
	protected DefaultSmppClient smppClient;

	ShutdownHook(SmppSession session, DefaultSmppClient smppClient) {
		this.session = session;
		this.smppClient = smppClient;
	}

	@Override
	public void run() {
		try {
			session.sendRequestPdu(new Unbind(), TimeUnit.SECONDS.toMillis(1), true);
		} catch (Exception e) {
			log.error("Error on unbind", e);
		}

		session.close();
		session.destroy();

		smppClient.destroy();
	}

}
