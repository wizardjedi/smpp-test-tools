package com.a1systems.smpp.multiplexer;

import org.kohsuke.args4j.Option;

public class CliConfig {
    @Option(name = "-p", usage = "Port on which smpp server will listen")
    protected Integer port;

    @Option(name = "-e", usage = "Endpoints to proxy client requests (host1:port1,host2:port2,...)")
    protected String endPoints;

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getEndPoints() {
        return endPoints;
    }

    public void setEndPoints(String endPoints) {
        this.endPoints = endPoints;
    }
}
