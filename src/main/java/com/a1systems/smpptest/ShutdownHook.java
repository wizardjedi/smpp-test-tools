package com.a1systems.smpptest;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutdownHook extends Thread{
	private final Logger logger = LoggerFactory.getLogger(ShutdownHook.class);

	protected AsyncTask task;

	public ShutdownHook(AsyncTask task) {
		this.task = task;
	}

	@Override
	public void run(){
		try {
			task.stop();

			ServiceMonitorUtils.waitStopped(task.getMonitor());
		} catch (InterruptedException ex) {
			logger.error("{}", ex);
		}
	}
}
