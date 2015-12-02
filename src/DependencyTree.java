import java.util.LinkedList;
import java.util.List;
import java.util.Collections;
import java.util.Iterator;

public class DependencyTree extends LinkedList<Arc> {

	private static final long serialVersionUID = 4099018308379880189L;
	
	public void sort() {
		Collections.sort(this);
	}
	
	public List<Integer> getSortedChildren(int head) {
		List<Integer> children = new LinkedList<Integer>();
		for (Arc arc : this) {
			if (head == arc.getHeadSentenceIndex()) {
				children.add(arc.getDependentSentenceIndex());
			}
		}
		Collections.sort(children);
		return children;
	}
	
	public boolean hasArc(Arc arc, boolean labeled) {
		for (Arc a : this) {
			int match = labeled ? a.compareTo(arc) : a.partialCompareTo(arc);
			if (0 == match) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DependencyTree)) {
			return false;
		}
		DependencyTree other = (DependencyTree)o;
		Iterator<Arc> thisIt = this.iterator();
		Iterator<Arc> otherIt = other.iterator();
		while (thisIt.hasNext() && otherIt.hasNext()) {
			Arc thisArc = thisIt.next();
			Arc otherArc = otherIt.next();
			if (0 != thisArc.compareTo(otherArc)) {
				return false;
			}
		}
		return !thisIt.hasNext() && !otherIt.hasNext();
	}
	
	@Override
	public String toString() {
		String result = "";
		for (Arc arc : this) {
			result += arc;
			result += "\n";
		}
		return result;
	}
}
