package com.a1systems.plugins.dbauth;

import com.a1systems.plugin.Authorizer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBAuthorizerPlugin implements Authorizer {

    public static Logger logger = LoggerFactory.getLogger(DBAuthorizerPlugin.class);

    protected volatile ConcurrentHashMap<String, Boolean> logins = new ConcurrentHashMap<String, Boolean>();

    protected ScheduledExecutorService asyncPool = Executors.newScheduledThreadPool(2);

    protected DataSource datasource;

    @Override
    public boolean auth(String systemId, String password) {
        String key = systemId + "/" + password;

        if (logins.containsKey(key) && logins.get(key).equals(true)) {
            logger.info("DBAuthPLugin Connection {}:{} found", systemId, password);

            return true;
        }

        logger.info("DBAuthPLugin Connection {}:{} not found", systemId, password);

        return false;
    }

    @Override
    public void load() {
        logger.info("Plugin " + getDescription() + " loaded");
    }

    @Override
    public void start() {
        PoolProperties p = new PoolProperties();
        p.setUrl("jdbc:mysql://localhost:3306/bill_primary");
        p.setDriverClassName("com.mysql.jdbc.Driver");
        p.setUsername("root");
        p.setPassword("");
        p.setJmxEnabled(true);
        p.setTestWhileIdle(false);
        p.setTestOnBorrow(true);
        p.setValidationQuery("SELECT 1");
        p.setTestOnReturn(false);
        p.setValidationInterval(30000);
        p.setTimeBetweenEvictionRunsMillis(30000);
        p.setMaxActive(100);
        p.setInitialSize(10);
        p.setMaxWait(10000);
        p.setRemoveAbandonedTimeout(60);
        p.setMinEvictableIdleTimeMillis(30000);
        p.setMinIdle(10);
        p.setLogAbandoned(true);
        p.setRemoveAbandoned(true);
        p.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"
            + "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
        datasource = new DataSource();
        datasource.setPoolProperties(p);

        asyncPool.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                Connection con = null;
                try {
                    con = datasource.getConnection();
                    Statement st = con.createStatement();
                    ResultSet rs = st.executeQuery("select * from channels where api_type='smpp' and allowed=1;");
                    int cnt = 1;

                    ConcurrentHashMap<String, Boolean> concurrentHashMap = new ConcurrentHashMap<String, Boolean>();

                    while (rs.next()) {
                        concurrentHashMap.put(rs.getString("api_login")+"/"+rs.getString("api_password"), Boolean.TRUE);

                        cnt++;
                    }

                    logins = concurrentHashMap;

                    logger.info("DBAuthPLugin {} connections loaded", cnt);

                    rs.close();
                    st.close();
                } catch (SQLException ex) {
                    /* */
                } finally {
                    if (con != null) {
                        try {
                            con.close();
                        } catch (Exception ignore) {
                        }
                    }
                }

            }
        }, 0, 30, TimeUnit.SECONDS);

        logger.info("Plugin " + getDescription() + " started");
    }

    @Override
    public void stop() {
        logger.info("Plugin " + getDescription() + " stopped");
    }

    @Override
    public String getDescription() {
        return DBAuthorizerPlugin.class.getCanonicalName() + " 1.0";
    }

}
