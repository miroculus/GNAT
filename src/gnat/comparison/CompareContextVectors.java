package gnat.comparison;

import gnat.representation.ContextVector;
import gnat.representation.Feature;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("serial")
public class CompareContextVectors implements Serializable {

	/** */
	public static int verbosity = 0;

	/**
	 * Checks if the two context vectors can be compared with one another.
	 * Decision is based on the <code>basic_type</code> of both vectors.
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	public static boolean canCompare (ContextVector cv1, ContextVector cv2) {
		if (cv1.basic_type.equals(cv2.basic_type)) return true;

		String prefix = cv1.basic_type.split("_")[0];
		String foreign = cv2.basic_type.split("_")[0];

		if (prefix.startsWith("TERM")) {
			if (foreign.startsWith("TERM") || foreign.startsWith("TEXT"))
				return true;
		} else if (prefix.startsWith("TEXT")) {
			if (foreign.startsWith("TERM") || foreign.startsWith("TEXT"))
				return true;
		}

		return false;
	}


	/**
	 * Returns all elements that are not common to both vectors.
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<Feature> disjointElements (ContextVector cv1, ContextVector cv2) {
		List<Feature> temp1 = (LinkedList<Feature>)cv1.elements.clone();
		List<Feature> temp2 = (LinkedList<Feature>)cv2.elements.clone();
		temp1.removeAll(cv2.elements);
		temp2.removeAll(cv1.elements);
		temp1.addAll(temp2);

		return temp1;
	}


	/**
	 * Calculates the cosine of the angle between both vectors as a
	 * measure of similarity.<br>
	 * cos0 = x * y / (||x|| * ||y||)
	 * <br>
	 * A return value of zero means that both vectors are orthogonal.
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	public static float getCosineSimilarity (ContextVector cv1, ContextVector cv2) {
		return innerproduct(cv1, cv2) / (cv1.euclideanNorm() * cv2.euclideanNorm());
	}


	/**
	 * Returns the overlap between both context vectors (number of shared elements).
	 * Normalizes this absolute number with the length of the shortest of the two
	 * vectors.
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	public static float getMinLenNormalizedOverlap (ContextVector cv1, ContextVector cv2) {
		float overlap = (float)getOverlap(cv1, cv2);
		int minlen = Math.min(cv1.length(), cv2.length());
		return overlap / (float)minlen;
	}


	/**
	 * Returns the overlap between both context vectors (number of shared elements).
	 * Normalizes this absolute number with the length of the shortest of the two
	 * vectors.
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	public static float getNormalizedOverlap (ContextVector cv1, ContextVector cv2) {
		float overlap = (float)getOverlap(cv1, cv2);
		float avglen = ( (float)cv1.length() + (float)cv2.length() ) / 2.0f;
		return overlap / avglen;
	}


	/**
	 * Returns the overlap between (number of shared elements) of both context vectors.
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static int getOverlap (ContextVector cv1, ContextVector cv2) {
		LinkedList<String> temp = (LinkedList<String>)cv1.elements.clone();
		temp.retainAll(cv2.elements);
		if (verbosity > 2 && temp.size() > 0)
			System.out.println("   Shared elements: " + temp.toString());
		return temp.size();
	}


	/**
	 *
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	public float getSimilarity (ContextVector cv1, ContextVector cv2) {
		if (!canCompare(cv1, cv2)) return -1.0f;

		if (verbosity > 3) {
			System.out.println("# comparing " + cv1.specific_type + " with " + cv2.specific_type +
				" (" + cv1.basic_type + "/" + cv2.basic_type + ")");
			System.out.println("# " + cv1 + "\n# " + cv2);
		}
		float sim = 0.0f;

		float normOverlap = getNormalizedOverlap(cv1, cv2);
		if (normOverlap > sim) sim = normOverlap;

		float cosineSim = getCosineSimilarity(cv1, cv2);
		if (cosineSim > sim) sim = cosineSim;

		float splittedCosineSim = 0.0f;
		if (cv1.isTermType() && cv2.isTextType()) {
			ContextVector c = cv1.splitElements();
			splittedCosineSim = CompareContextVectors.getCosineSimilarity(c, cv2);
		} else if (cv1.isTextType() && cv2.isTermType()) {
			ContextVector c = cv2.splitElements();
			splittedCosineSim = CompareContextVectors.getCosineSimilarity(cv1, c);
		}
		if (splittedCosineSim > sim) sim = splittedCosineSim;

		if (verbosity > 3) {
			System.out.print("# norm.overlap=" + normOverlap + ", cos=" + cosineSim + ", splittedCos=" + splittedCosineSim);
		}

		if (verbosity > 3) {
			System.out.println(", max="+sim);
		}

		return sim;//cosineSim;
	}



	/**
	 * Calculates the inner product of two vectors.
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	public static float innerproduct (ContextVector cv1, ContextVector cv2) {
		float product = 0.0f;

		List<Feature> shared = sharedElements(cv1, cv2);

		int idx1;
		int idx2;

		for (Feature s: shared) {
			idx1 = cv1.elements.indexOf(s);
			idx2 = cv2.elements.indexOf(s);
			product += cv1.elements.get(idx1).getValue() * cv2.elements.get(idx2).getValue();
		}

		return product;
	}


	/**
	 * Returns all elements common to both context vectors.
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<Feature> sharedElements (ContextVector cv1, ContextVector cv2) {
		List<Feature> temp1 = (List<Feature>)cv1.elements.clone();
		List<Feature> temp2 = (List<Feature>)cv2.elements.clone();
		temp1.retainAll(cv2.elements);
		temp2.retainAll(cv1.elements);

		//System.err.println("Shared elements: " + temp1);
		
		for (Feature t1: temp1) {
			//System.out.println("Feature t1=" + t1);
			int idx = temp2.indexOf(t1);
			Feature t2 = temp2.remove(idx);
			//System.out.println("Feature t2=" + t2);

			if (t1.value >= t2.value) {
			} else {
				t2.value = t1.value;
			}
			temp2.add(idx, t2);
		}
		return temp2;
	}


}
