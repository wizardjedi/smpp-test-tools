package com.a1systems.smpptest;

public class AsyncTaskImpl implements AsyncTask{

	protected ServiceMonitor monitor;

	public AsyncTaskImpl() {
		this.monitor = new ServiceMonitor();
	}

	public ServiceMonitor getMonitor(){
		return this.monitor;
	}

	@Override
	public void start() {
		monitor.running();

		monitor.working();
	}

	@Override
	public void start(int timeout) {
		start();
	}

	@Override
	public void stop() {
		monitor.stopping();

		monitor.stopped();
	}

	@Override
	public void stop(int timeout) {
		stop();
	}

}
