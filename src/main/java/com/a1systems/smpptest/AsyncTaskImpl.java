package com.a1systems.smpptest;

import org.slf4j.Logger;

public class AsyncTaskImpl implements AsyncTask{
	protected Logger logger;
	protected ServiceMonitor monitor;

	public AsyncTaskImpl(Logger logger) {
		this.monitor = new ServiceMonitor();
		this.logger = logger;
	}

	public ServiceMonitor getMonitor(){
		return this.monitor;
	}

	@Override
	public void start() {
		logger.trace("Starting");

		monitor.running();

		logger.trace("Started");
	}

	@Override
	public void start(int timeout) {
		start();
	}

	@Override
	public void stop() {
		if (monitor.isStopping() || monitor.isStopped()) {
			logger.error("Already stopping/ed");

			return;
		}

		logger.trace("Stopping");

		monitor.stopping();
	}

	@Override
	public void stop(int timeout) {
		stop();
	}

}
