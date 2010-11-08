package gnat.client;

import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.IdentifiedGene;
import gnat.representation.RecognizedEntity;
import gnat.representation.Text;
import gnat.representation.TextRepository;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a single run of {@link Filter}s on a {@link TextRepository}.
 * <br><br>
 * Each Run needs a {@link TextRepository} and a list of {@link Filter}s:<br>
 * - the TextRepository can be set using {@link #setTextRepository(TextRepository)}
 * or created text-by-text using {@link #addText(Text)}.<br>
 * - the filters can be added using {@link #addFilter(Filter)}.
 * <br><br>
 * A Context will be constructed that contains all the information about recognized genes ({@link RecognizedGene}),
 * and their {@link IdentificationStatus}, that is, whether they have been assigned a single identifier or
 * still have multiple candidates as potential solution.
 * <br><br>
 * One of the {@link Filter}s has to load a {@link GeneRepository}, before final identification can happen.
 * A typical filtering pipeline has steps for:<br>
 *  - named entity recognition (genes, species) with assignment of candidate IDs;<br>
 *  - loading of a gene repository that has information on each candidate;<br>
 *  - verification and direct identification of genes using external sources (Gene2PubMed, UniProt References, etc.);<br>
 *  - filtering of false positives: non-gene names, word sense disambiguation;<br>
 *  - filtering of false positives: exclusion of individual genes as not relevant to a text;<br>
 *  - disambiguation/ranking of remaining candidates;<br>
 * usually in that order.
 * 
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 */
public class Run {

	/** */
	public Context context;
	
	/** Stores the collection of {@link Text}s that have to be processed. */
	private TextRepository textRepository;
	
	/** Stores information on each gene candidate. */
	private GeneRepository geneRepository;
	
	/** Contains the filtering steps that will be invoked on the texts, typically in the order:<br>
	 *  - named entity recognition (genes, species) with assignment of candidate IDs;<br>
	 *  - loading of gene repository that has information on each candidate;<br>
	 *  - filtering of false positives (non-gene names);<br>
	 *  - disambiguation of remaining candidates. */
	private List<Filter> filterPipeline;
	
	/** Verbosity; 0: no output; 3: full status report after each filter (recognized genes, identified genes, ..)*/
	public int verbosity = 0;
	
	
	/**
	 * Constructs a new Run.
	 */
	public Run () {
		context = new Context();
		textRepository = new TextRepository();
		geneRepository = new GeneRepository();
		filterPipeline = new LinkedList<Filter>();
	}
	
	
	/**
	 * Sets an entire {@link TextRepository} for this Run.
	 * @param textRepository
	 */
	public void setTextRepository (TextRepository textRepository) {
		this.textRepository = textRepository;
	}
	
	
	/**
	 * Assume that all file names in the textRepository are based on PubMed IDs: they have the form
	 * "<tt>some/path/id.txt</tt>", where path and extension could be missing, but
	 * the <tt>id</tt> needs to be numeric. For each such text, sets the {@link Text#PMID} field
	 * to <tt>id</tt>.
	 * @param textRepository
	 */
	public void setFilenamesAsPubMedId () {
		for (Text text: textRepository.getTexts()) {
			String id = text.getID();
			if (id.matches("(.*\\/)?(\\d+)(\\.txt)?")) {
				int pmid = Integer.parseInt(id.replaceFirst("(.*\\/)?(\\d+)(\\.txt)?", "$2"));
				text.setPMID(pmid);
			}
				
		}
	}
	
	
	/**
	 * Adds a {@link Text} to the current {@link #textRepository}.
	 * @param text
	 */
	public void addText (Text text) {
		this.textRepository.addText(text);
	}
	
	
	/**
	 * Adds a {@link Filter} to the processing pipeline, which will be invoked
	 * on {@link #context}, {@link #textRepository}, and {@link #geneRepository}.
	 * @param filter
	 */
	public void addFilter (Filter filter) {
		filterPipeline.add(filter);
	}
	
	
	/**
	 * Log the current process of NER and disambiguation into the given stream.<br>
	 * For each level of identification (Recognized, Identified, Unidentified), prints
	 * a list of all genes and current candidate IDs, if any, per PubMed ID, together
	 * with the recognized gene name and its position.
	 * @param context
	 */
	void printCurrentStatus (PrintStream out) {
		out.append("Status:\n");
		//out.append("TextRepository has " + textRepository.size() + " texts.\n");
		//out.append("GeneRepository has " + geneRepository.size() + " genes.\n");
		out.append("RecognizedEntities:\n");
		for (RecognizedEntity re: context.getRecognizedEntities()) {
			out.append("R\t" + re.getText().getPMID()+"\t" + re.getName()
					+ "\t" + re.getBegin() + "-" + re.getEnd()
					+ "\t" + context.getIdentificationStatus(re).getIdCandidates()
					+ "\n");
		}
		out.append("IdentifiedGenes:\n");
		Iterator<IdentifiedGene> it = context.getIdentifiedGenesIterator();
		while (it.hasNext()) {
			IdentifiedGene gene = it.next();
			out.append("I\t" + gene.getRecognizedEntity().getText().PMID
					+ "\t" + gene.getName()
					+ "\t" + gene.getRecognizedEntity().getBegin() + "-" + gene.getRecognizedEntity().getEnd()
					+ "\t" + gene.getGene().getID()
					+ "\t" + gene.getGene().getTaxon()
					+ "\n");
		}
		out.append("UnidentifiedGenes:\n");
		for (RecognizedEntity re: context.getUnidentifiedEntities()) {
			out.append("U\t" + re.getText().getPMID()+"\t" + re.getName()
					+ "\t" + re.getBegin() + "-" + re.getEnd()
					+ "\t" + context.getIdentificationStatus(re).getIdCandidates()
					+ "\n");
		}
		out.append("--------------------\n");
	}
	
	
	/**
	 * Runs all filters in the order given by <tt>filterPipeline</tt>,
	 * working on and changing <tt>context</tt>, <tt>textRepository</tt>, and <tt>geneRepository</tt>.
	 * <br><br>
	 * A status is printed to STDOUT after each filter if {@link #verbosity} &gt; 2, using {@link #printCurrentStatus(PrintStream)).
	 */
	public void runFilters () {
		long starttime = System.currentTimeMillis();
		
		for (Filter filter: filterPipeline) {
			if (verbosity > 1)
				System.out.println("Running filter " + filter.getClass());
			
			filter.filter(context, textRepository, geneRepository);
			
			if (verbosity > 2)
				printCurrentStatus(System.out);
			
			if (verbosity > 3) {
				System.out.println("TextRepository has " + textRepository.size() + " texts.");
				System.out.println("GeneRepository has " + geneRepository.size() + " genes.");
			}
		}
		
		if (verbosity > 0) {
			long endtime = System.currentTimeMillis();
			float seconds = (float)(endtime - starttime) / 1000.0f;
			System.out.println("Run finished in " + seconds + "sec. ");
		}
	}
}
