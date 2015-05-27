package gnat.server.dictionary;

import gnat.ISGNProperties;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stops a DictionaryServer that is running for a specific taxon (or a GO/MeSH term dictionary). Call with the taxon ID (999999999 for GO/MeSH)
 * as parameter.
 * <br><br>
 * The mapping of species to server addresses is stored in a file found via the {@link ISGNProperties}
 * entry <tt>taxon2port</tt>. This file has two tab-separated columns for NCBI taxon ID and server 
 * address with port ("128.10.11.12:56001"); a third column for comments is optional. If the server address 
 * is a single number, we assume this points to a local port (localhost, 127.0.0.1).
 * <br><br>
 * Stopping a dictionary server requires a pass phrase. A default phrase is set in {@link gnat.server.dictionary.DictionaryServer#stopPassphrase}, 
 * which is overwritten by any setting in isgn_properties.xml (entry <tt>stopDictPhasephrase</tt>), which can be overwritten by specifying a pass phrase 
 * on the command line. The pass phrase known to a running dictionary server has to be specified at startup of that server using any of these three ways.
 * It cannot be changed once the server is running. StopDictionaryServer will use the same order of precedence to determine which pass phrase to send to 
 * the server for authentication.
 * 
 * @author Joerg
 */
public class StopDictionaryServer {

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
	
	private static String stopPassphrase = DictionaryServer.stopPassphrase;
	static {
		if (ISGNProperties.get("stopDictPassphrase") != null) stopPassphrase = ISGNProperties.get("stopDictPassphrase");
	}
	
	
	/**
	 * 
	 */
	public StopDictionaryServer () {
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
	 * Stop the dictionary server that is listening on the given port, by sending the stop signal and pass phrase.
	 * @param server
	 * @param port
	 * @param passphrase
	 */
	public void stop (String server, int port, String passphrase) {
		Socket socket;
		//BufferedReader dictionaryReader;
		BufferedWriter dictionaryWriter;
		try {
			socket = new Socket(server, port);
			//dictionaryReader = new BufferedReader(new InputStreamReader(
			//		new BufferedInputStream(socket.getInputStream() ), "UTF-8") );
			dictionaryWriter = new BufferedWriter(new OutputStreamWriter(
					new BufferedOutputStream(socket.getOutputStream()), "UTF-8") );

			dictionaryWriter.write("<stop passphrase=\"" + passphrase + "\" />");
			dictionaryWriter.newLine();
			dictionaryWriter.flush();
//			String annotatedTexts = dictionaryReader.readLine();
//			
//			if (ConstantsNei.verbosityAtLeast(ConstantsNei.OUTPUT_LEVELS.DEBUG))
//				System.out.println("#INFO dictionary returned entities\n" + annotatedTexts + "\n----------");
//			
//			List<String> entities = this.extractEntityTags(annotatedTexts);
//			if (entities != null && entities.size() > 0) {
//				String entityString = entities.remove(0);
//				addRecognizedEntities(context, text, entityString);
//			}
//			
//			dictionaryReader.close();
			dictionaryWriter.close();
			socket.close();
		} catch (UnknownHostException e) {
			System.err.println("#StopDictionaryServer: " + e.getMessage());
		} catch (java.net.SocketException e) {
			System.err.println("#StopDictionaryServer: Remote dictionary server unreachable! [" + server + ":" + port + "]");
		} catch (IOException e) {
			System.err.println("#StopDictionaryServer: " + e.getMessage());
		} 
	}
	
	
	/**
	 * Stop the dictionary server for the given NCBI taxon, by sending the stop signal and pass phrase.
	 * @param passphrase
	 * @param taxon
	 */
	public void stop (String passphrase, int taxon) {
		String serverName = getServerForTaxon(taxon);
		if (serverName == null) serverName = "localhost";
		int serverPort    = getPortForTaxon(taxon);
		stop(serverName, serverPort, passphrase);
	}
	
	
	/**
	 * 
	 * @param args
	 */
	public static void main (String[] args) {
		if (args.length == 0 || args[0].toLowerCase().matches("\\-\\-h(elp)?") || !args[0].matches("\\d+")) {
			System.out.println("StopDictionaryServer -t=taxon {-pass=<passphrase>}");
			System.exit(0);
		}

		//int taxon = -1;
		//int port  = -1;
		//if (args[0].startsWith("-t=")) {
		//	taxon = Integer.parseInt(args[0].replaceFirst("\\-\\-?t(axon)?=", ""));
		//} else {
		//	System.err.println("Specify a taxon ID corresponding to the dictionary server to stop.");
		//}

		int taxon = Integer.parseInt(args[0]);
		if (args.length > 2 && args[1].matches("\\-\\-?p(assphrase)?=(.+)")) {
			stopPassphrase = args[1].replaceFirst("\\-\\-?p(assphrase)?=", "");
		}
		
		StopDictionaryServer stop = new StopDictionaryServer();
		stop.stop(stopPassphrase, taxon);
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
		//if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
		//	System.out.println("#Trying to contact remote dictionary servers...");
		//boolean error = false;
		for (int tax: taxonToServerPortMap.keySet()) {
			//String server = getServerForTaxon(tax);
			//int port      = getPortForTaxon(tax);
			//
			//if (!tryContacting(server, port)) {
			//	if (!error) {
			//		error = true;
			//		if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
			//			System.err.print("# Error contacting dictionary server " + server + ":" + port);
			//	} else
			//		if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
			//			System.err.print(", " + server + ":" + port);
			//	success = false;
			//} else {
				availableServersForSpecies.add(tax);
			//}
			
		}
		//if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
		//	if (error)
		//		System.err.println();
		
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
	
//	
//	/**
//	 * Tries to contact the server (see <tt>serverName</tt>) at the given port.
//	 * @return
//	 */
//	boolean tryContacting (String serverName, int port) {
//		try {
//			Socket socket = new Socket(serverName, port);
//			socket.close();	
//		} catch (java.net.SocketException e) {
//			return false;
//		} catch (IOException e) {
//			//System.err.println(e.getMessage());
//			return false;
//		}
//		
//		return true;
//	}
	
	
}
