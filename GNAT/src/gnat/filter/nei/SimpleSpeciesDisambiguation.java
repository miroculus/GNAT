package gnat.filter.nei;

import java.util.Iterator;
import java.util.Set;

import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.IdentificationStatus;
import gnat.representation.RecognizedEntity;
import gnat.representation.Text;
import gnat.representation.TextAnnotation;
import gnat.representation.TextRepository;

/**
 * A naive implementation of species disambiguation for gene names.<br><br>
 * For each gene in a text, checks whether it occurs together with a species mention in the same sentence. 
 * Discards all gene candidates that refer to species not occurring in the same sentence anywhere.
 * 
 * 
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 */
public class SimpleSpeciesDisambiguation implements Filter {

	private String speciesRegex = "";
	
	/**
	 * 
	 */
	public SimpleSpeciesDisambiguation (String speciesRegex) {
		
	}
	
	
	@Override
	public void filter(Context context, TextRepository textRepository, GeneRepository geneRepository) {
		if (speciesRegex == null || speciesRegex.length() == 0) {
			System.err.println("#SimpleSpeciesDisambiguation: regular expression for species not set. Skipping this filter.");
			return;
		}

		
		Iterator<RecognizedEntity> unidentifiedGeneNames = context.getUnidentifiedEntities().iterator();
		while (unidentifiedGeneNames.hasNext()) {
			RecognizedEntity recognizedGeneName = unidentifiedGeneNames.next();
			String geneMention = recognizedGeneName.getName();
			
			// get the status of this gene mention: leads to the list of remaining candidates
			IdentificationStatus identificationStatus = context.getIdentificationStatus(recognizedGeneName);
			
			// get all candidate IDs for this gene mention
			Set<String> candidates = identificationStatus.getIdCandidates();
			
			// the text in which this gene name occurs
			Text text = recognizedGeneName.getText();
			
			TextAnnotation annotation = recognizedGeneName.getAnnotation();
			String sentence = text.getSentenceAround(annotation.getTextRange().getBegin());
			
			System.out.println("'" + geneMention + "' appears in ''" + sentence + "''");
			
		}
	}
	
	
	public static String SYNONYM_REGEX_HUMAN =
			"human|[Hh](?:\\.|omo)?\\ssap(?:\\.iens)?" + // names of the species
			"|HeLa"; // names of cell lines originating from that species
	
	public static String SYNONYM_REGEX_MOUSE =
		"mouse|[Mm](?:\\.|us)?\\smus(?:\\.culus)?|murine|mice" + // names of the species
		""; // names of cell lines originating from that species
	
	
}
