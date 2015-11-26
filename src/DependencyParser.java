import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
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
			System.err.println("Could not open conllu file: " + filePath);
			System.out.println("Could not open conllu file: " + filePath.getFileName());
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
				System.err.println("Failed to read word");
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
		try {
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
				line = reader.readLine();
				if (line == null) {
					this.EOFreached = true;
					break;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}
			if (line.isEmpty() || line.charAt(0) == '#') {
				break;
			}
			String[] parts = line.split("\t");
			words.add(new Token(parts, this.reverseVocabulary.get(parts[2])));
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
		} else {
			System.out.println("false");
		}
	}
	
	private List<Integer> getFeatures(Configuration c) {
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
		return state.getFeatures();
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
		int sentence = 0;
		if (args.length > 0) {
			sentence = Integer.parseInt(args[0]);
		}
		Path path = FileSystems.getDefault().getPath("data/UD_English", "en-ud-train.conllu");
		DependencyParser parser = new DependencyParser();
		parser.openFile(path);
		parser.initializeData();

		int count = 0;
		PrintWriter writer = null;
		try {
			writer = new PrintWriter("data/trainingdata.csv", "UTF-8");
			writer.println(DependencyParser.header);
			while (parser.hasNextSentence()) {
				List<Token> parsedSentence = parser.tokenizeNextSentence();
				++count;
				if (sentence != 0) {
					if (sentence < count) {
						break;
					}
					if (sentence > count) {
						continue;
					}
				}
				DependencyTree dTree = parser.getSentenceDependencyTree(parsedSentence);
				dTree.sort();
				
				parser.saveOracleDependencyTreeParse(parsedSentence, dTree, writer);
			}
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			System.err.println("Could not open file for writing training data!");
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
}
