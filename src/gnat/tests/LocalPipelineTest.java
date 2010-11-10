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
import gnat.filter.ner.DefaultSpeciesRecognitionFilter;
import gnat.filter.ner.RunDictionaries;
import gnat.preprocessing.NameRangeExpander;
import gnat.representation.TextFactory;
import gnat.utils.AlignmentHelper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Runs a test using a fixed GNAT text processing pipeline to ensure a local copy
 * is running as expected. Requires a local dictionary for human genes, a local 
 * database for gene information; uses a simple species recognizer.
 * 
 * <br><br>
 * Start the test with scripts/runTest.sh, see requirements below.
 * <br><br>
 * Test abstracts are read from texts/test/*.txt; an output file in texts/test/*.output
 * contains the expected results.
 * <br>
 * Requirements:<br>
 * - local copy of the GNAT client (Jar file);<br>
 * - running database server (locally or remote, visible via JDBC) with the GNAT gene information (see sql.tar.gz);<br>
 * 
 * 
 * 
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 */
public class LocalPipelineTest {

	
	/**
	 * 
	 * @param args
	 */
	public static void main (String[] args) {
		System.out.println("Testing a locally running pipeline: human dictionary and accessible database.");		
		System.out.println("Reading configuration from " + ISGNProperties.getPropertyFilename());
		
		Run run = new Run();
		run.verbosity = 0;
		
		if (args.length > 0) {
			// the only parameter is -v to regulate verbosity at runtime
			if (args[0].matches("\\-v=\\d+"))
				run.verbosity = Integer.parseInt(args[0].replaceFirst("^\\-v=(\\d+)$", "$1"));
		}
		
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
		// default species NER: spots human, mouse, rat, and fly only
		run.addFilter(new DefaultSpeciesRecognitionFilter());
		// genes via a GnatService
//		GnatServiceNer gnatServiceNer = new GnatServiceNer(GnatService.Tasks.GENE_NER);
//		// tell the remote service to run only for a few species:
//		gnatServiceNer.setLimitedToTaxa(9606); // only human genes
//		gnatServiceNer.useDefaultSpecies = true;
//		run.addFilter(gnatServiceNer);
		// construct a dictionary for human genes only
		RunDictionaries humanDictionaryFilter = new RunDictionaries();
		humanDictionaryFilter.addLimitToTaxon(9606);
		run.addFilter(humanDictionaryFilter);
		
		// NER post-processing filters here:
		run.addFilter(new RecognizedEntityUnifier());

		// include a few disambiguation filters that do not need specific information on each candidate gene
		// thus, these work on the gene's name and its context in the text
		run.addFilter(new ImmediateContextFilter());
		
		// strictFPs_2_2_context_all.object contains data on the context defined by two tokens left and two tokens right of a gene name
		run.addFilter(new LeftRightContextFilter("data/strictFPs_2_2_context_all.object", "data/nonStrictFPs_2_2_context_all.object", 0d, 2, 2));

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
		run.addFilter(new IdentifyAllFilter());
		
		// run all filters, changing run.context, run.textRepository, and run.geneRepository
		run.runFilters();

		
//		List<RecognizedEntity> sortedREs = 
//			run.context.sortRecognizedEntities(run.context.getRecognizedEntities());
//		for (RecognizedEntity re: sortedREs)
//			System.out.println(re.getText().getID() + "\t" + re.getName() + "\t" + re.getBegin() + "\t" + re.getEnd());
//		if (true) return;
				
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
	 * Check whether some basic settings in the configuration (files) have been set:<br>
	 * - database access (dbUser, dbAccessUrl, ...)<br>
	 * 
	 * @return
	 */
	static boolean testConfiguration () {
		boolean configOk = true;
		
		String dbUser = ISGNProperties.get("dbUser");
		String dbPass = ISGNProperties.get("dbPass");
		String dbAccessUrl = ISGNProperties.get("dbAccessUrl");
		String dbDriver = ISGNProperties.get("dbDriver");
		
		if (dbUser == null || dbPass == null || dbAccessUrl == null || dbDriver == null ||
			dbUser.length() == 0 || dbPass.length() == 0 || dbAccessUrl.length() == 0 || dbDriver.length() == 0) {
			System.err.println("Configuration file " + ISGNProperties.getPropertyFilename() + ":\nSet values " +
					"for the entries dbUser/dbPass/dbAccessUrl/dbDriver to reflect your local configuration.");
			configOk = false;
		}
		
		// check DB connectivity / queries
		try {
			Class.forName(ISGNProperties.get("dbDriver"));
			Connection conn = DriverManager.getConnection(
					ISGNProperties.get("dbAccessUrl"),
					ISGNProperties.get("dbUser"),
					ISGNProperties.get("dbPass"));
			Statement stm = conn.createStatement();
			ResultSet rs = stm.executeQuery("DESCRIBE GR_Origin");
			// the following fields have to be present in the GR_Origin table (for others we don't care):
			Set<String> expectedFields = new HashSet<String>();
			expectedFields.add("ID");
			expectedFields.add("taxon");
			while (rs.next()) {
				String fieldName = rs.getString("Field");
				expectedFields.remove(fieldName);
			}
			// succcess if all required fields could be found in that table
			if (expectedFields.size() == 0) {
				System.out.println("#LPT: database test ok.");
			} else {
				System.out.println("#LPT: database test failed -- missing table or fields!");
				System.out.println("#LPT: requiring table `GR_Origin` with fields `ID` and `taxon`");
				configOk = false;
			}
			
		} catch (SQLException sqle) {
			System.err.println("#LPT: database test failed:");
			System.err.println(sqle.getMessage());
			configOk = false;
		} catch (ClassNotFoundException e) {
			System.err.println("#LPT: database test failed:");
			System.err.println("Failed to load the database driver '" + ISGNProperties.get("dbDriver") + "'");
			configOk = false;
		}
		
		
		String tax2portFile = ISGNProperties.get("taxon2port");
		if (tax2portFile == null || tax2portFile.length() == 0) {
			System.err.println("Configuration file " + ISGNProperties.getPropertyFilename() + ":\nSet a value " +
				"for the entry 'taxon2port';\nshould point to a file that contains a mapping of NCBI taxonomy " +
				"IDs to servers and port;\nan example is provided with config/taxonToServerPort.txt");
			configOk = false;
		}

		return configOk;
	}


	/**
	 * Read the expected results from an existing file.
	 * @param filename
	 * @return
	 */
	static List<String> getExpectedOutput (String filename) {
		List<String> result = new LinkedList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#") || line.length() == 0 || line.matches("[\\s\\t]+")) continue;
				result.add(line);
			}
			br.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return result;
	}
	
}
