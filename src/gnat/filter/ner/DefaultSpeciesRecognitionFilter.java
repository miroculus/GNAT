package gnat.filter.ner;

import gnat.ConstantsNei;
import gnat.ISGNProperties;
import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.Text;
import gnat.representation.TextRepository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A filter that scans texts for species and assigns NCBI Taxonomy IDs; default implementation,
 * scans for human, mouse, rat, and fruit fly only.
 * <br><br>
 * Taxon IDs will be stored in each text's {@link Text#taxonIDs} field 
 * and thus not be annotated with positions.<br>In addition, the names found in a text representing
 * a species will be accessible via {@link Text#taxonIdsToNames}.
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 *
 */
public class DefaultSpeciesRecognitionFilter implements Filter {

	/** Use default species instead of actually running the NER for species. For debug purposes mostly. */
	public boolean useAllDefaultSpecies = false;
	
	
	/**
	 * 
	 */
	public DefaultSpeciesRecognitionFilter () {
	}
	

	/**
	 * Processes all texts in the text repository by calling the remote server and adds the outcome,
	 * i.e., recognized entities and candidate ids, to the context.
	 */
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {
		// store all taxon IDs ever found
		Set<Integer> allTaxonIDs = new TreeSet<Integer>();
		
		for (Text text : textRepository.getTexts()) {
			Set<Integer> taxonIDs = new TreeSet<Integer>();
			
			if (useAllDefaultSpecies) {
				taxonIDs = ISGNProperties.getDefaultSpecies();
				text.setTaxonIDs(taxonIDs);
				allTaxonIDs.addAll(taxonIDs);
				continue;
			}
			
			Map<Integer, Set<String>> id2names = new HashMap<Integer, Set<String>>();
			String[] lines = text.getPlainText().split("[\\r\\n]");
			for (String line: lines) {
				if (line.toLowerCase().matches(".*(?:^|[\\W])" +
						"(human|man|patient|h(\\.|omo)\\ssap(\\.|iens))" +
						"(?:[\\W]|$).*")) {
					String name = line.replaceFirst("^.*(?:^|[\\W])" +
							"(human|man|patient|h(\\.|omo)\\ssap(\\.|iens))" +
							"(?:[\\W]|$).*$", "$1");
					Set<String> names;
					if (id2names.containsKey(9606))
						names = id2names.get(9606);
					else
						names = new TreeSet<String>();
					names.add(name);
					id2names.put(9606, names);
				}
				
				if (line.toLowerCase().matches(".*(?:^|[\\W])" +
						"(mouse|mice|murine|m(\\.|us)\\smus(\\.|culus)?)" +
						"(?:[\\W]|$).*")) {
					String name = line.replaceFirst("^.*(?:^|[\\W])" +
							"(mouse|mice|murine|m(\\.|us)\\smus(\\.|culus)?)" +
							"(?:[\\W]|$).*$", "$1");
					Set<String> names;
					if (id2names.containsKey(10090))
						names = id2names.get(10090);
					else
						names = new TreeSet<String>();
					names.add(name);
					id2names.put(10090, names);
					
					//System.err.println("###Found mouse###");
				}
				
				if (line.toLowerCase().matches(".*(?:^|[\\W])" +
						"(rat|rats|r(\\.|attus)\\snor(\\.||v\\.|vegicus)?)" +
						"(?:[\\W]|$).*")) {
					String name = line.replaceFirst("^.*(?:^|[\\W])" +
							"(rat|rats|r(\\.|attus)\\snor(\\.||v\\.|vegicus)?)" +
							"(?:[\\W]|$).*$", "$1");
					Set<String> names;
					if (id2names.containsKey(10116))
						names = id2names.get(10116);
					else
						names = new TreeSet<String>();
					names.add(name);
					id2names.put(10116, names);
				}
				
				if (line.toLowerCase().matches(".*(?:^|[\\W])" +
						"(rodents?)" +
						"(?:[\\W]|$).*")) {
					String name = line.replaceFirst("^.*(?:^|[\\W])" +
							"(rodents?)" +
							"(?:[\\W]|$).*$", "$1");
					Set<String> names;
					if (id2names.containsKey(10090))
						names = id2names.get(10090);
					else
						names = new TreeSet<String>();
					names.add(name);
					id2names.put(10090, names);
					
					if (id2names.containsKey(10116))
						names = id2names.get(10116);
					else
						names = new TreeSet<String>();
					names.add(name);
					id2names.put(10116, names);
				}
				
				if (line.toLowerCase().matches(".*(?:^|[\\W])" +
						"(fruit[\\s\\-]?fly|flies|fly|d(\\.|rosophilae?)|d(\\.|rosophila)\\smel(\\.|anogaster))" +
						"(?:[\\W]|$).*")) {
					String name = line.replaceFirst("^.*(?:^|[\\W])" +
							"(fruit[\\s\\-]?fly|flies|fly|d(\\.|rosophilae?)|d(\\.|rosophila)\\smel(\\.|anogaster))" +
							"(?:[\\W]|$).*$", "$1");
					Set<String> names;
					if (id2names.containsKey(7227))
						names = id2names.get(7227);
					else
						names = new TreeSet<String>();
					names.add(name);
					id2names.put(10116, names);
				}
			}
			

			// assign default IDs if none where found in the text
			if (taxonIDs.size() == 0) {
				taxonIDs = ConstantsNei.DEFAULT_SPECIES;
				if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
					System.out.println("#SRF: no taxIds found for " + text.PMID + ". Using default: " + taxonIDs);
			}
			
			text.setTaxonIDs(taxonIDs);
			text.addTaxonToNameMap(id2names);
			if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
				System.out.println("#SRF: for text " + text.PMID + ", found species " + taxonIDs);
			
			allTaxonIDs.addAll(taxonIDs);
		}
		
		if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.STATUS) >= 0)
			System.out.println("#SRF: recognized species in the text collection: " + allTaxonIDs);
	}

}
