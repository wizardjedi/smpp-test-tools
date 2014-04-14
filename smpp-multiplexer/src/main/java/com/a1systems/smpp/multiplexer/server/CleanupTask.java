package com.a1systems.smpp.multiplexer.server;


import java.lang.ref.WeakReference;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CleanupTask implements Runnable {

    protected static final Logger logger  = LoggerFactory.getLogger(CleanupTask.class);

    protected WeakReference<ConcurrentHashMap<String, MsgRoute>> map;

    public CleanupTask(ConcurrentHashMap<String, MsgRoute> msgMap) {
        this.map = new WeakReference<ConcurrentHashMap<String, MsgRoute>>(msgMap);
    }

    @Override
    public void run() {
        logger.debug("Started cleanup task");

        if (map.get() != null) {
            ConcurrentHashMap<String, MsgRoute> msgMap = map.get();

            for (Entry<String, MsgRoute> entry:msgMap.entrySet()) {
                MsgRoute route = entry.getValue();

                if (route.getCreateDate().plusMinutes(3).isBeforeNow()) {
                    logger.debug("Remove entry:{}", entry.getKey());

                    msgMap.remove(entry.getKey());
                }
            }
        }
    }


}
