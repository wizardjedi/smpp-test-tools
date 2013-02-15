package com.a1systems.smpptest.domain;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.commons.charset.GSMCharset;
import com.cloudhopper.commons.gsm.GsmUtil;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import com.cloudhopper.smpp.util.SmppUtil;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Message {
	protected String sourceAddress;
	protected String destinationAddress;
	protected String text;

	public Message(String text) {
		this.text = text;
	}

	public SubmitSm getSubmitSm() {
		SubmitSm sm = new SubmitSm();
		
		sm.setSourceAddress(new Address((byte)5, (byte)0, "Test"));
		sm.setDestAddress(new Address((byte)1, (byte)1, "79264964806"));
		
		try {
			sm.setShortMessage(CharsetUtil.encode(text, CharsetUtil.CHARSET_GSM7));
		} catch (SmppInvalidArgumentException ex) {
			Logger.getLogger(Message.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		return sm;
	}

	@Override
	public String toString(){
		return this.text;
	}
}
