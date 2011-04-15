package gnat.server;

import gnat.ISGNProperties;
import gnat.client.Run;
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
import gnat.representation.Text;

import java.io.File;
import java.util.List;

/**
 * 
 * A filtering pipeline for gene normalization that is used by services.
 * 
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 *
 */
public class GnatServicePipe {
	Run run;
	
	public GnatServicePipe () {
		run = new Run();
		run.verbosity = 0;
		
		run.addFilter(new RecognizedEntityUnifier());
		run.addFilter(new GeneRepositoryLoader(GeneRepositoryLoader.RetrievalMethod.SERVICE));
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
		
		// set all remaining genes as 'identified' so they will be reported in the result
		run.addFilter(new IdentifyAllFilter());
	}
	
	/**
	 * Returns the result as list of identified genes, sorted by position.
	 * @param text
	 * @return
	 */
	public List<String> run (Text text) {
		run.clearTextRepository();
		run.addText(text);
		
		// run all filters, changing run.context, run.textRepository, and run.geneRepository
		run.runFilters();
		
		List<String> result = run.context.toIdentifiedGeneList_SortedByPosition();
		return result;
	}
}