package gnat.server.ner;

import gnat.utils.ArrayHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;


public class Term2Regex{

	public static final String tokenDelimiterWithWhitespace = "[ \\-]?";
	public static final String tokenDelimiterNoWhitespace = "\\-?";
	public static final String tokenDelimiterWithWhitespaceR = "[\\ \\\\-]?";
	public static final String tokenDelimiterWithWhitespaceUnderscoreR = "[\\ \\\\-\\\\_]?";
	public static final String tokenDelimiterNoWhitespaceR = "\\\\-?";
	public static final String trailContextRe = "[^A-Za-z0-9]";

	public static final String PREPOSITIONS_REGEX = "(?:or|at|as|in|of|for|by|on|with|near)";	// some prepositions

	public static final String GREEK_WORDLETTER_REGEX = "(?:[Aa]lpha|[Bb]eta|[Gg]amma|[Dd]elta|[Ee]psilon|[Ee]ta|[Kk]appa|[Ll]ambda" +
			"|[Zzeta]|[Oo]mega|[Tt]heta|[Ss]igma|[Rr]ho)";

	public static int verbosity = 0;


	/**
	 * Generates variations of a whole protein name (tumor necrosis factor alpha, TNF alpha, CD95 ligand)
	 *
	 * @param term
	 * @return
	 */
	public static String getVariationsForProteinnames (String term) {
		//String regex = term;

		//System.out.println("# Term:" + term);

		String[] tokens = term.split("[\\s\\-]");
		for (int t = 0; t < tokens.length; t++) {
			tokens[t] = getTokenVariations(tokens[t], t);
		}

		//System.out.println("# Tokens:" + tokens.length);


		String combined = tokens[0];
		for (int t = 1; t < tokens.length; t++) {
			combined += tokenDelimiterWithWhitespace + tokens[t];
		}

		return combined;
	}


	/**
	 * Checks if the given name part is a Arabic or Roman numeral, also considers single Greek letters/letter words
	 * as numerals.
	 * @param part
	 * @return
	 */
	public static boolean isNumeral (String part) {
		if (part.matches("\\d+")) return true;
		if (part.matches("([XVI]+|[xvi]+|[XVI][xvi]+)")) return true;
		if (part.matches("([Aa](lpha)?|[Bb](eta)?|[Gg](amma)?|[Cc]|[Dd](elta)?|[Ee](psilon)?|[Ee]ta)|[Hh]|[Ii]ota" +
			"|[Kk](appa)?|[Ll](ambda)?|[Tt](heta)?|[Qq]|[Zz](eta)?")) return true;
		return false;
	}


	/**
	 * Returns the potential reg.ex. markup for a part if it is a Roman numeral
	 * or Greek letter. For instance, returns "[Aa](lpha)?" if the part matches
	 * "A", "a", "Alpha", or "alpha".
	 * @param part
	 * @return
	 */
	public static String getMarkupForNumerals (String part) {
		if (part.matches("[Aa](lpha)?")) return "[Aa](lpha)?";
		if (part.matches("[Bb](eta)?")) return "[Bb](eta)?";
		if (part.matches("[Gg](amma)?")) return "([Gg](amma)?|[Cc])";
		if (part.matches("[Dd](elta)?")) return "[Dd](elta)?";
		if (part.matches("[Ee](psilon)?")) return "[Ee](psilon)?";
		if (part.matches("[Ee]ta")) return "([Ee]ta|[Hh])";
		if (part.matches("[Ii]ota")) return "[Ii]ota";
		if (part.matches("[Kk](appa)?")) return "[Kk](appa)?";
		if (part.matches("[Ll]ambda")) return "[Ll](ambda)?";
		if (part.matches("[Tt](heta)?")) return "([Tt](heta)?|[Qq])";
		if (part.matches("[Zz](eta)?")) return "[Zz](eta)?";

		if (part.matches("[Ii1]")) return "[Ii1]";
		if (part.matches("(II|ii|2)")) return "(II|ii|2)";
		if (part.matches("(III|iii|3)")) return "(III|iii|3)";
		if (part.matches("(IV|iv|4)")) return "(IV|iv|4)";
		if (part.matches("[Vv5]")) return "[Vv5]";
		if (part.matches("(VI|vi|6)")) return "(VI|vi|6)";
		if (part.matches("(VII|vii|7)")) return "(VII|vii|7)";
		if (part.matches("(VIII|viii|8)")) return "(VIII|viii|8)";
		if (part.matches("(IX|ix|9)")) return "(IX|ix|9)";
		if (part.matches("(X|x|10)")) return "(X|x|10)";
		if (part.matches("(XI|xi|11)")) return "(XI|xi|11)";
		if (part.matches("(XII|xii|12)")) return "(XII|xii|12)";
		if (part.matches("(XIII|xiii|13)")) return "(XIII|xiii|13)";
		if (part.matches("(XIV|xiv|14)")) return "(XIV|xiv|14)";
		if (part.matches("(XV|xv|15)")) return "(XV|xv|15)";
		if (part.matches("(XVI|xvi|16)")) return "(XVI|xvi|16)";
		if (part.matches("(XVII|xvii|17)")) return "(XVII|xvii|17)";
		if (part.matches("(XVIII|xviii|18)")) return "(XVIII|xviii|18)";
		if (part.matches("(XIX|xix|19)")) return "(XIX|xix|19)";
		if (part.matches("(XX|xx|20)")) return "(XX|xx|20)";
		if (part.matches("(XXI|xxi|21)")) return "(XXI|xxi|21)";
		// [..] not in UniProt/SwissProt 10.2?
		if (part.matches("(XXV|xxv|25)")) return "(XXV|xxv|25)";
		if (part.matches("(XXVI|xxvi|26)")) return "(XXVI|xxvi|26)";
		if (part.matches("(XXVII|xxvii|27)")) return "(XXVII|xxvii|27)";
		// [..] not in UniProt/SwissProt 10.2?
		if (part.matches("(XXX|xxx|30)")) return "(XXX|xxx|30)";
		if (part.matches("(XXXI|xxxi|31)")) return "(XXXI|xxxi|31)";
		// [..] not in UniProt/SwissProt 10.2?

		return "";
	}


	/**
	 *
	 * @param id
	 * @return
	 */
	public static String getRegexForProteinID (String id) {
		if (id.startsWith("EC")) {
			id = id.replaceAll("\\.-", "\\\\.\\\\-");
			id = id.replaceAll("\\.(\\d)", "\\\\.$1");
			id = id.replaceAll("\\s", " ?");
		} else if (id.toLowerCase().matches("kiaa\\d+")) {
			id = "(KIAA|Kiaa)" + id.substring(4);

		} else if (id.matches("[A-Z0-9]{1,6}_(HUMAN|RAT|MOUSE|YEAST|BOVIN)")) {
			id = id.substring(0, id.indexOf("_")) + "(_" + id.substring(id.indexOf("_") + 1) + ")?";
		}// else if (id.startsWith("HGNC:") || id.startsWith("PID:")) {
		//
		//}
		return id;
	}


	/**
	 * Analyzes a protein name (compound name) and returns a list
	 * of potential name variations to actually appear in text.
	 * <ul>
	 * <li>'Acyl-CoA dehydrogenase, short-chain specific, mitochondrial'
	 *     =&gt; 'Acyl-CoA dehydrogenase', 'short-chain specific Acyl-CoA dehydrogenase',
	 *     'mitochondrial short-chain specific Acyl-CoA dehydrogenase'
	 * <li>'Sarcoglycan, alpha (50kD dystrophin-associated glycoprotein; adhalin)'
	 *     =&gt; 'Sarcoglycan alpha', '50kD dystrophin-associated glycoprotein', 'adhalin'
	 * <li>'SMT3 (suppressor of mif two 3, yeast) homolog 2'
	 *     =&gt; 'SMT3', 'suppressor of mif two 3'
	 * <li>'snail 1 (drosophila homolog), zinc finger protein'
	 *     =&gt; 'snail 1'
	 * <li>synculein, alpha (bla) =%gt; 'alpha-synuclein', 'synuclein alpha'
	 * </ul>
	 *
	 * TODO
	 *
	 * @param name
	 * @return
	 */
	public static LinkedList<String> getProteinNameVariations (String name) {
		//String original = name;
		LinkedList<String> variations = new LinkedList<String>();

		if (verbosity > 1)
			System.out.println("0) '" + name + "'");

		name = name.replaceAll("([\\d\\.]+)\\s*([Kk][Dd][Aa])", "$1kDa");

		name = name.replaceFirst("^[Hh]ypothetical protein (.+)", "$1");
		if (verbosity > 1)
			System.out.println("1) '" + name + "'");

		// remove all species from names
		name = name.replaceAll("\\s\\((S(accharomyces|\\.)\\s?cerevisiae|[Dd]rosophila|X(enopus|\\.)\\s?laevis"
				+ "|mouse|rat|yeast|human|bacterial|M(us|\\.)\\s?musculus|E(scherichia|\\.)\\s?coli"
				+ "|C(aenorhabditis|\\.)\\s?elegans|S\\.\\s?pombe|H(\\.|omo)\\s?sapiens"
				+ "|Arabidopsis|A(\\.|rabidopsis)\\s?thaliana)([\\s\\-]homolog)\\)", "");
		name = name.replaceAll("\\s\\(homolog of (S(accharomyces|\\.)\\s?cerevisiae|[Dd]rosophila|X(enopus|\\.)\\s?laevis"
				+ "|mouse|rat|yeast|human|bacterial|M(us|\\.)\\s?musculus|E(scherichia|\\.)\\s?coli"
				+ "|C(aenorhabditis|\\.)\\s?elegans|S\\.\\s?pombe|H(\\.|omo)\\s?sapiens"
				+ "|Arabidopsis|A(\\.|rabidopsis)\\s?thaliana)(\\s[A-Za-z0-9\\.\\-]+)?\\)", "");

		if (verbosity > 1)
			System.out.println("2) '" + name + "'");

		// remove all "mitochondrial precursor", "short-chain specific" somewhere
		// but beware of: "testis specific, 10"
		name = name.replaceAll("(\\,\\s[a-z\\-\\/\\s]+[\\s\\-]specific)(\\,\\s|\\s\\(|$)", "$2");
		name = name.replaceAll("((\\,\\s)?([a-z\\-]+[\\s\\-])?specific)(\\,\\s|\\s\\(|$)", "$4");
		// ", neutral membrane" somewhere
		name = name.replaceAll("\\,\\sneutral\\s[a-z]+(\\,\\s|\\s\\(|$)", "$1");
		// "precursor" or ", xyz precursor" somewhere
		name = name.replaceAll("\\,\\s([a-z\\-]+\\s)?precursor(\\,\\s|\\s\\(|$)", "$2");
		name = name.replaceAll("\\sprecursor", "");
		// ", mitochondrial" at end of name
		name = name.replaceAll("\\,\\s([a-z]+al|[a-z]+ic)$", "");
		// "pseudogene" / "preprotein" at end of name
		name = name.replaceAll("(\\,\\s)?(pseudogene|preprotein|polypeptide)$", "");
		if (verbosity > 1)
			System.out.println("3) '" + name + "'");

		// remove all expressions in brackets
		// TODO: might contain additional synonyms!
		name = name.replaceAll("\\s\\[.+\\]([\\,\\;\\s]|$)", "");
		name = name.replaceAll("\\s\\(.+\\)([\\,\\;\\s]|$)", "");
		//if (verbosity > 1)
		//	System.out.println("4) '" + name +"'");

		// remove everything after a semicolon
		name = name.replaceFirst("^(.+?)\\;\\s.+$", "$1");
		if (verbosity > 1)
			System.out.println("4b) '" + name +"'");

		variations.add(name);


		// "synuclein, alpha" => "alpha synuclein"
		if (name.matches("^(.+)\\,\\s" + GREEK_WORDLETTER_REGEX + "$")) {
			String inv = name.replaceAll("^(.+)\\,\\s(" + GREEK_WORDLETTER_REGEX + ")$", "$2 $1");
			variations.add(inv);
			if (verbosity > 1)
				System.out.println("5) '" + inv + "'");
		}
		
		// xyz antigen -> Anti-xyz
		if (name.matches("(.+)[\\-\\s]?anti(gen|body)")) {
			String n = name.replaceFirst("^(.+)[\\-\\s]?anti(gen|body)$", "Anti-$1");
			variations.add(n);
			if (verbosity > 1)
				System.out.println("8) '" + n + "'");
		}

		// "nuclear factor NF bla" => "NF bla" OR "nuclear factor bla"
		// TODO generalize: long form abbrev => lf OR abbrev
		if (name.toLowerCase().matches("nuclear\\sfactor\\snf.+")) {
			String lf = name.replaceFirst("\\s(NF|Nf|nf|nF)", "");
			if (verbosity > 1)
				System.out.println("6a) " + lf);
			variations.add(lf);
			String sf = name.replaceFirst("[Nn]uclear\\s[Ff]actor\\s", "");
			if (verbosity > 1)
				System.out.println("6b) " + sf);
			variations.add(sf);
		}

		if (name.matches(".+\\,?\\siso(form|zyme)\\s[A-Za-z0-9]")) {
			String inv = name.replaceFirst("^(.+?)\\,?\\s(iso(?:form|zyme)\\s[A-Za-z0-9])$", "$1");
			variations.add(inv);
			if (verbosity > 1)
				System.out.println("7a) '" + inv + "'");
			inv = name.replaceFirst("^(.+?)\\,?\\s(iso(?:form|zyme)\\s[A-Za-z0-9])$", "$2 of $1");
			variations.add(inv);
			if (verbosity > 1)
				System.out.println("7b) '" + inv + "'");
		}

		return variations;
	}



	/**
	 *
	 * @param token
	 * @return
	 */
	public static String getTokenVariations (String token, int position) {
		String[] chars;
		String mid = "";

		//System.out.println("# Analyzing " + token);

		// single character
		// A => empty, A => alpha, I => 1, L => ligand, R => receptor
		if (token.matches("[A-Za-z]")) {
			if (token.matches("[Aa]"))
				//return "([Aa](lpha)?)?";
				return "[Aa](lpha)?";
			if (token.matches("[Bb]"))
				return "[Bb](eta)?";
			if (token.matches("[Ii]"))
				if (position == 0) return token;
				else return "[Ii1]";
			//if (token.matches("[Ii]"))
			//	return "[Ii1]?";
			if (token.matches("[Rr]"))
				return "[Rr](eceptor)?";
			if (token.matches("[Ll]"))
				return "[Ll](igand)?";

			return "[" + token.toUpperCase() + token.toLowerCase() + "]";
		}

		// multiple upper case characters:
		// FAS => Fas
		if (token.matches("[A-Z]+")) {
			//System.out.println("# multiple upper");
			if (token.equals("II"))
				if (position == 0) return "II";
				else return "(II|ii|2)";

			if (token.equals("III"))
				if (position == 0) return "III";
				else return "(II|ii|3)";

			chars = token.split("");
			//System.out.println("# has " + chars.length + "-1 characters");
			//for (int c = 0; c < chars.length; c++) {
			//	System.out.println("#   '" + chars[c] + "'");
			//}
			// Java String.split("") => first is always empty!!!
			int first = 1;
			int last = chars.length - 1;

			//System.out.println("# first was '" + chars[first] + "'");
			chars[first] = "[" + chars[first].toUpperCase() + chars[first].toLowerCase() + "]";
			//System.out.println("# first is '" + chars[first] + "'");
			if (chars.length > (first+1)) {
				//System.out.println("# more than one");
				chars[last] = "[" + chars[last].toUpperCase() + chars[last].toLowerCase() + "]";

				if (chars.length > (first+2)) {
					//System.out.println("# more than two");
					mid = chars[first+1];
					for (int c = (first+2); c < chars.length - 1; c++) {
						mid += chars[c];
					}
					if (mid.length() == 1) {
						mid = "[" + mid.toUpperCase() + "" + mid.toLowerCase() + "]";
					} else {
						mid = "(" + mid.toUpperCase() + "|" + mid.toLowerCase() + ")";
					}
					return chars[first] + mid + chars[last];
				}

				return chars[first] + chars[last];
			}
			return chars[first];
		}

		// multiple lower case characters
		if (token.matches("[a-z]+")) {
			if (token.equals("ii"))
				if (position == 0) return "ii";
				else return "(II|ii|2)";
			if (token.equals("iii"))
				if (position == 0) return "iii";
				else return "(III|iii|3)";
			if (token.equals("alpha"))
				//return "([Aa](lpha)?)?";
				return "[Aa](lpha)?";
			if (token.equals("beta"))
				return "[Bb](eta)?";
			if (token.equals("gamma"))
				return "[Gg](ammax)?";
			if (token.equals("ligand"))
				return "[Ll](igand)?";
			if (token.equals("receptor"))
				return "[Rr](eceptor)?";

			return getTokenVariations(token.toUpperCase(), position);
		}

		// Erbb => ErbB
		if (token.matches("[A-Z][a-z]+")) {
			//System.out.println("# upper than lower");
			return getTokenVariations(token.toUpperCase(), position);
		}

		// 95 => 95, 1 => i
		if (token.matches("\\d+")) {
			//System.out.println("# number");
			if (token.equals("1"))
				//return "[Ii1]?";
				return "[Ii1]";
			if (token.equals("2"))
				return "(II|ii|2)";

			return token;
		}

		// Abbreviation followed by number
		if (token.matches("([A-Z]+)(\\d+)")) {
			String string = token.replaceFirst("^([A-Za-z]+)(\\d+)$", "$1");
			String number = token.replaceFirst("^([A-Za-z]+)(\\d+)$", "$2");
			return getTokenVariations(string, position) + tokenDelimiterNoWhitespace + getTokenVariations(number, position);
		}

		// Abbreviation followed by number
		if (token.matches("([A-Za-z]+)(\\d+)")) {
			String string = token.replaceFirst("^([A-Za-z]+)(\\d+)$", "$1");
			String number = token.replaceFirst("^([A-Za-z]+)(\\d+)$", "$2");
			return getTokenVariations(string, position) + tokenDelimiterNoWhitespace + getTokenVariations(number, position);
		}

		// Abbreviation followed by number and ending with letters
		if (token.matches("([A-Za-z]+)(\\d+)([A-Za-z])")) {
			String string = token.replaceFirst("^([A-Za-z]+)(\\d+)([A-Za-z]+)$", "$1");
			String number = token.replaceFirst("^([A-Za-z]+)(\\d+)([A-Za-z]+)$", "$2");
			String string2 = token.replaceFirst("^([A-Za-z]+)(\\d+)([A-Za-z]+)$", "$3");
			return getTokenVariations(string, position)
				+ tokenDelimiterWithWhitespace + getTokenVariations(number, position)
				+ tokenDelimiterWithWhitespace + getTokenVariations(string2, position);
		}

		// erbB
		if (token.matches("[a-z]+[A-Z]")) return getTokenVariations(token.toUpperCase(), position);

		// MyD
		//if (token.matches("[A-Z][a-zA-Z]+[A-Z]")) return getTokenVariations(token.toUpperCase());
		if (token.matches("[a-zA-Z]+")) return getTokenVariations(token.toUpperCase(), position);

		if (!token.contains("."))
			System.err.println("# Term2Regex: unhandled token '" + token + "'");
		return token;
	}


	/**
	 *
	 * @param token
	 * @return
	 */
	public static String getSingularPluralRegex (String token) {
		// add plural forms when singular
		if (token.endsWith("ss"))
			return token + "(es)?";//token.replaceFirst("^(.*)ss$", "$1(ss|sses)");
		if (token.endsWith("y"))
			return token.replaceFirst("^(.*)y$", "$1(y|ies)");
		if (token.endsWith("us"))
			return token.replaceFirst("^(.*)us$", "$1(us|i)");
		if (token.endsWith("a"))
			return token.replaceFirst("^(.*)a$", "$1(a|ae|as)");
		if (token.matches(".*[A-Z]"))
			return token + "s?";

		// add singular forms when plural
		if (token.length() > 3 && token.endsWith("s"))
			return token + "?";

		return token;
	}


	/**
	 *
	 * @param term
	 * @return
	 */
	public static int getProteinNameCategory (String term) {
		if (isProteinID(term)) {
			//System.err.println("'" + term + "' is an ID");
			return 0;
		}

		if (isAbbreviation(term)) {
			//System.err.println("'" + term + "' is an abbreviation");
			return 1;
		}

		if (isLongTerm(term)) {
			//System.err.println("'" + term + "' is a long term / single word");
			return 2;
		}

		//System.err.println("'" + term + "' will be treated like a long term");
		return 2;
	}


	/**
	 * TODO use it. Variations: Na+ and Na(+)
	 * @param term
	 * @return
	 */
	public static boolean isIon (String term) {
		if (term.matches("(Ca|Cl|Fe|H|HCO3|I|K|L|Li|Mg|Na|P)([23]?[\\+\\-]|\\([23]?[\\+\\-]\\))")) return true;
		if (term.matches("(NADP|NAD\\(P\\))([\\+\\-]|\\([\\+\\-]\\))")) return true;
		return false;
	}


	/**
	 *
	 * @param term
	 * @return
	 */
	public static boolean isProteinID (String term) {
		if (term.matches("EC\\s?(\\d{1,3}|\\-)(\\.(\\d{1,3}|\\-)){2,4}")) {
			return true;
		}
		
		if (term.startsWith("HGNC:")
			|| term.startsWith("PID:")
			|| term.startsWith("GD:")
			|| term.toUpperCase().matches("KIAA\\d+")                            // occurs as KIAA, Kiaa, ...
			|| term.matches("[OPQ][0-9]([A-Z0-9]{3})[0-9]([\\.\\-][0-9]{1,2})?") // UniProt ID + isoform
			|| term.matches("[A-Z0-9]{1,6}_(HUMAN|RAT|YEAST|CAEEL|MOUSE)")       // UniProt Accession
			|| term.matches("FLJ\\d+")
			|| term.matches("LOC\\d+")
			|| term.matches("MGC\\d+")
			|| term.matches("ZNF\\d+")
			|| term.matches("OTTHUMP\\d+")
			|| term.matches("DKFZp\\d+")
			)
			return true;

		return false;
	}


	/**
	 *
	 * @param term
	 * @return
	 */
	public static boolean isAbbreviation (String term) {
		// allow for max 1 white space
		if (term.indexOf(" ") < term.lastIndexOf(" ")) return false;

		// Antigen p53
		if (term.matches("([Pp]rotein|[Aa]ntigen|[Gg]ene)\\s.+")) return false;
		if (term.matches(".+\\s([Pp]rotein|[Aa]ntigen|[Pp]recursor|[Gg]ene)")) return false;

		// Adrenodoxin reductase
		if (term.matches("([A-Za-z][a-z]+)\\s([A-Za-z][a-z]+)")) return false;

		return true;
	}


	/**
	 *
	 * @param term
	 * @return
	 */
	public static boolean isLongTerm (String term) {
		// one token only?
		if (term.indexOf(" ") == -1) {
			// upper case, symbols, digits?
			if (term.matches(".*[A-Z0-9\\-\\(\\)].*")) return false;
			else return true;
		}
		return true;
	}



	/***/
	public static boolean matchesPrepositionAndDigit(String name)
	{
		if (name.matches(PREPOSITIONS_REGEX + " \\d+")) {
			return true;
		}
		if (name.matches("\\d+ " + PREPOSITIONS_REGEX)) {
			return true;
		}
		return false;
	}

	/***/
	public static boolean matchesDigit(String name)
	{
		if (name.matches("(\\d*\\.)?\\d+")) {
			return true;
		}
		return false;
	}

	/***/
	public static boolean matchesRomanDigit(String name)
	{
		if (name.matches("([XVI]+|[xvi]+|[XVI][xvi]+)")) return true;
		return false;
	}

	/***/
	public static boolean matchesDigitsFromTo(String name)
	{
		if (name.matches("\\d+\\-\\d+")) {
			return true;
		}
		return false;
	}

	/***/
	public static boolean matchesDigitAndLetter(String name)
	{
		if (name.matches("[A-Za-z] \\d+")) {
			return true;
		}
		if (name.matches("\\d+ [A-Za-z]")) {
			return true;
		}
		return false;
	}

	/***/
	public static boolean matchesPrepositions(String name)
	{
		if (name.matches(PREPOSITIONS_REGEX + "( " + PREPOSITIONS_REGEX + ")*")) {
			return true;
		}
		return false;
	}

	/***/
	public static boolean matchesDeterminerAndDigit(String name)
	{
		if (name.matches("(the|an?) \\d+")) {
			return true;
		}
		if (name.matches("\\d+ (the|an?)")) {
			return true;
		}
		return false;
	}

	/***/
	public static boolean generateRegex(String name)
	{
		if (name.matches("[Gg]ene \\d+(\\.\\d+){0,1}"+tokenDelimiterWithWhitespace+"[Pp]rotein")) {
			return false;
		}
		if (name.matches("[Gg]ene \\d+(\\-\\d+){0,1}"+tokenDelimiterWithWhitespace+"[Pp]rotein")) {
			return false;
		}
		if (name.matches("\\d+"+tokenDelimiterWithWhitespace+"([Aa]ntigen|[Pp]rotein|[Gg]ene)")) {
			return false;
		}
		if (name.matches("([Aa]ntigen|[Pp]rotein|[Gg]ene)"+tokenDelimiterWithWhitespace+"\\d+")) {
			return false;
		}
		if (name.matches("\\d+\\-\\d+"+tokenDelimiterWithWhitespace+"([Aa]ntigen|[Pp]rotein|[Gg]ene)")) {
			return false;
		}
		if (name.matches("\\d+\\.\\d+"+tokenDelimiterWithWhitespace+"([Aa]ntigen|[Pp]rotein|[Gg]ene)")) {
			return false;
		}
		if (name.matches("\\d+[Ii]")) {
			return false;
		}
		if (name.matches("[Ii]\\d+")) {
			return false;
		}
		if (name.matches("\\d+(II|ii)")) {
			return false;
		}
		if (name.matches("(II|ii)\\d+")) {
			return false;
		}
		if (name.matches("\\d+\\.(\\-)?\\d+")) {
			return false;
		}
		if (name.matches("([Pp]rotein|[Gg]ene|[Aa]ntigen) \\d+(\\.\\d+){0,1}"+tokenDelimiterWithWhitespace+"([Hh]omolog|[Pp]recursor)?")) {
			return false;
		}
		if (name.matches("([Pp]rotein|[Gg]ene|[Aa]ntigen) \\d+(\\-\\d+){0,1}"+tokenDelimiterWithWhitespace+"([Hh]omolog|[Pp]recursor)?")) {
			return false;
		}
		if (name.matches("([Pp]rotein|[Gg]ene|[Aa]ntigen) (I|II|III|IV|V|VI|VII|VIII|IX|X|XI|XII|XIII|XIV|XV)"+tokenDelimiterWithWhitespace+"([Pp]rotein|[Gg]ene|[Hh]omolog|[Pp]recursor)?")) {
			return false;
		}
		if (name.matches("([Pp]rotein|[Gg]ene|[Aa]ntigen) (I|II|III|IV|V|VI|VII|VIII|IX|X|XI|XII|XIII|XIV|XV)"+tokenDelimiterWithWhitespace+"\\d+"+tokenDelimiterWithWhitespace+"([Pp]rotein|[Gg]ene|[Hh]omolog|[Pp]recursor)?")) {
			return false;
		}
		if (name.matches("[A-Z]\\d"))  // R1, L1
			return false;

		return true;
	}



	/**
	 * Takes a map from names to IDs and generates a map of regular expressions to IDs.
	 *
	 * @param names2ids
	 * @return
	 */
	public static Map<String, String> getMapFromRegexToIDs(Map<String, Set<String>> names2ids, Set<String> perfectMatchNames)
	{
		// maps from regex to ID
		HashMap<String, Set<String>> allVariations = new HashMap<String, Set<String>>();
		// maps from regex to source name
		// HashMap<String, String> allSources = new HashMap<String, String>();

		Set<String> allnames = names2ids.keySet();

		int category = 1;
		Iterator<String> allnamesIt = allnames.iterator();
		while (allnamesIt.hasNext()) {
			String regex = "";
			String name = (String) allnamesIt.next();
			Set<String> ID = names2ids.get(name);

			if (matchesDigit(name) || matchesRomanDigit(name) || matchesDigitsFromTo(name) || matchesDigitAndLetter(name) || matchesDeterminerAndDigit(name) || matchesPrepositionAndDigit(name) || matchesPrepositions(name)) {
				System.out.println("Term2Regex: Ignoring name " + name);
				continue;
			}

			if (isProteinID(name) && !name.matches("^(EC|KIAA|Kiaa).+$")) {
				System.out.println("Term2Regex: Ignoring ID " + name);
				continue;
			}

			// categorize the current name
			if (perfectMatchNames.contains(name) || !generateRegex(name)) {
				regex = name;
				System.out.println("Term2Regex: Perfect match regex for name " + name);
			}
			else {
				category = getProteinNameCategory(name);
				switch (category) {
				case 0: {
					regex = getRegexForProteinID(name);
					break;
				}
				case 1: {
					regex = getRegexForProteinAbbreviation(name);
					break;
				}
				default: {
					regex = getRegexForProteinName(name);
					break;
				}
				}
				// regex = "("+name+"|"+name.toLowerCase()+"|"+name.toUpperCase()+")";
			}

			if (regex != null) {
				if (allVariations.containsKey(regex)) {
					Set<String> idSet = allVariations.get(regex);
					idSet.addAll(ID);
				}
				else {
					Set<String> idSet = new HashSet<String>();
					idSet.addAll(ID);
					allVariations.put(regex, idSet);
				}
			}
		}

		Map<String, String> regexToIdString = new HashMap<String, String>();
		for (String regex : allVariations.keySet()) {
			Set<String> idset = allVariations.get(regex);
			String idString = ArrayHelper.joinStringArray(ArrayHelper.set2StringArray(idset), ";");
			regexToIdString.put(regex, idString);
		}

		return regexToIdString;
	}


	/**
	 * @param name
	 * @return
	 */
	public static String getRegexForProteinName(String name)
	{

		// if (name.indexOf("; ") >= 0)
		// name = name.substring(0, name.indexOf("; "));

		/*
		 * name = name.replaceFirst("^[Hh]ypothetical protein (.+)", "$1"); // remove all "mitochondrial precursor",
		 * "short-chain specific" somewhere name =
		 * name.replaceAll("(\\,\\s[a-z\\-\\/\\s]+[\\s\\-]specific)(\\,\\s|\\s\\(|$)", "$2"); name =
		 * name.replaceAll("((\\,\\s)?([a-z\\-]+[\\s\\-])?specific)(\\,\\s|\\s\\(|$)", "$4"); // ", neutral membrane"
		 * somewhere name = name.replaceAll("\\,\\sneutral\\s[a-z]+(\\,\\s|\\s\\(|$)", "$1"); // "precursor" or ", xyz
		 * precursor" somewhere name = name.replaceAll("((\\,\\s)?([a-z\\-]+\\s)?precursor)(\\,\\s|\\s\\(|$)", "$4"); // ",
		 * mitochondrial" at end of name name = name.replaceAll("\\,\\s([a-z]+al|[a-z]+ic)$", ""); // "pseudogene" /
		 * "preprotein" at end of name name = name.replaceAll("(\\,\\s)?(pseudogene|preprotein|polypeptide)$", "");
		 * System.err.println("1) '" + name + "'");
		 */

		// get some initial structural variations
		LinkedList<String> variations = getProteinNameVariations(name);
		// variations.add(name);

		String regex = "";
		if (variations.size() > 1)
			regex = "(";

		int current = 0;
		//
		for (String variation : variations) {
			if (variation.matches("[\\d+\\,\\-\\/\\_\\.\\;\\(\\)\\[\\]\\s]+")) {
				continue;
			}
			
			if (current >= 1)
				regex += "|";

			// split name into parts: English words, abbreviations, numbers
			variation = variation.replaceAll("\\-\\s", "-");
			variation = variation.replaceAll("\\-\\-", "-");

			String[] parts = variation.split("[\\s\\-]");
			boolean[] optional = new boolean[parts.length];

			for (int p = 0; p < parts.length; p++) {
				// System.err.println("# part: " + parts[p]);
				optional[p] = false;
				
				// last part is sometimes optional
				if (p == parts.length - 1 && parts[p].matches("([Pp]rotein|[Aa]ntigen|[Pp]recursor|[Pp]seudogene|[Gg]ene|[Hh]omolog)")) {
					// do not make 'protein' in "D protein" optional
					if (p > 0 && !parts[p-1].matches("[A-Za-z0-9]"))
						optional[p] = true;
					
				// first part is sometimes optional
				} else if (p == 0 && parts[p].matches("([Pp]rotein|[Aa]ntigen|[Gg]ene)")) {
					// do not make 'protein' in "protein D" optional
					if (parts.length > 1 && !parts[p+1].matches("[A-Za-z0-9]"))
						optional[p] = true;
					
				// some parts are always optional
				} else if (parts[p].matches("([Cc]hain|[Ii]sozyme|[Ii]soform|[Mm]ember|[Tt]ype|[Ss]ubunit)"))
					optional[p] = true;

				String pRegex = getRegexForProteinNamePart(parts[p]);
				// System.err.println("# regx: " + pRegex);

				if (p > 0) {
					if (optional[p]) {
						regex += "(" + tokenDelimiterWithWhitespace + pRegex + ")?";
					}
					else {
						regex += tokenDelimiterWithWhitespace + pRegex;
					}
				}
				else {
					if (optional[p]) {
						regex += "(" + pRegex + ")?";
					}
					else {
						regex += pRegex;
					}
				}
			}
			current++;
		}

		if (variations.size() > 1)
			regex += ")";

		// System.err.println("# Checking '" + regex + "'");
		// N-cadherin 1 in masterlist => N-cadherin in text
		// if (regex.endsWith("[Ii1]")) regex += "?";
		if (regex.endsWith("[Ii1]")) {
			// System.err.println("# Found [Ii1]");
			regex = regex.replaceFirst("^(.+)\\[\\s\\\\-\\]\\?\\[Ii1\\]$", "$1([ \\\\-]?[Ii1])?");
		}

		// retinoid X receptor alpha => retinoid X receptor
		if (regex.endsWith("[ \\-]?[Aa](lpha)?")) {
			// System.err.println("# Found alpha");
			regex = regex.replaceFirst("^(.+)\\[\\s\\\\-\\]\\?\\[Aa\\]\\(lpha\\)\\?$", "$1([ \\\\-]?[Aa](lpha)?)?");
		}

		// if (regex.endsWith("[Rr]eceptor"))
		// regex = regex.replaceFirst("^(.+)\\[Rr\\]eceptor$", "$1[Rr](eceptor)?");
		regex = regex.replaceFirst("\\[Rr\\]eceptor", "[Rr](eceptor)?");
		// if (regex.endsWith("[Ll]igand"))
		// regex = regex.replaceFirst("^(.+)\\[Ll\\]igand$", "$1[Ll](igand)?");
		regex = regex.replaceFirst("^\\[Ll\\]igand", "[Ll](igand)?");

		if (regex.matches(".+\\[\\s\\\\-\\]\\?\\[Kk\\]inase\\[\\s\\\\-\\]\\?.+")) {
			regex = regex.replaceFirst("(.+)(\\[\\s\\\\-\\]\\?)(\\[Kk\\]inase)(\\[\\s\\\\-\\]\\?)(.+)", "$1($2$3)?$4$5");
			// System.err.println("YES");
		}

		regex = regex.replaceAll("\\[Gg\\]lycine", "(\\[Gg\\]lycine|GLY|Gly)");

		if (regex.matches("((\\[Nn\\]uclear|\\[Ff\\]actor)\\[\\s\\\\-\\]\\?)+(.+)")) {
			regex = regex.replaceFirst("^(((\\[Nn\\]uclear|\\[Ff\\]actor)\\[\\s\\\\-\\]\\?)+)(.+)$", "($1)?$4");
		}

		regex = regex.replaceAll("\\,(\\[\\s|\\(\\[\\s)", ",?$1");

		// System.err.println("# New: '" + regex + "'");

		return regex;
	}

	/**
	 * @param part
	 * @return
	 */
	public static String getRegexForProteinNamePart(String part)
	{
		if (part.length() >= 4) {
			if (part.matches("[A-Za-z][a-z]+") && !isNumeral(part)) {
				String first = part.substring(0, 1);
				return "[" + first.toUpperCase() + first.toLowerCase() + "]" + part.substring(1);
			}
			if (part.matches("[\\d+\\.]+[Kk][Dd][Aa]?")) {
				return part.replaceFirst("([\\d+\\.]+)[Kk][Dd][Aa]?", "($1[ \\\\-]?[Kk][Dd][Aa]?)?");
			}
		}

		// System.err.println("# Getting regex for name part '" + part + "'");
		String regex = getRegexForProteinAbbreviation(part, false);

		return regex;
	}



	/**
	 * @param abbreviation
	 * @return
	 */
	public static String getRegexForProteinAbbreviation(String abbreviation)
	{
		return getRegexForProteinAbbreviation(abbreviation, true);
	}

	/**
	 * Generates variations of abbreviated protein names (Fas, CD95, C-ErbB2)
	 *
	 * @param abbreviation
	 * @return
	 */
	public static String getRegexForProteinAbbreviation(String abbreviation, boolean singleWord)
	{
		// System.err.println("# Abbreviation: '" + abbreviation + "'");

		String term = abbreviation;
		//String original = abbreviation;
		
		//if (term.matches("[A-Za-z]\\d+")) // L1, P1, R1
		//	return term;
		if (term.matches("([A-Za-z])([\\-\\s]?)(\\d+)")) { // L1, R1, P1, V-1
			String letter = term.replaceFirst("^([A-Za-z])([\\-\\s]?)(\\d+)$", "$1");
			String number = term.replaceFirst("^([A-Za-z])([\\-\\s]?)(\\d+)$", "$3");
			return "[" + letter.toUpperCase() + letter.toLowerCase() + "][ \\-]?" + number;
		}

		// escape some RegEx markup symbols
		term = term.replaceAll("\\(\\-\\)", "__LRB____MINUS____RRB__");
		term = term.replaceAll("\\(", "__LRB__");
		term = term.replaceAll("\\)", "__RRB__");
		term = term.replaceAll("\\[", "__LSB__");
		term = term.replaceAll("\\]", "__RSB__");
		term = term.replaceAll("\\{", "__LCB__");
		term = term.replaceAll("\\}", "__RCB__");
		term = term.replaceAll("\\<", "__LT__");
		term = term.replaceAll("\\>", "__GT__");
		term = term.replaceAll("\\.", "__DOT__");
		term = term.replaceAll("\\+", "__PLUS__");
		term = term.replaceAll("\\*", "__ASTERISK__");
		// term = term.replaceAll("\\_", "__UNDERSCORE__");

		// System.err.println("# Term: '" + term + "'");

		// introduce strong and weak bonds
		// from now on, a "-" indicates a strong, a " " a weak bond
		// a strong bond means: words stick together or have a hyphen inbetween
		// a weak bond means: same as strong, but words can also have a white space inbetween

		// optical gaps define bonds and their type
		// search for optical gaps: flow of letters/numbers is disturbed:
		// - change from upper to lower case (or vice versa)
		// - change from letters to numbers (or vice versa)
		// - ...

		// check for strong bonds
		// CD95 => CD-95, HER2 => HER-2
		term = term.replaceAll("([A-Za-z])(\\d+)", "$1-$2");
		term = term.replaceAll("(\\d+)([A-Za-z])", "$1-$2");
		term = term.replaceAll("([a-z])([A-Z])([a-z])", "$1-$2-$3");
		term = term.replaceAll("([A-Z])([a-z])([A-Z])", "$1-$2-$3");
		term = term.replaceAll("([a-z])([A-Z])", "$1-$2");
		term = term.replaceAll("([A-Z])([a-z])", "$1-$2");
		// fixed suffixes
		term = term.replaceFirst("R$", " receptor");
		term = term.replaceFirst("L$", " ligand");
		// special case: separate KV from name
		term = term.replaceFirst("^KV([^\\s])", "Kv $1");

		term = term.trim();
		// System.err.println("# Term: '" + term + "'");

		// check for weak bonds
		// TNF alpha => TNFalpha, TNF-alpha, or TNF alpha
		// create some more weak bonds based on syntax
		/*
		 * term = term.replaceAll("([0-9])\\-?([lL])\\-", "$1 ligand "); term = term.replaceAll("([0-9])\\-?([rR])\\-",
		 * "$1 receptor "); term = term.replaceAll("([0-9])\\-?([lL])$", "$1 ligand"); term =
		 * term.replaceAll("([0-9])\\-?([rR])$", "$1 receptor"); term = term.replaceAll("([a-z])\\-?L\\-", "$1 ligand
		 * "); term = term.replaceAll("([a-z])\\-?R\\-", "$1 receptor "); term = term.replaceAll("([a-z])\\-?L$", "$1
		 * ligand"); term = term.replaceAll("([a-z])\\-?R$", "$1 receptor");
		 */

		term = term.replaceAll("([0-9])\\-?([lLrR])(.*)", "$1 $2 $3");
		term = term.replaceAll("([A-Z])([lLrR])(?:\\-?(\\d+))", "$1 $2 $3");

		term = term.trim();
		// System.err.println("# Term: '" + term + "'");

		/*
		 * term = term.replaceAll(" [rR]\\-", " receptor "); term = term.replaceAll(" [lL]\\-", " ligand "); term =
		 * term.replaceAll(" [rR]$", " receptor"); term = term.replaceAll(" [lL]$", " ligand");
		 */

		term = term.replaceAll("A\\-?ntigen", "antigen");
		term = term.replaceAll("R\\-?eceptor", "receptor");
		term = term.replaceAll("L\\-?igand", "ligand");
		term = term.replaceAll("Precursor", "precursor");

		term = term.replaceAll("\\-([alpha|beta|gamma|delta|epsilon])$", " $1");
		term = term.replaceFirst("^([Pp]rotein)\\s", "([Pp]rotein )?");

		term = term.trim();

		// System.err.println("# Splitted into: '" + term + "'");

		// MASTER PLAN:
		// single letters can be upper or lower
		// multiple lower case letters can be all upper or all lower
		// multiple upper case letters can be XXX, Xxx, xxx
		// R/L at end can be receptor/ligand
		// antigen at beginning can be omitted
		// five or more lower case letters can only be xxxxx or Xxxxx, not XXXXX

		StringTokenizer tokenizer = new StringTokenizer(term, " -", true);
		String[] tokens = new String[tokenizer.countTokens()];
		String combined = "";
		LinkedList<String> parts = new LinkedList<String>();
		// for (String t: tokens)
		// parts.add(t);

		// store the index position of this part, 0: first part
		int p = -1;

		// go through all parts and examine them, build proper variations/reg.ex for each
		while (tokenizer.hasMoreTokens()) {
			p++;
			String part = tokenizer.nextToken();
			if (part.equals(" ") || part.equals("-") || part.equals("_")) {
				// parts[p] = part;
				combined += part;
				parts.add(part);
				continue;
			}

			// certain things should happen only when we are in the middle / at the end of a name
			if (p > 1 || tokens.length == 1) { // 1 = 0 + first delimiter
				if (isNumeral(part)) {
					String markup = getMarkupForNumerals(part);
					// System.err.println("# Markup for numeral: '" + markup + "'");
					if (!markup.equals("")) {
						combined += markup;
						parts.add(markup);
						continue;
					}
				}
			}

			if (part.matches("[LlRr]")) {
				if (part.equalsIgnoreCase("l")) {
					combined += "ligand";
					parts.add("ligand");
				}
				else {
					combined += "receptor";
					parts.add("receptor");
				}
				continue;
			}

			// single letter => upper or lower case
			if (part.matches("[A-Za-z]")) {
				// parts[p] = "[" + part.toUpperCase() + part.toLowerCase() + "]";
				combined += tokens[p] = "[" + part.toUpperCase() + part.toLowerCase() + "]";
				parts.add("[" + part.toUpperCase() + part.toLowerCase() + "]");
				continue;
			}

			// upper case only => XXXX, Xxxx, xxxx, XxxX
			if (part.matches("[A-Z]+")) {
				// parts[p] = "(" + part
				// + "|" + part.charAt(0) + part.substring(1, part.length()).toLowerCase()
				// + "|" + part.toLowerCase() + ")";
				combined += tokens[p] = "(" + part + "|" + part.charAt(0) + part.substring(1, part.length()).toLowerCase() + "|" + part.charAt(0) + part.substring(1, part.length() - 1).toLowerCase() + part.substring(part.length() - 1).toUpperCase() + "|" + part.toLowerCase() + ")";
				parts.add("(" + part + "|" + part.charAt(0) + part.substring(1, part.length()).toLowerCase() + "|" + part.charAt(0) + part.substring(1, part.length() - 1).toLowerCase() + part.substring(part.length() - 1).toUpperCase() + "|" + part.toLowerCase() + ")");
				continue;
			}

			// lower case only => xxxx or Xxxx or XXXX
			if (part.matches("[a-z]+")) {
				// parts[p] = "(" + part.substring(0, 1).toUpperCase() + part.substring(1, part.length())
				// + "|" + part + ")";
				if (part.matches("(receptor|ligand|antigen|precursor)")) {
					combined += part;
					parts.add(part);
				}
				else {
					combined += tokens[p] = "(" + part.substring(0, 1).toUpperCase() + part.substring(1, part.length()) + "|" + part + "|" + part.toUpperCase() + ")";
					parts.add("(" + part.substring(0, 1).toUpperCase() + part.substring(1, part.length()) + "|" + part + "|" + part.toUpperCase() + ")");
				}
				continue;
			}

			// one upper case then lower => XXXX, Xxxx, xxxx; for some, only Xxxx, xxxx
			if (part.matches("[A-Z][a-z]+")) {
				if (part.matches("(.+(ase|tive|sine|nal|son|rols?|rant|dant|sent|box" + "|sic|cic|dic|ion|ated|ate|ory|ates|xin|nin|ilin)|type|major)")) {
					combined += tokens[p] = "(" + part + "|" + part.substring(0, 1).toLowerCase() + part.substring(1, part.length()) + ")";
					parts.add("(" + part + "|" + part.substring(0, 1).toLowerCase() + part.substring(1, part.length()) + ")");
				}
				else {
					combined += tokens[p] = "(" + part.toUpperCase() + "|" + part + "|" + part.toLowerCase() + ")";
					parts.add("(" + part.toUpperCase() + "|" + part + "|" + part.toLowerCase() + ")");
				}
				continue;
			}

			// else
			// parts[p] = part;
			combined += part;
			parts.add(part);
		}
		term = combined;

		// some common abbreviations and words that can be omitted
		term = term.replaceAll("receptor", "[Rr](eceptor)?");
		term = term.replaceAll("ligand", "[Ll](igand)?");
		term = term.replaceAll(" antigen", " [Aa]ntigen");
		term = term.replaceAll("^antigen ", "([Aa]ntigen )?");
		term = term.replaceAll(" precursor", "( [Pp]recursor)?");
		// hypothetical protein
		// Probable serine/threonine-protein kinase zyg-1

		term = term.replaceAll("([<>])", "\\\\$1");

		term = term.replaceAll("__LRB____MINUS____RRB__", "\\\\(\\\\-\\\\)");
		term = term.replaceAll("__LRB__", "\\\\(");
		term = term.replaceAll("__RRB__", "\\\\)");
		term = term.replaceAll("__LSB__", "\\\\[");
		term = term.replaceAll("__RSB__", "\\\\]");
		term = term.replaceAll("__LCB__", "\\\\{");
		term = term.replaceAll("__RCB__", "\\\\}");
		term = term.replaceAll("__LT__", "\\\\<");
		term = term.replaceAll("__GT__", "\\\\>");
		term = term.replaceAll("__DOT__", "\\\\.");
		term = term.replaceAll("__PLUS__", "\\\\+");
		term = term.replaceAll("__MINUS__", "\\\\-");
		term = term.replaceAll("__ASTERISK__", "\\\\*");

		// term = term.replaceAll("([^\\\\])\\-", "$1" + tokenDelimiterNoWhitespaceR);
		term = term.replaceAll("\\s", tokenDelimiterWithWhitespaceR);
		term = term.replaceAll("([^\\\\])\\-", "$1" + tokenDelimiterWithWhitespaceR);
		term = term.replaceAll("_", tokenDelimiterWithWhitespaceUnderscoreR);

		/*
		 * String[] allparts = new String[parts.size()]; parts.toArray(allparts); for (String a: allparts)
		 * System.out.println(" " + a); System.out.println("-----");
		 */

		// System.err.println("# single word, before: '" + term + "'");
		if (singleWord) {
			if (term.endsWith("[Ii1]")) {
				term = term.replaceFirst("^(.+)\\[\\s\\\\-\\]\\?\\[Ii1\\]$", "$1([ \\\\-]?[Ii1])?");
			}
		}
		// System.err.println("# Expression: '" + term + "'");
		return term;
	}

}

