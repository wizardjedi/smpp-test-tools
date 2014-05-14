package com.a1systems.logwatcher;

public class ProcessHolder {
    protected Process process;
    protected long lastProcessedMillis;
    protected String cmd;

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public long getLastProcessedMillis() {
        return lastProcessedMillis;
    }

    public void setLastProcessedMillis(long lastProcessedMillis) {
        this.lastProcessedMillis = lastProcessedMillis;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }
}
