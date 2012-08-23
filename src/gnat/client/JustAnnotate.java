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
import gnat.filter.nei.SpeciesFrequencyFilter;
import gnat.filter.nei.SpeciesValidationFilter;
import gnat.filter.nei.StopWordFilter;
import gnat.filter.nei.UnambiguousMatchFilter;
import gnat.filter.nei.UnspecificNameFilter;
import gnat.filter.ner.DefaultSpeciesRecognitionFilter;
import gnat.filter.ner.RunAllGeneDictionaries;
import gnat.preprocessing.NameRangeExpander;
import gnat.representation.Text;
import gnat.representation.TextFactory;
import gnat.utils.AlignmentHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * A simple processing pipeline that takes a directory as input, reads all files (*.txt) and
 * prints the list of predicted genes.<br>
 * <br>
 * Assumes that the gene repository is held in a local database (specified in isgn_properties.xml)
 * and that dictionary servers for the species human, yeast, mouse, and fruit fly are running,
 * on ports specified in config/taxonToServerPorts, on the server specified under 'dictionaryServer'  
 * in isgn_properties.xml.
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 */
public class JustAnnotate {

	/**
	 * 
	 * @param args
	 */
	public static void main (String[] args) {
		// Each process is capsuled by a Run
		Run run = new Run();
		run.verbosity = 0;

		// check for command line parameters
		if (args.length == 0 || args[0].matches("\\-\\-?h(elp)?")) {
			System.out.println("A simple processing pipeline that takes a directory as input,\n" +
				"reads all files (*.txt) and prints the list of predicted genes in a tabular format.");
			System.out.println("Needs a directory with <PMID>.txt files to annotate as parameter.");
			System.out.println("Optional parameters:");
			System.out.println(" -v=<int>    - set verbosity");
			System.out.println(" -nodir      - do not print the directory name, just file name, in the output");
			System.out.println(" -out <file> - file name for the final output (tab-separted format); default: STDOUT");
			System.exit(1);
		}
		
		// parse and store command line parameters
		String dir = "";         // directory to read from
		String outfile = "";
		boolean printDir = true; // print full path name in output, or just file name		
		for (int a = 0; a < args.length; a++) {
			// parameter is -v to regulate verbosity at runtime
			if (args[a].matches("\\-v=\\d+"))
				run.verbosity = Integer.parseInt(args[a].replaceFirst("^\\-v=(\\d+)$", "$1"));
			else if (args[a].toLowerCase().matches("\\-\\-?nodir")) 
				printDir = false;
			else if (args[a].toLowerCase().matches("\\-\\-?out")) 
				outfile = args[++a];
			else if (args[a].toLowerCase().matches("\\-\\-?out=.+")) 
				outfile = args[a].replaceFirst("^\\-\\-?out=(.+)$", "$1");
			else {
				dir = args[a];
				File DIR = new File(dir);
				if (DIR.exists() && DIR.canRead()) {
					if (DIR.isDirectory()) {
						if (DIR.list().length == 0) {
							System.err.println("Error: there seem to be no files in the directory " + args[a]);
							System.exit(3);
						}
					} else {
						// is a single file
					}
				} else {
					System.err.println("Error: cannot access the file/directory " + args[a]);
					System.exit(2);					
				}
			}
		}


		//////////
		// INPUT
		
		// load all texts required for the test
		run.setTextRepository(TextFactory.loadTextRepositoryFromDirectories(dir));
		
		// assume the file name refers to a PubMed ID (path/<pmid>.txt)
		run.setFilenamesAsPubMedId();

		
		//////////
		// PROCESSING
		// Plug together a processing pipeline: add filters to the Run
		
		//////////
		// Pre-processing filters here:
		run.addFilter(new NameRangeExpander());
		
		
		//////////
		// NER filters here:
		// default species NER: spots human, mouse, rat, yeast, and fly only
		run.addFilter(new DefaultSpeciesRecognitionFilter());
		String assumeSpecies = ISGNProperties.get("assumeSpecies");
		if (assumeSpecies != null && assumeSpecies.length() > 0) {
			String[] species = assumeSpecies.split("[\\;\\,]\\s*");
			for (String spec: species) {
				if (!spec.matches("\\d+")) continue;
				int tax = Integer.parseInt(spec);
				for (Text text : run.getTextRepository().getTexts())
					text.addTaxonId(tax);
			}
		}
		
		// construct a dictionary filter for human, mouse, yeast, fruit fly genes only
		RunAllGeneDictionaries afewDictionaryFilters = new RunAllGeneDictionaries();
		afewDictionaryFilters.setLimitToTaxons(9606, 10090, 10116, 559292, 7227);
		run.addFilter(afewDictionaryFilters);
		
		// print the status of gene NER right after the NER step, before filtering anything out
		//run.addFilter(new PrintStatus());
		
		//////////
		// NER post-processing filters here:
		run.addFilter(new RecognizedEntityUnifier());

		// include a few disambiguation filters that do not need specific information on each candidate gene
		// thus, these work on the gene's name and its context in the text
		run.addFilter(new ImmediateContextFilter());
		
		// strictFPs_2_2_context_all.object contains data on the context defined by two tokens left and two tokens right of a gene name
		run.addFilter(new LeftRightContextFilter("data/strictFPs_2_2_context_all.object", "data/nonStrictFPs_2_2_context_all.object", 0d, 2, 2));

		// print the status on all recognized genes before loading information on each gene
		//run.addFilter(new PrintStatus());
		
		//
		run.addFilter(new ImmediateContextFilter());
		
		// load the gene repository to obtain information on each gene (if only the species)
		// not loading gene repository will produce an empty result at the end
		run.addFilter(new GeneRepositoryLoader(GeneRepositoryLoader.RetrievalMethod.DATABASE));

		//
		run.addFilter(new StopWordFilter(ISGNProperties.get("stopWords")));
		//
		run.addFilter(new UnambiguousMatchFilter());
		//
		run.addFilter(new UnspecificNameFilter());
		//
		run.addFilter(new AlignmentFilter(AlignmentHelper.globalAlignment, 0.7f));
		//
		run.addFilter(new NameValidationFilter());
		
		// filter by the number of occurrences of each organism
		run.addFilter(new SpeciesFrequencyFilter());
		
		//
		//run.addFilter(new SpeciesValidationFilter());
		
		// Final disambiguation filter
		run.addFilter(new MultiSpeciesDisambiguationFilter(
				Integer.parseInt(ISGNProperties.get("disambiguationThreshold")),
				Integer.parseInt(ISGNProperties.get("maxIdsForCandidatePrediction"))));
		
		// Mark everything that "survived" until here as OK, will be reported in output
		// Only for high-recall runs
		String tuning = ISGNProperties.get("tuning");
		if (tuning != null && tuning.equalsIgnoreCase("recall"))
			run.addFilter(new IdentifyAllFilter());
		
		
		//////////
		// Run all filters, changing run.context, run.textRepository, and run.geneRepository
		run.runFilters();

		
		//////////
		// OUTPUT
		// get the results for each text, in BioCreative tab-separated format
		List<String> result = run.context.getIdentifiedGeneList_SortedByTextAndId();
		// get expected results from saved file
		PrintWriter out = new PrintWriter(System.out);
		if (outfile.length() > 0) {
			try {
				out = new PrintWriter(new FileWriter(outfile));
			} catch (IOException o) {
				o.printStackTrace();
			}
		}
		for (String res: result) {
			// print output including filepath and not just the filename
			if (printDir)
				out.println(res);
			// print output with filename only
			else {
				// first column: path+file name -> split into path and name -> grab name only
				String[] cols = res.split("\t");
				String filename = cols[0].replaceFirst(".*\\/(.+?)", "$1");
				out.print(filename);
				// other columns are ok
				for (int c = 1; c < cols.length; c++)
					out.print("\t" + cols[c]);
				out.println();
			}
		}
		out.close();
	}
	
}
