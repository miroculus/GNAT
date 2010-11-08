package gnat.filter.nei;

import gnat.ConstantsNei;
import gnat.client.nei.GenePubMedScorer;
import gnat.database.go.GOAccess;
import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.Gene;
import gnat.representation.GeneRepository;
import gnat.representation.IdentificationStatus;
import gnat.representation.RecognizedEntity;
import gnat.representation.ScoredGene;
import gnat.representation.Text;
import gnat.representation.TextRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A disambiguation filter that picks among many candidate genes for a recognized gene name the one 
 * whose context fits best the context of the text. It uses a GenePubMedScorer to score a gene's context 
 * against the context of a text.
 * 
 * <br><br>
 * <b>Requirements:</b><br>
 * Needs information on each gene (such as species), this requires a loaded GeneRepository.
 * 
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 */
public class DisambiguationFilter implements Filter {

	private GenePubMedScorer genePubMedScorer;
	private GeneRepository geneRepository;
	private double threshold;
	@SuppressWarnings("unused")
	private int topNumber = 1;

	/** Maps Gene IDs to PubMed IDs to Scores */
	private HashMap<String, HashMap<String, Float>> scoredGenes = new HashMap<String, HashMap<String, Float>>();

	/** */
	public int verbosity = 1;

	/**
	 * Constructs a new DisambiguationFilter.
	 * @param threshold - removes all genes with a score below this threshold
	 * @param topNumber - keep this number of IDs per name
	 */
	public DisambiguationFilter (double threshold, int topNumber) {
		this.threshold = threshold;
		this.topNumber = topNumber;
		genePubMedScorer = new GenePubMedScorer(new GOAccess(), "data/go2go.object");
		genePubMedScorer.setVerbosity(this.verbosity);
	}

	/**
	 * Constructs a new DisambiguationFilter.
	 * @param threshold - removes all genes with a score below this threshold
	 * @param topNumber - keep this number of IDs per name
	 */
	public DisambiguationFilter (GOAccess goAccess, String go2goObjectFile, double threshold, int topNumber) {
		this.threshold = threshold;
		this.topNumber = topNumber;
		genePubMedScorer = new GenePubMedScorer(goAccess, go2goObjectFile);
		genePubMedScorer.setVerbosity(this.verbosity);
	}


	/**
	 *
	 * @param context
	 */
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {
		float overallTopScore = 0;

		Iterator<RecognizedEntity> unidentifiedGeneNames = context.getUnidentifiedEntities().iterator();
		while (unidentifiedGeneNames.hasNext()) {
			RecognizedEntity recognizedGeneName = unidentifiedGeneNames.next();

			//if (recognizedGeneName.getText().PMID.equals("15096576")) {
			//	System.err.println("#15096576: " + recognizedGeneName.getText().getPMID()+": " +
			//			recognizedGeneName.getName() + "/" + recognizedGeneName.getBegin() + "/" +
			//			context.getIdentificationStatus(recognizedGeneName).getIdCandidates());
			//}

			IdentificationStatus identificationStatus = context.getIdentificationStatus(recognizedGeneName);
//			Set<String> geneIdCandidates = identificationStatus.getIdCandidates();
			Set<String> candidates = identificationStatus.getIdCandidates();

//			TreeSet<Integer> taxonIDs = recognizedGeneName.getText().taxonIDs;
//			HashMap<Integer, TreeSet<String>> taxon2IdCandidates = new HashMap<Integer, TreeSet<String>>();
//			Iterator<String> it = geneIdCandidates.iterator();
//			while (it.hasNext()) {
//				String gid = it.next();
//				int tax = geneRepository.getGene(gid).taxon;
//				TreeSet<String> candidates;
//				if (taxon2IdCandidates.containsKey(tax)) {
//					candidates = taxon2IdCandidates.get(tax);
//				} else {
//					candidates = new TreeSet<String>();
//				}
//				candidates.add(gid);
//				taxon2IdCandidates.put(tax, candidates);
//			}
//
//
//			for (int tax: taxonIDs) {
//				TreeSet<String> candidates = taxon2IdCandidates.get(tax);
//				if (candidates == null || candidates.size() == 0) {
//					System.out.println("#No candidate IDs to disambiguate for species "+tax);
//					continue;
//				}

				if (verbosity > 1)
//					System.out.println("Now disambiguating gene "+recognizedGeneName.getName()+" with "+candidates.size()+" candidates from species " + tax);
					System.out.println("Now disambiguating gene "+recognizedGeneName.getName()+" with "+candidates.size()+" candidates.");

				if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.SVM) >= 0) {
					System.out.print("#RANKING: " + recognizedGeneName.getText().PMID + " "
							+ recognizedGeneName.getName());
					for (String id: candidates)
						System.out.print(" " + id);
					System.out.println();
				}

				// the text in which this gene name occurs
				Text text = recognizedGeneName.getText();

				//List<ScoredGene> rankedGenes = rankGenesForText (geneIdCandidates, text, recognizedGeneName.getName());
				List<ScoredGene> rankedGenes = rankGenesForText (candidates, text, recognizedGeneName.getName());

				if (rankedGenes.size() > 0) {
					float topScore = rankedGenes.get(0).getScore();
					//if (recognizedGeneName.getText().PMID.equals("15096576")) {
					//	System.err.println("#Top gene: " + rankedGenes.get(0).getGene().getID() + ", score=" + topScore);
					//}
					if(topScore>overallTopScore){
						overallTopScore = topScore;
					}
					for (int i = 0; i < rankedGenes.size(); i++) {
						if (rankedGenes.get(i).getScore() < topScore) break;
						if (rankedGenes.get(i).getScore() < threshold) break;
	//					System.out.print(" " + rankedGenes.get(i).ID);
						context.identifyAsGene(recognizedGeneName, rankedGenes.get(i).getGene(), rankedGenes.get(i).getScore());
					}
				}

//			} // for all species

		}

		genePubMedScorer.finalize();

		//System.out.println(this.getClass().getSimpleName()+": best overall score was "+overallTopScore);
	}


	/**
	 *	Stores a score for a gene and a text.
	 *
	 * @param gene
	 * @param text
	 * @param score
	 */
	void addScoredGene (Gene gene, Text text, float score) {
		HashMap<String, Float> ref;
		if (scoredGenes.containsKey(gene.ID)) {
			ref = scoredGenes.get(gene.ID);
		} else {
			ref = new HashMap<String, Float>();
		}
		ref.put(text.ID, score);
		scoredGenes.put(gene.ID, ref);
	}


	/**
	 *	Gets the stored score for a gene and a text.
	 * @param gene
	 * @param text
	 * @return
	 */
	float getKnownScore (Gene gene, Text text) {
		if (isScored(gene, text))
			return scoredGenes.get(gene.ID).get(text.ID);
		return -1.0f;
	}


	public double getThreshold() {
    	return threshold;
    }


	/**
	 * Checks if this gene was scored before together with the given text.
	 * @param gene
	 * @param text
	 * @return
	 */
	boolean isScored (Gene gene, Text text) {
		if (scoredGenes.containsKey(gene.ID)) {
			if (scoredGenes.get(gene.ID).containsKey(text.ID))
				return true;
		}
		return false;
	}


	/**
	 *
	 * @param geneIdCandidates
	 * @param text
	 * @return
	 */
	public List<ScoredGene> rankGenesForText (Set<String> geneIdCandidates, Text text, String geneName) {
		//
		//LinkedList<Gene> ranked = new LinkedList<Gene>();
		HashMap<Gene, LinkedList<Float>> allScores = new HashMap<Gene, LinkedList<Float>>();
		LinkedList<Float> maxima = new LinkedList<Float>();
		for (int i = 0; i < genePubMedScorer.NUMBER_OF_SCORES; i++)
			maxima.add(0.0f);

		//
		for (String id : geneIdCandidates) {
			Gene candidateGene = geneRepository.getGene(id);
			if(candidateGene!=null){
				LinkedList<Float> scores = genePubMedScorer.getScores(candidateGene, text, geneName);
				//if (geneIdCandidates.contains("51367"))
				//	System.err.println(scores);
				for (int i = 0; i < scores.size(); i++)
					if (scores.get(i) > maxima.get(i))
						maxima.set(i, scores.get(i));

				allScores.put(candidateGene, scores);
			}
		}

		List<ScoredGene> topGenes = new LinkedList<ScoredGene>();

		// normalize all scores with the highest score per vector
		for (Gene candidateGene: allScores.keySet()) {
			LinkedList<Float> scores = allScores.get(candidateGene);
			float sum = 0.0f;
			for (int i = 0; i < scores.size(); i++) {
				float score = scores.get(i);
				if (maxima.get(i) > 0.0f)
					score = score / maxima.get(i);
				if (score > 0.0f) sum += score;
			}
			topGenes.add(new ScoredGene(sum, candidateGene));
		}

		if (topGenes.size() > 0) {
			Collections.sort(topGenes);
			Collections.reverse(topGenes);
		}

		return topGenes;
	}


	/**
	 *
	 * @param verbosity
	 */
	public void setVerbosity (int verbosity) {
		this.verbosity = verbosity;
		genePubMedScorer.setVerbosity(verbosity);
	}


	/**
	 * Sets the minimum score a gene must reach to get identified.
	 * */
	public void setThreshold(double threshold)
    {
    	this.threshold = threshold;
    }
}
