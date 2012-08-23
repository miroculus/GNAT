package gnat.filter.nei;

import gnat.ConstantsNei;
import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.RecognizedEntity;
import gnat.representation.Text;
import gnat.representation.TextAnnotation;
import gnat.representation.TextRange;
import gnat.representation.TextRepository;
import gnat.utils.StringHelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * Filters names by their immediate context (sentence). Removes names where the
 * surrounding text points to non-gene keywords, keeps diseases when also a locus
 * is mentioned, etc.
 * <br>
 * <br>
 * Examples:
 * <ul>
 * <li>myotonic dystrophy (DM) -- kick
 * <li>new locus for myotonic dystrophy (DM) -- keep
 * <li>linkage group on chromosome 19, including now the loci for apoE, Le, C3, LW, Lu, Se, H, PEPD, myotonic dystrophy (DM), neurofibromatosis (NF) and familial hypercholesterolemia (FHC) -- keep all recognized gene names (Lu, Se, DM, NF, ..)
 * <li>
 * <li>
 * </ul>
 */

public class ImmediateContextFilter implements Filter {

	public static String leftWordBoundary = "(^|[\\s\\(\\[\\/])";
	public static String rightWordBoundary = "([\\s\\,\\)\\]\\/\\;\\!\\?]|$)";


	/**
	 *
	 */
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {
		Iterator<RecognizedEntity> unidentifiedGeneNames = context.getUnidentifiedEntities().iterator();
		while (unidentifiedGeneNames.hasNext()) {
			RecognizedEntity recognizedGeneName = unidentifiedGeneNames.next();
			Text text = recognizedGeneName.getText();
			TextAnnotation annotation = recognizedGeneName.getAnnotation();
			String sentence = text.getSentenceAround(annotation.getTextRange().getBegin());
			String name = recognizedGeneName.getName();

			// first, check if we have a name that resembles a disease name
			// => could be a reference to the locus of a gene that's involved in that disease!
//			if (isDiseaseName(name)) {
//				// reference to a locus => keep
//				if (//sentence.matches(".*[\\s\\(](locus|loci|location|chromosom[a-z]+|gene.+associated)[\\s\\,\\)].*")
//					//|| sentence.matches(".*[\\s\\(]" + name + "[\\s\\,\\)].*[\\s\\(](gene|protein)s?[\\s\\,\\)].*")) {
//					sentence.matches(".*" + leftWordBoundary + "(locus|loci|location|chromosom[a-z]+|gene.+associated)" + rightWordBoundary + ".*")
//					|| sentence.matches(".*" + leftWordBoundary + name + rightWordBoundary + ".*" + leftWordBoundary + "(gene|protein)s?" + rightWordBoundary + ".*")) {
//					// keep name
//				// no such reference => kicks
//				} else {
//					if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.SVM) >= 0)
//						System.out.println("#ICF: Kicking dis. " + recognizedGeneName.getName() + " in " + text.PMID +
//								", \"" + sentence + "\"");
//					context.removeEntitiesNamesHavingName(recognizedGeneName.getName(), text);
//				}
//				continue;
//			}

			if (UnspecificNameFilter.isCellLine(name, sentence)) {
				if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
					System.out.println("#ICF: Kicking cell " + recognizedGeneName.getName() + " in " + text.PMID +
							", \"" + sentence + "\"");
				context.removeEntitiesHavingName(name, text);
				continue;
			}

			// next, check for unspecific names
			String maskedName = StringHelper.espaceString(name);
			if ( (UnspecificNameFilter.isUnspecific(name)
					|| UnspecificNameFilter.isUnspecificSingleWordCaseInsensitive(name)
					|| UnspecificNameFilter.isUnspecificSingleWord(name)
					|| UnspecificNameFilter.isTissueCellCompartment(name)
					|| UnspecificNameFilter.isUnspecificAbbreviation(name, sentence)
					|| UnspecificNameFilter.isSpecies(name)
					|| UnspecificNameFilter.isAminoAcid(name)
					|| UnspecificNameFilter.isDiseaseName(name)
					//|| sentence.matches(".*" + maskedName + "( (gene|protein))? family.*")
					|| sentence.matches(".*" + maskedName + " homology(\\s.{0,7})? domain.*")
					) //&& !sentence.matches(".*" + maskedName + "\\)\\s(gene|protein|mRNA|cDNA|isoform|isozym).*")
					) {

				if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
					System.out.println("#ICF: Kicking uns. " + recognizedGeneName.getName() + " in " + text.PMID +
							", \"" + sentence + "\"");
				//context.removeEntitiesHavingName(name, text);
				context.removeRecognizedEntity(recognizedGeneName);
				continue;
			}
		}


		// next, check if the name is an abbreviation that is explained in the same sentence
		Map<RecognizedEntity, TextRange> geneNameToTextRange = new HashMap<RecognizedEntity, TextRange>(); // for mapping gene names to text ranges
		Map<RecognizedEntity, TextRange> geneNameToLongFormRange = new HashMap<RecognizedEntity, TextRange>(); // for mapping shortforms to longforms
		unidentifiedGeneNames = context.getUnidentifiedEntities().iterator();
		while (unidentifiedGeneNames.hasNext()) {
			RecognizedEntity recognizedGeneName = unidentifiedGeneNames.next();
			String geneName = recognizedGeneName.getName();
			TextAnnotation textAnnotation = recognizedGeneName.getAnnotation();
			Text text = recognizedGeneName.getText();

			if(textAnnotation.getTextRange().getBegin()-1>=0 && textAnnotation.getTextRange().getEnd()+1<text.length()){
				char leftChar = text.getCharAt(textAnnotation.getTextRange().getBegin()-1);
				char rightChar = text.getCharAt(textAnnotation.getTextRange().getEnd()+1);
				if(leftChar=='(' && rightChar == ')' && geneName.matches("[A-Za-z0-9\\-]+")){
					// name in brackets;
					String bestLongForm = AbbreviationFilter.findBestLongForm(geneName, text.getPlainText(), textAnnotation.getTextRange().getBegin()-1);
					if(bestLongForm!=null){
						// we have a long form
						int longFormBeginIndex = textAnnotation.getTextRange().getBegin() - 2 - bestLongForm.length();

						if(longFormBeginIndex<0){
							// happens if no whitespace between longform and open bracket of short form (see e.g. TAO in PMID 11031959)
							longFormBeginIndex = 0;
						}

						int longFormEndIndex = longFormBeginIndex + bestLongForm.length() - 1;
						geneNameToLongFormRange.put(recognizedGeneName, new TextRange(longFormBeginIndex, longFormEndIndex));
					}
				}
			}
			// store text ranges of all gene names
			geneNameToTextRange.put(recognizedGeneName, new TextRange(textAnnotation.getTextRange().getBegin(), textAnnotation.getTextRange().getEnd()));
		}

		// for all short forms -> long form mappings ...
		for (RecognizedEntity shortForm : geneNameToLongFormRange.keySet()) {
			TextRange longFormRange = geneNameToLongFormRange.get(shortForm);

			String longFormName = "";
			try{
				longFormName = shortForm.getText().getPlainText().substring(longFormRange.getBegin(), longFormRange.getEnd()+1).trim();
			}catch(StringIndexOutOfBoundsException e){
				e.printStackTrace();
				System.err.println("shortForm='"+shortForm.getName()+"' sf beginIndex="+shortForm.getBegin()+" sf endIndex="+shortForm.getEnd()+", PMID="+shortForm.getText().getID());
				System.err.println("lf beginIndex="+longFormRange.getBegin()+" lf endIndex="+longFormRange.getEnd()+", PMID="+shortForm.getText().getID());
				continue;
			}
			//System.out.println(this.getClass().getSimpleName()+": short form="+shortFormName.getName()+", long form="+longFormName);
			String shortFormPMID = shortForm.getText().getID();

			boolean keepShortFormName = false;

			for (RecognizedEntity geneName : geneNameToTextRange.keySet()) {
				if(geneName.getText().getID().equals(shortFormPMID)){ // gene name in same text as short form name?
					TextRange geneNameRange = geneNameToTextRange.get(geneName);

					if(longFormRange.equals(geneNameRange)){
						keepShortFormName = true;
						Set<String> longFormIds = context.getIdCandidates(geneName);
						List<RecognizedEntity> shortFormEntities = context.getRecognizedEntitiesHavingNameInText(shortForm.getName(), shortForm.getText());
						for (RecognizedEntity entity : shortFormEntities) {
							context.getIdentificationStatus(entity).setIdCandidates(longFormIds); // long forms are probably less ambiguous than short forms
                        }
						break;
					}

					else if(longFormRange.contains(geneNameRange)){
						keepShortFormName = true;
						break;
					}
				}
			}

			if(!keepShortFormName){
				// check if long form, at least, contains one of the following keywords
				String sentence = shortForm.getText().getSentenceAround(shortForm.getAnnotation().getTextRange().getBegin());
				String lfCopy = StringHelper.espaceString(longFormName);
				String sfCopy = StringHelper.espaceString(shortForm.getName());
				String lowerCaseLongForm = longFormName.toLowerCase();
				if(lowerCaseLongForm.contains("protein")
								|| lowerCaseLongForm.contains("gene")
								|| lowerCaseLongForm.contains("receptor")
								|| lowerCaseLongForm.contains("antigen")
								|| lowerCaseLongForm.contains("helicase")
								|| lowerCaseLongForm.contains("kinase")
								|| lowerCaseLongForm.endsWith("ase")
								|| lowerCaseLongForm.endsWith("transporter")
								|| lowerCaseLongForm.contains("factor")
								|| lowerCaseLongForm.contains("subunit")
								|| lowerCaseLongForm.contains("region")
//								|| ( (isDiseaseName(longFormName) || isDiseaseName(shortForm.getName()))
//									 && (keepDiseaseName(longFormName, sentence) || keepDiseaseName(shortForm.getName(), sentence) )
//								   )
								|| sentence.matches(".*" + sfCopy + "\\)\\s(gene|protein|mRNA|cDNA|isoform|isozym).*")
								|| sentence.matches(".*(gene|protein|mRNA|cDNA)s?.*" + lfCopy + ".*" + shortForm.toString().replaceAll("([\\+\\-\\*\\(\\)\\[\\]\\{\\}])", "\\\\$1") + ".*")

					)
				{
					keepShortFormName = true;
				}
			}


			if(!keepShortFormName){
				//if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
				//	System.out.println("#ICF: Kicking sfn. " + shortForm.getName() + " in " + shortFormPMID +
				//			", \"" + sentence + "\"");
				context.removeEntitiesHavingName(shortForm.getName(), shortForm.getText());
			}

		}
	}

	
}
