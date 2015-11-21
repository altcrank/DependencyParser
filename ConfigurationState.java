import java.util.List;
import java.util.LinkedList;

public class ConfigurationState {
	private List<Integer> words;
	private List<String> postags;
	private List<String> labels;
	
	public ConfigurationState() {
		this.words = new LinkedList<Integer>();
		this.postags = new LinkedList<String>();
		this.labels = new LinkedList<String>();
	}
	
	public void addWord(int word) {
		this.words.add(word);
	}
	
	public void addPOStag(String postag) {
		this.postags.add(postag);
	}
	
	public void addLabel(String label) {
		this.labels.add(label);
	}
	
	public List<Integer> getWords() {
		return this.words;
	}
	
	public List<String> getPOStags() {
		return this.postags;
	}
	
	public List<String> getLabels() {
		return this.labels;
	}
}
