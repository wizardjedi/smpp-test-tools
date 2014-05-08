#!/bin/bash

cd /usr/share/smpp-multiplexer/

java -Xms2048m -Xmx2048m -jar -Dlogback.configurationFile=etc/logback.xml ${project.build.finalName}.jar "$@"
