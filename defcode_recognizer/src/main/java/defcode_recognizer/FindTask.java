package defcode_recognizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.TreeSet;

public class FindTask implements Runnable {
	protected BufferedReader br;
	protected TreeSet<Range> tree;

	public FindTask(BufferedReader br, TreeSet<Range> tree) {
		this.br = br;
		this.tree = tree;
	}

	@Override
	public void run() {
		String line;

		try {
			while ((line = br.readLine())!=null) {
				processNumber(line);
			}
		} catch (IOException ex) {
			System.err.println("Something wrong when processing numbers base");
			System.err.println(ex.toString());
		}
	}

	protected void processNumber(String input) {
		String[] parts = input.split(";", 2);

		String payload = parts[1];

		long value = Long.parseLong(parts[0]);

		try {
			Range r = tree.ceiling(new Range(value, value, payload));

			System.out.println(value+";"+r.getLeft()+";"+r.getRight()+";"+r.getPayload().toString()+";"+payload);
		} catch (Exception e) {
			System.out.println(value+";;;;"+payload);
		}
	}
}
