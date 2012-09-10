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
	
	/** Defines the left side of a valid species name in a sentence: typically, a non-alphanumeric
	 *  character such as bracket or white space immmediately to the left, and any number of 
	 *  characters to the left till the sentence start. Has to start with "^" to match the sentence
	 *  start. */
	static final String PATTERN_LEFT  = "^.*(?:^|[\\W])";
	/** Defines the right side of a valid species name in a sentence: typically, a non-alphanumeric
	 *  character such as bracket or white space immmediately to the right, and any number of 
	 *  characters to the right till the end of the sentence. Has to end with "$" to match the sentence
	 *  end. */
	static final String PATTERN_RIGHT = "(?:[\\W]|$).*$";
	
	/** Patterns that match certain species names in a sentence. As a convention,
	 *  matching group 1 for the sentence has to return the matched species name.
	 *  All other parenthesis should be marked such that they to not denote matching
	 *  groups, using the markup "?:" as in "(?:...)".<br>
	 *  The patterns also have to match from the sentence start to the end and 
	 *  therefore always have to start with "^" and end with "$". Make use of 
	 *  {@link #PATTERN_LEFT} and {@link #PATTERN_RIGHT} to surround the species names
	 *  with valid left/right sides that also match to the sentence start/end. */
	static final String PATTERN_YEAST = PATTERN_LEFT +
			"(" +
			"[Yy]east|baker's yeast" +
			"|Saccharomyces cerevisiae S288c|Saccharomyces cerevisiae" +
			"|[Ss]\\.\\s?[Cc]er(?:\\.|evisiae)?" +
			"|ferments?" +
			")" +
			PATTERN_RIGHT;
	static final String PATTERN_HUMAN = PATTERN_LEFT +
			"(" +
			"[Hh]umans?|man|patients?|cohorts?|[Hh](\\.|omo)\\ssap(\\.|iens)" +
			// human cell lines
			"|(?:HTB-134|HT-29" + 
			"|293 cells?|HEK[\\-\\s]?293|293T|T47D|T-47D|Ramsey" +
			// typically human diseases
			"|breast cancer)" +
			// generalization: mammals = humans in Medline
			"|(?:mammalian cells?|mammalian|mammals|vertebrate homolog[a-z]*)" +
			")" +
			PATTERN_RIGHT;
	static final String PATTERN_MOUSE = PATTERN_LEFT +
			"(" +
			"[Mm]ouse|[Mm]ice|[Mm]urine|[Mm](?:\\.|us)\\s[Mm]us(?:\\.|culus)?" +
			// strains
			"|C57BL|C3H" + 
			// murine cell lines
			"|3T3|NIH\\-3T3|STO|Yac\\-1|CTLL\\-2|EGG" + 
			")" + 
			PATTERN_RIGHT;
	static final String PATTERN_RAT = PATTERN_LEFT +
			"([Rr]at|[Rr]ats|[Rr](\\.|attus)\\s[Nn]or(\\.||v\\.|vegicus)?)" +
			PATTERN_RIGHT;
	static final String PATTERN_FRUITFLY = PATTERN_LEFT +
			"(" +
			"[Ff]ruit[\\s\\-]?[Ff]ly|[Ff]lies|[Ff]ly" +
			"|[Dd](?:\\.|rosophilae?)|[Dd](?:\\.|rosophila)\\s[Mm]el(\\.|anogaster)" +
			")" +
			PATTERN_RIGHT;

	// species-specific gene prefixes
	// h, H - Homo sapiens
	// m - Mus musculus
	// d - Drosophila
	// Hp - Hansenula polymorpha (yeast)
	// At, Atp, Atb - Arabidopsis thaliana - AtRCE1, AtpDCT2, AtbZIP3
	// Cn - Cryptococcus neoformans (fungal pathogen) - CnEF3
	// Hw - Hortaea werneckii (black yeast) - HwSHO1A
	// Hv - Hordeum vulgare L. (barley) - HvPIP1;6
	// y - yeast (mostly S.cer.) - yBR101cp
	// Bm - Bomby mori (silkworm/moth/insect) - BmMBP
	// others:
	// Avr - avirulence
	// rp, Rp - ribosomal protein - RP17B
	
	
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
			
			boolean debug = ConstantsNei.verbosityAtLeast(ConstantsNei.OUTPUT_LEVELS.DEBUG);
			
			boolean foundAspecies = false;
			
			Map<Integer, List<String>> id2names = new HashMap<Integer, List<String>>();
			String[] lines = text.getPlainText().split("[\r\n]+");
			for (String line: lines) {
				while (line.matches(PATTERN_HUMAN)) {
					String name = line.replaceFirst(PATTERN_HUMAN, "$1");
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
				
				while (line.matches(PATTERN_MOUSE)) {
					String name = line.replaceFirst(PATTERN_MOUSE, "$1");
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
				
				while (line.matches(PATTERN_RAT)) {
					String name = line.replaceFirst(PATTERN_RAT, "$1");
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
				
				while (line.matches(PATTERN_FRUITFLY)) {
					String name = line.replaceFirst(PATTERN_FRUITFLY, "$1");
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
				while (line.matches(PATTERN_YEAST)
						&& !line.matches(".*((two|2|to|1|one|3|three)[\\s\\-]?hybrid|Y2H|YS2H).*")
						&& !line.matches(".*yeast homolog.*")) {
					String name = line.replaceFirst(PATTERN_YEAST, "$1");
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
				if (debug)
					System.out.println("#DSRF: no taxIds found for " + text.PMID + ". Using default: " + taxonIDs);
			}
			
			text.setTaxonIDs(taxonIDs);
			text.addTaxonToNameMap(id2names);
			if (debug)
				System.out.println("#DSRF: for text " + text.PMID + ", found species " + taxonIDs);
			
			allTaxonIDs.addAll(id2names.keySet());
		} // for each text
		
		
		if (ConstantsNei.verbosityAtLeast(ConstantsNei.OUTPUT_LEVELS.STATUS))
			System.out.println("#SRF: recognized species in the text collection: " + allTaxonIDs);
	}

}
