/* Created on 28.05.2004 */
package gnat.alignment;

/**
 * Representation of word. A word consists of a token and tag.<br>
 * <br>
 * <br>
 * TODO:<br>
 * - add attribute list (for generating XML output), used e.g. for:<br>
 *   - fb (Brit. Nat. Corpus frequeny),<br>
 *   - lemma, stem, or surface word<br>
 *   - other POS information, e.g. tempus, which is not encoded in the tag itself<br>
 *   - ID, position in sentence, (UniProt,) .. <br>
 *   - linguistic information, dependencies, ..<br>
 *   - entity class, if not corr. to tag<br>
 *   as vector or hashtable, ordering should be arbitray but fixed<br>
 * - set element tag (for XML output); default: 'token', but might be another one<br>
 * - XML output should include these attributes and use the setting for the element tag's name<br>
 *   
 * @author Conrad Plake, Joerg Hakenberg
 *
 */


public class Word {

	public String token;
	public String tag;
	
	/**
	 * Creates a new and empty word.
	 * */
	public Word() {
		this("", "");
	}


	/**
	 * Creates a new word for token and tag.
	 * @param token - token for this Word 
	 * @param tag - tag for this Word
	 * */
	public Word(String token, String tag) {
		this.token = token;
		this.tag = tag;
	}


	/**
	 * Returns the token of this word.
	 * @return token
	 * */
	public String getToken() {
		return token;
	}


	/**
	 * Returns the tag of this word.
	 * @return tag
	 * */
	public String getTag() {
		return tag;
	}


	/**
	 * Sets a new tag for this Word, replaces any old one
	 * @param t - new tag
	 */
	public void setTag (String t) {
	    tag = t;
	}


	/**
	 * Sets a new token for this Word, replaces any old one
	 * @param t - new token
	 */
	public void setToken (String t) {
	    token = t;
	}


	/**
	 * Sets a new token/tag pair for this Word, replaces any old values.
	 * @param tok - new token
	 * @param tg - new tag
	 */
	public void replace (String tok, String tg) {
	    token = tok;
	    tag = tg;
	}


	/**
	 * Clears this Word, i.e. deletes the value for the token and the tag.
	 */
	public void clear () {
	    token = "";
	    tag = "";
	}


	/**
	 * Returns a string representation of this word as: '('+token+'/'+'tag'+')'.
	 * @return (token/tag) pair
	 * */
	public String toString() {
		return "(" + token + "/" + tag + ")";
	}


	/**
	 * Returns the token/tag pair as one string, in Brown format: "token/tag"
	 * @return token/tag pair in Brown notation
	 */
	public String toBrown() {
	    return token + "/" + tag;
	}


	/**
	 * Returns the token/tag pair in XML format:<br>
	 * <tt>&lt;token tag=&quot;<em>tag</em>&quot;&gt;<em>token</em>&lt;/token&gt;</tt>
	 * @return token/tag pair in XML format
	 */
	public String toXML() {
	    return "<token tag=" + tag + ">" + token + "</token>";
	}


	/**
	 * Returns true if object o is of type Word and both tokens and tags are equal.
	 * @param o - object to compare to 
	 */
	public boolean equals(Object o) {
		boolean ret = false;
		Word w = (Word) o;
		if (w.token.equals(token) && w.tag.equals(tag)) {
			ret = true;
		}
		return ret;
	}

}