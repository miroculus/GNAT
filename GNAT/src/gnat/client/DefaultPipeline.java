package gnat.client;

import gnat.ISGNProperties;
import gnat.filter.RunAdditionalFilters;
import gnat.filter.nei.BANNERValidationFilter;
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
import gnat.preprocessing.NameRangeExpander;
import gnat.representation.TextFactory;
import gnat.server.GnatService;
import gnat.server.GnatService.Tasks;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Pipeline for performing Gene Mention Normalization, which runs a fixed set of filters.
 * <br><br>
 * Uses GnatService for remote NER of species and genes, also obtaining candidate IDs, and
 * a remote GeneService to obtain information on particular genes.<br>
 * - To use the GnatService, the property <tt>gnatServiceUrl</tt> incl. a port has to be set in ISGNProperties (via its XML file).<br>
 * - To use the GeneService, the property <tt>geneRepositoryServer</tt> has to point to the address and port of such a service.<br>
 * <br>
 * 
 * TODO read the list of filters to load from ISGNProperties: 'pipeline' entry
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt and Martin Gerner;
 */
public class DefaultPipeline {

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
		
		// Pre-processing filter here:
		run.addFilter(new NameRangeExpander());
		
		// invoke the remote GnatService for NER, for species and gene names
		// - can be used together with RunDictionaries, see above
		// - normally, the DictionaryServers would be run for species that GnatServiceNer does not
		//   support, and vice versa; or if better DictionaryServers can be provided by the user and run locally
		// - also, both NER methods could be run for the same species, to complement each other
		GnatServiceNer gnatServiceNer = new GnatServiceNer(Tasks.SPECIES_NER, GnatService.Tasks.GENE_NER);
		gnatServiceNer.useDefaultSpecies = true;
		run.addFilter(gnatServiceNer);
		run.addFilter(new RecognizedEntityUnifier());
				
		// load the gene repository to obtain information on each gene (if only the species)
		// not loading gene repository will produce an empty result at the end
		run.addFilter(new GeneRepositoryLoader(GeneRepositoryLoader.RetrievalMethod.SERVICE));

		// include a few disambiguation filters that do not need specific information on each candidate gene
		// thus, these work on the gene's name and its context in the text
		run.addFilter(new BANNERValidationFilter());
		String f1 = new File("data/strictFPs_2_2_context_all.object").getAbsolutePath();
		String f2 = new File("data/nonStrictFPs_2_2_context_all.object").getAbsolutePath();
		run.addFilter(new LeftRightContextFilter(f1, f2, 0d, 2, 2));
		run.addFilter(new ImmediateContextFilter());
		run.addFilter(new StopWordFilter(ISGNProperties.get("stopWords")));
		run.addFilter(new UnambiguousMatchFilter());
		run.addFilter(new UnspecificNameFilter());
		run.addFilter(new NameValidationFilter());
		run.addFilter(new MultiSpeciesDisambiguationFilter(
				Integer.parseInt(ISGNProperties.get("disambiguationThreshold")),
				Integer.parseInt(ISGNProperties.get("maxIdsForCandidatePrediction"))));
		
		// include all Filters that are specified in data/runAdditionalFilters.txt
		// this setting (file name) can be changed in ISGNProperties, key=runAdditionalFilters
		// good for adding Filters at run-time
		// the current data/runAdditionalFilters.txt contains a DummyFilter that does nothing
		run.addFilter(new RunAdditionalFilters());
		
		// set all remaining genes as 'identified' so they will be reported in the result
		run.addFilter(new IdentifyAllFilter());
		
		// run all filters, changing run.context, run.textRepository, and run.geneRepository
		run.runFilters();
		
		// print the results for each text, in BioCreative tab-separated format
		List<String> result = run.context.getIdentifiedGeneList_SortedByTextAndId();
		for (String res: result)
			System.out.println(res);
	}
}