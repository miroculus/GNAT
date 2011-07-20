package gnat.filter.ner;

import gnat.ConstantsNei;
import gnat.ISGNProperties;
import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.Text;
import gnat.representation.TextRepository;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A filter that scans texts for species and assigns NCBI Taxonomy IDs; default implementation,
 * scans for human, mouse, rat, yeast (S.cer), and fruit fly only.
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
			
			boolean debug = false;//text.getID().matches(".*(7498728|10833452).*");
			
			boolean foundAspecies = false;
			
			Map<Integer, List<String>> id2names = new HashMap<Integer, List<String>>();
			String[] lines = text.getPlainText().split("[\r\n]+");
			for (String line: lines) {
				while (line.matches(".*(?:^|[\\W])" +
						"([Hh]uman|man|patient|[Hh](\\.|omo)\\ssap(\\.|iens))" +
						"(?:[\\W]|$).*")) {
					String name = line.replaceFirst("^.*(?:^|[\\W])" +
							"([Hh]uman|man|patient|[Hh](\\.|omo)\\ssap(\\.|iens))" +
							"(?:[\\W]|$).*$", "$1");
					String replace = "x";
					for (int r = 1; r < name.length(); r++)	replace += "x";
					line = line.replaceFirst(name, replace);
					
					List<String> names;
					if (id2names.containsKey(9606))
						names = id2names.get(9606);
					else
						names = new LinkedList<String>();
					names.add(name);
					id2names.put(9606, names);
					
					if (debug) System.err.println("### Found human: '" + name + "' in text " + text.getID());
					foundAspecies = true;
				}
				
				while (line.matches(".*(?:^|[\\W])" +
						"([Mm]ouse|[Mm]ice|[Mm]urine|[Mm](\\.|us)\\s[Mm]us(\\.|culus)?)" +
						"(?:[\\W]|$).*")) {
					String name = line.replaceFirst("^.*(?:^|[\\W])" +
							"([Mm]ouse|[Mm]ice|[Mm]urine|[Mm](\\.|us)\\s[Mm]us(\\.|culus)?)" +
							"(?:[\\W]|$).*$", "$1");
					String replace = "x";
					for (int r = 1; r < name.length(); r++)	replace += "x";
					line = line.replaceFirst(name, replace);
					
					List<String> names;
					if (id2names.containsKey(10090))
						names = id2names.get(10090);
					else
						names = new LinkedList<String>();
					names.add(name);
					id2names.put(10090, names);
					
					if (debug) System.err.println("###Found mouse: '" + name + "' in text " + text.getID());
					foundAspecies = true;
				}
				
				while (line.matches(".*(?:^|[\\W])" +
						"([Rr]at|[Rr]ats|[Rr](\\.|attus)\\s[Nn]or(\\.||v\\.|vegicus)?)" +
						"(?:[\\W]|$).*")) {
					String name = line.replaceFirst("^.*(?:^|[\\W])" +
							"([Rr]at|[Rr]ats|[Rr](\\.|attus)\\s[Nn]or(\\.||v\\.|vegicus)?)" +
							"(?:[\\W]|$).*$", "$1");
					String replace = "x";
					for (int r = 1; r < name.length(); r++)	replace += "x";
					line = line.replaceFirst(name, replace);
					
					List<String> names;
					if (id2names.containsKey(10116))
						names = id2names.get(10116);
					else
						names = new LinkedList<String>();
					names.add(name);
					id2names.put(10116, names);
					
					if (debug) System.err.println("###Found rat: '" + name + "' in text " + text.getID());
					foundAspecies = true;
				}
				
				while (line.matches(".*(?:^|[\\W])" +
						"([Rr]odents?)" +
						"(?:[\\W]|$).*")) {
					String name = line.replaceFirst("^.*(?:^|[\\W])" +
							"([Rr]odents?)" +
							"(?:[\\W]|$).*$", "$1");
					String replace = "x";
					for (int r = 1; r < name.length(); r++)	replace += "x";
					line = line.replaceFirst(name, replace);
					
					List<String> names;
					if (id2names.containsKey(10090))
						names = id2names.get(10090);
					else
						names = new LinkedList<String>();
					names.add(name);
					id2names.put(10090, names);
					
					if (id2names.containsKey(10116))
						names = id2names.get(10116);
					else
						names = new LinkedList<String>();
					names.add(name);
					id2names.put(10116, names);
					
					if (debug) System.err.println("###Found rodent: '" + name + "' in text " + text.getID());
					foundAspecies = true;
				}
				
				while (line.matches(".*(?:^|[\\W])" +
						"([Ff]ruit[\\s\\-]?[Ff]ly|[Ff]lies|[Ff]ly|[Dd](\\.|rosophilae?)|[Dd](\\.|rosophila)\\s[Mm]el(\\.|anogaster))" +
						//"(drosophila)" +
						"(?:[\\W]|$).*")) {
					String name = line.replaceFirst("^.*(?:^|[\\W])" +
							"([Ff]ruit[\\s\\-]?[Ff]ly|[Ff]lies|[Ff]ly|[Dd](\\.|rosophilae?)|[Dd](\\.|rosophila)\\s[Mm]el(\\.|anogaster))" +
							//"(drosophila)" +
							"(?:[\\W]|$).*$", "$1");
					String replace = "x";
					for (int r = 1; r < name.length(); r++)	replace += "x";
					line = line.replaceFirst(name, replace);
					
					List<String> names;
					if (id2names.containsKey(7227))
						names = id2names.get(7227);
					else
						names = new LinkedList<String>();
					names.add(name);
					id2names.put(7227, names);
					
					if (debug) System.err.println("###Found fly: '" + name + "' in text " + text.getID());
					foundAspecies = true;
				}
				

				// Saccharomyces cerevisiae S288c
				while (line.matches(".*(?:^|[\\W])" +
						"(baker's yeast|[Yy]east" +
						"|Saccharomyces cerevisiae S288c|Saccharomyces cerevisiae" +
						"|[Ss]\\.\\s?[Cc]er(\\.|evisiae)?)" +
						"(?:[\\W]|$).*")) {
					String name = line.replaceFirst("^.*(?:^|[\\W])" +
							"([Yy]east|baker's yeast|Saccharomyces cerevisiae|[Ss]\\.\\s?[Cc]er(\\.|evisiae)?)" +
							"(?:[\\W]|$).*$", "$1");
					String replace = "x";
					for (int r = 1; r < name.length(); r++)	replace += "x";
					line = line.replaceFirst(name, replace);
					
					List<String> names;
					if (id2names.containsKey(559292))
						names = id2names.get(559292);
					else
						names = new LinkedList<String>();
					names.add(name);
					id2names.put(559292, names);
					
					if (debug) System.err.println("###Found yeast: '" + name + "' in text " + text.getID());
					foundAspecies = true;
				}
				
			} // for each line
			
			if (!foundAspecies && debug) {
				System.err.println("### No species found in text " + text.getID());
				for (String l: lines)
					System.err.println(">>>" + l + "<<<");
			} else if (debug) {
				System.err.println("# Species/names in the text: ");
				for (int t: id2names.keySet()) {
					System.err.println("#  taxon="+t + ", names="+id2names.get(t));
				}
			}
			
			//System.out.println("Species IDs: " + id2names.keySet());
			//for (int taxon: id2names.keySet()) {
			//	System.out.println("  names for " + taxon + ": " + id2names.get(taxon));
			//}
			
			taxonIDs.clear();
			taxonIDs.addAll(id2names.keySet()); //can't just do taxonIDs = id2names.keySet() since we then can't add taxons afterwards if necessary

			// assign default IDs if none where found in the text
			if (taxonIDs.size() == 0) {
				taxonIDs = ConstantsNei.DEFAULT_SPECIES;
				if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
					System.out.println("#DSRF: no taxIds found for " + text.PMID + ". Using default: " + taxonIDs);
			}
			
			text.setTaxonIDs(taxonIDs);
			text.addTaxonToNameMap(id2names);
			if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
				System.out.println("#DSRF: for text " + text.PMID + ", found species " + taxonIDs);
			
			allTaxonIDs.addAll(id2names.keySet());
		} // for each text
		
		
		if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.STATUS) >= 0)
			System.out.println("#SRF: recognized species in the text collection: " + allTaxonIDs);
	}

}
