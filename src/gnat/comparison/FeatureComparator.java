package gnat.comparison;

import java.util.Comparator;

import gnat.representation.Feature;

/**
 * A Comparator for float values. Used e.g. to sort lists numerically instead of lexically.
 *
 * <pre>
 *
 * </pre>
 *
 * @author Joerg Hakenberg
 *
 */
public class FeatureComparator implements Comparator<Feature> {

	/**
	 * Compares two Features and returns either a negative value, zero, or a positive value.
	 * Negative return values indicate that the Feature o1 should be sorted before o2,
	 * zero indicates that both are equal,
	 * and positive values indicate that o1 should be sorted after o2.
	 *
	 * @param o1 - first Feature
	 * @param o2 - second Feature
	 * @return negative or positive value or zero
	 */
	public int compare (Feature o1, Feature o2) {
		Float result = o1.compareTo(o2);
		return result.intValue();
	}

}