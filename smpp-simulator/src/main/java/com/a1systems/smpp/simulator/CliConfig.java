package com.a1systems.smpp.simulator;

import org.kohsuke.args4j.Option;

class CliConfig {
    @Option(name = "-p", usage = "Port on which smpp server will listen. Required.", required = true)
    protected int port;
    
    @Option(name = "-f", usage = "Path to file contains handlers written in JavaScript")
    protected String fileName;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    
}
