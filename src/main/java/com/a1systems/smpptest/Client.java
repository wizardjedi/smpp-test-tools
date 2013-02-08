/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.a1systems.smpptest;

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Client {
	protected static final Logger logger = LoggerFactory.getLogger(Client.class);

	protected String host;
	protected int port;
	protected String systemid;
	protected String password;
	protected BindType bindType;

	public Client(String host, int port, String systemid, String password, BindType bt) {
		logger.debug("Create client object for {}:{} with credentials {} {}", host, port, systemid, password);

		this.host = host;
		this.port = port;
		this.systemid = systemid;
		this.password = password;
		this.bindType = bt;
	}

	public void run(){
		SmppSessionConfiguration config = new SmppSessionConfiguration();

		config.setSystemId(systemid);
		config.setPassword(password);
		config.setHost(host);
		config.setPort(port);

		config.setWindowSize(20);

		ExecutorService pool = Executors.newFixedThreadPool(1);

		DefaultSmppClient client = new DefaultSmppClient(Executors.newCachedThreadPool(), 20);

		SmppSession session = null;

		pool.submit(new SmppClientSession(client, session, new ClientSmppSessionHandler(), config));
	}
}
