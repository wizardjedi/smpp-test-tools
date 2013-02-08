/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.a1systems.smpptest;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PacketSenderWorker implements Runnable {

	protected SmppSession session = null;
	protected CountDownLatch latch;
	protected byte[] msg;

	public PacketSenderWorker(SmppSession session, CountDownLatch latch, byte[] msg) {
		this.session = session;
		this.msg = msg;
		this.latch = latch;
	}

	@Override
	public void run() {
		SubmitSm submitSm = new SubmitSm();

		submitSm.setSourceAddress(new Address(SmppConstants.TON_ALPHANUMERIC, SmppConstants.NPI_UNKNOWN, "test"));
		submitSm.setDestAddress(new Address(SmppConstants.TON_INTERNATIONAL, SmppConstants.NPI_E164, "79264964806"));

		submitSm.setRegisteredDelivery(SmppConstants.REGISTERED_DELIVERY_SMSC_RECEIPT_REQUESTED);

		try {
			submitSm.setShortMessage(msg);
		} catch (SmppInvalidArgumentException ex) {
			Logger.getLogger(PacketSenderWorker.class.getName()).log(Level.SEVERE, null, ex);
		}
		try {
			session.sendRequestPdu(submitSm, TimeUnit.SECONDS.toMillis(60), false);
		} catch (Exception e) {

		}

		latch.countDown();
	}
}
