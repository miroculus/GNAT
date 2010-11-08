package gnat.representation;

import gnat.comparison.CompareContextVectors;
import gnat.comparison.FeatureComparator;
import gnat.preprocessing.TextPreprocessor;
import gnat.utils.StringHelper;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Joerg Hakenberg
 *
 */

@SuppressWarnings("serial")
public class ContextVector implements Serializable {

	/** */
	public LinkedList<Feature> elements = new LinkedList<Feature>();

	/** Default: UNKNOWN */
	public String specific_type = ContextModel.CONTEXTTYPE_UNKNOWN;

	/** Default: PLAINTEXT */
	public String basic_type = ContextModel.CONTEXTTYPE_TEXT;

	/** */
	public float weight = 1.0f;

	/**
	 *
	 *
	 */
	public ContextVector () {
	}


	/**
	 * Contructs a new context vector from the given array of elements.
	 * Sets the element frequency as value for the features of this
	 * vector.
	 *
	 * @param elements
	 */
	public ContextVector (String[] elements) {
		for (String elem: elements) {
			addElement(new Feature(elem, 1.0f));
		}
	}

	/**
	 * Contructs a new context vector from the given array of elements.
	 * Sets the element frequency as value for the features of this
	 * vector.
	 *
	 * @param elements
	 */
	public ContextVector (List<String> elements) {
		for (String elem: elements) {
			addElement(new Feature(elem, 1.0f));
		}
	}


	/**
	 * Contructs a new context vector from the given array of elements.
	 * Sets the element frequency as value for the features of this
	 * vector. Also sets the weight of this context vector (default: 1.0)
	 * for later scoring.
	 *
	 * @param elements
	 * @param weight
	 */
	public ContextVector (String[] elements, float weight) {
		for (String elem: elements) {
			addElement(new Feature(elem, 1.0f));
		}
		this.weight = weight;
	}


	/**
	 * Contructs a new context vector from the given list of elements.
	 * Sets the element frequency as value for the features of this
	 * vector.<br>
	 * Sets the type of this context vector according to <code>specific_type</code>.<br>
	 * If the elements are of a TERM or TEXT type, they get transformed into lowercase.
	 * <br>
	 * @param elements -
	 * @param specific_type - type of the context vector (plain text, GO codes, ...)
	 */
	public ContextVector (List<String> elements, String specific_type) {
		this(elements);
		setSpecificType(specific_type);
	}


	/**
	 * Contructs a new context vector from the given array of elements.
	 * Sets the element frequency as value for the features of this
	 * vector.<br>
	 * Sets the type of this context vector according to <code>specific_type</code>.<br>
	 * If the elements are of a TERM or TEXT type, they get transformed into lowercase.
	 * <br>
	 * @param elements -
	 * @param specific_type - type of the context vector (plain text, GO codes, ...)
	 */
	public ContextVector (String[] elements, String specific_type) {
		this(elements);
		setSpecificType(specific_type);
	}


	/**
	 * Contructs a new context vector from the given array of elements.
	 * Sets the element frequency as value for the features of this
	 * vector.<br>
	 * Sets the type of this context vector according to <code>specific_type</code>.<br>
	 * If the elements are of a TERM or TEXT type, they get transformed into lowercase.
	 * <br>
	 * Also sets the weight of this context vector (default: 1.0) for later scoring.
	 * <br>
	 * @param elements -
	 * @param specific_type - type of the context vector (plain text, GO codes, ...)
	 * @param weight
	 */
	public ContextVector (String[] elements, String specific_type, float weight) {
		this(elements, weight);
		setSpecificType(specific_type);
	}


	/**
	 * Contructs a new context vector from the given array of features. Sets the
	 * value for each feature according to the original features. Duplicate features
	 * in the array will be mapped to one feature, with combined (sum) values.
	 * @param elements -
	 */
	public ContextVector (Feature[] elements) {
		for (Feature elem: elements) {
			addElement(elem);
		}
	}


	/**
	 * Contructs a new context vector from the given array of features. Sets the
	 * value for each feature according to the original features. Duplicate features
	 * in the array will be mapped to one feature, with combined (sum) values. Also
	 * sets the weight of this context vector (default: 1.0) for later scoring.
	 * @param elements -
	 * @param weight
	 */
	public ContextVector (Feature[] elements, float weight) {
		for (Feature elem: elements) {
			addElement(elem);
		}
	}


	/**
	 * Contructs a new context vector from the given array of features. Sets the
	 * value for each feature according to the original features. Duplicate features
	 * in the array will be mapped to one feature, with combined (sum) values.<br>
	 * Sets the type of this context vector according to <code>type</code>.<br>
	 * If the elements are of a TERM or TEXT type, they get transformed into lowercase.
	 * <br>
	 * @param elements -
	 * @param specific_type - type of the context vector (plain text, GO codes, ...)
	 */
	public ContextVector (Feature[] elements, String specific_type) {
		this(elements);
		setSpecificType(specific_type);
	}


	/**
	 * Contructs a new context vector from the given array of features. Sets the
	 * value for each feature according to the original features. Duplicate features
	 * in the array will be mapped to one feature, with combined (sum) values.<br>
	 * Sets the type of this context vector according to <code>type</code>.<br>
	 * If the elements are of a TERM or TEXT type, they get transformed into lowercase.
	 * <br>
	 * Also sets the weight of this context vector (default: 1.0) for later scoring.
	 * <br>
	 * @param elements
	 * @param specific_type - type of the context vector (plain text, GO codes, ...)
	 * @param weight
	 * @return
	 */
	public ContextVector (Feature[] elements, String specific_type, float weight) {
		this(elements);
		setSpecificType(specific_type);
	}


	/**
	 * Adds a feature to this context vector. If it is already present, changes
	 * only the value of the existing feature accordingly (sum of both values).
	 * @param elem - the new feature
	 */
	public void addElement (Feature elem) {
		//System.out.println("Adding " + elem.label + ":" + elem.value);
		if (!elements.contains(elem)) {
			//System.out.println("   new element");
			elements.add(elem);
		} else {
			//System.out.println("   existing element");
			//System.err.println("addElement(): Duplicate element " + elem);
			int idx = elements.indexOf(elem);
			Feature rem = elements.remove(idx);
			rem.value += elem.value;
			elements.add(idx, rem);
			//System.err.println("              New value: " + rem.value);
		}
	}


	/*/*
	 * Checks if one of the context vectors (current or other one) is a TEXT type
	 * and the other is a TERM type (or vice versa).
	 * <br>
	 * In this case, they can be compared, but all terms should also be splitted into tokens;
	 * or, the terms should be matched to the original, untokenized text.
	 * @param cv
	 * @return
	 */
	/*public boolean isTermsVersusTexts (ContextVector cv) {
		String prefix = this.type.split("_")[0];
		String foreign = cv.type.split("_")[0];

		if (prefix.startsWith("TERM") && foreign.startsWith("TEXT")) return true;
		if (prefix.startsWith("TEXT") && foreign.startsWith("TERM")) return true;

		return false;
	}*/


	/*/*
	 * Compares this vector to another one. Comparison is based on (sorted) features of
	 * both vectors.<br>
	 * Returns a negative value if the first, non-overlapping feature of
	 * this vector precedes the corresponding feature of the other vector. Returns 0 if both
	 * vectors are identical (again based on <code>Feature.equals()</code>). Returns a positive
	 * value if the other vector precedes the this one.
	 * <br>
	 * TODO implement
	 * @param o
	 * @return
	 */
	//public float compareTo (Object o) {
	//	return 0.0f;
	//}


	/**
	 * Checks if this context vector contains the given element.
	 * @param elem
	 * @return
	 */
	public boolean contains (String elem) {
		Feature feature = new Feature(elem, 1.0f);
		return elements.contains(feature);
		//return elements.contains(elem);
	}


	/**
	 * Checks if this context vector contains the given feature.
	 * @param feature
	 * @return
	 */
	public boolean contains (Feature feature) {
		return elements.contains(feature);
	}


	/**
	 * Checks if this context vector contains the given elements in the same order. Uses
	 * <code>toString()</code> and joins all elements with an optional white space or hyphen.
	 * @param elements
	 * @return
	 */
	public boolean containsSubsequent (String[] elements) {
		for (String e: elements)
			if (!contains(e)) {
				//System.out.println("# Not all single components found: " + e);
				return false;
			}

		boolean ret = (labelsToString().indexOf(StringHelper.joinStringArray(elements, " ")) >= 0);//([\\\\s\\\\-]?)") + ".*$"));
//		if (!ret) {
//			System.out.println("# '" + labelsToString() + "' does not contain '"
//					+ StringHelper.joinStringArray(elements, " ") + "'");
//		}
		return ret;
	}


	/**
	 * Computes the Euclidean Norm of this vector: sqrt(x*x), where * is the inner product.
	 * @return
	 */
	public float euclideanNorm () {
		float product = CompareContextVectors.innerproduct(this, this);
		return (float)Math.sqrt(product);
	}


	/**
	 * Returns the element at the given index position (could be a
	 * coordinate) of the vector.
	 * @param index
	 * @return
	 */
	public Feature getElement (int index) {
		if (index < 0 || index >= this.length()) return null;
		return elements.get(index);
	}


	/**
	 *
	 * @return
	 */
	public LinkedList<String> getElementLabels () {
		LinkedList<String> res = new LinkedList<String>();
		for (int e = 0; e < elements.size(); e++) {
			Feature f = getElement(e);
			res.add(f.label);
		}
		return res;
	}


	/**
	 *
	 * @return
	 */
	public float getWeight () {
		return weight;
	}


	/**
	 * Checks if this context vector is a TERM type (TERMS_GO, TERMS_KEYWORDS, ..)
	 * @return
	 */
	public boolean isCodeType () {
		return this.specific_type.split("_")[0].startsWith("CODE");
	}


	/**
	 * Checks if this context vector is a TERM type (TERMS_GO, TERMS_KEYWORDS, ..)
	 * @return
	 */
	public boolean isTermType () {
		return this.specific_type.split("_")[0].startsWith("TERM");
	}


	/**
	 * Checks if this context vector is a TEXT type (TEXT_PLAIN, TEXT_SUMMARY, ..)
	 * @return
	 */
	public boolean isTextType () {
		return this.specific_type.split("_")[0].startsWith("TEXT");
	}


	/*/*
	 * Checks if this context vector is a TEXT type (TEXT_PLAIN, TEXT_SUMMARY, ..)
	 * @return
	 */
	//public boolean isTextType () {
	//	return this.type.split("_")[0].startsWith("TEXT");
	//}


	/**
	 *
	 */
	public String labelsToString () {
		StringBuffer sb = new StringBuffer();
		sb.append(this.getElement(0).label);
		for (int i = 1; i < this.length(); i++) {
			sb.append(" ");
			sb.append(this.getElement(i).label);
		}

		return sb.toString();
	}


	/**
	 * Returns the number of elements currently present in this context vector.
	 * @return
	 */
	public int length () {
		return elements.size();
	}


	/**
	 *
	 * @param basic_type
	 */
	public void setBasicType (String basic_type) {
		this.basic_type = basic_type;
	}


	/**
	 * Sets the specific type of this vector, and changes the basic type accordingly.
	 * @param specific_type
	 */
	public void setSpecificType (String specific_type) {
		this.specific_type = specific_type;
		if (isTextType())
			setBasicType(ContextModel.CONTEXTTYPE_TEXT);
		else if (isTermType())
			setBasicType(ContextModel.CONTEXTTYPE_TERM);
		else if (isCodeType())
			setBasicType(ContextModel.CONTEXTTYPE_CODE);
		else
			setBasicType(ContextModel.CONTEXTTYPE_UNKNOWN);
	}


	/**
	 *
	 * @param weight
	 */
	public void setWeight (float weight) {
		this.weight = weight;
	}


	/**
	 * Sorts the elements of this context vector, according to Feature.compareTo, in
	 * ascending order.
	 */
	public void sort () {
		FeatureComparator fc = new FeatureComparator();
		Collections.<Feature>sort(elements, fc);
	}


	/**
	 * Splits all text/term features into tokens and creates single features
	 * for each token.
	 */
	public ContextVector splitElements () {
		//LinkedList<Feature> copy = new LinkedList<Feature>();

		ContextVector result = new ContextVector();
		result.setSpecificType(this.specific_type);
		String label = "";
		String[] labels = new String[0];
		float value = 0.0f;

		for (Feature f: elements) {
			label = f.label;
			value = f.value;

			labels = TextPreprocessor.getEnglishWordTokens(label);
			for (String lab: labels) {
				Feature nf = new Feature(lab, value);
				result.addElement(nf);
			}

		}

		return result;
	}


	/**
	 *
	 */
	public String toString () {
		StringBuffer sb = new StringBuffer();
		sb.append(this.getElement(0));
		for (int i = 1; i < this.length(); i++) {
			sb.append(" ");
			sb.append(this.getElement(i));
		}

		return sb.toString();
	}


	/**
	 * For testing purposes only.
	 * @param args
	 */
	public static void main (String[] args) {
		// CASP7_HUMAN
		String s1 = "Involved in the activation cascade of caspases responsible for apoptosis execution. At the onset of apoptosis it proteolytically cleaves poly(ADP-ribose) polymerase (PARP) at a '216-Asp-|-Gly-217' bond. Cleaves and activates sterol regulatory element binding proteins (SREBPs) between the basic helix-loop-helix leucine zipper domain and the membrane attachment domain. Cleaves and activates caspase-6, -7 and -9. Involved in the cleavage of huntingtin.";
		// CASP3_HUMAN
		String s2 = "Involved in the activation cascade of caspases responsible for apoptosis execution. Cleaves and activates sterol regulatory element binding proteins (SREBPs). Proteolytically cleaves poly(ADP-ribose) polymerase (PARP) at a '216-Asp-|-Gly-217' bond. Overexpression promotes programmed cell death.";
		//String s1 = "Test 1 3";
		//String s2 = "Testx x2 x3";

		//ContextVector c1 = new ContextVector(s1.split("([\\,\\.])?( |$)+"));
		//ContextVector c2 = new ContextVector(s2.split("([\\,\\.])?( |$)+"));
		ContextVector c1 = new ContextVector(TextPreprocessor.getEnglishWordTokens(s1));
		ContextVector c2 = new ContextVector(TextPreprocessor.getEnglishWordTokens(s2));



		System.out.println("C1: " + c1);
		System.out.println("C2: " + c2);
//		c1.sort();
//		c2.sort();
//		System.out.println("C1: " + c1);
//		System.out.println("C2: " + c2);

		int overlap = CompareContextVectors.getOverlap(c1, c2);
		float noverlap = CompareContextVectors.getNormalizedOverlap(c1, c2);
		float cosine = CompareContextVectors.getCosineSimilarity(c1, c2);

		System.out.println("Length_1: " + c1.length() + ", length_2: " + c2.length());

		System.out.println("Overlap: " + overlap);
		System.out.println("Normalized overlap: " + noverlap);
		System.out.println("Cosine similarity " + cosine);

		List<Feature> shared = CompareContextVectors.sharedElements(c1, c2);
		System.out.print("Shared elements:");
		for (Feature elem: shared)
			System.out.print(" " + elem);
		System.out.println();

		List<Feature> disjoint = CompareContextVectors.disjointElements(c1, c2);
		System.out.print("Disjoint elements:");
		for (Feature elem: disjoint)
			System.out.print(" " + elem);
		System.out.println();

	}

}
