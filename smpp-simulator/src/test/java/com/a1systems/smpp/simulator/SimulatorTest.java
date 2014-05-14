package com.a1systems.smpp.simulator;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.util.DeliveryReceipt;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class SimulatorTest {
    protected Simulator sim;
    
    @Before
    public void setUp() {
        sim = new Simulator();
    }
    
    @After
    public void tearDown() {
        sim = null;
    }
    
    @Test
    public void testCreateDeliveryReceipt() throws Exception {
        SubmitSm sm = new SubmitSm();
        
        sm.setDestAddress(new Address((byte)1, (byte)2, "79111234567"));
        sm.setSourceAddress(new Address((byte)3, (byte)4, "79121234567"));
        
        sm.setShortMessage(CharsetUtil.encode("Hello there", CharsetUtil.CHARSET_GSM7));
        
        DeliverSm dsm = sim.createDeliveryReceipt(sm);
        
        Assert.assertEquals(sm.getSourceAddress().getTon(), dsm.getDestAddress().getTon());
        Assert.assertEquals(sm.getSourceAddress().getNpi(), dsm.getDestAddress().getNpi());
        Assert.assertEquals(sm.getSourceAddress().getAddress(), dsm.getDestAddress().getAddress());
        
        Assert.assertEquals(sm.getDestAddress().getTon(), dsm.getSourceAddress().getTon());
        Assert.assertEquals(sm.getDestAddress().getNpi(), dsm.getSourceAddress().getNpi());
        Assert.assertEquals(sm.getDestAddress().getAddress(), dsm.getSourceAddress().getAddress());
        
        Assert.assertEquals(SmppConstants.ESM_CLASS_MT_SMSC_DELIVERY_RECEIPT, dsm.getEsmClass());
    }

    @Test
    public void testSetUpDeliveryReceipt() throws Exception {
        SubmitSm sm = new SubmitSm();
        
        sm.setDestAddress(new Address((byte)1, (byte)2, "79111234567"));
        sm.setSourceAddress(new Address((byte)3, (byte)4, "79121234567"));
        
        sm.setShortMessage(CharsetUtil.encode("Hello there", CharsetUtil.CHARSET_GSM7));
        
        DeliverSm dsm = sim.createDeliveryReceipt(sm);
        
        DeliverSm dsmr = sim.setUpDeliveryReceipt(dsm, "123456789", "EXPIRED", "2014-04-15T11:10:20", "2014-04-15T12:01:02", 123);
        
        byte[] shortMessage = dsmr.getShortMessage();
        
        String decodedShortMessage = CharsetUtil.decode(shortMessage, CharsetUtil.CHARSET_GSM8);
        
        DeliveryReceipt deliverReceipt = DeliveryReceipt.parseShortMessage(decodedShortMessage, DateTimeZone.getDefault());
        
        Assert.assertEquals(SmppConstants.STATE_EXPIRED,deliverReceipt.getState());
        Assert.assertEquals(1,deliverReceipt.getSubmitCount());
        Assert.assertEquals(1,deliverReceipt.getDeliveredCount());
        Assert.assertEquals(123,deliverReceipt.getErrorCode());
        Assert.assertEquals("123456789",deliverReceipt.getMessageId());
        // Time precise to minutes
        Assert.assertEquals(DateTime.parse("2014-04-15T11:10:00"),deliverReceipt.getSubmitDate());
        Assert.assertEquals(DateTime.parse("2014-04-15T12:01:00"),deliverReceipt.getDoneDate());
    }
}