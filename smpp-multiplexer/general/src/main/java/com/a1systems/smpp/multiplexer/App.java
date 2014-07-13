package com.a1systems.smpp.multiplexer;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class App {

    public static void main(String[] args) throws Exception {
        CliConfig cfg = new CliConfig();

        CmdLineParser parser = new CmdLineParser(cfg);

        try {
            parser.parseArgument(args);

            if (cfg.getPort() == null || cfg.getEndPoints() == null) {
                printUsageAndExit(parser);
            }
        } catch (CmdLineException e) {
            printUsageAndExit(parser);
        }

        Application app = new Application();

        app.run(cfg);
    }

    public static void printUsageAndExit(CmdLineParser parser) {
        System.out.println("SMPP multiplexer.");
        System.out.println("Example: smppmultiplexer -p 3712 -e '127.0.0.1:2775,h:127.0.0.1:2776'");
        System.out.println("\tStart smpp multiplexer on port 3712. Use connections to 127.0.0.1:2775 and 127.0.0.1:2776."
                + " 127.0.0.1:2776 - hidden connection (No submit_sm will route to it, but deliver_sm will route back).");
        
        parser.printUsage(System.out);

        System.exit(1);
    }
}
