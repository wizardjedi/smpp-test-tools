package com.a1systems.smpptest;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutdownHook extends Thread{
	private final Logger logger = LoggerFactory.getLogger(ShutdownHook.class);

	protected ServiceMonitor monitor;

	public ShutdownHook(ServiceMonitor monitor) {
		this.monitor = monitor;
	}

	@Override
	public void run(){
		monitor.stopping();

		while (!monitor.isStopped()) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException ex) {
				logger.error("Error during stopping. {}", ex.getMessage());
				ex.printStackTrace();
				break;
			}
		}
	}
}
