package com.a1systems.smpp.zabbix.checker;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.pdu.BindTransceiverResp;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.simulator.SmppSimulatorServer;
import com.cloudhopper.smpp.simulator.SmppSimulatorSessionHandler;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.LoggingOptions;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class App 
{
    private SmppSimulatorServer sim;
    
    private DefaultSmppClient client;
    
    private long end;
    private long start;    
    
    public static void main( String[] args )
    {
        App app = new App();
        
        app.run(args);
    }
    
    public void run(String[] args) {    
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        
        String systemId = args[2];
        String password = args[3];
        
        String testAbonent = args[4];
        
        int serverPort = Integer.parseInt(args[5]);
        
        sim = new SmppSimulatorServer();
        
        sim.start(serverPort);
        
        SmppSessionConfiguration config = new SmppSessionConfiguration();
        LoggingOptions logOptions = new LoggingOptions();
        
        /*logOptions.setLogBytes(false);
        logOptions.setLogPdu(false);*/
        
        config.setLoggingOptions(logOptions);
        
        config.setSystemId(systemId);
        config.setPassword(password);
        config.setHost(host);
        config.setPort(port);
        
        client = new DefaultSmppClient();
        try {
            SmppSimulatorSessionHandler serverSession = sim.pollNextSession(TimeUnit.SECONDS.toMillis(30));
            BindTransceiverResp resp = new BindTransceiverResp();
            
            Pdu bindPdu = serverSession.pollNextPdu(TimeUnit.SECONDS.toMillis(30));
            
            resp.setSequenceNumber(bindPdu.getSequenceNumber());
            
            resp.setSystemId("");
            
            resp.setCommandLength(17);
            
            serverSession.sendPdu(resp);
            
            TimeUnit.SECONDS.sleep(2);
            
            start = System.currentTimeMillis();
            
            SmppSession session = client.bind(config);
            
            SubmitSm sm = new SubmitSm();
            
            Random r = new Random(System.currentTimeMillis());
            
            String body = "This is zabbix test message. Ignore it."+r.nextInt();
            
            sm.setShortMessage(CharsetUtil.encode(body,CharsetUtil.CHARSET_GSM8));
            
            sm.setDestAddress(new Address((byte)1, (byte)1, testAbonent));
            sm.setSourceAddress(new Address((byte)5, (byte)0, "zcheck"));
            SubmitSmResp submitSmResp = session.submit(sm, TimeUnit.SECONDS.toMillis(30));
            
            end = System.currentTimeMillis();
            
            if (submitSmResp.getCommandStatus() == 0) {
                Pdu pdu = serverSession.pollNextPdu(TimeUnit.SECONDS.toMillis(30));

                SubmitSm backSm = (SubmitSm)pdu;

                if (
                    backSm.getSourceAddress().getAddress().equals("zcheck")
                    && backSm.getDestAddress().getAddress().equals(testAbonent)
                    && CharsetUtil.decode(backSm.getShortMessage(), CharsetUtil.CHARSET_GSM8).equals(body)
                ) {
                     SubmitSmResp backSubmitSmResp = new SubmitSmResp();

                     backSubmitSmResp.setSequenceNumber(pdu.getSequenceNumber());
                     backSubmitSmResp.setMessageId(String.valueOf(System.currentTimeMillis()));

                     backSubmitSmResp.calculateAndSetCommandLength();

                     serverSession.sendPdu(backSubmitSmResp);

                     success();
                } else {
                    fail();
                }
            } else {
                fail();
            }
        } catch (Exception ex) {
            fail();
        }
    }
    
    public void success() {
        System.out.println((end - start));
        
        terminate();
    } 
    
    public void fail() {
        System.out.println(-1);
        
        terminate();
    }
    
    public void terminate() {
        sim.stop();
        client.destroy();
    }
}
