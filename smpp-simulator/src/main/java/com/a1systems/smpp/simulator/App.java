package com.a1systems.smpp.simulator;

import com.cloudhopper.smpp.type.SmppChannelException;
import java.io.IOException;

public class App {
    public static void main(String[] args) throws SmppChannelException, IOException {
        Application app = new Application();
        
        app.run(args);
    }
}
