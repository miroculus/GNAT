package gnat.filter.nei;

import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.Gene;
import gnat.representation.GeneRepository;
import gnat.representation.RecognizedEntity;
import gnat.representation.TextRepository;

import java.util.Iterator;
import java.util.Set;

/**
 * This filter "identifies" a gene name by forcing all remaining candidate IDs to
 * be in the final prediction.<br>
 * Gets all still unidentified gene names from the Context and sets each corresponding
 * candidate ID as indentified.
 *
 *
 * @author Conrad Plake
 */

public class IdentifyAllFilter implements Filter {

	/**
	 * Every unidentified entity gets identified with all possible candidate IDs.
	 * */
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {
		Iterator<RecognizedEntity> unidentifiedGeneNames = context.getUnidentifiedEntities().iterator();
		while (unidentifiedGeneNames.hasNext()) {
			//System.out.println("has a next unidentGene");
			RecognizedEntity recognizedGeneName = unidentifiedGeneNames.next();
			Set<String> candidateIds = context.getIdCandidates(recognizedGeneName);
			for (String id : candidateIds) {
				Gene gene = geneRepository.getGene(id);
				if (gene != null)
					context.identifyAsGene(recognizedGeneName, gene, 1.0f);
				//else
				//	System.out.println("gene is NULL");
			}
		}
	}

}
