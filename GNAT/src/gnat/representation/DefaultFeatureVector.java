// ©2006 Transinsight GmbH - www.transinsight.com - All rights reserved.
package gnat.representation;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Represents a feature vector. This vector values are stored in a hash table.
 *
 * @author Conrad Plake, Joerg Hakenberg
 */
public class DefaultFeatureVector implements FeatureVector {


	/** Stores all features in a hash table*/
	protected Hashtable<Feature, Float> table;

	protected String label;

	/**
	 * Constructs a new and empty HashedFeatureVector
	 */
	public DefaultFeatureVector(String label){
		table = new Hashtable<Feature, Float>();
		this.label = label;
	}


	/**
	 * Constructs a new HashedFeatureVector and initializes it with the given features and corresponding values.
	 * @param features
	 * @param values
	 */
	public DefaultFeatureVector (Feature[] features, float[] values){
		table  = new Hashtable<Feature, Float>();
		int max = features.length;
		if (values.length < max) max = values.length;
		for (int i = 0; i < max; i++)
			table.put(features[i], new Float(values[i]));
	}


	/**
	 * Puts a new feature into this vector, and assigns the given value to it, representing its weight. Does not check for duplicates.
	 * @param feature
	 * @param value
	 */
	public void putFeature (Feature feature, Float value){
		table.put(feature, value);
	}


	/**
	 * Puts a new feature into this vector, and assigns the given value to it, representing its weight. Does not check for duplicates.
	 * @param feature
	 * @param value
	 */
  	public void putFeature (Feature feature, float value){
  		table.put(feature, new Float(value));
  	}


	/**
	 * Returns the value of the given feature
	 * @param feature
	 */
	public Float getValueOf(Feature feature){
		return (Float)table.get(feature);
	}


	/**
	 * Removes the given feature from this feature vector.
	 * @param feature
	 */
  	public void removeFeature(Feature feature){
  		table.remove(feature);
  	}


	/**
	 * Checks whether this vector contains the given feature.
	 * @param feature
	 * @return true if the feature occurs in this vector
	 */
  	public boolean hasFeature(Feature feature){
  		return table.containsKey(feature);
  	}


	/**
	 * Returns an array of all features in this vector. Uses the TreeSet iterator.
	 * @return Feature[]
	 */
	public Feature[] getFeatures(){
		Feature[] features = new Feature[table.size()];
		int i=0;
		for(Enumeration<Feature> enm = table.keys();enm.hasMoreElements();){
			features[i++] = (Feature)enm.nextElement();
		}
		return features;
	}


	/**
	 * Returns the size of the feature vector, i.e. the number of features
	 * @return count
	 */
	public int size(){
		return table.size();
	}


	/**
	 * Returns the current length of this feature vector: sqrt( sum( x<sub>i</sub><sup>2</sup> ) ).
	 * @return length
	 */
	public float length () {
		float len = 0;
		for (Enumeration<Float> enm = table.elements();enm.hasMoreElements();) {
			len += Math.pow(((Float)enm.nextElement()).floatValue(), 2);
		}
		return (float)Math.sqrt(len);
	}


	/**
	 * Calculates the dot product of this feature vector with another one.
	 * @return float
	 */
	public float dotProduct(FeatureVector anotherFV){
		float result = 0;
		for(Enumeration<Feature> enm = table.keys();enm.hasMoreElements();){
			Feature f = (Feature)enm.nextElement();
			Float val2 = anotherFV.getValueOf(f);
			if(val2!=null){	// shared feature
				float val1 = getValueOf(f).floatValue();
				result += (val1 * val2.floatValue());
			}
		}
		return result;
	}


  	/**
	 * Returns a string representation of this feature vector: <tt>feature:value,feature:value,feature:value,..</tt>.
	 * @return string
	 */
  	public String toString(){
  		StringBuffer buffer = new StringBuffer();
  		for (Enumeration<Feature> enm = table.keys(); enm.hasMoreElements();) {
  			Feature f = (Feature)enm.nextElement();
  			Float value = (Float)table.get(f);
  			buffer.append(f.toString());
  			buffer.append(":");
  			buffer.append(value.toString());
  			if (enm.hasMoreElements()) buffer.append(",");
  		}
  		return buffer.toString();
  	}


	/**
	 * Returns a copy of this feature vector.
	 * @return FeatureVector
	 */
	public FeatureVector copy(){
		DefaultFeatureVector copy = new DefaultFeatureVector(label);
		for(Enumeration<Feature> enm = table.keys();enm.hasMoreElements();){
			Feature feature = (Feature)enm.nextElement();
			float value = ((Float)table.get(feature)).floatValue();
			copy.putFeature(feature, value);
		}
		return copy;
	}

	/***/
	public float cosineDistance(FeatureVector anotherFeatureVector){
		return (1 - dotProduct(anotherFeatureVector) / (length()*anotherFeatureVector.length()));
	}

	public String getLabel()
    {
    	return label;
    }


	public void setLabel(String label)
    {
    	this.label = label;
    }


	/**
	 *
	 * */
	public static void main(String[] args){
		Feature f1 = new Feature("x",1.0f);
		Feature f2 = new Feature("y",1.0f);
		Feature f3 = new Feature("z",1.0f);

		DefaultFeatureVector v1 = new DefaultFeatureVector("v1");
		v1.putFeature(f1, 1.0f);
		v1.putFeature(f2, 2.0f);
		//v1.putFeature(f3, 0.0f);

		DefaultFeatureVector v2 = new DefaultFeatureVector("v2");
		v2.putFeature(f1, 1.0f);
		v2.putFeature(f2, 1.0f);
		v2.putFeature(f3, 0.5f);

		System.out.println("len v1 = "+v1.length());
		System.out.println("len v2 = "+v2.length());

		float dotProduct = v1.dotProduct(v2);
		System.out.println("dotProd = "+dotProduct);

		float cosineDist = v1.cosineDistance(v2);
		System.out.println("cosineDist = "+cosineDist);
	}
}
