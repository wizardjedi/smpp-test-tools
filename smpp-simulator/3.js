print("file loaded");

Logger.error("Argument Map:[{}]", argumentMap);

writer = new java.io.PrintWriter("log_file.csv", "UTF-8");

function onStart() {
	print("On start handler");

}

function onTick(simSession) {
	Logger.error("Tick");

	session = simSession.getSession();

	simulator = simSession.getSimulator();

	dsm = simulator.createDeliverSm();

	dsm.setSourceAddress(simulator.createAddress("79111234567",1,1));
	dsm.setDestAddress(simulator.createAddress("79111234568",1,1));

	dsm.setShortMessage(simulator.encode("Hello there", "UCS-2"));

	session.sendRequestPdu(dsm, 60000, false);
}

function onBindRequest(cfg, request) {
	Logger.error("SystemId:{} Password:{}", cfg.getSystemId(), cfg.getPassword());

	if (cfg.getSystemId() == "test1") {
		return 0;
	} else {
		return 13;
	}
}

function onSessionCreated(simulatorSession) {
	Logger.info("Session have been created");
}


function onSessionDestroyed(session) {
	Logger.error("--->Session destroyed:{}",session);
}

function onPduRequest(simulatorSession, req) {
	num = simulatorSession.incrementCounterAndGet();
	Logger.info("Got PDU:{} num:{}", req, num);

	writer.println(req);
	writer.println("---");
	writer.flush();
	resp = req.createResponse();

	if (req instanceof com.cloudhopper.smpp.pdu.SubmitSm) {
		Logger.error("-------->{}", com.cloudhopper.commons.charset.CharsetUtil.decode(req.getShortMessage(),"GSM8"));

		msgId = num*messageStep;

		resp.setMessageId(msgId);

		sim = simulatorSession.getSimulator();
		dsm = sim.createDeliveryReceipt(req);

		dsm = sim.setUpDeliveryReceipt(dsm,msgId,"DELIVRD","2014-04-01T11:00:00","2014-04-01T12:02:02",0);

		sim.scheduleDeliverySm(dsm,simulatorSession.getSession(),5000);
		
	}
	
	return resp; 
}

function onChannelClosed(simulatorSession) {
	Logger.error("Channel unexpectedly closed {}", simulatorSession.getSession());
}
