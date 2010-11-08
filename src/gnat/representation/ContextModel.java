package gnat.representation;

import gnat.comparison.CompareContextModels;
import gnat.comparison.CompareContextVectors;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;

/**
 *
 * A context model represents the typical context a term occurs in.
 *
 * <ul>
 * <li>for a gene name, this context would consist of species, chromosomal location,
 * GO terms, diseases, known (pathological) mutations, known texts (for instance, Entrez Gene Summary
 * or UniProt Comments), interaction partners of the gene's products, all known synonyms, database
 * identifiers, etc.
 * </ul>
 *
 *
 * A context model can have multiple context vectors. Each vector represents one type and instance
 * of context (for instance, all GO terms, or an entire Entrez Gene Summary).
 *
 *
 * @author Joerg Hakenberg
 *
 */


@SuppressWarnings("serial")
public class ContextModel implements Serializable {

	/** */
	public static String CONTEXTTYPE_TEXT = "TEXT";
	public static String CONTEXTTYPE_TEXTS = "TEXTS";
	public static String CONTEXTTYPE_TERM = "TERM";
	public static String CONTEXTTYPE_TERMS = "TERMS";
	public static String CONTEXTTYPE_CODE = "CODE";
	public static String CONTEXTTYPE_CODES = "CODES";
	public static String CONTEXTTYPE_UNKNOWN = "UNKNOWN";


	/** */
	public int verbosity = 0;

	/** */
	public LinkedList<ContextVector> contexts = new LinkedList<ContextVector>();

	/** */
	public String ID = "-1";

	/** */
	public float innerCoherence = 0.0f;

	/** */
	public boolean isValid = false;


	/** */
	public CompareContextModels comparer;



	/**
	 *	A new and empty context model.
	 */
	public ContextModel () { }


	/**
	 *	A new and empty context model with an ID.
	 *
	 * @param ID
	 */
	public ContextModel (String ID) {
		this.ID = ID;
	}


	/**
	 *
	 * Adds a vector to this model.
	 *
	 * @param cv
	 */
	public void addContextVector (ContextVector cv) {
		if (!contexts.contains(cv)) {
			contexts.add(cv);
			isValid = true;
		} else {
			System.err.println("# Adding context to model: duplicate context vector!");
		}
	}


	/**
	 *
	 * @param cv
	 * @return
	 */
	public float compareWithContextVector (ContextVector cv) {
		float maxscore = 0.0f;
		float score = 0.0f;

		for (ContextVector current: contexts) {
			score = CompareContextVectors.getCosineSimilarity(current, cv);
			if (score > maxscore)
				maxscore = score;
		}

		return maxscore;
	}


	/**
	 *
	 * @param cvs
	 * @return
	 */
	public float compareWithContextVectors (ContextVector [] cvs) {
		float maxscore = 0.0f;
		float score = 0.0f;

		// TODO: check for types
		for (ContextVector current: contexts) {
			for (ContextVector other: cvs) {
				score = CompareContextVectors.getCosineSimilarity(current, other);
				if (score > maxscore)
					maxscore = score;
			}
		}

		return maxscore;
	}


	/**
	 *
	 * @param cvs
	 * @return
	 */
	public float compareWithContextVectors (LinkedList<ContextVector> cvs) {
		float maxscore = 0.0f;
		float score = 0.0f;

		// TODO: check for types
		for (ContextVector current: contexts) {
			for (ContextVector other: cvs) {
				score = CompareContextVectors.getCosineSimilarity(current, other);
				if (score > maxscore)
					maxscore = score;
			}
		}

		return maxscore;
	}


	/**
	 * Returns all context vectors in this model that have the given context type.
	 * Searches for specific and basic types.
	 *
	 * @param type
	 * @return
	 */
	public Collection<ContextVector> getAllContextVectorsForType (String type) {
		Collection<ContextVector> result = new LinkedList<ContextVector>();
		for (ContextVector cv: contexts) {
			if (cv.specific_type.equals(type))
				result.add(cv);
		}
		for (ContextVector cv: contexts) {
			if (cv.basic_type.equals(type))
				result.add(cv);
		}
		return result;
	}


	/**
	 * Returns the context vector in this model (the first one found!) that has the
	 * given context type. First searches for specific types, then for basic types.
	 * Returns null if no such context vector was found.
	 *
	 * @param type
	 * @return
	 */
	public ContextVector getContextVectorForType (String type) {
		for (ContextVector cv: contexts) {
			if (cv.specific_type.equals(type))
				return cv;
		}
		for (ContextVector cv: contexts) {
			if (cv.basic_type.equals(type))
				return cv;
		}
		return null;
	}


	/**
	 * Returns an array of all context types known in this model.
	 * @return
	 */
	public String[] getContextVectorTypes () {
		String[] result = new String[this.contexts.size()];
		int count = 0;
		for (ContextVector cv: contexts) {
			result[count] = cv.basic_type + "/" + cv.specific_type;
			count++;
		}
		return result;
	}


	/**
	 * Merges the current with the given model. Keeps old values for conflicting
	 * elements (ID).<br>
	 * - adds all new contexts<br>
	 * - checks for a valid (not "-1") ID<br>
	 * @param model
	 */
	public void mergeWithOtherModel (ContextModel model) {
		// plain copy of all new contexts
		contexts.addAll(model.contexts);
		// copy the new model's ID if this one's was unknown
		if (!model.ID.equals("-1") && this.ID.equals("-1"))
			this.ID = model.ID;
	}

}
