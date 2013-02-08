package com.a1systems.smpptest;

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppBindException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

public class SmppClientSession implements Runnable {

	protected static final Logger logger = LoggerFactory.getLogger(SmppClientSession.class);

	protected DefaultSmppClient client;
	protected SmppSession session;
	protected ClientSmppSessionHandler sessionHandler;
	protected SmppSessionConfiguration config;

	protected Semaphore sem;

	SmppClientSession(DefaultSmppClient clientBootstrap, SmppSession session, ClientSmppSessionHandler sessionHandler, SmppSessionConfiguration config) {
		logger.debug("Created SmppClientSession object");

		this.client = clientBootstrap;
		this.session = session;
		this.sessionHandler = sessionHandler;
		this.config = config;
	}

	@Override
	public void run() {
		do {
			Timer t = new Timer();

			sem = new Semaphore(0);

			try {
				logger.info("Try to bind");

				sessionHandler.setSmppClientSession(this);

				session = client.bind(config, sessionHandler);

				logger.info("Bound");

				// session started
				//sessionStarted();

				t.schedule(new EnquireLinkTask(session, this), TimeUnit.SECONDS.toMillis(3), TimeUnit.SECONDS.toMillis(3));

				waitEvent();

				logger.debug("Need to rebind");
			} catch (Exception e) {
				logger.error("Could not bind. {}", e.toString());
			}

			logger.info("Rebind in 10 seconds");

			try {
				TimeUnit.SECONDS.sleep(10);
			} catch (InterruptedException e) {
				break;
			}
		} while (true);
	}

	public void waitEvent() {
		try {
			sem.acquire();
		} catch (InterruptedException ex) {
			java.util.logging.Logger.getLogger(SmppClientSession.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void reset(){
		sem.release();
	}

	public void sessionStarted() {
		try {
			sem.acquire();
		} catch (InterruptedException ex) {
			java.util.logging.Logger.getLogger(SmppClientSession.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private static class EnquireLinkTask extends TimerTask {

		protected SmppSession session;
		protected SmppClientSession clientSession;

		private EnquireLinkTask(SmppSession session, SmppClientSession clientSession) {
			this.session = session;
			this.clientSession = clientSession;
		}

		@Override
		public void run() {
			try {
				logger.info("EnquireLink sending");

				session.enquireLink(new EnquireLink(), TimeUnit.SECONDS.toMillis(3));
			} catch (Exception e) {
				logger.error("Enquire link timeout will rebind.");

				clientSession.reset();
				this.cancel();
			}
		}
	}
}
