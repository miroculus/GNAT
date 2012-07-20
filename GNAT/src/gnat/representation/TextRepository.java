package gnat.representation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 *
 */

public class TextRepository {

	/** Maps text IDs to Text objects */
	public Map<String, Text> textMap = new HashMap<String, Text>();


	/**
	 *
	 *
	 */
	public TextRepository () {
	}
	

	/**
	 *
	 *
	 */
	public TextRepository (Collection<Text> texts) {
		for (Text text : texts) {
	        addText(text);
        }
	}


	/**
	 * Adds a text to this repository.
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
		for (Text text: texts)
			textMap.put(text.getID(), text);
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
	public Collection<Text> getTexts()
	{
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
}
