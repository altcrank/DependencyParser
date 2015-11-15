import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.io.BufferedReader;
import java.io.IOException;

public class DependencyParser {
	
	private Path filePath;
	private BufferedReader reader;
	private Map<Integer, String> vocabulary;
	private Map<Integer, String> tags;
	private Map<Integer, String> labels;
	private List<String> labelsList;
	private Map<String, Integer> reverseVocabulary;
	private Map<String, Integer> reverseTags;
	private Map<String, Integer> reverseLabels;
	
	public void openFile(Path filePath) {
		this.filePath = filePath;
		try {
			reader = Files.newBufferedReader(filePath);
		} catch (IOException e) {
			System.err.println("Could not open conllu file: " + filePath);
			System.out.println("Could not open conllu file: " + filePath.getFileName());
		}
	}
	
	public void initializeData() {
		String line = new String();
		Set<String> words = new TreeSet<String>();
		Set<String> tags = new TreeSet<String>();
		Set<String> labels = new TreeSet<String>();
		while (line != null) {
			try {
				line = reader.readLine();
				if (line == null) {
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
		
		
		vocabulary = new HashMap<Integer, String>();
		reverseVocabulary = new HashMap<String, Integer>();
		int wordId = 1;
		Iterator<String> it = words.iterator();
		while (it.hasNext()) {
			String word = it.next();
			vocabulary.put(wordId, word);
			reverseVocabulary.put(word, wordId);
			++wordId;
		}
		//add ROOT
		this.vocabulary.put(0, "ROOT");
		this.reverseVocabulary.put("ROOT", 0);
		
		this.tags = new HashMap<Integer, String>();
		this.reverseTags = new HashMap<String, Integer>();
		int tagId = 1;
		it = tags.iterator();
		while (it.hasNext()) {
			String tag = it.next();
			this.tags.put(tagId, tag);
			this.reverseTags.put(tag, tagId);
			++tagId;
		}
		
		this.labels = new HashMap<Integer, String>();
		this.reverseLabels = new HashMap<String, Integer>();
		this.labelsList = new ArrayList<String>(labels.size());
		int labelId = 1;
		it = labels.iterator();
		while (it.hasNext()) {
			String label = it.next();
			if (label.equals("root")) {
				continue;
			}
			this.labels.put(labelId, label);
			this.reverseLabels.put(label, labelId);
			this.labelsList.add(label);
			++labelId;
		}
		
		try {
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		if (0 == id) {
			return "ROOT";
		}
		return this.vocabulary.get(id);
	}
	
	public List<Token> tokenizeSentence() {
		String line = new String();
		List<Token> words = new LinkedList<Token>();
		while (line != null) {
			try {
				line = reader.readLine();
				if (line == null) {
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
			int head = (0 == headIdx) ? headIdx : this.reverseVocabulary.get(sentence.get(headIdx-1).getLemma());
			int dependent = this.reverseVocabulary.get(token.getLemma());
			dTree.add(new Arc(head, dependent, token.getLabel()));	
		}
		return dTree;
	}
	
	public DependencyTree getOracleDependencyTree(List<Token> sentence) {
		ArcStandard standard = new ArcStandard(this.labelsList);
		DependencyTree realDTree = this.getSentenceDependencyTree(sentence);
		Configuration c = standard.initialConfiguration(sentence);
		while (!standard.isTerminal(c)) {
			String transition = standard.getOracle(c, realDTree);
			standard.apply(c, transition);
		}
		return c.getDependencyTree();
	}
	
	public static void main(String[] args) {
		Path path = FileSystems.getDefault().getPath("/home/gterziev/UvA/Year1/Semester1/Period2/Natural Language Processing/Project/UD_English-master", "en-ud-train.conllu");
		DependencyParser parser = new DependencyParser();
		parser.openFile(path);
		parser.initializeData();

		List<Token> parsedSentence = parser.tokenizeSentence();
		DependencyTree dTree = parser.getSentenceDependencyTree(parsedSentence);
		dTree.sort();
		for (Arc arc : dTree) {
			System.out.println(parser.getWord(arc.getHead()) + " " + arc.getLabel() + " " + parser.getWord(arc.getChild())
			+ " (" + (arc.getHead() < arc.getChild() ? "right" : "left") + ")");
		}
		
		System.out.println("Predicted Tree");
		DependencyTree predictedTree = parser.getOracleDependencyTree(parsedSentence);
		predictedTree.sort();
		for (Arc arc : predictedTree) {
			System.out.println(parser.getWord(arc.getHead()) + " " + arc.getLabel() + " " + parser.getWord(arc.getChild())
			+ " (" + (arc.getHead() < arc.getChild() ? "right" : "left") + ")");
		}
	}
}
