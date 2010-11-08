package gnat.comparison;

import gnat.database.go.GOAccess;
import gnat.representation.ContextModel;
import gnat.representation.ContextVector;
import gnat.representation.Feature;
import gnat.representation.Gene;
import gnat.representation.GeneContextModel;
import gnat.representation.Text;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Used to compare a context model of a text against a context model of a gene.
 * It uses a GOTermSimilarity to compare GO codes for texts against GO codes for genes.
 *
 * */
@SuppressWarnings("serial")
public class CompareGeneAndTextContextModels extends CompareContextModels implements Serializable {

	/** */
	int verbosity = 1;

	private GOTermSimilarity goTermScorer;


	/**
	 * 	Initializes	this comparator with a GOAccess.
	 *
	 */
	public CompareGeneAndTextContextModels (GOAccess goAccess) {
		goTermScorer = new GOTermSimilarity(goAccess);
	}


	/**
	 * 	Initializes	this comparator with a GOAccess and a go2go object file (serialized Map<String, Integer>) of predefined go code similarities.
	 *
	 */
	public CompareGeneAndTextContextModels (GOAccess goAccess, String go2gofile) {
		goTermScorer = new GOTermSimilarity(goAccess);
		goTermScorer.loadGOTermDistances(go2gofile);
	}


	/**
	 * Checks if the two context vectors can be compared with one another.
	 * Decision is based on the <code>basic_type</code> of both vectors.
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	public boolean canCompare (ContextVector cv1, ContextVector cv2) {
		if (cv1.basic_type.equals(cv2.basic_type)) return true;

		String prefix = cv1.basic_type.split("_")[0];
		String foreign = cv2.basic_type.split("_")[0];

		if (prefix.startsWith("TERM")) {
			if (foreign.startsWith("TERM") || foreign.startsWith("TEXT"))
				return true;
		} else if (prefix.startsWith("TEXT")) {
			if (foreign.startsWith("TERM") || foreign.startsWith("TEXT"))
				return true;
		}

		return false;
	}


	/**
	 * Writes the current GO term distances to disk.
	 */
	public void finalizeIt () {
		goTermScorer.writeGOTermDistances();
	}


	/**
	 *	Returns the similarity of context vectoe cv1 with context vector cv2.
	 *  Both vectors represent go code vectors from context model c1 and c2 respectively.
	 *
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	public float getGOCodeSimilarity (ContextVector cv1, ContextVector cv2) {
		float sim = 0.0f;

		LinkedList<String> goCodes1 = cv1.getElementLabels();
		LinkedList<String> goCodes2 = cv2.getElementLabels();

		sim = goTermScorer.getGOSimilarity(goCodes1, goCodes2);

		return sim;
	}


	/**
	 * Returns all elements that are not common to both vectors.
	 *
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<Feature> disjointElements (ContextVector cv1, ContextVector cv2) {
		LinkedList<Feature> temp1 = (LinkedList<Feature>)cv1.elements.clone();
		LinkedList<Feature> temp2 = (LinkedList<Feature>)cv2.elements.clone();
		//System.out.println(temp1);
		//System.out.println(temp2);
		temp1.removeAll(cv2.elements);
		temp2.removeAll(cv1.elements);
		temp1.addAll(temp2);

		return temp1;
	}


	/**
	 * Calculates the cosine of the angle between both vectors as a
	 * measure of similarity.<br>
	 * cos0 = x * y / (||x|| * ||y||)
	 * <br>
	 * A return value of zero means that both vectors are orthogonal.
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	public float getCosineSimilarity (ContextVector cv1, ContextVector cv2) {
		return innerproduct(cv1, cv2) / (cv1.euclideanNorm() * cv2.euclideanNorm());
	}


	/**
	 * Returns the overlap between both context vectors (number of shared elements).
	 * Normalizes this absolute number with the length of the shortest of the two
	 * vectors.
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	public float getNormalizedOverlap (ContextVector cv1, ContextVector cv2) {
		float overlap = (float)getOverlap(cv1, cv2);
		float avglen = ( (float)cv1.length() + (float)cv2.length() ) / 2.0f;
		/*if (cv1.length() <= cv2.length())
			return overlap / (float)cv1.length();
		else
			return overlap / (float)cv2.length();*/
		return overlap / avglen;
	}


	/**
	 * Returns the overlap between (number of shared elements) of both context vectors.
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public int getOverlap (ContextVector cv1, ContextVector cv2) {
		LinkedList<String> temp = (LinkedList<String>)cv1.elements.clone();
		temp.retainAll(cv2.elements);
		return temp.size();
	}


	/**
	 *	Returns the similarity of a gene context vector to a text context vector.
	 *	Returns -1 if both vectors cannot be compared dtermined by method canCompare.
	 *
	 *
	 * @param geneCV
	 * @param textCV
	 * @return
	 */
	public float getSimilarity (ContextVector geneCV, ContextVector textCV) {
		if (!canCompare(geneCV, geneCV)) return -1.0f;

		if (verbosity > 3) {
			System.out.println("# comparing " + geneCV.specific_type + " with " + textCV.specific_type +
				" (" + geneCV.basic_type + "/" + textCV.basic_type + ")");
			System.out.println("# " + geneCV + "\n# " + textCV);
		}
		float sim = 0.0f;

		float normOverlap = getNormalizedOverlap(geneCV, textCV);
		if (normOverlap > sim) sim = normOverlap;
		//System.out.println("# norm="+normOverlap);

		float cosineSim = getCosineSimilarity(geneCV, textCV);
		if (cosineSim > sim) sim = cosineSim;
		//System.out.println("# coss="+cosineSim);

		float splittedCosineSim = 0.0f;
		if (geneCV.isTermType() && textCV.isTextType()) {
			ContextVector c = geneCV.splitElements();
			splittedCosineSim = CompareContextVectors.getCosineSimilarity(c, textCV);
		} else if (geneCV.isTextType() && textCV.isTermType()) {
			ContextVector c = textCV.splitElements();
			splittedCosineSim = CompareContextVectors.getCosineSimilarity(geneCV, c);
		}
		if (splittedCosineSim > sim) sim = splittedCosineSim;
		//System.out.println("# scos="+splittedCosineSim);

		if (verbosity > 3) {
			System.out.print("# norm.overlap=" + normOverlap + ", cos=" + cosineSim + ", splittedCos=" + splittedCosineSim);
		}

		if (geneCV.specific_type.equals(GeneContextModel.CONTEXTTYPE_GOCODES) &&
						textCV.specific_type.equals(GeneContextModel.CONTEXTTYPE_GOCODES)) {
			//System.err.println("# Getting GO term similarity...");
			float gosim = getGOCodeSimilarity(geneCV, textCV);//scoreMostSimilarGOCodes(geneCV, textCV);
			//System.out.println("# gosi="+gosim);
			if (gosim > sim) sim = gosim;
			if (verbosity > 3) {
				System.out.print(", go.sim="+gosim);
			}
		}

		if (verbosity > 3) {
			System.out.println(", max="+sim);
		}

		return sim;//cosineSim;
	}


	/**
	 *	Returns the similarity between a gene and a text based on their context models.
	 *
	 * @param gene
	 * @param text
	 * @return
	 */
	public float getSimilarity (Gene gene, Text text) {
		return getSimilarityScores(gene.getContextModel(), text.getContextModel()).get(0);
	}


	/**
	 *	Returns the similarity between a context model for gene and a context model for text.
	 */
	public float getSimilarity (ContextModel geneContextModel, ContextModel textContextModel) {
		return getSimilarityScores(geneContextModel, textContextModel).get(0);
	}


	/**
	 *	Returns a list of similarity scores between comparable vectors in the gene context model and in the text context model.
	 */
	public LinkedList<Float> getSimilarityScores (ContextModel geneContextModel, ContextModel textContextModel) {
		LinkedList<Float> allscores = new LinkedList<Float>();

		//
		LinkedList<Float> scores = new LinkedList<Float>();
		LinkedList<Float> weights = new LinkedList<Float>();

		//
		for (ContextVector current: geneContextModel.contexts) {
			for (ContextVector other: textContextModel.contexts) {

				if (canCompare(current, other)) {

					//System.out.print("# can compare: score=");

					float score = getSimilarity(current, other);
					scores.add(score);

					//System.out.println(score);

					float avgW = (current.getWeight() + other.getWeight()) / 2.0f;
					weights.add(avgW);

				}

			}
		}

		float sumAll = 0.0f;
		int countAll = 0;
		for (float score: scores) {
			if (score > 0.0f) {
				sumAll += score;
				countAll++;
			}
		}
		float avgAll = sumAll / (float)countAll;
		allscores.add(sumAll);
		allscores.add((float)countAll);
		allscores.add(avgAll);


		//
		float score = 0.0f;
		float weight = 0.0f;
		for (int s = 0; s < scores.size(); s++) {
			score += scores.get(s) + weights.get(s);
			weight += weights.get(s);
		}

		float finalscore = //0.0f;
			score / weight;

		allscores.add(0, finalscore);

		//return finalscore;
		return allscores;
	}


	/**
	 * Calculates the inner product of two vectors.
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	public static float innerproduct (ContextVector cv1, ContextVector cv2) {
		float product = 0.0f;

		LinkedList<Feature> shared = sharedElements(cv1, cv2);

		int idx1;
		int idx2;

		for (Feature s: shared) {
			idx1 = cv1.elements.indexOf(s);
			idx2 = cv2.elements.indexOf(s);
			product += cv1.elements.get(idx1).getValue() * cv2.elements.get(idx2).getValue();
		}

		return product;
	}


	/**
	 * Returns all elements common to both context vectors.
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static LinkedList<Feature> sharedElements (ContextVector cv1, ContextVector cv2) {
		LinkedList<Feature> temp1 = (LinkedList<Feature>)cv1.elements.clone();
		LinkedList<Feature> temp2 = (LinkedList<Feature>)cv2.elements.clone();
		temp1.retainAll(cv2.elements);
		temp2.retainAll(cv1.elements);

		for (Feature t1: temp1) {
			//System.out.println("Feature t1=" + t1);
			int idx = temp2.indexOf(t1);
			Feature t2 = temp2.remove(idx);
			//System.out.println("Feature t2=" + t2);

			if (t1.value >= t2.value) {
			} else {
				t2.value = t1.value;
			}
			temp2.add(idx, t2);
		}
		return temp2;
	}



}
