package gnat.representation;

/**
 * A FeatureVector representing a document, text, etc. (just everything that can be represented by features).
 *
 * @author Conrad Plake
 */
public interface FeatureVector {

	/** Adds a feature to this vector */
	public abstract void putFeature (Feature feature, Float value);

	/** Removes a feature from this vector */
	public abstract void removeFeature (Feature f);

	/** Checks if a feature occurs in this vector */
	public abstract boolean hasFeature (Feature feature);

	/** All features in this vector */
	public abstract Feature[] getFeatures();

	/** Value of the feature in this vector */
	public Float getValueOf (Feature f);

	/** Returns a label for this vector */
	public String getLabel();

	/** Length of this vector */
	public abstract float length();

	/** Dot product of this and another vector */
	public abstract float dotProduct (FeatureVector anotherFV);

	/** Distance between this and another vector */
	public abstract float cosineDistance (FeatureVector anotherFV);

	/** Copy of this feature vector*/
	public abstract FeatureVector copy();

}
