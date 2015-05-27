package gnat.filter.ner;

import gnat.ConstantsNei;
import gnat.ISGNProperties;
import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.RecognizedEntity;
import gnat.representation.Text;
import gnat.representation.TextAnnotation;
import gnat.representation.TextRange;
import gnat.representation.TextRepository;
import gnat.server.dictionary.DictionaryServer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter that contacts local or remote {@link DictionaryServer}s to perform NER.
 * <br><br>
 * The mapping of species to server addresses is stored in a file found via the {@link ISGNProperties}
 * entry <tt>taxon2port</tt>. This file has two tab-separated columns for NCBI taxon ID and server 
 * address with port ("128.10.11.12:56001"); a third column for comments is optional. If the server address 
 * is a single number, we assume this points to a local port (localhost, 127.0.0.1).
 * 
 * @author Joerg
 */
public class RunAllGeneDictionaries implements Filter {

	/** Mapping of species taxon ID to a server address (server:port"). Loaded from the file given by the
	 *  ISGNProperties entry <tt>taxon2port</tt>. */
	private Map<Integer, String[]> taxonToServerPortMap;
	
	/** Stores for which species the specified server was available at instantiation time. */
	Set<Integer> availableServersForSpecies;
	
	/**
	 * Do not run dictionaries for the taxon IDs given in <tt>excludeTaxons</tt>, even if they are
	 * specified via the properties/configuration files.<br>
	 * <b>Note</b>: <tt>excludeTaxons</tt> <em>overwrites</em> {@link #limitToTaxons}: if a taxon is included in both sets,
	 * it will be excluded.
	 */
	Set<Integer> excludeTaxons;
	
	/**
	 * Run dictionaries only for the species given in <tt>excludeTaxons</tt>, even if others are also
	 * specified via the properties/configuration files.<br>
	 * <b>Note</b>: {@link #excludeTaxons} <em>overwrites</em> <tt>limitToTaxons</tt>: if a taxon is included in both sets,
	 * it will be excluded.
	 */
	Set<Integer> limitToTaxons;
	
	
	/**
	 * 
	 */
	public RunAllGeneDictionaries () {
		taxonToServerPortMap = new HashMap<Integer, String[]>();
		availableServersForSpecies = new HashSet<Integer>();
		excludeTaxons = new HashSet<Integer>();
		limitToTaxons = new HashSet<Integer>();
		
		String dictionaryMappingFile = ISGNProperties.get("taxon2port");
		if (dictionaryMappingFile == null || dictionaryMappingFile.length() == 0) {
			System.err.println("#RunDictionaries: the entry 'taxon2port' is not specified in " + ISGNProperties.getPropertyFilename()
					+ "! Skipping.");
			return;
		}
		
		String defaultServer = ISGNProperties.get("dictionaryServer");
		if (defaultServer == null || defaultServer.length() == 0) {
			System.err.println("#RunDictionaries: no default server for dictionaries was found," +
							   " check the entry 'dictionaryServer' in " + ISGNProperties.getPropertyFilename() +
							   "; continuing.");
		}
		
		getDictionaryServers(dictionaryMappingFile, defaultServer);
	}
	
	
	/**
	 * Filters the <tt>context</tt> by invoking each relevant dictionary server to the texts in <tt>textRepository</tt>.<br>
	 * Relies on {@link Text#taxonIDs} to determine the species for each {@link Text}. Considers {@link #excludeTaxons}
	 * and {@link #limitToTaxons} to restrict the species for each text further.
	 * @param context
	 * @param textRepository
	 * @param geneRepository
	 */
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {
		Set<Integer> reported_missing_dictionaries = new HashSet<Integer>();
		StringBuffer buffer = new StringBuffer();
		for (Text text : textRepository.getTexts()) {
			Set<Integer> taxaForThisText = text.taxonIDs;
			if (taxaForThisText == null || taxaForThisText.size() == 0) {
				System.err.println("#RunDictionaries: No species assigned to text " + text.getID() + ", using default species " + ConstantsNei.DEFAULT_SPECIES);
				taxaForThisText = ConstantsNei.DEFAULT_SPECIES;
			} else
				;//System.out.println("#RunDictionaries: for text " + text.getPMID() + ", checking species " + taxaForThisText);
			
			String plain = text.getPlainText();
			
			Pattern p = Pattern.compile("[\\r\\n]", Pattern.UNIX_LINES | Pattern.MULTILINE);
			Matcher m = p.matcher(plain);
			plain = m.replaceAll(" ");
			
			buffer.append("<text>");
			buffer.append(plain);
			buffer.append("</text>");
			if (ConstantsNei.verbosityAtLeast(ConstantsNei.OUTPUT_LEVELS.DEBUG))
				System.out.println("#INFO sending text to dictionary:\n" + buffer.toString() + "\n----------");
			
			// go through all species recognized in this text
			for (int taxon: taxaForThisText) {
				// if this taxon is set to be excluded, skip
				if (isExcluded(taxon)) continue;
				// if limits are set
				if (hasLimited())
					// but this taxon is not specified in the limits, then skip
					if (!isLimited(taxon)) continue;
				
				// if the server for this taxon was not available at startup time, don't bother
				// TODO could add test, which runs once in a while, to check whether a dictionary became available?
				if (!availableServersForSpecies.contains(taxon)) {
					if (!reported_missing_dictionaries.contains(taxon)) {
						if (ConstantsNei.verbosityAtLeast(ConstantsNei.OUTPUT_LEVELS.WARNINGS))
							ConstantsNei.ERR.println("#RunDictionaries: dictionary server for species " + taxon + " not available");
						reported_missing_dictionaries.add(taxon);
					}
					continue;
				}
				
				String serverName = getServerForTaxon(taxon);
				int serverPort    = getPortForTaxon(taxon);
					
				// open a socket, send the text, receive annotations
				Socket socket;
				BufferedReader dictionaryReader;
				BufferedWriter dictionaryWriter;
				try {
					socket = new Socket(serverName, serverPort);
					dictionaryReader = new BufferedReader(new InputStreamReader(
							new BufferedInputStream(socket.getInputStream() ), "UTF-8") );
					dictionaryWriter = new BufferedWriter(new OutputStreamWriter(
							new BufferedOutputStream(socket.getOutputStream()), "UTF-8") );

					dictionaryWriter.write(buffer.toString());
					dictionaryWriter.newLine();
					dictionaryWriter.flush();
					String annotatedTexts = dictionaryReader.readLine();
					
					if (ConstantsNei.verbosityAtLeast(ConstantsNei.OUTPUT_LEVELS.DEBUG))
						System.out.println("#INFO dictionary returned entities\n" + annotatedTexts + "\n----------");
					
					List<String> entities = this.extractEntityTags(annotatedTexts);
					if (entities != null && entities.size() > 0) {
						String entityString = entities.remove(0);
						addRecognizedEntities(context, text, entityString);
					}
					
					dictionaryReader.close();
					dictionaryWriter.close();
					socket.close();
				} catch (UnknownHostException e) {
					System.err.println("#RunDictionaries: " + e.getMessage());
				} catch (java.net.SocketException e) {
					System.err.println("#RunDictionaries: Remote dictionary server unreachable!" +
						" [" + serverName + ":" + serverPort + "]");
				} catch (IOException e) {
					System.err.println("#RunDictionaries: " + e.getMessage());
				} 
				
			}
		
			// clear buffer for next text
			buffer.setLength(0);
		}
		
	}

	
	/**
	 * Reads the configuration for remote dictionary server from the config file to 
	 * load the <tt>taxonToServerPortMap</tt>; also tries to contact every dictionary server.
	 * 
	 * @param dictionaryMappingFile
	 * @return true if every dictionary was available (and the config file was readable)
	 */
	boolean getDictionaryServers (String dictionaryMappingFile, String defaultServer) {
		boolean success = true;
		
		// open the mapping file and get the list of taxon-to-server mappings
		try {
			BufferedReader br = new BufferedReader(new FileReader(dictionaryMappingFile));
			String line;
			String[] cols;
			String server;
			String defaul = defaultServer;
			
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#") || line.trim().length() == 0)
					continue;
				
				cols = line.split("(\t|\\s\\s+)");
				
				// the default server can be overwritten in the mapping file itself,
				// dicarding the entry from ISGNProperties; UNDOCUMENTED FEATURE
				// Nice: will only affect entries *following* this one, so multiple
				// 'defaultserver' entries can be used
				if (cols[0].toLowerCase().equals("defaultserver")) {
					defaul = cols[1];
					continue;
				}
				
				if (cols[0].matches("\\d+")) {
					
				} else {
					System.err.println("#Error reading taxon to server mapping from ''" + line + "'' -- invalid taxon ID; skipping");
					continue;
				}
				
				// addr will contain the server name in the first field, the port in the second field
				String[] addr = new String[2];
					
				server = cols[1];
				if (server.matches(".+\\:\\d+")) {
					addr[0] = server.split("\\:")[0];
					addr[1] = server.split("\\:")[1];
					
				} else if (server.matches("\\d+")) {
					// if just the port is given, assume default server
					addr[0] = defaul;
					addr[1] = server;
					
				} else {
					System.err.println("#Error reading taxon to server mapping from ''" + line + "'' -- invalid address; skipping");
					continue;
				}
				
				addTaxonMapping(Integer.parseInt(cols[0]), addr[0], Integer.parseInt(addr[1]));
			}
			br.close();
			
		} catch (IOException ioe) {
			System.err.println("#Error reading the dictionary config file '" + dictionaryMappingFile + "'");
			System.err.println(ioe.getMessage());
			return false;
		}


		// now check all species/servers found in the map; add them to 'availableServersForSpecies' if the
		// dictionary could be contacted w/o any errors
		if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
			System.out.println("#Trying to contact remote dictionary servers...");
		boolean error = false;
		for (int tax: taxonToServerPortMap.keySet()) {
			String server = getServerForTaxon(tax);
			int port      = getPortForTaxon(tax);
			
			if (!tryContacting(server, port)) {
				if (!error) {
					error = true;
					if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
						System.err.print("# Error contacting dictionary server " + server + ":" + port);
				} else
					if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
						System.err.print(", " + server + ":" + port);
				success = false;
			} else {
				availableServersForSpecies.add(tax);
			}
			
		}
		if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
			if (error)
				System.err.println();
		
		return success;
	}
	
	
	/**
	 * Adds a mapping from the taxon ID to a server:port address.
	 * @param taxon
	 * @param serverName
	 * @param serverPort
	 */
	public void addTaxonMapping (int taxon, String serverName, int serverPort) {
		taxonToServerPortMap.put(taxon, new String[]{serverName, ""+serverPort});
	}
	
	
	/**
	 * Returns the server address (URL, IP), excluding the port, of the 
	 * dictionary server for the given taxon; returns null if no such mapping
	 * exists.
	 * @param taxon
	 * @return
	 */
	public String getServerForTaxon (int taxon) {
		if (taxonToServerPortMap.containsKey(taxon))
			return taxonToServerPortMap.get(taxon)[0];
		else
			return null;
	}
	
	/**
	 * Return the server port for a given taxon, or -1 if no such mapping exists.
	 * @param taxon
	 * @return
	 */
	public int getPortForTaxon (int taxon) {
		if (taxonToServerPortMap.containsKey(taxon)) {
			if (taxonToServerPortMap.get(taxon)[1].matches("\\d+"))
				return Integer.parseInt(taxonToServerPortMap.get(taxon)[1]);
			else
				return -1;
		} else
			return -1;
	}
	
	
	/**
	 * Tries to contact the server (see <tt>serverName</tt>) at the given port.
	 * @return
	 */
	boolean tryContacting (String serverName, int port) {
		try {
			Socket socket = new Socket(serverName, port);
			socket.close();	
		} catch (java.net.SocketException e) {
			return false;
		} catch (IOException e) {
			//System.err.println(e.getMessage());
			return false;
		}
		
		return true;
	}
	
	
	/**
	 * Set the filter to not invoke dictionary servers for the given taxa.<br>
	 * <b>Note</b>: overwrites the previous setting. Use {@link #addExcludeTaxon(int)} to add a taxon.
	 * @param taxa
	 */
	public void setExcludeTaxons (Set<Integer> taxa) {
		if (excludeTaxons == null) 
			excludeTaxons = new HashSet<Integer>();
		else
			excludeTaxons.clear();
		excludeTaxons.addAll(taxa);
	}
	
	
	/**
	 * Set the filter to not invoke dictionary servers for the given taxon.<br>
	 * <b>Note</b>: adds this taxon to the existing list in <tt>excludeTaxons</tt>; set the 
	 * complete list with {@link #setExcludeTaxons(Set)}.
	 * @param taxon
	 */
	public void addExcludeTaxon (int taxon) {
		if (excludeTaxons == null) 
			excludeTaxons = new HashSet<Integer>();
		excludeTaxons.add(taxon);
	}
	
	
	/**
	 * Checks whether the given taxon ID should be excluded from processing, according to
	 * {@link #excludeTaxons}.
	 * @param taxon
	 * @return
	 */
	public boolean isExcluded (int taxon) {
		if (excludeTaxons == null) return false;
		return excludeTaxons.contains(taxon);
	}
	
	
	/**
	 * Checks whether the given taxon ID is valid and its corresponding dictionary should be used,
	 * accordint to {@link #limitToTaxons}.
	 * @param taxon
	 * @return
	 */
	public boolean isLimited (int taxon) {
		if (limitToTaxons == null) return false;
		return limitToTaxons.contains(taxon);
	}
	
	
	/**
	 * Checks whether dictionary calls should be limited to certain species.<br>
	 * Returns true if {@link #limitToTaxons} contains at least one taxon ID.
	 * @return
	 */
	public boolean hasLimited () {
		return limitToTaxons != null && limitToTaxons.size() > 0;
	}
	
	
	/**
	 * Set the filter to invoke dictionary servers only for the given taxa.<br>
	 * <b>Note</b>: overwrites the previous setting. Use {@link #addLimitToTaxon(int)} to add a taxon.<br>
	 * <b>Note</b>: the set {@link #excludeTaxons} overwrites the set {@link #limitToTaxons} - if a taxon
	 * is contained in both sets, the dictionary for this taxon will <em>not be invoked</em>.
	 * @param taxa
	 */
	public void setLimitToTaxons (Set<Integer> taxa) {
		if (limitToTaxons == null) 
			limitToTaxons = new HashSet<Integer>();
		else
			limitToTaxons.clear();
		limitToTaxons.addAll(taxa);
	}
	
	
	/**
	 * Set the filter to invoke dictionary servers only for the given taxa.<br>
	 * <b>Note</b>: overwrites the previous setting. Use {@link #addLimitToTaxon(int)} to add a taxon.<br>
	 * <b>Note</b>: the set {@link #excludeTaxons} overwrites the set {@link #limitToTaxons} - if a taxon
	 * is contained in both sets, the dictionary for this taxon will <em>not be invoked</em>.
	 * @param taxa
	 */
	public void setLimitToTaxons (int... taxa) {
		if (limitToTaxons == null) 
			limitToTaxons = new HashSet<Integer>();
		else
			limitToTaxons.clear();
		for (int t: taxa)
			limitToTaxons.add(t);
	}
	
	
	/**
	 * Add a taxon to invoke dictionary servers only for specified taxons.<br>
	 * <b>Note</b>: adds this taxon to the existing set {@link #limitToTaxons}; set the 
	 * complete list with {@link #setLimitToTaxons(Set)}.<br>
	 * <b>Note</b>: the set {@link #excludeTaxons} overwrites the set {@link #limitToTaxons} - if a taxon
	 * is contained in both sets, the dictionary for this taxon will <em>not be invoked</em>.
	 * @param taxon
	 */
	public void addLimitToTaxon (int taxon) {
		if (limitToTaxons == null) 
			limitToTaxons = new HashSet<Integer>();
		limitToTaxons.add(taxon);
	}
	
	
	/**
	 * Returns a list of entity mark-ups found in annotated texts.
	 * 
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
	
	
	/**
	 * Adds entities found as markup in the given annotated text to the context.
	 * */
	private void addRecognizedEntities (Context context, Text originalText, String annotatedText) {
		//Set<RecognizedGeneName> geneNamesInText = new HashSet<RecognizedGeneName>();

		int geneTagBeginIndex = annotatedText.indexOf("<entity");
		while (geneTagBeginIndex != -1) {
			// get the annotation of the next single entity
			int geneTagEndIndex = annotatedText.indexOf("</entity>", geneTagBeginIndex);
			String annotatedGeneName = annotatedText.substring(geneTagBeginIndex, geneTagEndIndex);
			
			String geneName = annotatedGeneName.substring(annotatedGeneName.indexOf(">") + 1);
			int idOpenQuot = annotatedGeneName.indexOf("\"");
			int idCloseQuot = annotatedGeneName.indexOf("\"", idOpenQuot + 1);
			int startIndexOpenQuot = annotatedGeneName.indexOf("\"", idCloseQuot + 1);
			int startIndexCloseQuot = annotatedGeneName.indexOf("\"", startIndexOpenQuot + 1);
			int endIndexOpenQuot = annotatedGeneName.indexOf("\"", startIndexCloseQuot + 1);
			int endIndexCloseQuot = annotatedGeneName.indexOf("\"", endIndexOpenQuot + 1);

			int startIndex = Integer.parseInt(annotatedGeneName.substring(startIndexOpenQuot+1, startIndexCloseQuot));
			int endIndex = Integer.parseInt(annotatedGeneName.substring(endIndexOpenQuot+1, endIndexCloseQuot));

			//int typeBeginIndex = annotatedGeneName.indexOf(" type=\"", geneTagBeginIndex);
			String type = annotatedGeneName.replaceFirst("^.*(^|\\s)type=\"([^\"]*)\".*$", "$1");
			TextAnnotation.Type ttype = TextAnnotation.Type.getValue(type);
			if (ttype == null || ttype.toString() == null
					|| ttype.toString().length() == 0 || ttype == TextAnnotation.Type.UNKNOWN) {
				ttype = TextAnnotation.Type.GENE;
			}
			
			String idString = annotatedGeneName.substring(idOpenQuot + 1, idCloseQuot);

			TextAnnotation textAnnotation =new TextAnnotation(new TextRange(startIndex, endIndex), geneName, ttype);
			textAnnotation.setSource("automatic");
			RecognizedEntity recognizedGeneName = new RecognizedEntity(originalText, textAnnotation);
			context.addRecognizedEntity(recognizedGeneName, idString.split(";"));

			geneTagBeginIndex = annotatedText.indexOf("<entity", geneTagEndIndex);
		}
	}
}
