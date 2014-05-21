# Usage

smpp-simulator - simple tool for testing purposes. SMPP-simulator listen for incoming smpp connections and respond with success resps and also generates DELIVERY RECEIPTS. 

# JavaScript handlers

smpp-simulator uses JDK's `ScriptEngine` to provide ability to create handlers with JS.

Example:
```
$ smpp-simulator -p 5000 -f handlers.js -D const1=1 -D const2=2 -t 500
```

This command will start smpp-simulator on port 5000 (`-p 5000`) with JS-handlers placed in `handlers.js` (`-f handlers.js`). On tick handler will be invoked for every 500 milliseconds (`-t 500`). This command will place 2 variables to global script context (`-D const1=1 -D const2=2`):
 * const1 = 1
 * const2 = 2

handlers.js contains handlers written with JavaScript. 

# Examples

Create SMPP-simulator with logging and writing PDU's to file. Reply submitsmresp with increased int numbers.

```
$ java -jar target/smpp-simulator-1.0-SNAPSHOT.jar -p 2775 -f 3.js -D messageStep=10
```

3.js

```JS
// Print banner on file loaded
print("file loaded");

// Print arguments passed from command line (with -D)
Logger.error("Argument Map:[{}]", argumentMap);

// Create print writer with Java io API
writer = new java.io.PrintWriter("log_file.csv", "UTF-8");

// Handler for bid request
function onBindRequest(cfg, request) {
  // Log system id and password
	Logger.error("SystemId:{} Password:{}", cfg.getSystemId(), cfg.getPassword());

  // check for systemid and password
	if (cfg.getSystemId() == "test1") {
	  // bind_resp OK
		return 0;
	} else {
	  // bind_resp bind_failed
		return 13;
	}
}

// Handler for session created
function onSessionCreated(simulatorSession) {
	Logger.info("Session have been created");
}

// Handler for session destroyed
function onSessionDestroyed(session) {
	Logger.error("--->Session destroyed:{}",session);
}

// Pdu request handler
function onPduRequest(simulatorSession, req) {
  // Get number of pdu in current session
	num = simulatorSession.incrementCounterAndGet();
	// Log PDU and count
	Logger.info("Got PDU:{} num:{}", req, num);

  // Write PDU to file
	writer.println(req);
	writer.println("---");
	writer.flush();
	
	// create response for PDU request
	resp = req.createResponse();

  // custom processing for SubmitSM
	if (req instanceof com.cloudhopper.smpp.pdu.SubmitSm) {
	  // log message text
		Logger.error("-------->{}", com.cloudhopper.commons.charset.CharsetUtil.decode(req.getShortMessage(),"GSM8"));

    // Generate message id with command line argument (-D messageStep=10)
		msgId = num*messageStep;

    // set message id to resp
		resp.setMessageId(msgId);

    // get simulator
		sim = simulatorSession.getSimulator();
		// create delivery sm from submit sm
		dsm = sim.createDeliveryReceipt(req);

    // set up delivery receipt to deliver sm
		dsm = sim.setUpDeliveryReceipt(dsm,msgId,"DELIVRD","2014-04-01T11:00:00","2014-04-01T12:02:02",0);

    // schedule deliver sm after 5000 milliseconds
		sim.scheduleDeliverySm(dsm,simulatorSession.getSession(),5000);
		
	}
	
	// return resp
	return resp; 
}

// channel closed handler
function onChannelClosed(simulatorSession) {
	Logger.error("Channel unexpectedly closed {}", simulatorSession.getSession());
}
```

# Event handlers

* onBindRequest
* onSessionCreated
* onSessionDestroyed
* onPduRequest
* onChannelClosed
* onStart
* onTick

# Global objects

* ScriptLogger - script logger (instance of slf4j Logger)

# Classes

* SimulatorSession - reperesents current session (session wrapper)
* SubmitSm - represents received SUBMIT_SM (from com.cloudhopper.smpp.pdu)
* DeliverSm - represents DELIVER_SM (from com.cloudhopper.smpp.pdu)

## SimulatorSession methods

* getSimulator() - get simulator object
* getMap() - get map to store objects
* put(key, val) - store object to map
* get(key) - get object from map
* incrementCounterAndGet() - increment and get value from counter
* decrementCounterAndGet() - decrement and get counter
* addCounterAndGet(delta) - add and get counter
* containsKey(key) - is object contains in map
* remove(key) - remove object by key from map
* clear() - clear map
* contains(value) - is object contains in map

## Simulator methods

* createDeliveryReceipt(SubmitSm) - helper method for creating delivery receipt object (DELIVER_SM) from SubmitSm
* setUpDeliveryReceipt(DeliverSm dsm, String messageId, String status, String sendDate, String deliveryDate, int errorCode)
* setUpDeliveryReceipt(DeliverSm dsm, String messageId, String status, DateTime sendDate, DateTime deliveryDate, int errorCode)
* scheduleDeliverySm(DeliverySm, Session, delayMillis) - helper method for schedule sending of DELIVERY_SM to session with specified delay in milliseconds

# Performance

Performance test was executed with smpp-load (https://github.com/PowerMeMobile/smppload)

Laptop hardware and versions

```
$ free -m
             total
mem:       7802

$ ./smppload -H 127.0.0.1 -P 2775 -u test1 -p test -d 79111234567 -s 79121234567 -c 50000 -r 2000 -v 1
INFO:  Connected to 127.0.0.1:2775
INFO:  Bound to cloudhopper
INFO:  Stats:
INFO:     Send success:     50000
INFO:     Delivery success: 0
INFO:     Send fail:        0
INFO:     Delivery fail:    0
INFO:     Errors:           0
INFO:     Avg Rps:          1847 mps
INFO:  Unbound
```


# TODO
* add more handlers
* add tests
* test with JDK 7, JDK 6, OpenJDK 6, OpenJDK 7
* add setup for logging
* add deb-package creating
* add GC-logging
* add Xmx, Xms
* add reload file with handlers 





