import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class Configuration {

	List<Token> sentence;
	
	List<Integer> stack;
	List<Integer> buffer;
	DependencyTree arcs;
	
	public Configuration(List<Token> sentence) {
		this.sentence = sentence;
		this.stack = new LinkedList<Integer>();
		this.buffer = new LinkedList<Integer>();
		this.arcs = new DependencyTree();
		
		this.stack.add(this.sentence.get(0).getSentenceId());
		boolean skipRoot = true;//skip first word since it is root
		for (Token token : this.sentence) {
			if (skipRoot) {
				skipRoot = false;
				continue;
			}
			this.buffer.add(token.getSentenceId());
		}
	}
	
	/**
	 * @param h - word index of head
	 * @param t - word index of dependent
	 * @param l - label
	 */
	public void addArc(int h, int t, String l) {
		this.arcs.add(new Arc(h, t, l));
	}
	
    /**
	 * Get the sentence index of the k-th word on the buffer
	 */
	public int getBuffer(int k) {
		if (k >= this.buffer.size()) {
			return -1;
		}
		return this.buffer.get(k);
	}

	public int getBufferSize() {
		return this.buffer.size();
	}
	
	public int getHead(int k) {
		return this.sentence.get(k).getHead();
	}

	public String getLabel(int k) {
		return this.sentence.get(k).getLabel();
	}

	public int getLeftChild(int k, int cnt) {
		int childCount = 0;
		List<Integer> sortedChildren = this.arcs.getSortedChildren(k);
		ListIterator<Integer> it = sortedChildren.listIterator();
		while (it.hasNext()) {
			int child = it.next();
			if (child > k) {
				break;
			}
			++childCount;
			if (childCount == cnt) {
				return child;
			}
		}
		return -1;
	}

	public String getPOS(int k) {
		return this.sentence.get(k).getPOSTag();
	}
	
	public int getRightChild(int k, int cnt) {
		int childCount = 0;
		List<Integer> sortedChildren = this.arcs.getSortedChildren(k);
		ListIterator<Integer> it = sortedChildren.listIterator(sortedChildren.size());
		while (it.hasPrevious()) {
			int child = it.previous();
			if (child < k) {
				break;
			}
			++childCount;
			if (childCount == cnt) {
				return child;
			}
		}
		return -1;
	}
	
//	public int getSentenceSize() {
//		return sentence.size();
//	}
	
	public int getStack(int k) {
		if (this.stack.size() - (1 + k) < 0) {
			return -1;
		}
		return this.stack.get(this.stack.size() - (1 + k));
	}
	
	public int getStackSize() {
		return this.stack.size();
	}
	
	public String getWord(int k) {
		return this.sentence.get(k).getLemma();
	}
	
	public boolean hasOtherChild(int k, DependencyTree goldTree) {
		return this.countChildren(k, this.arcs) != this.countChildren(k, goldTree);
	}
	
	public boolean removeSecondTopStack() {
		if (this.stack.size() < 2) {
			return false;
		}
		this.stack.remove(this.stack.size() - 2);
		return true;
	}
	
	public boolean removeTopStack() {
		if (this.stack.isEmpty()) {
			return false;
		}
		this.stack.remove(this.stack.size() - 1);
		return true;
	}
	
	public boolean shift() {
		if (this.buffer.isEmpty()) {
			return false;
		}
		this.stack.add(this.buffer.remove(0).intValue());
		return true;
	}
	
	public DependencyTree getDependencyTree() {
		return this.arcs;
	}
	
	private int countChildren(int k, DependencyTree tree) {
		int count = 0;
		for (Arc arc : tree) {
			if (arc.getHeadSentenceIndex() == k) {
				++count;
			}
		}
		return count;
	}
}
