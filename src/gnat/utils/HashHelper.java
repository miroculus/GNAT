package gnat.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides some methods to handle Hashes.
 * 
 * 
 * @author Joerg Hakenberg
 *
 */

public class HashHelper {

	
	/**
	 * Takes a HashMap&lt;String, Float&gt; and sorts the keys according to
	 * their respective values. Returns sorted keys in a List. 
	 * Ordering: highest value first.
	 * @param list
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<String> sortByValue (Map<String, Float> map) {
		Set<Map.Entry<String, Float>> set = map.entrySet();
		Map.Entry[] entries = (Map.Entry[]) set.toArray(new Map.Entry[set.size()]);
		Arrays.sort(entries, new Comparator() {
			public int compare(Object o1, Object o2) {
				Object v1 = ((Map.Entry) o1).getValue();
				Object v2 = ((Map.Entry) o2).getValue();
				return ((Comparable) v2).compareTo(v1);
			}
		});
		
		List<String> result = new LinkedList<String>();
		for (int e = 0; e < entries.length; e++)
			result.add((String)entries[e].getKey());
		
		return result;
	}
	
	
	/**
	 * Takes a HashMap&lt;String, Float&gt; and sorts the keys according to
	 * their respective values. Returns sorted keys in a Vector. 
	 * Ordering: highest value first.
	 * @param list
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<String> sortByIntegerValue (Map<String, Integer> map) {
		Set<Map.Entry<String, Integer>> set = map.entrySet();
		Map.Entry[] entries = (Map.Entry[]) set.toArray(new Map.Entry[set.size()]);
		Arrays.sort(entries, new Comparator() {
			public int compare(Object o1, Object o2) {
				Object v1 = ((Map.Entry) o1).getValue();
				Object v2 = ((Map.Entry) o2).getValue();
				return ((Comparable) v2).compareTo(v1);
			}
		});
		
		List<String> result = new LinkedList<String>();
		for (int e = 0; e < entries.length; e++)
			result.add((String)entries[e].getKey());
		
		return result;
	}
	
	
	/**
	 * 
	 * @param map
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<Integer> sortByKeys (Set<Integer> keys) {
		List<Integer> result = new LinkedList<Integer>();
		
		Iterator<Integer> it = keys.iterator();
		while (it.hasNext())
			result.add(it.next());
		
		Collections.sort(result, new Comparator() {
			public int compare (Object o1, Object o2) {
				int v1 = (Integer)o1;
				int v2 = (Integer)o2;
				return v1 - v2;
			}
			
		});
		
		return result;
	}
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List[] sortByValueWithValues (Map<String, Float> map) {
		Set<Map.Entry<String, Float>> set = map.entrySet();
		Map.Entry[] entries = (Map.Entry[]) set.toArray(new Map.Entry[set.size()]);
		Arrays.sort(entries, new Comparator() {
			public int compare(Object o1, Object o2) {
				Object v1 = ((Map.Entry) o1).getValue();
				Object v2 = ((Map.Entry) o2).getValue();
				return ((Comparable) v2).compareTo(v1);
			}
		});
		
		List<String> keys = new LinkedList<String>();
		List<Float> values = new LinkedList<Float>();
		for (int e = 0; e < entries.length; e++) {
			keys.add((String)entries[e].getKey());
			values.add(map.get((String)entries[e].getKey()));
		}
		
		return new List[]{keys, values};
	}
	
}
