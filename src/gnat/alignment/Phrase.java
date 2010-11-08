/* Created on 28.06.2004 */
package gnat.alignment;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * A representation of an annotated text.
 *
 * @author Conrad Plake
 * @author Joerg Hakenberg
 */

public class Phrase {


	/** Stores the words in this phrase */
	private List<Word> wordList;


	/**
	 * Constructs an empty phrase.
	 */
	public Phrase() {
		this(new Word[0]);
	}


	/**
	 * Constructs a new Phrase containing all words in the given list.
	 * @param words - list of Words to initialize the Phrase
	 */
	public Phrase (List<Word> words) {
		wordList = new LinkedList<Word>();
		wordList.addAll(words);
	}


	/**
	 * Constructs a Phrase from an XML source.<br>
	 * Example format:<br>
	 * <tt>&lt;phrase relation=&quot;2,0&quot;&gt;ANY/PTN and/cnj:K H4/xinc ANY/PTN H2A/xinc were/v:V affected/v:P to/prep a/det much/adv:b greater/adj:c degree/n:e than/cnj:S those/pron of/prep ANY/PTN&lt;/phrase&gt;</tt>
	 * @param xmlSource
	 */
	public Phrase (String xmlSource) {
		String sequence = xmlSource.replaceAll("^.*<phrase[^>]*?>(.*)</phrase>.*$", "$1");
		//System.err.println("% Building phrase from '" + sequence + "'");
		String[] list = sequence.split(" ");
		Word[] words = new Word[list.length];
		for (int l = 0; l < list.length; l++) {
			String[] layers;
			// if the source contains two // directly after each other, the first belongs to the content, the second splits layers
			if (list[l].matches(".*//.*")) {
				list[l] = list[l].replaceAll("//", "xxxSLASHxxx/");
				layers =  list[l].split("/");
				for (int i = 0; i < layers.length; i++) {
					layers[i] = layers[i].replaceAll("xxxSLASHxxx", "/");
				}
			} else {
		       layers = list[l].split("/");
			}

			/*words[l] = new MultiLayerWord(layers);
			words[l].setLayerNames(new String[]{"token", "tag"});*/
			words[l] = new Word(layers[0], layers[1]);
		}
		wordList = new LinkedList<Word>();
		for (int i=0; i < words.length; i++){
			wordList.add(words[i]);
		}
		//System.err.println(this.toWordSequence());
	}


	/*/*
	 * Constructs a Phrase from an XML source, each Word gets <tt>layerCount</tt> layers.<br>
	 * Example format:<br>
	 * <tt>&lt;phrase relation=&quot;2,0,1,1&quot;&gt;ANY/PTN and/cnj:K H4/xinc ANY/PTN H2A/xinc were/v:V affected/v:P to/prep a/det much/adv:b greater/adj:c degree/n:e than/cnj:S those/pron of/prep ANY/PTN&lt;/phrase&gt;</tt>
	 * @param xmlSource - source string
	 * @param layerCount - number of layers
	 */
	/*public Phrase (String xmlSource, int layerCount) {
		String sequence = xmlSource.replaceAll("^.*<phrase[^>]*?>(.*)</phrase>.*$", "$1");
		//System.err.println("% Building phrase from '" + sequence + "'");
		String[] list = sequence.split(" ");
		Word[] words = new Word[list.length];
		for (int l = 0; l < list.length; l++) {
			String[] layers = new String[layerCount];
			// if the source contains two // directly after each other, the first belongs to the content, the second splits layers
			//if (list[l].matches(".*()//.*")) {   /// REMOVE middle ()
				//list[l] = list[l].replaceAll("//", "xxxSLASHxxx/");
				//layers =  list[l].split("/");
				//for (int i = 0; i < layers.length; i++) {
				//	layers[i] = layers[i].replaceAll("xxxSLASHxxx", "/");
				//}
			//} else {
		    //   layers = list[l].split("/");
			//}
			if (layerCount == 2) {
				layers[0] = list[l].replaceFirst("^(.*)/(.*?)$", "$1");
				layers[1] = list[l].replaceFirst("^(.*)/(.*?)$", "$2");

			}

			//words[l] = new MultiLayerWord(layers);
			//words[l].setLayerNames(new String[]{"token", "tag"});
			words[l] = new Word(layers[0], layers[1]);

		}
		wordList = new LinkedList<Word>();
		for (int i=0; i < words.length; i++){
			wordList.add(words[i]);
		}
		//System.err.println(this.toWordSequence());
	}*/


	/**
	 * Constructs a new Phrase for an array of words.
	 * @param words - array of Words to initialize this Phrase
	 */
	public Phrase (Word[] words) {
		wordList = new LinkedList<Word>();
		for (int i=0; i < words.length; i++){
			wordList.add(words[i]);
		}
	}


	/**
	 * Adds a new Word for token and tag to the end of this phrase.
	 * @param token - token for the new Word
	 * @param tag - tag of the new Word
	 * */
	public void append (String token, String tag) {
		wordList.add(new Word(token, tag));
	}


	/**
	 * Adds Word w to the end of this phrase.
	 * @param w - word to append
	 */
	public void append (Word w){
		wordList.add(w);
	}


	/**
	 * Adds all words from phrase p to the end of this phrase.
	 * @param p - Phrase to append
	 */
	public void appendAll (Phrase p) {
		wordList.addAll(p.getWordList());
	}


	/**
	 * Removes all Words from this phrase.
	 */
	public void clear () {
		wordList.clear();
	}


	/**
	 * Replaces every tag in this Pattern with the empty String &quot;&quot;
	 */
	public void clearTags () {
	    clearTags("");
	}


	/**
	 * Replaces every tag in this Pattern with the String <tt>set</tt>.
	 * @param set
	 */
	public void clearTags (String set) {
	    Iterator<Word> it = wordList.iterator();
		for (int i = 0; it.hasNext(); i++) {
		    Word w = (Word)it.next();
		    w.setTag(set);
		    wordList.set(i, w);
		}
	}


	/**
	 * Replaces every token in this Pattern with the empty String &quot;&quot;
	 */
	public void clearTokens () {
	    clearTokens("");
	}


	/**
	 * Replaces every token in this Pattern with the String <tt>set</tt>.
	 * @param set
	 */
	public void clearTokens (String set) {
	    Iterator<Word> it = wordList.iterator();
		for (int i = 0; it.hasNext(); i++) {
		    Word w = (Word)it.next();
		    w.setToken(set);
		    wordList.set(i, w);
		}
	}


	/**
	 * Returns a new Phrase, where subsequent Words with equal tags are compressed into one Word.
	 * @return compress Phrase
	 * */
	public Phrase compress () {
		Phrase p = new Phrase();
		for (int i = 0; i < length(); i++) {
			Word w = getWord(i);
			if (i<length()-1 && getWord(i+1).tag.equals(w.tag)) {
				StringBuffer tokens = new StringBuffer(w.token);
				String tag = w.tag;
				i++;
				w = getWord(i);
				while(tag.equals(getWord(i).tag)) {
					tokens.append(" ");
					tokens.append(w.token);
					i++;
					if (i == length()) {
						break;
					}
					w = getWord(i);
				}
				p.append(new Word(tokens.toString().trim(), tag));
				i--;
			} else {
				p.append(w);
			}
		}
		return p;
	}


	/**
	 * Checks whether the Phrase contains a given tag or not
	 * @param tag - the tag to check
	 * @return true if the Phrase contains this tag
	 */
	public boolean containsTag (String tag) {
	    return (" "+this.toTagSequence()+" ").indexOf(" "+tag+" ") >= 0;
	}


	/**
	 * Checks whether the Phrase contains a given token or not
	 * @param token - token to check the Phrase againt
	 * @return true if the Phrase contains the token
	 */
	public boolean containsToken (String token) {
	    return (" " + this.toTokenSequence() + " ").indexOf(" " + token + " ") >= 0;
	}


	/**
	 * Checks whether the Phrase contains a given Word or not
	 * @param word - the word to search
	 * @return true if the Phrase contains the given Word
	 */
	public boolean containsWord (Word word) {
	    return wordList.contains(word);
	}


	/**
	 * Returns true, if object o is a phrase that is equal to this phrase. See Phrase.equals(Phrase p).
	 * @param o - object to compare this Phrase to
	 * @return true if the object is the same as this Phrase
	 */
	public boolean equals (Object o) {
		return equals((Phrase)o);
	}


	/**
	 * Returns true, if word w_i in Phrase p is equal to word w_i in this Phrase for all indices i.
	 * @return true if the Phrases are the same
	 */
	public boolean equals (Phrase p) {
		boolean equal = false;
		Word[] w = p.getWords();
		Word[] words = getWords();
		if(w.length ==words. length){
			equal = true;
			for(int i=0;i<w.length;i++){
				if(!w[i].equals(words[i])){
					equal = false;
					break;
				}
			}
		}
		return equal;
	}


	/**
	 * Returns a String of all tokens (no tags!) separated by whitespaces.
	 * @return token sequence as a String
	 */
	public @Deprecated String getTokenString () {
		return toTokenSequence();
		/*StringBuffer buffer = new StringBuffer();
		Iterator it = wordList.iterator();
		for(int i=0;it.hasNext();i++){
			buffer.append(  ((Word)it.next()).getToken() );
			buffer.append(" ");
		}
		return buffer.toString().trim();*/
	}


	/**
	 * Returns the word at the given index.
	 * @param index - position of the Word to get
	 * @return Word at the given position
	 */
	public Word getWord (int index) {
		return (Word)wordList.get(index);
	}


	/**
	 * Returns a list of all words in this phrase.
	 * @return list of all Words
	 */
	public List<Word> getWordList () {
		return wordList;
	}


	/**
	 * Returns an array of all words in this phrase.
	 * @return array of Words
	 */
	public Word[] getWords() {
		Word[] wordArr = new Word[wordList.size()];
		Iterator<Word> it = wordList.iterator();
		for(int i=0;it.hasNext();i++){
			wordArr[i] = (Word) it.next();
		}
		return wordArr;
	}


	/**
	 * Inserts word w into this phrase at the given index.
	 * @param w - Word to insert
	 * @param index - position where to put the new Word
	 * */
	public void insert (Word w, int index) {
		wordList.add(index, w);
	}


	/**
	 * Returns the number of words in this phrase.
	 * */
	public int length(){
		return wordList.size();
	}


	/**
	 * Removes and returns the Word at the given index.
	 * @param index - position of the Word to remove
	 * @return the Word just removed
	 */
	public Word remove (int index) {
		return (Word)wordList.remove(index);
	}


	/**
	 * Replaces the word at the given index with a new word
	 * @param w - the new Word
	 * @param index - position for the new Word, replacing the old Word at this position
	 */
	public void replace (int index, Word w) {
	    remove(index);
	    insert(w, index);
	}


	/**
	 * Returns this Phrase in reverse order.
	 * @return Phrase in reverse order
	 * */
	public Phrase reverse () {
		Phrase p = new Phrase();
		Word[] words = getWords();
		for(int i=words.length-1;i>=0;i--){
			p.append( words[i] );
		}
		return p;
	}


	/**
	 * Returns a subphrase of this phrase, starting and ending at the given index positions.
	 * @param beginIndex
	 * @param endIndex
	 * @return
	 */
	public Phrase subphrase (int beginIndex, int endIndex) {
		Phrase ret = new Phrase();
		Word[] words = getWords();
		for (int i = beginIndex; i < endIndex; i++) {
			ret.append(words[i]);
		}
		return ret;
	}


	/**
	 * Returns the pattern's tag sequence with XML markup.
	 * Format: <tt>&lt;pattern type subtype relations minscore maxscore&gt;pattern&lt;/pattern&gt;</tt>
	 * Default values for the attributes are:
	 *           type=&quot;unknown&quot;
	 *           subtype=&quot;unknown&quot;
	 *           relations=&quot;0,0,0&quot;
	 *           minscore=&quot;0&quot;
	 *           maxscore=&quot;1000&quot;
	 * @return String - pattern with XML markup, default values for attributes
	 */
	public String tagsToXMLPattern () {
	    return "<pattern type=\"unknown\" subtype=\"unknown\" relations=\"0,0,0,0\" minscore=\"0\" maxscore=\"0\">" +
	    	   this.toTagSequence().trim() + "</pattern>";
	}


	/**
	 * Returns a string for all words in this phrase, calls <tt>toWordSequence()</tt>
	 * @return word sequence
	 */
	public String toString () {
		return toWordSequence();
	}


	/**
	 * Returns a string of all tags, delimited with a single blank
	 * @return tag sequence
	 */
	public String toTagSequence () {
		StringBuffer sb = new StringBuffer();
		Iterator<Word> it = wordList.iterator();
		while(it.hasNext()){
			sb.append(((Word)it.next()).getTag());
			sb.append(" ");
		}
		return sb.toString().trim();
	}


	/**
	 * Returns a string of all tokens, delimited with a single blank
	 * @return token sequence
	 */
	public String toTokenSequence () {
		StringBuffer sb = new StringBuffer();
		Iterator<Word> it = wordList.iterator();
		while (it.hasNext()){
			sb.append(((Word)it.next()).getToken());
			sb.append(" ");
		}
		return sb.toString().trim();
	}


	/**
	 * Returns a string for all words in this phrase.
	 * @return word sequence
	 */
	public String toWordSequence () {
		StringBuffer sb = new StringBuffer();
		Iterator<Word> it = wordList.iterator();
		while(it.hasNext()){
			sb.append((Word)it.next());
			sb.append(" ");
		}
		return sb.toString();
	}

	/**
	 * Returns a string for all words in this phrase directly appended (without whitespaces inbetween).
	 * @return word sequence
	 */
	public String toSingleWordString () {
		StringBuffer sb = new StringBuffer();
		Iterator<Word> it = wordList.iterator();
		while(it.hasNext()){
			sb.append((Word)it.next());
		}
		return sb.toString();
	}

}