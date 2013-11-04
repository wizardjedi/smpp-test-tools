package com.a1systems.smpptest;

import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;

public class SmppSessionHandler extends DefaultSmppSessionHandler{

	@Override
	public PduResponse firePduRequestReceived(PduRequest pduRequest) {
		return pduRequest.createResponse();
	}





}
