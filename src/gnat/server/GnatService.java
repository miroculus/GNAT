package gnat.server;

import gnat.ServiceProperties;
import gnat.filter.ner.LinnaeusSpeciesServiceNer;
import gnat.representation.GeneContextModel;
import gnat.representation.RecognizedEntity;
import gnat.representation.Text;
import gnat.representation.TextAnnotation;
import gnat.representation.TextContextModel;
import gnat.representation.TextRange;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import martin.common.ArgParser;

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
 * <li>task       - task(s) to perform on the text: speciesNER, geneNER, geneNormalization, goTermRecognition; default: gner
 * <li>taxa       - get a list of all supported gene dictionaries (for each taxon) and their status (online/offline)
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
	public enum Tasks {UNKNOWN,
		SPECIES_NER,
		GENE_NER,
		GENE_NORM,
		GO_TERMS;
	public static Tasks getValue(String string) {
		string = string.toLowerCase();
		if (string.equals("sner") || string.equals("species") || string.equals("speciesner"))
			return SPECIES_NER;
		else if (string.equals("gner") || string.equals("genes") || string.equals("genener"))
			return GENE_NER;
		else if (string.equals("gnorm") || string.equals("genenormalization"))
			return GENE_NORM;
		else if (string.equals("goterms") || string.equals("goterm") || string.equals("gotermrecognition"))
			return GO_TERMS;
		else 
			return UNKNOWN;
	}
	}

	private Set<Tasks> providesTasks = new HashSet<Tasks>();
	private Set<Tasks> defaultTasks  = new HashSet<Tasks>();
	private Map<Integer, String> taxonToServerPortMap = new LinkedHashMap<Integer, String>();

	/**
	 * 
	 * @param args
	 */
	public static void main (String[] args) throws IOException {
		Set<Tasks> providesTasks = new HashSet<Tasks>();
		Set<Tasks> defaultTasks  = new HashSet<Tasks>();

		ArgParser ap = new ArgParser(args);

		int port = ap.getInt("port", 8081);
		int log = ap.getInt("log", 0);

		String provides = ServiceProperties.get("providesTasks");
		String defaults = ServiceProperties.get("defaultTasks");

		check(provides, defaults, providesTasks, defaultTasks);

		GnatService service = new GnatService();
		service.loadTaxonMap(ServiceProperties.get("dictionaryServer"), ServiceProperties.get("taxon2port"));
		service.logLevel = log;
		service.providesTasks = providesTasks;
		service.defaultTasks  = defaultTasks;
		service.start(port);
	}

	private static void check(String provides, String defaults, Set<Tasks> providesTasks, Set<Tasks> defaultTasks) {
		if (provides == null || provides.trim().length() == 0) {
			System.err.println("Set the tasks this service should provide in the entry 'providesTasks' in " +
					"" + ServiceProperties.getPropertyFilename());
			System.err.println("Select one or more from species NER ('sner'), gene NER ('gner'), " +
			"GO term recognition ('goterms'), gene normalization ('gnorm')");
			System.err.println("For example,");
			System.err.println("  <entry key=\"providesTasks\">gner,goterms</entry>");
			System.exit(2);
		}
		for (String task: provides.split("\\s*[\\;\\,]\\s*")) {
			Tasks aTask = Tasks.getValue(task);
			if (aTask != null)
				providesTasks.add(aTask);
			else {
				System.err.println("Unrecognized task to provide: '" + task + "'");
				System.err.println("Select one or more from species NER ('sner'), gene NER ('gner'), " +
				"GO term recognition ('goterms'), gene normalization ('gnorm')");
				System.err.println("Exiting.");
				System.exit(3);
			}
		}

		if (defaults == null || defaults.trim().length() == 0) {
			System.err.println("Set the tasks this service should provide by default in the entry 'defaultTasks' in " +
					"" + ServiceProperties.getPropertyFilename());
			System.err.println("Select one or more from species NER ('sner'), gene NER ('gner'), " +
			"GO term recognition ('goterms'), gene normalization ('gnorm')");
			System.err.println("For example,");
			System.err.println("  <entry key=\"defaultTasks\">gner</entry>");
			System.exit(2);
		}

		for (String task: defaults.split("\\s*[\\;\\,]\\s*")) {
			Tasks aTask = Tasks.getValue(task);
			if (aTask != null)
				defaultTasks.add(aTask);
			else {
				System.err.println("Unrecognized default task: '" + task + "'");
				System.err.println("Select one or more from species NER ('sner'), gene NER ('gner'), " +
				"GO term recognition ('goterms'), gene normalization ('gnorm')");
				System.err.println("Exiting.");
				System.exit(3);
			}
		}
	}

	/**
	 * 
	 * @param port
	 * @throws IOException
	 */
	private void start (int port) throws IOException {
		InetSocketAddress addr = new InetSocketAddress(port);
		server = HttpServer.create(addr, 0);

		server.createContext("/", new GnatServiceHandler(this.taxonToServerPortMap, this.logLevel, this.providesTasks, this.defaultTasks));
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();

		System.out.println("GnatService started on port " + port);
	}

	/**
	 * Loads the mapping of taxon IDs to servers &amp; ports.
	 * @param defaultServer      - address of the default server that handles dictionary requests
	 * @param taxon2portFilename - file that contains the mapping of taxon IDs to 'server:port' addresses; tab-separated;
	 *        if 'server:' is omitted from an entry and only a port given, uses the 'defaultServer' as address for that port     
	 */
	private void loadTaxonMap (String defaultServer, String taxon2portFilename) {
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
	private Map<Integer, String> taxonToServerPortMap;
	private Set<GnatService.Tasks> providesTasks;
	private Set<GnatService.Tasks> defaultTasks;
	private GnatServicePipe pipe;

	/**
	 * 
	 * @param taxonToServerPortMap
	 * @param logLevel
	 * @param providesTasks
	 * @param defaultTasks
	 */
	GnatServiceHandler (Map<Integer, String> taxonToServerPortMap, int logLevel,
			Set<GnatService.Tasks> providesTasks, Set<GnatService.Tasks> defaultTasks) {
		this.taxonToServerPortMap = taxonToServerPortMap;
		this.logLevel = logLevel;
		this.providesTasks = providesTasks;
		this.defaultTasks = defaultTasks;
		
		if (providesTasks.contains(gnat.server.GnatService.Tasks.GENE_NORM))
			this.pipe = new GnatServicePipe();
	}


	/**
	 * Handles HTTP requests (only GET and POST are implemented).
	 * @param exchange
	 */
	public synchronized void handle (HttpExchange exchange) throws IOException {
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
		if (userQuery == null || userQuery.isEmpty() || userQuery.hasParameter("help") || userQuery.hasParameter("parameters") || userQuery.hasParameter("getparameters")) {
			doHelp(responseBody);
			return;
		}

		// print a list of all supported taxa if requested
		if (userQuery.hasParameter("taxa")) {
			doTaxa(responseBody);
			return;
		}

		setReturnType(userQuery);
		Set<GnatService.Tasks> annotationTasks = getTasks(userQuery, responseBody);
		List<AnnotatedText> annotatedTexts = getTexts(userQuery, responseBody);

		// perform each requested task, in the order species NER, gene NER, GO term, gene normalization
		// perform species NER?
		if (annotationTasks.contains(GnatService.Tasks.SPECIES_NER))
			speciesNer(annotatedTexts);

		Set<Integer> requestedSpecies = getSpecies(userQuery, responseBody, annotatedTexts);
		
		// perform gene NER?
		if (annotationTasks.contains(GnatService.Tasks.GENE_NER))
			annotatedTexts = geneNer(annotatedTexts, requestedSpecies);

		// perform GO term recognition?
		if (annotationTasks.contains(GnatService.Tasks.GO_TERMS))
			annotatedTexts = goTermRecognition(annotatedTexts);

		// perform gene normalization?
		if (annotationTasks.contains(GnatService.Tasks.GENE_NORM)) 
			//responseBody.write(("<message>Gene mention normaliation is not yet implemented.</message>\n").getBytes());
			annotatedTexts = geneNormalization(annotatedTexts);

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

	private void speciesNer(List<AnnotatedText> annotatedTexts) {
		LinnaeusSpeciesServiceNer linn = new LinnaeusSpeciesServiceNer(ServiceProperties.get("linnaeusUrl"));
		for (AnnotatedText a : annotatedTexts)
			linn.annotate(a);
	}

	private List<AnnotatedText> getTexts(Request userQuery,
			OutputStream responseBody) throws IOException {
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

		return annotatedTexts;
	}


	private Set<Integer> getSpecies(Request userQuery, OutputStream responseBody, List<AnnotatedText> annotatedTexts) throws IOException {
		Set<Integer> requestedSpecies = new LinkedHashSet<Integer>();

		for (AnnotatedText a : annotatedTexts){
			for (String l : a.toTsv().split("\n")){
				String[] fs = l.split("\t");
				if (fs.length > 4 && fs[2].equals("species"))
					requestedSpecies.add(Integer.parseInt(fs[4]));
			}
		}
		
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
		if (requestedSpecies.size() == 0) {
			responseBody.write(("<error>No valid species left for this request; removed the invalid taxa " + failedTaxa + ".</error>\n").getBytes());				
		}

		return requestedSpecies;
	}


	private Set<GnatService.Tasks> getTasks(Request userQuery, OutputStream responseBody) throws IOException {
		Set<GnatService.Tasks> annotationTasks = new LinkedHashSet<GnatService.Tasks>();
		if (userQuery.hasParameter("task")) {
			String[] tasks = userQuery.getValue("task").split("\\s*[\\,\\;]\\s*");
			for (String task: tasks) {
				GnatService.Tasks aTask = GnatService.Tasks.getValue(task);
				if (aTask != null) {
					if (aTask == GnatService.Tasks.GENE_NORM) {
						// TODO don't run GENE_NORM if any of the sub-tasks is not provided by this service
						annotationTasks.add(GnatService.Tasks.SPECIES_NER);
						annotationTasks.add(GnatService.Tasks.GENE_NER);
						annotationTasks.add(GnatService.Tasks.GO_TERMS);
						annotationTasks.add(GnatService.Tasks.GENE_NORM);
					} else {
						if (providesTasks.contains(aTask))
							annotationTasks.add(aTask);
						else
							responseBody.write(("<error>The task '" + task + "' is not provided by this serivce; ignoring.</error>\n").getBytes());
					}
				} else
					responseBody.write(("<error>Unrecognized task in request: '" + task + "', ignoring.</error>\n").getBytes());
			}
			// only some unsupported tasks given via parameter? use defaults
			if (annotationTasks.size() == 0) {
				annotationTasks.addAll(defaultTasks);
			}				
			// if no list of tasks is given, use the default task(s)
		} else {
			//userQuery.setValue("tasks", "gner");
			annotationTasks.clear();
			annotationTasks.addAll(defaultTasks);
		}
		
		return annotationTasks;
	}


	private void setReturnType(Request userQuery) {
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
	}


	private void doTaxa(OutputStream responseBody) throws IOException {
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
	}


	private void doHelp(OutputStream responseBody) throws IOException {
		
		responseBody.write(
				("This service accepts HTTP GET and POST requests to annotate texts with species and genes.\n" + 
						"\n" + 
						"Valid parameters for this service are:\n" + 
						"\n" + 
						"  help        - print a list of supported parameters (will ignore other parameters)\n" + 
						"  taxa        - get a list of all supported taxa (will ignore other parameters)\n" + 
						"  pmid        - ID(s) of PubMed record to get and annotate\n" + 
						"  pmc         - ID(s) of PubMedCentral record to get and annotate\n" + 
						"  text        - a text string to annotate\n" + 
						"  textid      - ID that will be assigned to the text submitted via the 'text' parameter\n" + 
						"  textxref    - the source or database cross-reference for the 'textid' parameter \n" + 
						"  species     - NCBI taxon ID(s) for the species whose genes to annotate (default: 9606)\n" + 
						"  task        - gner: perform the gene NER task on the text (default)\n" + 
						"              - gnorm: perform the gene normalization task on the text\n" + 
						"              - sner: perform the species NER task on the text\n" + 
						"  returntype  - tsv: tab-separated list of annotations (default, see format below)\n" + 
						"              - xml: XML-style list of annotations\n" + 
						"\n" + 
						"Parameters are submitted as key=value(s) pairs, beginning with a ? after the base URL. A \n" + 
						"minimal request has at least one of 'pmid', 'pmc', or 'text'; with a corresponding value.\n" + 
						"The parameters 'pmid', 'pmc' and 'species' can take a comma-separated list of PubMed, \n" + 
						"PubMedCentral or NCBI taxonomy IDs, respectively. If 'pmid' or 'pmc' parameters have \n" + 
						"multiple IDs as their value, the results will appear in the same order these IDs were \n" + 
						"given. The parameters 'text', 'pmid', and 'pmc' can be used together. In the response, \n" + 
						"results for 'text' are displayed first, then for pmid, then for pmc. Multiple  tasks can \n" + 
						"be performed on the text by providing a comma separated list to the 'task' parameter. \n" + 
						"\n" + 
						"In TSV output, the fields for an entity annotation are text ID (e.g., PubMed ID), text \n" + 
						"cross-reference (e.g., PubMed), entity type (e.g. gene, goterm), entity subtype (e.g. \n" + 
						"species of the gene, GO branch), entity candidate ID(s) [semi-colon separated] (e.g. \n" + 
						"Entrez gene ID, GO code), 0-based start position in the text, 0-based end position in \n" + 
						"the text, mention as found in the text, and a confidence score (optional; higher is better).\n" + 
						"\n" + 
						"**Examples**\n" + 
						"\n" + 
						"1) Print this help menu:\n" + 
						"http://bergmanlab.smith.man.ac.uk:8081/?help\n" + 
						"\n" + 
						"2) Annotate gene mentions in the abstracts of PMIDs 21483786 & 21483692:\n" + 
						"http://bergmanlab.smith.man.ac.uk:8081/?pmid=21483786,21483692\n" + 
						"\n" + 
						"3) Annotate gene mentions in the full text of PMCID PMC3069089 & abstract of PMID \n" + 
						"21483786:\n" + 
						"http://bergmanlab.smith.man.ac.uk:8081/?pmc=PMC3069089&pmid=21483786\n" + 
						"\n" + 
						"4) Annotate gene mentions in the text string \"p53 gene\":\n" + 
						"http://bergmanlab.smith.man.ac.uk:8081/?text=p53 gene\n" + 
						"\n" + 
						"5) Annotate gene mentions in the text string \"p53 gene\" using both human and mouse \n" + 
						"Entrez gene IDs:\n" + 
						"http://bergmanlab.smith.man.ac.uk:8081/?text=p53 gene&species=9606,10090\n" + 
						"\n" + 
						"6) Annotate species and gene mentions in the full text of PMCID PMC3069089:\n" + 
						"http://bergmanlab.smith.man.ac.uk:8081/?pmc=PMC3069089&task=sner,gner\n" + 
						"\n" + 
						"7) Generate XML files of normalized gene mentions and associated data for PMCID\n" + 
						"PMC3069089:\n" + 
						"http://bergmanlab.smith.man.ac.uk:8081/?pmc=PMC3069089&task=gnorm&returntype=xml\n").getBytes());
		
		
//		responseBody.write("This service accepts HTTP GET and POST requests to annotate texts with species and genes.\n\n".getBytes());
//		responseBody.write("Valid parameters (submitted as key=value pairs):\n".getBytes());
//		responseBody.write("  help        - print a list of supported parameters; will ignore other parameters\n".getBytes());
//		responseBody.write("  pmid        - get and annotate these PubMed abstracts (comma-separated list of PubMed IDs)\n".getBytes());
//		responseBody.write("  pmc         - get and annotate these full texts from PubMedCentral (comma-separated list of PMC IDs)\n".getBytes());
//		responseBody.write("  returntype  - xml: inline XML-style annotations in the submitted text\n".getBytes());
//		responseBody.write("                tsv: tab-separated list of annotations, with position, evidence, score; default\n".getBytes());
//		responseBody.write("  species     - taxon IDs for the species whose genes to annotate, comma separated; default: 9606\n".getBytes());
//		responseBody.write(("  task        - the task(s) to perform on the text, comma separated: speciesNER (sner), geneNER (gner), " +
//		"geneNormalization (gnorm), GO term recognition (gotrec)\n").getBytes());
//		responseBody.write("  taxa        - get a list of all supported taxa; will ignore other parameters\n".getBytes());
//		responseBody.write("  text        - the text to annotate\n".getBytes());
//		responseBody.write("  textid      - an ID that will be assigned to the text submitted via the 'text' parameter\n".getBytes());
//		responseBody.write("  textxref    - cross-reference/source for 'textid'\n\n".getBytes());
//		responseBody.write(("The parameters 'text', 'pmid', and 'pmc' can be used together. In the response, results for 'text' are displayed " +
//				"first, then for pmid, then for pmc. If 'pmid'/'pmc' had multiple IDs as their value, the results will appear in the " +
//		"same order these IDs were given.\n\n").getBytes());
//		responseBody.write(("A minimal request has at least one of 'pmid', 'pmc', or 'text'; with a corresponding value.\n\n").getBytes());
//		responseBody.write(("In TSV output, the fields for an entity annotation are text ID (e.g., PubMed ID), text cross-reference (source, " +
//				"e.g., PubMed), entity type (gene, goterm), entity subtype (species of the gene, GO branch), " +
//				"entity candidate ID(s) [semi-colon separated], start position, end position, mention as found in the text, " +
//		"and a score (optional).\n").getBytes());
//		responseBody.write("\n".getBytes());
		responseBody.close();
	}


	/**
	 * 
	 * @param annotatedTexts
	 * @return
	 */
	private List<AnnotatedText> geneNer (List<AnnotatedText> annotatedTexts, Collection<Integer> requestedSpecies) {
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
	private List<AnnotatedText> goTermRecognition (List<AnnotatedText> annotatedTexts) {
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
						currentEntity = currentEntity.replaceFirst("<entity ", "<entity type=\"gocode\" subtype=\"-\" ");
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
	 * 
	 * @param annotatedTexts
	 * @return
	 */
	private List<AnnotatedText> geneNormalization (List<AnnotatedText> annotatedTexts) {
		// TODO could be merged into one request to the DictionaryServer that has all texts at once

		pipe.run.context.clear();
		
		for (int a = 0; a < annotatedTexts.size(); a++) {
			AnnotatedText annotatedText = annotatedTexts.get(a);
			Text text = new Text(annotatedText.id, annotatedText.text);

			// every Text needs a context model
			TextContextModel tcm = new TextContextModel(text.ID);
			tcm.addPlainText(text.getPlainText());

			// add the extracted context model to the text
			text.setContextModel(tcm);

			List<String> keptAnnotations = new LinkedList<String>();
			// convert gene annotations into RecognizedGenes with IdentificationStatus for IDs
			// convert species annotations into taxon IDs
			// convert GO codes into a ContextModel vector
			for (String annotation: annotatedText.getAnnotations()) {
				if (annotation.startsWith("<entity")) {
					String type = annotation.replaceFirst("^<entity.* type=\"(.*?)\".*$", "$1");
					String subtype = annotation.replaceFirst("^<entity.* subtype=\"(.*?)\".*$", "$1");
					String ids = annotation.replaceFirst("^<entity.* ids=\"(.*?)\".*$", "$1");
					String startIndex = annotation.replaceFirst("^<entity.* startIndex=\"(.*?)\".*$", "$1");
					String endIndex = annotation.replaceFirst("^<entity.* endIndex=\"(.*?)\".*$", "$1");
					String name = annotation.replaceFirst("^<entity.*>(.*?)</entity>$", "$1");

					int start = Integer.parseInt(startIndex);
					int end   = Integer.parseInt(endIndex);
					TextRange textRange = new TextRange(start, end);

					if (type.equals("species")) {
						String[] taxa = ids.split("\\s*[\\;\\,]\\s*");
						for (String taxon: taxa)
							if (taxon.matches("\\d+"))
								text.addTaxonWithName(Integer.parseInt(taxon), name);

						keptAnnotations.add(annotation);
						continue;
					}

					if (type.equals("gocode")) {
						String[] goCodes = ids.split("\\s*[\\;\\,]\\s*");

						//String[] gocodes = objectTable.get(textId);
						if (goCodes != null)
							tcm.addCodes(goCodes, GeneContextModel.CONTEXTTYPE_GOCODES);

						keptAnnotations.add(annotation);
						continue;
					}

					if (type.equals("gene")) {
						TextAnnotation.Type ttype = TextAnnotation.Type.GENE;
						TextAnnotation textAnnotation = new TextAnnotation(textRange, name, ttype);
						textAnnotation.setSource("automatic");
						RecognizedEntity gene = new RecognizedEntity(text, textAnnotation);

						String[] idCandidates = ids.split("\\s*[\\;\\,]\\s*");
						pipe.run.context.addRecognizedEntity1(gene, idCandidates);	
						
						continue;
					}
				}

				// else? add this annotation as it was
				keptAnnotations.add(annotation);
			}
			
			for (RecognizedEntity r : pipe.run.context.getRecognizedEntities())
				for (String s : pipe.run.context.getIdCandidates(r))
					System.out.println(r.getName() + "\t" + s);

			// remove all old annotations
			annotatedText.clearAnnotations();
			// start adding the new annoations: first the ones we kept (species, GO terms, ..)
			for (String keptAnnotation: keptAnnotations)
				annotatedText.addAnnotation(keptAnnotation);

			// run the processing pipeline to normalize all genes
			List<String> normalizedResult = pipe.run(text);

			// convert result into new gene annotations
			for (String normalized: normalizedResult) {
				String[] cols = normalized.split("\t");

				String annotation = "<entity type=\"gene\" subtype=\"" + cols[6] + "\"" +
				" ids=\"" + cols[1] + "\"" +
				" startIndex=\"" + cols[4] + "\" endIndex=\"" + cols[5] + "\"" +
				" score=\"" + cols[3] + "\">" +
				cols[2] + "</entity>";

				annotatedText.addAnnotation(annotation);
			}

			// overwrite old, unannotated text
			annotatedTexts.set(a, annotatedText);
		}

		return annotatedTexts;
	}


	/**
	 * Tries to establish a connection to the given address. Format of <tt>serverAddress</tt>: <tt>IP:Port</tt> or <tt>DNS-Name:Port</tt>. 
	 * @param address
	 * @return true if a socket could be created successfully
	 */
	private boolean checkDictionaryServer (String serverAddress) {
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


