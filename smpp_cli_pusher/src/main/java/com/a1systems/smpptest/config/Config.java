package com.a1systems.smpptest.config;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import java.util.List;

public class Config {

    @Option(name = "-h", usage = "Set host:port for connection")
    protected String hostPort;

    @Option(name = "-u", usage = "Set system id")
    protected String systemId;

    @Option(name = "-p", usage = "Set password")
    protected String password;

    @Option(name = "-r", usage = "Use receiver bind mode (NOT CURRENTLY SUPPORTED)")
    protected boolean receiver;

    @Option(name = "-t", usage = "Use transmitter bind mode (NOT CURRENTLY SUPPORTED)")
    protected boolean transmitter;

    @Option(name = "-tc", usage = "Use transceiver bind mode (NOT CURRENTLY SUPPORTED)")
    protected boolean transceiver = true;

    @Option(name = "-stdin", usage = "Read data from STDIN (NOT CURRENTLY SUPPORTED)")
    protected boolean stdin;

    @Option(name = "-example", usage = "Print examples of packets. (NOT CURRENTLY SUPPORTED)")
    protected boolean example;

    @Option(name = "-exec", usage = "Exec command on packet receive. (NOT CURRENTLY SUPPORTED)")
    protected boolean exec;

    @Option(name = "-w", usage = "Wait")
    protected boolean wait;

    @Argument
    protected List<String> arguments;

    public boolean isReceiver() {
        return receiver;
    }

    public void setReceiver(boolean receiver) {
        this.receiver = receiver;
    }

    public boolean isTransmitter() {
        return transmitter;
    }

    public void setTransmitter(boolean transmitter) {
        this.transmitter = transmitter;
    }

    public boolean isWait() {
        return wait;
    }

    public void setWait(boolean wait) {
        this.wait = wait;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public boolean isExample() {
        return example;
    }

    public void setExample(boolean example) {
        this.example = example;
    }

    public boolean isExec() {
        return exec;
    }

    public void setExec(boolean exec) {
        this.exec = exec;
    }

    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    public boolean isTransceiver() {
        return transceiver;
    }

    public void setTransceiver(boolean transceiver) {
        this.transceiver = transceiver;
    }

    public String getHostPort() {
        return hostPort;
    }

    public void setHostPort(String hostPort) {
        this.hostPort = hostPort;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isStdin() {
        return stdin;
    }

    public void setStdin(boolean stdin) {
        this.stdin = stdin;
    }

}
