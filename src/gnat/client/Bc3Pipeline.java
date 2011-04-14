package gnat.client;

import gnat.ISGNProperties;
import gnat.filter.nei.AlignmentFilter;
import gnat.filter.nei.GeneRepositoryLoader;
import gnat.filter.nei.IdentifyAllFilter;
import gnat.filter.nei.ImmediateContextFilter;
import gnat.filter.nei.LeftRightContextFilter;
import gnat.filter.nei.MultiSpeciesDisambiguationFilter;
import gnat.filter.nei.NameValidationFilter;
import gnat.filter.nei.RecognizedEntityUnifier;
import gnat.filter.nei.SpeciesValidationFilter;
import gnat.filter.nei.StopWordFilter;
import gnat.filter.nei.UnambiguousMatchFilter;
import gnat.filter.nei.UnspecificNameFilter;
import gnat.filter.ner.GOFilter;
import gnat.filter.ner.GnatServiceNer;
import gnat.filter.ner.DefaultSpeciesRecognitionFilter;
import gnat.filter.ner.LinnaeusSpeciesServiceNer;
import gnat.preprocessing.NameRangeExpander;
import gnat.representation.TextFactory;
import gnat.server.GnatService;
import gnat.utils.AlignmentHelper;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * A pipeline of processing filters that resembles the one used in BioCreative3.
 * 
 * <br><br>
 * TODO missing: the LINNAEUS input
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 */
public class Bc3Pipeline {
	public static void main (String[] args) {
		Run run = new Run();
		run.verbosity = 2;
		
		List<String> directoriesToProcess = new LinkedList<String>();
		for (String arg: args) {
			if (arg.matches("\\-\\-?v(erbosity)?\\=\\d+")) {
				run.verbosity = Integer.parseInt(arg.replaceFirst("^\\-\\-?v(erbosity)?\\=(\\d+)$", "$2"));
			} else {
				File DIR = new File(arg);
				if (DIR.exists() && DIR.isDirectory())
					directoriesToProcess.add(arg);
				else
					System.out.println("Parameter '" + arg + "' is not a valid directory; skipping.");
			}
		}
		
		// load all texts from the given directory/ies:
		if (directoriesToProcess.size() > 0)
			// load texts from the given directories
			run.setTextRepository( TextFactory.loadTextRepositoryFromDirectories(directoriesToProcess));
		else {
			System.err.println("Include at least one directory to read from as parameter!");
			System.exit(2);
		}
		
		BasicExamplePipeline.addExampleTexts(run);
		
		// Pre-processing filter here:
//		run.addFilter(new NameRangeExpander());

		
		
		// NER filters here:
		//
		run.addFilter(new LinnaeusSpeciesServiceNer());
		// invoke the remote GnatService for NER, for species and gene names
		// for all species that are supported
		GnatServiceNer gnatServiceNer = new GnatServiceNer(GnatService.Tasks.GENE_NER);
		gnatServiceNer.useDefaultSpecies = true;
		run.addFilter(gnatServiceNer);
		//

//		run.addFilter(new GOFilter(ISGNProperties.get("dbAccessUrl"), ISGNProperties.get("dbUser"), ISGNProperties.get("dbPass")));
		
		// NER post-processing filters here:
		run.addFilter(new RecognizedEntityUnifier());

		// simple FP filters here (not depending on gene information):
		//
		//run.addFilter(new AbbreviationFilter()); not used in BC3
		// strictFPs_2_2_context_all.object contains data on the context defined by two tokens left and two tokens right of a gene name
		run.addFilter(new LeftRightContextFilter("data/strictFPs_2_2_context_all.object", "data/nonStrictFPs_2_2_context_all.object", 0d, 2, 2));
		//
		run.addFilter(new ImmediateContextFilter());
		//
		run.addFilter(new StopWordFilter(ISGNProperties.get("stopWords")));
		//
		run.addFilter(new UnambiguousMatchFilter());
		//
		run.addFilter(new UnspecificNameFilter());

		// TODO check these and remove or move to an appropriate position in the pipeline:
		//run.addFilter(new HmmFilter(FP_HMM, TP_HMM, LEFT_CONTEXT, RIGHT_CONTEXT, HMM_TOKEN_INDICES_FILE)); not used in BC
		//run.addFilter(new NearestCandidateFilter()); filter not used in BC
		//run.addFilter(new TfIdfFilter(A_TFIDF_THRESHOLD)); not used in BC
		//run.addFilter(new StrictFPFilter(FP_FILE, NON_FP_FILE)); not used in BC
		// TODO end (re)move
		
		// load a gene repository for all remaining candidate IDs now:
		run.addFilter(new GeneRepositoryLoader(GeneRepositoryLoader.RetrievalMethod.SERVICE));
		
		
		// gene information-dependent FP filters here:
		//
		run.addFilter(new NameValidationFilter());
//		//
//		run.addFilter(new AlignmentFilter(AlignmentHelper.globalAlignment, 0.7f));
//		//
//		run.addFilter(new SpeciesValidationFilter());

		
		// final ranking and disambiguation filters here:
		//run.addFilter(new DisambiguationFilter()); not used in BC3, used in BC2 where species are fixed
		run.addFilter(new MultiSpeciesDisambiguationFilter(
				Integer.parseInt(ISGNProperties.get("disambiguationThreshold")),
				Integer.parseInt(ISGNProperties.get("maxIdsForCandidatePrediction"))));
		
		
		// prediction post-processing filters here:
		// set all of the remaining candidates to 'Identified' so that they appear in the output
		run.addFilter(new IdentifyAllFilter());
		
		
		// run all filters, changing run.context, run.textRepository, and run.geneRepository
		run.runFilters();
		
		// print the results for each text, in BioCreative tab-separated format
		List<String> result = run.context.getIdentifiedGeneList();
		for (String res: result)
			System.out.println(res);
	}
	
}
