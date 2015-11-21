
public class Arc implements Comparable<Arc> {

	private int head;
	private int child;
	private String label;
	
	public Arc(int head, int child) {
		this(head, child, "");
	}
	
	public Arc(int headSentenceId, int child, String label) {
		this.head = headSentenceId;
		this.child = child;
		this.label = label;
	}
	
	public int getHead() {
		return this.head;
	}

	public int getChild() {
		return this.child;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String getDirection() {
		return (head < child) ? "right" : "left";
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
	
	@Override
	public String toString() {
		String result = String.valueOf(this.head);
		result += " ";
		result += String.valueOf(this.label);
		result += " ";
		result += String.valueOf(this.child);
		result += " ";
		return result + this.getDirection();
	}
}
