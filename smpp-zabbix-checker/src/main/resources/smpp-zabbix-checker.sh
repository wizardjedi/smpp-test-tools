#!/bin/bash

cd /usr/share/smpp-zabbix-checker/

java -Dorg.slf4j.simpleLogger.defaultLogLevel=error -jar smpp-zabbix-checker-1.0-SNAPSHOT.jar "$@"