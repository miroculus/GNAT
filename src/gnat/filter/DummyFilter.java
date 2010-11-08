package gnat.filter;

import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.TextRepository;

/**
 * A Filter that does nothing.
 * 
 * 
 * @author Joerg
 *
 */
public class DummyFilter implements Filter {

	@Override
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {		
	}
	
}
