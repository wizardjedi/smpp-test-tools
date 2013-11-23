package com.a1systems.smpptest;

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.impl.DefaultSmppClient;

public class ShutdownHook extends Thread{
	protected SmppSession session;
	protected DefaultSmppClient smppClient;

	ShutdownHook(SmppSession session, DefaultSmppClient smppClient) {
		this.session = session;
		this.smppClient = smppClient;
	}

	@Override
	public void run() {
		session.close();
		session.destroy();

		smppClient.destroy();
	}

}
