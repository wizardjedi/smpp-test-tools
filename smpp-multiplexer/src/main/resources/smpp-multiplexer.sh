#!/bin/bash

cd /usr/share/smpp-multiplexer/

java -jar ${project.build.finalName} "$@"
