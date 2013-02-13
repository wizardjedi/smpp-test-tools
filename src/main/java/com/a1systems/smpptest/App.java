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
				Config config = validate(line);

				new Client(config).run(line.getArgs());
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
		//options.addOption("hexraw", false, "Use hex string as a packet.");
		//options.addOption("nodlr", false, "Do not accept deliver_sm.");

		options.addOption(Config.OPT_SPEED, true, "Set speed in sms/sec. Default:"+Config.DEFAULT_SPEED);
		options.addOption(Config.OPT_WAIT, false, "Do not exit after sending all messages.");
		options.addOption(Config.OPT_NO_REBIND, false, "Do not rebind on error or connection closed.");
		options.addOption(Config.OPT_SUMMARY, false, "Print summary on exit.");

		options.addOption(Config.OPT_SHORT_VERBOSE, Config.OPT_LONG_VERBOSE, false, "Verbose logging.");

		options.addOption(Config.OPT_ENQUIRE_LINK_PERIOD, true, "Set enquire link period in seconds.Default:"+Config.DEFAULT_ELINK_PERIOD);
		options.addOption(Config.OPT_REBIND_PERIOD, true, "Set rebind period in seconds.Default:"+Config.DEFAULT_REBIND_PERIOD);

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

		return config;
	}
}
