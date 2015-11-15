import java.util.LinkedList;
import java.util.List;

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
		
		for (Token token : sentence) {
			buffer.add(token.getId());
		}
	}
	
	/**
	 * @param h - word index of head
	 * @param t - word index of dependent
	 * @param l - label
	 */
	public void addArc(int h, int t, String l) {
		this.arcs.add(new Arc(h, t, l));
/*		System.out.println("Stack");
		for (int s : stack) {
			System.out.println(s);
		}
		System.out.println("Buffer");
		for (int s : buffer) {
			System.out.println(s);
		}
*/	}
	
/**
    //**
	 * Get the sentence index of the k-th word on the buffer
	 *//*
	public int getBuffer(int k) {
		return buffer.get(k);
	}
*/	

	public int getBufferSize() {
		return buffer.size();
	}
	
/*	
	*//**
	 * @param k - word index (zero is root node, actual word index begins at 1)
	 *//*
	public int getHead(int k) {
		return sentence.get(k - 1).getHead();
	}

	public String getLabel(int k) {
		return sentence.get(k - 1).getLabel();
	}

	public int getLeftChild(int k, int cnt) {
		int child = 0;
		for (Arc arc : arcs) {
			if (arc.getChild() > k) {
				break;
			}
			if (arc.getHead() == k) {
				++child;
				if (child == cnt) {
					return arc.getChild();
				}
			}
		}
		return 0;
	}

	public String getPOS(int k) {
		return sentence.get(k - 1).getPOSTag();
	}
	
	public int getRightChild(int k, int cnt) {
		int child = 0;
		ListIterator<Arc> it = arcs.listIterator(arcs.size());
		while (it.hasPrevious()) {
			Arc arc = it.previous();
			if (arc.getChild() < k) {
				break;
			}
			if (arc.getHead() == k) {
				++child;
				if (child == cnt) {
					return arc.getChild();
				}
			}
		}
		return 0;
	}
	
	public int getSentenceSize() {
		return sentence.size();
	}
*/	
	public int getStack(int k) {
		return stack.get(stack.size() - (1 + k));
	}
	
	public int getStackSize() {
		return stack.size();
	}
	
	public int getWord(int k) {
		return sentence.get(k).getSentenceId();
	}
	
	public boolean removeSecondTopStack() {
		if (stack.size() < 2) {
			return false;
		}
		stack.remove(stack.size() - 2);
		return true;
	}
	
	public boolean removeTopStack() {
		if (stack.isEmpty()) {
			return false;
		}
		stack.remove(stack.size() - 1);
		return true;
	}
	
	public boolean shift() {
		if (buffer.isEmpty()) {
			return false;
		}
		stack.add(buffer.remove(0).intValue());
		return true;
	}
	
	public DependencyTree getDependencyTree() {
		return arcs;
	}
}
