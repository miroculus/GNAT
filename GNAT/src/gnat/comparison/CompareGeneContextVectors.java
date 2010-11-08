package gnat.comparison;

import gnat.database.go.GOAccess;
import gnat.representation.ContextVector;
import gnat.representation.GeneContextModel;

import java.util.LinkedList;



@SuppressWarnings("serial")
public class CompareGeneContextVectors extends CompareContextVectors {

	private GOTermSimilarity goTermScorer;

	/**
	 *
	 *
	 */
	public CompareGeneContextVectors (GOAccess goAccess) {
		goTermScorer = new GOTermSimilarity(goAccess);
	}


	/**
	 *
	 *
	 */
	public CompareGeneContextVectors (GOAccess goAccess, String go2gofile) {
		// load existing go2go hash from object file

		goTermScorer = new GOTermSimilarity(goAccess);
		goTermScorer.loadGOTermDistances(go2gofile);
	}


	/**
	 * Writes the current GO term distances to disk.
	 */
	public void finalizeIt () {
		goTermScorer.writeGOTermDistances();
	}


	/**
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
	 *
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	@Override
	public float getSimilarity (ContextVector cv1, ContextVector cv2) {
		if (!canCompare(cv1, cv2)) return -1.0f;

		if (verbosity > 3) {
			System.out.println("# comparing " + cv1.specific_type + " with " + cv2.specific_type +
				" (" + cv1.basic_type + "/" + cv2.basic_type + ")");
			System.out.println("# " + cv1 + "\n# " + cv2);
		}
		float sim = 0.0f;

		float normOverlap = getNormalizedOverlap(cv1, cv2);
		if (normOverlap > sim) sim = normOverlap;

		float cosineSim = getCosineSimilarity(cv1, cv2);
		if (cosineSim > sim) sim = cosineSim;

		float splittedCosineSim = 0.0f;
		if (cv1.isTermType() && cv2.isTextType()) {
			ContextVector c = cv1.splitElements();
			splittedCosineSim = CompareContextVectors.getCosineSimilarity(c, cv2);
		} else if (cv1.isTextType() && cv2.isTermType()) {
			ContextVector c = cv2.splitElements();
			splittedCosineSim = CompareContextVectors.getCosineSimilarity(cv1, c);
		}
		if (splittedCosineSim > sim) sim = splittedCosineSim;

		if (verbosity > 3) {
			System.out.print("# norm.overlap=" + normOverlap + ", cos=" + cosineSim + ", splittedCos=" + splittedCosineSim);
		}

		if (cv1.specific_type.equals(GeneContextModel.CONTEXTTYPE_GOCODES) &&
				cv2.specific_type.equals(GeneContextModel.CONTEXTTYPE_GOCODES)) {
			//System.err.println("# Getting GO term similarity...");
			float gosim = getGOCodeSimilarity(cv1, cv2);//scoreMostSimilarGOCodes(cv1, cv2);
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
	 *
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	public float scoreMostSimilarGOCodes (ContextVector cv1, ContextVector cv2) {
		float sim = 0.0f;

		LinkedList<String> goCodes1 = cv1.getElementLabels();
		LinkedList<String> goCodes2 = cv2.getElementLabels();

		sim = GOTermSimilarity.scoreMostSimilarGOCodes(goCodes1, goCodes2);

		return sim;
	}



}
