#!/bin/bash

cat $1 | awk -f optimizer.awk 
