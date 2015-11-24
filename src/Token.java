
public class Token {

	private int sentenceIndex;
	private int id;
	private String word;
	private String lemma;
	private String POS;
	private int head;
	private String label;
	
	public Token(String[] args, int id) {
		sentenceIndex = Integer.parseInt(args[0]);
		this.id = id;
		word = args[1];
		lemma = args[2];
		POS = args[4];
		head = Integer.parseInt(args[6]);
		label = args[7];
	}
	
	public int getSentenceId() {
		return sentenceIndex;
	}
	
	public int getId() {
		return id;
	}
	
	public String getWord() {
		return word;
	}
	
	public String getLemma() {
		return lemma;
	}
	
	public String getPOSTag() {
		return POS;
	}
	
	public int getHead() {
		return head;
	}
	
	public String getLabel() {
		return label;
	}
}
