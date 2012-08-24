package gnat;

import java.io.PrintStream;
import java.util.Set;
import java.util.TreeSet;

/**
 * 
 * Defines some constants that are needed throughout the project.
 * TODO: could go into a Properties file, together with dictionary ports, links to benchmark sets, etc.
 */

public class ConstantsNei {

	/**
	 * Controls different levels of output: no output, progress/current status,
	 * output for SVM meta-learning, output in clear text,
	 * full output for debugging.
	 */
	public static enum OUTPUT_LEVELS {NOOUTPUT, WARNINGS, STATUS, SVM, CLEARTEXT, DEBUG};

	/** 
	 * Sets the current output level. Default: status.
	 * @see enum OUTPUT_LEVELS
	 */
	public static OUTPUT_LEVELS OUTPUT_LEVEL = OUTPUT_LEVELS.STATUS;
	
	/**
	 * PrintStream that will be used to report errors, depending on OUTPUT_LEVEL.
	 */
	public static PrintStream ERR = System.err;
	
	/**
	 * PrintStream that will be used to report any (non-error) events, depending on OUTPUT_LEVEL.
	 */
	public static PrintStream OUT = System.out;

	/** 
	 * The dictionary can run as a remove host. Use RemoteEntityRecogntion
	 * in this case. This field holds the server's name.
	 */
	public static final String REMOTE_DICTIONARY_SERVER = ISGNProperties.get("dictionaryServer");

	/** 
	 * The dictionary can run as a remote host. Use RemoteEntityRecogntion
	 * in this case. This field holds the server's port. If you use multiple dictionaries 
	 * (inter-species case), use the file <tt>taxonToServerPort.txt</tt>.
	 */
	public static final int REMOTE_DICTIONARY_PORT = 56001;

	/** Number of scores/comparisons the GenePubMedScorer currently uses (chr. locations,
	 * GO codes, interaction partners, ...) Needed for SVM data when metalearning. */
	public static final int NUMBER_OF_SCORES = 14;

	/** Assigns these species to a text if no other mention was recognized. Currently: human and mouse. */
	public static Set<Integer> DEFAULT_SPECIES = new TreeSet<Integer>();
	static {
		String[] defSpec = ISGNProperties.get("defaultSpecies").split("[\\,\\;\\s]+");
		for (String def: defSpec)
			DEFAULT_SPECIES.add(Integer.parseInt(def));
	}
	
	/**
	 * 
	 * @param verbosity
	 */
	public static void setOutputLevel (int verbosity) {
		if (verbosity <= 0)
			OUTPUT_LEVEL = OUTPUT_LEVELS.NOOUTPUT;
		else if (verbosity == 1)
			OUTPUT_LEVEL = OUTPUT_LEVELS.WARNINGS;
		else if (verbosity == 2)
			OUTPUT_LEVEL = OUTPUT_LEVELS.STATUS;
		else if (verbosity == 3)
			OUTPUT_LEVEL = OUTPUT_LEVELS.SVM;
		else if (verbosity == 4)
			OUTPUT_LEVEL = OUTPUT_LEVELS.CLEARTEXT;
		else if (verbosity >= 5)
			OUTPUT_LEVEL = OUTPUT_LEVELS.DEBUG;
	}
	
	
	/**
	 * 
	 * @param level
	 * @return
	 */
	public static boolean verbosityAtLeast (OUTPUT_LEVELS level) {
		return (ConstantsNei.OUTPUT_LEVEL.compareTo(level) >= 0);
	}
	
	
	/**
	 * 
	 * @param level
	 * @return
	 */
	public static boolean verbosityIs (OUTPUT_LEVELS level) {
		return (ConstantsNei.OUTPUT_LEVEL.compareTo(level) == 0);
	}

}
