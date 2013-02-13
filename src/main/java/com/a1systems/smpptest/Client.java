/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.a1systems.smpptest;

import com.cloudhopper.commons.charset.GSMCharset;
import com.cloudhopper.commons.gsm.GsmUtil;
import com.cloudhopper.commons.util.HexString;
import com.cloudhopper.commons.util.HexUtil;
import com.cloudhopper.commons.util.StringUtil;
import com.cloudhopper.smpp.SmppClient;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Client {
	protected static final Logger logger = LoggerFactory.getLogger(Client.class);

	protected Config config;

	public Client(Config config) {
		logger.debug("Create client object for with {}", config);

		this.config = config;
	}

	public void run(String[] msgs){
		ExecutorService pool = Executors.newFixedThreadPool(1);

		DefaultSmppClient client = new DefaultSmppClient(Executors.newCachedThreadPool(), 20);

		SmppClientSession clientSession = new SmppClientSession(client, config);

		pool.submit(clientSession);
		pool.submit(new Sender(clientSession, msgs));
	}

	public static class Sender implements Runnable{
		protected final Logger logger = LoggerFactory.getLogger(Sender.class);

		protected SmppClientSession clientSession;
		protected String[] arguments;

		public Sender(SmppClientSession clientSession, String[] arguments){
			this.clientSession = clientSession;
			this.arguments = arguments;
		}

		@Override
		public void run() {
			do {
				if (clientSession.isBound()) {
					SmppSession smppSession = clientSession.getSession();

					SubmitSm ssm = new SubmitSm();

					ssm.setDestAddress(getAddress(arguments[0]));
					ssm.setSourceAddress(getAddress(arguments[1]));

					byte[] buffer = StringUtil.getAsciiBytes(arguments[2]);
					try {
						ssm.setShortMessage(buffer);
					} catch (SmppInvalidArgumentException ex) {
						//
					}

					try {
						logger.debug("Try to send {}", ssm.toString());

						smppSession.submit(ssm, TimeUnit.SECONDS.toMillis(60));
					} catch (RecoverablePduException ex) {
						java.util.logging.Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
					} catch (UnrecoverablePduException ex) {
						java.util.logging.Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
					} catch (SmppTimeoutException ex) {
						java.util.logging.Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
					} catch (SmppChannelException ex) {
						java.util.logging.Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
					} catch (InterruptedException ex) {
						java.util.logging.Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
					}

					break;
				}
			} while(true);
		}

		public Address getAddress(String adr){
			Address a = new Address();

			String[] parts = adr.split(":");

			a.setTon((byte)Integer.parseInt(parts[0]));
			a.setNpi((byte)Integer.parseInt(parts[1]));

			a.setAddress(parts[2]);

			return a;
		}
	}
}
