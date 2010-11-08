package gnat.retrieval;

import gnat.representation.Gene;
import gnat.representation.GeneContextModel;
import gnat.representation.GeneRepository;
import gnat.utils.FileHelper;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.TreeSet;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * Retrieves information on a gene or set of genes with EntrezGene as the
 * hub for background knowledge. Goes from there to GOA and UniProt to
 * collect more data.
 * 
 * 
 * @author Joerg
 *
 */

public class GetInfoOnGene {

	/**
	 * The URL to access a gene record in XML format from NCBI eUtils. Simply add the Entrez gene
	 * ID (or list of IDs, comma-separated.)
	 */
	static String URL_ENTREZGENE_XMLDATA = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=gene&retmode=xml&id=";

	/** 
	 * URL to access UniProt entries. Add the UniProt ID (P17173) and ".xml".
	 */
	static String URL_UNIPROT_XMLDATA = "http://beta.uniprot.org/uniprot/";
	
	/** EntrezGene record*/
	public static String xpq_record_entrezgene = "//Entrezgene";

	/** EntrezGene record*/
	public static String xpq_record_uniprot = "//uniprot/entry";

	
	/** GO codes */
	public static String xpq_goid = "//Other-source/Other-source_src/Dbtag[Dbtag_db=\"GO\"]/Dbtag_tag/Object-id/Object-id_id";

	/** GO terms */
	public static String xpq_goterm = "//Other-source[Other-source_src/Dbtag/Dbtag_db=\"GO\"]/Other-source_anchor";

	/** UniProt IDs*/
	public static String xpq_uniprotid = "//Other-source/Other-source_src/Dbtag[Dbtag_db=\"UniProt\"]/Dbtag_tag/Object-id/Object-id_str"
		//"//Other-source/Other-source_src/Dbtag[Dbtag_db=\"UniProtKB/Swiss-Prot\"]/Dbtag_tag/Object-id/Object-id_str"
		+ " | //Other-source/Other-source_src/Dbtag[Dbtag_db=\"UniProtKB\"]/Dbtag_tag/Object-id/Object-id_str"
		+ " | //Other-source/Other-source_src/Dbtag[Dbtag_db=\"UniProtKB/Swiss-Prot\"]/Dbtag_tag/Object-id/Object-id_str"
		+ " | //Other-source/Other-source_src/Dbtag[Dbtag_db=\"UniProtKB/TrEMBL\"]/Dbtag_tag/Object-id/Object-id_str";
	
	/** Gene RIFs (PubMed snippets)*/
	public static String xpq_generif = "//Gene-commentary[Gene-commentary_refs/Pub/Pub_pmid/PubMedId>0]/Gene-commentary_text";

	/** PubMed IDs*/
	public static String xpq_pmid = "//Gene-commentary/Gene-commentary_refs/Pub/Pub_pmid/PubMedId";

	/** EntrezGene summary */
	public static String xpq_summary = "//Entrezgene_summary";

	/** Location (Gene-ref_maploc, BioSource_SubSource) */
	public static String xpq_location = //"//Gene-ref_locus | " +
			"//Gene-ref_maploc | //BioSource_subtype/SubSource[SubSource_subtype[@value=\"chromosome\"]]/SubSource_name";

	/** */
	public static String xpq_interactorname = //"//Gene-commentary[Gene-commentary_heading=\"Interactions\"]/Gene-commentary_comment/Gene-commentary//Gene-commentary[Gene-commentary_type[@value=\"peptide\"]]//Other-source[Other-source_src/Dbtag/Dbtag_db=\"GeneID\"]/Other-source_anchor"
		//+ " | //Gene-commentary[Gene-commentary_heading=\"Interactions\"]/Gene-commentary_comment/Gene-commentary//Gene-commentary[Gene-commentary_type[@value=\"other\"]]//Other-source[Other-source_src/Dbtag/Dbtag_db=\"GeneID\"]/Other-source_anchor"
		"//Gene-commentary[Gene-commentary_heading=\"Interactions\"]/Gene-commentary_comment/Gene-commentary//Gene-commentary[Gene-commentary_type[@value=\"peptide\"]]/Gene-commentary_source[Other-source[Other-source_src/Dbtag/Dbtag_db=\"GeneID\"]]//Other-source_anchor"
		+ " | //Gene-commentary[Gene-commentary_heading=\"Interactions\"]/Gene-commentary_comment/Gene-commentary//Gene-commentary[Gene-commentary_type[@value=\"other\"]]/Gene-commentary_source[Other-source[Other-source_src/Dbtag/Dbtag_db=\"GeneID\"]]//Other-source_anchor";

	/** */
	public static String xpq_uniprot_keywords = "//keyword";

	/** */
	public static String xpq_uniprot_tissuespec = "//comment[@type=\"tissue specificity\"]/text";

	/** */
	public static String xpq_uniprot_disease = "//comment[@type=\"disease\"]/text";

	/** */
	public static String xpq_uniprot_function = "//comment[@type=\"function\"]/text";

	/** */
	public static String xpq_uniprot_subunit = "//comment[@type=\"subunit\"]/text";

	/** */
	public static String xpq_uniprot_cdomain = "//comment[@type=\"domain\"]/text";

	/** */
	public static String xpq_uniprot_feature_domain = "//feature[@type=\"domain\"]/@description";
	
	
	/** */
	ResourceHelper rh = new ResourceHelper(false);
	
	
	/**
	 * Queries a Document with the given XPath query.<br>
	 * Removes duplicate elements (case sensitive). Sorts elements lexicographically (uses a TreeSet).
	 *
	 * @param entrezGeneRecord
	 * @param path
	 * @return all values in an array
	 */
	@SuppressWarnings("unchecked")
	public static String[] collectDataOnElements (Element element, String xpath) {
		TreeSet<String> set = new TreeSet<String>();

		// query the document and collect all elements satisfying the query
		try {
			XPath xp = XPath.newInstance(xpath);
			List<Element> content = xp.selectNodes(element);
			for (Element e : content)
				set.add(e.getTextTrim());
		} catch (JDOMException jde) {
			jde.printStackTrace();
		}

		// from set to array
		String[] result = new String[set.size()];
		int i = 0;
		for (String e: set) {
			result[i] = e;
			i++;
		}

		// a single number as location certainly refers to a chromosome and it should not be
		// searched alone
		if (xpath.equals(xpq_location))
			for (int j = 0; j < result.length; j++)
				if (result[j].matches("^(\\d+|[XYZ])$"))
					result[j] = "chromosome " + result[j];

		// remove all NP_-IDs from the set of interactor names
		if (xpath.equals(xpq_interactorname)) {
			for (int j = 0; j < result.length; j++)
				if (result[j].startsWith("NP_"))
					set.remove(result[j]);
			i = 0;
			for (String e: set) {
				result[i] = e;
				i++;
			}
		}

		return result;
	}
	
	
	/**
	 * Queries a Document with the given XPath query.<br>
	 * Removes duplicate elements (case sensitive). Sorts elements lexicographically (uses a TreeSet).
	 *
	 * @param entrezGeneRecord
	 * @param path
	 * @return all values in an array
	 */
	@SuppressWarnings("unchecked")
	public static String[] collectDataOnAttributes (Element element, String xpath) {
		TreeSet<String> set = new TreeSet<String>();

		// query the document and collect all elements satisfying the query
		try {
			XPath xp = XPath.newInstance(xpath);
			List<Attribute> content = xp.selectNodes(element);
			for (Attribute attr : content)
				set.add(attr.getValue().trim());
		} catch (JDOMException jde) {
			jde.printStackTrace();
		}

		// from set to array
		String[] result = new String[set.size()];
		int i = 0;
		for (String attr: set) {
			result[i] = attr;
			i++;
		}

		return result;
	}
	
	
	/**
	 * 
	 * @param ID
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Gene getFullGene (String ID) {
		long startmillis = System.currentTimeMillis();
		Gene gene = new Gene(ID);
		GeneContextModel gcm = new GeneContextModel();
		
		System.out.println("Fetching EntrezGene XML for " + ID);
		String entrezGeneXML = rh.getURLContent(URL_ENTREZGENE_XMLDATA + ID);
		System.out.println("  XML content has length " + entrezGeneXML.length());
		
		// query the XML using XPath
		SAXBuilder builder = new SAXBuilder();
		List<Element> records = null;
		try {
			System.out.println("  Building document");
			Document document = builder.build(new StringReader(entrezGeneXML));
			
			System.out.println("  XQuerying document");
			XPath xp = XPath.newInstance(xpq_record_entrezgene);
			records = xp.selectNodes(document);
			// there should be only one single record
			for (Element record : records) {
				
				// fetch different types of info from the EntrezGene record
				// the EntrezGene summary
				String[] data = collectDataOnElements(record, xpq_summary);
				for (String summary: data)
					gcm.addEntrezGeneSummary(summary);

				// chromosomal location(s)
				data = collectDataOnElements(record, xpq_location);
				gcm.addLocations(data);
				
				// GO terms...
				data = collectDataOnElements(record, xpq_goterm);
				gcm.addGOTerms(data);
				
				// ...and GO codes
				data = collectDataOnElements(record, xpq_goid);
				gcm.addGOCodes(data);
				
				// GeneRIFS
				data = collectDataOnElements(record, xpq_generif);
				gcm.addGeneRIFs(data);
				
				// PubMed IDs
				data = collectDataOnElements(record, xpq_pmid);
				gcm.addPubMedReferences(data);
				
				// known interaction partners (PPI or PGI)
				data = collectDataOnElements(record, xpq_interactorname);
				//gcm.a
				
				// UniProt IDs for the gene's products
				data = collectDataOnElements(record, xpq_uniprotid);
				// TODO: at this time, get info from UniProt for all the IDs
				// (aa length, diseases, tissue spec, ...)
				System.out.println("  Found " + data.length + " UniProt-IDs.");
				List<Element> records2 = null;
				for (String uniprotID: data) {
					
					System.out.println("  Fetching UniProt XML for " + uniprotID);
					String uniprotXML = rh.getURLContent(URL_UNIPROT_XMLDATA + uniprotID + ".xml");
					uniprotXML = uniprotXML.replaceFirst("<\\?xml[^>]+>", "");
					uniprotXML = uniprotXML.replaceFirst("<uniprot(\\s[^>]+)?>", "<uniprot>");
					System.out.println("    XML content has length " + uniprotXML.length());
					//System.out.println(uniprotXML);
					//SAXBuilder builder2 = new SAXBuilder();
					Document document2 = builder.build(new StringReader(uniprotXML));
					
					System.out.println("    XQuerying document");
					XPath xp2 = XPath.newInstance(xpq_record_uniprot);
					records2 = xp2.selectNodes(document2);
					// there should be only one single record
					System.out.println("    Records: " + records2.size());
					for (Element record2 : records2) {
						String[] data2 = collectDataOnElements(record2, xpq_uniprot_keywords);
						//System.out.print("    Found " + data2.length + " keywords:");
						//for (int i = 0; i < data2.length; i++)
						//	System.out.print(" " + data2[i]);
						//System.out.println();
						gcm.addKeywords(data2);
						
						data2 = collectDataOnElements(record2, xpq_uniprot_tissuespec);
						//System.out.print("    Found " + data2.length + " tissue specs:");
						gcm.addTissues(data2);
						
						data2 = collectDataOnAttributes(record2, xpq_uniprot_feature_domain);
						gcm.addProteinDomains(data2);
						System.out.print("    Found " + data2.length + " domains:");
						for (int i = 0; i < data2.length; i++)
							System.out.print(" " + data2[i]);
						System.out.println();
					}
					/*gcm.addDiseases(diseases);
					gcm.addEnzymaticActivity(acts);
					gcm.addFunctions(fcts);
					
					gcm.addGOCodes(codes);
					gcm.addGOTerms(terms);
					gcm.addProteinDomains(domains);
					gcm.addProteinLengths(lengths);
					gcm.addProteinLengths(lengths);
					gcm.addPubMedReferences(pmids);
					*/
				}

				

			}

			
		} catch (JDOMException jde) {
			//System.err.println(jde.getMessage());
			jde.printStackTrace();
		} catch (IOException ioe) {
			//System.err.println(ioe.getMessage());
			ioe.printStackTrace();
		}

		gene.setContextModel(gcm);
		System.out.println("  Done in " + ((float)(System.currentTimeMillis() - startmillis)/1000f) + "sec.");
		
		return gene;
	}
	
	
	/**
	 * 
	 * @param args
	 */
	public static void main (String[] args) {
		
		GetInfoOnGene go = new GetInfoOnGene();
		
		GeneRepository geneRep = new GeneRepository();

		//Gene gene = go.getFullGene("493754");
		//geneRep.addGene(gene);
		
		//Gene gene = go.getFullGene("4800");
		//geneRep.addGene(gene);
		
		//Gene gene = go.getFullGene("4801");
		//geneRep.addGene(gene);
		
		Gene gene = go.getFullGene("8772");
		geneRep.addGene(gene);
		
		
		System.out.println("GeneRepository has " + geneRep.size() + " genes.");
		
		FileHelper.writeObjectToFile(geneRep, new File("generep.object"));
		
		//geneRep = null;
		//geneRep = (GeneRepository)FileHelper.readObjectFromFile(new File("generep.object"));
		//System.out.println("GeneRepository has " + geneRep.size() + " genes.");

	}
	
	
}
