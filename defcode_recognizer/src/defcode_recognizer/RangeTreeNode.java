package defcode_recognizer;

public class RangeTreeNode {
	protected Range range;
	protected RangeTreeNode left, right;

	public RangeTreeNode(Range r) {
		this.range = r;
	}

	public Range getRange() {
		return range;
	}

	public void setRange(Range range) {
		this.range = range;
	}

	public RangeTreeNode getLeft() {
		return left;
	}

	public void setLeft(RangeTreeNode left) {
		this.left = left;
	}

	public RangeTreeNode getRight() {
		return right;
	}

	public void setRight(RangeTreeNode right) {
		this.right = right;
	}

	public void add(Range r) {
		if (this.range.isLess(r)) {
			left = addTo(left, r);
		} else {
			right = addTo(right,r);
		}
	}

	public Range find(long value) throws Exception {
		if (
			range.getLeft() <= value
			&& range.getRight() >= value
		) {
			return range;
		} else {
			if (value <= range.getLeft()) {
				if (left != null) {
					return left.find(value);
				} else {
					throw new Exception("ad");
				}
			} else {
				if (right != null) {
					return right.find(value);
				} else {
					throw new Exception("ad");
				}
			}
		}
	}

	public String toString() {
		String s = "("+range.toString()+" L ";

		if (left != null) {
			s+= left.toString();
		} else {
			s+="null";
		}

		s+=" R ";

		if (right != null) {
			s+= right.toString();
		} else {
			s+="null";
		}

		s+=")";

		return s;
	}

	private RangeTreeNode addTo(RangeTreeNode branch, Range r) {
		if (branch == null ) {
			return new RangeTreeNode(r);
		} else {
			branch.add(r);

			return branch;
		}
	}
}
