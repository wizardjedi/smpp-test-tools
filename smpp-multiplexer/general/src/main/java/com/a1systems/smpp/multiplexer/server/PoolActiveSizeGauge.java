package com.a1systems.smpp.multiplexer.server;

import com.codahale.metrics.Gauge;
import java.util.concurrent.ThreadPoolExecutor;

public class PoolActiveSizeGauge implements Gauge<Integer>{

    protected ThreadPoolExecutor pool;
    
    public PoolActiveSizeGauge(ThreadPoolExecutor pool) {
        this.pool = pool;
    }

    @Override
    public Integer getValue() {
        return pool.getActiveCount();
    }
    
    
}
