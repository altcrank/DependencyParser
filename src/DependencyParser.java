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
	private static final double convergenceThreshold = 95;
	
//	private NeuralNetworkJBLAS jnetwork;
	
	private Map<Integer, String> vocabulary;
	private Map<Integer, String> tags;
	private Map<Integer, String> labels;
	private List<String> labelsList;
	private Map<String, Integer> reverseVocabulary;
	private Map<String, Integer> reverseTags;
	private Map<String, Integer> reverseLabels;
	
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
		this.labelsList = null;
		this.reverseVocabulary = new HashMap<String, Integer>();
		this.reverseTags = new HashMap<String, Integer>();
		this.reverseLabels = new HashMap<String, Integer>();
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
	
	private void populateVocabulary(Set<String> words) {
		//add ROOT
		int wordId = 1;
		this.vocabulary.put(1, "ROOT");
		this.reverseVocabulary.put("ROOT", 1);
		++wordId;
		//add other words
		Iterator<String> it = words.iterator();
		while (it.hasNext()) {
			String word = it.next();
			this.vocabulary.put(wordId, word);
			this.reverseVocabulary.put(word, wordId);
			++wordId;
		}
	}
	
	private void populatePOStags(Set<String> tags) {
		this.tags = new HashMap<Integer, String>();
		this.reverseTags = new HashMap<String, Integer>();
		int tagId = 1;
		Iterator<String> it = tags.iterator();
		while (it.hasNext()) {
			String tag = it.next();
			this.tags.put(tagId, tag);
			this.reverseTags.put(tag, tagId);
			++tagId;
		}
	}
	
	private void populateLabels(Set<String> labels) {
		this.labels = new HashMap<Integer, String>();
		this.reverseLabels = new HashMap<String, Integer>();
		this.labelsList = new ArrayList<String>(labels.size());
		int labelId = 1;
		Iterator<String> it = labels.iterator();
		while (it.hasNext()) {
			String label = it.next();
			this.labels.put(labelId, label);
			this.reverseLabels.put(label, labelId);
			this.labelsList.add(label);
			++labelId;
		}
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
//			int head = (0 == headIdx) ? headIdx : sentence.get(headIdx-1).getId();
			int headSentenceIndex = (0 == headIdx) ? headIdx : sentence.get(headIdx - 1).getSentenceId();
//			int dependent = token.getId();
			int depSentenceIndex = token.getSentenceId();
//			dTree.add(new Arc(head, headSentenceIndex, dependent, depSentenceIndex, token.getLabel()));
			dTree.add(new Arc(headSentenceIndex, depSentenceIndex, token.getLabel()));
		}
		return dTree;
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
//		else {
//			System.out.println("false");
//		}
	}
	
	public void train(Path inputPath, Path modelFile) {
		File mFile = new File(modelFile.toString());
		if (mFile.exists()) {
			this.loadModel(modelFile);
		}
		Path trainFile = FileSystems.getDefault().getPath("data", "trainingdata.csv");
		this.generateTrainingData(inputPath, trainFile);
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
			this.train(examples, modelFile);
		}
		this.serialize(modelFile);
	}
	
	private void serialize(Path modelFile) {
		try {
			OutputStream fileOut = Files.newOutputStream(modelFile);
			ObjectOutputStream objOutStream = new ObjectOutputStream(fileOut);
			objOutStream.writeObject(this.reverseVocabulary);
			objOutStream.writeObject(this.reverseTags);
			objOutStream.writeObject(this.reverseLabels);
			objOutStream.writeObject(this.labelsList);
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
	
	private void train(List<TrainingExample> data, Path modelFile) {
		double correct = Double.NEGATIVE_INFINITY;
		TrainingExample[] randomAccessData = data.toArray(new TrainingExample[0]);
		int[] indices = MatrixOperations.initializeIndices(data.size());
		int iterations = 1;
		while (correct < DependencyParser.convergenceThreshold) {
			long start = System.currentTimeMillis();
			System.out.println("Starting iteration " + iterations);
			System.out.flush();
			this.network.trainIteration(randomAccessData, indices);
			System.out.println("Iteration took: " + (System.currentTimeMillis() - start) + " milliseconds");
			System.out.flush();
			start = System.currentTimeMillis();
			correct = this.network.countCorrect(data);
			System.out.println("Counting correct took: " + (System.currentTimeMillis() - start) + " milliseconds");
			System.out.println("Correct: " + correct);
			System.out.println("Iteration: " + iterations);
			System.out.flush();
			this.backupModel(modelFile);
			this.serialize(modelFile);
			++iterations;
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
	
	@SuppressWarnings("unchecked")
	public void loadModel(Path modelFile) {
		try {
			InputStream fileIn = Files.newInputStream(modelFile);
			ObjectInputStream objInStream = new ObjectInputStream(fileIn);
			try {
				this.reverseVocabulary = (HashMap<String, Integer>) objInStream.readObject();
				this.reverseTags = (HashMap<String, Integer>) objInStream.readObject();
				this.reverseLabels = (HashMap<String, Integer>) objInStream.readObject();
				this.labelsList = (List<String>) objInStream.readObject();
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
	
	public DependencyTree predict(List<Token> sentence) {
		Configuration c = this.arcStandard.initialConfiguration(sentence);
		while (!this.arcStandard.isTerminal(c)) {
			ConfigurationState state = this.getConfigurationState(c);
			int[] wordInputs = this.listToArray(state.getWords());
			int[] tagInputs = this.listToArray(state.getTags());
			int[] labelInputs = this.listToArray(state.getLabels());
			int transitionId = this.network.chooseTransition(wordInputs, tagInputs, labelInputs);
			String transition = this.arcStandard.getTransition(transitionId);
			if (!this.arcStandard.canApply(c, transition)) {
				transitionId = this.network.chooseSecondBestTransition(wordInputs, tagInputs, labelInputs);
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
		while (this.hasNextSentence()) {
			List<Token> parsedSentence = this.tokenizeNextSentence();
			if (parsedSentence.isEmpty()) {
				break;
			}
			++total;
			
			DependencyTree dTree = this.getSentenceDependencyTree(parsedSentence);
			dTree.sort();
			DependencyTree predictedTree = this.predict(parsedSentence);
			uas += this.getAS(predictedTree, dTree, false);
			las += this.getAS(predictedTree, dTree, true);
			if (dTree.equals(predictedTree)) {
				++correct;
			} else {
				System.out.println(dTree);
				System.out.println();
				System.out.println(predictedTree);
				System.out.println();
			}
		}
		double percentage = (double) correct * 100 / (double) total;
		System.out.println("Correct: " + correct + " out of: " + total + " " + percentage + "%");
		System.out.println("UAS: " + (uas / total) + "%");
		System.out.println("LAS: " + (las / total) + "%");
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
	
	private void generateTrainingData(Path inputFile, Path outputFile) {
		this.openFile(inputFile);
		this.initializeData();
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
			System.err.println("Could not open file for writing training data!" + " " + e.getMessage());
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
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
		Path modelFile;
		
		switch (args[0]) {
		case "--train":
			Path inputPath = FileSystems.getDefault().getPath("data/UD_English", args[1]);
			modelFile = FileSystems.getDefault().getPath("data", "model.mem");
			parser.train(inputPath, modelFile);
			break;
		case "--test":
			Path testFile = FileSystems.getDefault().getPath("data/UD_English", args[1]);
			modelFile = FileSystems.getDefault().getPath("data", "model.mem");
			parser.loadModel(modelFile);
			parser.test(testFile);
			break;
		}
	}
}
