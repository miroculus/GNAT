package gnat.utils;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class offers some methods useful when working with (arrays of) strings.
 *
 * <h4>String comparison</h4>
 * <ul>
 *   <li>stringLengthComparatorAscending - for use in Collections.sort(Comparator)
 *   <li>stringLengthComparatorDescending - for use in Collections.sort(Comparator)
 *   <li>equalsIgnorePlural(String, String)
 * </ul>
 * 
 * <h4>Morphology</h4>
 * <ul>
 *   <li>toSingularForm(String) - returns the singular form of a word, if applicable
 *   <li>toSingularForms(String[]) - returns the singular forms for an array of word, if applicable
 *   <li>equalsIgnorePlural(String, String)
 * </ul>
 *
 * <h4>Orthography</h4>
 * <ul>
 * </ul>
 *
 * <h4>To sort into categories:</h4>
 * <ul>
 *   <li>espaceString(String) - espace regular expression meta-characters (precede by \\)
 *   <li>reduceToFirstCharacters(String[], int) - reduces each element in an array to its suffix of specified length
 *   <li>concatenate(String[], String[]) = concatenates two string arrays
 *   <li>splitAndSort - tokenize, sort alphabetically, then de-tokenize again
 *   <li>removeStrings
 *   <li>removeStringsIgnoreCase
 *   <li>joinStringArray
 *   <li>joinStringList
 *   <li>joinStringSet
 *   <li>joinIntegerSet
 *   <li>tokenizeToLowerCase
 * </ul>
 *
 * @author Joerg Hakenberg, Conrad Plake
 *
 */
public class StringHelper {
	
	/** A comparator that sorts strings by length, in ascending order (shorest first). */
	public static Comparator<String> stringLengthComparatorAscending = new StringLengthComparatorAscending();
	/** A comparator that sorts strings by length, in descending order (longest first). */
	public static Comparator<String> stringLengthComparatorDescending = new StringLengthComparatorDescending();


	/** Suppresses subclassing. */
	private StringHelper () { throw new AssertionError(); }


	/**
	 * Escapes certain characters in a string so that it can be used inside a regular expression.
	 * Inserts <tt>\\</tt> before any brackets, '*', '+', and '-'.
	 * @param string
	 * @return
	 */
	public static String espaceString (String string) {
		return string.replaceAll("([\\+\\-\\*\\(\\)\\[\\]\\{\\}])", "\\\\$1");
	}


	/**
	 * Goes through an array of strings and reduces every element to its first
	 * <tt>suffixLength</tt> characters. Strings smaller than <tt>first</tt> remain
	 * intact.<br><br>
	 * <tt>reduceToFirstCharacters({"foo","bar","n"}, 2)</tt> would result in <tt>{"fo","ba","n"}</tt>.
	 * 
	 * @param array
	 * @param suffixLength
	 * @return
	 */
	public static String[] reduceToFirstCharacters (String[] array, int suffixLength) {
		for (int a = 0; a < array.length; a++) {
			if (array[a].length() > suffixLength)
				array[a] = array[a].substring(0, suffixLength);
		}
		return array;
	}


	/**
	 * Concatenates to string arrays into one.<br>
	 * <br>
	 * The new array will have length <tt>array1.length + array2.length</tt> and contain, from elements
	 * 0 to array1.length-1, the elements of the original <tt>array1</tt> first, then, starting from array1.length,
	 * the elements of original <tt>array2</tt>.
	 * 
	 * @param array1
	 * @param array2
	 * @return
	 */
	public static String[] concatenate(String[] array1, String[] array2){
		String[] array = new String[array1.length + array2.length];
		int i = 0;
		for(String s : array1){
			array[i++] = s;
        }
		for(String s : array2){
			array[i++] = s;
        }
		return array;
	}


	/**
	 * Checks a word, <tt>assertedNoun</tt> for a plural form. Replaces it with its singular form if applicable.<br>
	 * <b>No word category check</b>---will change all words, not only nouns, thus affects verbs as well (encodes=&gt;encode)!
	 * @param noun
	 * @return
	 */
	public static String toSingularForm (String assertedNoun) {
		// some special cases
		if (assertedNoun.endsWith("etes"))       return assertedNoun;  // diabetes
		if (assertedNoun.endsWith("langerhans")) return assertedNoun;  // langerhans
		if (assertedNoun.endsWith("itis"))       return assertedNoun;  // arthritis
		if (assertedNoun.endsWith("ss"))         return assertedNoun;  // class, mass, mess

		if (assertedNoun.endsWith("omas"))
			//if (assertedNoun.length() > 4)
				return assertedNoun.substring(0, assertedNoun.lastIndexOf("omas")) + "oma";

		// cities
		if (assertedNoun.endsWith("ies"))
			if (assertedNoun.length() > 4)
				return assertedNoun.substring(0, assertedNoun.lastIndexOf("ies")) + "y";
			else
				return assertedNoun.substring(0, assertedNoun.lastIndexOf("ies")) + "ie";

		// 
		if (assertedNoun.endsWith("ienes"))
			return assertedNoun.substring(0, assertedNoun.lastIndexOf("ienes")) + "ien";

		// classes
		if (assertedNoun.endsWith("sses"))
			return assertedNoun.substring(0, assertedNoun.lastIndexOf("sses")) + "ss";

		// any other word ending with -es
		if (assertedNoun.endsWith("es"))
			if (!assertedNoun.matches("^.*[aeiou]es$"))
				return assertedNoun.substring(0, assertedNoun.lastIndexOf("es")) + "e";

		// any word ending with -s
		if (assertedNoun.endsWith("s"))
			if (!assertedNoun.matches("^.*[aeiou]s$"))
				return assertedNoun.substring(0, assertedNoun.length() - 1);
		
		// Latin plural for '-a'
		if (assertedNoun.endsWith("ae"))
			return assertedNoun.substring(0, assertedNoun.length() - 1);
		
		return assertedNoun;
	}

	
	/**
	 * Checks an array of words for plural forms. Replaces every plural form with its singular form.<br>
	 * <b>No word category check</b>---will change all words, not only nouns, thus affects verbs as well (encodes=&gt;encode)!
	 * @param array
	 * @return
	 */
	public static String[] toSingularForms (String[] array) {
		for (int a = 0; a < array.length; a++) {
			array[a] = toSingularForm(array[a]);
		}
		return array;
	}


	/**
	 * Simple method for testing if two string are equal even if they vary in an ending 's'.
	 * */
	public static boolean equalsIgnorePlural (String one, String two) {
		if (one.equals(two)) return true;
		if (one.equals(two + "s")) return true;
		if (two.equals(one + "s")) return true;
		return false;
	}


	/**
	 * Splits a string into tokens at whitespaces and sorts them alphabetically.
	 * All tokens are then again glued together to a single string separated by whitespaces.
	 * 
	 * @param string
	 * @return
	 */
	public static String splitAndSort(String string)
	{
		String[] tokens = string.split(" ");
		TreeSet<String> sorter = new TreeSet<String>();
		for (int i = 0; i < tokens.length; i++) {
			sorter.add(tokens[i]);
		}
		StringBuilder sBuilder = new StringBuilder();
		Iterator<String> tokIt = sorter.iterator();
		while (tokIt.hasNext()) {
			sBuilder.append(tokIt.next() + " ");
		}
		return sBuilder.toString().trim();
	}


	/**
	 * Removes all tokens in strings that are also in unwantedStrings. Could be used as a stop word filter.
	 * @param strings
	 * @param unwantedStrings
	 * @return String[]
	 */
	public static String[] removeStrings (String[] strings, String[] unwantedStrings) {
		boolean[] keepit = new boolean[strings.length];
		int newsize = strings.length;
		for (int i = 0; i < strings.length; i++) {
			boolean keep = true;
			for (int j = 0; j < unwantedStrings.length; j++) {
				if (strings[i].equals(unwantedStrings[j])) {
					keep = false;
					newsize--;
					break;
				}
			}
			keepit[i] = keep;
		}
		String[] remainingWords = new String[newsize];
		int j = 0;
		for (int i = 0; i < strings.length; i++) {
			if (keepit[i]) {
				remainingWords[j++] = strings[i];
			}
		}
		return remainingWords;
	}


	/**
	 * Removes all tokens in strings that are also in unwantedStrings. Could be used as a stop word filter.
	 * @param strings
	 * @param unwantedStrings
	 * @return String[]
	 */
	public static String[] removeStringsIgnoreCase (String[] strings, String[] unwantedStrings) {
		boolean[] keepit = new boolean[strings.length];
		int newsize = strings.length;
		for (int i = 0; i < strings.length; i++) {
			boolean keep = true;
			for (int j = 0; j < unwantedStrings.length; j++) {
				if (strings[i].equalsIgnoreCase(unwantedStrings[j])) {
					keep = false;
					newsize--;
					break;
				}
			}
			keepit[i] = keep;
		}
		String[] remainingWords = new String[newsize];
		int j = 0;
		for (int i = 0; i < strings.length; i++) {
			if (keepit[i]) {
				remainingWords[j++] = strings[i];
			}
		}
		return remainingWords;
	}


	/**
	 * Joins the entries in a String array into a single String. All entries get
	 * separated by the specified delimiter.
	 * @param array
	 * @param delimiter
	 * @return joined array
	 */
	public static String joinStringArray (String[] array, String delimiter) {
		String result = "";
		if (array.length >= 1)
			result = array[0];
		for (int i = 1; i < array.length; i++)
			result += delimiter + array[i];
		return result;
	}


	/**
	 * Joins the entries in a String list into a single String. All entries get
	 * separated by the specified delimiter.
	 * @param list
	 * @param delimiter
	 * @return joined array
	 */
	public static String joinStringList (List<String> list, String delimiter) {
		StringBuffer result = new StringBuffer();
		for (String string : list) {
			if(result.length()>0){
				result.append(delimiter);
			}
			result.append(string);
        }
		return result.toString();
	}

	/**
	 * Puts all elements of a set into a single delimited string.
	 * Note that elements in the set are unordered!
	 * */
	public static String joinStringSet(Set<String> set, String delimiter)
    {
		StringBuffer result = new StringBuffer();
		for (String string : set) {
			if(result.length()>0){
				result.append(delimiter);
			}
			result.append(string);
        }
		return result.toString();
    }

	/**
	 * Puts all elements of a set into a single delimited string.
	 * Note that elements in the set are unordered!
	 * */
	public static String joinIntegerSet(Set<Integer> set, String delimiter)
    {
		StringBuffer result = new StringBuffer();
		for (Integer integer : set) {
			if(result.length()>0){
				result.append(delimiter);
			}
			result.append(integer);
        }
		return result.toString();
    }


	/**
	 * Splits a given text into tokens, all transformed into lower case.
	 * @param text
	 * @return
	 */
	public static String[] tokenizeToLowerCase (String text) {
		return text.toLowerCase().split("([\\,\\.\\)\\]\\;]?\\s[\\(\\[]?|\\.$)");
	}


	/** stop words */
	public static String[] stopWords = {
		// new, test:

		"protein",
		"proteins",
		"gene",
		"genes",
		"cell",
		"specific",
		"function",
		"sequence",
		"isoform",
		"isoforms",
		/*-----*/

		"characterized",
		"due",
		"associated",
		"role",
		"activation",
		"study",
		"provide",
		"interact",
		"structure",
		"important",
		"potential",
		"play",
		"association",
		"interaction",
		"transcription",
		"level",
		"similarity",
		"interaction",
		"identification",
		"manner",
		"show",
		
		//"characterizes",
		//"characterize",
		//"characterizing",
		//"characterization",
		//"characterizations",
		//"association",
		//"associations",
		//"associates",
		//"associate",
		//"associating",
		//"activates",
		//"activated",
		//"activating",
		//"activate",
		//"studies",
		//"studied",
		//"studying",
		//"interactions",
		//"interact",
		//"interacted",
		//"provided",
		//"provides",
		//"interacting",
		
		/*-------*/
		
		/*
		 "critical",
		"require",
		"act",
		"initially",
		"occurrence",
		"following",
		"inhibiting",
		"able",

		"forms",
		"levels",
		"binds",
		"biological",
		"contribute",
		"contributed",
		"contributes",
		"tissue",
		"contain",
		"contained",
		"containing",
		"contains",
		"two",
		"interacted",
		"interacting",
		"interacts",
		"lines",
		"overexpressed",
		"overexpresses",
		"overexpression",
		"number",
		"numbers",
		"characteristic",
		"characterize",
		"functional",
		"functions",
		"encoding",
		"encode",
		"coding",
		"different",
		"one",
		"reduced",
		"reduces",
		"reducing",
		"reduction",
		"sequences",
		"little",
		*/

		// tested, to sort:


		///// established:
		// domain-specific:
		/*"activate",
		"activated",
		"activator",
		"activity",
		"binding",
		"bound",
		"cells",
		"cellular",
		"characteristic",*/
		"cloning",
		/*"concentrated",
		"conserved",
		"downregulated",
		"down-regulated",
		"encoded",
		"encodes",
		"enhanced",*/
		"expressed",
		"expression",
		/*"factor",
		"family",
		"function",
		"human",
		"induced",
		"induction",
		"individual",
		"inhibit",
		"inhibition",
		"inhibitor",
		"interaction",
		"kda",
		"level",*/
		"localization",
		/*"member",
		"pathway",
		"peptide",
		"promoter",
		"reaction",
		"reactive",
		"receptor",
		"recruitment",
		"regulate",
		"regulation",
		"regulating",
		"response",
		"sequence",
		"site",
		"upregulated",
		"up-regulated",

		// longer words, sometimes domain-specific:
		"ability",
		"act",
		"action",
		"addition",
		"analysis",
		"direct",
		"effect",
		"increased",
		"influence",
		"involved",
		"leading",
		"light",
		"line",
		"mapped",
		"mechanism",
		"preventing",
		"process",
		"produced",
		"prominent",
		"providing",
		"require",
		"required",
		"requirement",
		"similar",
		"synergize",
		"system",
		"undergo",
		"undertake",
		"uptake",*/

		// scientific publication stop words
		"appear",
		"appeared",
		"confirm",
		"confirmed",
		"confirming",
		"data",
		"demonstrate",
		"demonstrated",
		"demonstrating",
		"detect",
		"detected",
		"detecting",
		"document",
		"documented",
		"documenting",
		"documents",
		"evidence",
		"experiment",
		"find",
		"finding",
		"found",
		"help",
		"helps",
		"helped",
		"identified",
		"investigated",
		"involved",
		"known",
		"observe",
		"observed",
		"occur",
		"occurred",
		"present",
		"presented",
		"propose",
		"proposed",
		"recent",
		"recently",
		"report",
		"reported",
		"result",
		"resulted",
		"results",
		"reveal",
		"revealed",
		"show",
		"showed",
		"shown",
		"studied",
		"study",
		"suggest",
		"suggested",
		"suggests",
		"suggesting",
		"support",
		"test",
		"tested",
		"thought",
		"use",
		"used",
		"using",
		"utilized",


		// common adjectives/adverbs
		/*"directly",
		"essential",
		"first",
		"high",
		"highly",
		"important",
		"low",
		"likely",
		"little",
		"major",
		"minor",
		"multiple",*/
		"new",
		"novel",
		"possible",
		/*"overall",
		"potential",
		"potentially",
		"remarkable",
		"remarkably",
		"several",
		"significant",
		"significantly",
		"single",*/
		"small",
		/*"special",
		"specific",
		"strong",
		"strongly",
		"unlikely",
		"unique",*/
		"widely",
		//"well",


		// "real" stop words
		"a",
		"after",
		"against",
		"all",
		"also",
		"although",
		"among",
		"an",
		"and",
		"and/or",
		"are",
		"as",
		"at",
		"be",
		"because",
		"been",
		"belong",
		"between",
		"both",
		"but",
		"by",
		"can",
		"could",
		"did",
		"do",
		"does",
		"during",
		"each",
		"either",
		"for",
		"from",
		"furthermore",
		"had",
		"has",
		"have",
		"however",
		"in",
		//"into",
		"is",
		"it",
		"its",
		"itself",
		"many",
		"may",
		"might",
		"more",
		"most",
		"neither",
		"no",
		"nor",
		"not",
		"of",
		"on",
		"only",
		"or",
		"other",
		"our",
		"same",
		"such",
		"than",
		"that",
		"the",
		"their",
		"there",
		"thereby",
		"therefore",
		"these",
		"they",
		"this",
		"those",
		"through",
		"thus",
		"to",
		"toward",
		"towards",
		"under",
		"upon",
		"via",
		"was",
		"we",
		"were",
		"when",
		"where",
		"whereas",
		"which",
		"while",
		"whose",
		"within",
		"with",
		"would"
	};
}



/** Class implenting a StringLengthComparator provided by StringHeler. */
class StringLengthComparatorDescending implements Comparator<String> {

	/**
	 * Compares two strings and returns either a negative value, zero, or a positive value.
	 * Negative return values indicate that the first string is longer than the second;
	 * zero indicates that both have the same length, and positive values indicate
	 * that the first string is shorter than the second.<br>
	 * <br>
	 * In particular, it returns o1.length() - o2.length();
	 * 
	 * @param o1 - first String
	 * @param o2 - second String
	 * @return negative or positive value or zero 
	 */
	public int compare (String o1, String o2) {
		String o1f = ((String)o1).toString();
		String o2f = ((String)o2).toString();
		int result = o2f.length() - o1f.length();
		return result; 
	}

}


/** Class implenting a StringLengthComparator provided by StringHeler. */
class StringLengthComparatorAscending implements Comparator<String> {

	/**
	 * Compares two strings and returns either a negative value, zero, or a positive value.
	 * Negative return values indicate that the first string is shorter than the second;
	 * zero indicates that both have the same length, and positive values indicate
	 * that the first string is longer than the second.<br>
	 * <br>
	 * In particular, it returns o2.length() - o1.length();
	 * 
	 * @param o1 - first String
	 * @param o2 - second String
	 * @return negative or positive value or zero 
	 */
	public int compare (String o1, String o2) {
		String o1f = ((String)o1).toString();
		String o2f = ((String)o2).toString();
		int result = o1f.length() - o2f.length();
		return result; 
	}

}
