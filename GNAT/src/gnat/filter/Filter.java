package gnat.filter;

import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.TextRepository;

/**
 * Interface for all filters that handle contexts, text repositories, and gene repositories.<br>
 * 
 * 
 * @author Joerg
 *
 */
public interface Filter {

	/**
	 * The minimum filtering method that has to be implemented. A {@link gnat.client.Run} will call this
	 * method for each filter in sequence.<br>
	 * Each filter can read and potentially change context, textRepository, and geneRepository
	 * in this method.
	 * 
	 * @param context
	 * @param textRepository
	 * @param geneRepository
	 */
	public abstract void filter (Context context, TextRepository textRepository, GeneRepository geneRepository);

}
