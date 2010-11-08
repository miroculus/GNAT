package gnat.preprocessing.tokenization;

public class SimpleTokenizer {

	/**
	 * Adds additional whitespaces after sentence marks, and brackets and then splits at whitespaces.
	 *
	 * @param text
	 * @return
	 */
	public static String[] tokenize (String text) {
		text = text.replaceAll(" ([\\(\\{\\[])", " $1 ");
		text = text.replaceAll("([\\)\\]\\}\\.\\,\\?\\!]) ", " $1 ");

		return text.split(" ");
	}

}
