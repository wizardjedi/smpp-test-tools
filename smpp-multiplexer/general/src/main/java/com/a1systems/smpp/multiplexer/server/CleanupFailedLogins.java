package com.a1systems.smpp.multiplexer.server;

import static com.a1systems.smpp.multiplexer.server.CleanupTask.logger;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CleanupFailedLogins implements Runnable {
    public static final Logger logger = LoggerFactory.getLogger(CleanupFailedLogins.class);
    
    protected WeakReference<ConcurrentHashMap<String, DateTime>> failedLogins;
    
    public CleanupFailedLogins(ConcurrentHashMap<String, DateTime> failedLogins) {
        this.failedLogins =  new WeakReference<ConcurrentHashMap<String, DateTime>>(failedLogins);
    }

    @Override
    public void run() {
        logger.info("Clean up failed logins started");
        
        if (failedLogins.get() != null) {
            ConcurrentHashMap<String, DateTime> failedLoginsMap = failedLogins.get();

            int dropped=0;
            
            for (Map.Entry<String, DateTime> entry:failedLoginsMap.entrySet()) {
                if (entry.getValue().plusMinutes(3).isBeforeNow()) {
                    logger.debug("Remove entry:{}", entry.getKey());

                    failedLoginsMap.remove(entry.getKey());
                    
                    dropped++;
                }
            }
            
            logger.info("Removed {} failed logins", dropped);
        } else {
            logger.info("Nothing clean up in failed logins");
        }
        
        logger.info("Clean up failed logins completed");
    }
    
    
}
