package com.a1systems.smpptest;

import com.a1systems.smpptest.config.Config;
import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.commons.gsm.GsmUtil;
import com.cloudhopper.commons.util.HexUtil;
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppBindException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import java.util.concurrent.TimeUnit;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class App {

	protected ExpressionParser parser;

	public static Logger log = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) throws SmppInvalidArgumentException {
		new App().run(args);
	}

	public void run(String[] args) throws SmppInvalidArgumentException {
		Config cfg = new Config();

		CmdLineParser parser = new CmdLineParser(cfg);

		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.out.println("SMPP sender. Note: currently uses only BIND_TRANSCEIVER. For testing purposes only.");
			System.out.println("Example: smpptester -h 127.0.0.1:2775 -u test -p test 1:1:791231234567 5:0:test 'gsm7:test msg'");

			parser.printUsage(System.out);

			System.exit(1);
		}

		this.parser = new SpelExpressionParser();

		runApplication(cfg);
	}

	public void runApplication(Config cfg) throws SmppInvalidArgumentException {
		DefaultSmppClient smppClient = new DefaultSmppClient();

		SmppSessionConfiguration sessionConfig;

		sessionConfig =
			new SmppSessionConfiguration(
				SmppBindType.TRANSCEIVER,
				cfg.getSystemId(),
				cfg.getPassword()
			);

		String []hostPort = cfg.getHostPort().split(":");

		sessionConfig.setHost(hostPort[0]);
		sessionConfig.setPort(Integer.parseInt(hostPort[1]));

		try {
			SmppSession session = smppClient.bind(sessionConfig);

			ExpressionParser parser = new SpelExpressionParser();

			byte[] encodedString = parseString(cfg.getArguments().get(2));

			if (encodedString.length > 140) {

				int msgId = (int)Math.round(Math.random()*100);

				byte[][] msgParts = GsmUtil.createConcatenatedBinaryShortMessages(encodedString, (byte) 11);

				for (byte[] msgPart : msgParts) {

					SubmitSm sm = new SubmitSm();

					sm.setRegisteredDelivery((byte)1);

					sm.setDestAddress(parseSmppAddress(cfg.getArguments().get(0)));
					sm.setSourceAddress(parseSmppAddress(cfg.getArguments().get(1)));

					sm.setShortMessage(msgPart);
					session.submit(sm, TimeUnit.SECONDS.toMillis(60));
				}
			} else {
				SubmitSm sm = new SubmitSm();

				sm.setRegisteredDelivery((byte)1);

				sm.setDestAddress(parseSmppAddress(cfg.getArguments().get(0)));
				sm.setSourceAddress(parseSmppAddress(cfg.getArguments().get(1)));

				sm.setShortMessage(encodedString);
				session.submit(sm, TimeUnit.SECONDS.toMillis(60));
			}
			TimeUnit.SECONDS.sleep(10);

			log.info("Bye!");

			session.close();
			session.destroy();

			smppClient.destroy();

		} catch (SmppTimeoutException ex) {
			log.error("{}", ex);
		} catch (SmppChannelException ex) {
			log.error("{}", ex);
		} catch (SmppBindException ex) {
			log.error("{}", ex);
		} catch (UnrecoverablePduException ex) {
			log.error("{}", ex);
		} catch (InterruptedException ex) {
			log.error("{}", ex);
		} catch (RecoverablePduException ex) {
			log.error("{}", ex);
		}
	}

	protected byte[] parseString(String str) {
		if (str.toLowerCase().matches("^(gsm7|gsm8|ucs-2|hex):.*$")) {
			String[] parts = str.split(":", 2);

			if ("hex".equals(parts[0].toLowerCase())) {
				return HexUtil.toByteArray(parts[1]);
			} else {
				String chartset = parts[0].toUpperCase();
				String text = (String)spelParse("'"+parts[1]+"'");

				return CharsetUtil.encode(text, chartset);
			}
		} else {
			throw new IllegalArgumentException("Wrong text format");
		}
	}

	protected Address parseSmppAddress(String str) {
		if (str.matches("^[xXa-fA-F0-9]{1,4}:[xXa-fA-F0-9]{1,4}:.{1,15}$")) {
			String[] parts = str.split(":",3);

			Address a = new Address();

			a.setTon((byte)spelParse(parts[0], Byte.class));
			a.setNpi((byte)spelParse(parts[1], Byte.class));

			a.setAddress(parts[2]);

			return a;
		} else {
			throw new IllegalArgumentException("String "+str+" has no format digit:digit:address");
		}
	}

	protected Object spelParse(String exp) {
		return parser.parseExpression(exp).getValue();
	}

	protected Object spelParse(String exp, Class resultClass) {
		return resultClass.cast(parser.parseExpression(exp).getValue(resultClass));
	}
}
