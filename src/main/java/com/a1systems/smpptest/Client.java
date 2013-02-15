package com.a1systems.smpptest;

import com.a1systems.smpptest.domain.Message;
import com.google.common.util.concurrent.RateLimiter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.slf4j.LoggerFactory;

public class Client extends AsyncTaskImpl{
	protected Config config;

	protected List<AsyncTask> childTasks = new ArrayList<AsyncTask>();
	protected List<ServiceMonitor> childMonitors = new ArrayList<ServiceMonitor>();
	protected ExecutorService taskPool;
	protected RateLimiter rateLimiter;
	protected java.util.concurrent.ConcurrentLinkedQueue<Message> messageQueue = new ConcurrentLinkedQueue<Message>();

	public Client(Config config) {
		super(LoggerFactory.getLogger(Client.class));

		this.config = config;

		this.rateLimiter = RateLimiter.create(config.getSpeed());
	}

	public void start(){
		logger.trace("Running");

		monitor.running();

		// create child threads
		taskPool = Executors.newCachedThreadPool();

		logger.trace("Creating session task");

		SessionSupTask sessionTask = new SessionSupTask(config);
		childTasks.add(sessionTask);
		childMonitors.add(sessionTask.getMonitor());

		logger.trace("Submitting session task to pool");

		taskPool.submit(sessionTask);

		for (int i=0;i<10;i++) {
			logger.trace("Creating sender task {}", i);

			SenderTask task = new SenderTask(config, rateLimiter, messageQueue);

			childTasks.add(task);

			childMonitors.add(task.getMonitor());

			logger.trace("Submiting task to pool");

			taskPool.submit(task);
		}

		try {
			ServiceMonitorUtils.waitAllWorking(childMonitors);
		} catch (InterruptedException ex) {
			logger.error("{}", ex);
		}

		logger.trace("Working");

		monitor.working();
	}

	@Override
	public void stop() {
		if (monitor.isStopped() || monitor.isStopping()) {
			logger.error("Already stopping");
			
			return;
		}

		logger.trace("Stopping");

		monitor.stopping();

		logger.trace("Stopping all tasks");

		taskPool.shutdown();
		
		for (AsyncTask task:childTasks) {
			task.stop();
		}
		
		try {
			logger.trace("Waiting child tasks stopped");

			ServiceMonitorUtils.waitAllStopped(childMonitors);
		} catch (InterruptedException ex) {
			logger.error("{}", ex);
		}
	}

	public void run(CommandLine line) {
		start();

		try {
			ServiceMonitorUtils.waitWorking(monitor);
		} catch (InterruptedException ex) {
			logger.error("{}", ex);
		}

		logger.trace("Start working");

		/*int i=0;

		while(!monitor.isStopping()) {
			if (messageQueue.isEmpty()) {
				messageQueue.add(new Message("msg #"+i));

				//logger.debug("Generated message msg #"+i);

				i++;
			}
		}*/

		logger.debug("{},{}", config.isExitOnDone(), config.isStdin());
		try {
			Thread.sleep(TimeUnit.SECONDS.toMillis(2));
		} catch (InterruptedException ex) {
			Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
		}

		if (
			!config.isExitOnDone()
			|| config.isStdin()
		) {
			try {
				ServiceMonitorUtils.waitStopping(monitor);
			} catch (InterruptedException ex) {
				logger.error("{}", ex);
			}
		} else {
			stop();
		}

		monitor.stopped();

		logger.trace("Shutdown");
	}
}
