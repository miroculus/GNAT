package gnat.preprocessing;

import gnat.utils.StringHelper;

public class TextPreprocessor {


	/**
	 * Sets first letter after a sentence to lower case.
	 * @param text
	 * @return
	 */
	public static String firstWordsToLowerCase (String text) {
		// transform all "English looking words" after a full stop
		// Definition: first letter upper case, than (at least) one lower case letter
		text = text.replaceAll("(^|\\.\\s)([A-Z])([a-z])", "$1xxxxx$2xxxxx$3");
		while (text.matches(".*?xxxxx[A-Z]xxxxx[a-z].*?")) {
			String match = text.replaceFirst("^.*?(xxxxx[A-Z]xxxxx)[a-z].*?$", "$1");
			String upper = text.replaceFirst("^.*?(xxxxx([A-Z])xxxxx)[a-z].*?$", "$2");
			//System.err.println("match='" + match + "', upper='" + upper + "'");
			String before = text.substring(0, text.indexOf(match));
			//System.err.println("before='" + before + "'");
			String after = text.substring(before.length() + match.length());
			//System.err.println("after='" + after + "'");
			
			text = before + upper.toLowerCase() + after;
			//break;
		}
		
		// as usual, some special cases exist: A and I can start a sentence
		// but do not look like "English words"
		text = text.replaceAll("(^|\\.\\s)([AI])(\\s)", "$1xxxxx$2xxxxx$3");
		while (text.matches(".*?xxxxx[AI]xxxxx\\s.*?")) {
			String match = text.replaceFirst("^.*?(xxxxx[AI]xxxxx)\\s.*?$", "$1");
			String upper = text.replaceFirst("^.*?(xxxxx([AI])xxxxx)\\s.*?$", "$2");
			//System.err.println("match='" + match + "', upper='" + upper + "'");
			String before = text.substring(0, text.indexOf(match));
			//System.err.println("before='" + before + "'");
			String after = text.substring(before.length() + match.length());
			//System.err.println("after='" + after + "'");
			
			text = before + upper.toLowerCase() + after;
			//break;
		}
		
		return text;
	}


	/**
	 * Returns everything but interpunctuation tokens.
	 * @param text
	 * @return
	 */
	public static String[] getEnglishWordTerms (String[] terms) {
		for (int t = 0; t < terms.length; t++) {
			//System.out.println("Old term: " + terms[t]);
			terms[t] = firstWordsToLowerCase(terms[t]);
			//System.out.println("New term: " + terms[t]);
		}
		
		return terms;
	}

	
	/**
	 * Returns everything but interpunctuation tokens.
	 * @param text
	 * @return
	 */
	public static String[] getEnglishWordTokens (String text) {
		//String original = text;
		text = text.replaceAll("([a-z]{2,})\\-([a-z]{2,})", "$1 $2");
		/*if (!text.equals(original)) {
			System.out.println("1: " + original);
			System.out.println("2: " + text);
		}*/
		
		text = firstWordsToLowerCase(text);
		
		// ...bla poly(a) bla...
		text = text.replaceAll("(^| )\\((.+?)\\)([\\s\\.\\,])", " $2 ");
		
		// ...bla (SREBs). Bla...
		text = text.replaceAll("([\\)\\]\\}])\\. ", " ");
		
		text = text.replaceAll("([\\.\\,\\;\\:\\?\\!\\)\\]\\}])( |$)", " ");
		
		text = text.replaceAll(" ([\\(\\[\\{])", " ");
		
		text = text.replaceAll("(^| )([A-Za-z0-9\\_\\-]+\\([A-Za-z0-9\\_\\-]+)([^\\)]) ", " $2$3) ");

		text = text.replaceAll("\\s\\s+", " ");

		String[] tokens = text.split(" ");
		tokens = StringHelper.removeStrings(tokens, StringHelper.stopWords);
		
		// set to singular forms
		for (int t = 0; t < tokens.length; t++) {
			if (tokens[t].endsWith("s")) {
				// neurofibromatosis
				if (tokens[t].endsWith("is")) continue;
				// stories
				tokens[t] = tokens[t].replaceFirst("ies$", "y");
				// stresses
				if (tokens[t].endsWith("sses")) {
					tokens[t] = tokens[t].replaceFirst("sses$", "ss");
					continue;
				}
				// nervous
				if (tokens[t].endsWith("ous")) continue;
				// normal plurals
				tokens[t] = tokens[t].replaceFirst("s$", "");
			}
		}
		
		text = null;
		return tokens;
	}


	public static void main (String[] args) {
		String test2 = "The modification of proteins with ubiquitin is an important cellular mechanism for targeting abnormal or short-lived proteins for degradation. Ubiquitination involves at least three classes of enzymes: ubiquitin-activating enzymes, or E1s, ubiquitin-conjugating enzymes, or E2s, and ubiquitin-protein ligases, or E3s. This gene encodes a member of the E2 ubiquitin-conjugating enzyme family. This enzyme functions in the ubiquitination of the tumor-suppressor protein p53, which is induced by an E3 ubiquitin-protein ligase. Two alternatively spliced transcript variants have been found for this gene and they encode distinct isoforms.";
		getEnglishWordTokens(test2);
		
		/*String test = "Cloning, expression and localization of an RNA helicase gene from a human lymphoid cell line with chromosomal breakpoint 11q23.3. A gene encoding a putative human RNA helicase, p54, has been cloned and mapped to the band q23.3 of chromosome 11. The predicted amino acid sequence shares a striking homology (75% identical) with the female germline-specific RNA helicase ME31B gene of Drosophila. Unlike ME31B, however, the new gene expresses an abundant transcript in a large number of adult tissues and its 5' non-coding region was found split in a t(11;14)(q23.3;q32.3) cell line from a diffuse large B-cell lymphoma.";
		String lower = firstWordsToLowerCase(test);
		System.out.println(test);
		System.out.println(lower);*/
	}
}
