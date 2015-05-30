package com.a1systems.smpp.simulator;

import java.util.HashMap;
import java.util.Map;
import org.kohsuke.args4j.Option;

class CliConfig {
    @Option(name = "-p", usage = "Port on which smpp server will listen. Required.", required = true)
    protected int port;

    @Option(name = "-f", usage = "Path to file contains handlers written in JavaScript")
    protected String fileName;

    @Option(name = "-D", usage = "Add arguments for scripts. Example, -D arg1=test -D arg2=test2")
    protected Map<String, String> map = new HashMap<String, String>();

    @Option(name = "-t", usage = "Set tick delay in milliseconds. Default value is 1000 millis")
    protected int tickDelay = 1000;

    public int getTickDelay() {
        return tickDelay;
    }

    public void setTickDelay(int tickDelay) {
        this.tickDelay = tickDelay;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }

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
