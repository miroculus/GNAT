package gnat.representation;

import java.io.Serializable;

/**
 *	A feature representing an attribute, property, or predicate etc.
 *
 * @author Joerg Hakenberg
 *
 */

@SuppressWarnings("serial")
public class Feature implements Serializable {

	/** */
	public String label = "";

	/** */
	public float value = 0.0f;

	/** */
	public int index = -1;


	/**
	 *
	 * @param label
	 * @param value
	 */
	public Feature (String label, float value) {
		this.label = label;
		this.value = value;
	}


	/**
	 *
	 * @param label
	 * @param value
	 * @param index
	 */
	public Feature (String label, float value, int index) {
		this(label, value);
		this.index = index;
	}


	/**
	 * Comparison is based on lexicographic comparison of both labels. Returns a negative
	 * value if this feature's labels lexicographically precedes the other vector's label,
	 * zero if both labels are the same, a positive value if the other vector's label
	 * precedes this vector's label.
	 * @param f
	 * @return
	 */
//	@Override
	public float compareTo (Feature f) {
		//System.out.println("compareTo called");
		//if (this.label.equals(f.label))
		//	return this.value - f.value;
		//else
			return this.label.compareTo(f.label);
	}


	/**
	 * Equality is based on string equality of both labels.
	 * @param o
	 * @return
	 */
	@Override
	public boolean equals (Object o) {
		//System.out.println("equals called");
		try {
			Feature f = (Feature)o;
			return (this.label.equals(f.label));
		} catch (ClassCastException cce) {
			return false;
		}
	}


	/**
	 *
	 * @return
	 */
	public String getLabel () {
		return label;
	}


	public float getValue () {
		return value;
	}


	/**
	 *
	 */
	@Override
	public int hashCode () {
		//System.out.println("hashCode called");
		return this.label.hashCode();
	}


	/**
	 *
	 * @param index
	 */
	public void setIndex (int index) {
		this.index = index;
	}


	/**
	 *
	 */
	public String toString () {
		return label + ":" + value;
	}

}
