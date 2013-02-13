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
import com.cloudhopper.smpp.pdu.BaseSm;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoder;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoderContext;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.util.PduUtil;
import com.cloudhopper.smpp.util.SmppUtil;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.jboss.netty.buffer.BigEndianHeapChannelBuffer;
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

	public void run(String[] msgs) {
		ExecutorService pool = Executors.newFixedThreadPool(1);

		DefaultSmppClient client = new DefaultSmppClient(Executors.newCachedThreadPool(), 20);

		SmppClientSession clientSession = new SmppClientSession(client, config);

		pool.submit(clientSession);
		pool.submit(new Sender(clientSession, msgs));
	}

	public static class Sender implements Runnable {

		protected final Logger logger = LoggerFactory.getLogger(Sender.class);
		protected SmppClientSession clientSession;
		protected String[] arguments;

		public Sender(SmppClientSession clientSession, String[] arguments) {
			this.clientSession = clientSession;
			this.arguments = arguments;
		}

		@Override
		public void run() {
			do {
				if (clientSession.isBound()) {
					SmppSession smppSession = clientSession.getSession();

					if (arguments.length == 3) {
						SubmitSm ssm = new SubmitSm();

						ssm.setDestAddress(getAddress(arguments[0]));
						ssm.setSourceAddress(getAddress(arguments[1]));

						byte[] buffer = StringUtil.getAsciiBytes(arguments[2]);
						try {
							ssm.setShortMessage(buffer);

							try {
								logger.debug("Try to send {}", ssm.toString());

								smppSession.submit(ssm, TimeUnit.SECONDS.toMillis(60));
							} catch (RecoverablePduException ex) {
								//
							} catch (UnrecoverablePduException ex) {
								//
							} catch (SmppTimeoutException ex) {
								//
							} catch (SmppChannelException ex) {
								//
							} catch (InterruptedException ex) {
								//
							}

							break;
						} catch (SmppInvalidArgumentException ex) {
							logger.error("{}", ex);
						}
					} else if (arguments.length == 1) {
						logger.debug("Length {}", arguments[0].length());

						String len = HexUtil.toHexString(arguments[0].length() / 2 + 16);

						String head = len+"00000004"+"00000000"+"00000001";

						logger.debug(head+arguments[0]);

						byte[] bytes = HexString.valueOf(head+arguments[0]).asBytes();

						BigEndianHeapChannelBuffer buffer = new BigEndianHeapChannelBuffer(bytes);

						logger.debug("{}", buffer);

						DefaultPduTranscoderContext context = new DefaultPduTranscoderContext();
						DefaultPduTranscoder transcoder = new DefaultPduTranscoder(context);

						SubmitSm ssm;
						try {
							logger.debug("Try to decode");

							ssm = (SubmitSm)transcoder.decode(buffer);

							logger.debug("{}", ssm);
							try {
								smppSession.sendRequestPdu(ssm, 60000, true);
							} catch (SmppTimeoutException ex) {
								logger.error("{}", ex);
							} catch (SmppChannelException ex) {
								logger.error("{}", ex);
							} catch (InterruptedException ex) {
								logger.error("{}", ex);
							}
						} catch (UnrecoverablePduException ex) {
							logger.error("{}", ex);
						} catch (RecoverablePduException ex) {
							logger.error("{}", ex);
						}

						break;
					}
				}
			} while (true);
		}

		public Address getAddress(String adr) {
			Address a = new Address();

			String[] parts = adr.split(":");

			a.setTon((byte) Integer.parseInt(parts[0]));
			a.setNpi((byte) Integer.parseInt(parts[1]));

			a.setAddress(parts[2]);

			return a;
		}
	}
}
