
public class Token {

	private int sentenceIndex;
	private int id;
	private String word;
	private String lemma;
	private String POS;
	private int head;
	private String label;
	
	public Token(String[] args, int id) {
		this.sentenceIndex = Integer.parseInt(args[0]);
		this.id = id;
		this.word = args[1];
		this.lemma = args[2];
		this.POS = args[4];
		this.head = Integer.parseInt(args[6]);
		this.label = args[7];
	}
	
	public Token(int sentenceIndex, int id, String word, String lemma, String pos, int head, String label) {
		this.sentenceIndex = sentenceIndex;
		this.id = id;
		this.word = word;
		this.lemma = lemma;
		this.POS = pos;
		this.head = head;
		this.label = label;
	}
	
	public int getSentenceId() {
		return this.sentenceIndex;
	}
	
	public int getId() {
		return this.id;
	}
	
	public String getWord() {
		return this.word;
	}
	
	public String getLemma() {
		return this.lemma;
	}
	
	public String getPOSTag() {
		return this.POS;
	}
	
	public int getHead() {
		return this.head;
	}
	
	public String getLabel() {
		return this.label;
	}
}
