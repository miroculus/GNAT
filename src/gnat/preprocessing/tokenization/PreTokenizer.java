/**
 * 
 */
package gnat.preprocessing.tokenization;

/**
 * Pre-tokenizes sentences, but only regarding some errors made by the actual tokenizer. Do not use on its own!
 * This tokenizer makes up for some errors our part-of-speech tagger makes when tokenizing and tagging texts
 * at the same time.<br>
 * For instance,<br>
 * - words separated with slashes were not recognized as tokens properly in some cases (multiple slashes, long second terms)<br>
 * - &quot;-induced|mediated|dependent&quot; was not separeted from the preceeding token 
 * 
 * @author Joerg Hakenberg
 *
 */

public class PreTokenizer {

	/**
	 * Performs the pre-tokenization.
	 * */
	public static String pretokenize (String sentence) {
		return 
			sentence
			.replaceAll("([^\\+\\-])/([^\\+\\-])", "$1 / $2")
			//.replaceAll("/", "$1 / $2")
			.replaceAll("([^\\s])\\-(induced|mediated|dependent)", "$1 -$2")
			.replaceAll("\\-([A-Za-z0-9]+) (pathway|complex|heterodimer)", " - $1 $2")
			//.replaceAll("\\.(Aims?|Conclusions?|Results?|Methods?)([A-Z][a-z]*?)", ". $1: $2")
			//.replaceAll("^(Aims?|Conclusions?|Results?|Methods?)([A-Z][a-z]*?)", "$1: $2")
			;
//		String ret = sentence;
//		ret = ret.replaceAll("/", " / ");
//		ret = ret.replaceAll("([^\\s])\\-(induced|mediated|dependent)", "$1 -$2");
//		//ret = ret.replaceAll("\\s\\s", " ");
//		return ret;
	}
	
}
