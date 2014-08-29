#!/bin/bash

cd /usr/share/smpp-multiplexer/

java -Xms2048m -Xmx2048m -Xloggc:log/gc.log -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=128K -jar -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9011 -Dcom.sun.management.jmxremote.authenticate=false -Djava.rmi.server.hostname=`hostname -i` -Dcom.sun.management.jmxremote.ssl=false -Dlogback.configurationFile=etc/logback.xml ${project.build.finalName}.jar "$@"
