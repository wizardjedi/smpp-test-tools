package com.a1systems.http.adapter.sender;

import com.a1systems.http.adapter.Application;

public class SenderTask implements Runnable {

    protected Application application;

    public SenderTask(Application application) {
        this.application = application;
    }

    @Override
    public void run() {
        while (true) {
            ;;
        }
    }

}
