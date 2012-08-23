package gnat.client.nei;

import gnat.ConstantsNei;
import gnat.ISGNProperties;
import gnat.comparison.CompareContextVectors;
import gnat.comparison.GOTermSimilarity;
import gnat.database.go.GOAccess;
import gnat.evaluation.TestCase;
import gnat.representation.ContextModel;
import gnat.representation.ContextVector;
import gnat.representation.Gene;
import gnat.representation.GeneContextModel;
import gnat.representation.Text;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Scores one or more genes given their occurrence in a particular piece of text. Its main use
 * is to rank a list of gene candidates, depending on which of them is more likely to be referred
 * to in the text. 
 * <br><br>
 * Scoring takes into consideration knowledge about each gene, such as
 * <ul>
 * <li>Gene Ontology terms associated with it,
 * <li>sequence features such as length, domains, mutations,
 * <li>protein mass
 * <li>origin: species and chromosomal location,
 * <li>protein interaction partners,
 * <li>diseases associated with a gene,
 * <li>etc.
 * </ul>
 * These information are taken from GO/GOA, EntrezGene, UniProt, and other resources. For each gene,
 * each type of information is sought in the given text and compared to the known data, resulting in
 * a score and ranking per gene, depending on which gene's information most overlaps with the given
 * text.
 *
 * @author Joerg
 */

public class GenePubMedScorer {

	private GOTermSimilarity goTermScorer;

	public int verbosity = 1;

	/** The number of sub-scores we currently calculate:<br>
	 * GO-term, GeneRIF, tissue spec, disease, EntrezGene summary,
	 * molecular weight, amino acids, chr. location, mutations,
	 * function, keywords, PubMed, protein domains. */
	public final int NUMBER_OF_SCORES = ConstantsNei.NUMBER_OF_SCORES;

	/** Output format for genes and their scores; 1: plain text, 2: SVM. */
	public int DEBUG_OUTPUT = 2;

	/** */
	Map <String, LinkedList<Float>> scoreCache = new HashMap <String, LinkedList<Float>>();

	/**
	 * Constructs a new GenePubMedScorer with an access to the GO
	 * term distance database and a file that contains previously
	 * calculated GO term distances.
	 * @param goAccess
	 * @param go2gofile
	 */
	public GenePubMedScorer (GOAccess goAccess, String go2gofile) {
		goTermScorer = new GOTermSimilarity(goAccess);
		goTermScorer.loadGOTermDistances(go2gofile);
		goTermScorer.verbosity = this.verbosity-1;
		//goTermScorer.useDatabase = false;
		//if (!goTermScorer.useDatabase)
		//	System.out.println("#GenePubMedScorer:: Warning: not using the LCA-database to calculate new GO-term distances.");
	}


	/**
	 * Call when not using the Scorer anymore. Closes the DB connection
	 * and writes new GO distances to the file <tt>go2gofile</tt>.
	 */
	public void finalize () {
		goTermScorer.writeGOTermDistances();
		goTermScorer.closeDBConnection();
	}

	/**
	 * Convenience method. Returns true if cv is not null.
	 * @return
	 */
	public boolean checkContextVector (ContextVector cv) {
		return (cv != null);
	}


	/**
	 * Convenience method. Returns true if both vectors check to true.
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	public boolean checkContextVectors (ContextVector cv1, ContextVector cv2) {
		return (checkContextVector(cv1) && checkContextVector(cv2));
	}


	/**
	 * Decides on a single Gene that best fits the given text. Decision is
	 * based on score (average and number of scored elements [GO terms,
	 * diseases, GeneRIFS, etc.]).
	 * @param genes
	 * @param text
	 * @return
	 */
	public Gene disambiguate (Collection<Gene> genes, Text text) {
		LinkedList<Gene> orderedGenes = new LinkedList<Gene>();
		orderedGenes.addAll(genes);

		//		LinkedList<HashMap<String, Float>> orderedAllScores = new LinkedList<HashMap<String, Float>>();

		LinkedList<Float> orderedScores = new LinkedList<Float>();

		int maxIndex = 0;
		float maxScore = -1.0f;
		for (int i = 0; i < orderedScores.size(); i++) {
			float score = orderedScores.get(i);
			if (score > maxScore) {
				maxScore = score;
				maxIndex = i;
			}
		}

		return orderedGenes.get(maxIndex);
	}


	/**
	 *	Returns the cosine similarity between both vectors.
	 *
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	public float getCosineSimilarity (ContextVector cv1, ContextVector cv2) {
		float sim = -1.0f;

		if (checkContextVectors(cv1, cv2))
			sim = CompareContextVectors.getCosineSimilarity(cv1, cv2);

		return sim;
	}


	/**
	 *
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	public float getNormalizedOverlapSimilarity (ContextVector cv1, ContextVector cv2) {
		float sim = -1.0f;

		//if (!checkContextVector(cv2))
		//	System.out.println("No such CV2");
		//else
		//	System.out.println("There is a CV2");

		if (checkContextVectors(cv1, cv2))
			sim = CompareContextVectors.getNormalizedOverlap(cv1, cv2);


		return sim;
	}


	/**
	 *
	 * @param cv1s
	 * @param cv2
	 * @return
	 */
	public float getNormalizedOverlapSimilarity (Collection<ContextVector> cv1s, ContextVector cv2) {
		float sim = -1.0f;

		for (ContextVector cv1: cv1s) {
			float newsim = getNormalizedOverlapSimilarity(cv1, cv2);
			if (newsim > sim) sim = newsim;
		}

		return sim;
	}


	/**
	 * Calculates a score for the Gene in the context of the Text.
	 *
	 * @param gene
	 * @param text
	 * @return
	 */
	public float getScore (Gene gene, Text text, String geneName) {
		if (verbosity > 0)
			System.out.print("Score of gene " + gene.ID + " in text " + text.ID + ": ");

		//
		//HashMap<String, Float> allScores = new HashMap<String, Float>();
		//

		LinkedList<Float> scores = scoreCache.get(gene.getID() + ";" + text.getID());
		if (scores == null) {
			scores = getScores(gene, text, geneName);
			scoreCache.put(gene.getID() + ";" + text.getID(), scores);
		}
		//LinkedList<Float> scores = getScores(gene, text, geneName);//new LinkedList<Float>();

		// PMID score == 1? Immediate identification
		//if (scores.get(6) != null && scores.get(6) == 1.0f)
		//	return 1.0f;

		// locationScore == 1? Immediate identification
		//if (scores.get(5) != null && scores.get(5) == 1.0f)
		//	return 1.0f;

		float score = 0.0f;
		Collections.sort(scores);
		Collections.reverse(scores);
		float sum3 = 0.0f;
		int count3 = 0;

		if (scores.get(0) > 0.0f) {
			sum3 += scores.get(0);
			count3++;
		}
		if (scores.get(1) > 0.0f) {
			sum3 += scores.get(1);
			count3++;
		}
		if (scores.get(2) > 0.0f) {
			sum3 += scores.get(2);
			count3++;
		}

		float avg3 = sum3 / (float)count3;
		score = avg3;

		// if we have only one single score (all others NaN or -1), then be careful!
		if (count3 == 1 && //(allScores.containsKey("GO") || allScores.containsKey("LC")))
				( (scores.get(3) != null && scores.get(3) >= 0.0f) || (scores.get(5) != null && scores.get(5) >= 0.0f) )
		)
			score = -1.0f * score;

		if(verbosity>0)
			System.out.println(" => " + score);

		return score;
	}


	/**
	 * Returns a list of scores for scoring each context vector. Each score is between -1.0f and 1.0f.
	 * Some entries in the list might be null, when the gene did not have a corresponding context vector;
	 * @param gene
	 * @param text
	 * @param geneName
	 * @return
	 */
	public LinkedList<Float> getScores (Gene gene, Text text, String geneName) {
		if (scoreCache.containsKey(gene.getID() + ";" + text.getID()))
			return scoreCache.get(gene.getID() + ";" + text.getID());

		
		//
		HashMap<String, Float> allScores = new HashMap<String, Float>();
		LinkedList<Float> scores = new LinkedList<Float>();

		// protein sequence length
		// consists of 775 amino-acids
		// testcase: gene name "XCE" (9427) in PMID 9931490
		float proteinLengthScore = getScore_ProteinLength(
				gene.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_PROTEINLENGTHS),
				text.plainText);
		//text.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_TEXT));
		if (proteinLengthScore > 0.0f)
			allScores.put("AA", proteinLengthScore);
		scores.add(proteinLengthScore);

		// protein weight, in kDa
		float proteinMassScore = getScore_ProteinMass(
				gene.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_PROTEINMASS),
				text.plainText);
		//text.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_TEXT));
		if (proteinMassScore > 0.0f)
			allScores.put("MS", proteinMassScore);
		scores.add(proteinMassScore);

		// domains
		float proteinDomainScore = getScore_ProteinDomain(
				gene.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_PROTEINDOMAINS),
				text.plainText);
		//text.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_TEXT));
		if (proteinDomainScore > 0.0f)
			allScores.put("DO", proteinDomainScore);
		scores.add(proteinDomainScore);

		float diseaseScore = getScore_Diseases(
				//gene.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_DISEASE),
				gene.getContextModel().getAllContextVectorsForType(GeneContextModel.CONTEXTTYPE_DISEASE),
				text.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_TEXT));
		//diseaseScore *= 4.0f;
		if (diseaseScore > 0.0f)
			allScores.put("DS", diseaseScore);
		scores.add(diseaseScore);

		//System.out.println("Getting FU score");
		float functionScore = getScore_Functions(
				gene.getContextModel().getAllContextVectorsForType(GeneContextModel.CONTEXTTYPE_FUNCTION),
				text.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_TEXT));
		if (functionScore < 0.03f)
			functionScore = 0.0f;
		if (functionScore > 0.0f)
			allScores.put("FU", functionScore);
		scores.add(functionScore);

		// GO IDs
		float goCodeScore = getScore_GOCodes(
				gene.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_GOCODES),
				text.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_GOCODES));
		if (goCodeScore > 0.0f)
			allScores.put("GO", goCodeScore);
		scores.add(goCodeScore);

		// GeneRIFs
		//System.out.println("#  scoring generifs:");
		float geneRifScore = getScore_GeneRifs(
				gene.getContextModel().getAllContextVectorsForType(GeneContextModel.CONTEXTTYPE_GENERIF),
				text.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_TEXT));
		if (geneRifScore > 0.0f)
			allScores.put("GR", geneRifScore);
		scores.add(geneRifScore);

		// keywords
		//System.out.println("# scoring keywords:");
		float keywordScore = getScore_Keywords(
				gene.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_KEYWORDS),
				text.plainText);
		//text.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_TEXT));
		if (keywordScore > 0.0f){
			allScores.put("KW", keywordScore);
		}
		scores.add(keywordScore);

		// chr. location
		float locationScore = getScore_Location(
				gene.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_LOCATIONS),
				text.plainText);
		if (locationScore > 0.0f)
			allScores.put("LC", locationScore);
		scores.add(locationScore);

		// mutations
		float proteinMutationScore = getScore_ProteinMutation(
				gene.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_PROTEINMUTATIONS),
				text.plainText);
		//text.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_TEXT));
		if (proteinMutationScore > 0.0f)
			allScores.put("MT", proteinMutationScore);
		scores.add(proteinMutationScore);

		// PMIDs
		if (ISGNProperties.get("useGenePubmed") != null
				&& ISGNProperties.get("useGenePubmed").toLowerCase().matches("(true|1|y|yes)")) {
			float pmidScore = getScore_PubMedID(
					gene.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_PMIDS),
					text.getPMID());
			if (pmidScore > 0.0f) {
				allScores.put("PM", pmidScore);
				//System.err.println("PMID score for gene " + gene.ID + " = " + pmidScore);
			}
			scores.add(pmidScore);
		} else {
			//System.err.println("NOT USING PUBMED");
		}

		float summaryScore = getScore_Summary(
				gene.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_ENTREZGENESUMMARY),
				text.getContextModel().getContextVectorForType(ContextModel.CONTEXTTYPE_TEXT));
		if (summaryScore < 0.08f)
			summaryScore = 0.0f;
		if (summaryScore > 0.0f)
			allScores.put("SU", summaryScore);
		scores.add(summaryScore);

		// tissue spec.
		//System.out.println("#  scoring tissues:");
		float tissueScore = getScore_Tissues(
				gene.getContextModel().getAllContextVectorsForType(GeneContextModel.CONTEXTTYPE_TISSUE),
				text.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_TEXT));
		if (tissueScore > 0.0f)
			allScores.put("TS", tissueScore);
		scores.add(tissueScore);

		// EG- and UG-interactors
		float interactorScore = getScore_Interactors(
				gene.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_INTERACTORS),
				gene.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_PROTEININTERACTIONS),
				text.plainText);
		if (interactorScore > 0.0f)
			allScores.put("IT", interactorScore);
		scores.add(interactorScore);

		// UniProt-interaction
		//		float uinteractionsScore = getScore_UInteractors(
		//				gene.getContextModel().getContextVectorForType(GeneContextModel.CONTEXTTYPE_PROTEININTERACTIONS),
		//				text.plainText);
		//		if (uinteractionsScore > 0.0f)
		//			allScores.put("UI", uinteractionsScore);
		//		scores.add(uinteractionsScore);

		// output scores if needed
		if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
			System.out.println("#Scores of gene " + gene.ID + " in text " + text.ID + ": " + allScores);
		else if (ConstantsNei.OUTPUT_LEVEL == ConstantsNei.OUTPUT_LEVELS.SVM) {
			System.out.print("0");
			if (allScores.containsKey("AA"))
				System.out.print(" 1:"+allScores.get("AA"));
			if (allScores.containsKey("DO"))
				System.out.print(" 2:"+allScores.get("DO"));
			if (allScores.containsKey("DS"))
				System.out.print(" 3:"+allScores.get("DS"));
			if (allScores.containsKey("FU"))
				System.out.print(" 4:"+allScores.get("FU"));
			if (allScores.containsKey("GO"))
				System.out.print(" 5:"+allScores.get("GO"));
			if (allScores.containsKey("GR"))
				System.out.print(" 6:"+allScores.get("GR"));
			if (allScores.containsKey("KW"))
				System.out.print(" 7:"+allScores.get("KW"));
			if (allScores.containsKey("LC"))
				System.out.print(" 8:"+allScores.get("LC"));
			if (allScores.containsKey("MT"))
				System.out.print(" 9:"+allScores.get("MT"));
			if (allScores.containsKey("PM"))
				System.out.print(" 10:"+allScores.get("PM"));
			if (allScores.containsKey("SU"))
				System.out.print(" 11:"+allScores.get("SU"));
			if (allScores.containsKey("TS"))
				System.out.print(" 12:"+allScores.get("TS"));
			if (allScores.containsKey("MS"))
				System.out.print(" 13:"+allScores.get("MS"));
			if (allScores.containsKey("IT"))
				System.out.print(" 14:"+allScores.get("IT"));
			System.out.println(" #0 " + text.ID + " " + gene.ID + " " + geneName);
		} // output


		scoreCache.put(gene.getID() + ";" + text.getID(), scores);

		return scores;
	}


	/**
	 *
	 * @param cv1s
	 * @param cv2
	 * @return
	 */
	public float getScore_Diseases (Collection<ContextVector> cv1s, ContextVector cv2) {
		return getNormalizedOverlapSimilarity(cv1s, cv2);
	}


	/**
	 *
	 * @param cv1s
	 * @param cv2
	 * @return
	 */
	public float getScore_Functions (Collection<ContextVector> cv1s, ContextVector cv2) {
		float sim = getNormalizedOverlapSimilarity(cv1s, cv2);

		return sim;
	}


	/**
	 *
	 * @param cv1s
	 * @param cv2
	 * @return
	 */
	public float getScore_GeneRifs (Collection<ContextVector> cv1s, ContextVector cv2) {
		return getNormalizedOverlapSimilarity(cv1s, cv2);
	}


	/**
	 *
	 * @return
	 */
	public float getScore_GOCodes (ContextVector cv1, ContextVector cv2) {
		
		//System.err.println("Calling GPS.getScore_GOC");
		
		float sim = -1.0f;

		LinkedList<String> goCodes1;
		LinkedList<String> goCodes2;

		if (cv1 != null) {
			goCodes1 = cv1.getElementLabels();
		} else {
			//System.err.println("  No GO codes for gene.");
			return -1.0f;
		}

		if (cv2 != null) {
			goCodes2 = cv2.getElementLabels();
		} else {
			//System.err.println("  No GO codes for text.");
			return -1.0f;
		}
		
		

		sim = goTermScorer.getGOSimilarity(goCodes1, goCodes2);

		//		System.out.println("getScore_GOCodes: #codes1="+goCodes1.size());
		//		System.out.println("getScore_GOCodes: #codes2="+goCodes2.size());
		//		System.out.println("getScore_GOCodes: "+sim);

		return sim;
	}


	/**
	 * Computes the similarity by comparing the number of keywords found in the given text with
	 * all keywords assigned to the gene (#found / #all).
	 * @param cv1
	 * @param sourceText
	 * @return
	 */
	public float getScore_Keywords (ContextVector cv1, String sourceText) {
		if (!checkContextVector(cv1)) return -1.0f;
		LinkedList<String> keywords = cv1.getElementLabels();
		if (keywords.size() == 0) return -1.0f;
		int counter = 0;
		String lowerCaseText = sourceText.toLowerCase();
		for (String keyword: keywords) {
			if (lowerCaseText.matches(".*(^|[\\s\\(\\[])" + keyword.toLowerCase() + "([\\s\\,\\.\\;\\)\\]]|$).*")) {
				counter++;
			}
		}
		float sim = (float)counter / (float)keywords.size();

		return sim;
	}


	/**
	 *
	 * @param cv1
	 * @param sourceText
	 * @return
	 */
	public float getScore_Location (ContextVector cv1, String sourceText) {
		if (!checkContextVector(cv1)) return -1.0f;

		float sim = 0.0f;

		// 11q23.3 = band q23.3 of chromosome 11
		// Xp22.2 = chromosomal band Xp22.2
		// chromosome 1...X,Y,Z

		// chromosomal band 11q13
		// chromosome band 9p24
		// chromosome 4 at band q21
		// 9p12 chromosomal band
		// chromosome 6
		// 11q
		// 11q13
		// 11qter
		// 9p
		// t(3;9)(q12;p24)

		LinkedList<String> locations = cv1.getElementLabels();
		//System.err.println("   Searching for " + locations.toString());
		//System.err.println("   in: " + cv2);
		for (String loc: locations) {
			if (loc.matches("([0-9XYZ]+)[pq][0-9]+(\\.[0-9]+)?")) {
				//System.err.print("   -> " + loc + "?");
				//if (cv2.contains(loc)) sim = 1.0f;
				if (sourceText.matches(".*(^|\\D)" + loc + "(\\D|$).*")) sim = 1.0f;

				String chr = loc.replaceFirst("^([0-9XYZ]+)[pq][0-9]+(\\.[0-9]+)?$", "$1");
				String arm = loc.replaceFirst("^[0-9XYZ]+([pq])[0-9]+(\\.[0-9]+)?$", "$1");
				String band = loc.replaceFirst("^[0-9XYZ]+[pq]([0-9]+)(\\.[0-9]+)?$", "$1");
				String subband = loc.replaceFirst("^[0-9XYZ]+[pq]([0-9]+)(\\.[0-9]+)?$", "$2");

				//System.out.println("#LC: checking for " + chr + "/" + arm + "/" + band + "/" + subband);

				//System.err.print(" " + chr + " & " + arm+band+subband + "?");
				//if (cv2.contains(chr) && cv2.contains(arm + band + subband)) if (sim < 0.95f) sim = 0.95f;
				if (sourceText.matches(".*(^|\\D)" + chr + "(\\D|$).*"))
					if (sourceText.matches(".*(^|\\D)" + arm + band + subband + "(\\D|$).*"))
						if (sim < 0.95f) sim = 0.95f;

				//System.err.print(" " + chr + " & " + arm+band + "?");
				//if (cv2.contains(chr) && cv2.contains(arm + band)) if (sim < 0.9f) sim = 0.9f;
				if (sourceText.matches(".*(^|\\D)" + chr + "(\\D|$).*"))
					if (sourceText.matches(".*(^|\\D)" + arm + band + "(\\D|$).*"))
						if (sim < 0.9f) sim = 0.9f;

				//System.err.print(" " + arm+band+subband + "?");
				//if (cv2.contains(arm + band + subband)) if (sim < 0.85f) sim = 0.85f;
				if (sourceText.matches(".*(^|\\D)" + arm + band + subband + "(\\D|$).*"))
					if (sim < 0.85f) sim = 0.85f;

				//System.err.print(" " + arm+band + "?");
				//if (cv2.contains(arm + band)) if (sim < 0.8f) sim = 0.8f;
				if (sourceText.matches(".*(^|\\D)" + arm + band + "(\\D|$).*"))
					if (sim < 0.8f) sim = 0.8f;

				//if (cv2.contains("chromosome") && cv2.contains(chr))
				if (sourceText.matches(".*chromosome\\s" + chr + "(\\D|$).*")
						|| sourceText.matches(".*(^|\\D)" + chr + "\\schromosome.*"))
					//if (cv2.containsSubsequent(new String[]{"chromosome", chr})
					//|| cv2.containsSubsequent(new String[]{chr, "chromosome"}))
					if (sim < 0.5f) sim = 0.5f;

				//System.err.println();
			} else if (loc.matches("(chromosome\\s)?[0-9XYZ]+")) {
				String chr = loc.replaceFirst("^(chromosome\\s)?([0-9XYZ]+)$", "$2");
				//if (cv2.containsSubsequent(new String[]{"chromosome", chr})
				//	|| cv2.containsSubsequent(new String[]{chr, "chromosome"}))
				if (sourceText.matches(".*chromosome\\s" + chr + "(\\D|$).*")
						|| sourceText.matches(".*(^|\\D)" + chr + "\\schromosome.*"))
					if (sim < 0.5f) sim = 0.5f;

			}
		}

		return sim;
	}


	/**
	 *
	 * @param cv1
	 * @param sourceText
	 * @return
	 */
	public float getScore_ProteinDomain (ContextVector cv1, String sourceText) {
		//System.out.println("# Getting protein domain scores:");
		//System.out.println("# cv1: " + cv1);
		//System.out.println("# cv2: " + cv2);
		//System.out.println("# cv2: " + cv2.labelsToString());
		if (!checkContextVector(cv1)) return -1.0f;

		float score = -1.0f;
		LinkedList<String> domains = cv1.getElementLabels();
		for (String dom: domains) {
			//System.out.println("# Checking for domain: '" + dom + "'");
			if (sourceText.matches(".*(^|\\s)" + dom + "([\\s\\,\\.\\;\\)]|$).*")) {
				//System.out.println("# Found it.");
				return 1.0f;
			}
		}

		return score;
	}


	/**
	 *
	 * @param cv1
	 * @param sourceText
	 * @return
	 */
	public float getScore_ProteinLength (ContextVector cv1, String sourceText) {
		//System.out.println("# Getting protein length scores:");
		//System.out.println("# cv1: " + cv1);
		//System.out.println("# cv2: " + cv2);
		//System.out.println("# cv2: " + cv2.labelsToString());
		if (!checkContextVector(cv1)) return -1.0f;

		float score = -1.0f;
		LinkedList<String> lengths = cv1.getElementLabels();
		for (String len: lengths) {
			//System.out.println("# Checking for length: '" + len + "'");
			if (sourceText.matches(".*(^|\\D)" + len + "\\s?(aa|amino[\\s\\-]?acid)s?.*")) {
				//System.out.println("# Found it.");
				return 1.0f;
			}
		}

		return score;
	}


	/**
	 *
	 * @param cv1
	 * @param sourceText
	 * @return
	 */
	public float getScore_ProteinMass (ContextVector cv1, String sourceText) {
		if (!checkContextVector(cv1)) return -1.0f;

		float score = -1.0f;
		LinkedList<String> masses = cv1.getElementLabels();
		for (String mass: masses) {
			if (!mass.equals("-")){
				int dalton = Integer.parseInt(mass);
				//System.out.println("# Checking for mass: '" + mass + "'");
				//System.out.println("#-----Checking mass " + mass + " dalton");
				if (sourceText.matches(".*(^|\\D)" + mass + "[\\s\\-]?[Dd](a(lton)?)?.*")) {
					//System.out.println("#-----Found mass " + mass + " dalton");
					return 1.0f;
				}

				float fdalton = (float)dalton;
				float fkda = fdalton / 1000.0f;

				// round to kDa, no floats
				// 38200 => 38 kDa
				int kda = Math.round(fkda);
				//System.out.println("#-----Checking mass " + kda + " -- kda (was) " + mass + " da)");
				if (sourceText.matches(".*(^|\\D)" + kda + "[\\s\\-]?[Kk][Dd][Aa].*")) {
					//System.out.println("#-----Found mass " + kda + " -- kda (was) " + mass + " da)");
					return 1.0f;
				}

				// round to kDa, one digit in floats
				// 38200 => 38.2 kDa
				fkda = fdalton / 100.0f;
				kda = Math.round(fkda);
				String skda = "" + kda;
				skda = skda.replaceFirst("^(\\d+)(\\d)$", "$1.$2");
				//System.out.println("#-----Checking mass " + skda + " -- skda (was) " + mass + " da)");
				if (sourceText.matches(".*(^|\\D)" + skda + "[\\s\\-]?[Kk][Dd][Aa].*")) {
					//System.out.println("#-----Found mass " + skda + " -- skda (was) " + mass + " da)");
					return 1.0f;
				}
			}
		}

		return score;
	}


	/**
	 *
	 * @param cv1
	 * @param sourceText
	 * @return
	 */
	public float getScore_ProteinMutation (ContextVector cv1, String sourceText) {
		//System.out.println("# Getting protein mutation scores:");
		//System.out.println("# cv1: " + cv1);
		//System.out.println("# cv2: " + cv2);
		//System.out.println("# cv2: " + cv2.labelsToString());
		if (!checkContextVector(cv1)) return -1.0f;

		float score = -1.0f;
		LinkedList<String> lengths = cv1.getElementLabels();
		for (String mut: lengths) {
			//System.out.println("# Checking for mutation '" + mut + "'");
			if (sourceText.matches(".*(^|[\\s\\(\"'])" + mut + "([\\s\\,\\.\\;\\)\"']|$)s?.*")) {
				//System.out.println("# Found it.");
				return 1.0f;
			}
		}

		return score;
	}


	/**
	 *
	 * @param cv1
	 * @param sourceText
	 * @return
	 */
	public float getScore_EInteractors (ContextVector cv1, String sourceText) {
		if (!checkContextVector(cv1)) return -1.0f;

		float score = -1.0f;
		LinkedList<String> lengths = cv1.getElementLabels();
		for (String interactor: lengths) {
			//System.out.println("# Checking for mutation '" + mut + "'");
			if (sourceText.matches(".*(^|[\\s\\(\\[\\-\\/\"\'])" + interactor + "([\\s\\)\\]\\;\\:\\,\\-\\/\"\']|$)s?.*")) {
				//System.out.println("#-----Found interactor (E): " + interactor);
				return 1.0f;
			}
		}

		return score;
	}


	/**
	 *
	 * @param cv1
	 * @param sourceText
	 * @return
	 */
	public float getScore_UInteractors (ContextVector cv1, String sourceText) {
		if (!checkContextVector(cv1)) return -1.0f;

		float score = -1.0f;
		LinkedList<String> lengths = cv1.getElementLabels();
		for (String interactor: lengths) {
			//System.out.println("# Checking for mutation '" + mut + "'");
			if (sourceText.matches(".*(^|[\\s\\(\\[\\-\\/\"\'])" + interactor + "([\\s\\)\\]\\;\\:\\,\\-\\/\"\']|$)s?.*")) {
				//System.out.println("#-----Found interactor (U): " + interactor);
				return 1.0f;
			}
		}

		return score;
	}


	/**
	 *
	 * @param cv1
	 * @param cv2
	 * @param sourceText
	 * @return
	 */
	public float getScore_Interactors (ContextVector cv1, ContextVector cv2, String sourceText) {
		if (!checkContextVector(cv1) && !checkContextVector(cv2))
			return -1.0f;

		float eScore = getScore_EInteractors(cv1, sourceText);
		float uScore = getScore_UInteractors(cv2, sourceText);

		if (eScore > uScore)
			return eScore;
		else
			return uScore;
	}


	/**
	 *
	 * @param cv1
	 * @param pmid
	 * @return
	 */
	public float getScore_PubMedID (ContextVector cv1, int pmid) {
		//if (!checkContextVectors(cv1)) return -1.0f;
		if (cv1 == null) return -1.0f;
		if (cv1.contains(""+pmid)) return 1.0f;
		return 0.0f;
	}


	/**
	 *
	 * TODO
	 * - if the normalized overlap between a text and the gene summary is >0.2, it's almost certainly a hit<br>
	 * - a normalized overlap of >0.1 should also result in a higher score<br>
	 * =&gt; stretch normed overlaps [0.0...0.2) -&gt; [0.0...0.9) or something like that<br>
	 * <br>
	 * OR: take cosine similarity<br>
	 *
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	public float getScore_Summary (ContextVector cv1, ContextVector cv2) {
		if (!checkContextVectors(cv1, cv2)) return -1.0f;
		//float norm = getNormalizedOverlapSimilarity(cv1, cv2);
		float cos = getCosineSimilarity(cv1, cv2);
		//if (cos > norm)
		//	System.err.println("   # cos>norm for summary score ("+cos+" / "+norm+")");
		//float score = norm;
		return cos;//score;
	}


	/**
	 *
	 * @param cv1s
	 * @param cv2
	 * @return
	 */
	public float getScore_Tissues (Collection<ContextVector> cv1s, ContextVector cv2) {
		//if (!checkContextVectors(cv1, cv2)) return -1.0f;
		return getNormalizedOverlapSimilarity(cv1s, cv2);
	}


	/**
	 *
	 *
	 */
	public static void test () {
		//  355,581,596,9531,1656,4841 1579499 => 1656

		TestCase testcase;

		// tough: 22985 vs 8878
		testcase = new TestCase(
				"data/abs_test/10681536.txt",
				new String[]{"29985", "8878", "27173"},
				new boolean[]{true, false, true});
		testcase.test();

		testcase = new TestCase(
				"data/abs/1579499.txt",
				new String[]{"1656", "4841"},
				new boolean[]{true, false});
		testcase.test();

		testcase = new TestCase(
				"data/abs/9694715.txt",
				new String[]{"2147","2316","2317","2318","7450","26","6462"},
				new boolean[]{true, true, true, true, true, false, false});
		testcase.test();

		testcase = new TestCase(
				"data/abs/1648265.txt",
				new String[]{"3569","3570","1270","1271"},
				new boolean[]{true, true, true, true});
		testcase.test();

		// tough: 4313 vs. 4314
		testcase = new TestCase(
				"data/abs/10706098.txt",
				new String[]{"3263","4312","4313","4314","64386"},
				new boolean[]{false, false, true, false, true});
		testcase.test();

		/*testcase = new TestCase(
				"data/abs/.txt",
				new String[]{""},
				new boolean[]{true});
		testcase.test();

		testcase = new TestCase(
				"data/abs/.txt",
				new String[]{""},
				new boolean[]{true});
		testcase.test();

		 */

	}


	/**
	 *
	 * @param verbosity
	 */
	public void setVerbosity (int verbosity) {
		this.verbosity = verbosity;
	}
	
	
	public GOTermSimilarity getGoTermScorer () {
		return goTermScorer;
	}


}
