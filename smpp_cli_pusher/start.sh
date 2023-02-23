#!/bin/bash

java -jar target/smpptester-1.0.jar -h 127.0.0.1:2775 -u smppclient1 -p password -tc 1:1:791111234567 5:0:test ucs-2:'привет земляне!!!' 8
