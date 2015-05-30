package com.a1systems.plugins.dbauth;

import com.a1systems.plugin.Authorizer;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBAuthorizerPlugin implements Authorizer{

    public static Logger logger = LoggerFactory.getLogger(DBAuthorizerPlugin.class);

    protected volatile ConcurrentHashMap<String, Boolean> logins = new ConcurrentHashMap<String, Boolean>();

    @Override
    public boolean auth(String systemId, String password) {
        String key = systemId+"/"+password;

        if (logins.containsKey(key) && logins.get(key).equals(true)) {
            return true;
        }

        return false;
    }

    @Override
    public void load() {
        logger.info("Plugin "+getDescription()+" loaded");

        logins.put("test1/test1", Boolean.TRUE);
    }

    @Override
    public void start() {
        logger.info("Plugin "+getDescription()+" started");
    }

    @Override
    public void stop() {
        logger.info("Plugin "+getDescription()+" stopped");
    }

    @Override
    public String getDescription() {
        return DBAuthorizerPlugin.class.getCanonicalName()+" 1.0";
    }

}
