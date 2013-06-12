package com.a1systems.smpptest.config;

import java.util.List;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class Config {
	@Option(name = "-wait", usage = "Do not exit after work done")
	protected boolean wait;
	
	@Option(name = "-h", usage = "Set host:port for connection")
	protected String hostPort;
	
	@Option(name = "-u", usage = "Set system id")
	protected String systemId;
	
	@Option(name = "-p", usage = "Set password")
	protected String password;

	@Option(name = "-tc", usage="Use transceiver bind mode")
	protected boolean transceiver;

	@Argument
	protected List<String> arguments;

	public List<String> getArguments() {
		return arguments;
	}

	public void setArguments(List<String> arguments) {
		this.arguments = arguments;
	}
	
	public boolean isTransceiver() {
		return transceiver;
	}

	public void setTransceiver(boolean transceiver) {
		this.transceiver = transceiver;
	}
	
	public boolean isWait() {
		return wait;
	}

	public void setWait(boolean wait) {
		this.wait = wait;
	}

	public String getHostPort() {
		return hostPort;
	}

	public void setHostPort(String hostPort) {
		this.hostPort = hostPort;
	}

	public String getSystemId() {
		return systemId;
	}

	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
