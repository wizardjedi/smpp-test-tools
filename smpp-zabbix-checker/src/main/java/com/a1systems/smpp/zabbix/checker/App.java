package com.a1systems.smpp.zabbix.checker;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.BindTransceiverResp;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.simulator.SmppSimulatorServer;
import com.cloudhopper.smpp.simulator.SmppSimulatorSessionHandler;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.LoggingOptions;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import com.cloudhopper.smpp.type.SmppProcessingException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App 
{
    public static void main( String[] args )
    {
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        
        String systemId = args[2];
        String password = args[3];
        
        String testAbonent = args[4];
        
        int serverPort = Integer.parseInt(args[5]);
        
        SmppSimulatorServer sim = new SmppSimulatorServer();
        
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
        
        DefaultSmppClient client = new DefaultSmppClient();
        try {
            SmppSimulatorSessionHandler serverSession = sim.pollNextSession(TimeUnit.SECONDS.toMillis(30));
            BindTransceiverResp resp = new BindTransceiverResp();
            
            Pdu bindPdu = serverSession.pollNextPdu(TimeUnit.SECONDS.toMillis(30));
            
            resp.setSequenceNumber(bindPdu.getSequenceNumber());
            
            resp.setSystemId("");
            
            resp.setCommandLength(17);
            
            serverSession.sendPdu(resp);
            
            TimeUnit.SECONDS.sleep(2);
            
            long start = System.currentTimeMillis();
            
            SmppSession session = client.bind(config);
            
            SubmitSm sm = new SubmitSm();
            
            Random r = new Random(System.currentTimeMillis());
            
            String body = "This is zabbix test message. Ignore it."+r.nextInt();
            
            sm.setShortMessage(CharsetUtil.encode(body,CharsetUtil.CHARSET_GSM8));
            
            sm.setDestAddress(new Address((byte)1, (byte)1, testAbonent));
            sm.setSourceAddress(new Address((byte)5, (byte)0, "zcheck"));
            SubmitSmResp submitSmResp = session.submit(sm, TimeUnit.SECONDS.toMillis(30));
            
            long end = System.currentTimeMillis();
            
            if (submitSmResp.getCommandStatus() != 0) {
                System.out.println(-1);
                System.exit(0);
            }
            
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
                
                 System.out.println((end - start));
                 System.exit(0);
            }
            
            System.out.println(-1);
            System.exit(0);
        } catch (SmppTimeoutException ex) {
            System.out.println(-1);
            System.exit(0);
        } catch (SmppChannelException ex) { 
            System.out.println(-1);
            System.exit(0);
        } catch (UnrecoverablePduException ex) {
            System.out.println(-1);
            System.exit(0);                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     
        } catch (InterruptedException ex) {
            System.out.println(-1);
            System.exit(0);
        } catch (SmppInvalidArgumentException ex) {
            System.out.println(-1);
            System.exit(0);
        } catch (RecoverablePduException ex) {
            System.out.println(-1);
            System.exit(0);
        } catch (Exception ex) {
            System.out.println(-1);
            System.exit(0);
        }
    }
}
