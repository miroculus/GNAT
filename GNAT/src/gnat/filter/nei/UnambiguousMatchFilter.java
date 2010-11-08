package gnat.filter.nei;

import gnat.ConstantsNei;
import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.Gene;
import gnat.representation.GeneRepository;
import gnat.representation.RecognizedEntity;
import gnat.representation.TextRepository;

import java.util.Iterator;
import java.util.Set;

/**
 * Identifies a recognized gene name if it has exactly one candidate ID (thus gene) assigned.
 * 
 * @author Conrad
 *
 */
public class UnambiguousMatchFilter implements Filter {

	/**
	 * 
	 */
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {
		Iterator<RecognizedEntity> unidentifiedGeneNames = context.getUnidentifiedEntities().iterator();
		while (unidentifiedGeneNames.hasNext())
		{
			RecognizedEntity recognizedGeneName = unidentifiedGeneNames.next();
			Set<String> idCandidates = context.getIdCandidates(recognizedGeneName);
			if(idCandidates.size()==1){
				Gene gene = geneRepository.getGene(idCandidates.iterator().next());
				if(gene!=null){
					context.identifyAsGene(recognizedGeneName, gene, 1.0f);
					
					if (ConstantsNei.OUTPUT_LEVEL == ConstantsNei.OUTPUT_LEVELS.SVM)
						System.out.println("0 " + (ConstantsNei.NUMBER_OF_SCORES+1) + ":1.0 #0 " + recognizedGeneName.getText().PMID + " " + gene.getID() + " " + recognizedGeneName.getName());
					else if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
						System.out.println("Identifying unambiguous match '" + recognizedGeneName.getName() + "' (" + gene.getID() + ") in text " + recognizedGeneName.getText().PMID);
				}
			}
		}

	}

}
