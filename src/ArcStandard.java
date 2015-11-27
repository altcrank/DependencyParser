import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ArcStandard {
//	private List<String> labels;
	private String rootLabel;
	private List<String> transitions;
	
	private String shift;
	private String left;
	private String right;
	
	public ArcStandard(List<String> labels) {
//		this.labels = labels;
		this.rootLabel = "root";
		this.shift = "shift";
		this.left = "left";
		this.right = "right";
		this.transitions = new ArrayList<String>(2*labels.size() + 1);
		this.transitions.add(this.shift);
		for (String label : labels) {
			if (!label.equals(this.rootLabel)) {
				this.transitions.add(left + label);
			}
			this.transitions.add(right + label);
		}
	}
	/**
	 * Apply the given transition to the given configuration
	 * @param c Configuration
	 * @param t Transition
	 */
	public void apply(Configuration c, String t) {
		if (t.equals(this.shift)) {
			c.shift();
			return;
		}
		//else
		if (c.getStackSize() < 2) {
			int dependent = c.getStack(0);
			if (t.equals(this.right + this.rootLabel)) {
				c.addArc(0, dependent, this.rootLabel);
			}
			else {
				System.err.println("Fuck!");
			}
			c.removeTopStack();
			return;
		}
		int head = c.getStack(0);
		int dependent = c.getStack(1);
		String label;
		boolean isLeft = t.startsWith(this.left);
		if (isLeft) {
			label = t.substring(this.left.length());
		} else {
			int temp = head;
			head = dependent;
			dependent = temp;
			label = t.substring(this.right.length());
		}
		c.addArc(head, dependent, label);
		if (isLeft) {
			c.removeSecondTopStack();
		} else {
			c.removeTopStack();
		}
	}
	
/*
  	//**
	 * Determine whether the given transition is legal for this configuration
	 * @param c Configuration
	 * @param t Transition
	 * @return Whether the given transition is legal for this configuration
	 *//*
	public boolean canApply(Configuration c, String t) {
		
	}
	
	public boolean canReach(Configuration c, DependencyTree dTree) {
		
	}
*/
	
	/**
	 * Recommend a transition
	 * @param c	Current configuration
	 * @param dTree Gold-Standard tree that needs to be reached
	 * @return Transition string
	 */
	public String getOracle(Configuration c, DependencyTree dTree) {
		int stackSize = c.getStackSize();
		int bufferSize = c.getBufferSize();
		
		if (stackSize < 2) {
			//not enough for an arc
			if (bufferSize > 0) {
				return this.shift;
			}
			//end of sentence just pop "ROOT".
//			return this.right + this.rootLabel;
			return null;
		}
		//enough for an arc
		int top = c.getStack(0);
		int belowTop = c.getStack(1);
		//Try left
		Arc leftArc = new Arc(top, belowTop);
		for (Arc arc : dTree) {
			if (0 == leftArc.partialCompareTo(arc)) {
				if (!c.hasOtherChild(leftArc.getDependentSentenceIndex(), dTree)) {
					return this.left + arc.getLabel();
				}
			}
		}
		//Find right arc
		Arc rightArc = new Arc(belowTop, top);
		for (Arc arc : dTree) {
			if (0 == rightArc.partialCompareTo(arc)) {
				if (!c.hasOtherChild(rightArc.getDependentSentenceIndex(), dTree)) {
					return this.right + arc.getLabel();
				}
			}
		}
		//If no arc - shift
		if (bufferSize > 0) {
			return this.shift;
		}

		return "Ooops! Ooops;";
	}
	
	public int getTransitionId(String t) {
		return transitions.indexOf(t);
	}
	
	public String getTransition(int transitionId) {
		return transitions.get(transitionId);
	}
	
	public Configuration initialConfiguration(List<Token> sentence) {
		List<Token> modifiedSentence = new LinkedList<Token>();
		modifiedSentence.add(new Token(0, 1, "ROOT", "ROOT", "", -1, ""));
		modifiedSentence.addAll(sentence);
		return new Configuration(modifiedSentence);
	}
	
	public boolean isTerminal(Configuration c) {
		return c.getStackSize() == 1 && c.getBufferSize() == 0;
	}
}
