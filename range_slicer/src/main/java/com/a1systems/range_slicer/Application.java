package com.a1systems.range_slicer;

import defcode_recognizer.Range;
import defcode_recognizer.RangeComparator;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeSet;

class Application {
	protected TreeSet<Range> tree;

	protected String fileName;

	public void run(String fileName) {
		this.fileName = fileName;

		tree = new TreeSet<Range>(new RangeComparator());

		try {
			BufferedReader bis = new BufferedReader(new FileReader(fileName));

			String line;

			while ((line = bis.readLine()) != null) {
				processLine(line);
			}

			System.out.println("-----");

			for (Range r:tree) {
				System.out.println(r.toString());
			}
		} catch (FileNotFoundException ex) {
			System.err.println(ex.toString());
		} catch (IOException ex) {
			System.err.println(ex.toString());
		}
	}

	protected void processLine(String line) {
		String[] parts = line.split(";",3);

		Range range = new Range(Long.parseLong(parts[0]), Long.parseLong(parts[1]) , parts[2]);

		Range r = tree.ceiling(range);

		if (r == null) {
			tree.add(range);
		} else {
			processRange(tree, r, range);
		}

	}


	protected void processRange(TreeSet<Range> tree, Range treeRange, Range newRange) {
		if (
			treeRange.getLeft() >= newRange.getRight()
		) {
			tree.add(newRange);
		} else if (
			treeRange.getRight() >= newRange.getLeft()
		) {
			tree.add(newRange);
		} else {
			System.out.println("Oops"+treeRange.toString()+newRange.toString());
		}
	}

}
