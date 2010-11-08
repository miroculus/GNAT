package gnat.filter.nei;

import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.RecognizedEntity;
import gnat.representation.Text;
import gnat.representation.TextAnnotation;
import gnat.representation.TextRange;
import gnat.representation.TextRepository;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tries to find long forms for short forms of gene names in a text.
 * It then filters a gene name based on its long form, i.e., keeps it when the long form
 * is annotated too or at least contains some predefined keywords.
 *
 * @author Joerg
 */
public class AbbreviationFilter implements Filter {

	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {
		
		Map<RecognizedEntity, TextRange> geneNameToTextRange = new HashMap<RecognizedEntity, TextRange>(); // for mapping gene names to text ranges
		Map<RecognizedEntity, TextRange> geneNameToLongFormRange = new HashMap<RecognizedEntity, TextRange>(); // for mapping short forms to long forms

		// scan context for short forms
		Iterator<RecognizedEntity> unidentifiedGeneNames = context.getUnidentifiedEntities().iterator();
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
					String bestLongForm = findBestLongForm(geneName, text.getPlainText(), textAnnotation.getTextRange().getBegin()-1);
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
			geneNameToTextRange.put(recognizedGeneName, textAnnotation.getTextRange());
		}


		// any abbreviations found?
		if(geneNameToLongFormRange.size()==0)
			return;


		Map<String, Set<RecognizedEntity>> textToGeneNames = context.getRecognizedEntitiesAsMap();

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

			Set<RecognizedEntity> geneNamesInSameTextAsShortForm = textToGeneNames.get(shortFormPMID);

			for (RecognizedEntity geneName : geneNamesInSameTextAsShortForm) {
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

			if(!keepShortFormName){
				// check if long form, at least, contains one of the following keywords
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
								)
				{
					keepShortFormName = true;
				}
			}


			if(!keepShortFormName){
				context.removeEntitiesHavingName(shortForm.getName(), shortForm.getText());
				//System.out.println(this.getClass().getSimpleName()+": removing short form "+shortFormName.getName()+" in text "+shortFormName.getText().getID()+". Its long form ("+longFormName+") does not contain a gene name.");
			}

		}

    }



	/**
	 * Returns the best long form, or null if no long form was found.
	 * */
	public static String findBestLongForm(String shortForm, String text, int shortFormOpenBracketIndex) {
		// TODO should use existing methods from StringHelper instead of implementing new ones here!!!

		int shortFormLength = shortForm.length();
		if(shortForm.endsWith("s")){
			shortFormLength--;
		}
		if(shortForm.indexOf('-')!=-1){
			shortFormLength--;
		}

		List<String> possibleLongForm = extractLongFormCandidates(shortForm, text, shortFormOpenBracketIndex, shortFormLength * 3);

		StringBuffer longFormBuffer = new StringBuffer();
		String bestLongForm = null;
		for(int i=0;i<possibleLongForm.size();i++){
			String token = possibleLongForm.get(i);
			if(longFormBuffer.length()>0){
				longFormBuffer.insert(0, " ");
			}
			longFormBuffer.insert(0, token);
			String foundBestLongForm = findBestLongForm(shortForm, longFormBuffer.toString());
			if(foundBestLongForm!=null){
				bestLongForm = foundBestLongForm;
			}
		}

		return bestLongForm;
	}


	/**
	 * Starts at the opening bracket and collects all preceding tokens.
	 * */
	public static List<String> extractLongFormCandidates(String shortForm, String text, int shortFormOpenBracketIndex, int maxPrecedingTokens){

		List<String> longFormList = new LinkedList<String>();

		String precedingText = text.substring(0, shortFormOpenBracketIndex);
		String[] precedingTokens = precedingText.split(" ");
		for(int i=0; i<maxPrecedingTokens && precedingTokens.length-1-i>=0; i++){
			String token = precedingTokens[precedingTokens.length-1 - i];
			longFormList.add(token);
		}

		//Collections.reverse(longFormList);
		return longFormList;
	}

	/**
     * Algorithm from Schwartz and Hearst:
     * Method findBestLongForm takes as input a short-form and a long-form
     * candidate (a list of words) and returns the best long-form that matches
     * the short-form, or null if no match is found.
     */
    public static String findBestLongForm(String shortForm, String longForm) {
            int sIndex; // The index on the short form
            int lIndex; // The index on the long form
            char currChar; // The current character to match

            sIndex = shortForm.length() - 1; // Set sIndex at the end of the
            // short form
            lIndex = longForm.length() - 1; // Set lIndex at the end of the
            // long form

            int firstLIndex = -1;

            for (; sIndex >= 0; sIndex--) { // Scan the short form starting
            // from end to start
                    // Store the next character to match. Ignore case
                    currChar = Character.toLowerCase(shortForm.charAt(sIndex));
                    // ignore non alphanumeric characters
                    if (!Character.isLetterOrDigit(currChar))
                            continue;
                    // Decrease lIndex while current character in the long form
                    // does not match the current character in the short form.
                    // If the current character is the first character in the
                    // short form, decrement lIndex until a matching character
                    // is found at the beginning of a word in the long form.
                    while (((lIndex >= 0) && (Character.toLowerCase(longForm
                                    .charAt(lIndex)) != currChar))
                                    || ((sIndex == 0) && (lIndex > 0) && (Character
                                                    .isLetterOrDigit(longForm.charAt(lIndex - 1)))))
                            lIndex--;
                    // If no match was found in the long form for the current
                    // character, return null (no match).
                    if (lIndex < 0)
                            return null;
                    // A match was found for the current character. Move to the
                    // next character in the long form.

                    if(firstLIndex==-1){
                            firstLIndex = lIndex;
                    }

                    lIndex--;
            }

            int endIndex = longForm.indexOf(" ", firstLIndex);
//            if(endIndex==-1){
                    endIndex = longForm.length();
//            }

            // Find the beginning of the first word (in case the first
            // character matches the beginning of a hyphenated word).
            lIndex = longForm.lastIndexOf(" ", lIndex) + 1;
            // Return the best long form, the substring of the original
            // long form, starting from lIndex up to the end of the original
            // long form.

            //return longForm.substring(lIndex);

            return longForm.substring(lIndex, endIndex).trim();
    }

    /**
     * Returns true if short form is an abbreviation for the long form.
     * */
    public static boolean isAbbreviation(String shortForm, String longForm){
    	return findBestLongForm(shortForm, longForm)!=null;
    }

}
