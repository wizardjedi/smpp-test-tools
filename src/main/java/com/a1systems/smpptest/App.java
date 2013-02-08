package com.a1systems.smpptest;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class App {

	protected String host;
	protected int port;
	protected String systemid;
	protected String password;

	public static void main(String[] args) {
		new App().run(args);
	}

	public void run(String[] args) {
		// ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("spring/appcontext.xml");

		CommandLineParser parser = new GnuParser();
		try {
			Options options = getOptions();

			CommandLine line = parser.parse(options, args);

			String[] ar = line.getArgs();

			for (String a:ar) {
				System.out.println(a);
			}

			if (line.hasOption("help")) {
				HelpFormatter hf = new HelpFormatter();

				hf.setWidth(80);

				hf.printHelp("smpptest <options> (<ton:npi:abonent> <ton:npi:sender> <text>|<hex>)", options);
			} else {
				String hostLine = line.getOptionValue("h");

				String[] parts = hostLine.split(":");

				host = parts[0];
				port = Integer.parseInt(parts[1]);

				systemid = line.getOptionValue("u");
				password = line.getOptionValue("p");

				BindType bt;

				if (line.hasOption("rc")) {
					bt = BindType.RECIEVER;
				} else if (line.hasOption("ts")) {
					bt = BindType.TRANSMITTER;
				} else {
					bt = BindType.TRANSCIEVER;
				}

				new Client(host, port, systemid, password, bt).run();
			}
		} catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
		}
	}

	private static Options getOptions() {
		Options options = new Options();

		options.addOption(new Option("help", "Show usage."));
		options.addOption(new Option("rc", "Use receiver."));
		options.addOption(new Option("ts", "Use transmitter."));
		options.addOption(new Option("tc", "Use transceiver."));

		options.addOption("h", true, "Set host:port for connection.");
		options.addOption("u", true, "Set systemid for connection.");
		options.addOption("p", true, "Set password for connection.");

		//options.addOption("hex", false, "Use hex string as packet body.");
		//options.addOption("hexraw", false, "Use hex string as a packet.");
		//options.addOption("nodlr", false, "Do not accept deliver_sm.");

		//options.addOption("speed", true, "Set speed in sms/sec.");
		//options.addOption("wait", false, "Do not exit after send all messages.");

		//options.addOption("elink", true, "Interval in seconds for enquire link.");

		//options.addOption("v", "verbose", false, "Verbose logging.");

		return options;
	}
}
