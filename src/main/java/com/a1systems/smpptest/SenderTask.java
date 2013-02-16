package com.a1systems.smpptest;

import com.a1systems.smpptest.domain.Message;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.google.common.util.concurrent.RateLimiter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

class SenderTask extends AsyncTaskImpl implements Runnable{
	protected Config config;
	protected RateLimiter rateLimiter;
	protected ConcurrentLinkedQueue<Message> queue;
	protected Client client;
	
	public SenderTask(Config config, RateLimiter limiter, ConcurrentLinkedQueue<Message> queue, Client client) {
		super(LoggerFactory.getLogger(SenderTask.class));

		this.config = config;

		this.rateLimiter = limiter;

		this.queue = queue;
		
		this.client = client;
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
		if (config.isStdin()) {
			
		} else {
			Message msg = queue.poll();
			
			SessionSupTask sessionTask = client.getSessionTask();
			
			if (msg != null && sessionTask.getSession().isBound()) {
				SmppSession session = sessionTask.getSession();
				
				try {
					session.submit(msg.getSubmitSm(), TimeUnit.SECONDS.toMillis(60));
					
					sessionTask.packetSent();
					
				} catch (RecoverablePduException ex) {
					Logger.getLogger(SenderTask.class.getName()).log(Level.SEVERE, null, ex);
				} catch (UnrecoverablePduException ex) {
					Logger.getLogger(SenderTask.class.getName()).log(Level.SEVERE, null, ex);
				} catch (SmppTimeoutException ex) {
					Logger.getLogger(SenderTask.class.getName()).log(Level.SEVERE, null, ex);
				} catch (SmppChannelException ex) {
					Logger.getLogger(SenderTask.class.getName()).log(Level.SEVERE, null, ex);
				} catch (InterruptedException ex) {
					Logger.getLogger(SenderTask.class.getName()).log(Level.SEVERE, null, ex);
				}
				
				client.setDone(true);
			}
		}
	}

}
