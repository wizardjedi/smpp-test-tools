# Description

SMPP-Multiplexer - is a tool to multiplexing and load balancing smpp connections.

# Usage
```
$ smpp-multiplexer -p 5000 -e host1:port1,host2:port2
```

When client try to bind to port 5000 smpp-multiplexer will try to bound with the same system id and password to endpoints (host1 and host2). Packets received from client will be sent to endpoints with round-robin schema. 

Multiplexer can recognize multipart messages (concatinated with UDH) and route parts to the same endpoint.

Delivery receipts from endpoints will be routed to client.

Multiplexer have setup for hidden endpoints. These endpoints will be used only for routing delivery receipts from them to client and not submit_sm from client to endpoint. This kind of endpoints will be usefull for maintainance when you have to exclude endpoint from routing process.

Multiplexer has JMX for monitoring purposes.

Multiplexer can be used as SMPP-proxy (when set only single endpoint).

# Installation

Smpp-multiplexer is distributed with DEB-package. So, installation is pure simple:

```
$ dpkg -i smpp-multiplexer.deb
```

Smpp-multiplexer will be installed in `/usr/share/smpp-multiplexer/`.

# Directories

* `/usr/share/smpp-multiplexer/` - root-directory
* `/usr/share/smpp-multiplexer/log/` - log-directory with log rotating. By default, log files will be rotated by size (300 MB per file) and by date. Old files will be gzipped.
* `/usr/share/smpp-multiplexer/etc/` - etc folder. Contains properties files. Suchas logback.xml
* `/usr/share/smpp-multiplexer/plugins/` - directory contains JAR-files with plugins
* `/usr/share/smpp-multiplexer/lib/` - lib directory with dependencies

# Todo-list
* Setup endpoints with JMX
* Read configs from .properties-file
* Monitoring via JMX
* Impoved logging (log packets and bytes)
* Security logging
* Web-interface for monitoring and endpoint setup
