package com.a1systems.smpptest;

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.type.SmppBindException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmppClientSession implements Runnable {

	public static final int DEFAULT_IDLE = 0;
	public static final int DEFAULT_BINDING = 1;
	public static final int DEFAULT_BOUND = 2;
	protected static final Logger logger = LoggerFactory.getLogger(SmppClientSession.class);
	protected Timer timer;
	protected DefaultSmppClient client;
	protected SmppSession session;
	protected ClientSmppSessionHandler sessionHandler;
	protected SmppSessionConfiguration smppConfig;
	protected Config config;
	protected AtomicInteger sessionState = new AtomicInteger(DEFAULT_IDLE);

	SmppClientSession(DefaultSmppClient client, Config config) {
		this.config = config;

		smppConfig = new SmppSessionConfiguration();

		smppConfig.setSystemId(config.getSystemId());
		smppConfig.setPassword(config.getPassword());
		smppConfig.setHost(config.getHost());
		smppConfig.setPort(config.getPort());

		smppConfig.setWindowSize(20);

		this.sessionHandler = new ClientSmppSessionHandler(this);

		this.client = client;
	}

	public DefaultSmppClient getClient() {
		return client;
	}

	public boolean isBound(){
		return sessionState.get() == DEFAULT_BOUND;
	}

	public void setClient(DefaultSmppClient client) {
		this.client = client;
	}

	public SmppSession getSession() {
		return session;
	}

	public void setSession(SmppSession session) {
		this.session = session;
	}

	public ClientSmppSessionHandler getSessionHandler() {
		return sessionHandler;
	}

	public void setSessionHandler(ClientSmppSessionHandler sessionHandler) {
		this.sessionHandler = sessionHandler;
	}

	public SmppSessionConfiguration getSmppConfig() {
		return smppConfig;
	}

	public void setSmppConfig(SmppSessionConfiguration smppConfig) {
		this.smppConfig = smppConfig;
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public void binding() {
		if (
			sessionState.get() == DEFAULT_IDLE
			|| config.isRebind()
		) {
			timer = new Timer();

			timer.scheduleAtFixedRate(new RebindTask(this), 0, TimeUnit.SECONDS.toMillis(config.getRebindPeriod()));
		}
		this.sessionState.set(DEFAULT_BINDING);
	}

	public void bound(SmppSession session) {
		this.sessionState.compareAndSet(DEFAULT_BINDING, DEFAULT_BOUND);

		this.session = session;

		timer.scheduleAtFixedRate(new EnquireLinkTask(this), 0, TimeUnit.SECONDS.toMillis(config.getEnquireLinkPeriod()));
	}

	@Override
	public void run() {
		timer = new Timer();

		binding();
	}

	public void reset() {
		timer.cancel();
		session.destroy();
		binding();
	}

	private static class RebindTask extends TimerTask {

		protected final Logger logger = LoggerFactory.getLogger(RebindTask.class);
		protected SmppClientSession clientSession;

		private RebindTask(SmppClientSession clientSession) {
			this.clientSession = clientSession;
		}

		@Override
		public void run() {
			try {
				logger.info("Try to bind");

				SmppSession session = clientSession.getClient().bind(clientSession.getSmppConfig(), clientSession.getSessionHandler());

				logger.info("Bound");

				clientSession.bound(session);

				cancel();
			} catch (SmppTimeoutException ex) {
				//
			} catch (SmppChannelException ex) {
				//
			} catch (SmppBindException ex) {
				//
			} catch (UnrecoverablePduException ex) {
				//
			} catch (InterruptedException ex) {
				//
			} finally {
				if (!clientSession.getConfig().isRebind()) {
					cancel();
				}
			}
		}
	}

	private static class EnquireLinkTask extends TimerTask {

		protected SmppClientSession clientSession;

		private EnquireLinkTask(SmppClientSession clientSession) {
			this.clientSession = clientSession;
		}

		@Override
		public void run() {
			try {
				logger.info("EnquireLink sending");

				clientSession.getSession().enquireLink(new EnquireLink(), TimeUnit.SECONDS.toMillis(clientSession.getConfig().getEnquireLinkPeriod()));
			} catch (Exception e) {
				logger.error("Enquire link timeout will rebind.");
				this.cancel();
			}
		}
	}
}
