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
import gnat.representation.IdentifiedGene;
import gnat.representation.TextFactory;
import gnat.server.GnatService;
import gnat.server.GnatService.Tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import martin.common.ArgParser;
import uk.ac.man.documentparser.input.DocumentIterator;

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
		ArgParser ap = new ArgParser(args);
		ap.addAlternate("verbosity", "v");

		if (args.length == 0 || ap.containsKey("help")){
			System.out.println("Usage: [--text <text file>] [--textDir <.txt file directory] [--recursirve] --out <output file> [--verbosity <level>]");
			System.exit(0);
		}

		DocumentIterator documents = uk.ac.man.documentparser.DocumentParser.getDocuments(ap, null);

		Run run = new Run();
		run.verbosity = ap.getInt("verbosity", 2);

		// load texts from the given directories
		run.setTextRepository(TextFactory.loadTextRepository(documents));

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

		try{
			File outFile = new File(ap.getRequired("out"));
			BufferedWriter outStream = new BufferedWriter(new FileWriter(outFile));

//			for (String res: result)
//				outStream.write(res + "\n");
			
			for (IdentifiedGene ig : run.context.getEntitiesIdentifiedAsGene()){
				outStream.write(ig.getRecognizedEntity().getText().getID() + "\t");
				outStream.write(ig.getGene().getID() + "\t");
				outStream.write(""+ig.getRecognizedEntity().getBegin() + "\t");
				outStream.write(""+ig.getRecognizedEntity().getEnd() + "\t");
				outStream.write(ig.getRecognizedEntity().getName() + "\t");
				outStream.write(ig.getGene().getTaxon());
				outStream.write("\n");
			}
			
			outStream.close();
		} catch (Exception e){
			System.err.println(e);
			e.printStackTrace();
			System.exit(-1);
		}
	}
}