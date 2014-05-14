package com.a1systems.logwatcher;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.HtmlUtils;

class Application {

    public static final Logger logger = LoggerFactory.getLogger(Application.class);

    protected List<ProcessHolder> processList = new CopyOnWriteArrayList<ProcessHolder>();

    protected ScheduledExecutorService asyncPool = Executors.newScheduledThreadPool(2);

    public Application() {
        asyncPool.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                logger.debug("Clear task");

                final long currentMillis = System.currentTimeMillis();

                for (ProcessHolder ph:processList) {
                    if (currentMillis - ph.getLastProcessedMillis() > 10000 ) {
                        Process p = ph.getProcess();

                        logger.debug("Kill process {}", p);
                        p.destroy();

                        processList.remove(ph);
                    }
                }
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    public void start() {

    }

    public String processCommand(String cmd) throws IOException {
        Process p = null;

        ProcessHolder processHolder = new ProcessHolder();

        for (ProcessHolder ph:processList) {
            if (cmd.equals(ph.getCmd())) {
                p = ph.getProcess();
                processHolder = ph;
                break;
            }
        }

        if (p == null) {
            p = Runtime.getRuntime().exec(cmd);

            processHolder = new ProcessHolder();

            processHolder.setProcess(p);
            processHolder.setCmd(cmd);

            processList.add(processHolder);
        }

        processHolder.setLastProcessedMillis(System.currentTimeMillis());

        logger.debug("process:{}", p);

        InputStream inputStream = p.getInputStream();

        if (inputStream.available() > 0) {
            byte [] buffer = new byte[8192];

            inputStream.read(buffer);

            logger.debug("Stream.avail.bytes:{}",inputStream.available());

            String s = new String(buffer, Charset.forName("UTF-8"));

            return HtmlUtils.htmlEscape(s);
        } else {
            return "";
        }
    }
}
