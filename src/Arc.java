
public class Arc implements Comparable<Arc> {

	private int headSentenceIndex;
	private int dependentSentenceIndex;
	private String label;
	
	public Arc(int headSentenceIndex, int dependentSentenceIndex) {
		this(headSentenceIndex, dependentSentenceIndex, "");
	}
	
	public Arc(int headSentenceIndex, int dependentSentenceIndex, String label) {
		this.headSentenceIndex = headSentenceIndex;
		this.dependentSentenceIndex = dependentSentenceIndex;
		this.label = label;
	}
	
	public int getHeadSentenceIndex() {
		return this.headSentenceIndex;
	}

	public int getDependentSentenceIndex() {
		return this.dependentSentenceIndex;
	}
	
	public String getLabel() {
		return this.label;
	}
	
	public String getDirection() {
		return (this.headSentenceIndex < this.dependentSentenceIndex) ? "right" : "left";
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
		int headSiDiff = this.headSentenceIndex - other.headSentenceIndex;
		if (headSiDiff != 0) {//not equal
			return headSiDiff;
		}
		return this.dependentSentenceIndex - other.dependentSentenceIndex;
	}
	
	@Override
	public String toString() {
		String result = String.valueOf(this.headSentenceIndex);
		result += " ";
		result += String.valueOf(this.label);
		result += " ";
		result += String.valueOf(this.dependentSentenceIndex);
		result += " ";
		return result + this.getDirection();
	}
}
