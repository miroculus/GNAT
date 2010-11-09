package gnat.client;

import gnat.filter.RunAdditionalFilters;
import gnat.filter.nei.GeneRepositoryLoader;
import gnat.filter.nei.IdentifyAllFilter;
import gnat.filter.nei.ImmediateContextFilter;
import gnat.filter.ner.GnatServiceNer;
import gnat.filter.ner.RunDictionaries;
import gnat.filter.ner.DefaultSpeciesRecognitionFilter;
import gnat.representation.Text;
import gnat.representation.TextFactory;
import gnat.server.GnatService;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * An examplary pipeline for Gene Mention Normalization, which runs a fixed set of filters.
 * <br><br>
 * Uses GnatService for remote NER of species and genes, also obtaining candidate IDs, and
 * a remote GeneService to obtain information on particular genes.<br>
 * - To use the GnatService, the property <tt>gnatServiceUrl</tt> incl. a port has to be set in ISGNProperties (via its XML file).<br>
 * - To use the GeneService, the property <tt>geneRepositoryServer</tt> has to point to the address and port of such a service.<br>
 * <br>
 * 
 * TODO read the list of filters to load from ISGNProperties: 'pipeline' entry
 * 
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 */
public class DefaultPipe {

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
			
		// add some individual mocked-up texts:
		// #1
		Text text = new Text("Test-1", "Selective TRAIL-triggered apoptosis due to overexpression of TRAIL death receptor 5 (DR5) in P-glycoprotein-bearing multidrug resistant CEM/VBL1000 human leukemia cells.");
		text.setPMID(20953314);
		// also add a species that is relevant to the text
		text.taxonIDs.add(9606);
		run.addText(text);
		
		// #2, without species
		run.addText(new Text("Test-2", "Fas and a simple test with FADD, both involved in apoptosis."));
		
		// #3, with two species, one (9606) is irrelevant for the gene 'Fas' mentioned here
		Text text2 = new Text("Test-3", "Another simple test with murine Fas.");
		text2.taxonIDs.add(9606);
		text2.taxonIDs.add(10090);
		run.addText(text2);
		
		// include NER for species names
		//run.filterPipeline.add(new SpeciesServiceNer());

		// EXAMPLE:		
		// use dictionary servers for NER, as specified in properties and data/taxon2port.txt
		// except for species 9606, for which another Filter would have be provided
//		RunDictionaries runDictionaries = new RunDictionaries();
//		Set<Integer> excludeTaxaFromDefaultDictionaries = new HashSet<Integer>();
//		excludeTaxaFromDefaultDictionaries.add(9606);
//		runDictionaries.setExcludeTaxons(excludeTaxaFromDefaultDictionaries);
//		run.addFilter(runDictionaries);
		
		// run local or remote DictionaryServers
		// can be used together with GnatServiceNer, see below
		RunDictionaries runDictionaries = new RunDictionaries();
		// run DictionaryServers for these species only:
		runDictionaries.addLimitToTaxon(7227);  // D. mel
		runDictionaries.addLimitToTaxon(10116); // R. norv
		run.addFilter(runDictionaries);

		//
		run.addFilter(new DefaultSpeciesRecognitionFilter());
		// invoke the remote GnatService for NER, for species and gene names
		// - can be used together with RunDictionaries, see above
		// - normally, the DictionaryServers would be run for species that GnatServiceNer does not
		//   support, and vice versa; or if better DictionaryServers can be provided by the user and run locally
		// - also, both NER methods could be run for the same species, to complement each other
		GnatServiceNer gnatServiceNer = new GnatServiceNer(GnatService.Tasks.SPECIES_NER, GnatService.Tasks.GENE_NER);
		// tell the remote service to run only for a few species:
		gnatServiceNer.setLimitedToTaxa(9606, 10090); // H. sap, M. mus
		// tell the remote service to never run for some species: D. mel, R. norv
		// these are covered by the DictionaryServers above, but as mentioned, we could keep & run them again
		gnatServiceNer.setExcludeTaxa(7227, 10116);
		// if no species was assigned to a text, don't run any NER, not even for default species
		// default species can be set in ISGNProperties, and will be used if this switch is 'true'
		//   key=defaultSpecies
		gnatServiceNer.useDefaultSpecies = false;
		//gnatServiceNer.useDefaultSpecies = true;
		run.addFilter(gnatServiceNer);
				
		// include all Filters that are specified in data/runAdditionalFilters.txt
		// this setting (file name) can be changed in ISGNProperties, key=runAdditionalFilters
		// good for adding Filters at run-time
		// the current data/runAdditionalFilters.txt contains a DummyFilter that does nothing
		run.addFilter(new RunAdditionalFilters());
		
		// include a few disambiguation filters that do not need specific information on each candidate gene
		// thus, these work on the gene's name and its context in the text
		run.addFilter(new ImmediateContextFilter());
		
		// load the gene repository to obtain information on each gene (if only the species)
		// not loading gene repository will produce an empty result at the end
		run.addFilter(new GeneRepositoryLoader(GeneRepositoryLoader.RetrievalMethod.SERVICE));
		
		// add disambiguation filters here that need information on the genes, such as species, GO terms, ...
		// ...
		
		// set all remaining genes as 'identified' so they will be reported in the result
		run.addFilter(new IdentifyAllFilter());
		
		// run all filters, changing run.context, run.textRepository, and run.geneRepository
		run.runFilters();
		
		// print the results for each text, in BioCreative tab-separated format
		List<String> result = run.context.getIdentifiedGeneList();
		for (String res: result)
			System.out.println(res);
	}
	
}
