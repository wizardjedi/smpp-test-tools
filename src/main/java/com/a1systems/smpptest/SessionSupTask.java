package com.a1systems.smpptest;

import com.cloudhopper.smpp.SmppClient;
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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.LoggerFactory;

public class SessionSupTask extends AsyncTaskImpl implements Runnable {

	protected Config config;
	protected SmppSessionConfiguration smppConfig;
	protected SmppSessionHandler sessionHandler;
	protected volatile SmppSession session;
	protected SmppClient client;
	protected Timer timer;
	protected TimerTask rebindTask, elinkTask;

	public SessionSupTask(Config config) {
		super(LoggerFactory.getLogger(SessionSupTask.class));

		this.config = config;

		smppConfig = new SmppSessionConfiguration();

		smppConfig.setSystemId(config.getSystemId());
		smppConfig.setPassword(config.getPassword());
		smppConfig.setHost(config.getHost());
		smppConfig.setPort(config.getPort());

		smppConfig.setWindowSize(20);

		smppConfig.setType(config.getBindType());

		this.sessionHandler = new SmppSessionHandler(this);
	}

	public SmppSessionConfiguration getSmppSessionConfiguration() {
		return this.smppConfig;
	}

	public SmppSessionHandler getSmppSessionHandler() {
		return this.sessionHandler;
	}

	public SmppClient getClient() {
		return this.client;
	}

	public void setSession(SmppSession session) {
		this.session = session;
	}

	public SmppSession getSession() {
		return this.session;
	}

	@Override
	public void run() {
		start();

		monitor.working();

		logger.trace("Working");

		timer = new Timer();

		client = new DefaultSmppClient(Executors.newCachedThreadPool(), 20);

		rebindTask = new RebindTimerTask(this);
		elinkTask = new EnquireLinkTask(this);

		timer.scheduleAtFixedRate(rebindTask, 0, TimeUnit.SECONDS.toMillis(config.getRebindPeriod()));
		timer.scheduleAtFixedRate(elinkTask, 0, TimeUnit.SECONDS.toMillis(config.getEnquireLinkPeriod()));

		try {
			ServiceMonitorUtils.waitStopping(monitor);
		} catch (InterruptedException ex) {
			logger.error("{}", ex);
		}

		monitor.stopped();

		logger.debug("Exit thread");
	}

	@Override
	public void stop(){
		if (monitor.isStopping() || monitor.isStopped()) {
			return ;
		}

		logger.trace("Stopping");

		monitor.stopping();

		rebindTask.cancel();
		elinkTask.cancel();

		timer.cancel();
		timer.purge();
		session.destroy();
		client.destroy();
	}

	public boolean isBound() {
		return (session != null && session.isBound());
	}

	public void channelClosed() {
		this.session.destroy();
	}
}
