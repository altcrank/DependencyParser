import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import common.MatrixOperations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class DependencyParser {
	
	private Path filePath;
	private BufferedReader reader;
	private boolean EOFreached;
	private static final String header =  //words
										  "s1,lc1(s1),lc1(lc1(s1)),lc2(s1),rc1(s1),rc1(rc1(s1)),rc2(s1),"
										+ "s2,lc1(s2),lc1(lc1(s2)),lc2(s2),rc1(s2),rc1(rc1(s2)),rc2(s2),"
										+ "s3,b1,b2,b3,"
										  //corresponding POS tags
										+ "s1.t,lc1(s1).t,lc1(lc1(s1)).t,lc2(s1).t,rc1(s1).t,rc1(rc1(s1)).t,rc2(s1).t,"
										+ "s2.t,lc1(s2).t,lc1(lc1(s2)).t,lc2(s2).t,rc1(s2).t,rc1(rc1(s2)).t,rc2(s2).t,"
										+ "s3.t,b1.t,b2.t,b3.t,"
										  //corresponding arcs
										  //except the 3 words on the stack and the 3 on the buffer since they don't have eny yet
										+ "lc1(s1).l,lc1(lc1(s1)).l,lc2(s1).l,rc1(s1).l,rc1(rc1(s1)).l,rc2(s1).l,"
										+ "lc1(s2).l,lc1(lc1(s2)).l,lc2(s2).l,rc1(s2).l,rc1(rc1(s2)).l,rc2(s2).l,"
										  //transition
										+ "transition";
	
	private ArcStandard arcStandard;
	private NeuralNetwork network;
	private static final double convergenceThreshold = 10;
	
//	private NeuralNetworkJBLAS jnetwork;
	
	private Map<Integer, String> vocabulary;
	private Map<Integer, String> tags;
	private Map<Integer, String> labels;
	private List<String> labelsList;
	private Map<String, Integer> reverseVocabulary;
	private Map<String, Integer> reverseTags;
	private Map<String, Integer> reverseLabels;
	
	private Set<String> trainVocabulary;
	
	public DependencyParser() {
		this.filePath = null;
		this.reader = null;
		this.EOFreached = true;
		//arc standard needs to be passed labels
		this.arcStandard = null;
		this.vocabulary = new HashMap<Integer, String>();
		this.tags = new HashMap<Integer, String>();
		this.labels = new HashMap<Integer, String>();
		//we want to know how many labels we have before we initialize this
		this.labelsList = new LinkedList<String>();
		this.reverseVocabulary = new HashMap<String, Integer>();
		this.reverseTags = new HashMap<String, Integer>();
		this.reverseLabels = new HashMap<String, Integer>();
		this.trainVocabulary = new HashSet<String>();
	}
	
	public void openFile(Path filePath) {
		this.EOFreached = false;
		this.filePath = filePath;
		try {
			reader = Files.newBufferedReader(this.filePath);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Could not open conllu file: " + filePath + " " + e.getMessage());
		}
	}
	
	public void buildVocabulary(Path train, Path valid, Path test) {
		this.collectData(train);
		this.collectData(valid);
		this.collectData(test);
	}
	
	private void collectData(Path file) {
		this.openFile(file);
		//Tree sets because for some reason I though it might be nice
		//if they are ordered alphabetically
		Set<String> words = new TreeSet<String>();
		Set<String> tags = new TreeSet<String>();
		Set<String> labels = new TreeSet<String>();
		String line = new String();
		while (line != null) {
			try {
				line = this.reader.readLine();
				if (line == null) {
					this.EOFreached = true;
					break;
				}
			} catch (IOException e) {
				System.err.println("Failed to read word" + " " + e.getMessage());
				continue;
			}
			if (line.isEmpty() || line.charAt(0) == '#') {
				continue;
			}
			String[] parts = line.split("\t");
			Token token = new Token(parts, 0);
			words.add(token.getLemma());
			tags.add(token.getPOSTag());
			labels.add(token.getLabel());
		}
		
		this.populateVocabulary(words);
		this.populatePOStags(tags);
		this.populateLabels(labels);
	}
	
	public void serializeVocabulary(Path vocab) {
		try {
			OutputStream fileOut = Files.newOutputStream(vocab);
			ObjectOutputStream objOutStream = new ObjectOutputStream(fileOut);
			objOutStream.writeObject(this.reverseVocabulary);
			objOutStream.writeObject(this.reverseTags);
			objOutStream.writeObject(this.reverseLabels);
			objOutStream.writeObject(this.labelsList);
			objOutStream.close();
			fileOut.close();
			System.out.println("Serialized in " + vocab.toString());
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Some step of serialization failed..." + " " + e.getMessage());
		}
	}
	
	@SuppressWarnings("unchecked")
	public void deserializeVocabulary(Path vocab) {
		try {
			InputStream fileIn = Files.newInputStream(vocab);
			ObjectInputStream objInStream = new ObjectInputStream(fileIn);
			try {
				this.reverseVocabulary = (HashMap<String, Integer>) objInStream.readObject();
				this.reverseTags = (HashMap<String, Integer>) objInStream.readObject();
				this.reverseLabels = (HashMap<String, Integer>) objInStream.readObject();
				this.labelsList = (List<String>) objInStream.readObject();
				System.out.println("Loaded model from: " + vocab.toString());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				System.err.println("Failed to deserialize object" + " " + e.getMessage());
			}
			objInStream.close();
			fileIn.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Some step of deserialization failed..." + " " + e.getMessage());
		}
		this.completeParser();
	}
	
	private void completeParser() {
		for (Map.Entry<String, Integer> entry : this.reverseVocabulary.entrySet()) {
			this.vocabulary.put(entry.getValue(), entry.getKey());
		}
		for (Map.Entry<String, Integer> entry : this.reverseTags.entrySet()) {
			this.tags.put(entry.getValue(), entry.getKey());
		}
		for (Map.Entry<String, Integer> entry : this.reverseLabels.entrySet()) {
			this.labels.put(entry.getValue(), entry.getKey());
		}
		this.initializeArcStandard();
	}
	
	public void initializeData() {
		//Tree sets because for some reason I though it might be nice
		//if they are ordered alphabetically
		Set<String> words = new TreeSet<String>();
		Set<String> tags = new TreeSet<String>();
		Set<String> labels = new TreeSet<String>();
		String line = new String();
		while (line != null) {
			try {
				line = this.reader.readLine();
				if (line == null) {
					this.EOFreached = true;
					break;
				}
			} catch (IOException e) {
				System.err.println("Failed to read word" + " " + e.getMessage());
				continue;
			}
			if (line.isEmpty() || line.charAt(0) == '#') {
				continue;
			}
			String[] parts = line.split("\t");
			Token token = new Token(parts, 0);
			words.add(token.getLemma());
			tags.add(token.getPOSTag());
			labels.add(token.getLabel());
		}
		
		this.populateVocabulary(words);
		this.populatePOStags(tags);
		this.populateLabels(labels);
		this.initializeArcStandard();
		this.initializeNeuralNetwork();
		
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
		this.openFile(filePath);
	}
	
	public void printVocabulary() {
		printMap(vocabulary);
	}
	
	public void printTags() {
		printMap(tags);
	}
	
	public void printLabels() {
		printMap(labels);
	}
	
	public void printMap(Map<Integer, String> map) {
		for (Map.Entry<Integer, String> entry : map.entrySet()) {
			System.out.println(entry.getKey().toString() + " " + entry.getValue());
		}
	}
	
	public String getWord(int id) {
		return this.vocabulary.get(id);
	}
	
	public boolean hasNextSentence() {
		return !this.EOFreached;
	}
	
	public List<Token> tokenizeNextSentence() {
		String line = new String();
		List<Token> words = new LinkedList<Token>();
		while (line != null) {
			try {
				line = this.reader.readLine();
				if (line == null) {
					this.EOFreached = true;
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println(e.getMessage());
				continue;
			}
			if (line.isEmpty() || line.charAt(0) == '#') {
				break;
			}
			String[] parts = line.split("\t");
			words.add(new Token(parts, this.reverseVocabulary.get(parts[2])));
		}
		if (this.EOFreached) {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println(e.getMessage());
			}
		}
		return words;
	}
	
	public DependencyTree getSentenceDependencyTree(List<Token> sentence) {
		DependencyTree dTree = new DependencyTree();
		for (int i = 0; i < sentence.size(); ++i) {
			Token token = sentence.get(i);

			int headIdx = token.getHead();
			int headSentenceIndex = (0 == headIdx) ? headIdx : sentence.get(headIdx - 1).getSentenceId();
			int depSentenceIndex = token.getSentenceId();
			dTree.add(new Arc(headSentenceIndex, depSentenceIndex, token.getLabel()));
		}
		return dTree;
	}
	
	public DependencyTree getOracleDependencyTree(List<Token> sentence, DependencyTree goldTree) {
		Configuration c = this.arcStandard.initialConfiguration(sentence);
		while (!this.arcStandard.isTerminal(c)) {
			String transition = this.arcStandard.getOracle(c, goldTree);
			this.arcStandard.apply(c, transition);
		}
		DependencyTree predictedTree = c.getDependencyTree();
		predictedTree.sort();
		return predictedTree;
	}
	
	public void saveOracleDependencyTreeParse(List<Token> sentence, DependencyTree goldTree, PrintWriter writer) {
		Configuration c = this.arcStandard.initialConfiguration(sentence);
		List<List<Integer>> parseFeatures = new LinkedList<List<Integer>>();
		List<String> transitions = new LinkedList<String>();
		while (!this.arcStandard.isTerminal(c)) {
			List<Integer> features = this.getFeatures(c);
			parseFeatures.add(features);
			String transition = this.arcStandard.getOracle(c, goldTree);
			transitions.add(transition);
			this.arcStandard.apply(c, transition);
		}
		DependencyTree predictedTree = c.getDependencyTree();
		predictedTree.sort();
		boolean equal = goldTree.equals(predictedTree);
		if (equal) {
			Iterator<List<Integer>> featIt = parseFeatures.iterator();
			Iterator<String> trIt = transitions.iterator();
			while (featIt.hasNext() && trIt.hasNext()) {
				List<Integer> features = featIt.next();
				int transitionId = this.arcStandard.getTransitionId(trIt.next());
				String featureString = "";
				for (int feature : features) {
					featureString += feature + ",";
				}
				writer.println(featureString + transitionId);
			}
		}
	}
	
	public void loadModel(Path modelFile) {
		try {
			InputStream fileIn = Files.newInputStream(modelFile);
			ObjectInputStream objInStream = new ObjectInputStream(fileIn);
			try {
				this.network = (NeuralNetwork) objInStream.readObject();
//				this.jnetwork = (NeuralNetworkJBLAS) objInStream.readObject();
				System.out.println("Loaded model from: " + modelFile.toString());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				System.err.println("Failed to deserialize object" + " " + e.getMessage());
			}
			objInStream.close();
			fileIn.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Some step of deserialization failed..." + " " + e.getMessage());
		}
		this.initializeArcStandard();
	}
	
	public void train(Path inputPath, Path validationFile, Path modelFile) {
		File mFile = new File(modelFile.toString());
		Path vocab = FileSystems.getDefault().getPath("data/", "vocabulary.mem");
		this.deserializeVocabulary(vocab);
		if (mFile.exists()) {
			this.loadModel(modelFile);
		} else {
			this.initializeNeuralNetwork();
		}
		Path trainFile = FileSystems.getDefault().getPath("data", "trainingdata.csv");
		Path validFile = FileSystems.getDefault().getPath("data", "validationdata.csv");
		this.generateData(inputPath, trainFile, true);
		this.generateData(validationFile, validFile, false);
		List<TrainingExample> trainExamples = this.getExamples(trainFile);
		List<TrainingExample> validExamples = this.getExamples(validFile);
		
		if (!trainExamples.isEmpty()) {
			this.train(trainExamples, validExamples, modelFile);
		}
		this.serialize(modelFile);
	}
	
	public DependencyTree predict(List<Token> sentence) {
		Configuration c = this.arcStandard.initialConfiguration(sentence);
		while (!this.arcStandard.isTerminal(c)) {
			ConfigurationState state = this.getConfigurationState(c);
			int[] wordInputs = this.listToArray(state.getWords());
			int[] tagInputs = this.listToArray(state.getTags());
			int[] labelInputs = this.listToArray(state.getLabels());
			int best = 0;
			int transitionId = this.network.chooseTransition(wordInputs, tagInputs, labelInputs, best);
			String transition = this.arcStandard.getTransition(transitionId);
			while (!this.arcStandard.canApply(c, transition)) {
				++best;
				transitionId = this.network.chooseTransition(wordInputs, tagInputs, labelInputs, best);
				transition = this.arcStandard.getTransition(transitionId);
			}
//			int transitionId = this.jnetwork.chooseTransition(wordInputs, tagInputs, labelInputs);
			this.arcStandard.apply(c, transition);
		}
		DependencyTree predictedTree = c.getDependencyTree();
		predictedTree.sort();
		return predictedTree;
	}
	
	public void test(Path testFile) {
		int correct = 0;
		int total = 0;
		this.openFile(testFile);
		double uas = 0;
		double las = 0;
		int nonprojective = 0;
		double puas = 0;
		double plas = 0;
		int correctWithUnknownWords = 0;
		int wrongWithUnknownWords = 0;
		
		Path wrongProjective = FileSystems.getDefault().getPath("data", "projectiveTrees.txt");
		Path wrongNonProjective = FileSystems.getDefault().getPath("data", "nonprojectiveTrees.txt");
		Path unknownWordsPath = FileSystems.getDefault().getPath("data/", "unknownWords.txt");
		
		PrintWriter writerProj = null;
		PrintWriter writerNonProj = null;
		PrintWriter unknownWordsWriter = null;
		try {
			writerProj = new PrintWriter(wrongProjective.toFile(), "UTF-8");
			writerNonProj = new PrintWriter(wrongNonProjective.toFile(), "UTF-8");
			unknownWordsWriter = new PrintWriter(unknownWordsPath.toFile(), "UTF-8"); 
		} catch (Exception e) {
			System.err.println("Could not open file to write wrong trees: " + e.getMessage());
			e.printStackTrace();
		}
		
		this.initTrainVocabulary();
		Set<String> allUnknownWords = new TreeSet<String>();
		
		while (this.hasNextSentence()) {
			List<Token> parsedSentence = this.tokenizeNextSentence();
			if (parsedSentence.isEmpty()) {
				break;
			}
			++total;
			
			List<String> unknownWords = this.collectUnknownWords(parsedSentence);
			for (String word : unknownWords) {
				allUnknownWords.add(word);
			}
			boolean hasUnknownWords = !unknownWords.isEmpty();
			
			DependencyTree dTree = this.getSentenceDependencyTree(parsedSentence);
			dTree.sort();
			
			DependencyTree oracleTree = this.getOracleDependencyTree(parsedSentence, dTree);
			boolean projective = dTree.equals(oracleTree);
			if (!projective) {
				++nonprojective;
			}
			
			DependencyTree predictedTree = this.predict(parsedSentence);
			
			if (projective) {
				puas += this.getAS(predictedTree, dTree, false);
				plas += this.getAS(predictedTree, dTree, true);
			}
			
			uas += this.getAS(predictedTree, dTree, false);
			las += this.getAS(predictedTree, dTree, true);

			if (dTree.equals(predictedTree)) {
				++correct;
				if (hasUnknownWords) {
					++correctWithUnknownWords;
				}
			} else {
				if (hasUnknownWords) {
					++wrongWithUnknownWords;
				}
				if (projective) {
					writerProj.println("Sentence " + total + ":");
					writerProj.println(dTree);
					writerProj.println();
					writerProj.println(predictedTree);
					writerProj.println();
				} else {
					writerNonProj.println("Sentence " + total + ":");
					writerNonProj.println(dTree);
					writerNonProj.println();
					writerNonProj.println(predictedTree);
					writerNonProj.println();
				}
			}
		}
		double percentage = (double) correct * 100 / (double) total;
		System.out.println("Correct: " + correct + " out of: " + total + " " + percentage + "%");
		System.out.println("Nonprojective: " + nonprojective);
		System.out.println("UAS: " + (uas / total) + "%");
		System.out.println("LAS: " + (las / total) + "%");
		System.out.println("Projective UAS: " + (puas / (total - nonprojective)));
		System.out.println("Projective LAS: " + (plas / (total - nonprojective)));
		System.out.println("Correct with unknown words: " + correctWithUnknownWords);
		System.out.println("Wrong with unknown words: " + wrongWithUnknownWords);
		
		for (String word : allUnknownWords) {
			unknownWordsWriter.println(word);
		}
		
		writerProj.close();
		writerNonProj.close();
		unknownWordsWriter.close();
	}
	
	private List<String> collectUnknownWords(List<Token> sentence) {
		List<String> unknownWords = new LinkedList<String>();
		for (Token token : sentence) {
			if (!this.trainVocabulary.contains(token.getLemma())) {
				unknownWords.add(token.getLemma());
			}
		}
		return unknownWords;
	}
	
	private void dumpEmbeddingsToFile(Path modelFile, String choice) {
		Path vocab = FileSystems.getDefault().getPath("data", "vocabulary.mem");
		this.deserializeVocabulary(vocab);
		this.loadModel(modelFile);
		double[][] embeddings = this.network.getEmbeddings(choice);
		if (embeddings == null) {
			return;
		}
		Path outFilePath = FileSystems.getDefault().getPath("data", choice + "Embeddings.txt");
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(outFilePath.toFile(), "UTF-8");
		} catch (Exception e) {
			System.err.println("Could not open file to dump embeddings: " + e.getMessage());
			e.printStackTrace();
			writer = null;
		}
		if (writer == null) {
			return;
		}
		for (int row = 0; row < embeddings.length; ++row) {
			for (int col = 0; col < embeddings[0].length; ++col) {
				if (col > 0) {
					writer.print(" ");
				}
				writer.print(embeddings[row][col]);
			}
			writer.println();
		}
		writer.close();
		//write the corresponding words/tags/labels
		List<String> labels = this.getEmbeddingLabels(choice);
		outFilePath = FileSystems.getDefault().getPath("data", choice + "Labels.txt");
		try {
			writer = new PrintWriter(outFilePath.toFile(), "UTF-8");
		} catch (Exception e) {
			System.err.println("Could not open file to dump labels: " + e.getMessage());
			e.printStackTrace();
			writer = null;
		}
		if (writer == null) {
			return;
		}
		boolean first = true;
		for (String label : labels) {
			if (first) {
				first = false;
			} else {
				writer.print(" ");
			}
			writer.print(label);
		}
		writer.close();
	}
	
	private void initTrainVocabulary() {
		BufferedReader trainReader = null;
		Path trainFile = FileSystems.getDefault().getPath("data/UD_English", "en-ud-train.conllu");
		try {
			trainReader = Files.newBufferedReader(trainFile);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Could not open conllu file: " + e.getMessage());
			return;
		}
		
		String line = new String();
		while (line != null) {
			try {
				line = trainReader.readLine();
				if (line == null) {
					break;
				}
			} catch (IOException e) {
				System.err.println("Failed to read word " + e.getMessage());
				continue;
			}
			if (line.isEmpty() || line.charAt(0) == '#') {
				continue;
			}
			String[] parts = line.split("\t");
			Token token = new Token(parts, 0);
			this.trainVocabulary.add(token.getLemma());
		}
		
		try {
			trainReader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Failed to close file: " + e.getMessage());
		}
	}
	
	private List<String> getEmbeddingLabels(String choice) {
		Map<Integer, String> embeddingLabels = null;
		switch (choice) {
		case "words":
			embeddingLabels = this.vocabulary;
			break;
		case "tags":
			embeddingLabels = this.tags;
			break;
		case "labels":
			embeddingLabels = this.labels;
			break;
		}
		if (embeddingLabels == null) {
			return null;
		}
		List<String> embLabelsList = new LinkedList<String>();
		for (int i = 1; i <= embeddingLabels.size(); ++i) {
			embLabelsList.add(embeddingLabels.get(i));
		}
		return embLabelsList;
	}
	
	private void populateVocabulary(Set<String> words) {
		//add ROOT
		int wordId = this.vocabulary.size() + 1;
		if (this.vocabulary.isEmpty()) {
			this.reverseVocabulary.put("ROOT", wordId);
			this.vocabulary.put(wordId, "ROOT");
			++wordId;
		}
		//add other words
		Iterator<String> it = words.iterator();
		while (it.hasNext()) {
			String word = it.next();
			if (!this.reverseVocabulary.containsKey(word)) {
				this.reverseVocabulary.put(word, wordId);
				this.vocabulary.put(wordId, word);
				++wordId;
			}
		}
	}
	
	private void populatePOStags(Set<String> tags) {
		int tagId = this.tags.size() + 1;
		Iterator<String> it = tags.iterator();
		while (it.hasNext()) {
			String tag = it.next();
			if (!this.reverseTags.containsKey(tag)) {
				this.tags.put(tagId, tag);
				this.reverseTags.put(tag, tagId);
				++tagId;	
			}
		}
	}
	
	private void populateLabels(Set<String> labels) {
		int labelId = this.labels.size() + 1;
		Iterator<String> it = labels.iterator();
		while (it.hasNext()) {
			String label = it.next();
			if (!this.reverseLabels.containsKey(label)) {
				this.labels.put(labelId, label);
				this.reverseLabels.put(label, labelId);
				this.labelsList.add(label);
				++labelId;
			}
		}
	}
	
	private void initializeArcStandard() {
		this.arcStandard = new ArcStandard(this.labelsList);
	}
	
	private void initializeNeuralNetwork() {
		this.network = new NeuralNetwork(18, 18, 12,
				this.vocabulary.size(), this.tags.size(), this.labels.size(),
				200, 2*this.labels.size()+1, 50);
//		this.jnetwork = new NeuralNetworkJBLAS(18, 18, 12,
//				this.vocabulary.size(), this.tags.size(), this.labels.size(),
//				200, 2*this.labels.size()+1, 50);
	}
	
	private void train(List<TrainingExample> trainData, List<TrainingExample> validData, Path modelFile) {
		TrainingExample[] randomAccessData = trainData.toArray(new TrainingExample[0]);
		int[] indices = MatrixOperations.initializeIndices(trainData.size());
		int iterations = 1;
		double prevTrainError = Double.POSITIVE_INFINITY;
		double prevValidError = Double.POSITIVE_INFINITY;
		double trainError = Double.POSITIVE_INFINITY;
		double validError = Double.POSITIVE_INFINITY;
		do {
			prevTrainError = trainError;
			prevValidError = validError;
			long start = System.currentTimeMillis();
			System.out.println("Starting iteration " + iterations);
			System.out.flush();
			this.network.trainIteration(randomAccessData, indices);
			System.out.println("Iteration took: " + (System.currentTimeMillis() - start) + " milliseconds");
			System.out.flush();
			start = System.currentTimeMillis();
			trainError = this.network.computeError(trainData);
			validError = this.network.computeError(validData);
			System.out.println("Computing errors took: " + (System.currentTimeMillis() - start) + " milliseconds");
			System.out.println("Training Error: " + trainError);
			System.out.println("Validation Error: " + validError);
			System.out.println("Iteration: " + iterations);
			System.out.flush();
			this.backupModel(modelFile);
			this.serialize(modelFile);
			++iterations;
		} while (iterations < 8 || trainError > DependencyParser.convergenceThreshold || !this.hasConverged(prevTrainError, trainError, prevValidError, validError));
	}
	
	private boolean hasConverged(double prevTrainError, double trainError,
								 double prevValidError, double validError) {
		return (prevTrainError > trainError) && (prevValidError < validError);
	}
	
	private TrainingExample getTrainingExample(String line) {
		String[] rawExample = line.split(",");
		int[] wordInputs = new int[18];
		for (int i = 0; i < 18; ++i) {
			wordInputs[i] = Integer.parseInt(rawExample[i]);
		}
		int[] tagInputs = new int[18];
		int offset = 18;
		for (int i = 0; i < 18; ++i) {
			tagInputs[i] = Integer.parseInt(rawExample[offset + i]);
		}
		int[] labelInputs = new int[12];
		offset += 18;
		for (int i = 0; i < 12; ++i) {
			labelInputs[i] = Integer.parseInt(rawExample[offset + i]);
		}
		int output = Integer.parseInt(rawExample[offset + 12]);
		return new TrainingExample(wordInputs, tagInputs, labelInputs, output);
	}
	
	private void serialize(Path modelFile) {
		try {
			OutputStream fileOut = Files.newOutputStream(modelFile);
			ObjectOutputStream objOutStream = new ObjectOutputStream(fileOut);
			objOutStream.writeObject(this.network);
//			objOutStream.writeObject(this.jnetwork);
			objOutStream.close();
			fileOut.close();
			System.out.println("Serialized in " + modelFile.toString());
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Some step of serialization failed..." + " " + e.getMessage());
		}
	}
	
	private void backupModel(Path modelFile) {
		File mFile = new File(modelFile.toString());
		if (!mFile.exists()) {
			return;
		}
		Path modelFileBakPath = modelFile.resolveSibling(modelFile.getFileName() + ".bak");
		File modelFileBak = new File(modelFileBakPath.toString());
		if (modelFileBak.exists()) {
			modelFileBak.delete();
		}
		try {
			Files.move(modelFile, modelFileBakPath);
			System.out.println("Backed up in " + modelFileBakPath.toString());
		} catch (IOException e) {
			System.err.println("Faled to create backup model" + e.getMessage());
			System.out.println(modelFile.toString());
			System.out.println(modelFileBakPath.toString());
			e.printStackTrace();
		}
	}
	
	private double getAS(DependencyTree tree, DependencyTree goldTree, boolean labeled) {
		int correct = 0;
		for (Arc arc : tree) {
			if (goldTree.hasArc(arc, labeled)) {
				++correct;
			}
		}
		return (double) 100 * correct / (double) goldTree.size();
	}
	
	private void generateData(Path inputFile, Path outputFile, boolean training) {
		this.openFile(inputFile);
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(outputFile.toFile(), "UTF-8");
			writer.println(DependencyParser.header);
			while (this.hasNextSentence()) {
				List<Token> parsedSentence = this.tokenizeNextSentence();
				DependencyTree dTree = this.getSentenceDependencyTree(parsedSentence);
				dTree.sort();
				this.saveOracleDependencyTreeParse(parsedSentence, dTree, writer);
			}
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			System.err.println("Could not open file for writing data!" + " " + e.getMessage());
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
	
	private List<TrainingExample> getExamples(Path examplesFile) {
		List<TrainingExample> examples = new LinkedList<TrainingExample>();
		try {
			BufferedReader reader = Files.newBufferedReader(examplesFile);
			//skip header
			String line = reader.readLine();
			while ((line = reader.readLine()) != null) {
				TrainingExample example = this.getTrainingExample(line);
				examples.add(example);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Could not open conllu file: " + examplesFile + " " + e.getMessage());
		}
		return examples;
	}
	
	private int[] listToArray(List<Integer> list) {
		int[] array = new int[list.size()];
		int index = -1;
		for (int number : list) {
			array[++index] = number;
		}
		return array;
	}
	
	private List<Integer> getFeatures(Configuration c) {
		return this.getConfigurationState(c).getFeatures();
	}
	
	private ConfigurationState getConfigurationState(Configuration c) {
		ConfigurationState state = new ConfigurationState();
		int stackIndex = 0;
		while (stackIndex < 3) {
			int sentenceIndex = c.getStack(stackIndex);
			this.addWordToState(c, state, sentenceIndex, false);
			if (stackIndex < 2) {
				if (sentenceIndex < 0) {
					state.addWords(Collections.nCopies(6, 0));
					state.addPOStags(Collections.nCopies(6, 0));
					state.addLabels(Collections.nCopies(6, 0));
				} else {
					this.addChildren(c, sentenceIndex, true, state);
					this.addChildren(c, sentenceIndex, false, state);
				}
			}
			++stackIndex;
		}
		int bufferIndex = 0;
		while (bufferIndex < 3) {
			int sentenceIndex = c.getBuffer(bufferIndex);
			this.addWordToState(c, state, sentenceIndex, false);
			++bufferIndex;
		}
		return state;
	}
	
	private void addChildren(Configuration c, int word, boolean left, ConfigurationState state) {
		int childCount = 1;
		while (childCount < 3) {
			int child;
			if (left) {
				child = c.getLeftChild(word, childCount);
			} else {
				child = c.getRightChild(word, childCount);
			}
			this.addWordToState(c, state, child, true);
			if (childCount == 1) {
				if (child < 0) {
					this.addWordToState(c, state, -1, true);
				} else {
					int grandChild;
					if (left) {
						grandChild = c.getLeftChild(child, childCount);
					} else {
						grandChild = c.getRightChild(child, childCount);
					}
					this.addWordToState(c, state, grandChild, true);					
				}
			}
			++childCount;
		}
	}
	
	private void addWordToState(Configuration c, ConfigurationState state, int word, boolean addLabel) {
		if (word < 0) {
			state.addWord(0);
			state.addPOStag(0);
			if (addLabel) {
				state.addLabel(0);
			}
		} else {
			Integer wordId = this.reverseVocabulary.get(c.getWord(word));
			if (null != wordId) {
				state.addWord(wordId);
			} else {
				state.addWord(0);
			}
			Integer tagId = this.reverseTags.get(c.getPOS(word));
			if (null != tagId) {
				state.addPOStag(tagId);
			} else {
				state.addPOStag(0);
			}
			if (addLabel) {
				Integer labelId = this.reverseLabels.get(c.getLabel(word));
				if (null != labelId) {
					state.addLabel(labelId);
				} else {
					state.addLabel(0);
				}
			}
		}
	}
	
	private class ConfigurationState {
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
		
		public List<Integer> getFeatures() {
			List<Integer> features = new LinkedList<Integer>();
			features.addAll(this.words);
			features.addAll(this.postags);
			features.addAll(this.labels);
			return features;
		}
		
		public List<Integer> getWords() {
			return this.words;
		}
		
		public List<Integer> getTags() {
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
	
	public static void main(String[] args) {
		if (0 == args.length) {
			return;
		}
		
		DependencyParser parser = new DependencyParser();
		Path vocab = FileSystems.getDefault().getPath("data/", "vocabulary.mem");
		Path modelFile;
		
		switch (args[0]) {
		case "--buildVocab":
			if (args.length < 4) {
				System.out.println("Usage: java DependencyParser --buildVocab <training file> <validation file> <testing file>");
				return;
			}
			Path trainData = FileSystems.getDefault().getPath("data/UD_English", args[1]);
			Path validData = FileSystems.getDefault().getPath("data/UD_English", args[2]);
			Path testData = FileSystems.getDefault().getPath("data/UD_English", args[3]);
			parser.buildVocabulary(trainData, validData, testData);
			parser.serializeVocabulary(vocab);
			break;
		case "--train":
			if (args.length < 3) {
				System.out.println("Usage: java DependencyParser --train <training file> <validation file>");
				return;
			}
//			Path inputPath = FileSystems.getDefault().getPath("data/UD_English", args[1]);
//			Path validPath = FileSystems.getDefault().getPath("data/UD_English", args[2]);
//			modelFile = FileSystems.getDefault().getPath("data", "model.mem");
//			parser.train(inputPath, validPath, modelFile);
			break;
		case "--test":
			if (args.length < 3) {
				System.out.println("Usage: java DependencyParser --test <test file> <model file>");
				return;
			}
			Path testFile = FileSystems.getDefault().getPath("data/UD_English", args[1]);
			modelFile = FileSystems.getDefault().getPath("data", args[2]);
			parser.deserializeVocabulary(vocab);
			parser.loadModel(modelFile);
			parser.test(testFile);
			break;
		case "--emb":
			if (args.length < 3) {
				System.out.println("Usage: java DependencyParser --emb <modelFile> <words/tags/labels>");
				return;
			}
			if (!args[2].equals("words") && !args[2].equals("tags") && !args[2].equals("labels")) {
				System.out.println("Usage: java DependencyParser --emb <modelFile> <words/tags/labels>");
				return;
			}
			modelFile = FileSystems.getDefault().getPath("data/", args[1]);
			parser.dumpEmbeddingsToFile(modelFile, args[2]);
			break;
		}
	}
}
