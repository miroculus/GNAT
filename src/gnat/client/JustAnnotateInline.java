package gnat.client;

import gnat.ISGNProperties;
import gnat.filter.nei.AlignmentFilter;
import gnat.filter.nei.GeneRepositoryLoader;
import gnat.filter.nei.IdentifyAllFilter;
import gnat.filter.nei.ImmediateContextFilter;
import gnat.filter.nei.LeftRightContextFilter;
import gnat.filter.nei.MultiSpeciesDisambiguationFilter;
import gnat.filter.nei.NameValidationFilter;
import gnat.filter.nei.RecognizedEntityUnifier;
import gnat.filter.nei.SpeciesFrequencyFilter;
import gnat.filter.nei.StopWordFilter;
import gnat.filter.nei.UnambiguousMatchFilter;
import gnat.filter.nei.UnspecificNameFilter;
import gnat.filter.ner.DefaultSpeciesRecognitionFilter;
import gnat.filter.ner.RunAllGeneDictionaries;
import gnat.preprocessing.NameRangeExpander;
import gnat.representation.Gene;
import gnat.representation.IdentificationStatus;
import gnat.representation.RecognizedEntity;
import gnat.representation.Text;
import gnat.representation.TextAnnotation;
import gnat.representation.TextFactory;
import gnat.utils.AlignmentHelper;
import gnat.utils.StringHelper;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
	public static String xml_tag = "GNAT";
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
		run.verbosity = 0;

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
			else if (args[a].toLowerCase().matches("\\-\\-?outdir\\=.+")) 
				outDir = args[a].replaceFirst("^\\-\\-?[Oo][Uu][Tt][Dd][Ii][Rr]\\=", "");
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
		
		if (dir.length() == 0) {
			System.out.println("Please specify an input directory!");
			System.exit(1);
		} else {
			File DIR = new File(dir);
			if (!DIR.exists() || !DIR.isDirectory()) {
				System.out.println("Please specify a valid input directory!");
				System.exit(1);
			}
		}


		//////////
		// INPUT

		// load all texts required for the test
        run.setTextRepository(TextFactory.loadTextRepositoryFromDirectories(dir));
		
		//////////
		// PROCESSING
		// Plug together a processing pipeline: add filters to the Run
		
		//////////
		// Pre-processing filters here:
        boolean keepTextIntact = true;
        String keeptemp = ISGNProperties.get("keepTextIntact");
        if (keeptemp.toLowerCase().matches("(0|no|false)"))
        	keepTextIntact = false;
        if (!keepTextIntact)
        	run.addFilter(new NameRangeExpander());
		
		//////////
		// NER filters here:
		// default species NER: spots human, mouse, rat, yeast, and fly only
//		DefaultSpeciesRecognitionFilter dsrf = new DefaultSpeciesRecognitionFilter();
		//System.err.println("#Skipping species NER, using only defaults: " + ISGNProperties.get("defaultSpecies"));
		//dsrf.useAllDefaultSpecies = true;
//		run.addFilter(dsrf);
		run.addFilter(new DefaultSpeciesRecognitionFilter());
		String assumeSpecies = ISGNProperties.get("assumeSpecies");
		if (assumeSpecies != null && assumeSpecies.length() > 0) {
			String[] species = assumeSpecies.split("[\\;\\,]\\s*");
			for (String spec: species) {
				if (!spec.matches("\\d+")) continue;
				int tax = Integer.parseInt(spec);
				for (Text text : run.getTextRepository().getTexts())
					text.addTaxonId(tax);
			}
		}
		
		// construct a dictionary for human, mouse, yeast, fruit fly genes only
		RunAllGeneDictionaries afewDictionaryFilters = new RunAllGeneDictionaries();
		//afewDictionaryFilters.setLimitToTaxons(9606, 10090, 10116, 559292, 7227);
//		afewDictionaryFilters.setLimitToTaxons(9606);
		afewDictionaryFilters.setLimitToTaxons(9606, 10090, 10116, 559292, 7227);
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
		run.addFilter(new LeftRightContextFilter("data/strictFPs_2_2_context_all.object", "data/nonStrictFPs_2_2_context_all.object", 0d, 2, 2));

		//
		run.addFilter(new ImmediateContextFilter());
		
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
		// Only for high-recall runs
		String tuning = ISGNProperties.get("tuning");
		if (tuning != null && tuning.equalsIgnoreCase("recall"))
			run.addFilter(new IdentifyAllFilter());
		
		
		//////////
		// RUN
		
		// Run all filters, affecting run.context, run.textRepository, and run.geneRepository
		run.runFilters();

		
		//////////
		// OUTPUT

		// create the output directory if it does not exist
		if (!outDir.equals(".")) {
			File DIR = new File(outDir);
			if (!DIR.exists())
				DIR.mkdirs();
		}
		Set<Text> texts = run.context.getTexts();
		
		
		for (Text text: texts) {
			Set<RecognizedEntity> entities = run.context.getRecognizedEntitiesInText(text);
			System.err.println("For text " + text.getPMID() + ", found " + entities.size() + " entities (including duplicates):");
			// sort entities by position within each text:
			List<SortableEntity> sortedEntities = new LinkedList<SortableEntity>();
			for (RecognizedEntity re: entities) {
				sortedEntities.add(new SortableEntity(re));
			}
			Collections.sort(sortedEntities);
			// insert into the text from back to end
			Collections.reverse(sortedEntities);
			
			
			String annotatedText = text.plainText;
			//System.err.println("Original text:\n-----\n"+annotatedText);
			for (SortableEntity se: sortedEntities) {
				TextAnnotation ta = se.entity.getAnnotation();
				ta.setType(TextAnnotation.Type.GENE);
				//System.err.print(text.getID()
				//		+ "\t" + se.entity.getBegin() + "\t" + se.entity.getEnd()
				//		+ "\t" + se.entity.getName() + "\t" + ta.getType().toString());
				IdentificationStatus idStatus = run.context.getIdentificationStatus(se.entity);
				String geneId = idStatus.getId();
				//System.err.println("\t" + geneId + "\t" + idStatus.getIdCandidates());
				Set<String> otherIds_set = new TreeSet<String>();
				otherIds_set.addAll(idStatus.getIdCandidates());
				if (otherIds_set != null && otherIds_set.size() > 0 && geneId != null)
					otherIds_set.remove(geneId);
				String otherIds = "";
				if (otherIds_set.size() > 0)
					otherIds = StringHelper.joinStringSet(otherIds_set, ";");
				
				annotatedText = annotatedText.substring(0, se.end + 1) + "</" + xml_tag + ">" + annotatedText.substring(se.end + 1);
				
				// insert the gene's ID into the XML
				//String geneId = cols[1];
				//String otherIds = "";
				// in some cases, candidate IDs with the same score are returned
				// pick the first ID and set as main ID
				// add the other IDs in a separate XML attribute
				if (geneId == null) geneId = "";
//				if (geneId.matches(".*[\\;\\,].*")) {
//					String[] allIds = geneId.split("\\s*[\\,\\;]\\s*");
//					geneId = allIds[0].trim();
//					if (allIds.length > 1) {
//						otherIds = allIds[1];
//						for (int a = 2; a < allIds.length; a++)
//							otherIds += ";" + allIds[a];
//					}
//				}
				String insert = "<" + xml_tag;
				if (geneId.length() > 0)
					insert += " " + xml_attribute_id + "=\"" + geneId + "\"";
				if (otherIds.length() > 0)
					insert += " " + xml_attribute_other_ids + "=\"" + otherIds + "\"";
				
				// get the Gene object for the (main) gene ID
				// and from that, get the official symbol (if known)
				// and add to XML attribute
				if (geneId != null && geneId.length() > 0) {
					Gene gene = run.getGene(geneId);
					String symbol = "";
					if (gene != null && gene.officialSymbol != null && gene.officialSymbol.length() > 0)
						symbol = gene.officialSymbol;
					if (symbol.matches("\\s*[\\;\\,]\\s*"))
						symbol = symbol.split("\\s*[\\,\\;]\\s*")[0].trim();
					if (symbol.length() > 0)
						insert += " " + xml_attribute_symbol + "=\"" + gene.officialSymbol + "\"";
					
					//float score = run.context.getConfidenceScore(gene, text);
					//System.err.println("Getting score for text " + text_id);
					float score = run.context.getConfidenceScore(gene, text.ID);
					if (score >= 0.0)
						insert +=  " " + xml_attribute_score + "=\"" + score + "\"";
				} else {
					insert +=  " " + xml_attribute_score + "=\"-1\"";
				}
				
				insert += ">";
				annotatedText = annotatedText.substring(0, se.start) + insert + annotatedText.substring(se.start);
			}
			
			//System.err.println("-----\nAnnotated text:\n-----\n"+annotatedText+"\n----------");

			// get the first sentence from the text, by finding the first sentence end mark
			// assumes it is the full title of the paper
			// TODO better to store the title separately, since some titles consist of multiple sentences
			String annotatedTitle = annotatedText.replaceFirst("^(.+?[\\.\\!\\?])\\s.*$", "$1");
			text.annotateXmlTitle(annotatedTitle);
			// assume the 2nd and following sentences are the abstract
			String annotatedAbstract = annotatedText.replaceFirst("^.+?[\\.\\!\\?]\\s(.*)$", "$1");
			text.annotateXmlAbstract(annotatedAbstract);
			//System.err.println("-----");
			//System.err.println(annotatedAbstract);
			//System.err.println("-----");
			text.buildJDocumentFromAnnotatedXml();

			// if the XML tag used to mark gene names has a prefix ("prefix:TAG"), we need to bind this prefix
			if (xml_tag.indexOf(":") > 0) {
				String prefix = xml_tag.replaceFirst("^(.+?)\\:.*$", "$1"); 
				text.addPrefixToXml(prefix);
				text.buildJDocumentFromAnnotatedXml();
			}
			
			String outfileName = text.getID() + ".annotated.xml";
			text.toXmlFile(outDir + "/" + outfileName);
			
			
			// or, if no gene was recognized in the current text:
			//} else {
			//	Text aText = run.getTextRepository().getText(text_id);
			//	String outfileName = aText.getID() + "_nogenesfound.annotated.xml";
			//	aText.toXmlFile(outDir + "/" + outfileName);
			//}
				
		}

	}
	
	
}


class SortableEntity implements Comparable<SortableEntity> {
	
	public RecognizedEntity entity;
	public int start;
	public int end;
	
	public SortableEntity (RecognizedEntity entity) {
		this.entity = entity;
		this.start  = entity.getBegin();
		this.end    = entity.getEnd();
	}
	
	@Override public int compareTo (SortableEntity o) {
		if (this.start != o.start) return this.start - o.start;
		if (this.end != o.end) return this.end - o.end;
		return 0;
	}
	
}