package gnat.comparison;

import java.util.Comparator;

/**
 * A Comparator for float values. Used e.g. to sort Lists numerically instead of lexically.
 * 
 * <pre>
 * Hashtable hash = new Hashtable();
 * hash.add...
 * 
 * Vector keys = new Vector(hash.keySet());
 * FloatComparator fc = new FloatComparator();
 * Collections.sort(keys, fc);
 * 
 * for (Enumeration e = keys.elements(); e.hasMoreElements();) {
 *     Float key = (Float)e.nextElement();
 *     Object o = hash.get(key);
 * }
 * </pre>
 * 
 * @author Joerg Hakenberg
 *
 */
public class FloatComparator implements Comparator<Float> {

	/**
	 * Compares to numerical values and returns either a negative value, zero, or a positive value. Negative return values
	 * indicate that the float value of o1 is less than that of o2, zero indicates that both are equal, and positive values indicate
	 * that o1 is greater than o2.<br>
	 * <br>
	 * In particular, it returns Float.intValue() of <tt>o1 - o2</tt>.<br>
	 * 
	 * @param o1 - first integer value as a String
	 * @param o2 - second integer value as a String
	 * @return negative or positive value or zero 
	 */
	public int compare (Float o1, Float o2) {
		float o1f = ((Float)o1).floatValue();
		float o2f = ((Float)o2).floatValue();
		int result = (new Float(o1f - o2f)).intValue();
		return result; //(new Float((float)Float.parseFloat((String)o1) - (float)Float.parseFloat((String)o2))).intValue(); 
	}
	
}
