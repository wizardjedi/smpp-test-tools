package com.a1systems.smpp.multiplexer;


import com.a1systems.smpp.multiplexer.client.Client;
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import io.netty.channel.nio.NioEventLoopGroup;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() throws InterruptedException
    {
        ScheduledExecutorService async = Executors.newScheduledThreadPool(5);

        DefaultSmppClient smppClient = new DefaultSmppClient(new NioEventLoopGroup());

        Client clients[] = new Client[1000];

        for (int i=0;i<1000;i++) {
            SmppSessionConfiguration cfg = new SmppSessionConfiguration(SmppBindType.TRANSCEIVER, "sys"+i, "pass"+i);

            cfg.setHost("127.0.0.1");
            cfg.setPort(2775);

            Client c = new Client(cfg);
            c.setSmppClient(smppClient);
            c.setTimer(async);


            c.start();

            clients[i] = c;

            TimeUnit.MILLISECONDS.sleep(100);
        }

        TimeUnit.SECONDS.sleep(100);

        for (int i=0;i<1000;i++) {
            Client c = clients[i];
            if (
                !c.getCfg().getSystemId().equals(c.getSession().getConfiguration().getSystemId())
                || !c.getCfg().getPassword().equals(c.getSession().getConfiguration().getPassword())
            ) {
                fail("Not equeal "+i);
            }
        }
    }
}
