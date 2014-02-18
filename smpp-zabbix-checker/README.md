#SMPP checker for Zabbix

##Usage

```
$ smpp-zabbix-checker <host> <port> <systemid> <password> <test abonent number> <server port>
```

This tool will run SMPP server on port (< server port >). Then this tool will connect with credentials ( < host > < port > < systemid > < password >) and send SMS message (submitsm) to SMPP-server. After that this tool will wait for back submitsm with parameters like sent message for check.

This tool will send message like this:

* Destination: < test abonent number >
* Source: zcheck
* Text: This is zabbix test message. Ignore it.1737385304

If error was occured during send operation (no bind from external server, no success bind to external server, malformed parameters in back submitsm or error in submitsmresp) will return -1. If operation was successfull then will return number of milliseconds took to send operation (from initial submitsm send till back submitsm received).

## Build

```
$ mvn clean install
```

## Installation

```
$ sudo dpkg -i target/smpp-zabbix-checker_1.0~SNAPSHOT_all.deb
```

## Run

```
$ smpp-zabbix-checker <host> <port> <systemid> <password> <test abonent number> <server port>
```

Example:
```
$ smpp-zabbix-checker 127.0.0.1 2775 test test 79121234567 3712
```

## Production example

By default Zabbix will wait for 3 seconds for task execution. So, you've to create cron to make check and zabbix will get data from local file.

```
$ crontab -l
*	*	*	*	*	smpp-zabbix-checker 127.0.0.1 2775 systemid password 79121234567 3712 > /tmp/smpp-zabbix-checker
$ echo 'UserParameter=smpp.checker[*],cat /tmp/smpp-zabbix-checker' > /etc/zabbix/zabbix_agentd.conf.d/smpp-zabbix-checker.cfg 
$ chown zabbix:zabbix /etc/zabbix/zabbix_agentd.conf.d/smpp-zabbix-checker.cfg 
$ /etc/init.d/zabbix-agent restart
```

Than you've to create data element in Zabbix and setup triggers.

In future release will be released command line argument for only check submit_sm status without waiting for back submitsm.
