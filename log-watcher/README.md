# Description

log-watcher - simple tool for display logs from remote machines

log-watcher use AJAX to retrieve data from remote host

# Usage

Clean, build and run.

```
$ mvn clean install && java -jar target/log-watcher-1.0-SNAPSHOT.jar
20140514233807 Loading XML bean definitions from class path resource [spring-context.xml]
20140514233808 Refreshing org.springframework.context.support.GenericXmlApplicationContext@7c16905e: startup date [Wed May 14 23:38:08 MSK 2014]; root of context hierarchy
20140514233808 Logging initialized @975ms
20140514233808 jetty-9.1.4.v20140401
20140514233808 Started o.e.j.s.ServletContextHandler@c86b9e3{/,null,AVAILABLE}
20140514233808 Started ServerConnector@d21a74c{HTTP/1.1}{0.0.0.0:8182}
20140514233808 Started @1208ms

```

Run browser and go to http://127.0.0.1:8182/

Just enter command to execute and see output to window.

You can set regular expression to hightlight with red bold font.
