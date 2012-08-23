package gnat.filter.nei;

import gnat.ConstantsNei;
import gnat.ISGNProperties;
import gnat.database.GeneRepositoryFromDatabase;
import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.Gene;
import gnat.representation.GeneFactory;
import gnat.representation.GeneRepository;
import gnat.representation.RecognizedEntity;
import gnat.representation.TextRepository;
import gnat.utils.StringHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * A filter that wraps GeneRepository loader methods, from GeneRepositoryFromDatabase and GeneFactory.
 * 
 * @author Joerg
 *
 */
public class GeneRepositoryLoader implements Filter {

	/** */
	public enum RetrievalMethod {DATABASE, LOCAL_FILE, SERVICE}

	/** */
	RetrievalMethod method;


	/**
	 * Constructs a GeneRepositoryLoader factory that retrieves information on genes via the
	 * given <tt>method</tt>.
	 * @param method
	 */
	public GeneRepositoryLoader (RetrievalMethod method) {

		// for each method, check the properties for database/file/server names
		if (method == RetrievalMethod.DATABASE) {
			String dbUser      = ISGNProperties.get("dbUser");
			if (dbUser == null || dbUser.length() == 0) {
				System.err.println("#GeneRepLoader: no user specified via the entry 'dbUser' in " + ISGNProperties.getPropertyFilename());
				return;
			}

			String dbPass      = ISGNProperties.get("dbPass");
			if (dbPass == null || dbPass.length() == 0) {
				System.err.println("#GeneRepLoader: no user password specified via the entry 'dbPass' in " + ISGNProperties.getPropertyFilename());
				return;
			}

			String dbAccessUrl = ISGNProperties.get("dbAccessUrl");
			if (dbAccessUrl == null || dbAccessUrl.length() == 0) {
				System.err.println("#GeneRepLoader: no access URL specified via the entry 'dbAccessUrl' in " + ISGNProperties.getPropertyFilename());
				return;
			}

			String dbDriver    = ISGNProperties.get("dbDriver");
			if (dbDriver == null || dbDriver.length() == 0) {
				System.err.println("#GeneRepLoader: no database driver specified via the entry 'dbDriver' in " + ISGNProperties.getPropertyFilename());
				return;
			}


		} else if (method == RetrievalMethod.LOCAL_FILE) {

			String geneRepositoryFile = ISGNProperties.get("geneRepositoryFile");
			if (geneRepositoryFile == null || geneRepositoryFile.length() == 0) {
				System.err.println("#GeneRepLoader: no file with a gene repository driver specified via the entry 'geneRepositoryFile' in " + ISGNProperties.getPropertyFilename());
				return;
			}


		} else if (method == RetrievalMethod.SERVICE) {

			String geneRepositoryServer = ISGNProperties.get("geneRepositoryService");
			if (geneRepositoryServer == null || geneRepositoryServer.length() == 0) {
				System.err.println("#GeneRepLoader: no gene repository server specified via the entry 'geneRepositoryServer' in " + ISGNProperties.getPropertyFilename());
				return;
			} else {
				if (!geneRepositoryServer.matches(".*\\:\\d+")) {
					System.err.println("#GeneRepLoader: the entry 'geneRepositoryServer' in " + ISGNProperties.getPropertyFilename() + " should have the format 'addr:port'.");
					return;
				}
			}

		}

		this.method = method;
	}


	/**
	 * @param context
	 */
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {

		// get a list of all candidate gene IDs, obtain a GeneRep that has information on all those genes
		TreeSet<Integer> allCandidateIDs = new TreeSet<Integer>();
		Iterator<RecognizedEntity> allGeneNames = context.getUnidentifiedEntities().iterator();
		while (allGeneNames.hasNext()) {
			RecognizedEntity recognizedGeneName = allGeneNames.next();
			Set<String> idCandidates = context.getIdCandidates(recognizedGeneName);
			Iterator<String> it = idCandidates.iterator();
			while (it.hasNext()) {
				String next = it.next();
				allCandidateIDs.add(Integer.parseInt(next));
			}
		}

		// obtain a GeneRep, using the given RetrievalMethod
		if (method == null) {
			System.err.println("#GeneRepLoader: no valid method for retrieval was set. Not able to load a repository.");
			return;
		}

		// log the time it took to load the repository
		long starttime = System.currentTimeMillis();

		// load the gene repo, depending on the retrieval method
		if (method == RetrievalMethod.SERVICE) {
			// contact a server:port
			String geneIds = StringHelper.joinIntegerSet(allCandidateIDs, ",");

			try {
				// Construct data
				String data = URLEncoder.encode("genes", "UTF-8")    + "=" + URLEncoder.encode(geneIds, "UTF-8");
				// + "&" + URLEncoder.encode("text", "UTF-8")       + "=" + URLEncoder.encode(text.getPlainText(), "UTF-8");

				// Send data
				if (!ISGNProperties.getProperty("geneRepositoryService").startsWith("http://"))
					throw new IllegalStateException("The geneRepositoryService need to start with http://");
				
				URL url = new URL(ISGNProperties.getProperty("geneRepositoryService"));
				URLConnection conn = url.openConnection();
				
				conn.setDoOutput(true);
				OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
				wr.write(data);
				wr.flush();

				// get and parse the response
				BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String line;
				while ((line = rd.readLine()) != null) {
					//System.err.println("Adding " + line);
					// skip comments and messages
					if (line.startsWith("#") || line.startsWith("<error") || line.length() == 0) continue;
					// every valid entry starts with an EntrezGene ID
					if (!line.matches("\\d+\\t.+")) continue;

					Gene gene = GeneFactory.makeGeneFromTsv(line);
					//System.err.println("Added " + gene.ID);
					geneRepository.addGene(gene);	
				}
				wr.close();
				rd.close();
			} catch (UnsupportedEncodingException use) {
				use.printStackTrace();
			} catch (MalformedURLException mue) {
				mue.printStackTrace();
			} catch (IOException ioe) {
				if (ioe.toString().contains("Premature EOF"))
					System.err.println("Premature EOF");
				else
					ioe.printStackTrace();
			}

		} else if (method == RetrievalMethod.DATABASE) {
			// access a database
			if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.STATUS) >= 0)
				System.out.println("#Getting gene repository from database (" + allCandidateIDs.size() + " genes) ...");
			//			GeneRepositoryFromDatabase grepper = new GeneRepositoryFromDatabase();
			//			grepper.verbosity = 1;
			//			//geneRepository = grepper.getGeneRepository(allCandidateIDs);
			//			// TODO be careful here: overwriting geneRepository won't work, add the genes using addGenes(Collection) instead!!!
			//			geneRepository = grepper.getGeneRepositoryFAST((Set<Integer>)allCandidateIDs);			
			GeneRepositoryFromDatabase grepper = new GeneRepositoryFromDatabase();
			geneRepository.addGenes(grepper.getGeneRepositoryFAST((Set<Integer>)allCandidateIDs));


		} else if (method == RetrievalMethod.LOCAL_FILE) {
			// load from a local object file

			if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.STATUS) >= 0)
				System.out.println("#Getting gene repository from file...");
			GeneRepository aGeneRepository = GeneFactory.loadGeneRepositoryFromFile(new File(ISGNProperties.get("geneRepositoryFile")));
			geneRepository.addGenes(aGeneRepository.getGenes());

			// check if the file contained all genes we need
			// get genes that were not found in the file from the database
			// TODO re-activate - used SERVER or DATABASE as fall back methods if a gene does not exist in the local file
			//			for (Integer candidate: allCandidateIDs) {
			//				if (geneRepository.getGene(candidate.toString()) == null) {
			//					GeneRepositoryFromDatabase grepper = new GeneRepositoryFromDatabase();
			//					Gene gene = grepper.getGene(candidate.toString());
			//					if (gene != null)
			//						geneRepository.addGene(gene);
			//				}
			//			}

		}

		long time = System.currentTimeMillis() - starttime;
		if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.STATUS) > 0)
			System.out.println("#GeneRepository loaded in " + time + "ms.");

		if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.STATUS) >= 0)
			System.out.println("#GeneRepository loaded with " + geneRepository.size() + " genes.");

		// TODO writing the GeneRep to a local file has to be re-activated
		//		if (GENEREP_FILE_WRITE != null && GENEREP_FILE_WRITE.length() > 0) {
		//			if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.STATUS) >= 0)
		//				System.out.println("#Writing gene repository to file...");
		//			GeneFactory.writeGeneRepositoryToObjectFile(geneRepository, new File(GENEREP_FILE_WRITE));
		//		}

	}

}
