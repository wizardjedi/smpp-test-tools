package com.a1systems.smpptest;

public interface AsyncTask {
	public void start();

	public void start(int timeout);

	public void stop();

	public void stop(int timeout);
}
