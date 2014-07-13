This folder contains 3rd party plugins for multiplexer.

SMPP-multiplexer provides very simple plugin system.

All plugins have to implement `com.a1systems.plugin.Plugin` interface.

`Plugin interface`
```java
public interface Plugin {
    public void load();

    public void start();

    public void stop();

    public String getDescription();
}
```

 * `load()` - this method will invoke on plugin loading
 * `start()` - this method will invoke on plugin start
 * `getDescription()` - this method will invoke to get information about plugin

To create your own plugin you have to implement interface(s) such as:

 * `com.a1systems.plugin.Authorizer` - authorization plugin

SMPP-multiplexer plugin system built oninterfaces. So, SMPP-multiplexer have some interfaces inherited from `Plugin`. SMPP-mux uses this interfaces in connection points.

Plugins will load with ServiceLoader, so your plugin have to provide service file with you plugin class name in `META-INF/services` folder.

For example `dbauth`:
`META-INF/services/com.a1systems.plugin.Authorizer`
```
com.a1systems.plugins.dbauth.DBAuthorizerPlugin
```
