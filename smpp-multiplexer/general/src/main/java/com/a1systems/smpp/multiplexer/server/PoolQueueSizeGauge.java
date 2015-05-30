package com.a1systems.smpp.multiplexer.server;

import com.codahale.metrics.Gauge;
import java.util.concurrent.ThreadPoolExecutor;

public class PoolQueueSizeGauge implements Gauge<Integer>{

    protected ThreadPoolExecutor pool;
    
    public PoolQueueSizeGauge(ThreadPoolExecutor pool) {
        this.pool = pool;
    }

    @Override
    public Integer getValue() {
        return pool.getQueue().size();
    }
    
    
}
