package gnat.server.dictionary;

import gnat.utils.FileHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import brics.automaton.ActionAutomaton;
import brics.automaton.Actor;
import brics.automaton.Automaton;
import brics.automaton.BasicOperations;
import brics.automaton.GeneTokenDeterminer;
import brics.automaton.RegExp;
import brics.automaton.State;


/**
 * Dictionaries are used for the recognition of entities.
 * <br><br>
 * How to get to a final dictionary?
 * <ul>
 * <li>extract names and synonyms from a database: <em>MakeLexiconList</em> or {@link gnat.server.ner.GeneInfo2Dictionary}
 * <li>check the lexicon, remove spurious entries: <em>LexiconChecker</em>
 * <li>convert lexicon format: from one gene per line to one ID / name pair per line: {@link gnat.server.ner.ConvertDictionaryToIDNamePairs}
 * <li>generate regular expressions for each name: {@link gnat.server.ner.GeneNamesToMWT}
 * <li>generate the final automaton: {@link gnat.server.dictionary.Dictionary}
 * <li>run dictionary as server: {@link gnat.server.dictionary.DictionaryServer}
 * </ul>
 * <em>Classes in italics</em> are not contained in this release.
 * 
 * <br><br>
 * Check dictionaries/readme.dictionaries.txt for additional information.
 * 
 * @author Conrad, Joerg
 * */
public class Dictionary {

	public static int MAX_REGEX_PER_AUTOMATON = 10000;	// the number of regex to fit into a single state machine.
	List<ActionAutomaton> actionAutomata;

	GeneTokenDeterminer geneTokenDeterminer = new GeneTokenDeterminer();

	/**
	 * Creates an empty dictionary.
	 * */
	public Dictionary() {
		actionAutomata = new LinkedList<ActionAutomaton>();
	}


	/**
	 * Loads a dictionary from a directory containing serializd automata generated by method loadAndStore.
	 *
	 * @throws ClassNotFoundException
	 * @throws ClassCastException */
	public Dictionary(String serializedAutomataDirectory) throws IOException, ClassCastException, ClassNotFoundException{
		this();

		long start = System.currentTimeMillis();
		if(!serializedAutomataDirectory.endsWith("/")){
			serializedAutomataDirectory = serializedAutomataDirectory+="/";
		}
		File dir = new File(serializedAutomataDirectory);
		String[] automataFiles = dir.list();
		for (String automataFile : automataFiles) {
			File file = new File(serializedAutomataDirectory+automataFile);
			if(!file.isDirectory()){
				FileInputStream fis = new FileInputStream(file);
		        ObjectInputStream ois = new ObjectInputStream(fis);
		        Automaton automaton = Automaton.load(ois);
		        actionAutomata.add(new ActionAutomaton(automaton, true));
			}
        }
		System.out.println("Dictionary startup in "+(System.currentTimeMillis()-start)+" ms");
	}


	/***/
	public Dictionary(List<String> ids, List<String> regularExpressions){
		this();

		long start = System.currentTimeMillis();
		List<Automaton> automata = new LinkedList<Automaton>();
		for (int i=0; i<regularExpressions.size(); i++) {
			String regex = regularExpressions.get(i);
			try{
		        RegExp regexp = new RegExp(regex);
		        Automaton entryAutomaton = regexp.toAutomaton();
		        Actor actor = new DictionaryActor(ids.get(i));
		        Set<State> acceptStates = entryAutomaton.getAcceptStates();
		        for (State state : acceptStates) {
		            state.setActor(actor);
	            }
		        automata.add(entryAutomaton);
	        }
	        catch(IllegalArgumentException iae){
	        	System.out.println("Dictionary: error creating regex for '"+regex+"'. message was: "+iae.getMessage());
	        }
        }
		Automaton automaton = Automaton.union(automata);
	    actionAutomata.add(new ActionAutomaton(automaton, true));
		System.out.println("Dictionary startup in "+(System.currentTimeMillis()-start)+" ms");
	}


	/**
	 * Loads automata from a dictionary file (*.mwt) and stores them in the storage directory.
	 * All automata get pruned by a set of names to exclude.
	 * */
	@SuppressWarnings("unchecked")
    public static void loadAndStore(File dictionary, Set<String> excludeNames, String storageDirectory) throws JDOMException, IOException{

		SAXBuilder saxBuilder = new SAXBuilder();
		Document document = saxBuilder.build(dictionary);
		Element dictionaryRootElement = document.getRootElement();
		List<Element> entries = dictionaryRootElement.getChildren();

		List<Automaton> automata = new LinkedList<Automaton>();

		long start = System.currentTimeMillis();
		int entryCount = 0;
		int automataCount = 0;
		for (Element entry : entries) {
	        String ids = entry.getAttributeValue("p1");
	        String regex = entry.getTextTrim();
	        try{
		        RegExp regexp = new RegExp(regex);
		        Automaton entryAutomaton = regexp.toAutomaton();
		        Set<String> excludeNamesMatchingRegex = getMatchingWords(entryAutomaton, excludeNames);
		        entryAutomaton = pruneAutomaton(entryAutomaton, excludeNamesMatchingRegex);
		        if(!BasicOperations.isEmpty(entryAutomaton) && !BasicOperations.isEmptyString(entryAutomaton)){
		        	Actor actor = new DictionaryActor(ids);
			        Set<State> acceptStates = entryAutomaton.getAcceptStates();
			        for (State state : acceptStates) {
			            state.setActor(actor);
		            }
			        automata.add(entryAutomaton);
			        entryCount++;
		        }else{
		        	System.out.println("Dictionary: regex '"+regex+"' was cleaned out.");
		        }
	        }
	        catch(IllegalArgumentException iae){
	        	System.out.println("Dictionary: error creating regex for '"+regex+"'. message was: "+iae.getMessage());
	        }

	        if(automata.size() % MAX_REGEX_PER_AUTOMATON == 0){
	        	Automaton automaton = Automaton.union(automata);
	        	automaton.determinize();
		        storeAutomaton("./"+storageDirectory+"/", "dictionary"+automataCount++, automaton);
	        	automata.clear();
	        }

	        if(entryCount % 10000 == 0){
	        	System.out.println("Dictionary: entries read: "+entryCount);
	        }
        }

		if(automata.size()>0){
			Automaton automaton = Automaton.union(automata);
			automaton.determinize();
	        storeAutomaton("./"+storageDirectory+"/", "dictionary"+automataCount++, automaton);
			automata.clear();
		}

		System.out.println("Finished loadAndStore in "+(System.currentTimeMillis()-start)+" ms");
	}


	/**
	 *	Removes a set of strings from the given automaton's language.
	 * */
	public static Automaton pruneAutomaton(Automaton entryAutomaton, Set<String> excludeStrings)
    {
		Automaton excludeAutomaton = new Automaton();
	    for (String word : excludeStrings) {
	    	RegExp regex = new RegExp(word);
	    	Automaton a = regex.toAutomaton();
	    	excludeAutomaton = BasicOperations.union(excludeAutomaton, a);
        }

	    entryAutomaton = BasicOperations.minus(entryAutomaton, excludeAutomaton);
	    entryAutomaton.minimize();
	    return entryAutomaton;
    }


	/**
	 * Returns a subset of words matching the given automaton.
	 * */
    public static Set<String> getMatchingWords(Automaton automaton, Set<String> words)
    {
		Set<String> matchingWords = new HashSet<String>();
		for (String string : words) {
	        if(BasicOperations.run(automaton, string)){
	        	matchingWords.add(string);
	        }
        }
		return matchingWords;
    }

	/**
	 * Stores a list of automata to a storage directory.
	 *
	 * @throws IOException
	 * */
	public void storeAutomata(LinkedList<Automaton> automata, String storeDir, String filenamePrefix) throws IOException{
		if(!storeDir.endsWith("/")){
			storeDir = storeDir+="/";
		}
		File dir = new File(storeDir);
		dir.mkdir();

		int i=0;
		for (Automaton automaton : automata) {
			storeAutomaton(storeDir, filenamePrefix+(i++), automaton);
		}
	}

	/**
	 * Stores an automaton as object file.
	 *
	 * @throws IOException
	 * */
	private static void storeAutomaton(String storeDir, String filename,  Automaton automaton) throws IOException{
		if(!storeDir.endsWith("/")){
			storeDir = storeDir+="/";
		}
		File dir = new File(storeDir);
		dir.mkdir();

		File file = new File(storeDir+filename);
		FileOutputStream fos = new FileOutputStream(file, false);
	       ObjectOutputStream oos = new ObjectOutputStream(fos);
		automaton.store(oos);
		oos.close();
		fos.close();
	}


	/**
	 * Returns a set of strings found in the given text.
	 * */
	public Set<String> getIdentifiedEntries(String text){
		Set<String> entrySet = new HashSet<String>();

		StringBuffer dictionaryOutput = new StringBuffer();

		for (ActionAutomaton actionAutomaton : actionAutomata) {
			actionAutomaton.run(text, dictionaryOutput, geneTokenDeterminer);
        }

		if(dictionaryOutput.length()>0){
			String[] actorAnnotations = dictionaryOutput.toString().split(DictionaryActor.SEPARATOR);
			for (String string : actorAnnotations) {
				entrySet.add(string);
	        }
		}
		return entrySet;
	}
	

	/**
	 * @throws IOException
	 * @throws JDOMException */
	public static void main(String[] args) throws JDOMException, IOException{
		if (args.length != 4) {
			System.out.println("Usage: java <classpath> Dictionary <dictionaryFileName>" + // <entity-type> <sub-type>" +
					           " <exclusionFileName> <automataStorageDirectory> <excludeNamesToLowerCase>");
			//System.out.println("Parameters:");
			//System.out.println("  entity-type   -  the class of entities that this dictionary annotates (gene, species, ..)");
			//System.out.println("  sub-type      -  the sub-type of entities, that is, a particular species (human), gene family, ..");

		} else {
			boolean toLowerCase = Boolean.parseBoolean(args[3]);
			System.out.println("to lower case = "+toLowerCase);
			Set<String> excludeNames = FileHelper.readFileIntoSet(args[1], toLowerCase, true);
			//Dictionary.loadAndStore(new File(args[0]), args[1], new HashSet<String>(), new HashSet<String>());
			Dictionary.loadAndStore(new File(args[0]), excludeNames, args[2]);
		}
	}
}
