package com.a1systems.smpptest;

import java.util.concurrent.atomic.AtomicInteger;

public class ServiceMonitor {
	private final static int STATE_IDLE = 0;
	private final static int STATE_RUNNING = 1;
	private final static int STATE_WORKING = 2;
	private final static int STATE_STOPPING = 3;
	private final static int STATE_STOPPED = 4;

	protected volatile AtomicInteger state = new AtomicInteger(STATE_IDLE);

	public void running(){
		this.state.compareAndSet(STATE_IDLE, STATE_RUNNING);
	}

	public void working(){
		this.state.compareAndSet(STATE_RUNNING, STATE_WORKING);
	}

	public void stopping(){
		this.state.set(STATE_STOPPING);
	}

	public void stopped(){
		this.state.compareAndSet(STATE_STOPPING, STATE_STOPPED);
	}

	public boolean isIdle(){
		return state.get() == STATE_IDLE;
	}

	public boolean isRunning(){
		return state.get() == STATE_RUNNING;
	}

	public boolean isWorking(){
		return state.get() == STATE_WORKING;
	}

	public boolean isStopping(){
		return state.get() == STATE_STOPPING;
	}

	public boolean isStopped(){
		return state.get() == STATE_STOPPED;
	}

	@Override
	public String toString(){
		int st = state.get();

		Statuses[] statuses = Statuses.values();

		return statuses[st].toString();
	}

	private static enum Statuses {
		IDLE,
		RUNNING,
		WORKING,
		STOPPING,
		STOPPED;
	}

}
