package gnat.client;

import gnat.ConstantsNei;
import gnat.ISGNProperties;
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
import gnat.filter.ner.RunAllGeneDictionaries;
import gnat.preprocessing.NameRangeExpander;
import gnat.representation.Gene;
import gnat.representation.IdentificationStatus;
import gnat.representation.RecognizedEntity;
import gnat.representation.Text;
import gnat.representation.TextAnnotation;
import gnat.representation.TextFactory;
import gnat.utils.Sorting;
import gnat.utils.StringHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * Another simple demo client that takes a directory as input, reads all XML files (Medline/PubMed XML format)
 * and return one tab-separated output file with annotations: PubMed ID, mention start/end, gene ID, gene mention, score, taxonomy ID.
 * <pre>
 * Supported file formats:
   - Medline XML (MedlineCitation and MedlineCitationSet),
   - PubMed XML (PubmedArticle and PubmedArticleSet),
   - GZipped Medline/Pubmed XML files like 'medline12n0123.xml.gz'
   As a convention, Medline/Pubmed XML files have to be named as such:
   - *.medline.xml   -  single MedlineCitation/PubmedArticle in XML,
   - *.medlines.xml  -  MedlineCitationSet/PubmedArticleSet,
   - medline[year]n[number].xml(.gz)  -  MedlineCitationSet,
 * </pre>
 * Assumes that the gene repository is held in a local database (specified in isgn_properties.xml)
 * and that dictionary servers for human genes and GeneOntology terms are running,
 * on ports specified in config/taxonToServerPorts, on the server specified under 'dictionaryServer'  
 * in isgn_properties.xml.
 * <br><br>
 * TODO Note: in MedlineCitationSets, currently loses all entries in DeleteCitation elements
 * 
 * @author Joerg Hakenberg
 */
public class AnnotateMedline_TsvOutput {
	
	/** Name of the XML element tag that will be used to annotate genes found by GNAT. */
	public static String xml_tag = "GNAT";
	/** XML tag for a gene mention that has no ID. */
	public static String xml_tag_mention = "GNATGM";
	/** Name of the XML attribute for the {@link #xml_tag} element, which will contain the gene ID(s). */
	public static String xml_attribute_id     = "id";
	/** Name of the XML attribute for the {@link #xml_tag} element, which will contain the gene symbols(s), also known as primary terms. */
	public static String xml_attribute_symbol = "pt";
	/** Name of the XML attribute for the {@link #xml_tag} element, which will contain the disambiguation score, or multiple
	 *  semi-colon-separated scores in case multiple candidates are left. */
	public static String xml_attribute_score = "score";
	/** */
	public static String xml_attribute_other_ids = "otherIds";
	/** */
	public static String xml_attribute_candidate_ids = "candidateIds";

	/** Collection of (PubMed or other) IDs not to parse (again). */
	public static Set<String> blacklist = new HashSet<String>();
	/** */
	public static Set<String> whitelist = new HashSet<String>();

	
	/**
	 * 
	 * @param args
	 */
	public static void main (String[] args) {
		
		int verbosity = 0;

		// check for command line parameters
		if (args.length == 0 || args[0].matches("\\-\\-?h(elp)?")) {
			System.out.println("AnnotateMedline (TSV output) -- annotates genes in a Medline citation set\n" +
					           "Supported file formats:\n" +
					           "- Medline XML (MedlineCitation in a MedlineCitationSet),\n" +
					           "- PubMed XML (PubmedArticle in a PubmedArticleSet),\n" +
					           "- GZipped Medline/Pubmed XML files like 'medline12n0123.xml.gz'\n" +
					           "As a convention, Medline/Pubmed XML files have to be named as such:\n" +
					           "- medline<year>n<number>.xml(.gz)");
			System.out.println("Call: AnnotateMedline <dir>");
			System.out.println(" <dir>     -  directory with one or more .xml or .xml.gz files");
			System.out.println("Optional parameters:");
			System.out.println(" -g        -  Print only those texts to the output that have a gene");
			System.out.println(" -v        -  Set verbosity level for progress and debugging information");
			System.out.println("              Default: 0; warnings: 1, status: 2, ... debug: 6");
			System.out.println(" --outfile -  File to write the output list; default is STDOUT");
			System.out.println(" --black <file>  -  Do not parse PubMed articles listed in this file (PMIDs); blacklist has precedence over whitelist");
			System.out.println(" --white <file>  -  Parse only the PubMed articles listed in this file; blacklist has precedence over whitelist");
			System.exit(1);
		}
		
		// parse and store command line parameters
		String dir = "";        // directory to read from
		String outDir = ".";    // 
		boolean skipNoGeneAbstracts = false;
		String blacklistfile = "";
		String whitelistfile = "";
		for (int a = 0; a < args.length; a++) {
			// parameter is -v to regulate verbosity at runtime
			if (args[a].matches("\\-v=\\d+"))
				verbosity = Integer.parseInt(args[a].replaceFirst("^\\-v=(\\d+)$", "$1"));
			else if (args[a].toLowerCase().matches("\\-\\-?outfile")) 
				outDir = args[++a];
			else if (args[a].toLowerCase().matches("\\-\\-?outfile\\=.+")) 
				outDir = args[a].replaceFirst("^\\-\\-?[Oo][Uu][Tt][Ff][Ii][Ll][Ee]\\=", "");
			else if (args[a].toLowerCase().equals("-g")) 
				skipNoGeneAbstracts = true;
			else if (args[a].toLowerCase().matches("\\-\\-?b(lack(list)?)?"))  
				blacklistfile = args[++a];
			else if (args[a].toLowerCase().matches("\\-\\-?w(hite(list)?)?"))  
				whitelistfile = args[++a];
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
			if (!DIR.exists()) // || !DIR.isDirectory()) 
				{
				//System.out.println("Please specify a valid input directory!");
				System.exit(1);
			}
		}

		// create the output directory if it does not exist
		if (!outDir.equals(".")) {
			File DIR = new File(outDir);
			if (!DIR.exists())
				DIR.mkdirs();
		}
		
		//
		if (blacklistfile.length() > 0) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(blacklistfile));
				String line = "";
				while ((line = br.readLine()) != null) {
					if (line.matches("\\d+"))
						blacklist.add(line);
				}
				br.close();
			} catch (IOException e) {
				System.err.println("#ERROR reading black list file: " + blacklistfile);
				System.err.println("#ERROR " + e.getMessage());
				System.exit(2);
			}
			System.err.println("#INFO blacklisted " + blacklist.size() + " PubMed IDs");
		}
		
		//
		if (whitelistfile.length() > 0) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(whitelistfile));
				String line = "";
				while ((line = br.readLine()) != null) {
					if (line.matches("\\d+"))
						whitelist.add(line);
				}
				br.close();
			} catch (IOException e) {
				System.err.println("#ERROR reading white list file: " + whitelistfile);
				System.err.println("#ERROR " + e.getMessage());
				System.exit(2);
			}
			System.err.println("#INFO whitelisted " + whitelist.size() + " PubMed IDs");
		}
		
		//
		ConstantsNei.setOutputLevel(verbosity);

		// check the input directory for all valid files
		File DIR = new File(dir);
		String[] filelist_p = {};
		List<String> filelist = new LinkedList<String>();
		// if 'dir' indeed points to a directory:
		if (DIR.isDirectory()) {
			filelist_p = DIR.list();
			for (String filename: filelist_p) {
				if (filename.matches("medline\\d+n\\d+\\.xml(\\.gz)?") || filename.matches("outfile\\.\\d+\\.xml(\\.gz)?"))
					filelist.add(filename);
			}
		// if 'dir' seems to be a single file:
		} else {
			String filename = dir.replaceFirst("^(.+)?\\/(.+?)$", "$2"); // get file name part
			dir = dir.replaceFirst("^(.+)?\\/(.+?)$", "$1");             // get new directory name part
			if (filename.matches("medline\\d+n\\d+\\.xml(\\.gz)?") || filename.matches("outfile\\.\\d+\\.xml(\\.gz)?"))
				filelist.add(filename);
		}
		if (filelist.size() == 0) {
			System.err.println("Error: found no files matching the naming convention medline12n123.xml or .xml.gz");
			System.exit(1);
		}

		// loop through all XML files, creating a Run each and process them individually
		int c_file = 0;
		long starttime = System.currentTimeMillis();
		for (String filename: filelist) {
			c_file++;
			if (c_file == 1)
				System.err.println("#INFO annotating " + filename + " (" + c_file + " out of " + filelist.size() + ")");
			else {
				long delta = System.currentTimeMillis() - starttime;
				//float msec_per_file  = (float)delta / (float)c_file;
				//float msec_all_files = filelist.size() * msec_per_file;
				//float min_all_files  = msec_all_files / 1000f / 60f;
				
				String time = String.format("%d min %02d sec", 
					    TimeUnit.MILLISECONDS.toMinutes(delta),
					    TimeUnit.MILLISECONDS.toSeconds(delta) - 
					    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(delta))
					);
				//System.err.println("#INFO annotating " + filename + " (" + c_file + " out of " + filelist.size() +
				//	", ETA " + min_all_files + "min");
				System.err.println("#INFO annotating " + filename + " (" + c_file + " out of " + filelist.size() +
					", ETA " + time + " (based on previously annotated file)");
			}
			
			// each pipeline is handled by a "Run"
			Run run = new Run();
			run.verbosity = verbosity;
			
			//////////
			// INPUT
			TextFactory.setBlacklist(blacklist);
			TextFactory.setWhitelist(whitelist);
			run.setTextRepository(TextFactory.loadTextRepositoryFromMedlineFile(dir + "/" + filename));
			System.err.println("#INFO done loading text repository: " + run.getTextRepository().size() + " texts");
			
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
			afewDictionaryFilters.setLimitToTaxons(9606, 10090, 10116, 559292, 7227);
			run.addFilter(afewDictionaryFilters);
	
			//////////
			// NER post-processing filters here:
			run.addFilter(new RecognizedEntityUnifier());
	
			// include a few disambiguation filters that do not need specific information on each candidate gene
			// thus, these work on the gene's name and its context in the text
			run.addFilter(new ImmediateContextFilter());
			
			// strictFPs_2_2_context_all.object contains data on the context defined by two tokens left and two tokens right of a gene name
			//run.addFilter(new LeftRightContextFilter("data/strictFPs_2_2_context_all.object", "data/nonStrictFPs_2_2_context_all.object", 0d, 2, 2));
	
			//
			run.addFilter(new ImmediateContextFilter());
			
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
			//run.addFilter(new AlignmentFilter(AlignmentHelper.globalAlignment, 0.7f));
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
			if (ConstantsNei.verbosityAtLeast(ConstantsNei.OUTPUT_LEVELS.STATUS))
				System.err.println("#Writing output file(s)...");


			// For texts belonging to a document set (one XML document with multiple texts),
			// store the annotated XML in a buffer before writing the actual XML set file(s)
			// Input can come from multiple such document set files, therefore the two maps, which also
			// store the file type (mostly to distinguish medline xml vs plain xml)
			Map<String, StringBuilder> file2buffer  = new HashMap<String, StringBuilder>();
			Map<String, Text.SourceTypes> file2type = new HashMap<String, Text.SourceTypes>();

			// loop through all texts, generate the annotated XML
			// and write the new content to file(s)
			Collection<Text> texts = run.getTextRepository().getTexts();
			// TODO the output is currently not in the order of input!
			// especially for PubmedArticleSet XML files it might be better to retain the input order
			// (within one XML file)
			for (Text text: texts) {

				// sort entities by position within each text, insert into the text from back to end
				List<RecognizedEntity> entitiesBackwards = new LinkedList<RecognizedEntity>();
				entitiesBackwards.addAll(run.context.getRecognizedEntitiesInText(text));
				Collections.sort(entitiesBackwards, new Sorting.RecognizedEntitySorter());
				Collections.reverse(entitiesBackwards);
				
				// do not print abstracts that don't have genes (command line parameter)
				if (skipNoGeneAbstracts && entitiesBackwards.size() == 0)
					continue;

				// start with the plain text ...
				String annotatedText = text.plainText;
				// ... and insert markup for all recognized entities
				for (RecognizedEntity se: entitiesBackwards) {
					TextAnnotation ta = se.getAnnotation();
					ta.setType(TextAnnotation.Type.GENE);
					//System.err.print(text.getID()
					//		+ "\t" + se.entity.getBegin() + "\t" + se.entity.getEnd()
					//		+ "\t" + se.entity.getName() + "\t" + ta.getType().toString());
					IdentificationStatus idStatus = run.context.getIdentificationStatus(se);
					String geneId = idStatus.getId();
					//System.err.println("\t" + geneId + "\t" + idStatus.getIdCandidates());
					Set<String> otherIds_set = new TreeSet<String>();
					otherIds_set.addAll(idStatus.getIdCandidates());
					if (otherIds_set != null && otherIds_set.size() > 0 && geneId != null)
						otherIds_set.remove(geneId);
					String otherIds = "";
					if (otherIds_set.size() > 0)
						otherIds = StringHelper.joinStringSet(otherIds_set, ";");

					String tag = xml_tag;
					String symbol = "N/A";
					String sscore = "N/A";
					String taxId = "N/A";
					if (geneId == null || geneId.length() == 0) {
						tag = xml_tag_mention;
					} else {
						Gene gene = run.getGene(geneId);
						if (gene != null && gene.officialSymbol != null && gene.officialSymbol.length() > 0)
							symbol = gene.officialSymbol;
						if (symbol.matches("\\s*[\\;\\,]\\s*"))
							symbol = symbol.split("\\s*[\\,\\;]\\s*")[0].trim();
						if (symbol.length() > 0)
							symbol = gene.officialSymbol;
						float score = run.context.getConfidenceScore(gene, text.ID);
						if (score >= 0.0)
							sscore = score + "";
						taxId = gene.getTaxon() + "";
					}
					String candidateIds = "N/A";
					if (otherIds.length() > 0)
						candidateIds = otherIds;
					String mention = se.getName();
					
					
					System.out.println(text.getPMID() + "\t" + se.getBegin() + "\t" + se.getEnd() + "\t" + mention + "\t" + tag + "\t" + candidateIds + "\t" + geneId + "\t" + symbol + "\t" + taxId + "\t" + sscore);
					
				}

				//System.err.println("#Annotated text:\n"+annotatedText+"\n----------");

				//if (entitiesBackwards.size() == 0) {
				//	if (ConstantsNei.verbosityAtLeast(ConstantsNei.OUTPUT_LEVELS.WARNINGS))
				//		ConstantsNei.OUT.println("Found no genes in text " + text.getPMID());
				//}

				if (text.sourceType == Text.SourceTypes.PLAIN) {
					//System.err.println("#text="+text.getID() + ", source="+text.sourceType.toString());
					annotatedText = "<text id=\"" + text.getID() + "\">\n" + annotatedText + "\n</text>";
					text.annotatedXml = annotatedText;

				} else if (text.sourceType == Text.SourceTypes.MEDLINE_XML || text.sourceType == Text.SourceTypes.MEDLINES_XML ) {
					// get the first sentence from the text, by finding the first sentence end mark
					// assumes it is the full title of the paper
					// TODO better to store (and annotate!) the title separately, since some titles consist of multiple sentences
					String annotatedTitle = annotatedText.replaceFirst("^(.+?[\\.\\!\\?])\\s.*$", "$1");
					text.annotateXmlTitle(annotatedTitle);

					// assume the 2nd and following sentences are the abstract
					String annotatedAbstract = annotatedText.replaceFirst("^.+?[\\.\\!\\?]\\s(.*)$", "$1");
					text.annotateXmlAbstract(annotatedAbstract);
				}

				// if the XML tag used to mark gene names has a prefix ("prefix:TAG"), we need to bind this prefix
				if (xml_tag.indexOf(":") > 0) {
					String prefix = xml_tag.replaceFirst("^(.+?)\\:.*$", "$1"); 
					text.addPrefixToXml(prefix);
				}

				//
				text.buildJDocumentFromAnnotatedXml();

				// write annotated text to file:
				// either into one or more document sets (file(s) with more than one text) ...
				if (text.sourceType == Text.SourceTypes.MEDLINES_XML) {
					String basefilename = text.filename.replaceFirst("^(.*)\\/([^\\/]+?)$", "$2");
					if (basefilename.endsWith(".xml.gz"))
						basefilename = basefilename.replaceFirst("\\.xml\\.gz$", ".annotated.xml");
					else if (basefilename.endsWith(".xml"))
						basefilename = basefilename.replaceFirst("\\.xml$", ".annotated.xml");
					else
						basefilename = basefilename.replaceFirst(".medline", ".annotated.medline");

					String x = text.toXmlString();
					// texts that are part of a collection within one file get stored
					// in a buffer for that file; we're storing this buffer in memory
					// and write it to disk, together with appropriate XML root elements,
					// once all texts were processed here
					if (file2buffer.containsKey(basefilename)) {
						StringBuilder buf = file2buffer.get(basefilename);
						buf.append(x);
						file2buffer.put(basefilename, buf);
					} else {
						StringBuilder buf = new StringBuilder();
						buf.append(x);
						file2buffer.put(basefilename, buf);
						file2type.put(basefilename, Text.SourceTypes.MEDLINES_XML);
					}
					// ... or individually
				} else {
					// we write individual files directly to the disk:
					String outfileName = text.getID() + ".annotated.xml";
					if (outDir.length() > 0) text.toXmlFile(outfileName);
					else					 text.toXmlFile(outDir + "/" + outfileName);
				}

				// discard this text
				text = null;

			} // foreach text
			
			run.clearTextRepository();
			
			// haha
			System.gc();

		} // for each Medline XML file
		
	}
	
}


