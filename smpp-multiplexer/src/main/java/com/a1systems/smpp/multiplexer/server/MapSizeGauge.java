package com.a1systems.smpp.multiplexer.server;

import com.codahale.metrics.Gauge;
import java.util.Map;

public class MapSizeGauge implements Gauge<Integer>{

    protected Map map;
    
    public MapSizeGauge(Map map) {
        this.map = map;
    }

    @Override
    public Integer getValue() {
        return map.size();
    }
    
    
}
