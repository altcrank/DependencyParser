import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ArcStandard {
//	private List<String> labels;
	private static final String rootLabel = "root";
	private List<String> transitions;
	
	private static final String shift = "shift";
	private static final String left = "left";
	private static final String right = "right";
	
	public ArcStandard(List<String> labels) {
//		this.labels = labels;
		this.transitions = new ArrayList<String>(2*labels.size() + 1);
		this.transitions.add(ArcStandard.shift);
		for (String label : labels) {
			if (!label.equals(ArcStandard.rootLabel)) {
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
		if (t.equals(ArcStandard.shift)) {
			c.shift();
			return;
		}
		//else
		if (c.getStackSize() < 2) {
			int dependent = c.getStack(0);
			if (t.equals(ArcStandard.right + ArcStandard.rootLabel)) {
				c.addArc(0, dependent, ArcStandard.rootLabel);
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
		boolean isLeft = t.startsWith(ArcStandard.left);
		if (isLeft) {
			label = t.substring(ArcStandard.left.length());
		} else {
			int temp = head;
			head = dependent;
			dependent = temp;
			label = t.substring(ArcStandard.right.length());
		}
		c.addArc(head, dependent, label);
		if (isLeft) {
			c.removeSecondTopStack();
		} else {
			c.removeTopStack();
		}
	}
	
	public boolean canApply(Configuration c, String t) {
		//several special cases
		//when buffer is empty and it predicted shift
		//when ROOT will be head and the label is not leftroot
		switch (t) {
		case ArcStandard.shift:
			return 0 != c.getBufferSize();
		case (ArcStandard.right + ArcStandard.rootLabel):
			return 0 == c.getStack(1);
		}
		return true;
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
				return ArcStandard.shift;
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
					return ArcStandard.left + arc.getLabel();
				}
			}
		}
		//Find right arc
		Arc rightArc = new Arc(belowTop, top);
		for (Arc arc : dTree) {
			if (0 == rightArc.partialCompareTo(arc)) {
				if (!c.hasOtherChild(rightArc.getDependentSentenceIndex(), dTree)) {
					return ArcStandard.right + arc.getLabel();
				}
			}
		}
		//If no arc - shift
		if (bufferSize > 0) {
			return ArcStandard.shift;
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
