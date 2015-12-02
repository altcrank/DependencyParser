import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jblas.DoubleMatrix;

import common.MatrixOperations;

public class NeuralNetworkJBLAS implements Serializable {
	
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
	
	private DoubleMatrix wordEmbeddings;
	private DoubleMatrix tagEmbeddings;
	private DoubleMatrix labelEmbeddings;
	
	private DoubleMatrix wordWeights;
	private DoubleMatrix tagWeights;
	private DoubleMatrix labelWeights;
	
	private DoubleMatrix biases;
	
	private DoubleMatrix softMaxWeights;
	
	/**
	 * hyperparams
	 */
	private static final double convergenceThreshold = 95;
	private static double learningRate = 0.001;
	private static final double regularizingPenalty = 0.000001;
	private static final int batchSize = 100;
	
	/**
	 * accumulated squared gradients for AdaGrad (Adaptive Gradient)
	 */
	private DoubleMatrix wordEmbeddingsGrad;
	private DoubleMatrix tagEmbeddingsGrad;
	private DoubleMatrix labelEmbeddingsGrad;
	
	private DoubleMatrix wordWeightsGrad;
	private DoubleMatrix tagWeightsGrad;
	private DoubleMatrix labelWeightsGrad;
	
	private DoubleMatrix biasesGrad;
	
	private DoubleMatrix softMaxWeightsGrad;
	
	/**
	 * accumulated gradients to do in one batch
	 */
	private DoubleMatrix wordEmbeddingsBatch;
	private DoubleMatrix tagEmbeddingsBatch;
	private DoubleMatrix labelEmbeddingsBatch;
	
	private DoubleMatrix wordWeightsBatch;
	private DoubleMatrix tagWeightsBatch;
	private DoubleMatrix labelWeightsBatch;
	
	private DoubleMatrix biasesBatch;
	
	private DoubleMatrix softMaxWeightsBatch;
	
	
	
	public NeuralNetworkJBLAS(int wordInputsCount, int tagInputsCount, int labelInputsCount,
			int vocabularySize, int tagsCount, int labelsCount,
			int hiddens, int transitionsCount, int embeddingSize) {
		this.wordInputsCount = wordInputsCount;
		this.tagInputsCount = tagInputsCount;
		this.labelInputsCount = labelInputsCount;
		this.embeddingSize = embeddingSize;
		this.currentBatch = 0;
		
		this.wordEmbeddings = new DoubleMatrix(MatrixOperations.randomInitialize(this.embeddingSize, vocabularySize, -0.01, 0.01));
		this.tagEmbeddings = new DoubleMatrix(MatrixOperations.randomInitialize(this.embeddingSize, tagsCount, -0.01, 0.01));
		this.labelEmbeddings = new DoubleMatrix(MatrixOperations.randomInitialize(this.embeddingSize, labelsCount, -0.01, 0.01));
		
		this.wordWeights = new DoubleMatrix(MatrixOperations.randomInitialize(hiddens, this.embeddingSize*this.wordInputsCount, -0.01, 0.01));
		this.tagWeights = new DoubleMatrix(MatrixOperations.randomInitialize(hiddens, this.embeddingSize*this.tagInputsCount, -0.01, 0.01));
		this.labelWeights = new DoubleMatrix(MatrixOperations.randomInitialize(hiddens, this.embeddingSize*this.labelInputsCount, -0.01, 0.01));
		
		this.biases = new DoubleMatrix(MatrixOperations.randomInitialize(hiddens, -0.01, 0.01));
		
		this.softMaxWeights = new DoubleMatrix(MatrixOperations.randomInitialize(transitionsCount, hiddens, -0.01, 0.01));
		
		this.wordEmbeddingsGrad = DoubleMatrix.zeros(this.embeddingSize, vocabularySize);
		this.tagEmbeddingsGrad = DoubleMatrix.zeros(this.embeddingSize, tagsCount);
		this.labelEmbeddingsGrad = DoubleMatrix.zeros(this.embeddingSize, labelsCount);
		
		this.wordWeightsGrad = DoubleMatrix.zeros(hiddens, this.embeddingSize*this.wordInputsCount);
		this.tagWeightsGrad = DoubleMatrix.zeros(hiddens, this.embeddingSize*this.tagInputsCount);
		this.labelWeightsGrad = DoubleMatrix.zeros(hiddens, this.embeddingSize*this.labelInputsCount);
		
		this.biasesGrad = DoubleMatrix.zeros(hiddens);
		
		this.softMaxWeightsGrad = DoubleMatrix.zeros(transitionsCount, hiddens);
		
		this.wordEmbeddingsBatch = DoubleMatrix.zeros(this.embeddingSize, vocabularySize);
		this.tagEmbeddingsBatch = DoubleMatrix.zeros(this.embeddingSize, tagsCount);
		this.labelEmbeddingsBatch = DoubleMatrix.zeros(this.embeddingSize, labelsCount);
		
		this.wordWeightsBatch = DoubleMatrix.zeros(hiddens, this.embeddingSize*this.wordInputsCount);
		this.tagWeightsBatch = DoubleMatrix.zeros(hiddens, this.embeddingSize*this.tagInputsCount);
		this.labelWeightsBatch = DoubleMatrix.zeros(hiddens, this.embeddingSize*this.labelInputsCount);
		
		this.biasesBatch = DoubleMatrix.zeros(hiddens);
		
		this.softMaxWeightsBatch = DoubleMatrix.zeros(transitionsCount, hiddens);
		
//		this.printNN();
	}
	
	public int chooseTransition(int[] wordInputs, int[] tagInputs, int[] labelInputs) {
		DoubleMatrix inputWordEmbeddingsVector = this.getEmbeddingsVector(this.wordEmbeddings, wordInputs);
		DoubleMatrix inputTagsEmbeddingsVector = this.getEmbeddingsVector(this.tagEmbeddings, tagInputs);
		DoubleMatrix inputLabelEmbeddingsVector = this.getEmbeddingsVector(this.labelEmbeddings, labelInputs);
		
		DoubleMatrix hiddenActivations = this.computeHiddenActivations(inputWordEmbeddingsVector,
																	   inputTagsEmbeddingsVector,
																	   inputLabelEmbeddingsVector);
		
		DoubleMatrix outputs = this.computeOutputs(hiddenActivations);
		
		return outputs.argmax();
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
		while (correct < NeuralNetworkJBLAS.convergenceThreshold) {
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
	
	private DoubleMatrix getEmbeddingsVector(DoubleMatrix embeddings, int[] inputs) {
		double[] inputEmbeddingsVector = new double[inputs.length*this.embeddingSize];
		for (int i = 0; i < this.embeddingSize; ++i) {
			for (int input = 0; input < inputs.length; ++input) {
				if (0 == inputs[input]) {
					inputEmbeddingsVector[input*this.embeddingSize + i] = 0;
				} else {
					inputEmbeddingsVector[input*this.embeddingSize + i] = embeddings.get(i, inputs[input]-1);	
				}
			}
		}
		return new DoubleMatrix(inputEmbeddingsVector);
	}
	
	private DoubleMatrix computeHiddenActivationsBase(DoubleMatrix inputWordEmbeddings,
			DoubleMatrix inputTagEmbeddings, DoubleMatrix inputLabelEmbeddings) {
		DoubleMatrix wordActivations = this.wordWeights.mmul(inputWordEmbeddings);
		DoubleMatrix tagActivations = this.tagWeights.mmul(inputTagEmbeddings);
		DoubleMatrix labelActivations = this.labelWeights.mmul(inputLabelEmbeddings);
		DoubleMatrix activationsSum = wordActivations.add(tagActivations);
		activationsSum.addi(labelActivations);
		activationsSum.addi(this.biases);
		return activationsSum;
	}
	
	private DoubleMatrix computeHiddenActivations(DoubleMatrix inputWordEmbeddings,
			DoubleMatrix inputTagEmbeddings, DoubleMatrix inputLabelEmbeddings) {
		DoubleMatrix activationsSum = this.computeHiddenActivationsBase(inputWordEmbeddings, inputTagEmbeddings, inputLabelEmbeddings);
		return activationsSum.mul(activationsSum).mul(activationsSum);
	}
	
	private DoubleMatrix computeOutputs(DoubleMatrix inputs) {
		//Note Do not compute exponents. Leads to very bad things. => Work with log probabilitites
		return this.softMaxWeights.mmul(inputs);
	}
	
	private void trainIteration(TrainingExample[] data, int[] indices) {
		MatrixOperations.shuffle(indices);
		int examples = 0;
		long start = System.currentTimeMillis();
		for (int i : indices) {
			if (examples != 0 && 0 == examples % 10000) {
				long end = System.currentTimeMillis();
				System.out.println(examples + "/" + data.length + " examples for " + (end - start) + "ms.");
			}
//			this.printNN();
			this.updateWeights(data[i]);
			++examples;
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
		DoubleMatrix inputWordEmbeddingsVector = this.getEmbeddingsVector(this.wordEmbeddings, example.getWordInputs());
		DoubleMatrix inputTagsEmbeddingsVector = this.getEmbeddingsVector(this.tagEmbeddings, example.getTagInputs());
		DoubleMatrix inputLabelEmbeddingsVector = this.getEmbeddingsVector(this.labelEmbeddings, example.getLabelInputs());
		//compute hidden activations
		DoubleMatrix hiddenActivationsBase = this.computeHiddenActivationsBase(inputWordEmbeddingsVector,
					    													   inputTagsEmbeddingsVector,
					    													   inputLabelEmbeddingsVector);
		
		DoubleMatrix hiddenActivationDerivatives = hiddenActivationsBase.mul(hiddenActivationsBase);
		DoubleMatrix hiddenActivations = hiddenActivationDerivatives.mul(hiddenActivationsBase);
		//compute output (they are already logged)
	    DoubleMatrix loggedOutputs = this.computeOutputs(hiddenActivations);
	    
//	    System.out.println("Outputs:");
//	    this.printArray(loggedOutputs);
	    
	    //compute derivatives for SoftMax layer
	    //compute normalizing constant (sum of outputs)
	    double normalizingConstant = this.computeNormalizingConstant(loggedOutputs);
	    //compute derivatives (use regularizing penalty)
	    DoubleMatrix outputDerivatives = loggedOutputs.mul(1/normalizingConstant);
	    outputDerivatives.put(example.getOutput(), outputDerivatives.get(example.getOutput()) - 1);
	    DoubleMatrix softMaxWeightsDerivatives = outputDerivatives.mmul(hiddenActivations.transpose());
	    this.softMaxWeightsBatch.addi(softMaxWeightsDerivatives);
	    
	    //compute derivatives for weights layer
	    DoubleMatrix hiddenDerivatives = this.softMaxWeights.transpose().mmul(outputDerivatives);
	    
	    hiddenActivationDerivatives.muli(3);
	    
	    //compute bias derivatives as part of weights layer
	    DoubleMatrix biasDerivatives = hiddenDerivatives.mul(hiddenActivationDerivatives);
	    this.biasesBatch.addi(biasDerivatives);
	    
	    //use bias derivatives to compute the rest of the derivatives for weights layer
	    DoubleMatrix wordWeightsDerivatives = biasDerivatives.mmul(inputWordEmbeddingsVector.transpose());
	    DoubleMatrix tagsWeightsDerivatives = biasDerivatives.mmul(inputTagsEmbeddingsVector.transpose());
	    DoubleMatrix labelWeightsDerivatives = biasDerivatives.mmul(inputLabelEmbeddingsVector.transpose());
	    this.wordWeightsBatch.addi(wordWeightsDerivatives);
	    this.tagWeightsBatch.addi(tagsWeightsDerivatives);
	    this.labelWeightsBatch.addi(labelWeightsDerivatives);
	    
	    //compute embeddings derivatives
	    DoubleMatrix commonMultipleVector = hiddenDerivatives.mul(hiddenActivationDerivatives);
	    DoubleMatrix wordEmbeddingsDerivatives = this.wordWeights.transpose().mmul(commonMultipleVector);
	    DoubleMatrix tagEmbeddingsDerivatives = this.tagWeights.transpose().mmul(commonMultipleVector);
	    DoubleMatrix labelEmbeddingsDerivatives = this.labelWeights.transpose().mmul(commonMultipleVector);
	    this.addEmbeddingsBatch(this.wordEmbeddingsBatch, wordEmbeddingsDerivatives, example.getWordInputs());
	    this.addEmbeddingsBatch(this.tagEmbeddingsBatch, tagEmbeddingsDerivatives, example.getTagInputs());
	    this.addEmbeddingsBatch(this.labelEmbeddingsBatch, labelEmbeddingsDerivatives, example.getLabelInputs());
	    
	    //Just accumulated another set of derivatives to the batch
	    ++this.currentBatch;
	    if (NeuralNetworkJBLAS.batchSize != this.currentBatch) {
	    	return;
	    }
	    //else reset batch and continue to upgrading weights
	    this.currentBatch = 0;
		
	    //penalize batch
	    //penalize sofrMax
	    DoubleMatrix softMaxPenalty = this.softMaxWeights.mul(NeuralNetworkJBLAS.regularizingPenalty);
	    this.softMaxWeightsBatch.addi(softMaxPenalty);
	    //penalize weights layer
	    DoubleMatrix wordWeightsPenalty = this.wordWeights.mul(NeuralNetworkJBLAS.regularizingPenalty);
	    this.wordWeightsBatch.addi(wordWeightsPenalty);
	    DoubleMatrix tagWeightsPenalty = this.tagWeights.mul(NeuralNetworkJBLAS.regularizingPenalty);
	    this.tagWeightsBatch.addi(tagWeightsPenalty);
	    DoubleMatrix labelWeightsPenalty = this.labelWeights.mul(NeuralNetworkJBLAS.regularizingPenalty);
	    this.labelWeightsBatch.addi(labelWeightsPenalty);
	    //weights layer includes biases
	    DoubleMatrix biasesPenalty = this.biases.mul(NeuralNetworkJBLAS.regularizingPenalty);
	    this.biasesBatch.addi(biasesPenalty);
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

	private double computeNormalizingConstant(DoubleMatrix loggedOutputs) {
		//normalizingConstant = maxLoggedOutput + log(sum(exp(loggedOutputs - maxLoggedOutput)))
		//pick max logged output
		double maxLoggedOutput = loggedOutputs.max();
		DoubleMatrix adjustedLoggedOutputs = loggedOutputs.sub(maxLoggedOutput);
		DoubleMatrix expedAdjustedLoggedOutputs = MatrixOperations.expComponentWise(adjustedLoggedOutputs);
		double sum = expedAdjustedLoggedOutputs.sum();
		return maxLoggedOutput + Math.log(sum);
	}
	
	private void addEmbeddingsBatch(DoubleMatrix embeddingsBatch, DoubleMatrix embeddings, int[] inputs) {
		for (int i = 0; i < embeddings.length; ++i) {
			int inputIndex = inputs[i / this.embeddingSize];
			if (0 == inputIndex) {
				i += this.embeddingSize - 1;
				continue;
			}
			--inputIndex;
			int embeddingCoordinate = i % this.embeddingSize;
			double orig = embeddingsBatch.get(embeddingCoordinate, inputIndex);
			embeddingsBatch.put(embeddingCoordinate, inputIndex, orig + embeddings.get(i));
		}
	}
	
	private void penalizeEmbeddings(DoubleMatrix embeddingsBatch, DoubleMatrix embeddings) {
		for (int row = 0; row < embeddingsBatch.rows; ++row) {
			for (int col = 0; col < embeddingsBatch.columns; ++col) {
				double embedding = embeddingsBatch.get(row, col);
				if (0 == embedding) {
					continue;
				}//else
				embeddingsBatch.put(row, col, embedding + NeuralNetworkJBLAS.regularizingPenalty * embeddings.get(row, col));
			}
		}
	}
		
	private void updateAdaGradWeights(DoubleMatrix weights, DoubleMatrix weightsDerivatives) {
		weightsDerivatives.muli(weightsDerivatives);
		weights.addi(weightsDerivatives);
	}
	
	private void updateSingleSetOfWeights(DoubleMatrix weights, DoubleMatrix weightsDerivatives, DoubleMatrix weightsGrad) {
		weightsDerivatives.muli(NeuralNetworkJBLAS.learningRate);
		for (int i = 0; i < weightsDerivatives.length; ++i) {
			double weightGrad = weightsGrad.get(i);
			if (0 == weightGrad) {
				continue;
			}//else
			weightsDerivatives.put(i, weightsDerivatives.get(i) / Math.sqrt(weightGrad));
		}
		weights.subi(weightsDerivatives);
	}
	
	private void resetBatchDerivatives() {
		this.wordEmbeddingsGrad = DoubleMatrix.zeros(this.wordEmbeddingsGrad.rows, this.wordEmbeddingsGrad.columns);
		this.wordEmbeddingsGrad = DoubleMatrix.zeros(this.wordEmbeddingsGrad.rows, this.wordEmbeddingsGrad.columns);
		this.tagEmbeddingsGrad = DoubleMatrix.zeros(this.tagEmbeddingsGrad.rows, this.tagEmbeddingsGrad.columns);
		this.labelEmbeddingsGrad = DoubleMatrix.zeros(this.labelEmbeddingsGrad.rows, this.labelEmbeddingsGrad.columns);

		this.wordWeightsGrad = DoubleMatrix.zeros(this.wordWeightsGrad.rows, this.wordWeightsGrad.columns);
		this.tagWeightsGrad = DoubleMatrix.zeros(this.tagWeightsGrad.rows, this.tagWeightsGrad.columns);
		this.labelWeightsGrad = DoubleMatrix.zeros(this.labelWeightsGrad.rows, this.labelWeightsGrad.columns);

		this.biasesGrad = DoubleMatrix.zeros(this.biasesGrad.rows, this.biasesGrad.columns);

		this.softMaxWeightsGrad = DoubleMatrix.zeros(this.softMaxWeightsGrad.rows, this.softMaxWeightsGrad.columns);
	}
	
//	//debugging purposes
//	private void printNN() {
//		boolean debug = false;
//		if (!debug) {
//			return;
//		}
//		System.out.println("Word Inputs: " + this.wordInputsCount);
//		System.out.println("Tag Inputs: " + this.tagInputsCount);
//		System.out.println("Label Inputs: " + this.labelInputsCount);
//		System.out.println("Embedding Size: " + this.embeddingSize);
//
//		System.out.println("Wrod Embeddings:");
//		this.printMatrix(this.wordEmbeddings);
//		System.out.println("Tag Embeddings:");
//		this.printMatrix(this.tagEmbeddings);
//		System.out.println("Label Embeddings:");
//		this.printMatrix(this.labelEmbeddings);
//		
//		System.out.println("Wrod Weights:");
//		this.printMatrix(this.wordWeights);
//		System.out.println("Tag Weights:");
//		this.printMatrix(this.tagWeights);
//		System.out.println("Label Weights:");
//		this.printMatrix(this.labelWeights);
//		
//		System.out.println("Biases");
//		this.printArray(this.biases);
//		
//		System.out.println("SoftMaxWeights");
//		this.printMatrix(this.softMaxWeights);
//
//	}
//	
//	private void printMatrix(double[][] matrix) {
//		for (int row = 0; row < matrix.length; ++row) {
//			for (int col = 0; col < matrix[0].length; ++col) {
//				System.out.print(matrix[row][col] + "\t");
//			}
//			System.out.println();
//		}
//	}
//	
//	private void printArray(double[] array) {
//		for (int i = 0; i < array.length; ++i) {
//			System.out.print(array[i] + "\t");
//		}
//		System.out.println();
//	}
}

