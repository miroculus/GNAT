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

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 
 * Performs gene name disambiguation when genes of multiple species are sought.
 * 
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 */
public class MultiSpeciesDisambiguationFilter implements Filter {

	private GenePubMedScorer genePubMedScorer;
	private double threshold;
	@SuppressWarnings("unused") private int topNumber = 1;

	/** Maps Gene IDs to PubMed IDs to Scores */
	private Map<String, Map<String, Float>> scoredGenes = new HashMap<String, Map<String, Float>>();

	/** */
	public int verbosity = 6;
	
	Context context;
	TextRepository textRepository;
	GeneRepository geneRepository;
	

	/**
	 * Constructs a new DisambiguationFilter.
	 * @param threshold - removes all genes with a score below this threshold
	 * @param topNumber - keep this number of IDs per name
	 */
	public MultiSpeciesDisambiguationFilter (double threshold, int topNumber) {
		this.threshold = threshold;
		this.topNumber = topNumber;
		genePubMedScorer = new GenePubMedScorer(new GOAccess(), "data/go2go.object");
		genePubMedScorer.setVerbosity(this.verbosity);
	}
	

	/**
	 *
	 * @param context
	 * @param textRepository
	 * @param geneRepository
	 */
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {
		float overallTopScore = 0;
		this.context = context;
		this.textRepository = textRepository;
		this.geneRepository = geneRepository;

		Iterator<RecognizedEntity> unidentifiedGeneNames = context.getUnidentifiedEntities().iterator();
		while (unidentifiedGeneNames.hasNext()) {
			RecognizedEntity recognizedGeneName = unidentifiedGeneNames.next();

			IdentificationStatus identificationStatus = context.getIdentificationStatus(recognizedGeneName);
			Set<String> geneIdCandidates = identificationStatus.getIdCandidates();

			Set<Integer> taxonIDs = recognizedGeneName.getText().taxonIDs;
			Map<Integer, Set<String>> taxon2IdCandidates = new HashMap<Integer, Set<String>>();
			Iterator<String> it = geneIdCandidates.iterator();
			while (it.hasNext()) {
				String gid = it.next();
				int tax = geneRepository.getGene(gid).getTaxon();
				Set<String> candidates;
				if (taxon2IdCandidates.containsKey(tax)) {
					candidates = taxon2IdCandidates.get(tax);
				} else {
					candidates = new TreeSet<String>();
				}
				candidates.add(gid);
				taxon2IdCandidates.put(tax, candidates);
			}
			
			if (taxonIDs == null || taxonIDs.size() == 0)
				taxonIDs = ConstantsNei.DEFAULT_SPECIES;
			
			if (taxonIDs != null && taxonIDs.size() > 0) {
				for (int tax: taxonIDs) {
					Set<String> candidates = taxon2IdCandidates.get(tax);
					//System.out.println("#MSDF: " + recognizedGeneName + " for taxon " + tax + " has cIDs " + candidates);
					if (candidates == null || candidates.size() == 0) {
						if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
							System.out.println("#No candidate IDs to disambiguate for species "+tax);
						continue;
						
					} else
						if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
							System.out.println("#Disambiguating gene "+recognizedGeneName.getName()+" with "+candidates.size()+" candidates from species " + tax);
	
					if (ConstantsNei.OUTPUT_LEVEL == ConstantsNei.OUTPUT_LEVELS.SVM) {
						System.out.print("#RANKING: " + recognizedGeneName.getText().PMID + " " 
								+ recognizedGeneName.getName());
						for (String id: candidates)
							System.out.print(" " + id);
						System.out.println();
					}
					
					// the text in which this gene name occurs
					Text text = recognizedGeneName.getText();
					Text text2 = textRepository.getText(text.ID);
					
					//List<ScoredGene> rankedGenes = rankGenesForText (geneIdCandidates, text, recognizedGeneName.getName());
					List<ScoredGene> rankedGenes = rankGenesForText (candidates, text2, recognizedGeneName.getName());
		
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
							context.identifyAsGene(recognizedGeneName, rankedGenes.get(i).getGene(), rankedGenes.get(i).getScore());
						}
					}
	
				} // for all species
			} else {
				System.err.println("#MSDF: No taxon IDs for " + recognizedGeneName.getName()
						+ " in " + recognizedGeneName.getText().getPMID());
			}

		}

		genePubMedScorer.finalize();
	}


	/**
	 *
	 * @param context
	 * @throws IOException
	 */
	public void filterX (Context context, String outfile) throws IOException {
		float overallTopScore = 0;

		FileWriter writer = new FileWriter(outfile);

		Iterator<RecognizedEntity> unidentifiedGeneNames = context.getUnidentifiedEntities().iterator();
		while (unidentifiedGeneNames.hasNext()) {
			RecognizedEntity recognizedGeneName = unidentifiedGeneNames.next();

			IdentificationStatus identificationStatus = context.getIdentificationStatus(recognizedGeneName);
			Set<String> geneIdCandidates = identificationStatus.getIdCandidates();

			//System.out.println("Now disambiguating gene "+recognizedGeneName.getName()+" with "+geneIdCandidates.size()+" candidates...");

			// the text in which this gene name occurs
			Text text = recognizedGeneName.getText();

			List<ScoredGene> rankedGenes = rankGenesForText (writer, recognizedGeneName.getName(), geneIdCandidates, text);

			if (rankedGenes.size() > 0) {
				float topScore = rankedGenes.get(0).getScore();
				if(topScore>overallTopScore){
					overallTopScore = topScore;
				}
				for (int i = 0; i < rankedGenes.size(); i++) {
					if (rankedGenes.get(i).getScore() < topScore) break;
					if (rankedGenes.get(i).getScore() < threshold) break;
					context.identifyAsGene(recognizedGeneName, rankedGenes.get(i).getGene(), rankedGenes.get(i).getScore());
				}
			}
		}

		genePubMedScorer.finalize();
		writer.close();
	}


	/**
	 *	Stores a score for a gene and a text.
	 *
	 * @param gene
	 * @param text
	 * @param score
	 */
	void addScoredGene (Gene gene, Text text, float score) {
		Map<String, Float> ref;
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
	 * @param geneIdCandidates
	 * @param text
	 * @return
	 * @throws IOException
	 */
	public List<ScoredGene> rankGenesForText (FileWriter writer, String geneName, Set<String> geneIdCandidates, Text text) throws IOException {
		int NUMBER_OF_SCORES = 12;

		//
		//LinkedList<Gene> ranked = new LinkedList<Gene>();
		HashMap<Gene, LinkedList<Float>> allScores = new HashMap<Gene, LinkedList<Float>>();
		LinkedList<Float> maxima = new LinkedList<Float>();
		for (int i = 0; i < NUMBER_OF_SCORES; i++)
			maxima.add(0.0f);

		//
		for (String id : geneIdCandidates) {
			Gene candidateGene = geneRepository.getGene(id);
			LinkedList<Float> scores = genePubMedScorer.getScores(candidateGene, text, geneName);
			writer.write(geneName+"\t"+id);
			for (int i = 0; i < scores.size(); i++){
				writer.write("\t"+scores.get(i));
				if (scores.get(i) > maxima.get(i))
					maxima.set(i, scores.get(i));
			}
			writer.write("\n");

			allScores.put(candidateGene, scores);
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
