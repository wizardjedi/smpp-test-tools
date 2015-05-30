package com.a1systems.smpp.multiplexer;

import com.a1systems.smpp.multiplexer.client.Client;
import com.a1systems.smpp.multiplexer.server.SmppServerHandlerImpl;
import com.a1systems.smpp.multiplexer.server.SmppServerSessionHandler;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionCounters;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.impl.DefaultSmppServerCounters;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportTask implements Runnable {

    public static final Logger log = LoggerFactory.getLogger(ReportTask.class);
    
    protected Application app;
    
    public ReportTask(Application app) {
        this.app = app;
    }
    
    @Override
    public void run() {
        log.info("Report task started");
        
        StringBuilder sb = new StringBuilder("Report task:\n");
        
        ThreadPoolExecutor mainPool = ((ThreadPoolExecutor)app.getPool());
        
        ScheduledThreadPoolExecutor asyncPool = (ScheduledThreadPoolExecutor)app.getAsyncPool();
        ScheduledThreadPoolExecutor monitorPool = (ScheduledThreadPoolExecutor)app.getMonitorPool();
        
        sb
            .append("Pool size:")
            .append(mainPool.getQueue().size())
            .append("\n");
        sb
            .append("Pool active:")
            .append(mainPool.getActiveCount())
            .append("\n");
        
        sb
            .append("Async pool:")
            .append(asyncPool.getQueue().size())
            .append("\n");
        
        sb
            .append("Async active:")
            .append(asyncPool.getActiveCount())
            .append("\n");
        
        sb
            .append("Monitor pool:")
            .append(monitorPool.getQueue().size())
            .append("\n");
        sb
            .append("Monitor active:")
            .append(monitorPool.getActiveCount())
            .append("\n");
        
        SmppServerHandlerImpl serverHandler = app.getServerHandler();
        
        ConcurrentHashMap<Long, SmppServerSessionHandler> handlers = serverHandler.getHandlers();
        
        sb
            .append("Session handlers:")
            .append(handlers.size())
            .append("\n");
        
        DefaultSmppServer server = app.getServer();
        
        sb
            .append("Server channel size:")
            .append(server.getChannels().size())
            .append("\n")
            .append("Channels connected:")
            .append(server.getChannelConnects())
            .append("\n")
            .append("Channels disconnected:")
            .append(server.getChannelDisconnects())
            .append("\n")
            .append("Connection size:")
            .append(server.getConnectionSize())
            .append("\n");
                
        sb
            .append("Client Nio Group events count:")
            .append(countEvents(app.getClientGroup()))
            .append("\n");
            
        sb
            .append("Nio Group events count:")
            .append(countEvents(app.getNioGroup()))
            .append("\n");
        
        DefaultSmppServerCounters counters = server.getCounters();
        
        sb
            .append("Session size:")
            .append(counters.getSessionSize())
            .append("\n")
            .append("Session created:")
            .append(counters.getSessionCreated())
            .append("\n")
            .append("Session destroyed:")
            .append(counters.getSessionDestroyed())
            .append("\n")
            .append("Bind requested:")
            .append(counters.getBindRequested())
            .append("\n")
            .append("Bind timeouts:")
            .append(counters.getBindTimeouts())
            .append("\n")
            .append("Receiver session size:")
            .append(counters.getReceiverSessionSize())
            .append("\n")
            .append("Transmitter session size:")
            .append(counters.getTransmitterSessionSize())
            .append("\n")
            .append("Transceiver session size:")
            .append(counters.getTransceiverSessionSize())
            .append("\n");
            
        
        sb
            .append("Failed logins count:")
            .append(app.getServerHandler().getFailedLogins().size())
            .append("\n");
        
        if (!app.getServerHandler().getFailedLogins().isEmpty()) {
            sb.append("Failed logins:\n");
            
            Set<Map.Entry<String, DateTime>> entrySet = app.getServerHandler().getFailedLogins().entrySet();
            
            for (Map.Entry<String, DateTime> entry:entrySet) {
                sb
                    .append(entry.getKey())
                    .append(":")
                    .append(entry.getValue().toString())
                    .append("\n");
            }
        }
        
        if (
            app.getServerHandler().getHandlers() != null
            && !app.getServerHandler().getHandlers().isEmpty()
        ) {
            sb.append("Active sessions:\n");

            Set<Map.Entry<Long, SmppServerSessionHandler>> entrySet = app.getServerHandler().getHandlers().entrySet();
            
            for (Map.Entry<Long, SmppServerSessionHandler> entry : entrySet) {
                sb
                    .append(entry.getKey())
                    .append(":");
                
                SmppServerSessionHandler value = entry.getValue();
                
                if (value.getSession() != null) {
                    
                    if (value.getSession().getConfiguration() != null) {
                        sb.append(value.getSession().getConfiguration().getName());
                    }
                    
                    DefaultSmppSession defSess = (DefaultSmppSession)value.getSession();
                    
                    if (
                        defSess != null
                        && defSess.getChannel() != null
                        && defSess.getChannel().remoteAddress() != null
                    ) {
                        InetSocketAddress remoteAddress = (InetSocketAddress)defSess.getChannel().remoteAddress();
                
                        sb
                            .append(" ")
                            .append(remoteAddress.getHostString())
                            .append(":")
                            .append(remoteAddress.getPort());
                    }
                    
                    SmppSessionCounters counters1 = value.getSession().getCounters();
                    
                    if (counters1 != null) {
                        sb
                            .append(" ")
                            .append(counters1.getRxSubmitSM())
                            .append("/")
                            .append(counters1.getTxDeliverSM());
                    }
                    
                    CopyOnWriteArrayList<Client> aliveClients = value.getAliveClients();
                    
                    if (
                        aliveClients != null
                        && !aliveClients.isEmpty()
                    ) {
                        sb.append(" [");
                        
                        for (Client c:aliveClients) {
                            if (c!=null) {
                                sb.append(c.getName());
                            }
                        }
                        
                        sb.append("]");
                    }
                } else {
                    sb.append("-null-");
                }
                
                sb.append("\n");
            }
        }
        
        /*
        // for debug purposes
        Iterator<Runnable> taskIterator = asyncPool.getQueue().iterator();
        
        sb.append("Async tasks:");
        
        ArrayList<String> elems = new ArrayList<String>();
        
        for (Runnable r:asyncPool.getQueue()) {
            elems.add(r.toString());
        }
        
        sb.append(elems.toString());
        
        sb.append("\n");*/
        
        log.info(sb.toString());
    }

    private long countEvents(NioEventLoopGroup clientGroup) {
        Iterator<EventExecutor> iterator = clientGroup.iterator();
        
        long items = 0;
        
        while (iterator.hasNext()) {
            items++;
            iterator.next();
        }
        
        return items;
    }
    
    
    
}
