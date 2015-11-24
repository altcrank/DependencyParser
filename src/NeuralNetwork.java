import common.MatrixOperations;

public class NeuralNetwork {
	
	private int wordInputsCount;
	private int tagInputsCount;
	private int labelInputsCount;
	private int embeddingSize;
	private double[][] wordEmbeddings;
	private double[][] tagEmbeddings;
	private double[][] labelEmbeddings;
	
	private double[][] wordWeights;
	private double[][] tagWeights;
	private double[][] labelWeights;
	
	private double[] biases;
	
	private double[][] softMaxWeights;
	
	public NeuralNetwork(int wordInputsCount, int tagInputsCount, int labelInputsCount,
			int vocabularySize, int tagsCount, int labelsCount,
			int hiddens, int transitionsCount, int embeddingSize) {
		this.wordInputsCount = wordInputsCount;
		this.tagInputsCount = tagInputsCount;
		this.labelInputsCount = labelInputsCount;
		this.embeddingSize = embeddingSize;
		
		this.wordEmbeddings = MatrixOperations.randomInitialize(this.embeddingSize, vocabularySize, -1.0, 1.0);
		this.tagEmbeddings = MatrixOperations.randomInitialize(this.embeddingSize, tagsCount, -1.0, 1.0);
		this.labelEmbeddings = MatrixOperations.randomInitialize(this.embeddingSize, labelsCount, -1.0, 1.0);
		
		this.wordWeights = MatrixOperations.randomInitialize(hiddens, this.embeddingSize*this.wordInputsCount, -1.0, 1.0);
		this.tagWeights = MatrixOperations.randomInitialize(hiddens, this.embeddingSize*this.tagInputsCount, -1.0, 1.0);
		this.labelWeights = MatrixOperations.randomInitialize(hiddens, this.embeddingSize*this.labelInputsCount, -1.0, 1.0);
		
		this.biases = MatrixOperations.randomInitialize(hiddens, -1.0, 1.0);
		
		this.softMaxWeights = MatrixOperations.randomInitialize(transitionsCount, hiddens, -1.0, 1.0);
	}
	
	public int chooseTransition(int[] wordInputs, int[] tagInputs, int[] labelInputs) {
		double[] inputWordEmbeddingsVector = this.getWordEmbeddingsVector(wordInputs);
		double[] inputTagsEmbeddingsVector = this.getTagEmbeddingsVector(tagInputs);
		double[] inputLabelEmbeddingsVector = this.getLabelEmbeddingsVector(labelInputs);
		
		double[] hiddenActivations = this.computeHiddenActivations(inputWordEmbeddingsVector,
																   inputTagsEmbeddingsVector,
																   inputLabelEmbeddingsVector);
		
		double[] outputs = this.computeOutputs(hiddenActivations);
		
		return MatrixOperations.maxCoordinateIndex(outputs);
	}
	
	private double[] getWordEmbeddingsVector(int[] wordInputs) {
		double[] inputWordEmbeddingsVector = new double[this.wordInputsCount*this.embeddingSize];
		for (int i = 0; i < this.embeddingSize; ++i) {
			for (int word = 0; word < wordInputs.length; ++word) {
				if (0 == wordInputs[word]) {
					inputWordEmbeddingsVector[word*this.embeddingSize + i] = 0;
				} else {
					inputWordEmbeddingsVector[word*this.embeddingSize + i] = this.wordEmbeddings[i][wordInputs[word]-1];	
				}
			}
		}
		return inputWordEmbeddingsVector;
	}
	
	private double[] getTagEmbeddingsVector(int[] tagInputs) {
		double[] inputTagEmbeddingsVector = new double[this.tagInputsCount*this.embeddingSize];
		for (int i = 0; i < this.embeddingSize; ++i) {
			for (int tag = 0; tag < tagInputs.length; ++tag) {
				if (0 == tagInputs[tag]) {
					inputTagEmbeddingsVector[tag*this.embeddingSize + i] = 0;
				} else {
					inputTagEmbeddingsVector[tag*this.embeddingSize + i] = this.tagEmbeddings[i][tagInputs[tag]-1];
				}
			}
		}
		return inputTagEmbeddingsVector;
	}
	
	private double[] getLabelEmbeddingsVector(int[] labelInputs) {
		double[] inputLabelEmbeddingsVector = new double[this.labelInputsCount*this.embeddingSize];
		for (int i = 0; i < this.embeddingSize; ++i) {
			for (int label = 0; label < labelInputs.length; ++label) {
				if (0 == labelInputs[label]) {
					inputLabelEmbeddingsVector[label*this.embeddingSize + i] = 0;
				} else {
					inputLabelEmbeddingsVector[label*this.embeddingSize + i] = this.labelEmbeddings[i][labelInputs[label]-1];
				}
			}
		}
		return inputLabelEmbeddingsVector;
	}
	
	private double[] computeHiddenActivations(double[] inputWordEmbeddings, double[] inputTagEmbeddings, double[] inputLabelEmbeddings) {
		double[] wordActivations = MatrixOperations.multiply(this.wordWeights, inputWordEmbeddings);
		double[] tagActivations = MatrixOperations.multiply(this.tagWeights, inputTagEmbeddings);
		double[] labelActivations = MatrixOperations.multiply(this.labelWeights, inputLabelEmbeddings);
		double[] activationsSum = MatrixOperations.sum(wordActivations, tagActivations);
		activationsSum = MatrixOperations.sum(activationsSum, labelActivations);
		activationsSum = MatrixOperations.sum(activationsSum, this.biases);
		return MatrixOperations.pow(activationsSum, 3);
	}
	
	private double[] computeOutputs(double[] inputs) {
		double[] outputActivations = MatrixOperations.multiply(this.softMaxWeights, inputs);
		return MatrixOperations.exp(outputActivations);
	}
}
