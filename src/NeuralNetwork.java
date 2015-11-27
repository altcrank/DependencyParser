import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import common.MatrixOperations;

public class NeuralNetwork implements Serializable {
	
	/**
	 * Serializable requires it. This is the one added by default
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * working weights
	 */
	private int wordInputsCount;
	private int tagInputsCount;
	private int labelInputsCount;
	private int embeddingSize;
	private int currentBatch;
	
	private double[][] wordEmbeddings;
	private double[][] tagEmbeddings;
	private double[][] labelEmbeddings;
	
	private double[][] wordWeights;
	private double[][] tagWeights;
	private double[][] labelWeights;
	
	private double[] biases;
	
	private double[][] softMaxWeights;
	
	/**
	 * hyperparams
	 */
	private static final double convergenceThreshold = 95;
	private static double learningRate = 0.001;
	private static final double regularizingPenalty = 0.000001;
	private static final int batchSize = 30;
	
	/**
	 * accumulated squared gradients for AdaGrad (Adaptive Gradient)
	 */
	private double[][] wordEmbeddingsGrad;
	private double[][] tagEmbeddingsGrad;
	private double[][] labelEmbeddingsGrad;
	
	private double[][] wordWeightsGrad;
	private double[][] tagWeightsGrad;
	private double[][] labelWeightsGrad;
	
	private double[] biasesGrad;
	
	private double[][] softMaxWeightsGrad;
	
	/**
	 * accumulated gradients to do in one batch
	 */
	private double[][] wordEmbeddingsBatch;
	private double[][] tagEmbeddingsBatch;
	private double[][] labelEmbeddingsBatch;
	
	private double[][] wordWeightsBatch;
	private double[][] tagWeightsBatch;
	private double[][] labelWeightsBatch;
	
	private double[] biasesBatch;
	
	private double[][] softMaxWeightsBatch;
	
	
	
	public NeuralNetwork(int wordInputsCount, int tagInputsCount, int labelInputsCount,
			int vocabularySize, int tagsCount, int labelsCount,
			int hiddens, int transitionsCount, int embeddingSize) {
		this.wordInputsCount = wordInputsCount;
		this.tagInputsCount = tagInputsCount;
		this.labelInputsCount = labelInputsCount;
		this.embeddingSize = embeddingSize;
		this.currentBatch = 0;
		
		this.wordEmbeddings = MatrixOperations.randomInitialize(this.embeddingSize, vocabularySize, -0.01, 0.01);
		this.tagEmbeddings = MatrixOperations.randomInitialize(this.embeddingSize, tagsCount, -0.01, 0.01);
		this.labelEmbeddings = MatrixOperations.randomInitialize(this.embeddingSize, labelsCount, -0.01, 0.01);
		
		this.wordWeights = MatrixOperations.randomInitialize(hiddens, this.embeddingSize*this.wordInputsCount, -0.01, 0.01);
		this.tagWeights = MatrixOperations.randomInitialize(hiddens, this.embeddingSize*this.tagInputsCount, -0.01, 0.01);
		this.labelWeights = MatrixOperations.randomInitialize(hiddens, this.embeddingSize*this.labelInputsCount, -0.01, 0.01);
		
		this.biases = MatrixOperations.randomInitialize(hiddens, -0.01, 0.01);
		
		this.softMaxWeights = MatrixOperations.randomInitialize(transitionsCount, hiddens, -0.01, 0.01);
		
		this.wordEmbeddingsGrad = new double[this.embeddingSize][vocabularySize];
		this.tagEmbeddingsGrad = new double[this.embeddingSize][tagsCount];
		this.labelEmbeddingsGrad = new double[this.embeddingSize][labelsCount];
		
		this.wordWeightsGrad = new double[hiddens][this.embeddingSize*this.wordInputsCount];
		this.tagWeightsGrad = new double[hiddens][this.embeddingSize*this.tagInputsCount];
		this.labelWeightsGrad = new double[hiddens][this.embeddingSize*this.labelInputsCount];
		
		this.biasesGrad = new double[hiddens];
		
		this.softMaxWeightsGrad = new double[transitionsCount][hiddens];
		
		this.wordEmbeddingsBatch = new double[this.embeddingSize][vocabularySize];
		this.tagEmbeddingsBatch = new double[this.embeddingSize][tagsCount];
		this.labelEmbeddingsBatch = new double[this.embeddingSize][labelsCount];
		
		this.wordWeightsBatch = new double[hiddens][this.embeddingSize*this.wordInputsCount];
		this.tagWeightsBatch = new double[hiddens][this.embeddingSize*this.tagInputsCount];
		this.labelWeightsBatch = new double[hiddens][this.embeddingSize*this.labelInputsCount];
		
		this.biasesBatch = new double[hiddens];
		
		this.softMaxWeightsBatch = new double[transitionsCount][hiddens];
		
		this.printNN();
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
	
	public void train(Path trainFile) {
		List<TrainingExample> examples = new LinkedList<TrainingExample>();
		try {
			BufferedReader reader = Files.newBufferedReader(trainFile);
			//skip header
			String line = reader.readLine();
			while ((line = reader.readLine()) != null) {
				TrainingExample example = this.getTrainingExample(line);
				examples.add(example);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Could not open conllu file: " + trainFile + " " + e.getMessage());
		}
		if (!examples.isEmpty()) {
			this.train(examples);
		}
	}
	
	//private methods start from here
	
	private TrainingExample getTrainingExample(String line) {
		String[] rawExample = line.split(",");
		int[] wordInputs = new int[this.wordInputsCount];
		for (int i = 0; i < this.wordInputsCount; ++i) {
			wordInputs[i] = Integer.parseInt(rawExample[i]);
		}
		int[] tagInputs = new int[this.tagInputsCount];
		int offset = this.wordInputsCount;
		for (int i = 0; i < this.tagInputsCount; ++i) {
			tagInputs[i] = Integer.parseInt(rawExample[offset + i]);
		}
		int[] labelInputs = new int[this.labelInputsCount];
		offset += this.tagInputsCount;
		for (int i = 0; i < this.labelInputsCount; ++i) {
			labelInputs[i] = Integer.parseInt(rawExample[offset + i]);
		}
		int output = Integer.parseInt(rawExample[offset + this.labelInputsCount]);
		return new TrainingExample(wordInputs, tagInputs, labelInputs, output);
	}

	private void train(List<TrainingExample> data) {
		double correct = Double.NEGATIVE_INFINITY;
		TrainingExample[] randomAccessData = data.toArray(new TrainingExample[0]);
		int[] indices = MatrixOperations.initializeIndices(data.size());
		int iterations = 1;
		while (correct < NeuralNetwork.convergenceThreshold) {
			long start = System.currentTimeMillis();
			System.out.println("Starting iteration " + iterations);
			System.out.flush();
			this.trainIteration(randomAccessData, indices);
			System.out.println("Iteration took: " + (System.currentTimeMillis() - start) + " milliseconds");
			System.out.flush();
			start = System.currentTimeMillis();
			correct = this.countCorrect(data);
			System.out.println("Counting correct took: " + (System.currentTimeMillis() - start) + " milliseconds");
			System.out.println("Correct: " + correct);
			System.out.println("Iteration: " + iterations);
			System.out.flush();
			++iterations;
		}
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
		return MatrixOperations.powComponentWise(activationsSum, 3);
	}
	
	private double[] computeOutputs(double[] inputs) {
		//Note Do not compute exponents. Leads to very bad things. => Work with log probabilitites
		return MatrixOperations.multiply(this.softMaxWeights, inputs);
//		return MatrixOperations.exp(outputActivations);
	}
	
	private void trainIteration(TrainingExample[] data, int[] indices) {
		MatrixOperations.shuffle(indices);
		for (int i : indices) {
			this.printNN();
			this.updateWeights(data[i]);
		}
	}
	
	private double countCorrect(List<TrainingExample> data) {
		//compute Sum of Squared Errors (SSE) for data
		Iterator<TrainingExample> dataIt = data.iterator();
		int correct = 0;
		while (dataIt.hasNext()) {
			TrainingExample example = dataIt.next();
			int transition = this.chooseTransition(example.getWordInputs(), example.getTagInputs(), example.getLabelInputs());
//			System.out.println("Real: " + example.getOutput() + " Predicted: " + transition);
			if (transition == example.getOutput()) {
				++correct;
			}
		}
		//return percentage of errors
		System.out.println("Correct: " + correct);
		System.out.println("Total: " + data.size());
		return (double) (correct * 100) / (double) data.size();
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
		double[] hiddenActivations = MatrixOperations.powComponentWise(hiddenActivationsBase, 3);
		//compute output (they are already logged)
	    double[] loggedOutputs = this.computeOutputs(hiddenActivations);
	    
//	    System.out.println("Outputs:");
//	    this.printArray(loggedOutputs);
	    
	    //compute derivatives for SoftMax layer
	    //compute normalizing constant (sum of outputs)
	    double normalizingConstant = this.computeNormalizingConstant(loggedOutputs);
	    //compute derivatives (use regularizing penalty)
	    double[] outputDerivatives = MatrixOperations.multiply(loggedOutputs, 1/normalizingConstant);
	    outputDerivatives[example.getOutput()] -= 1;
	    double[][] softMaxWeightsDerivatives = MatrixOperations.outerProduct(outputDerivatives, hiddenActivations);
	    MatrixOperations.addInline(this.softMaxWeightsBatch, softMaxWeightsDerivatives);
	    
	    //compute derivatives for weights layer
	    double[] hiddenDerivatives = MatrixOperations.multiply(MatrixOperations.transpose(this.softMaxWeights), outputDerivatives);
	    double[] hiddenActivationDerivatives = MatrixOperations.powComponentWise(hiddenActivationsBase, 2);
	    MatrixOperations.multiplyInline(hiddenActivationDerivatives, 3);
	    
	    //compute bias derivatives as part of weights layer
	    double[] biasDerivatives = MatrixOperations.multiplyComponentWise(hiddenDerivatives, hiddenActivationDerivatives);
	    MatrixOperations.addInline(this.biasesBatch, biasDerivatives);
	    
	    //use bias derivatives to compute the rest of the derivatives for weights layer
	    double[][] wordWeightsDerivatives = MatrixOperations.outerProduct(biasDerivatives, inputWordEmbeddingsVector);
	    double[][] tagsWeightsDerivatives = MatrixOperations.outerProduct(biasDerivatives, inputTagsEmbeddingsVector);
	    double[][] labelWeightsDerivatives = MatrixOperations.outerProduct(biasDerivatives, inputLabelEmbeddingsVector);
	    MatrixOperations.addInline(this.wordWeightsBatch, wordWeightsDerivatives);
	    MatrixOperations.addInline(this.tagWeightsBatch, tagsWeightsDerivatives);
	    MatrixOperations.addInline(this.labelWeightsBatch, labelWeightsDerivatives);
	    
	    //compute embeddings derivatives
	    double[] commonMultipleVector = MatrixOperations.multiplyComponentWise(hiddenDerivatives, hiddenActivationDerivatives);
	    double[] wordEmbeddingsDerivatives = MatrixOperations.multiply(MatrixOperations.transpose(this.wordWeights), commonMultipleVector);
	    double[] tagEmbeddingsDerivatives = MatrixOperations.multiply(MatrixOperations.transpose(this.tagWeights), commonMultipleVector);
	    double[] labelEmbeddingsDerivatives = MatrixOperations.multiply(MatrixOperations.transpose(this.labelWeights), commonMultipleVector);
	    this.addEmbeddingsBatch(this.wordEmbeddingsBatch, wordEmbeddingsDerivatives, example.getWordInputs());
	    this.addEmbeddingsBatch(this.tagEmbeddingsBatch, tagEmbeddingsDerivatives, example.getTagInputs());
	    this.addEmbeddingsBatch(this.labelEmbeddingsBatch, labelEmbeddingsDerivatives, example.getLabelInputs());
	    
	    //Just accumulated another set of derivatives to the batch
	    ++this.currentBatch;
	    if (NeuralNetwork.batchSize != this.currentBatch) {
	    	return;
	    }
	    //else reset batch and continue to upgrading weights
	    this.currentBatch = 0;
		
	    //penalize batch
	    //penalize sofrMax
	    double[][] softMaxPenalty = MatrixOperations.multiply(this.softMaxWeights, NeuralNetwork.regularizingPenalty);
	    MatrixOperations.addInline(this.softMaxWeightsBatch, softMaxPenalty);
	    //penalize weights layer
	    double[][] wordWeightsPenalty = MatrixOperations.multiply(this.wordWeights, NeuralNetwork.regularizingPenalty);
	    MatrixOperations.addInline(this.wordWeightsBatch, wordWeightsPenalty);
	    double[][] tagWeightsPenalty = MatrixOperations.multiply(this.tagWeights, NeuralNetwork.regularizingPenalty);
	    MatrixOperations.addInline(this.tagWeightsBatch, tagWeightsPenalty);
	    double[][] labelWeightsPenalty = MatrixOperations.multiply(this.labelWeights, NeuralNetwork.regularizingPenalty);
	    MatrixOperations.addInline(this.labelWeightsBatch, labelWeightsPenalty);
	    //weights layer includes biases
	    double[] biasesPenalty = MatrixOperations.multiply(this.biases, NeuralNetwork.regularizingPenalty);
	    MatrixOperations.addInline(this.biasesBatch, biasesPenalty);
	    //penalize embeddings BUT only the ones that were actually used during this batch!
	    //their derivatives should be different from 0
	    this.penalizeEmbeddings(this.wordEmbeddingsBatch, this.wordEmbeddings);
	    this.penalizeEmbeddings(this.tagEmbeddingsBatch, this.tagEmbeddings);
	    this.penalizeEmbeddings(this.labelEmbeddingsBatch, this.labelEmbeddings);
	    
	    //AdaGrad
	    this.updateAdaGradWeights(this.softMaxWeightsGrad, this.softMaxWeightsBatch);
	    this.updateAdaGradWeights(this.biasesGrad, this.biasesBatch);
	    this.updateAdaGradWeights(this.wordWeightsGrad, this.wordWeightsBatch);
	    this.updateAdaGradWeights(this.tagWeightsGrad, this.tagWeightsBatch);
	    this.updateAdaGradWeights(this.labelWeightsGrad, this.labelWeightsBatch);
	    this.updateAdaGradWeights(this.wordEmbeddingsGrad, this.wordEmbeddingsBatch);
	    this.updateAdaGradWeights(this.tagEmbeddingsGrad, this.tagEmbeddingsBatch);
	    this.updateAdaGradWeights(this.labelEmbeddingsGrad, this.labelEmbeddingsBatch);
	    //update weights
	    this.updateSingleSetOfWeights(this.softMaxWeights, this.softMaxWeightsBatch, this.softMaxWeightsGrad);
	    this.updateSingleSetOfWeights(this.biases, this.biasesBatch, this.biasesGrad);
	    this.updateSingleSetOfWeights(this.wordWeights, this.wordWeightsBatch, this.wordWeightsGrad);
	    this.updateSingleSetOfWeights(this.tagWeights, this.tagWeightsBatch, this.tagWeightsGrad);
	    this.updateSingleSetOfWeights(this.labelWeights, this.labelWeightsBatch, this.labelWeightsGrad);
	    this.updateSingleSetOfWeights(this.wordEmbeddings, this.wordEmbeddingsBatch, this.wordEmbeddingsGrad);
	    this.updateSingleSetOfWeights(this.tagEmbeddings, this.tagEmbeddingsBatch, this.tagEmbeddingsGrad);
	    this.updateSingleSetOfWeights(this.labelEmbeddings, this.labelEmbeddingsBatch, this.labelEmbeddingsGrad);
	    
	    this.resetBatchDerivatives();
	}

	private double computeNormalizingConstant(double[] loggedOutputs) {
		//normalizingConstant = maxLoggedOutput + log(sum(exp(loggedOutputs - maxLoggedOutput)))
		//pick max logged output
		double maxLoggedOutput = MatrixOperations.max(loggedOutputs);
		double[] adjustedLoggedOutputs = MatrixOperations.subtract(loggedOutputs, maxLoggedOutput);
		double[] expedAdjustedLoggedOutputs = MatrixOperations.expComponentWise(adjustedLoggedOutputs);
		double sum = MatrixOperations.sumCoordinates(expedAdjustedLoggedOutputs);
		return maxLoggedOutput + Math.log(sum);
	}
	
	private void addEmbeddingsBatch(double[][] embeddingsBatch, double[] embeddings, int[] inputs) {
		for (int i = 0; i < embeddings.length; ++i) {
			int inputIndex = inputs[i / this.embeddingSize];
			if (0 == inputIndex) {
				i += this.embeddingSize - 1;
				continue;
			}
			--inputIndex;
			int embeddingCoordinate = i % this.embeddingSize;
			embeddingsBatch[embeddingCoordinate][inputIndex] += embeddings[i];
		}
	}
	
	private void penalizeEmbeddings(double[][] embeddingsBatch, double[][] embeddings) {
		for (int row = 0; row < embeddingsBatch.length; ++row) {
			for (int col = 0; col < embeddingsBatch[0].length; ++col) {
				if (0 == embeddingsBatch[row][col]) {
					continue;
				}//else
				embeddingsBatch[row][col] += NeuralNetwork.regularizingPenalty * embeddings[row][col];
			}
		}
	}
		
	private void updateAdaGradWeights(double[] weights, double[] weightsDerivatives) {
		double[] weightsDerivativesSquared = MatrixOperations.powComponentWise(weightsDerivatives, 2);
		MatrixOperations.addInline(weights, weightsDerivativesSquared);
	}
	
	private void updateAdaGradWeights(double[][] weights, double[][] weightsDerivatives) {
		double[][] weightsDerivativesSquared = MatrixOperations.powComponentWise(weightsDerivatives, 2);
		MatrixOperations.addInline(weights, weightsDerivativesSquared);
	}
	
	private void updateSingleSetOfWeights(double[] weights, double[] weightsDerivatives, double[] weightsGrad) {
		MatrixOperations.multiplyInline(weightsDerivatives, NeuralNetwork.learningRate);
		for (int i = 0; i < weightsDerivatives.length; ++i) {
			if (0 == weightsGrad[i]) {
				continue;
			}//else
			weightsDerivatives[i] /= Math.sqrt(weightsGrad[i]);
		}
		MatrixOperations.subtractInline(weights, weightsDerivatives);
	}
	
	private void updateSingleSetOfWeights(double[][] weights, double[][] weightsDerivatives, double[][] weightsGrad) {
		MatrixOperations.multiplyInline(weightsDerivatives, NeuralNetwork.learningRate);
		for (int row = 0; row < weightsDerivatives.length; ++row) {
			for (int col = 0; col < weightsDerivatives[0].length; ++col) {
				double grad = weightsGrad[row][col];
				if (0 == grad) {
					continue;
				}//else
				weightsDerivatives[row][col] /= Math.sqrt(grad);
			}
		}
		MatrixOperations.subtractInline(weights, weightsDerivatives);
	}
	
	private void resetBatchDerivatives() {
		this.setToZeros(this.wordEmbeddingsGrad);
		this.setToZeros(this.tagEmbeddingsGrad);
		this.setToZeros(this.labelEmbeddingsGrad);

		this.setToZeros(this.wordWeightsGrad);
		this.setToZeros(this.tagWeightsGrad);
		this.setToZeros(this.labelWeightsGrad);

		Arrays.fill(this.biasesGrad, 0);

		this.setToZeros(this.softMaxWeightsGrad);
	}
	
	private void setToZeros(double[][] matrix) {
		for (int row = 0; row < matrix.length; ++row) {
			Arrays.fill(matrix[row], 0);
		}
	}
	
	//debugging purposes
	private void printNN() {
		boolean debug = false;
		if (!debug) {
			return;
		}
		System.out.println("Word Inputs: " + this.wordInputsCount);
		System.out.println("Tag Inputs: " + this.tagInputsCount);
		System.out.println("Label Inputs: " + this.labelInputsCount);
		System.out.println("Embedding Size: " + this.embeddingSize);

		System.out.println("Wrod Embeddings:");
		this.printMatrix(this.wordEmbeddings);
		System.out.println("Tag Embeddings:");
		this.printMatrix(this.tagEmbeddings);
		System.out.println("Label Embeddings:");
		this.printMatrix(this.labelEmbeddings);
		
		System.out.println("Wrod Weights:");
		this.printMatrix(this.wordWeights);
		System.out.println("Tag Weights:");
		this.printMatrix(this.tagWeights);
		System.out.println("Label Weights:");
		this.printMatrix(this.labelWeights);
		
		System.out.println("Biases");
		this.printArray(this.biases);
		
		System.out.println("SoftMaxWeights");
		this.printMatrix(this.softMaxWeights);

	}
	
	private void printMatrix(double[][] matrix) {
		for (int row = 0; row < matrix.length; ++row) {
			for (int col = 0; col < matrix[0].length; ++col) {
				System.out.print(matrix[row][col] + "\t");
			}
			System.out.println();
		}
	}
	
	private void printArray(double[] array) {
		for (int i = 0; i < array.length; ++i) {
			System.out.print(array[i] + "\t");
		}
		System.out.println();
	}
}
