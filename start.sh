#!/bin/bash

java -jar target/smpptester-1.0.jar -wait -h 127.0.0.1:2775 -u smppclient1 -p password -tc -rebindperiod 30 -elinkperiod 2 -speed 1
