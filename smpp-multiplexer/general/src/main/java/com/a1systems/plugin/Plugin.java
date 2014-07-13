package com.a1systems.plugin;

public interface Plugin {
    public void load();

    public void start();

    public void stop();

    public String getDescription();
}
