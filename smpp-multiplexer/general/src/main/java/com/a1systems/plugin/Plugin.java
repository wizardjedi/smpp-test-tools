package com.a1systems.plugin;

/**
 * Base plugin interface. All plugins have to implement this interface.
 */
public interface Plugin {
    /**
     * This method will be invoked on loading plugin.
     */
    public void load();

    /**
     * This method will be invoked on starting plugin. For some plugins
     * {@code}start(){@code} method can be invoke just after {@code}load(){@code}
     * method.
     */
    public void start();


    /**
     * This method will be invoked on stopping plugin.
     */
    public void stop();

    /**
     * This method have to return plugin name and version.
     * @return plugin name and version
     */
    public String getDescription();
}
