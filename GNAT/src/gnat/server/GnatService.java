package gnat.server;

import gnat.ServiceProperties;
import gnat.retrieval.PmcAccess;
import gnat.retrieval.PubmedAccess;
import gnat.server.dictionary.DictionaryServer;
import gnat.utils.StringHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * The GNAT Service accepts HTTP GET and POST request by GNAT clients and via standard HTTP (wget etc.).
 * It contacts downstream servers (for instance, {@link DictionaryServer}) to annotate gene names, species
 * names, gene IDs, etc.
 * <br><br>
 * Parameters/keys: 
 * <ul>
 * <li>text       - the text to annotate
 * <li>returntype - type of data to return
 *   <ul>
 *   <li>xml: text with inline, XML-style annotations
 *   <li>tsv: tab-separated list of annotiations, with positions, etc.
 *   </ul>
 * <li>species    - list of taxon IDs, depicting the species for which genes should be annotated; default: 9606=human
 * <li>task       - task(s) to perform on the text: speciesNER, geneNER, geneNormalization
 * <li>help       - returns a short description and list of valid parameters; disregards all other parameters
 * </ul>
 * <br><br>
 * A GnatService can be called, for instance, by a {@link gnat.filter.ner.GnatServiceNer} {@link gnat.filter.Filter}.
 * 
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 */
public class GnatService extends HttpService {

	/**
	 * Tasks provided by this service.
	 */
	public enum Tasks {SPECIES_NER,
					   GENE_NER,
					   GENE_NORM,
					   GO_TERMS}
	
	Map<Integer, String> taxonToServerPortMap = new LinkedHashMap<Integer, String>();
	
	/**
	 * 
	 * @param args
	 */
	public static void main (String[] args) throws IOException {
		int port = 8081;
		int log = 0;
		Modes mode = Modes.STATUS;
		
		for (String a: args) {
			if (a.toLowerCase().matches("\\-\\-?p(ort)?=(\\d+)"))
				port = Integer.parseInt(a.replaceFirst("\\-\\-?p(ort)?=(\\d+)", "$2"));
			else if (a.toLowerCase().matches("\\-\\-?log=(\\d+)"))
				log = Integer.parseInt(a.replaceFirst("\\-\\-?log=(\\d+)", "$1"));
			else if (a.equalsIgnoreCase("start"))
				mode = Modes.START;
			else if (a.equalsIgnoreCase("stop"))
				mode = Modes.STOP;
			else
				System.err.println("Unknown parameter: " + a + " - ignoring.");
		}
		
		if (mode == Modes.START) {
			GnatService service = new GnatService();
			service.loadTaxonMap(ServiceProperties.get("dictionaryServer"), ServiceProperties.get("taxon2port"));
			service.logLevel = log;
			service.start(port);
		} else if (mode == Modes.STOP) {
			//service.stop();
			
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
		
		server.createContext("/", new GnatServiceHandler(this.taxonToServerPortMap, this.logLevel));
		server.setExecutor(Executors.newCachedThreadPool());
	    server.start();
	    
	    System.out.println("GnatService started on port " + port + " in " + (System.currentTimeMillis() - start) + "ms.");
	}

	
	
	/**
	 * Loads the mapping of taxon IDs to servers &amp; ports.
	 * @param defaultServer      - address of the default server that handles dictionary requests
	 * @param taxon2portFilename - file that contains the mapping of taxon IDs to 'server:port' addresses; tab-separated;
	 *        if 'server:' is omitted from an entry and only a port given, uses the 'defaultServer' as address for that port     
	 */
	public void loadTaxonMap (String defaultServer, String taxon2portFilename) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(taxon2portFilename));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.matches("\\d+\t.+(\t.*)?")) {
					String[] cols = line.split("\t");
					int taxon     = Integer.parseInt(cols[0]);
					String server = cols[1];
					if (server.matches("\\d+"))
						server = defaultServer + ":" +server;
					if (taxonToServerPortMap.containsKey(taxon))
						System.out.println("#Duplicate entry for taxon " + taxon + " in " + taxon2portFilename + ": mapping it to " + server);
					taxonToServerPortMap.put(taxon, server);
				}
			}
			br.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	
}


class GnatServiceHandler extends ServiceHandler {
	
	Map<Integer, String> taxonToServerPortMap = new HashMap<Integer, String>();
	
	
	/**
	 * 
	 * @param taxonToServerPortMap
	 */
	GnatServiceHandler (Map<Integer, String> taxonToServerPortMap, int logLevel) {
		this.taxonToServerPortMap = taxonToServerPortMap;
		this.logLevel = logLevel;
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
		
		if (logLevel > 3) {
			System.out.println("New request:");
			if (userQuery != null)
				userQuery.printAll();
			System.out.println("------------");
		}
		
		// get an output stream for the response
		OutputStream responseBody = exchange.getResponseBody();
		
		// print a help screen if requested
		if (userQuery == null || userQuery.isEmpty() || // TODO see above - print help only if requested?
			userQuery.hasParameter("help") || userQuery.hasParameter("parameters") || userQuery.hasParameter("getparameters")
			) {
			responseBody.write("This service accepts HTTP GET and POST requests to annotate texts with species and genes.\n\n".getBytes());
			responseBody.write("Valid parameters (submitted as key=value pairs):\n".getBytes());
			responseBody.write("  help        - print a list of supported parameters; will ignore other parameters\n".getBytes());
			responseBody.write("  pmid        - get and annotate these PubMed abstracts (comma-separated list of PubMed IDs)\n".getBytes());
			responseBody.write("  pmc         - get and annotate these full texts from PubMedCentral (comma-separated list of PMC IDs)\n".getBytes());
			responseBody.write("  returntype  - xml: inline XML-style annotations in the submitted text\n".getBytes());
			responseBody.write("                tsv: tab-separated list of annotations, with position, evidence, score; default\n".getBytes());
			responseBody.write("  species     - taxon IDs for the species whose genes to annotate, comma separated; default: 9606\n".getBytes());
			responseBody.write(("  task        - the task(s) to perform on the text, comma separated: speciesNER (sner), geneNER (gner), " +
							    "geneNormalization (gnorm), GO term recognition (gotrec)\n").getBytes());
			responseBody.write("  taxa        - get a list of all supported taxa; will ignore other parameters\n".getBytes());
			responseBody.write("  text        - the text to annotate\n".getBytes());
			responseBody.write("  textid      - an ID that will be assigned to the text submitted via the 'text' parameter\n".getBytes());
			responseBody.write("  textxref    - cross-reference/source for 'textid'\n\n".getBytes());
			responseBody.write(("The parameters 'text', 'pmid', and 'pmc' can be used together. In the response, results for 'text' are displayed " +
					            "first, then for pmid, then for pmc. If 'pmid'/'pmc' had multiple IDs as their value, the results will appear in the " +
					            "same order these IDs were given.\n\n").getBytes());
			responseBody.write(("A minimal request has at least one of 'pmid', 'pmc', or 'text'; with a corresponding value.\n\n").getBytes());
			responseBody.write(("In TSV output, the fields for an entity annotation are text ID (e.g., PubMed ID), text cross-reference (source, " +
								"e.g., PubMed), entity type (gene, goterm), entity subtype (species of the gene, GO branch), " +
								"entity candidate ID(s) [semi-colon separated], start position, end position, mention as found in the text.\n").getBytes());
			responseBody.write("\n".getBytes());
			responseBody.close();
			return;
		}
		
		
		// print a list of all supported taxa if requested
		if (userQuery.hasParameter("taxa")) {
			responseBody.write("Taxon\tServer status\n".getBytes());
			for (int taxon: taxonToServerPortMap.keySet()) {
				String addr = taxonToServerPortMap.get(taxon);
				boolean online = checkDictionaryServer(addr);
				
				responseBody.write((taxon + "\t").getBytes());
				if (online)
					responseBody.write("online".getBytes());
				else
					responseBody.write("offline".getBytes());
				responseBody.write("\n".getBytes());
			}
			responseBody.write("\n".getBytes());
			responseBody.close();
			return;
		}
		

		// set some default parameters
		if (userQuery.hasParameter("returntype")) {
			// by default, return a tab-separated list, not XML
			String retType = userQuery.getValue("returntype");
			if (retType.equalsIgnoreCase("xml"))
				userQuery.setValue("returntype", "xml");
			else
				userQuery.setValue("returntype", "tsv");
		} else
			userQuery.setValue("returntype", "tsv");
		
		// set the tasks to perform on the text
		Set<String> annotationTasks = new LinkedHashSet<String>();
		if (userQuery.hasParameter("task")) {
			String[] tasks = userQuery.getValue("task").split("\\s*[\\,\\;]\\s*");
			for (String task: tasks) {
				task = task.toLowerCase();
				if (task.equals("speciesner") || task.equals("sner") || task.equals("species"))
					annotationTasks.add("sner");
				else if (task.equals("genener") || task.equals("gner") || task.equals("gene") || task.equals("genes"))
					annotationTasks.add("gner");
				else if (task.equals("genenormalization") || task.equals("gnorm")) {
					annotationTasks.add("sner");
					annotationTasks.add("gner");
					annotationTasks.add("gnorm");
				} else if (task.equals("gotermrecognition") || task.equals("gotrec") || task.equals("goterms")) {
					annotationTasks.add("goterms");
				} else
					responseBody.write(("<error>Unrecognized task in request: '" + task + "', ignoring.</error>\n").getBytes());
			}
			// only some unsupported tasks given via parameter? use default: gene NER
			if (annotationTasks.size() == 0) {
				//userQuery.setValue("tasks", "gner");
				annotationTasks.add("gner");
			}				
		// if no list of tasks is given, use the default task: gene NER
		} else {
			//userQuery.setValue("tasks", "gner");
			annotationTasks.add("gner");
		}
		
		// get the species requested by the user
		Set<Integer> requestedSpecies = new LinkedHashSet<Integer>();
		if (userQuery.hasParameter("species")) {
			String[] species = userQuery.getValue("species").split("\\s*[\\;\\,]\\s*");
			for (String s: species) {
				if (s.matches("\\d+")) {
					requestedSpecies.add(Integer.parseInt(s));
				} else if (s.equals("all")) {
					Set<Integer> allSpecies = taxonToServerPortMap.keySet();
					for (int aSpec: allSpecies)
						requestedSpecies.add(aSpec);
				} else
					responseBody.write(("<error>Unrecognized species in request: '" + s + "'. Use NCBI Taxonomy IDs only.</error>\n").getBytes());
			}
		}
		// set human=9606 as default species if none were given by the user
		if (requestedSpecies.size() == 0)
			requestedSpecies.add(9606);
		// check if connections can be establised to each of the requested taxa
		Set<Integer> failedTaxa = new HashSet<Integer>();
		for (int taxon: requestedSpecies) {
			if (!taxonToServerPortMap.containsKey(taxon)) {
				responseBody.write(("<error>Species '" + taxon + "' currently not supported: no mapping to dictionary server found.</error>\n").getBytes());
				failedTaxa.add(taxon);
			} else if (!checkDictionaryServer(taxonToServerPortMap.get(taxon))) {
				responseBody.write(("<error>Error contacting the dictionary server for species '" + taxon + "' at the address " + taxonToServerPortMap.get(taxon) + ".</error>\n").getBytes());				
				failedTaxa.add(taxon);
			}
		}
		requestedSpecies.removeAll(failedTaxa);
		//
		if (requestedSpecies.size() == 0) {
			// TODO
			responseBody.write(("<error>No valid species left for this request; removed the invalid taxa " + failedTaxa + ".</error>\n").getBytes());				
		}
		
		
		List<RequestedText> textsToAnnotate = new LinkedList<RequestedText>();

		// add texts given directly via the 'text' parameter in the query
		String queryText = userQuery.getValue("text");
		if (queryText != null) {
			// get ID and XRef if given in the query
			String textId   = "UnknownId";
			String textXref = "UserQuery";
			if (userQuery.hasParameter("textid"))
				textId = userQuery.getValue("textid");
			if (userQuery.hasParameter("textxref"))
				textXref = userQuery.getValue("textxref");
				
			RequestedText newText = new RequestedText(textId, textXref, queryText);
			textsToAnnotate.add(newText);
		}

		// add texts given via the PubMedID parameter: download from PubMed
		String pmids = userQuery.getValue("pmid");
		if (pmids != null) {
			if (pmids.matches("\\d+(\\s*[\\,\\;]\\s*\\d+)*")) {
				String[] pmidArray = pmids.split("\\s*[\\,\\;]\\s*");
				// get all abstracts at once
				String[][] titlesAndAbstracts = PubmedAccess.getAbstractsAsTitleAndText(pmidArray);
				for (int p = 0; p < titlesAndAbstracts.length; p++) {
					String[] text = titlesAndAbstracts[p];
					RequestedText newText = new RequestedText(pmidArray[p], "PubMed", text[0], text[1]);
					textsToAnnotate.add(newText);
					//responseBody.write((newText.id + " (" + newText.xref + ")\n"
					//		+ (newText.text + "\n")).getBytes());
				}
			} else {
				// TODO invalid PubMed ID(s) given
			}
		}

		// add texts given via the PubMedCentral parameter: download from PubMedCentral
		String pmcids = userQuery.getValue("pmc");
		if (pmcids != null) {
			if (pmcids.toLowerCase().matches("(pmc)?\\d+(\\s*[\\,\\;]\\s*(pmc)?\\d+)*")) {
				String[] pmcArray = pmcids.split("\\s*[\\,\\;]\\s*");
				// get each full article, one by one
				for (String pmcId: pmcArray) {
					pmcId = pmcId.toUpperCase();
					String pmcIdNumber = pmcId.replaceFirst("^(PMC)?(\\d+)$", "$2");
					pmcId = "PMC" + pmcIdNumber;
					
					if (logLevel > 3)
						System.out.println("Getting PMC" + pmcIdNumber + " via OAI");
					String xml = PmcAccess.getArticle(Integer.parseInt(pmcIdNumber));
					
					//if (logLevel > 4)
					//	System.out.println("  size of XML: " + xml.length());
					//if (logLevel > 5)
					//	System.out.println(xml);
										
					String fulltext = PmcAccess.getPlaintext(xml);
					//if (logLevel > 4)
					//	System.out.println("  size of full plain text: " + fulltext.length());
					
					if (fulltext.length() == 0)
						responseBody.write(("<message>The PubMedCentral text " + pmcId + " is not available through Open Access or not in XML form.</message>\n").getBytes());
					
					RequestedText text = new RequestedText(pmcId, "PubMedCentral", fulltext);
					textsToAnnotate.add(text);
				}
			} else {
				// TODO invalid PMC ID(s) given
			}
		}


		List<AnnotatedText> annotatedTexts = new LinkedList<AnnotatedText>();
		for (RequestedText qText : textsToAnnotate) {
			AnnotatedText aText = new AnnotatedText(qText);
			annotatedTexts.add(aText);
		}
		
		
		// perform each requested task, in the order species NER, gene NER, gene normalization
		// perform species NER?
		if (annotationTasks.contains("sner")) {
			responseBody.write(("<message>Named entity recognition for species not yet implemented.</message>\n").getBytes());
		}
		
		// perform gene NER?
		if (annotationTasks.contains("gner")) {
			annotatedTexts = geneNer(annotatedTexts, requestedSpecies);
		} // if geneNER was requested
		
		
		// perform GO term recognition?
		if (annotationTasks.contains("goterms")) {
			annotatedTexts = goTermRecognition(annotatedTexts);
		}
		
		
		// perform gene normalization?
		if (annotationTasks.contains("gnorm")) {
			responseBody.write(("<message>Gene mention normaliation is not yet implemented.</message>\n").getBytes());
		}

		
		// return the results for each queried text as a response
		for (AnnotatedText aText: annotatedTexts) {
			if (userQuery.getValue("returntype").equals("xml")) {
				responseBody.write(aText.toXml(false).getBytes());
				//responseBody.write("\n".getBytes());
			} else {
				responseBody.write(aText.toTsv().getBytes());
			}
		}

		responseBody.close();	    
	}

	
	/**
	 * 
	 * @param annotatedTexts
	 * @return
	 */
	public List<AnnotatedText> geneNer (List<AnnotatedText> annotatedTexts, Collection<Integer> requestedSpecies) {
		// TODO could be merged into one request to the DictionaryServer that has all texts at once

		for (int a = 0; a < annotatedTexts.size(); a++) {
			AnnotatedText aText = annotatedTexts.get(a);
			// the dictionary taggers expect each individual text encapsulated in <text> tags
			// send the user query a single text for now TODO switch to multiple texts later?
			StringBuffer buffer = new StringBuffer();
			buffer.append("<text>");
			buffer.append(aText.text.replaceAll("[\\n\\r]+", " "));
			buffer.append("</text>");
			String preparedText = buffer.toString();
							
			for (int currentSpecies: requestedSpecies) {
				
				String serverAddress = taxonToServerPortMap.get(currentSpecies);
				String serverName = serverAddress.split("\\:")[0];
				String serverPort = serverAddress.split("\\:")[1];
	
				try {
					Socket socket = new Socket(serverName, Integer.parseInt(serverPort));
					BufferedReader dictionaryReader = new BufferedReader(new InputStreamReader(
							new BufferedInputStream(socket.getInputStream() ), "UTF-8") );
					BufferedWriter dictionaryWriter = new BufferedWriter(new OutputStreamWriter(
							new BufferedOutputStream(socket.getOutputStream()), "UTF-8") );
	
					dictionaryWriter.write(preparedText);
					dictionaryWriter.newLine();
					dictionaryWriter.flush();
					String annotations = dictionaryReader.readLine();
	
					// entities are returned by the dictionary server as a list of entities, one entry per <text>
					// each entry in this list contains a string where entities are enclosed in <entity> tags
					List<String> entities = this.extractEntityTags(annotations);
					if (entities != null && entities.size() > 0) {
						//responseBody.write("<entities>\n".getBytes());
	
						// get only the first entry for the first text (here: only one text is sent anyway) TODO switch to multiple texts later
						String entityString = entities.remove(0);
						//addRecognizedEntities(context, text, entityString);
	
						while (entityString.matches("([\\s\\t]*)(<entity.+?</entity>)(.*)")) {
							String currentEntity = entityString.replaceFirst("([\\s\\t]*)(<entity.+?</entity>)(.*)", "$2");
							entityString = entityString.replaceFirst("([\\s\\t]*)(<entity.+?</entity>)(.*)", "$3");
							//responseBody.write(currentEntity.getBytes());
							//responseBody.write("\n".getBytes());
							
							// add information about the dictionary: entity type and sub-type (=species for the gene, GO-branch for GO terms, ...)
							currentEntity = currentEntity.replaceFirst("<entity ", "<entity type=\"gene\" subtype=\"" + currentSpecies + "\" ");
							aText.addAnnotation(currentEntity);
						}
	
						//responseBody.write("</entities>\n".getBytes());
	
					} else { // no entities were found	
						//responseBody.write("<entities>\n".getBytes());
						//responseBody.write(("<comment msg=\"No entities were found for species " + currentSpecies + "\"/>\n").getBytes());
						//responseBody.write("</entities>\n".getBytes());
						aText.addAnnotation("<comment msg=\"No entities were found for species " + currentSpecies + "\"/>");
					}
	
					dictionaryReader.close();
					dictionaryWriter.close();
					socket.close();
					
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			} // for each species
			
			// overwrite old, unannotated text
			annotatedTexts.set(a, aText);
			
		} // for each text
		
		return annotatedTexts;
	}
	
	
	/**
	 * 
	 * @param annotatedTexts
	 * @return
	 */
	public List<AnnotatedText> goTermRecognition (List<AnnotatedText> annotatedTexts) {
		// TODO could be merged into one request to the DictionaryServer that has all texts at once

		for (int a = 0; a < annotatedTexts.size(); a++) {
			AnnotatedText aText = annotatedTexts.get(a);
			// the dictionary taggers expect each individual text encapsulated in <text> tags
			// send the user query a single text for now TODO switch to multiple texts later?
			StringBuffer buffer = new StringBuffer();
			buffer.append("<text>");
			buffer.append(aText.text.replaceAll("[\\n\\r]+", " "));
			buffer.append("</text>");
			String preparedText = buffer.toString();
							
			String serverAddress = ServiceProperties.get("dictionaryServerGO");
			int serverPort = 80;
			if (serverAddress.matches(".+\\:\\d+"))
				serverPort = Integer.parseInt(serverAddress.replaceFirst("(.+)\\:(\\d+)", "$2"));
			String serverName = serverAddress.replaceFirst("(.+)\\:(\\d+)", "$1");
	
			try {
				Socket socket = new Socket(serverName, serverPort);
				BufferedReader dictionaryReader = new BufferedReader(new InputStreamReader(
						new BufferedInputStream(socket.getInputStream() ), "UTF-8") );
				BufferedWriter dictionaryWriter = new BufferedWriter(new OutputStreamWriter(
						new BufferedOutputStream(socket.getOutputStream()), "UTF-8") );

				dictionaryWriter.write(preparedText);
				dictionaryWriter.newLine();
				dictionaryWriter.flush();
				String annotations = dictionaryReader.readLine();

				// entities are returned by the dictionary server as a list of entities, one entry per <text>
				// each entry in this list contains a string where entities are enclosed in <entity> tags
				List<String> entities = this.extractEntityTags(annotations);
				if (entities != null && entities.size() > 0) {
					//responseBody.write("<entities>\n".getBytes());

					// get only the first entry for the first text (here: only one text is sent anyway) TODO switch to multiple texts later
					String entityString = entities.remove(0);
					//addRecognizedEntities(context, text, entityString);

					while (entityString.matches("([\\s\\t]*)(<entity.+?</entity>)(.*)")) {
						String currentEntity = entityString.replaceFirst("([\\s\\t]*)(<entity.+?</entity>)(.*)", "$2");
						entityString = entityString.replaceFirst("([\\s\\t]*)(<entity.+?</entity>)(.*)", "$3");
						//responseBody.write(currentEntity.getBytes());
						//responseBody.write("\n".getBytes());
						
						//System.out.println(currentEntity);
						
						String idString = currentEntity.replaceFirst("^.*\\sids?=\"([^\"]*)\".*$", "$1");
						// no IDs? => skip
						if (idString.length() == 0 || idString.equals(currentEntity)) continue;
						// just a MeSH term; skipping
						if (!idString.matches(".*(^|\\;)G\\d+(\\;|$).*")) continue;
						
						// grab all GO codes
						List<String> goIds = new LinkedList<String>();
						while (idString.matches("^(.*)(^|\\;)(G\\d+)(\\;|$)(.*)$")) {
							String id = idString.replaceFirst("^(.*)(^|\\;)(G\\d+)(\\;|$)(.*)$", "$3");
							id = id.replaceFirst("^G0+", "");
							//System.out.println("  found GO-ID: " + id);
							goIds.add(id);
							idString  = idString.replaceFirst("^(.*)(^|\\;)(G\\d+)(\\;|$)(.*)$", "$1$2$4$5");
							//System.out.println("  remaining IDs: ''" + idString + "''");
						}
						
						idString = StringHelper.joinStringList(goIds, ";");
						currentEntity = currentEntity.replaceFirst(" ids=\"[^\"]+\"", " ids=\"" + idString + "\"");

						// add information about the dictionary: entity type and sub-type (=species for the gene, GO-branch for GO terms, ...)
						currentEntity = currentEntity.replaceFirst("<entity ", "<entity type=\"goterm\" subtype=\"-\" ");
						aText.addAnnotation(currentEntity);
					}

					//responseBody.write("</entities>\n".getBytes());

				} // else: no GO terms found in this text

				dictionaryReader.close();
				dictionaryWriter.close();
				socket.close();

			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// overwrite old, unannotated text
			annotatedTexts.set(a, aText);
			
		} // for each text
		
		return annotatedTexts;
	}
	
	
	/**
	 * Tries to establish a connection to the given address. Format of <tt>serverAddress</tt>: <tt>IP:Port</tt> or <tt>DNS-Name:Port</tt>. 
	 * @param address
	 * @return true if a socket could be created successfully
	 */
	boolean checkDictionaryServer (String serverAddress) {
		String serverName = serverAddress.split("\\:")[0];
		String serverPort = serverAddress.split("\\:")[1];
		try {
			Socket socket = new Socket(serverName, Integer.parseInt(serverPort));
			socket.close();
		} catch (Exception e) {
			if (logLevel > 3)
				System.err.println("#Error contacting server " + serverAddress);
			return false;
		}
		return true;
	}

	
	/**
	 * Returns a list of entity mark-ups found in annotated texts.
	 * */
	private List<String> extractEntityTags(String annotatedTexts) {
		List<String> entities = new LinkedList<String>();
		if (annotatedTexts == null)
			return entities;

		int textTagBeginIndex = annotatedTexts.indexOf("<text");
		while (textTagBeginIndex != -1) {
			int textTagEndIndex = annotatedTexts.indexOf("</text>", textTagBeginIndex);
			String singleAnnotatedText = annotatedTexts.substring(textTagBeginIndex, textTagEndIndex);
			String annotations = singleAnnotatedText.substring(singleAnnotatedText.indexOf(">") + 1);
			if (annotations == null || annotations.length() == 0) {
				entities.add(" ");
			}
			else {
				entities.add(annotations);
			}
			textTagBeginIndex = annotatedTexts.indexOf("<text", textTagEndIndex);
		}

		return entities;
	}

}


