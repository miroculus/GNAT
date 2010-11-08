package gnat.filter;

import gnat.ISGNProperties;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.TextRepository;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * A Filter that can invoke several other Filters, specified at run time, to perform filtering steps 
 * on a Context, TextRepository, and GeneRepository.
 * <br><br>
 * Which filters to run and in which order is specified in the file found via the {@link ISGNProperties}
 * entry <tt>runAdditionalFilters</tt>. The file is a simple list of class names (one per line,
 * including the full package name: "gnat.filter.ner.GnatServiceNer") that will the called in the given order.
 * Lines starting with '#' are comments; additional comments can be added after the class name, separated from it
 * with a tabulator; thus, only the first column of a tab-separated list will be considered.
 * 
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 */
public class RunAdditionalFilters implements Filter {

	/** */
	List<Filter> filters;
	
	
	/**
	 * 
	 */
	public RunAdditionalFilters () {
		filters = new LinkedList<Filter>();
		String filterListFile = ISGNProperties.get("runAdditionalFilters");
		if (filterListFile == null || filterListFile.length() == 0) {
			System.err.println("#RunFilters: the entry 'runAdditionalFilters' is not specified in " + ISGNProperties.getPropertyFilename() + "! Skipping.");
			return;
		}
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(filterListFile));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#")) continue;
				
				String className = line.split("\t")[0];
				
				// don't load this very class again!
				if (className.equals(this.getClass().getName())) {
					System.err.println("#RunAdditionalFilters: class " + className + " cannot be run recursively; skipping.");
					continue;
				}
				
				try {
					Class<?> clas = (RunAdditionalFilters.class.getClassLoader().loadClass(className));
					
					// check whether the given class implements Filter
					Class<?>[] interfaces = clas.getInterfaces();
					boolean isFilter = false;
					for (Class<?> interfac: interfaces) {
						if (interfac.getCanonicalName().equals("gnat.filter.Filter")) {
							isFilter = true;
							break;
						}
					}
					if (!isFilter) {
						System.err.println("#RunAdditionalFilters: class " + className + " does not appear to implement gnat.filter.Filter; skipping.");
						continue;
					}
					
					Filter newFilter = (Filter)clas.newInstance();
					filters.add(newFilter);
					
				} catch (ClassNotFoundException e) {
					System.err.println("#RunAdditionalFilters: the specified filter '" + className + "' does not appear to exist; please check the class path.");					
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				
			}
			br.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	
	/**
	 * 
	 */
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {
		for (Filter filter: filters) {
			System.out.println("Running filter " + filter.getClass());
			filter.filter(context, textRepository, geneRepository);
		}
	}

}
