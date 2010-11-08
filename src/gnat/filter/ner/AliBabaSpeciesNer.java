package gnat.filter.ner;

import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.Text;
import gnat.representation.TextRepository;
import gnat.retrieval.ResourceHelper;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * <b>Deprecated: this should be replaced with another species-service running on a dedicated machine.</b>
 * <br><br>
 * 
 * Recognizes species in a text by invoking a servlet provided by AliBaba 
 * (<a href="http://alibaba.informatik.hu-berlin.de/">alibaba.informatik.hu-berlin.de/</a>).
 * <br><br>
 * Queries AliBaba for species mentioned in a text or abstract for a given
 * PubMed ID. Returns a set of NCBI Taxonomy IDs.<br>
 * <br><br>
 * AliBaba runs a tagger with a lexicon derived from NCBI Taxonomy (ca. 200,000 entries) to handle the
 * initial recognition species' names in the given text. Additionally, searches for
 * cell lines mentioned in the text and resolves them to the species they originate from.
 * For instance, a mention of "HeLa cells" get mapped to the species 9606, human, "COS-7 
 * cells" originate from 10090, mouse.
 * 
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 *
 */
@Deprecated
public class AliBabaSpeciesNer implements Filter {

	static final String tokenEndMark = "([\\,\\s\\.\\)\\]\\;\\?\\!]|$)";
	
	static final String URL_ASK_ALIBABA_PMID = "http://alibaba.informatik.hu-berlin.de/servlet/AskAliBaba?type=pmid&annotation=S,P&query=";
	static final String URL_ASK_ALIBABA_TEXT = "http://alibaba.informatik.hu-berlin.de/servlet/AskAliBaba?type=text&annotation=S,P&query=";
	static final String URL_EUTILS_FETCHMEDLINERECORD_BYPMID = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&report=medline&mode=file&id=";

	ResourceHelper rh = new ResourceHelper();


	@Override
	public void filter(Context context, TextRepository textRepository, GeneRepository geneRepository) {
		for (Text text: textRepository.getTexts()) {
			
			// run the plain text through AliBaba
			Map<Integer, Set<String>> id2names = textToTaxIDs(text.getPlainText());
			
			//text.addTaxonToNameMap(id2names);
			//text.taxonIDs = id2names.keySet();
			
			// if the text has a PubMed identifier, check the MeSH terms for species as well
			if (text.hasPMID()) {
				int pmid = text.getPMID();
				Map<Integer, Set<String>> id2namesFromMeSH = pmidToTaxIDsFromMeSH(pmid);
				
				for (int taxon: id2namesFromMeSH.keySet()) {
					if (id2names.containsKey(taxon)) {
						Set<String> oldNames = id2names.get(taxon);
						oldNames.addAll(id2namesFromMeSH.get(taxon));
						id2names.put(taxon, oldNames);
					} else {
						id2names.put(taxon, id2namesFromMeSH.get(taxon));
					}
				}
			}
			
			// START OLD CODE: now done in SpeciesPostProcessingFilter:
			//id2names = removeSpeciesWithAmbiguousNames(id2names);
			//
			//TreeSet<Integer> result = new TreeSet<Integer>();
			//result.addAll(id2names.keySet());
			//
			//result.addAll(getIndirectReferences(text));
			//result = normalizeTaxonIDs(result);
			// END old code
			
			//text.taxonIDs.addAll(getIndirectReferences(text.getPlainText()));
			Map<Integer, Set<String>> indirect = getIndirectReferencesAndNames(text.getPlainText());
			for (int tax: indirect.keySet()) {
				Set<String> newnames = indirect.get(tax);
				
				if (id2names.containsKey(tax)) {
					Set<String> names = id2names.get(tax);
					for (String newname: newnames) {
						names.add(newname);
					}
				} else {
					id2names.put(tax, newnames);
				}
			}
			
			text.addTaxonToNameMap(id2names);			
		}
	}
	
	
	/**
	 * Takes a PubMed ID and returns the set of species that are discussed in the corresponding abstract,
	 * by invoking AliBaba's species NER tagger on the PubMed ID's abstract, which AliBaba will download
	 * or get from its own cache.
 	 * <br><br>
	 * Alternatively, use {@link #pmidToTaxIDsFromMeSH(int)} to search the list of MeSH terms for species;
	 * or use {@link #textToTaxIDsAndNames(String)} if no PubMed is available.
	 * 
	 * @see #pmidToTaxIDsFromMeSH(int)
	 * @see #textToTaxIDsAndNames(String)
	 * @param pmid
	 * @return Set of NCBI Taxonomy identifiers
	 */
	public Map<Integer, Set<String>> pmidToTaxIDs (int pmid) {
		// get the content from AliBaba, i.e., the abstract with XML markup for species
		String content = "";
		//try {
		content = rh.getURLContent(URL_ASK_ALIBABA_PMID + pmid, 3000);
		//} catch (Exception ioe) {
		//	System.err.println("#Got HTTP:503 error (PubMed eUtils temporarily unavaiable). Setting default species.");
		//	//content = "The default species are human, mouse, rat, S. cerevisiae, and fruitfly.";
		//}

		//System.out.println("at2m");
		// extract all species annotations from the text
		Map<Integer, Set<String>> id2names = annotatedTextToMap(content);
		
		return id2names;

//		//System.out.println("remSpec");
//		// remove some typically FP species names (cancers, monitors, ..)
//		id2names = removeSpeciesWithAmbiguousNames(id2names);
//		
//		// store all Taxon IDs
//		TreeSet<Integer> result = new TreeSet<Integer>();
//		result.addAll(id2names.keySet());
//		
//		//System.out.println("getIndirect");
//		// also look for indirect mentions of species (cell lines etc.) in the abstract
//		result.addAll(getIndirectReferences(content));
//		
//		//System.out.println("norm");
//		// normalize the taxon IDs (murinae -> M.mus etc.)
//		result = normalizeTaxonIDs(result);
//		
//		//System.out.println("pmid2tax");
//		// add species found in MeSH terms assoc. with this PMID
//		if (result.size() == 0)
//			result.addAll(pmidToTaxIDsFromMeSH(pmid));
//		
//		//System.out.println("norm");
//		// .. normalize again
//		// [forgot why we have to call it twice, but it makes a difference!]
//		result = normalizeTaxonIDs(result);
//
//		//System.out.println("done.");
//		return result;
	}
	
	
	/**
	 * Searches the MeSH annotations of the given PubMed ID for occurrences of species names,
	 * instead of annotating the PubMed ID's abstract. This method fetches the MeSH annotations
	 * (URL is stored in {@link #URL_EUTILS_FETCHMEDLINERECORD_BYPMID} and sends them to AliBaba
	 * for annotation of species.
	 * <br><br>
	 * Alternatively, use {@link #pmidToTaxIDs(int)} to annotate the abstract itself;
	 * use {@link #textToTaxIDsAndNames(String)} if no PubMed is available.
	 * 
	 * @see pmidToTaxIDs(int)
	 * @see textToTaxIDsAndNames(String)
	 * @param pmid
	 * @return
	 */
	public Map<Integer, Set<String>> pmidToTaxIDsFromMeSH (int pmid) {
		Map<Integer, Set<String>> id2names = new HashMap<Integer, Set<String>>();
		//ResourceHelper rh = new ResourceHelper(false);

		// get the Medline entry for the specified PubMed ID
		String content = rh.getURLContent(URL_EUTILS_FETCHMEDLINERECORD_BYPMID + pmid, 3000);
		String[] lines = content.split("[\r\n]+");
		String mesh = "";
		// get all lines starting with MH: entries with MeSH terms
		for (String line: lines) {
			if (line.startsWith("MH  - ")) {
				line = line.replaceFirst("^MH\\s\\s\\-\\s(.*)$", "$1");
				line = line.replaceAll("\\s*[\\/\\&]\\s*", " and ");
				line = line.replaceAll("\\*", "");
				// put all these entries together into one 'text'
				mesh = mesh + line + ". ";
			}
		}

		// send the text containing all MeSH terms to AliBaba to look for species names
		if (mesh.trim().length() > 0) {
			String url = URL_ASK_ALIBABA_TEXT + mesh;
			url = url.replaceAll(" ", "%20");
			url = url.replaceAll("\\+", "%20");
			url = url.replaceAll("\\n", "%20");
			url = url.replaceAll("\\r", "%20");
			content = rh.getURLContent(url, 3000);
			
			id2names = annotatedTextToMap(content);
		}
		
		return id2names;

//		// put together the taxon IDs returned from AliBaba ...
//		TreeSet<Integer> result = new TreeSet<Integer>();
//		result.addAll(id2names.keySet());
//		// ... and taxon IDs obtained from searching for specific cell lines and related terms
//		result.addAll(getIndirectReferences(mesh));
//			
//		return result;
	}
	
	
	/**
	 * Takes a text and returns a set of species that are discussed therein.<br>
	 * Alternatively, if the PubMed ID of a text is known and only the abstract is needed,
	 * run {@link #pmidToTaxIDs(int)} to get taxon IDs and potentially use cached results.<br>
	 * Use {@link #textToTaxIDsAndNames(String)} is you need species IDs and the mentions used
	 * to refer to them in the text.
	 * 
	 * @see #pmidToTaxIDs(int)
	 * @see #pmidToTaxIDsFromMeSH(int)
	 * @see #textToTaxIDsAndNames(String)
	 * 
	 * @param text
	 * @return a map of NCBI Taxonomy identifiers to names as they occurred in a text
	 */
	public Map<Integer, Set<String>> textToTaxIDs (String text) {
		String content = "";
		
		try {
			String encodedText = URLEncoder.encode(text, "UTF-8");
			
			String url = URL_ASK_ALIBABA_TEXT + encodedText;
//			url = url.replaceAll(" ", "%20");
//			url = url.replaceAll("\\+", "%20");
//			url = url.replaceAll("\\n", "%20");
//			url = url.replaceAll("\\r", "%20");
			content = rh.getURLContent(url, 10000);

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Map<Integer, Set<String>> id2names = annotatedTextToMap(content);
		
		return id2names;
	}
	
	
//	/**
//	 * Takes a text and returns a set of species that are discussed therein.<br>
//	 * Alternatively, if the PubMed ID of a text is known and only the abstract is needed,
//	 * run {@link #pmidToTaxIDs(int)} to get taxon IDs and potentially use cached results.
//	 * 
//	 * @see #pmidToTaxIDs(int)
//	 * @see #pmidToTaxIDsFromMeSH(int)
//	 * @param text
//	 * @return Set or NCBI Taxonomy identifiers
//	 */
//	public Map<Integer, TreeSet<String>> textToTaxIDsAndNames (String text) {
//		//ResourceHelper rh = new ResourceHelper();
//		//rh.setProxySet("false");
//
//		String url = URL_ASK_ALIBABA_TEXT + text;
//		url = url.replaceAll(" ", "%20");
//		url = url.replaceAll("\\+", "%20");
//		url = url.replaceAll("\\n", "%20");
//		url = url.replaceAll("\\r", "%20");
//		String content = rh.getURLContent(url, 3000);
//
//		Map<Integer, TreeSet<String>> id2names = annotatedTextToMap(content);
//		id2names = removeSpeciesWithAmbiguousNames(id2names);
//
//		// TODO: add indirect references
////		
////		TreeSet<Integer> result = new TreeSet<Integer>();
////		result.addAll(id2names.keySet());
//
//		Map<Integer, TreeSet<String>> indirect = getIndirectReferencesAndNames(text);
//		for (int tax: indirect.keySet()) {
//			TreeSet<String> newnames = indirect.get(tax);
//			
//			if (id2names.containsKey(tax)) {
//				TreeSet<String> names = id2names.get(tax);
//				for (String newname: newnames) {
//					names.add(newname);
//				}
//			} else {
//				id2names.put(tax, newnames);
//			}
//		}
//
//		// 
//		id2names = normalizeTaxonIDs(id2names);
//
//		return id2names;
//	}
	
	
	/**
	 * Parses a text annotated by AliBaba for species and returns all NCBI Taxonomy IDs as a
	 * map to a set of names that were found in the text and refer to that species.
	 * 
	 * @param annotatedText
	 * @return
	 */
	private Map<Integer, Set<String>> annotatedTextToMap (String annotatedText) {
		Map<Integer, Set<String>> result = new HashMap<Integer, Set<String>>();
		String[] lines = annotatedText.split("[\r\n]+");
		Set<String> temp;
		String oldLine = "";
		for (String line: lines) {
			
			// to prevent endless loops resulting from errors in the text or reg.ex,
			// break if nothing has changes as compared to the previous iteration
			while (line.indexOf("<z:species") >= 0 && !line.equals(oldLine)) {
				oldLine = line;
				String spec = line.replaceFirst("^.*?(<z:species[^>]+>[^<]+</z:species>).*$", "$1");
				line = line.replaceFirst("^.*?(<z:species[^>]+>[^<]+</z:species>)(.*)$", "$2");
				String ids = spec.replaceFirst("^.*<z:species[^>]+ids=\"([^\"]*)\".*$", "$1");
				String name = spec.replaceFirst("^.*<z:species[^>]+ids=\"([^\"]*)\"[^>]*>([^<]+)</z:species>.*$", "$2");
				if (name.matches("<([A-Za-z0-9\\_\\:]+])(?:\\s[^>]*?)?>(.*)</\1>")) {
					name = name.replaceFirst("<([A-Za-z0-9\\_\\:]+])(?:\\s[^>]*?)?>(.*)</\1>", "$2");
				}
				for (String id: ids.split(";")) {
					if (result.containsKey(Integer.parseInt(id)))
						temp = result.get(Integer.parseInt(id));
					else
						temp = new TreeSet<String>();
					temp.add(name);
					result.put(Integer.parseInt(id), temp);
				}
			}
		}
		return result;
	}

	
//	/**
//	 * Parses a text annotated by AliBaba for species and returns all NCBI Taxonomy
//	 * IDs as a set.
//	 * 
//	 * @param annotatedText
//	 * @return
//	 */
//	@SuppressWarnings({"unused"})
//	private TreeSet<Integer> annotatedTextToTaxIDs (String annotatedText) {
//		TreeSet<Integer> result = new TreeSet<Integer>();
//		String[] lines = annotatedText.split("[\r\n]+");
//		String oldLine = "";
//		for (String line: lines) {
//			
//			// to prevent endless loops resulting from errors in the text or reg.ex,
//			// break if nothing has changes as compared to the previous iteration
//			while (line.indexOf("<z:species") >= 0) {
//				String spec = line.replaceFirst("^.*?(<z:species[^>]+>[^<]+</z:species>).*$", "$1");
//				line = line.replaceFirst("^.*?(<z:species[^>]+>[^<]+</z:species>)(.*)$", "$2");
//				String ids = spec.replaceFirst("^<z:species[^>]+ids=\"([^\"]*)\".*$", "$1");
//				for (String id: ids.split(";")) 
//					result.add(Integer.parseInt(id));
//			}
//		}
//		return result;
//	}
	

	/**
	 * Looks for indirect mentions of species in a text, for instance by means of cell lines and related terms.
	 * Returns a mapping from NCBI Taxon IDs to recognized terms.<br>
	 * For instance, a mention of <em>Jurkat cells</tt> clearly indicates that the species is human, Tax ID 9606.
	 * @param text
	 * @return
	 */
	public Map<Integer, Set<String>> getIndirectReferencesAndNames (String text) {
		Map<Integer, Set<String>> result = new HashMap<Integer, Set<String>>();

		String temp = text;
		// mask any characters that have a meaning in reg.exes
		temp = temp.replaceAll("([\\+\\-\\*\\]\\[\\(\\)])", "\\\\$1");
		
		for (String line: text.split("[\n\r]+")) {

			// cell lines
			// see, eg, 
			// - http://en.wikipedia.org/wiki/Cell_line#List_of_cell_lines
			// - http://en.wikipedia.org/wiki/List_of_contaminated_cell_lines
			// - http://en.wikipedia.org/wiki/Cell_culture
			// - http://www.atcc.org/
			//
			// human
			// unambiguous names of cell lines
			if (line.matches(".*(" +
					"AML\\-193" +
					"|CCRF\\-CEM" +
					"|COLO\\-800" +
					"|CTV\\-?1" +
					"|DU\\-145" +
					"|HEK\\-?293|human embryonic kidney cells?|human 293T" +
					"|HeLa|Henrietta Lacks" +
					"|human leukemia cells?" +
					"|HMEC|human mammary epithelial cells?" +
					"|HPB\\-ALL" +
					"|HUVEC|human umbilical vein endothelial cells?" +
					"|Jijoye" +
					"|Jurkat" +
					"|LNCap|Lncap" +
					"|MCF\\-?10A|MCF\\-?7|MCF\\-F|Michigan Cancer Foundation" +
					"|MDA\\-MB\\-435" +
					"|NALM\\-?6" +
					"|MUTZ\\-?3" +
					"|OCI/AML\\-?2" +
					"|OMK\\-?210" +
					"|P3JHR\\-?1" +
					"|RPMI\\-1788" +
					"|SHSY5Y" +
					"|SK\\-?HEP\\-?1" +
					"|SCLC\\-21/22H" +
					"|SW\\-480\\/SW\\-620" +
					")" + tokenEndMark + ".*")) {
				
				Set<String> names = result.get(9606);
				if (names == null) {
					names = new TreeSet<String>();
					result.put(9606, names);
				}
				names.add(line.replaceFirst("^.*(" +
					"AML\\-193" +
					"|CCRF\\-CEM" +
					"|COLO\\-800" +
					"|CTV\\-?1" +
					"|DU\\-145" +
					"|HEK\\-?293|human embryonic kidney cells?|human 293T" +
					"|HeLa|Henrietta Lacks" +
					"|human leukemia cells?" +
					"|HMEC|human mammary epithelial cells?" +
					"|HPB\\-ALL" +
					"|HUVEC|human umbilical vein endothelial cells?" +
					"|Jijoye" +
					"|Jurkat" +
					"|LNCap|Lncap" +
					"|MCF\\-?10A|MCF\\-?7|MCF\\-F|Michigan Cancer Foundation" +
					"|MDA\\-MB\\-435" +
					"|NALM\\-?6" +
					"|MUTZ\\-?3" +
					"|OCI/AML\\-?2" +
					"|OMK\\-?210" +
					"|P3JHR\\-?1" +
					"|RPMI\\-1788" +
					"|SHSY5Y" +
					"|SK\\-?HEP\\-?1" +
					"|SCLC\\-21/22H" +
					"|SW\\-480\\/SW\\-620" +
					")" + tokenEndMark + ".*$", "$1"));
			}

			// potentially ambiguous names: have to be followed by ... cell
			if (line.matches(".*((?:" +
					"A\\-?172" +
					"|A\\-?549" +
					"|BCP\\-?1|PEL" +
					"|293T\\?" +
					"|Dami" +
					"|HEL" +
					"|HK" +
					"|HL\\-?60" + 
					"|HT[\\-\\s]?29" +
					"|HU\\-3" +
					"|K\\-?562" +
					"|KYO\\-1" +
					"|MKN28" + 
					"|MO7[Ee]" +
					"|Peer|PEER" +
					"|Raji" +
					"|RC\\-?K8" +
					"|REH" +
					"|Strain[\\s\\-]L" +
					"|T\\-?24" +
					"|T\\-?84" +
					"|THP\\-?1" +
					"|U2OS" +
					"|U\\-?87" +
					"|U\\-?937" +
					"|W[I1]38" +
					")\\scells?)" + tokenEndMark + ".*")) {
				
				Set<String> names = result.get(9606);
				if (names == null) {
					names = new TreeSet<String>();
					result.put(9606, names);
				}
				names.add(line.replaceFirst("^.*((?:" +
						"A\\-?172" +
						"|A\\-?549" +
						"|BCP\\-?1|PEL" +
						"|293T\\?" +
						"|Dami" +
						"|HEL?" +
						"|HK" +
						"|HL\\-?60" + 
						"|HT[\\-\\s]?29" +
						"|HU\\-3" +
						"|K\\-?562" +
						"|KYO\\-1" +
						"|MKN28" + 
						"|MO7[Ee]" +
						"|Peer|PEER" +
						"|Raji" +
						"|RC\\-?K8" +
						"|REH" +
						"|Strain[\\s\\-]L" +
						"|T\\-?24" +
						"|T\\-?84" +
						"|THP\\-?1" +
						"|U2OS" +
						"|U\\-?87" +
						"|U\\-?937" +
						"|W[I1]38" +
						")\\scells?)" + tokenEndMark + ".*$", "$1"));
			}
			
			// can occur as "cell line XYZ" or "... cell line, XYZ, ..."
			if (line.matches("^.*(cell[\\s\\_]?lines?\\,?\\s(?:" +
					"A\\-?172" +
					"|A\\-?549" +
					"|AML\\-193" +
					"|BCP\\-?1|PEL" +
					"|CCRF\\-CEM" +
					"|COLO\\-800" +
					"|CTV\\-?1" +
					"|Dami" +
					"|DU\\-145" +
					"|HEK\\-?293|293T" +
					"|HEL" +
					"|HeLa|Henrietta Lacks" +
					"|HK" +
					"|HL\\-?60" +
					"|HMEC" +
					"|HPB\\-ALL" +
					"|HT[\\-\\s]?29" +
					"|HU\\-3" +
					"|HUVEC" +
					"|Jijoye" +
					"|Jurkat" +
					"|K\\-?562" +
					"|KYO\\-1" +
					"|LNCap|Lncap" +
					"|MCF\\-?10A|MCF\\-?7|MCF\\-F" +
					"|MDA\\-MB\\-435" +
					"|MKN28" + 
					"|MO7[Ee]" +
					"|NALM\\-?6" +
					"|MUTZ\\-?3" +
					"|OCI/AML\\-?2" +
					"|OMK\\-?210" +
					"|P3JHR\\-?1" +
					"|Peer|PEER" +
					"|Raji" +
					"|RC\\-?K8" +
					"|REH" +
					"|RPMI\\-1788" +
					"|SHSY5Y" +
					"|SK\\-?HEP\\-?1" +
					"|SCLC\\-21/22H" +
					"|Strain[\\s\\-]L" +
					"|SW\\-480\\/SW\\-620" +
					"|T\\-?24" +
					"|T\\-?84" +
					"|THP\\-?1" +
					"|U2OS" +
					"|U\\-?87" +
					"|U\\-?937" +
					"|W[I1]38" +
					"))" + tokenEndMark + ".*$")) {
				
				Set<String> names = result.get(9606);
				if (names == null) {
					names = new TreeSet<String>();
					result.put(9606, names);
				}
				names.add(line.replaceFirst("^.*(cell[\\s\\_]?lines?\\,?\\s(?:" +
						"A\\-?172" +
						"|A\\-?549" +
						"|AML\\-193" +
						"|BCP\\-?1|PEL" +
						"|CCRF\\-CEM" +
						"|COLO\\-800" +
						"|CTV\\-?1" +
						"|Dami" +
						"|DU\\-145" +
						"|HEK\\-?293|293T" +
						"|HEL" +
						"|HeLa|Henrietta Lacks" +
						"|HK" +
						"|HL\\-?60" +
						"|HMEC" +
						"|HPB\\-ALL" +
						"|HT[\\-\\s]?29" +
						"|HU\\-3" +
						"|HUVEC" +
						"|Jijoye" +
						"|Jurkat" +
						"|K\\-?562" +
						"|KYO\\-1" +
						"|LNCap|Lncap" +
						"|MCF\\-?10A|MCF\\-?7|MCF\\-F" +
						"|MDA\\-MB\\-435" +
						"|MKN28" + 
						"|MO7[Ee]" +
						"|NALM\\-?6" +
						"|MUTZ\\-?3" +
						"|OCI/AML\\-?2" +
						"|OMK\\-?210" +
						"|P3JHR\\-?1" +
						"|Peer|PEER" +
						"|Raji" +
						"|RC\\-?K8" +
						"|REH" +
						"|RPMI\\-1788" +
						"|SHSY5Y" +
						"|SK\\-?HEP\\-?1" +
						"|SCLC\\-21/22H" +
						"|Strain[\\s\\-]L" +
						"|SW\\-480\\/SW\\-620" +
						"|T\\-?24" +
						"|T\\-?84" +
						"|THP\\-?1" +
						"|U2OS" +
						"|U\\-?87" +
						"|U\\-?937" +
						"|W[I1]38" +
						"))" + tokenEndMark + ".*$", "$1"));
			}

			// dog
			if (line.matches("^.*(CMT cells?|canine mammary tumor" +
					"|MDCK[\\s\\-]?(?:II|2)|MDCK cells?|Madin[\\s\\-]Darby canine kidney)" + tokenEndMark + ".*$")) {
				Set<String> names = result.get(9615);
				if (names == null) {
					names = new TreeSet<String>();
					result.put(9615, names);
				}
				names.add(line.replaceFirst("^.*(" +
						"CMT cells?|canine mammary tumor" +
						"|MDCK[\\s\\-]?(?:II|2)|MDCK cells?|Madin[\\s\\-]Darby canine kidney" +
						")" + tokenEndMark + ".*$", "$1"));
			}
			
			// rat
			if (line.matches("^.*(" +
					"GH3 cells?|9L cells?|PC12 cells?)" + tokenEndMark + ".*$")) {
				Set<String> names = result.get(10116);
				if (names == null) {
					names = new TreeSet<String>();
					result.put(10116, names);
				}
				names.add(line.replaceFirst("^.*(" +
						"GH3 cells?|9L cells?|PC12 cells?)" + tokenEndMark + ".*$", "$1"));
			}

			// simian kidney COSM6
			// simian COS cell lines
			// simian COS cells
			
			// moth
			if (line.matches("^.*(Sf[\\-]?9 cells?)" + tokenEndMark + ".*$")) {
				Set<String> names = result.get(7108);
				if (names == null) {
					names = new TreeSet<String>();
					result.put(7108, names);
				}
				names.add(line.replaceFirst("^.*(Sf[\\-]?9 cells?)" + tokenEndMark + ".*$", "$1"));
			}
			
			// asian tiger mosquito
			if (line.matches(".*(C6\\/36)" + tokenEndMark + ".*")) {
				Set<String> names = result.get(7160);
				if (names == null) {
					names = new TreeSet<String>();
					result.put(7160, names);
				}
				names.add(line.replaceFirst("^.*(C6\\/36)" + tokenEndMark + ".*$", "$1"));
			}
			
			// xenopus
			if (line.matches(".*(A6 kidney epithelial cells?|A6 cells?)" + tokenEndMark + ".*")) {
				Set<String> names = result.get(8355);
				if (names == null) {
					names = new TreeSet<String>();
					result.put(8355, names);
				}
				names.add(line.replaceFirst("^.*(A6 kidney epithelial cells?|A6 cells?)" + tokenEndMark + ".*$", "$1"));
			}
			
			// bovine
			if (line.matches(".*(BHK cells?|BHK bovine cells?)" + tokenEndMark + ".*")) {
				Set<String> names = result.get(9913);
				if (names == null) {
					names = new TreeSet<String>();
					result.put(9913, names);
				}
				names.add(line.replaceFirst("^.*(BHK cells?|BHK bovine cells?)" + tokenEndMark + ".*$", "$1"));
			}
			
			// chinese hamster
			if (line.matches(".*(CHO cells?)" + tokenEndMark + ".*")) {
				Set<String> names = result.get(10029);
				if (names == null) {
					names = new TreeSet<String>();
					result.put(10029, names);
				}
				names.add(line.replaceFirst("^.*(CHO cells?)" + tokenEndMark + ".*$", "$1"));
			}
			
			// tobacco
			if (line.matches(".*(BY\\-2 cells?)" + tokenEndMark + ".*")) {
				Set<String> names = result.get(4097);
				if (names == null) {
					names = new TreeSet<String>();
					result.put(4097, names);
				}
				names.add(line.replaceFirst("^.*(BY\\-2 cells?)" + tokenEndMark + ".*$", "$1"));
			}
			
			// zebrafish
			if (line.matches(".*(ZF\\-?4 cells?|AB\\-?9 cells?)" + tokenEndMark + ".*")) {
				Set<String> names = result.get(7955);
				if (names == null) {
					names = new TreeSet<String>();
					result.put(7955, names);
				}
				names.add(line.replaceFirst("^.*(ZF\\-?4 cells?|AB\\-?9 cells?)" + tokenEndMark + ".*$", "$1"));
			}
			
			// mouse
			if (line.matches(".*(" +
					"3T3|MC\\-?3T3|NIH\\-3T3" +
					"|AtT\\-?20" +
					"|bEnd\\.[35]" +
					"|C3H\\-10T1/2|C3H\\-10T1|C3H\\-10T2" +
					"|MTD\\-1A" +
					"|MyEnd" +
					"|mouse L cells?" +
					"|murine corticotropic tumour cell line" +
					")" + tokenEndMark + ".*")) {
				Set<String> names = result.get(10090);
				if (names == null) {
					names = new TreeSet<String>();
					result.put(10090, names);
				}
				names.add(line.replaceFirst("^.*(" +
					"3T3|MC\\-?3T3|NIH\\-3T3" +
					"|AtT\\-?20" +
					"|bEnd\\.[35]" +
					"|C3H\\-10T1/2|C3H\\-10T1|C3H\\-10T2" +
					"|MTD\\-1A" +
					"|MyEnd" +
					"|mouse L cells?" +
					"|murine corticotropic tumour cell line" +
					")" + tokenEndMark + ".*$", "$1"));
			}
			
			// ape, african green monkey
			if (line.matches(".*(COS\\-?[17]|Cercopithecus aethiops|origin\\-defective SV\\-40" +
					"|VERO|Vero cells?)" + tokenEndMark + ".*")) {
				Set<String> names = result.get(9534);
				if (names == null) {
					names = new TreeSet<String>();
					result.put(9534, names);
				}
				names.add(line.replaceFirst("^.*(COS\\-?[17]|Cercopithecus aethiops|origin\\-defective SV\\-40" +
					"|VERO|Vero cells?)" + tokenEndMark + ".*$", "$1"));
			}
			
		}
		
		return result;
	}


//	/**
//	 * Checks a text for indirect mentions of species, e.g., references to cell
//	 * lines that originated from an organism.
//	 * 
//	 * @param text
//	 * @return Set of NCBI Taxonomy identifiers
//	 */
//	public Set<Integer> getIndirectReferences (String text) {
//		Set<Integer> result = new TreeSet<Integer>();
//		Map<Integer, Set<String>> map = getIndirectReferencesAndNames(text);
//		result.addAll(map.keySet());
//		return result;
//	}
}
