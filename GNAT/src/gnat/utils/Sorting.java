package gnat.utils;

import gnat.representation.RecognizedEntity;

import java.util.Comparator;

public class Sorting {

	/**
	 * A comparison function that provides a total ordering {@link RecognizedEntity RecognizedEntities}.
	 * 
	 *
	 */
	public static class RecognizedEntitySorter implements Comparator<RecognizedEntity> {
		@Override public int compare (RecognizedEntity re1, RecognizedEntity re2) {
			if (re1.getBegin() != re2.getBegin()) return re1.getBegin() - re2.getBegin();
			if (re1.getEnd()     != re2.getEnd()) return re1.getEnd()   - re2.getEnd();
			return 0;
		}
	}

}

