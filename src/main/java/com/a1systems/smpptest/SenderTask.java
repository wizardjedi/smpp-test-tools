package com.a1systems.smpptest;

import com.a1systems.smpptest.domain.Message;
import com.google.common.util.concurrent.RateLimiter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

class SenderTask extends AsyncTaskImpl implements Runnable{
	protected Config config;
	protected RateLimiter rateLimiter;
	protected ConcurrentLinkedQueue<Message> queue;

	public SenderTask(Config config, RateLimiter limiter, ConcurrentLinkedQueue<Message> queue) {
		super(LoggerFactory.getLogger(SenderTask.class));

		this.config = config;

		this.rateLimiter = limiter;

		this.queue = queue;
	}

	@Override
	public void run() {
		start();

		monitor.working();

		logger.trace("Working");

		while (!monitor.isStopping()) {
			if (rateLimiter.tryAcquire()) {
				doWork();
			} else {
				try {
					Thread.sleep(200);
				} catch (InterruptedException ex) {
					logger.error("{}", ex);
				}
			}
		}

		logger.error("Exit thread");

		monitor.stopped();
	}

	protected void doWork() {
		Message msg = queue.poll();
		if (msg != null) {
			//logger.debug(msg.toString());
		}
	}

}
