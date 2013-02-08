/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.a1systems.smpptest;

import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientSmppSessionHandler extends DefaultSmppSessionHandler {
	protected SmppClientSession clientSession;

	Logger logger = LoggerFactory.getLogger(ClientSmppSessionHandler.class);

	public ClientSmppSessionHandler() {
		super(LoggerFactory.getLogger(ClientSmppSessionHandler.class));
	}

	@Override
	public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
		//super.fireExpectedPduResponseReceived(pduAsyncResponse);
	}

	@Override
	public void firePduRequestExpired(PduRequest pduRequest) {
		logger.warn("PDU request expired: {}", pduRequest);
	}

	@Override
	public void fireChannelUnexpectedlyClosed() {
		logger.warn("Channel closed will rebind");

		clientSession.reset();
	}

	@Override
	public PduResponse firePduRequestReceived(PduRequest pduRequest) {
		PduResponse response = pduRequest.createResponse();

		response.setCommandStatus(0);

		return response;
	}

	public void setSmppClientSession(SmppClientSession clientSession) {
		this.clientSession = clientSession;
	}
}