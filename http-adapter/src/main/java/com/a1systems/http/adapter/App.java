package com.a1systems.http.adapter;

import org.springframework.context.support.GenericXmlApplicationContext;

/**
 * Hello world!
 *
 */
public class App
{
    public static void main( String[] args )
    {
        GenericXmlApplicationContext genericXmlApplicationContext = new GenericXmlApplicationContext("spring-context.xml");

        /*
        SmppSessionConfiguration sessionConfig = new SmppSessionConfiguration();

		sessionConfig.setType(SmppBindType.TRANSCEIVER);
		sessionConfig.setHost("127.0.0.1");
		sessionConfig.setPort(2775);
		sessionConfig.setSystemId("smppclient1");
		sessionConfig.setPassword("password");

		Client client = new Client(sessionConfig);

		client.setSessionHandler(new MySmppSessionHandler(client));

		ExecutorService pool = Executors.newFixedThreadPool(2);

		pool.submit(client);

		client.start();
        */
    }
}
