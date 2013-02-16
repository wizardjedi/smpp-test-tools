package com.a1systems.smpptest;

import com.cloudhopper.smpp.SmppBindType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class App {

	protected String host;
	protected int port;
	protected String systemid;
	protected String password;
	protected ServiceMonitor monitor = new ServiceMonitor();

	public static void main(String[] args) {
		new App().run(args);
	}

	public void run(String[] args) {
		// ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("spring/appcontext.xml");

		CommandLineParser parser = new GnuParser();
		try {
			Options options = getOptions();

			CommandLine line = parser.parse(options, args);

			if (line.hasOption(Config.OPT_HELP)) {
				HelpFormatter hf = new HelpFormatter();

				hf.setWidth(80);

				hf.printHelp("smpptest <options> (<ton:npi:abonent> <ton:npi:sender> <text>|<hex>)", options);
			} else if (line.hasOption(Config.OPT_EXAMPLE)) {
				StringBuilder sb = new StringBuilder();

				sb.append("TON:NPI:destinattion_addr ");
				sb.append("TON:NPI:source_addr ");
				sb.append("text ");
				sb.append("dcs=0x08 ");
				sb.append("esm_class=0x40 ");
				sb.append("protocol_id=0x00 ");

				sb.append("TLV:id:value ");
				sb.append("TLV:0x2485:\"4584 758\" ");

				System.out.println(sb.toString());
			} else {
				Config config = validate(line);

				Client client = new Client(config);
				ShutdownHook shutdownHook = new ShutdownHook(client);

				Runtime.getRuntime().addShutdownHook(shutdownHook);

				client.run(line);

				//Runtime.getRuntime().addShutdownHook(shutdownHook);
			}
		} catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
		}
	}

	private static Options getOptions() {
		Options options = new Options();

		options.addOption(new Option(Config.OPT_HELP, "Show usage."));
		options.addOption(new Option(Config.OPT_RECEIVER, "Use receiver."));
		options.addOption(new Option(Config.OPT_TRANSMITTER, "Use transmitter."));
		options.addOption(new Option(Config.OPT_TRANSCEIVER, "Use transceiver."));

		options.addOption(Config.OPT_HOST, true, "Set host:port for connection.");
		options.addOption(Config.OPT_SYSTEM_ID, true, "Set systemid for connection.");
		options.addOption(Config.OPT_PASSWORD, true, "Set password for connection.");

		options.addOption(Config.OPT_HEX, false, "Use hex string as packet body.");
		options.addOption(Config.OPT_HEXRAW, false, "Use hex string as packet.");
		//options.addOption("nodlr", false, "Do not accept deliver_sm.");

		options.addOption(Config.OPT_SPEED, true, "Set speed in sms/sec. Default:"+Config.DEFAULT_SPEED);
		options.addOption(Config.OPT_WAIT, false, "Do not exit after sending all messages.");
		options.addOption(Config.OPT_NO_REBIND, false, "Do not rebind on error or connection closed.");
		options.addOption(Config.OPT_SUMMARY, false, "Print summary on exit.");

		options.addOption(Config.OPT_SHORT_VERBOSE, Config.OPT_LONG_VERBOSE, false, "Verbose logging.");

		options.addOption(Config.OPT_ENQUIRE_LINK_PERIOD, true, "Set enquire link period in seconds.Default:"+Config.DEFAULT_ELINK_PERIOD);
		options.addOption(Config.OPT_ENQUIRE_LINK_ON_NO_TRANSMIT, false, "Send enquire link only if there are no messages.");
		options.addOption(Config.OPT_REBIND_PERIOD, true, "Set rebind period in seconds.Default:"+Config.DEFAULT_REBIND_PERIOD);

		/*
		options.addOption(Config.OPT_SMPP_ESM_CLASS, true, "Set esm_class. (Not currently supported)");
		options.addOption(Config.OPT_SMPP_PROTOCOL_ID, true, "Set protocol_id. (Not currently supported)");
		*/

		options.addOption(Config.OPT_EXAMPLE, false, "Print example string with all params. (Not currently supported)");

		options.addOption(Config.OPT_STDIN, false, "Use STDIN instead of command line params. This option will apply -wait by default.");
		options.addOption(Config.OPT_SUBTOTAL_PERIOD, true, "Set period in seconds for subtotal summary. Default: "+Config.DEFAULT_SUBTOTAL_PERIOD);

		return options;
	}

	private Config validate(CommandLine line) {
		Config config = new Config();

		if (!line.hasOption(Config.OPT_HOST)) {
			throw new IllegalArgumentException("You have to specify host");
		}

		String[] parts = line.getOptionValue(Config.OPT_HOST).split(":");

		if (parts.length != 2 ) {
			throw new IllegalArgumentException("You have to specify host in format host:port.");
		}

		config.setHost(parts[0]);
		config.setPort(Integer.parseInt(parts[1]));

		int connectionTypeCount = 0;
		connectionTypeCount += line.hasOption(Config.OPT_RECEIVER) ? 1 : 0;
		connectionTypeCount += line.hasOption(Config.OPT_TRANSCEIVER) ? 1 : 0;
		connectionTypeCount += line.hasOption(Config.OPT_TRANSMITTER) ? 1 : 0;

		if (
			connectionTypeCount > 1
			|| connectionTypeCount == 0
		) {
			throw new IllegalArgumentException("You have to specify one connection type.");
		}

		if (line.hasOption(Config.OPT_RECEIVER)) {
			config.setBindType(SmppBindType.RECEIVER);
		}

		if (line.hasOption(Config.OPT_TRANSMITTER)) {
			config.setBindType(SmppBindType.TRANSMITTER);
		}

		if (line.hasOption(Config.OPT_TRANSCEIVER)) {
			config.setBindType(SmppBindType.TRANSCEIVER);
		}

		if (!line.hasOption(Config.OPT_SYSTEM_ID)) {
			throw new IllegalArgumentException("You have to specify system id.");
		}

		config.setSystemId(line.getOptionValue(Config.OPT_SYSTEM_ID));

		if (!line.hasOption(Config.OPT_PASSWORD)) {
			throw new IllegalArgumentException("You have to specify password.");
		}

		config.setPassword(line.getOptionValue(Config.OPT_PASSWORD));

		config.setHex(line.hasOption(Config.OPT_HEX));

		config.setExitOnDone(!line.hasOption(Config.OPT_WAIT));

		if (line.hasOption(Config.OPT_ENQUIRE_LINK_PERIOD)) {
			String elinkStr = line.getOptionValue(Config.OPT_ENQUIRE_LINK_PERIOD);

			int period = Integer.parseInt(elinkStr);

			config.setEnquireLinkPeriod(period);
		}

		if (line.hasOption(Config.OPT_REBIND_PERIOD)) {
			String rebindStr = line.getOptionValue(Config.OPT_REBIND_PERIOD);

			int period = Integer.parseInt(rebindStr);

			config.setRebindPeriod(period);
		}

		if (line.hasOption(Config.OPT_SPEED)) {
			String speedStr = line.getOptionValue(Config.OPT_SPEED);

			int speed = Integer.parseInt(speedStr);

			config.setSpeed(speed);
		}

		config.setVerboseLogging(line.hasOption(Config.OPT_SHORT_VERBOSE));

		config.setRebind(!line.hasOption(Config.OPT_NO_REBIND));

		config.setSummary(line.hasOption(Config.OPT_SUMMARY));

		config.setStdin(line.hasOption(Config.OPT_STDIN));

		config.setElinkNoTransmit(line.hasOption(Config.OPT_ENQUIRE_LINK_ON_NO_TRANSMIT));
		
		return config;
	}
}
