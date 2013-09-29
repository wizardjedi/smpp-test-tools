package defcode_recognizer;


public class Defcode_recognizer {

	public static void main(String[] args) {
		String defCodeBase = args[0];
		int threads = Integer.parseInt(args[1]);
		String numbers = args[2];

		Application app = new Application(defCodeBase, threads, numbers);

		app.run();
	}
}
