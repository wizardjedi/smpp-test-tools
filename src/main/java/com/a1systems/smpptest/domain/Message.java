package com.a1systems.smpptest.domain;

import com.cloudhopper.smpp.pdu.SubmitSm;

public class Message {
	protected String sourceAddress;
	protected String destinationAddress;
	protected String text;

	public Message(String text) {
		this.text = text;
	}

	public SubmitSm getSubmitSm() {
		return new SubmitSm();
	}

	@Override
	public String toString(){
		return this.text;
	}
}
