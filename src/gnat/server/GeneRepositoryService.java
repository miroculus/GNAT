package gnat.server;

import gnat.ServiceProperties;
import gnat.utils.StringHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;


/**
 * Provides remote access to a local database on genes.<br>
 * Accepts HTTP GET and POST request by GNAT clients and via standard HTTP (wget etc.).
 * 
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 *
 */
public class GeneRepositoryService extends HttpService {
	
	/**
	 * 
	 * @param args
	 */
	public static void main (String[] args) throws IOException {
		int port = 8082;
		int log = 0;
		Modes mode = Modes.STATUS;
		
		for (String a: args) {
			if (a.toLowerCase().matches("\\-\\-?p(ort)?=(\\d+)"))
				port = Integer.parseInt(a.replaceFirst("\\-\\-?p(ort)?=(\\d+)", "$2"));
			else if (a.toLowerCase().matches("\\-\\-?log=(\\d+)"))
				log = Integer.parseInt(a.replaceFirst("\\-\\-?log=(\\d+)", "$1"));
			else if (a.equalsIgnoreCase("start"))
				mode = Modes.START;
			//else if (a.equalsIgnoreCase("stop"))
			//	mode = Modes.STOP;
			else
				System.err.println("Unknown parameter: " + a + " - ignoring.");
		}
		
		if (mode == Modes.START) {
			GeneRepositoryService service = new GeneRepositoryService();
			service.logLevel = log;
			service.start(port);
		//} else if (mode == Modes.STOP) {
		//	service.stop();
			
		} else {
			//service.status();
		}
	}
	
	
	/**
	 * 
	 * @param port
	 * @throws IOException
	 */
	public void start (int port) throws IOException {
		long start = System.currentTimeMillis();
		
		InetSocketAddress addr = new InetSocketAddress(port);
		server = HttpServer.create(addr, 0);
		
		server.createContext("/", new GeneServiceHandler());
		server.setExecutor(Executors.newCachedThreadPool());
	    server.start();
	    
	    System.out.println("GeneRepositoryService started on port " + port + " in " + (System.currentTimeMillis() - start) + "ms.");
	}
	
	
	/**
	 * 
	 *
	 */
	class GeneServiceHandler extends ServiceHandler {
		
		/** */
		Connection connection = null;
		Statement  statement = null;
		ResultSet  resultset = null;
		boolean openConnection = false;
		
		
		/**
		 * 
		 */
		GeneServiceHandler () {
			openConnection = openConnection();
		}

		
		/**
		 * Handles HTTP requests (only GET and POST are implemented).
		 * @param exchange
		 */
		public void handle (HttpExchange exchange) throws IOException {
			String requestMethod = exchange.getRequestMethod();
			
			// Not a GET/POST request? We're not handling these here.
			if (!requestMethod.equalsIgnoreCase("GET") && !requestMethod.equalsIgnoreCase("POST")) {
				Headers responseHeaders = exchange.getResponseHeaders();
				responseHeaders.set("Content-Type", "text/plain");
				exchange.sendResponseHeaders(400, 0);
				return;
			}
			
			// get the query parameters
			Request userQuery = getQuery(exchange);
			// point the user to the help screen option if no parameters were sent at all
			if (userQuery == null) {
				Headers responseHeaders = exchange.getResponseHeaders();
				responseHeaders.set("Content-Type", "text/plain");
				exchange.sendResponseHeaders(200, 0);
				OutputStream responseBody = exchange.getResponseBody();
				responseBody.write(("Call this service with the URL parameter 'help' to view instructions.\n").getBytes());
				responseBody.close();
				return;
			}
			
			// so far, the request seems valid, send an OK ACK
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, 0);
		
			// get an output stream for the response
			OutputStream responseBody = exchange.getResponseBody();
						
			// print a help screen if requested
			if (userQuery == null || userQuery.isEmpty() || // TODO see above - print help only if requested?
				userQuery.hasParameter("help") || userQuery.hasParameter("parameters") || userQuery.hasParameter("getparameters")
				) {
				responseBody.write("This service accepts HTTP GET and POST requests to retrieve information on genes.\n\n".getBytes());
				responseBody.write("Valid parameters (submitted as key=value pairs):\n".getBytes());
				responseBody.write("  help        -  print a list of supported parameters; will ignore other parameters\n".getBytes());
				responseBody.write("  genes       -  get information on one or more genes (comma-separated list of EntrezGene IDs)\n".getBytes());
				responseBody.write("  returntype  -  output format: either 'xml' or 'tsv'; default: tsv\n".getBytes());
				responseBody.write("\n".getBytes());
				responseBody.write("Returns a list of tab-separated entries (TSV, one line per gene), with\n".getBytes());
				responseBody.write(("Entrez Gene ID, NCBI taxon ID, Offical symbold (short and long form where available), aliases and synonyms, " +
						"chromosomal location, EntrezGene summary, GeneRIFs, PubMed IDs, protein mass, protein length, protein mutations, " +
						"protein domains, subcellular location, tissue specificity, protein interaction partners, function, disease, GO codes, keywords\n").getBytes());
				responseBody.write(("If one of these data is not available, the respective column will show a dash ('-'). In case of multiple entries per item, " +
						"these will be separated by '; ' (semi-colon and whitespace).\n").getBytes());
				responseBody.write("This format might change in future releases, especially adding more information.\n".getBytes());
				responseBody.write("\n".getBytes());
				responseBody.close();
				return;
			}
			
			// get the gene IDs requested by the user
			Set<Integer> requestedGenes = new LinkedHashSet<Integer>();
			if (userQuery.hasParameter("genes")) {
				String[] species = userQuery.getValue("genes").split("\\s*[\\;\\,]\\s*");
				for (String s: species) {
					if (s.matches("\\d+")) {
						requestedGenes.add(Integer.parseInt(s));
					} else
						responseBody.write(("<error>Unrecognized gene ID in request: '" + s + "'. Use NCBI EntrezGene IDs only.</error>\n").getBytes());
				}
			}

			Collection<GeneInfo> geneCollection = getGeneListFAST(requestedGenes);
			
			if (userQuery.hasParameter("returntype") && userQuery.getValue("returntype").equalsIgnoreCase("xml")) {
				responseBody.write("<genes>\n".getBytes());
				for (GeneInfo gene: geneCollection)
					writeGeneAsXml(responseBody, gene);
				responseBody.write("</genes>".getBytes());
			} else
				for (GeneInfo gene: geneCollection)
					writeGeneAsTsv(responseBody, gene);
			
			responseBody.close();			
		}
		
		
		/**
		 * Writes all the data on the given gene to <tt>out</tt>, as a tab-separated list of items.
		 * <br><br>
		 * Order: EntrezGene ID, taxon,offical symbold (short and long form where available), aliases and synonyms, 
		 * chromosomal location, EntrezGene summary, GeneRIFs, PubMed IDs, protein mass, protein length, 
		 * protein mutations, protein domains, subcellular location, tissue specificity, protein interaction 
		 * partners, function, disease, GO codes, keywords.
		 * 
		 * @see #writeTsvItem(OutputStream, String)
		 * @param out
		 * @param gene
		 * @throws IOException
		 */
		void writeGeneAsTsv (OutputStream out, GeneInfo gene) throws IOException {
			out.write(gene.id.getBytes());
			writeTsvItem(out, gene.get("Origin"));
			writeTsvItem(out, gene.get("GeneRef"));
			writeTsvItem(out, gene.get("Name"));
			
			writeTsvItem(out, gene.get("ChrLoc"));
			
			String summary = gene.get("Summary");
			if (summary == null) summary = "-";
			else {
				String[] summaries = summary.split("[\r\n]+");
				summary = StringHelper.joinStringArray(summaries, ". ");
			}
			writeTsvItem(out, summary);
			
			writeTsvItem(out, gene.get("GeneRIF"));
			writeTsvItem(out, gene.get("PubMed"));
			
			writeTsvItem(out, gene.get("ProteinMass"));
			writeTsvItem(out, gene.get("ProteinLength"));
			writeTsvItem(out, gene.get("ProteinMutation"));
			writeTsvItem(out, gene.get("ProteinDomain"));
			writeTsvItem(out, gene.get("SubcellularLocation"));
			writeTsvItem(out, gene.get("Tissue"));
			writeTsvItem(out, gene.get("Interactor"));
			writeTsvItem(out, gene.get("Function"));
			writeTsvItem(out, gene.get("Disease"));
			
			writeTsvItem(out, gene.get("GOCode"));
			writeTsvItem(out, gene.get("Keyword"));
			
			out.write("\n".getBytes());
		}
		
		
		/**
		 * Writes a TSV item to <tt>out</tt>: first a tab and then the <tt>data</dd>.
		 * If the data is empty or null, writes a dash '-'.
		 * @param out
		 * @param data
		 * @throws IOException
		 */
		void writeTsvItem (OutputStream out, String data) throws IOException {
			if (data == null) data = "-";
			out.write("\t".getBytes());
			out.write(data.getBytes());
		}
		
		
		/**
		 * Writes all the data on the given gene to <tt>out</tt>, in an XML format.
		 * <br><br>
		 * Content: EntrezGene ID, taxon,offical symbold (short and long form where available), aliases and synonyms, 
		 * chromosomal location, EntrezGene summary, GeneRIFs, PubMed IDs, protein mass, protein length, 
		 * protein mutations, protein domains, subcellular location, tissue specificity, protein interaction 
		 * partners, function, disease, GO codes, keywords.
		 * 
		 * @see #writeXmlItem(OutputStream, String)
		 * @see #writeGeneAsTsv(OutputStream, GeneInfo)
		 * @param out
		 * @param gene
		 * @throws IOException
		 */
		void writeGeneAsXml (OutputStream out, GeneInfo gene) throws IOException {
			out.write("<gene id=\"".getBytes());
			out.write(gene.id.getBytes());
			out.write("\">\n".getBytes());
			
			writeXmlItem(out, "origin", gene.get("Origin"));
			writeXmlItem(out, "symbol", gene.get("GeneRef"));
			writeXmlItem(out, "names", gene.get("Name"));
			
			writeXmlItem(out, "chrLocation", gene.get("ChrLoc"));
			
			String summary = gene.get("Summary");
			if (summary == null) summary = "-";
			else {
				String[] summaries = summary.split("[\r\n]+");
				summary = StringHelper.joinStringArray(summaries, ". ");
			}
			writeXmlItem(out, "summary", summary);
			
			writeXmlItem(out, "geneRIFs", gene.get("GeneRIF"));
			writeXmlItem(out, "pubmedRefs", gene.get("PubMed"));
			
			writeXmlItem(out, "proteinMass", gene.get("ProteinMass"));
			writeXmlItem(out, "proteinLength", gene.get("ProteinLength"));
			writeXmlItem(out, "mutations", gene.get("ProteinMutation"));
			writeXmlItem(out, "domains", gene.get("ProteinDomain"));
			writeXmlItem(out, "subCellularLocation", gene.get("SubcellularLocation"));
			writeXmlItem(out, "tissueSpecificity", gene.get("Tissue"));
			writeXmlItem(out, "interactors", gene.get("Interactor"));
			writeXmlItem(out, "function", gene.get("Function"));
			writeXmlItem(out, "disease", gene.get("Disease"));
			
			writeXmlItem(out, "goCodes", gene.get("GOCode"));
			writeXmlItem(out, "keywords", gene.get("Keyword"));
			
			out.write("</gene>\n".getBytes());
		}
		
		
		/**
		 * Writes an Xml item to <tt>out</tt>: an <tt>element</tt> with <tt>content</tt>.
		 * If the content is empty or null, skips the element.
		 * TODO no Xml validity check is currently done on <tt>content</tt>
		 * @param out
		 * @param data
		 * @throws IOException
		 */
		void writeXmlItem (OutputStream out, String element, String content) throws IOException {
			if (content == null || content.length() == 0) return;
			
			out.write("<".getBytes());
			out.write(element.getBytes());
			out.write(">".getBytes());
			
			out.write(content.getBytes());
			
			out.write("</".getBytes());
			out.write(element.getBytes());
			out.write(">\n".getBytes());
		}
		
		
		/**
		 * Returns a set of genes from the database, stored in a GeneRepository.
		 * @param ids
		 * @return
		 */
		public Collection<GeneInfo> getGeneListFAST (Set<Integer> geneIds) {
			Collection<GeneInfo> geneList = new HashSet<GeneInfo>();

			Map<Integer, Set<String>> gene2goIds	= getValues("GR_GOID", "GOID", geneIds);
			Map<Integer, Set<String>> gene2chrLoc 	= getValues("GR_ChrLocation", "location", geneIds);
			Map<Integer, Set<String>> gene2geneRifs = getValues("GR_GeneRIF", "generif", geneIds);
			Map<Integer, Set<String>> gene2geneRefs = getValues("GR_GeneRef", "generef", geneIds);
			Map<Integer, Set<String>> gene2pmIds = getValues("GR_PubMedID", "PMID", geneIds);
			Map<Integer, Set<String>> gene2summary = getValues("GR_Summary", "summary", geneIds);
			//Map<Integer, Set<String>> gene2interactor = getValues("GR_Interactorname", "name", geneIds);
			Map<Integer, Set<String>> gene2disease = getValues("GR_ProteinDisease", "disease", geneIds);
			Map<Integer, Set<String>> gene2domain = getValues("GR_ProteinDomain", "domain", geneIds);
			Map<Integer, Set<String>> gene2function = getValues("GR_ProteinFunction", "function", geneIds);
			Map<Integer, Set<String>> gene2keywords = getValues("GR_ProteinKeywords", "keywords", geneIds);
			Map<Integer, Set<String>> gene2protLength = getValues("GR_ProteinLength", "length", geneIds);
			Map<Integer, Set<String>> gene2protMass = getValues("GR_ProteinMass", "mass", geneIds);
			Map<Integer, Set<String>> gene2mutation = getValues("GR_ProteinMutation", "mutation", geneIds);
			Map<Integer, Set<String>> gene2tissue = getValues("GR_ProteinTissueSpecificity", "tissue", geneIds);
			Map<Integer, Set<String>> gene2subCell = getValues("GR_ProteinSubcellularLocation", "location", geneIds);
			Map<Integer, Set<String>> gene2protInteraction = getValues("GR_ProteinInteraction", "interaction", geneIds);
			Map<Integer, Set<String>> gene2names = getValues("GR_Names", "name", geneIds);
			Map<Integer, Set<String>> gene2protNames = getValues("GR_ProteinNames", "name", geneIds);
			Map<Integer, Set<String>> gene2taxon = getValues("GR_Origin", "taxon", geneIds);

			for (Integer id : geneIds) {

				String geneId = ""+id;
				GeneInfo gene = new GeneInfo(geneId);

				Set<String> names = gene2names.get(id);
				if (names!=null)
					gene.set("Name", StringHelper.joinStringSet(names, "; "));

				Set<String> protNames = gene2protNames.get(id);
				if (protNames!=null)
					gene.add("Name", StringHelper.joinStringSet(names, "; "), "; ");

				Set<String> generefs = gene2geneRefs.get(id);
				if(generefs!=null)
					gene.set("GeneRef",  StringHelper.joinStringSet(generefs, "; "));

				Set<String> pmids = gene2pmIds.get(id);
				if(pmids!=null)
					gene.set("PubMed", StringHelper.joinStringSet(pmids, "; "));
				
				Set<String> generifs = gene2geneRifs.get(id);
				if(generifs!=null)
					gene.set("GeneRIF",  StringHelper.joinStringSet(generifs, "; "));

				Set<String> summary = gene2summary.get(id);
				if (summary!=null && summary.size()>0)
					gene.set("Summary", StringHelper.joinStringSet(summary, ". "));

				Set<String> goCodes = gene2goIds.get(id);
				if (goCodes != null)
					gene.set("GOCode", StringHelper.joinStringSet(goCodes, "; "));

				Set<String> chrLoc = gene2chrLoc.get(id);
				if(chrLoc!=null)
					gene.set("ChrLoc", StringHelper.joinStringSet(chrLoc, "; "));

//				Set<String> interactors = gene2interactor.get(id);
//				if (interactors!=null)
//					gene.set("Interactor", StringHelper.joinStringSet(interactors, "; "));

				Set<String> diseases = gene2disease.get(id);
				if(diseases!=null)
					gene.set("Disease", StringHelper.joinStringSet(diseases, "; "));

				Set<String> domains = gene2domain.get(id);
				if (domains!=null)
					gene.set("ProteinDomain", StringHelper.joinStringSet(domains, "; "));

				Set<String> functions = gene2function.get(id);
				if (functions!=null)
					gene.set("Function", StringHelper.joinStringSet(functions, "; "));

				Set<String> keywords = gene2keywords.get(id);
				if (keywords!=null)
					gene.set("Keyword", StringHelper.joinStringSet(keywords, "; "));

				Set<String> protLength = gene2protLength.get(id);
				if (protLength!=null)
					gene.set("ProteinLength", StringHelper.joinStringSet(protLength, "; "));
				
				Set<String> protMass = gene2protMass.get(id);
				if (protMass!=null)
					gene.set("ProteinMass", StringHelper.joinStringSet(protMass, "; "));

				Set<String> mutation = gene2mutation.get(id);
				if (mutation!=null)
					gene.set("ProteinMutation", StringHelper.joinStringSet(mutation, "; "));

				Set<String> tissue = gene2tissue.get(id);
				if (tissue!=null)
					gene.set("Tissue", StringHelper.joinStringSet(tissue, "; "));

				Set<String> subcellLoc = gene2subCell.get(id);
				if (subcellLoc!=null)
					gene.set("SubcellularLocation", StringHelper.joinStringSet(subcellLoc, "; "));

				Set<String> protInteraction = gene2protInteraction.get(id);
				if (protInteraction!=null)
					gene.add("Interactor", StringHelper.joinStringSet(protInteraction, "; "), "; ");

				Set<String> taxonIds = gene2taxon.get(id);
				if (taxonIds!=null && taxonIds.size()>0)
					gene.set("Origin", StringHelper.joinStringSet(taxonIds, "; "));
				else
					gene.set("Origin", "-1");

				if (gene.hasData())
					geneList.add(gene);
	        }

			return geneList;
		}
		
		
		/**
		 * Reads data from the given <tt>column</tt> and <tt>table</tt>, for all <tt>geneIds</tt>.
		 * <br><br>
		 * If a database connection could not be established / re-opened, tries three more times 
		 * by invoking {@link #getValues(String, String, Set, int)} recursively.
		 *
		 * @param table
		 * @param column
		 * @param geneIds
		 * @return
		 */
		private Map<Integer, Set<String>> getValues (String table, String column, Set<Integer> geneIds) {
			Map<Integer, Set<String>> gene2values = new HashMap<Integer, Set<String>>();
			if (geneIds == null || geneIds.size() == 0) return gene2values;

			try {
				StringBuffer geneIdBuffer = toIdBuffer(geneIds);
				resultset = statement.executeQuery("SELECT ID, " +column + " FROM " + table + " WHERE ID IN ("+geneIdBuffer+")");
				while (resultset.next()) {
					int geneId   = resultset.getInt(1);
					String value = resultset.getString(2);

					Set<String> values = gene2values.get(geneId);
					if (values == null){
						values = new HashSet<String>();
						gene2values.put(geneId, values);
					}

					values.add(value);
				}
				
			} catch (com.mysql.jdbc.exceptions.jdbc4.MySQLNonTransientConnectionException e) {
				// if the connection has timed out, re-open it and try again
				openConnection();
				
				// is this a good idea?
				return getValues(table, column, geneIds, 1);
				
			} catch (SQLException sqe) {
				sqe.printStackTrace();
			}

			return gene2values;
		}
		
		
		/**
		 * Reads data from the given <tt>column</tt> and <tt>table</tt>, for all <tt>geneIds</tt>.
		 * <br><br>
		 * If a database connection could not be established, stops after the third try.
		 * <br><br>
		 * <em>This method is invoked recursively and should not be called directly!</em>
		 * It is called only by {@link #getValues(String, String, Set)}.
		 * 
		 * @param table
		 * @param column
		 * @param geneIds
		 * @param tries
		 * @return
		 */
		private Map<Integer, Set<String>> getValues (String table, String column, Set<Integer> geneIds, int tries) {
			Map<Integer, Set<String>> gene2values = new HashMap<Integer, Set<String>>();
			if (tries >= 4 || tries < 0) return gene2values;
			
			if (geneIds == null || geneIds.size() == 0) return gene2values;

			try {
				StringBuffer geneIdBuffer = toIdBuffer(geneIds);
				resultset = statement.executeQuery("SELECT ID, " +column + " FROM " + table + " WHERE ID IN ("+geneIdBuffer+")");
				while (resultset.next()) {
					int geneId   = resultset.getInt(1);
					String value = resultset.getString(2);

					Set<String> values = gene2values.get(geneId);
					if (values == null){
						values = new HashSet<String>();
						gene2values.put(geneId, values);
					}

					values.add(value);
				}
				
			} catch (com.mysql.jdbc.exceptions.jdbc4.MySQLNonTransientConnectionException e) {
				// if the connection has timed out, re-open it and try again
				openConnection();
				
				// is this a good idea?
				return getValues(table, column, geneIds, tries + 1);
				
			} catch (SQLException sqe) {
				sqe.printStackTrace();
			}

			return gene2values;
		}
		
		
		/**
		 * Returns a comma-separated list of the given IDs in a string. */
		private StringBuffer toIdBuffer(Set<Integer> ids){
			StringBuffer idBuffer = new StringBuffer();
	        for (Integer integer : ids) {
	        	if (idBuffer.length() > 0) {
	        		idBuffer.append(",");
	        	}
	        	idBuffer.append(integer);
	        }
	        return idBuffer;
		}
		
		
		/**
		 * 
		 * @return
		 */
		private boolean openConnection () {
			boolean isOpen = false;
			try {
				if (connection != null && connection.isValid(0)) return true;
				
				Class.forName(ServiceProperties.get("dbDriver"));
				connection = DriverManager.getConnection(
						ServiceProperties.get("dbAccessUrl"),
						ServiceProperties.get("dbUser"),
						ServiceProperties.get("dbPass")
				);
				statement = connection.createStatement();
				openConnection = true;
				isOpen = true;
			} catch (java.sql.SQLException sqle) {
				sqle.printStackTrace();
			} catch (ClassNotFoundException cnfe) {
				cnfe.printStackTrace();
			} catch (Exception e) {
				System.err.println("Exception: unknown host?");
			}
			return isOpen;
		}
	}
	
}

class GeneInfo {
	public String id;
	Map<String, String> data = new HashMap<String, String>();
	public GeneInfo (String id) {
		this.id = id;
	}
	public void add (String key, String value, String delimiter) {
		String newValue = value;
		if (data.containsKey(key))
			newValue = data.get(key) + delimiter + newValue;
		data.put(key, newValue);
	}
	public void set (String key, String value) {
		data.put(key, value);
	}
	public String get (String key) {
		return data.get(key);
	}
	/** Checks if the gene info has more than one entry 
	 *  (there should at least be the origin=species). */
	public boolean hasData () {
		return data.size() > 1;
	}
}