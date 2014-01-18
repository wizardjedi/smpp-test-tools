package com.a1systems.http.adapter;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicLong;

public class IdGenerator {

    protected AtomicLong id = new AtomicLong();

    protected Long step;

    public IdGenerator(Long id, Long step) {
        this.step = step;

        if (id != null) {
            this.id.set(id);
        } else {
            Calendar date = Calendar.getInstance();

            this.id.set(date.getTimeInMillis()*1000*step);
        }
    }

    public Long generate() {
        return this.id.addAndGet(this.step);
    }
}
