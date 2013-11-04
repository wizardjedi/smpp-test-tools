package defcode_recognizer;

public class Range {
	protected long left, right;
	protected Object payload;

	public Range(long left, long right, Object payload) {
		if ( left <= right) {
			this.left = left;
			this.right = right;
		} else {
			this.left = right;
			this.right = left;
		}

		this.payload = payload;
	}

	public boolean isGreater(Range r) {
		return (r.getLeft() >= this.getRight());
	}

	public boolean isLess(Range r) {
		return (r.getRight() <= this.getLeft());
	}

	public long getLeft() {
		return left;
	}

	public void setLeft(long left) {
		this.left = left;
	}

	public long getRight() {
		return right;
	}

	public void setRight(long right) {
		this.right = right;
	}

	public Object getPayload() {
		return payload;
	}

	public void setPayload(Object payload) {
		this.payload = payload;
	}

	public String toString() {
		return "Range{"+left+","+right+","+payload+"}";
	}


}
