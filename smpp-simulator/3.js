function onBindRequest(cfg, request) {
	Logger.error("SystemId:{} Password:{}", cfg.getSystemId(), cfg.getPassword());

	if (cfg.getSystemId() == "test1") {
		return 0;
	} else {
		return 13;
	}
}

function onSessionCreated(session) {
	Logger.info("Session have been created");
}


function onSessionDestroyed(session) {
	Logger.error("--->Session destroyed:{}",session);
}

function onPduRequest(simulatorSession, req) {
	Logger.info("Got PDU:{} num:{}", req, simulatorSession.incrementCounterAndGet());

	throw new Error("++++");
}

function onChannelClosed(simulatorSession) {
	Logger.error("Channel unexpectedly closed {}", simulatorSession.getSession());
}
