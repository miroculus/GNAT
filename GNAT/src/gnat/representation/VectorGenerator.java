/**
 *
 */
package gnat.representation;

import gnat.utils.ArrayHelper;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;


/**
 * Generates feature vector instances from lists of tokens to represent texts.
 *
 *
 * @author cplake
 *
 */
public class VectorGenerator {

	Map<String, Integer> overallFrequencies;

	/** Contains all global features */
	protected Hashtable<String, Feature> globalFeatures;

	/** Switch between training and testing mode */
	protected boolean trainMode = true;


	/**
	 * Constructs a new generator
	 * @throws Exception
	 */
	public VectorGenerator(){
		globalFeatures = new Hashtable<String, Feature>();
		overallFrequencies = new HashMap<String, Integer>();
	}

	/**
	 *
	 * Returns the max. frequency that was seen for this token.
	 * */
	public int getOverallFrequency(String token){
		Integer frequency = overallFrequencies.get(token);
		if(frequency==null){
			frequency = new Integer(1);
		}
		return frequency;
	}

	/**
	 * Sets the max. frequency that was seen for every token in the list.
	 * */
	public void initializeOverallFrequencies(List<String> tokenList){
		initializeOverallFrequencies(ArrayHelper.list2StringArray(tokenList));
	}

	/**
	 * Sets the max. frequency that was seen for every token in the array.
	 * */
	public void initializeOverallFrequencies(String[] tokens){
		for (int i = 0; i < tokens.length; i++) {
			String token = tokens[i];
			//token = BiocreativeHelper.stemmer.stemWords(new String[]{token})[0];
			Integer frequency = overallFrequencies.get(token);
			if(frequency==null){
				frequency = new Integer(0);
			}
			frequency++;
			overallFrequencies.put(token, frequency);
		}
	}


	/**
	 * Returns an array of all global features
	 * @return Feature[]
	 */
	public Feature[] features() {
		Feature[] features = new Feature[globalFeatures.size()];
		Enumeration<Feature> enm = globalFeatures.elements();
		for (int i = 0; enm.hasMoreElements(); i++) {
			features[i] = (Feature) enm.nextElement();
		}
		return features;
	}


	/**
	 * Sets the mode (training or testing) - true for training, false for testing
	 * @param mode
	 */
	public void setTrainMode (boolean mode) {
		trainMode = mode;
	}

	/**
	 * Generates an array of feature vectors from a two-dimensional array of documents: documents vs. tokens<br>
	 * <tt>document.length</tt> should be the number of documents.
	 * @return FeatureVector[]
	 */
	public FeatureVector[] generateFeatureVectors (String[][] documents, String[] labels) {
		int nod = documents.length; // number of documents
		FeatureVector[] vectors = new DefaultFeatureVector[nod];
		for (int i = 0; i < nod; i++) {
			vectors[i] = generateFeatureVector(documents[i], labels[i]);
		}
		return vectors;
	}


	/**
	 * Generates a feature vector from a list of tokens.
	 * @param tokens
	 */
	public FeatureVector generateFeatureVector (List<String> tokens, String label) {
		String[] tokenArray = new String[tokens.size()];
		for(int i=0;i<tokenArray.length;i++){
			tokenArray[i] = tokens.get(i);
		}
		return generateFeatureVector(tokenArray, label);
	}

	/**
	 * Generates a feature vector from an array of tokens.
	 * @param tokens
	 */
	public FeatureVector generateFeatureVector (String[] tokens, String label) {
	    FeatureVector vector = new DefaultFeatureVector(label);
		for (int j = 0; j < tokens.length; j++) {
			String token = tokens[j];
			//token = BiocreativeHelper.stemmer.stemWords(new String[]{token})[0];
			Feature f = (Feature) globalFeatures.get(token);
			if (f == null) {
				if (trainMode) {
					f = new Feature(token, 1.0f, globalFeatures.size() + 1);
					globalFeatures.put(token, f);
				} else {
					continue; // token not considered in testing mode
				}
			}
			Float val = vector.getValueOf(f);
			if (val == null) {
				val = new Float(0);
			}
			val++;
			vector.putFeature(f, val);
		}

		// adjust feature values from TF to TF*IDF
		Feature[] features = vector.getFeatures();
		for (int i = 0; i < features.length; i++) {
			Feature feature = features[i];
			String token = feature.getLabel();
			Float value = vector.getValueOf(feature);
			float totalFrequency = (float)this.getOverallFrequency(token);
			vector.putFeature(feature, value / totalFrequency);
		}
		return vector;
	}

	/***/
	public static float distance(FeatureVector fv1, FeatureVector fv2){
		return (1 - fv1.dotProduct(fv2) / (fv1.length()*fv2.length()));
	}

}
