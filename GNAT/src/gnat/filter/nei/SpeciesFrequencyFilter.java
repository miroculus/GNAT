package gnat.filter.nei;

import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.IdentificationStatus;
import gnat.representation.RecognizedEntity;
import gnat.representation.Text;
import gnat.representation.TextRepository;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Crude filter that checks which taxa are discussed most frequently in a text
 * ({@link Text.getMostFrequentTaxons()}) and removes gene IDs that are not
 * associated with any of these. If the most freq. occ. species occurs more than 
 * twice as often as the species of the a gene candidate, removes that candidate.
 * 
 * @author Joerg Hakenberg
 */
public class SpeciesFrequencyFilter implements Filter {

	public void filter(Context context, TextRepository textRepository, GeneRepository geneRepository) {
		
		// get all yet unidentified genes
		Iterator<RecognizedEntity> unidentifiedGeneNames = context.getUnidentifiedEntities().iterator();
		while (unidentifiedGeneNames.hasNext()) {
			RecognizedEntity recognizedGeneName = unidentifiedGeneNames.next();
			Text text = recognizedGeneName.getText();
			
			Set<Integer> mostFrequentTaxa = text.getMostFrequentTaxons();
			// nothing can be done...
			if (mostFrequentTaxa.size() == 0)
				continue;
			
			int highestFrequency = text.getTaxonFrequency(mostFrequentTaxa.iterator().next());

			// debug info
			//if (text.getID().matches(".*(10833452).*")) {
			//	System.err.println("# Text " + text.getID() + ": most freq taxon(s)=" + mostFrequentTaxa
			//			+ " (" + highestFrequency + ")");
			//}
			
			// get the IdStatus to obtain all candidate IDs
			IdentificationStatus identificationStatus = context.getIdentificationStatus(recognizedGeneName);
			Set<String> geneIdCandidates = identificationStatus.getIdCandidates();
			TreeSet<String> removeGeneIds = new TreeSet<String>();
			// iterate over all candidate IDs, get taxon ID, check if it's among the most freq ones, otherwise mark for deletion
			Iterator<String> it = geneIdCandidates.iterator();
			while (it.hasNext()) {
				String gid = it.next();
				
				int currentTax = geneRepository.getGene(gid).getTaxon();
				if (currentTax == -1) // TODO observed some taxon=-1 in results output, x-check if they are all wrong
					continue;
				
				// if the most freq. occ. species occurs more than twice as often as 
				// the species of the current gene candidate, remove the candidate
				if (!mostFrequentTaxa.contains(currentTax)) {
					int myFrequency = text.getTaxonFrequency(currentTax);
					if ((float)highestFrequency / 2.0f > (float)myFrequency)
						removeGeneIds.add(gid);
				}
			}
			
			// remove all IDs associated with less frequently occurring taxa
			for (String remove: removeGeneIds)
				identificationStatus.removeIdCandidate(remove);
		}
		
	}

}
