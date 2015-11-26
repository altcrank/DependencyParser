import java.util.Iterator;
import java.util.List;

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
	

	private static final double convergenceThreshold = 0.01;
	private static double learningRate = 0.01;
	private static final double regularizingPenalty = 0.01;
	
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
	
	public void train(List<TrainingExample> data) {
		double error = Double.POSITIVE_INFINITY;
		TrainingExample[] randomAccessData = data.toArray(new TrainingExample[0]);
		int[] indices = MatrixOperations.initializeIndices(data.size());
		while (error > NeuralNetwork.convergenceThreshold) {
			this.trainIteration(randomAccessData, indices);
			error = this.computeError(data);
		}
	}

//private methods start from here
	
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
	
	private double[] computeHiddenActivationsBase(double[] inputWordEmbeddings, double[] inputTagEmbeddings, double[] inputLabelEmbeddings) {
		double[] wordActivations = MatrixOperations.multiply(this.wordWeights, inputWordEmbeddings);
		double[] tagActivations = MatrixOperations.multiply(this.tagWeights, inputTagEmbeddings);
		double[] labelActivations = MatrixOperations.multiply(this.labelWeights, inputLabelEmbeddings);
		double[] activationsSum = MatrixOperations.sum(wordActivations, tagActivations);
		activationsSum = MatrixOperations.sum(activationsSum, labelActivations);
		activationsSum = MatrixOperations.sum(activationsSum, this.biases);
		return activationsSum;
	}
	
	private double[] computeHiddenActivations(double[] inputWordEmbeddings, double[] inputTagEmbeddings, double[] inputLabelEmbeddings) {
		double[] activationsSum = this.computeHiddenActivationsBase(inputWordEmbeddings, inputTagEmbeddings, inputLabelEmbeddings);
		return MatrixOperations.pow(activationsSum, 3);
	}
	
	private double[] computeOutputs(double[] inputs) {
		double[] outputActivations = MatrixOperations.multiply(this.softMaxWeights, inputs);
		return MatrixOperations.exp(outputActivations);
	}
	
	private void trainIteration(TrainingExample[] data, int[] indices) {
		MatrixOperations.shuffle(indices);
		for (int i : indices) {
			this.updateWeights(data[i]);
		}
	}
	
	private double computeError(List<TrainingExample> data) {
		//compute Sum of Squared Errors (SSE) for data
		Iterator<TrainingExample> dataIt = data.iterator();
		int errors = 0;
		while (dataIt.hasNext()) {
			TrainingExample example = dataIt.next();
			int transition = this.chooseTransition(example.getWordInputs(), example.getTagInputs(), example.getLabelInputs());
			if (transition != example.getOutput()) {
				++errors;
			}
		}
		//return percentage of errors
		return (double) (errors * 100) / (double) data.size();
	}
	
	private void updateWeights(TrainingExample example) {
		//compute projections
		double[] inputWordEmbeddingsVector = this.getWordEmbeddingsVector(example.getWordInputs());
		double[] inputTagsEmbeddingsVector = this.getTagEmbeddingsVector(example.getTagInputs());
		double[] inputLabelEmbeddingsVector = this.getLabelEmbeddingsVector(example.getLabelInputs());
		//compute hidden activations
		double[] hiddenActivationsBase = this.computeHiddenActivationsBase(inputWordEmbeddingsVector,
																		   inputTagsEmbeddingsVector,
																		   inputLabelEmbeddingsVector);
		double[] hiddenActivations = MatrixOperations.pow(hiddenActivationsBase, 3);
		//compute output
	    double[] outputs = this.computeOutputs(hiddenActivations);
	    //log outputs
	    double[] loggedOutputs = MatrixOperations.log(outputs);
	    //compute normalizing constant (sum of outputs)
	    double normalizingConstant = this.computeNormalizingConstant(loggedOutputs);
	    //compute derivatives (use regularizing penalty)
	    double[] outputDerivatives = MatrixOperations.multiply(loggedOutputs, 1/normalizingConstant);
	    outputDerivatives[example.getOutput()] -= 1;
	    double[][] softMaxWeightsDerivatives = MatrixOperations.outerProduct(outputDerivatives, hiddenActivations);
	    double[][] softMaxPenalty = MatrixOperations.multiply(this.softMaxWeights, NeuralNetwork.regularizingPenalty);
	    MatrixOperations.addInline(softMaxWeightsDerivatives, softMaxPenalty);
	    
	    double[] hiddenDerivatives = MatrixOperations.multiply(MatrixOperations.transpose(this.softMaxWeights), outputDerivatives);
	    double[] hiddenActivationDerivatives = MatrixOperations.pow(hiddenActivationsBase, 2);
	    MatrixOperations.multiplyInline(hiddenActivationDerivatives, 3);
	    
	    double[] biasDerivatives = MatrixOperations.multiplyComponentWise(hiddenDerivatives, hiddenActivationDerivatives);
	    
	    double[][] wordWeightsDerivatives = this.computeWeightsDerivatives(this.wordWeights, biasDerivatives, inputWordEmbeddingsVector);
	    double[][] tagsWeightsDerivatives = this.computeWeightsDerivatives(this.tagWeights, biasDerivatives, inputTagsEmbeddingsVector);
	    double[][] labelWeightsDerivatives = this.computeWeightsDerivatives(this.labelWeights, biasDerivatives, inputLabelEmbeddingsVector);
	    
	    double[] commonMultipleVector = MatrixOperations.multiplyComponentWise(hiddenDerivatives, hiddenActivationDerivatives);
	    double[] wordEmbeddingsDerivatives = MatrixOperations.multiply(MatrixOperations.transpose(this.wordWeights), commonMultipleVector);
	    double[] tagEmbeddingsDerivatives = MatrixOperations.multiply(MatrixOperations.transpose(this.tagWeights), commonMultipleVector);
	    double[] labelEmbeddingsDerivatives = MatrixOperations.multiply(MatrixOperations.transpose(this.labelWeights), commonMultipleVector);
		
	    //update weights
	    this.updateSingleSetOfWeights(this.softMaxWeights, softMaxWeightsDerivatives);
	    this.updateSingleSetOfWeights(this.biases, biasDerivatives);
	    this.updateSingleSetOfWeights(this.wordWeights, wordWeightsDerivatives);
	    this.updateSingleSetOfWeights(this.tagWeights, tagsWeightsDerivatives);
	    this.updateSingleSetOfWeights(this.labelWeights, labelWeightsDerivatives);
	    this.updateEmbeddings(this.wordEmbeddings, wordEmbeddingsDerivatives, example.getWordInputs());
	    this.updateEmbeddings(this.tagEmbeddings, tagEmbeddingsDerivatives, example.getTagInputs());
	    this.updateEmbeddings(this.labelEmbeddings, labelEmbeddingsDerivatives, example.getLabelInputs());
	}

	private double computeNormalizingConstant(double[] loggedOutputs) {
		//normalizingConstant = maxLoggedOutput + log(sum(exp(loggedOutputs - maxLoggedOutput)))
		//pick max logged output
		double maxLoggedOutput = MatrixOperations.max(loggedOutputs);
		double[] adjustedLoggedOutputs = MatrixOperations.subtract(loggedOutputs, maxLoggedOutput);
		double[] expedAdjustedLoggedOutputs = MatrixOperations.exp(adjustedLoggedOutputs);
		double sum = MatrixOperations.sumCoordinates(expedAdjustedLoggedOutputs);
		return maxLoggedOutput + Math.log(sum);
	}
	
	private double[][] computeWeightsDerivatives(double[][] weights, double[] biasDerivatives, double[] embeddingsVector) {
		double[][] weightsDerivatives = MatrixOperations.outerProduct(biasDerivatives, embeddingsVector);
	    double[][] weightsPenalty = MatrixOperations.multiply(weights, NeuralNetwork.regularizingPenalty);
	    MatrixOperations.addInline(weightsDerivatives, weightsPenalty);
	    return weightsDerivatives;
	}
	
	private void updateSingleSetOfWeights(double[] weights, double[] weightsDerivatives) {
		MatrixOperations.multiplyInline(weightsDerivatives, NeuralNetwork.learningRate);
		MatrixOperations.subtractInline(weights, weightsDerivatives);
	}
	
	private void updateSingleSetOfWeights(double[][] weights, double[][] weightsDerivatives) {
		MatrixOperations.multiplyInline(weightsDerivatives, NeuralNetwork.learningRate);
		MatrixOperations.subtractInline(weights, weightsDerivatives);
	}

	private void updateEmbeddings(double[][] embeddings, double[] embeddingDerivatives, int[] inputs) {
		for (int i = 0; i < embeddingDerivatives.length; ++i) {
			int inputIndex = i / this.embeddingSize;
			if (0 == inputs[i / this.embeddingSize]) {
				i += this.embeddingSize -1;
				continue;
			}
			int embeddingCoordinate = i % this.embeddingSize;
			double penalizedDerivative = embeddingDerivatives[i] + NeuralNetwork.regularizingPenalty * embeddings[embeddingCoordinate][inputIndex];
			embeddings[embeddingCoordinate][inputIndex] -= penalizedDerivative;
		}
	}
}
