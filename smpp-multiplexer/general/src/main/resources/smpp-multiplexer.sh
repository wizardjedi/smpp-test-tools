#!/bin/bash

cd /usr/share/smpp-multiplexer/

java -cp plugins/* -Xms2048m -Xmx2048m -Xloggc:log/gc.log -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=128K -jar -Dlogback.configurationFile=etc/logback.xml ${project.build.finalName}.jar "$@"
