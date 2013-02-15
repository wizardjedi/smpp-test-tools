package com.a1systems.smpptest;

import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmppSessionHandler extends DefaultSmppSessionHandler {
	protected Logger logger = LoggerFactory.getLogger(SmppSessionHandler.class);
	protected SessionSupTask sessionTask;

	public SmppSessionHandler(SessionSupTask sessionTask) {
		this.sessionTask = sessionTask;
	}

	@Override
	public void fireChannelUnexpectedlyClosed() {
		sessionTask.channelClosed();
	}
}
