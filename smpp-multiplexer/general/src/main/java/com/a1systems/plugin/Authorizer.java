package com.a1systems.plugin;

public interface Authorizer extends Plugin {
    public boolean auth(String systemId, String password);
}
