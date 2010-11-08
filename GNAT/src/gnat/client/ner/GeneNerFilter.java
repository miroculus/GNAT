package gnat.client.ner;

import gnat.ISGNProperties;
import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.Text;
import gnat.representation.TextRepository;
import gnat.server.dictionary.DictionaryServer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * TODO not fully implemented yet !!!<br>
 * Uses config/geneNerDictionaryServers.txt to determine NER servers at runtime.
 * <br><br><br>
 * 
 * A {@link Filter} used for NER, which can be configured at run time via property files.
 * Runs a variety of gene NER services and dictionaries as specified by a mapping provided via properties.
 * <br><br>
 * The name of the properties file is specified in the 'geneNerServiceMapping' entry of {@link ISGNProperties}.<br>
 * <br>
 * The properties file has a mapping of taxon IDs to server addresses and ports; alternatively to an address, 
 * the name of a gnat.filter.Filter can be given, including the package.
 * <br><br>
 * - If a species is mapped by multiple entries, all corresponding servers will be called, one after the other.<br>
 * - An entry 'ALL' for the taxon ID means that this Server/Filter will be called for all species; if a particular
 * taxon is also given, both servers will be called for that species.<br>
 * - An entry 'OTHER' for the taxon ID means that the given server will be called if no other mapping for a particular species exists.
 * <br><br>
 * Thus, if you are proving your own NER for a certain species, but want to use GNAT for all others, you should have an entry
 * for 'OTHER', mapped to 'gnat.filter.ner.GnatServiceNer'; plus an entry for your own dictionary, mapping its species to a
 * server address:port or your own Filter class.
 * <br><br>
 * 
 * Example:<pre>
 * #Tab-separated entries for:
 * #Taxon_ID	Server_address_or_filter_class	Optional_comment
 * OTHER	gnat.filter.ner.GnatServiceNer  # handle all NER requests *except* for the ones below (if any) with GNAT
 * 10090	localhost:56002	# handle NER requests for mouse genes via a DicionaryServer that listens on localhost, port 56002
 * 10116	127.1.2.3:56003	# handle NER requests for rat genes via a DictionaryServer that resides on the server 127.1.2.3, port 56003</pre>
 * Will handle murine gene NER through the {@link DictionaryServer} on "localhost:56002", rat gene NER through "127.1.2.3:56003",
 * and the gene NER requests for all other species through the {@link Filter} "gnat.filter.ner.GnatServiceNer".
 * <br><br>
 * 
 * Another example:<pre>
 * #Taxon_ID	Server_address_or_filter_class	Optional_comment
 * ALL	gnat.filter.ner.GnatServiceNer  # handle all NER requests with GNAT
 * 9606	localhost:56002	# handle NER requests for human genes *also* via a DicionaryServer that listens on localhost, port 56002
 * 7227	127.1.2.3:56003	# handle NER requests for fruit fly genes *also* via a DictionaryServer that resides on the server 127.1.2.3, port 56003</pre>
 * Will handle all gene NER requests using the {@link Filter} "gnat.filter.ner.GnatServiceNer"; requests for genes from the species
 * 7227 and 9606 will also be send to the respective {@link DictionaryServer}s, even if gnat.filter.ner.GnatServiceNer has handled them
 * already.
 * <br>
 * @author Joerg
 *
 */
public class GeneNerFilter implements Filter {

	/** */
	Map<Integer, LinkedList<Object>> taxonToServiceMap;


	/**
	 * Constructs a new service, reads configuration files.
	 */
	public GeneNerFilter () {
		taxonToServiceMap = new HashMap<Integer, LinkedList<Object>>();
		String geneNerMappingFile = ISGNProperties.get("geneNerServiceMapping");
		try {
			BufferedReader br = new BufferedReader(new FileReader(geneNerMappingFile));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#")) continue;
				String[] cols = line.split("\t");
				if (cols.length < 2) continue;
				
				String taxon   = cols[0];
				String address = cols[1];
				
				// is the address an IP:port or does it point to a Filter?
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
		
		// for each text
		// determine the species in that text
		// for each species
		// call one or more NER services
		
		for (Text text: textRepository.getTexts()) {
			
		}
		
	}

	
}
