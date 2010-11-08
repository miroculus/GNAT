package gnat.comparison;

import gnat.representation.ContextModel;

/**
 * Base class to compare to context models with each other. Extend this class and override method getSimilarity.
 * */
public class CompareContextModels {// implements Comparer {

	/**
	 * OVERRIDE!
	 * @param cm1
	 * @param cm2
	 * @return
	 */
	public float getSimilarity (ContextModel cm1, ContextModel cm2) {
		return 0.0f;
	}

}
