package defcode_recognizer;

import java.util.Comparator;

public class RangeComparator implements Comparator<Range> {

	@Override
	public int compare(Range r1, Range r2) {
		if (r1.getRight() <= r2.getLeft()) {
			return -1;
		} else if (r1.getLeft() > r2.getRight()) {
			return 1;
		} else {
			return 0;
		}
	}
}
