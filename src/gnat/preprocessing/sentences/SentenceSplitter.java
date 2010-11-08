package gnat.preprocessing.sentences;

/**
 * 
 * A SentenceSplitter takes a piece of text and splits it into individual sentences.
 * 
 * @author Joerg
 *
 */
public interface SentenceSplitter {

	/**
	 * 
	 * @param text
	 * @return
	 */
	public String[] split (String text);
	
}
