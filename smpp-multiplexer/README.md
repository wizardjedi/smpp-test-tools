SMPP-Multiplexer - is a tool to multiplexing and load balancing smpp connections.

Usage:
```
$ smpp-multiplexer -p 5000 -e host1:port1,host2:port2
```

When client try to bind to port 5000 smpp-multiplexer will try to bound with the same system id and password to endpoints (host1 and host2). Packets received from client will be sent to endpoints with round-robin schema. 

Multiplexer can recognize multipart messages (concatinated with UDH) and route parts to the same endpoint.

Delivery receipts from endpoints will be routed to client.

Multiplexer have setup for hidden endpoints. These endpoints will be used only for routing delivery receipts from them to client and not submit_sm from client to endpoint. This kind of endpoints will be usefull for maintainance when you have to exclude endpoint from routing process.

Multiplexer has JMX for monitoring purposes.
