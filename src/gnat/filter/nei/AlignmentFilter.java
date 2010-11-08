package gnat.filter.nei;

import gnat.alignment.Alignment;
import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.Gene;
import gnat.representation.GeneRepository;
import gnat.representation.IdentificationStatus;
import gnat.representation.RecognizedEntity;
import gnat.representation.TextRepository;
import gnat.utils.AlignmentHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Filter for recognized gene names based on the aligment score against candidate gene's synonyms.
 * This filter removes recognized gene names if none of its candidate genes has a similar synonym.
 *
 * <br><br>
 * <b>Requirements:</b><br>
 * Needs information on each gene (such as synonysm), this requires a loaded GeneRepository.
 */
public class AlignmentFilter implements Filter {

	//private GeneRepository geneRepository;
	private Alignment alignment;
	private float threshold = 0;

	/**
	 * Creates a new filter for a given alignment method, a gene repository to lookup candidate genes, and a minimum score threshold.
	 * */
	public AlignmentFilter (Alignment alignment, float threshold) {
		this.alignment = alignment;
		this.threshold = threshold;
	}

	
	/**
	 * Filter for recognized gene names based on the aligment score against candidate gene's synonyms.
	 * This filter removes recognized gene names if none of its candidate genes has a similar synonym,
	 * i.e., aligns with a score >= threshold.
	 */
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository)
	{
		Map<String, List<Gene>> closestGeneMap = new HashMap<String, List<Gene>>();

		Iterator<RecognizedEntity> unidentifiedGeneNames = context.getUnidentifiedEntities().iterator();
		while (unidentifiedGeneNames.hasNext())
		{
			RecognizedEntity recognizedGeneName = unidentifiedGeneNames.next();
			IdentificationStatus identificationStatus = context.getIdentificationStatus(recognizedGeneName);
			List<Gene> candidateGenesWithSimilarSynonyms = closestGeneMap.get(recognizedGeneName.getName());

			if(candidateGenesWithSimilarSynonyms==null){
				Set<String> candidateIds = identificationStatus.getIdCandidates();
				Set<Gene> candidateGenes = new HashSet<Gene>();
				for (String id : candidateIds) {
					Gene gene = geneRepository.getGene(id);
					if (gene != null) {
						candidateGenes.add(gene);
					}
	            }
				candidateGenesWithSimilarSynonyms = AlignmentHelper.getAlignedGenes(alignment, recognizedGeneName, candidateGenes, threshold);
				closestGeneMap.put(recognizedGeneName.getName(), candidateGenesWithSimilarSynonyms);
			}

			Set<String> closestGeneIds = new HashSet<String>();
			for (Gene gene : candidateGenesWithSimilarSynonyms) {
				closestGeneIds.add(gene.getID());
            }

			identificationStatus.setIdCandidates(closestGeneIds);
		}
	}

	/**
	 * Sets the minimum alignment threshold.
	 * */
	public void setThreshold (float i) {
	    this.threshold = i;
    }

}
