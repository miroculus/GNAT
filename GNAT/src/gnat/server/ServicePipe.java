package gnat.server;

import gnat.client.Run;
import gnat.filter.nei.GeneRepositoryLoader;
import gnat.filter.nei.IdentifyAllFilter;
import gnat.filter.nei.ImmediateContextFilter;
import gnat.representation.Text;

import java.util.List;

/**
 * 
 * A filtering pipeline for gene normalization that is used by services.
 * 
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 *
 */
public class ServicePipe {

	Run run;
	
	public ServicePipe () {
		run = new Run();
		run.verbosity = 0;
		
		run.addFilter(new ImmediateContextFilter());
		run.addFilter(new GeneRepositoryLoader(GeneRepositoryLoader.RetrievalMethod.SERVICE));
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
