smpp-test-tools
===============

Testing tools for smpp 3.4 based on Twitter cloudhopper library.

Today it's single threaded smpp test tool. It can send submit_sm's with parameters 

Creating DEB-package
====================

```
$ mvn install
```

After packaging you can find you deb-package in target directory

Installing from DEB-package
===========================

```
$ mvn clean install
$ sudo dpkg -i target/smpp_tester_1.0.deb
```

After installation:
 * will be created link to smpptester in /usr/bin/
 * will be created filder /usr/share/smpptester
 * you can use smpptester command to send messages


Usage
=====

```
$ smpptester -h host:port -u user -p password ton:npi:destination ton:npi:source coding:text dcs
```

Example
=======

```
$ smpptester -h 127.0.0.1:2775 -u test -p test 1:1:79111234567 '5:0:test sender' 'gsm8:Hello this is test message' 0
```

Multipart messages will send automatically.

Available values for parameters:
 * coding : gsm8 - GSM 8 bit, gsm7 - GSM 7 bit, ucs-2 - UCS-2BE for unicode, hex - hexdump of message
 * dcs - 0,4,8
