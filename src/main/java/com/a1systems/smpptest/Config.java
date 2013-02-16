package com.a1systems.smpptest;

import com.cloudhopper.smpp.SmppBindType;
import com.google.common.base.Objects;

public class Config {
	final static String OPT_HELP = "help";
	final static String OPT_HOST = "h";
	final static String OPT_SYSTEM_ID = "u";
	final static String OPT_PASSWORD = "p";
	final static String OPT_RECEIVER = "rc";
	final static String OPT_TRANSMITTER = "ts";
	final static String OPT_TRANSCEIVER = "tc";
	final static String OPT_HEX = "hex";
	final static String OPT_HEXRAW = "hexraw";
	final static String OPT_WAIT = "wait";
	final static String OPT_ENQUIRE_LINK_PERIOD = "elinkperiod";
	final static String OPT_ENQUIRE_LINK_ON_NO_TRANSMIT = "elinkonnotx";
	final static String OPT_REBIND_PERIOD = "rebindperiod";
	final static String OPT_SPEED = "speed";
	final static String OPT_SHORT_VERBOSE = "v";
	final static String OPT_LONG_VERBOSE = "verbose";
	final static String OPT_NO_REBIND = "norebind";
	final static String OPT_SUMMARY = "summary";
	final static String OPT_STDIN = "stdin";
	final static String OPT_SUBTOTAL_PERIOD = "subtotalperiod";


	final static String OPT_SMPP_ESM_CLASS = "esm_class";
	final static String OPT_SMPP_PROTOCOL_ID = "protocol_id";

	final static String OPT_EXAMPLE = "example";

	final static int DEFAULT_SUBTOTAL_PERIOD = 60;
	final static int DEFAULT_ELINK_PERIOD = 30;
	final static int DEFAULT_REBIND_PERIOD = 60;
	final static int DEFAULT_SPEED = 10;

	protected String host;
	protected int port;
	protected String systemId;
	protected String password;
	protected SmppBindType bindType;
	protected boolean hex = false;
	protected boolean exitOnDone = true;
	protected boolean verboseLogging = false;
	protected boolean rebind = true;
	protected boolean summary = false;
	protected boolean stdin = false;
	protected boolean elinkNoTransmit = false;

	protected int enquireLinkPeriod = DEFAULT_ELINK_PERIOD;
	protected int rebindPeriod = DEFAULT_REBIND_PERIOD;
	protected int speed = DEFAULT_SPEED;

	public boolean isStdin() {
		return stdin;
	}

	public void setStdin(boolean stdin) {
		this.stdin = stdin;
	}

	public boolean isSummary() {
		return summary;
	}

	public void setSummary(boolean summary) {
		this.summary = summary;
	}

	public boolean isRebind() {
		return rebind;
	}

	public void setRebind(boolean rebind) {
		this.rebind = rebind;
	}

	public boolean isVerboseLogging() {
		return verboseLogging;
	}

	public void setVerboseLogging(boolean verboseLogging) {
		this.verboseLogging = verboseLogging;
	}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}


	public int getRebindPeriod() {
		return rebindPeriod;
	}

	public void setRebindPeriod(int rebindPeriod) {
		this.rebindPeriod = rebindPeriod;
	}

	public int getEnquireLinkPeriod() {
		return enquireLinkPeriod;
	}

	public void setEnquireLinkPeriod(int enquireLinkPeriod) {
		this.enquireLinkPeriod = enquireLinkPeriod;
	}

	public boolean isExitOnDone() {
		return exitOnDone;
	}

	public void setExitOnDone(boolean exitOnDone) {
		this.exitOnDone = exitOnDone;
	}

	public boolean isHex() {
		return hex;
	}

	public void setHex(boolean hex) {
		this.hex = hex;
	}

	public SmppBindType getBindType() {
		return bindType;
	}

	public void setBindType(SmppBindType bindType) {
		this.bindType = bindType;
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

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isElinkNoTransmit() {
		return elinkNoTransmit;
	}

	public void setElinkNoTransmit(boolean elinkNoTransmit) {
		this.elinkNoTransmit = elinkNoTransmit;
	}
	
	@Override
	public String toString(){
		return
			Objects.
				toStringHelper(this).
				add("host",host).
				add("port",port).
				add("systemId",systemId).
				add("password",password).
				add("bindType",bindType).
				add("hex",hex).
				add("exitOnDone",exitOnDone).
				add("enquireLinkPeriod",enquireLinkPeriod).
				add("rebindPeriod",rebindPeriod).
				add("speed",speed).
				add("verboseLogging",verboseLogging).
				add("rebind",rebind).
				add("summary",summary).
				toString();
	}
}
