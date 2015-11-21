import java.util.List;
import java.util.LinkedList;

public class ConfigurationState {
	private List<Integer> words;
	private List<Integer> postags;
	private List<Integer> labels;
	
	public ConfigurationState() {
		this.words = new LinkedList<Integer>();
		this.postags = new LinkedList<Integer>();
		this.labels = new LinkedList<Integer>();
	}
	
	public void addWord(int word) {
		this.words.add(word);
	}
	
	public void addWords(List<Integer> words) {
		this.words.addAll(words);
	}
	
	public void addPOStag(int postag) {
		this.postags.add(postag);
	}
	
	public void addPOStags(List<Integer> postags) {
		this.postags.addAll(postags);
	}
	
	public void addLabel(int label) {
		this.labels.add(label);
	}
	
	public void addLabels(List<Integer> labels) {
		this.labels.addAll(labels);
	}
	
	public List<Integer> getWords() {
		return this.words;
	}
	
	public List<Integer> getPOStags() {
		return this.postags;
	}
	
	public List<Integer> getLabels() {
		return this.labels;
	}
	
	@Override
	public String toString() {
		String result = "";
		for (int word : this.words) {
			result += String.valueOf(word) + ",";
		}
		for (int postag : this.postags) {
			result += String.valueOf(postag) + ",";
		}
		for (int label : this.labels) {
			result += String.valueOf(label) + ",";
		}
		return result;
	}
}
