
public class Arc implements Comparable<Arc> {
	private int child;
	private int head;
	private String label;
	
	public Arc(int head, int child) {
		this(head, child, "");
	}
	
	public Arc(int head, int child, String label) {
		this.head = head;
		this.child = child;
		this.label = label;
	}
	
	public int getChild() {
		return child;
	}

	public int getHead() {
		return head;
	}
	
	public String getLabel() {
		return label;
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof Arc)) {
			return false;
		}
		return 0 == this.compareTo((Arc)other);
	}
	
	public int compareTo(Arc other) {
		//smaller -> -
		//equal -> 0
		//greater -> +
		int diff = this.partialCompareTo(other);
		if (diff != 0) {
			return diff;
		}
		return this.label.compareTo(other.label);
	}
	
	public int partialCompareTo(Arc other) {
		//smaller -> -
		//equal -> 0
		//greater -> +
		int headDiff = this.head - other.head;
		if (headDiff != 0) {
			return headDiff;
		}
		return this.child - other.child;
	}
}
