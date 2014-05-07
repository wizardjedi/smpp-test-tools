# Usage

smpp-simulator - simple tool for testing purposes. SMPP-simulator listen for incoming smpp connections and respond with success resps and also generates DELIVERY RECEIPTS. 

# TODO

## JavaScript handlers

smpp-simulator uses JDK's `ScriptEngine` to provide ability to create handlers with JS.

```
$ smpp-simulator -p 5000 -f handlers.js
```

handlers.js contains handlers written with JavaScript

## Examples

```JS
onSubmitSm=function(submitSm, session) {
  print("We've got SubmitSm:"+submitSm);
  return true;
};
```

```JS
onBindRequest=function(bindRequest) {
  if (bindRequest.getSystemId()=="test" && bindRequest.getPassword()=="test") {
    return true;
  } else {
    return false;
  }
};
onSessionCreated=function(session) {
  session.put("countSubmitSm",0);
};
onSubmitSm=function(submitSm, session){
  count = session.get("countSubmitSm");
  
  count++;
  
  session.put("countSubmitSm", count);
  
  print("Got submit sm number:"+count);
  
  dsm = session.getSimulator().createDeliveryReceipt(submitSm);
  
  dsm.setState("DELIVRD");
  
  session.getSimulator().scheduleDeliverySm(dsm);
  
  // return MSG_ID
  return count;
};
```

## Event handlers

* function onStart() - start handler for simulator
* function onBindRequest() - handler for bind request received
* function onSessionCreated() - handler for session have been created
* function onSubmitSm() - handler fired when SUBMIT_SM was received
* function onTick() - tick handler

### onStart()
### onBindRequest()
### onSessionCreated()
### onSubmitSm()
### onTick()

## Global objects

* Simulator - global object for simulator

## Classes

* Session - reperesents current session
* SubmitSm - represents received SUBMIT_SM
* DeliverSm - represents DELIVER_SM

### Session methods

* getSimulator() - get simulator object

### Simulator methods

* createDeliveryReceipt(SubmitSm) - helper method for creating delivery receipt object from SubmitSm
* scheduleDeliverySm(DeliverySm) - helper method for schedule sending of DELIVERY_SM









