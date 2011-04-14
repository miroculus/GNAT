package gnat.tests;

import gnat.ISGNProperties;
import gnat.client.Run;
import gnat.filter.nei.AlignmentFilter;
import gnat.filter.nei.GeneRepositoryLoader;
import gnat.filter.nei.IdentifyAllFilter;
import gnat.filter.nei.ImmediateContextFilter;
import gnat.filter.nei.LeftRightContextFilter;
import gnat.filter.nei.RecognizedEntityUnifier;
import gnat.filter.nei.StopWordFilter;
import gnat.filter.nei.UnambiguousMatchFilter;
import gnat.filter.nei.UnspecificNameFilter;
import gnat.filter.ner.GnatServiceNer;
import gnat.filter.ner.LinnaeusSpeciesServiceNer;
import gnat.preprocessing.NameRangeExpander;
import gnat.representation.TextFactory;
import gnat.server.GnatService;
import gnat.utils.AlignmentHelper;

import java.util.List;


/**
 * Tests a pipeline in which the steps for named entity recognition (GnatServiceNer 
 * and remove Linnaeus instance) as well as the Gene Repository are hosted on
 * remote servers. Disambiguation runs locally.
 * 
 * 
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 */
public class RemoteNerGeneRepTest extends PipelineTest {

	
	public static void main (String[] args) {
		System.out.println("Testing a pipeline where NER (species: Linnaeus; genes, GO terms: GnatService) runs remotely, " +
				" the gene repository is obtained via a remote GeneRepositoryService, and disambiguation runs locally.");
		
		if (!testConfiguration())
			System.exit(2);
		
		Run run = new Run();
		run.verbosity = 0;
		
		//
		if (!testConfiguration())
			System.exit(2);
			
		// load all texts required for the test
		run.setTextRepository(TextFactory.loadTextRepositoryFromDirectories("texts/test"));
		
		//
		run.setFilenamesAsPubMedId();

		// Pre-processing filter here:
		run.addFilter(new NameRangeExpander());

		// NER filters here:
		// Linnaeus for species NER
		run.addFilter(new LinnaeusSpeciesServiceNer());
		// genes and GO terms via a GnatService
		GnatServiceNer gnatServiceNer = new GnatServiceNer(GnatService.Tasks.GENE_NER, GnatService.Tasks.GO_TERMS);
		// tell the remote service to run only for a few species:
		gnatServiceNer.setLimitedToTaxa(9606, 10090, 7227, 10116); // use for human, murine, fruit fly, and rat genes
		gnatServiceNer.useDefaultSpecies = true;
		run.addFilter(gnatServiceNer);
		
		// NER post-processing filters here:
		run.addFilter(new RecognizedEntityUnifier());

		// include a few disambiguation filters that do not need specific information on each candidate gene
		// thus, these work on the gene's name and its context in the text
		run.addFilter(new ImmediateContextFilter());
		
		// strictFPs_2_2_context_all.object contains data on the context defined by two tokens left and two tokens right of a gene name
		run.addFilter(new LeftRightContextFilter("data/strictFPs_2_2_context_all.object", "data/nonStrictFPs_2_2_context_all.object", 0d, 2, 2));

		// load the gene repository to obtain information on each gene (if only the species)
		// not loading gene repository will produce an empty result at the end
		run.addFilter(new GeneRepositoryLoader(GeneRepositoryLoader.RetrievalMethod.SERVICE));

		//
		run.addFilter(new StopWordFilter(ISGNProperties.get("stopWords")));
		//
		run.addFilter(new UnambiguousMatchFilter());
		//
		run.addFilter(new UnspecificNameFilter());

		//
		run.addFilter(new AlignmentFilter(AlignmentHelper.globalAlignment, 0.7f));
		
		//
		run.addFilter(new IdentifyAllFilter());
		
		// run all filters, changing run.context, run.textRepository, and run.geneRepository
		run.runFilters();
				
		// get the results for each text, in BioCreative tab-separated format
		List<String> result = run.context.getIdentifiedGeneList_SortedByTextAndId();
		// get expected results from saved file
		List<String> expected = getExpectedOutput("texts/test/test.solution");
		
		// compare tested and expected results
		int foundInTest = 0;
		for (String exp: expected) {
			if (result.contains(exp))
				foundInTest++;
			else
				System.out.println("#Not found in result: '" + exp + "'");
		}
		
		if (foundInTest == expected.size() && expected.size() == result.size()) {
			System.out.println("Result:");
			for (String res: result)
				System.out.println(res);
			System.out.println("\nTest okay!");
			
		} else {
			System.out.println("\nTested and expected results differ!");
			System.out.println("Expected result:");
			for (String exp: expected)
				System.out.println(exp);
			System.out.println("-----\nResult from test:");
			for (String res: result)
				System.out.println(res);
		}
		
	}
	
	
	
	/**
	 * Checks the settings and connections to a GnatService.
	 * 
	 * @return
	 */
	static boolean testConfiguration () {
		boolean configOk = true;
		
		// test GNAT Service URL
		String gnatServiceUrl = ISGNProperties.get("gnatServiceUrl");
		if (gnatServiceUrl == null || gnatServiceUrl.length() == 0) {
			System.err.println("Configuration file " + ISGNProperties.getPropertyFilename() + ":\nSet a value " +
				"for the entry 'gnatServiceUrl'.");
			configOk = false;
		}
		
		// test Gene Service URL
		String geneServiceUrl = ISGNProperties.get("geneRepositoryService");
		if (geneServiceUrl == null || geneServiceUrl.length() == 0) {
			System.err.println("Configuration file " + ISGNProperties.getPropertyFilename() + ":\nSet a value " +
				"for the entry 'geneRepositoryService'.");
			configOk = false;
		}

		return configOk;
	}
}
