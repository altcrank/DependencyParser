
public class TrainingExample {
	
	private int[] wordInputs;
	private int[] tagInputs;
	private int[] labelInputs;
	private int output;
	
	public TrainingExample(int[] wordInputs, int[] tagInputs, int[] labelInputs, int output) {
		this.wordInputs = wordInputs;
		this.tagInputs = tagInputs;
		this.labelInputs = labelInputs;
		this.output = output;
	}
	
	public int[] getWordInputs() {
		return this.wordInputs;
	}
	
	public int[] getTagInputs() {
		return this.tagInputs;
	}
	
	public int[] getLabelInputs() {
		return this.labelInputs;
	}
	
	public int getOutput() {
		return output;
	}
}
