package gnat.client;

import gnat.ISGNProperties;
import gnat.filter.nei.AlignmentFilter;
import gnat.filter.nei.GeneRepositoryLoader;
import gnat.filter.nei.IdentifyAllFilter;
import gnat.filter.nei.ImmediateContextFilter;
import gnat.filter.nei.MultiSpeciesDisambiguationFilter;
import gnat.filter.nei.NameValidationFilter;
import gnat.filter.nei.RecognizedEntityUnifier;
import gnat.filter.nei.SpeciesFrequencyFilter;
import gnat.filter.nei.StopWordFilter;
import gnat.filter.nei.UnambiguousMatchFilter;
import gnat.filter.nei.UnspecificNameFilter;
import gnat.filter.ner.DefaultSpeciesRecognitionFilter;
import gnat.filter.ner.RunDictionaries;
import gnat.preprocessing.NameRangeExpander;
import gnat.representation.Gene;
import gnat.representation.RecognizedEntity;
import gnat.representation.Text;
import gnat.representation.TextAnnotation;
import gnat.representation.TextFactory;
import gnat.representation.TextRepository;
import gnat.utils.AlignmentHelper;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * A simple processing pipeline that takes a directory as input, reads all XML files (PubMed XML format)
 * and annotates the predicted genes inline with the text. Returns a simplified version of that XML,
 * containing only the ArticleTitle and AbstractText elements (but no author information, etc.)
 * <br><br>
 * Assumes that the gene repository is held in a local database (specified in isgn_properties.xml)
 * and that dictionary servers for human genes and GeneOntology terms are running,
 * on ports specified in config/taxonToServerPorts, on the server specified under 'dictionaryServer'  
 * in isgn_properties.xml.
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 */
public class JustAnnotateInline {

	/** Name of the XML element tag that will be used to annotate genes found by GNAT. */
	public static String xml_tag = "src:GNAT";
	/** Name of the XML attribute for the {@link #xml_tag} element, which will contain the gene ID(s). */
	public static String xml_attribute_id     = "id";
	/** Name of the XML attribute for the {@link #xml_tag} element, which will contain the gene symbols(s), also known as primary terms. */
	public static String xml_attribute_symbol = "pt";
	/** Name of the XML attribute for the {@link #xml_tag} element, which will contain the disambiguation score, or multiple
	 *  semi-colon-separated scores in case multiple candidates are left. */
	public static String xml_attribute_score = "score";
	/** */
	public static String xml_attribute_other_ids = "otherIds";


	/**
	 * 
	 * @param args
	 */
	public static void main (String[] args) {
		// Each process is capsuled by a Run
		Run run = new Run();
		run.verbosity = 7;

		// check for command line parameters
		if (args.length == 0 || args[0].matches("\\-\\-?h(elp)?")) {
			System.out.println("Need a directory with <ID>.xml files to annotate as parameter!");
			System.out.println("Optional parameters:");
			System.out.println(" -v        -  Set verbosity level for progress and debugging information");
			System.out.println("              Default: 0");
			System.out.println(" --outdir  -  Folder in which to write the output XML");
			System.out.println("              By default, will write into the current directory.");
			System.exit(1);
		}
		
		// parse and store command line parameters
		String dir = "";        // directory to read from
		String outDir = ".";    // 	
		for (int a = 0; a < args.length; a++) {
			// parameter is -v to regulate verbosity at runtime
			if (args[a].matches("\\-v=\\d+"))
				run.verbosity = Integer.parseInt(args[0].replaceFirst("^\\-v=(\\d+)$", "$1"));
			else if (args[a].toLowerCase().matches("\\-\\-?outdir")) 
				outDir = args[++a];
			else {
				dir = args[a];
				File DIR = new File(dir);
				if (DIR.exists() && DIR.canRead()) {
					if (DIR.isDirectory()) {
						if (DIR.list().length == 0) {
							System.err.println("Error: there seem to be no files in the directory " + args[a]);
							System.exit(3);
						}
					} else {
						// is a single file
					}
				} else {
					System.err.println("Error: cannot access the file/directory " + args[a]);
					System.exit(2);					
				}
			}
		}


		//////////
		// INPUT
		
		// load all texts required for the test
		//run.setTextRepository(TextFactory.loadTextRepositoryFromDirectories(dir));
		TextRepository tr = TextFactory.loadTextRepositoryFromDirectories(dir);
		run.setTextRepository(tr);
		
		// assume the file name refers to a PubMed ID (path/<pmid>.xml)
		run.setFilenamesAsPubMedId();

		
		Collection<Text> texts2 = tr.getTexts();
		for (Text text: texts2) {
			System.out.println(text.plainText);
		}
		
		//////////
		// PROCESSING
		// Plug together a processing pipeline: add filters to the Run
		
		//////////
		// Pre-processing filters here:
		run.addFilter(new NameRangeExpander());

		
		//////////
		// NER filters here:
		// default species NER: spots human, mouse, rat, yeast, and fly only
		DefaultSpeciesRecognitionFilter dsrf = new DefaultSpeciesRecognitionFilter();
		System.err.println("#Skipping species NER, using only defaults: " + ISGNProperties.get("defaultSpecies"));
		dsrf.useAllDefaultSpecies = true;
		run.addFilter(dsrf);
		
		// construct a dictionary for human, mouse, yeast, fruit fly genes only
		RunDictionaries afewDictionaryFilters = new RunDictionaries();
		//afewDictionaryFilters.setLimitToTaxons(9606, 10090, 10116, 559292, 7227);
		afewDictionaryFilters.setLimitToTaxons(9606);
		run.addFilter(afewDictionaryFilters);
		
		// print the status of gene NER right after the NER step, before filtering anything out
		//run.addFilter(new PrintStatus());
		
		//////////
		// NER post-processing filters here:
		run.addFilter(new RecognizedEntityUnifier());

		// include a few disambiguation filters that do not need specific information on each candidate gene
		// thus, these work on the gene's name and its context in the text
		run.addFilter(new ImmediateContextFilter());
		
		// strictFPs_2_2_context_all.object contains data on the context defined by two tokens left and two tokens right of a gene name
		//run.addFilter(new LeftRightContextFilter("data/strictFPs_2_2_context_all.object", "data/nonStrictFPs_2_2_context_all.object", 0d, 2, 2));

		// print the status on all recognized genes before loading information on each gene
		//run.addFilter(new PrintStatus());
		
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
		run.addFilter(new NameValidationFilter());
		
		// filter by the number of occurrences of each organism
		run.addFilter(new SpeciesFrequencyFilter());
		
		// Final disambiguation filter
		run.addFilter(new MultiSpeciesDisambiguationFilter(
				Integer.parseInt(ISGNProperties.get("disambiguationThreshold")),
				Integer.parseInt(ISGNProperties.get("maxIdsForCandidatePrediction"))));
		
		// Mark everything that "survived" until here as OK, will be reported in output 
		run.addFilter(new IdentifyAllFilter());
		
		
		//////////
		// Run all filters, changing run.context, run.textRepository, and run.geneRepository
		run.runFilters();

		
		//////////
		// OUTPUT
		// get the results for each text, in BioCreative tab-separated format
//		List<String> result = run.context.getInlineAnnotations();
		// create the output directory if it does not exist
		if (!outDir.equals(".")) {
			File DIR = new File(outDir);
			if (!DIR.exists())
				DIR.mkdirs();
		}
		Set<Text> texts = run.context.getTexts();
		
		System.out.println("\n#By text, with type:");
		for (Text text: texts) {
			String id = text.getID();
//			File FILE = new File(outDir + "/" + id + ".annotated.xml");
			
			Set<RecognizedEntity> entities = run.context.getRecognizedEntitiesInText(text);
			for (RecognizedEntity re: entities) {
				
				TextAnnotation ta = re.getAnnotation();
				
				System.out.println(text.getID() + "\t" + re.getBegin() + "\t" + re.getEnd() + "\t" + re.getName() + "\t" + ta.getType());
			}

//			try {
//				BufferedWriter wr = new BufferedWriter(new FileWriter(FILE, false));
//				wr.append(textWithInlineXml);
//				wr.close();
//			} catch (IOException ioe) {
//				ioe.printStackTrace();
//			}
		}
		
		System.out.println("\n#By position, across texts:");
		List<String> genes = run.context.toIdentifiedGeneList_SortedByPosition();
		for (String gene: genes)
			System.out.println(gene);
		
		
		System.out.println();
		Map<String, List<String>> sorted_genes_by_text = run.context.toIdentifiedGeneList_SortedByPosition_byText();
		System.err.println("TextIds: " + run.getTextRepository().getTextIDs());
		for (String text_id: run.getTextRepository().getTextIDs()) {
			// if any genes were recognized in the current text:
			if (sorted_genes_by_text.containsKey(text_id)) {
				Text aText = run.context.getText_ifAnnotated(text_id);
				String annotated = aText.plainText;
				
				List<String> genes_in_this_text = sorted_genes_by_text.get(text_id);
				for (String gene_tsv: genes_in_this_text) {
					String[] cols = gene_tsv.split("\t");
					int start = Integer.parseInt(cols[4]);
					int end   = Integer.parseInt(cols[5]);
					
					annotated = annotated.substring(0, end + 1) + "</" + xml_tag + ">" + annotated.substring(end + 1);
					
					// insert the gene's ID into the XML
					String id = cols[1];
					String otherIds = "";
					// in some cases, candidate IDs with the same score are returned
					// pick the first ID and set as main ID
					// add the other IDs in a separate XML attribute
					if (id.matches(".*[\\;\\,].*")) {
						String[] allIds = id.split("\\s*[\\,\\;]\\s*");
						id = allIds[0].trim();
						if (allIds.length > 1) {
							otherIds = allIds[1];
							for (int a = 2; a < allIds.length; a++)
								otherIds += ";" + allIds[a];
						}
					}
					String insert = "<" + xml_tag + " " + xml_attribute_id + "=\"" + id + "\"";
					if (otherIds.length() > 0)
						insert += " " + xml_attribute_other_ids + "=\"" + otherIds + "\""; 
					
					// get the Gene object for the (main) gene ID
					// and from that, get the official symbol (if known)
					// and add to XML attribute
					Gene gene = run.getGene(id);
					String symbol = "";
					if (gene != null && gene.officialSymbol != null && gene.officialSymbol.length() > 0)
						symbol = gene.officialSymbol;
					if (symbol.matches("\\s*[\\;\\,]\\s*"))
						symbol = symbol.split("\\s*[\\,\\;]\\s*")[0].trim();
					if (symbol.length() > 0)
						insert += " " + xml_attribute_symbol + "=\"" + gene.officialSymbol + "\"";
					
					//float score = run.context.getConfidenceScore(gene, text);
					//System.err.println("Getting score for text " + text_id);
					float score = run.context.getConfidenceScore(gene, text_id);
					if (score >= 0.0)
						insert +=  " " + xml_attribute_score + "=\"" + score + "\"";
					
					insert += ">";
					annotated = annotated.substring(0, start) + insert + annotated.substring(start);
					
					//System.out.println(plain);
				}
	
				String annotatedTitle = annotated.replaceFirst("^(.+?[\\.\\!\\?])\\s.*$", "$1");
				aText.annotateXmlTitle(annotatedTitle);
				
				String annotatedAbstract = annotated.replaceFirst("^.+?[\\.\\!\\?]\\s(.*)$", "$1");
				aText.annotateXmlAbstract(annotatedAbstract);
				
				System.out.println("##########");
				System.out.println(aText.annotatedXml);
				System.out.println("##########");

				// if the XML tag used to mark gene names has a prefix ("prefix:TAG"), we need to bind this prefix
				if (xml_tag.indexOf(":") > 0) {
					String prefix = xml_tag.replaceFirst("^(.+?)\\:.*$", "$1"); 
					aText.addPrefixToXml(prefix);
					aText.buildJDocumentFromAnnotatedXml();
				}
				
				String outfileName = aText.getID() + ".annotated.xml";
				aText.toXmlFile(outDir + "/" + outfileName);


			// or, if no gene was recognized in the current text:
			} else {
				Text aText = run.getTextRepository().getText(text_id);
				String outfileName = aText.getID() + "_nogenesfound.annotated.xml";
				aText.toXmlFile(outDir + "/" + outfileName);
			}
			
			
		}

	}
	
	
}
