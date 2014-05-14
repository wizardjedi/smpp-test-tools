package com.a1systems.smpp.simulator;

import com.cloudhopper.smpp.SmppSession;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class SimulatorSession {
    protected ConcurrentHashMap<String,Object> map = new ConcurrentHashMap<String, Object>();
    
    protected Simulator simulator;
    
    protected SmppSession session;

    protected AtomicLong counter = new AtomicLong(0);
    
    public long incrementCounterAndGet() {
        return counter.incrementAndGet();
    }

    public long decrementCounterAndGet() {
        return counter.decrementAndGet();
    }

    public long addCounterAndGet(long delta) {
        return counter.addAndGet(delta);
    }
    
    public Simulator getSimulator() {
        return simulator;
    }

    public void setSimulator(Simulator simulator) {
        this.simulator = simulator;
    }
    
    public ConcurrentHashMap<String, Object> getMap() {
        return map;
    }

    public void setMap(ConcurrentHashMap<String, Object> map) {
        this.map = map;
    }

    public SmppSession getSession() {
        return session;
    }

    public void setSession(SmppSession session) {
        this.session = session;
    }

    public Object get(Object key) {
        return map.get(key);
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public Object put(String key, Object value) {
        return map.put(key, value);
    }

    public Object remove(Object key) {
        return map.remove(key);
    }

    public void clear() {
        map.clear();
    }

    public boolean contains(Object value) {
        return map.contains(value);
    }
}
