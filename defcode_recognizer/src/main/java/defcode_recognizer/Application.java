package defcode_recognizer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Application {
	protected String defCodeBasePath;
	protected int threads;
	protected String numbersPath;
	protected TreeSet<Range> tree;
	protected ExecutorService pool;

	public Application(String defCodeBasePath, int threads, String numbersPath) {
		this.defCodeBasePath = defCodeBasePath;
		this.threads = threads;
		this.numbersPath = numbersPath;
	}

	public void run() {
		pool = Executors.newFixedThreadPool(threads);

		tree = new TreeSet<Range>(new RangeComparator());

		readCsvBase();

		readAndFind();
	}

	protected void readCsvBase() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(defCodeBasePath));

			String line;

			while ((line = br.readLine())!=null) {
				addToTree(line);
			}

			br.close();
		} catch (FileNotFoundException ex) {
			System.err.println("Something wrong when openning csv base");
			System.err.println(ex.toString());
		} catch (IOException ex) {
			System.err.println("Something wrong when reading csv base");
			System.err.println(ex.toString());
		}
	}

	private void addToTree(String line) {
		String[] parts = line.split(";");

		long left = Long.parseLong(parts[0]);
		long right = Long.parseLong(parts[1]);

		String payLoad = parts[2]+";"+parts[3];

		Range r = new Range(left, right, payLoad);

		tree.add(r);
	}

	private void readAndFind() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(numbersPath));

			for (int i=0;i<threads;i++) {
				pool.submit(new FindTask(br, tree));
			}

			pool.shutdown();

			while (!pool.isTerminated()) {

			}

			br.close();
		} catch (FileNotFoundException ex) {
			System.err.println("Something wrong when openning numbers base");
			System.err.println(ex.toString());
		} catch (IOException ex) {
			System.err.println("Something wrong when processing numbers base");
			System.err.println(ex.toString());
		}
	}
}


