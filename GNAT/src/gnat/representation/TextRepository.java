package gnat.representation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 *
 */

public class TextRepository {

	/** Maps text IDs to Text objects.<br>
	 *  Using a LinkedHashMap keeps FIFO order. */
	public Map<String, Text> textMap = new LinkedHashMap<String, Text>(); // enforce LinkedHashMap to keep FIFO order

//	public List<String> frontMatter = new LinkedList<String>(); // XML etc in a CitationSet before the first Citation
//	public List<String> backMatter  = new LinkedList<String>(); // XML etc in a CitationSet after the last Citation, for example, DeleteCitation in Medline XML!
	
	/**
	 *
	 *
	 */
	public TextRepository () { }
	

	/**
	 *
	 *
	 */
	public TextRepository (Collection<Text> texts) {
		for (Text text : texts)
	        addText(text);
	}


	/**
	 * Adds a {@link Text} to this repository.
	 * <br>If another Text with the same ID exists already, overwrites the old Text.
	 * @param text
	 */
	public void addText (Text text) {
		textMap.put(text.getID(), text);
	}
	
	
	/**
	 * Adds a collection of texts to this repository.
	 * <br>If another Text with the same ID exists already, overwrites the old Text.
	 * @param text
	 */
	public void addTexts (Collection<Text> texts) {
		for (Text text: texts) {
			textMap.put(text.getID(), text);
		}
	}


	/**
	 * Adds all the texts from the specified repository to this repository.
	 * */
	public void addAll(TextRepository textRepository){
		for (Text text : textRepository.getTexts()) {
	        addText(text);
        }
	}

	
	/**
	 * Returns a collection of all text IDs in this repository.
	 * @return
	 */
	public Collection<String> getTextIDs () {
		return textMap.keySet();
	}


	/**
	 * Returns all Text objects currently in the repository
	 * @return all Texts
	 */
	public Collection<Text> getTexts() {
	    return textMap.values();
    }


	/**
	 * Returns the Text object for the given ID. Returns null if not such
	 * ID was found.
	 * @return the Text object for the ID
	 */
	public Text getText (String id) {
		return textMap.get(id);
	}


	/**
	 * Checks whether the repository contains a text with the specified ID.
	 * @param id
	 * @return
	 */
	public boolean hasText (String id) {
		return textMap.containsKey(id);
	}


	/**
	 * Sets a text with ID and Text object. Overwrites an existing Text with the
	 * same ID.
	 * @param id
	 * @param text
	 */
	public void setText (String id, Text text) {
		textMap.put(id, text);
	}


	/**
	 * Returns the size of the text repository, that is, the number of texts.
	 * @return current number of texts in this repository
	 */
	public int size () {
		return textMap.size();
	}

	
	/**
	 * Clears this TextRepository, removing all Texts.
	 */
	public void clear () {
		textMap.clear();
	}
	
	
//	public void addFrontMatter (List<String> lines) {
//		frontMatter.addAll(lines);
//	}
//	
//	public void addFrontmatter (String line) {
//		frontMatter.add(line);
//	}
//	
//	public void addBackMatter (List<String> lines) {
//		backMatter.addAll(lines);
//	}
//	
//	public void addBackmatter (String line) {
//		backMatter.add(line);
//	}
//
//	public List<String> getFrontMatter () {
//		return this.frontMatter;
//	}
//	
//	public List<String> getBackMatter () {
//		return this.backMatter;
//	}
	
}
