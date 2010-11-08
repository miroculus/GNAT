package gnat.utils;

import gnat.comparison.FloatComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;


public class ArrayHelper {

	/**
	 * Adds an entry to a String array and returns the new array. Adds the
	 * entry to the end of the array. Use addEntry(String, String[]) to
	 * add the entry to the beginning.
	 * @param array
	 * @param entry
	 * @return
	 */
	public static String[] addEntry (String entry, String[] array) {
		String[] result = new String[array.length + 1];
		result[0] = entry;
		for (int i = 0; i < array.length; i++)
			result[i+1] = array[i];
		return result;
	}


	/**
	 * Adds an entry to a String array and returns the new array. Adds the
	 * entry to the end of the array. Use <tt>addEntry(String, String[])</tt>
	 * to add the entry to the beginning.
	 * @param array
	 * @param entry
	 * @return
	 */
	public static String[] addEntry (String[] array, String entry) {
		String[] result = new String[array.length + 1];
		for (int i = 0; i < array.length; i++)
			result[i] = array[i];
		result[array.length] = entry;
		return result;
	}


	/**
	 * Checks whether the array contains the given element.
	 * @param array
	 * @param element
	 * @return
	 */
	public static boolean containsElement (String[] array, String element) {
		for (int i = 0; i < array.length; i++)
			if (array[i].equals(element)) return true;
		return false;
	}



	/**
	 * Transforms a Vector of String arrays (Vector&lt;String[]&gt;) into a
	 * single String array. All Strings in the Vector appear in the same order
	 * in the new String array.
	 * @param input
	 * @return flat String array
	 */
	public static String[] flattenVectorOfStringArrays (Vector<String[]> input) {
		Vector<String> inter = new Vector<String>();
		for (int d = 0; d < input.size(); d++) {
			String[] dimension = input.get(d);
			if (dimension != null && dimension.length > 0)
				for (int s = 0; s < dimension.length; s++)
					inter.add(dimension[s]);
		}

		String[] result = new String[inter.size()];
		for (int s = 0; s < inter.size(); s++) {
			result[s] = inter.get(s);
		}

		return result;
	}


	/**
	 * Puts all elements of a list into a String array.
	 * Convenience method.
	 * @param vector
	 * @return
	 */
	public static String[] list2StringArray (List<String> list) {
		//String[] results = new String[list.size()];
		//for (int i = 0; i < list.size(); i++)
		//	results[i] = list.get(i);
		//return results;
		return list.toArray(new String[list.size()]);
	}


	/**
	 * Joins the entries in a String array into a single String. All entries get
	 * separated by the specified delimiter.
	 * @param array
	 * @param delimiter
	 * @return joined array
	 */
	public static String joinStringArray (String[] array, String delimiter) {
		StringBuilder result = new StringBuilder();
		if (array.length >= 1)
			result.append(array[0]);
		for (int i = 1; i < array.length; i++) {
			result.append(delimiter);
			result.append(array[i]);
		}
		return result.toString();
	}
	
	
	/**
	 * Joins the entries in an array into a single String. All entries get
	 * separated by the specified delimiter.
	 * @param array
	 * @param delimiter
	 * @return joined array
	 */
	public static <T> String joinArray (T[] array, String delimiter) {
		StringBuilder result = new StringBuilder();
		if (array.length >= 1)
			result.append(array[0]);
		for (int i = 1; i < array.length; i++) {
			result.append(delimiter);
			result.append(array[i]);
		}
		return result.toString();
	}


	/**
	 * Combines the elements of two String arrays into a single String array.
	 * @param one
	 * @param two
	 * @return joined String array
	 */
	public static String[] joinTwoStringArraysIntoOne (String[] one, String[] two) {
		if (one == null) return two;
		if (two == null) return one;
		String[] result = new String[one.length + two.length];
		for (int i = 0; i < one.length; i++)
			result[i] = one[i];
		for (int i = 0; i < two.length; i++)
			result[i + one.length] = two[i];
		return result;
	}


	/**
	 * Returns the number of elements the larger of the two arrays contains.
	 * @param one
	 * @param two
	 * @return
	 */
	public static int maxLengthOfTwoArrays (String[] one, String[] two) {
		if (one.length > two.length) return one.length;
		return two.length;
	}


	/**
	 * Returns the median of this float array.
	 * @param array
	 * @return
	 */
	public static float median (float[] array) {
		/*float[] checkf = new float[]{26.1f, 25.6f, 25.7f, 25.2f, 25.0f};
		float median = ArrayHelper.median(checkf);
		System.out.println("Median is " + median);
		System.exit(0);*/

		Vector<Float> values = new Vector<Float>();
		for (float val : array)
			values.add(val);
		FloatComparator fc = new FloatComparator();
		Collections.<Float>sort(values, fc);
		Collections.reverse(values);
		int position = (array.length + 1) / 2;
		float median = values.get(position).floatValue();
		return median;
	}


	/**
	 * Returns the number of elements the smaller of the two arrays contains.
	 * @param one
	 * @param two
	 * @return
	 */
	public static int minLengthOfTwoArrays (String[] one, String[] two) {
		if (one.length < two.length) return one.length;
		return two.length;
	}


	/**
	 * Removes duplicate entries from a String array. Ordering might differ in the
	 * result!
	 * @param values
	 * @return array without duplicate entries
	 */
	public static String[] removeDuplicates (String[] values) {
		Set<String> set = new LinkedHashSet<String>(); // LinkedHashSet keeps the order, which is not strictly necessary acc. the contract
		for (String value: values)
			set.add(value);
		
		String[] result = new String[set.size()];
		int index = 0;
		for (String value: set) {
			result[index] = value;
			index++;
		}

		return result;
	}


	/**
	 * Turns a set of strings into a string array.
	 * @param set
	 * @return
	 */
	public static String[] set2StringArray (Set<String> set) {
		String[] result = new String[set.size()];
		Iterator<String> it = set.iterator();
		int r = 0;
		while (it.hasNext()) {
			result[r] = (String)it.next();
			r++;
		}
		return result;
	}

	/**
	 * Turns a set of integers into a string array.
	 * @param set
	 * @return
	 */
	public static int[] set2IntegerArray (Set<Integer> set) {
		int[] result = new int[set.size()];
		int r = 0;
		for (int i : set) {
			result[r] = i;
			r++;
		}
		return result;
	}


	/**
	 * Returns an array of all elements that both arrays have in common.
	 * @param one
	 * @param two
	 * @return shared elements
	 */
	public static String[] sharedElements (String[] one, String[] two) {
		List<String> elements = new LinkedList<String>();
		for (int o = 0; o < one.length; o++) {
			for (int t = 0; t < two.length; t++) {
				if (one[o].equals(two[t]))
					elements.add(one[o]);
			}
		}
		return list2StringArray(elements);
	}


	/**
	 * Returns an array of all elements that both arrays have in common.
	 * Removes duplicates from the resulting array.
	 * @param one
	 * @param two
	 * @return shared elements
	 */
	public static String[] sharedElementsNoDuplicates (String[] one, String[] two) {
		String[] shared = sharedElements(one, two);
		shared = removeDuplicates(shared);
		return shared;
	}


	/**
	 * Checks if two String arrays share at least one element
	 * @param one
	 * @param two
	 * @return true if at least one element occurs in both arrays
	 */
	public static boolean shareElement (String[] one, String[] two) {
		for (int o = 0; o < one.length; o++) {
			for (int t = 0; t < two.length; t++) {
				if (one[o].equals(two[t]))
					return true;
			}
		}
		return false;
	}


	/**
	 * Checks if two String arrays share some elements. Returns the number of
	 * elements both arrays have in common.
	 * @param one
	 * @param two
	 * @return number of identical elements
	 */
	public static int shareElements (String[] one, String[] two) {
		int result = 0;
		for (int o = 0; o < one.length; o++) {
			for (int t = 0; t < two.length; t++) {
				if (one[o].equals(two[t]))
					result++;
			}
		}
		return result;
	}


	/**
	 * Checks if two String arrays share some elements. Returns the number of
	 * elements both arrays have in common. Removes duplicate shared elements!
	 * @param one
	 * @param two
	 * @return number of identical elements
	 */
	public static int shareElementsNoDuplicates (String[] one, String[] two) {
		String[] shared = sharedElementsNoDuplicates(one, two);
		return shared.length;
	}


	/**
	 *
	 * */
	public static String[] sortAlphabetically (String[] array) {
		List<String> inter = stringArray2List(array);
		Collections.sort(inter);
		return list2StringArray(inter);
	}


	/**
	 * Converts an array of strings into a sorted set representation. Because the
	 * result is a set, duplicates are not present anymore.
	 * @param input
	 * @return
	 */
	public static TreeSet<String> stringArray2TreeSet (String[] input) {
		TreeSet<String> result = new TreeSet<String>();
		for (int i = 0; i < input.length; i++)
			result.add(input[i]);
		return result;
	}

	
	/**
	 *
	 * @param input
	 * @return
	 */
	public static HashSet<String> stringArray2HashSet (String[] input) {
		HashSet<String> result = new HashSet<String>();
		for (int i = 0; i < input.length; i++)
			result.add(input[i]);
		return result;
	}


	/**
	 * Converts an array of strings into a vector representation. Same ordering
	 * remains.
	 * @param input
	 * @return
	 */
	public static List<String> stringArray2List (String[] input) {
		List<String> result = new LinkedList<String>();
		for (int i = 0; i < input.length; i++)
			result.add(input[i]);
		return result;
	}

	/**
	 * Converts an array of strings into a array list representation. Same ordering
	 * remains.
	 * @param input
	 * @return
	 */
	public static List<String> stringArray2ArrayList (String[] input) {
		List<String> result = new ArrayList<String>(input.length);
		for (int i = 0; i < input.length; i++)
			result.add(input[i]);
		return result;
	}


	/**
	 *
	 * @param array
	 * @return
	 */
	public static TreeSet<String> toTreeSet (String[] array) {
		TreeSet<String> result = new TreeSet<String>();
		for (String element : array)
			result.add(element);
		return result;
	}


	/**
	 * Removes all elements that get trimmed to a length of zero.
	 * */
	public static String[] trimArray(String[] tokens){
		List<String> tokenList = new ArrayList<String>(tokens.length);
        for (String string : tokens) {
        	string = string.trim();
            if(string.length()>0){
            	tokenList.add(string);
            }
        }
        return ArrayHelper.list2StringArray(tokenList);
	}

	/**
	 * Removes all elements that get trimmed to a length of zero or occur in the given set of stopwords.
	 * */
	public static String[] trimArray(String[] tokens, Set<String> stopwords){
		List<String> tokenList = new ArrayList<String>(tokens.length);
        for (String string : tokens) {
        	string = string.trim();
            if(string.length()>0 && !stopwords.contains(string)){
            	tokenList.add(string);
            }
        }
        return ArrayHelper.list2StringArray(tokenList);
	}

}
