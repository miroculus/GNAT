package gnat.client;

import gnat.ISGNProperties;
import gnat.filter.RunAdditionalFilters;
import gnat.filter.nei.AlignmentFilter;
import gnat.filter.nei.GeneRepositoryLoader;
import gnat.filter.nei.IdentifyAllFilter;
import gnat.filter.nei.ImmediateContextFilter;
import gnat.filter.nei.LeftRightContextFilter;
import gnat.filter.nei.MultiSpeciesDisambiguationFilter;
import gnat.filter.nei.NameValidationFilter;
import gnat.filter.nei.RecognizedEntityUnifier;
import gnat.filter.nei.StopWordFilter;
import gnat.filter.nei.UnambiguousMatchFilter;
import gnat.filter.nei.UnspecificNameFilter;
import gnat.filter.ner.GnatServiceNer;
import gnat.filter.ner.LinnaeusSpeciesServiceNer;
import gnat.filter.ner.RunDictionaries;
import gnat.preprocessing.NameRangeExpander;
import gnat.representation.Text;
import gnat.representation.TextFactory;
import gnat.server.GnatService;
import gnat.utils.AlignmentHelper;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * A simple example pipeline for Gene Mention Normalization, which runs a fixed set of filters.
 * <br><br>
 * Uses GnatService for remote NER of species and genes, also obtaining candidate IDs, and
 * a remote GeneService to obtain information on particular genes.<br>
 * - To use the GnatService, the property <tt>gnatServiceUrl</tt> incl. a port has to be set in ISGNProperties (via its XML file).<br>
 * - To use the GeneService, the property <tt>geneRepositoryServer</tt> has to point to the address and port of such a service.<br>
 * <br>
 * 
 * TODO read the list of filters to load from ISGNProperties: 'pipeline' entry
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 */
public class BasicExamplePipeline {

	/**
	 * 
	 * @param args
	 */
	public static void main (String[] args) {
		Run run = new Run();
		run.verbosity = 2;

		List<String> directoriesToProcess = new LinkedList<String>();
		for (String arg: args) {
			if (arg.matches("\\-\\-?v(erbosity)?\\=\\d+")) {
				run.verbosity = Integer.parseInt(arg.replaceFirst("^\\-\\-?v(erbosity)?\\=(\\d+)$", "$2"));
			} else {
				File DIR = new File(arg);
				if (DIR.exists() && DIR.canRead())
					directoriesToProcess.add(arg);
				else
					System.out.println("Parameter '" + arg + "' is not a valid/readable file or directory; skipping.");
			}
		}

		// load all texts from the given directory/ies:
		if (directoriesToProcess.size() == 0) {
			System.err.println("Provide at least one file or directory with files (*.txt) to annotate.");
			System.exit(2);
		}

		// load texts from the given directories
		run.setTextRepository(TextFactory.loadTextRepositoryFromDirectories(directoriesToProcess));

		addExampleTexts(run);

		// Pre-processing filter here:
		run.addFilter(new NameRangeExpander());

		// SPECIES NER: either LINNAEUS or Bergman lab web service
		//		run.addFilter(new LinnaeusSpeciesServiceNer());
		run.addFilter(new GnatServiceNer(GnatService.Tasks.SPECIES_NER));

		// GENE NER: either local species-specific dictionary servers or Bergman lab web service
		{ // LOCAL DICT SERVER EXAMPLE

			// use dictionary servers for NER, as specified in properties and data/taxon2port.txt
			// runDictionaries.setExcludeTaxons and .setLimitedTo can be used to exclude or limit the set
			// of species used for the filter
			
			//		RunDictionaries runDictionaries = new RunDictionaries();
			//		Set<Integer> excludeTaxaFromDefaultDictionaries = new HashSet<Integer>();
			//		runDictionaries.setExcludeTaxons(excludeTaxaFromDefaultDictionaries);
			//		run.addFilter(runDictionaries);
		}
		
		{ // BERGMAN SERVICE EXAMPLE - USED BY DEFAULT
			// invoke the remote GnatService for NER, for species and gene names
			// - can be used together with RunDictionaries, see above
			// - normally, the DictionaryServers would be run for species that GnatServiceNer does not
			//   support, and vice versa; or if better DictionaryServers can be provided by the user and run locally
			// - also, both NER methods could be run for the same species, to complement each other
			GnatServiceNer gnatServiceNer = new GnatServiceNer(GnatService.Tasks.GENE_NER);

			// flag determining if the default species should be assumed if no species is mentioned in the article
			// the def species is set in ISGNProperties.xml; default is 9606 (human)
			gnatServiceNer.useDefaultSpecies = true;
			
			run.addFilter(gnatServiceNer);
		}

		// load the gene repository to obtain information on each gene (if only the species)
		// not loading gene repository will produce an empty result at the end
		run.addFilter(new GeneRepositoryLoader(GeneRepositoryLoader.RetrievalMethod.SERVICE));

		//FOR THIS VERY SIMPLE EXAMPLE, NO FILTERING OR NORMALIZATION FILTERS ARE APPLIED (FOR SAKE OF CLARITY)
		//FOR A MORE REALISTIC PIPELINE, SEE DEFAULTPIPELINE		
		
		// set all remaining genes as 'identified' so they will be reported in the result
		run.addFilter(new IdentifyAllFilter());

		// run all filters, changing run.context, run.textRepository, and run.geneRepository
		run.runFilters();

		// print the results for each text, in BioCreative tab-separated format
		List<String> result = run.context.getIdentifiedGeneList();
		for (String res: result)
			System.out.println(res);
	}

	static void addExampleTexts(Run run) {
		// add some individual mocked-up texts:
		// #1
		Text text = new Text("Test-1", "Selective TRAIL-triggered apoptosis due to overexpression of TRAIL death receptor 5 (DR5) in P-glycoprotein-bearing multidrug resistant CEM/VBL1000 human leukemia cells.");
		text.setPMID(20953314);
		// also add a species that is relevant to the text
		//		text.taxonIDs.add(9606);
		run.addText(text);

		// #2, without species
		run.addText(new Text("Test-2", "Fas and a simple test with FADD, both involved in apoptosis."));

		// #3, with two species, one (9606) is irrelevant for the gene 'Fas' mentioned here
		Text text2 = new Text("Test-3", "Another simple test with murine Fas.");
		//		text2.taxonIDs.add(9606);
		//		text2.taxonIDs.add(10090);
		run.addText(text2);

		// #3, with two species, one (9606) is irrelevant for the gene 'Fas' mentioned here
		Text text3 = new Text("Test-4", "Drosophila melanogaster p53.");
		//		text3.taxonIDs.add(7227);
		run.addText(text3);
	}
}