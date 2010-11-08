package gnat.filter.nei;

import gnat.ConstantsNei;
import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.Gene;
import gnat.representation.GeneRepository;
import gnat.representation.RecognizedEntity;
import gnat.representation.TextRepository;
import gnat.utils.StringHelper;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Removes gene names that refer to families etc. rather than individual genes.
 * <br><br>
 * Short and abbreviated gene names ("TNF") often depict a gene family rather than an individual gene.
 * For gene mention normalization, such occurrences should be removed. However, it also happens often
 * that such an unspecific name actually refers to the first member ("TNF alpha", "ABC-1") of a family.
 * <br>
 * This filter scans the text for any occurrence of the full name, either with an attached "-1" etc. or
 * by reference using the full name -- when the same gene ID was assigned to another occurrence that 
 * has a different name. If no such synonym was found, removes the unspecific name as potential gene
 * throughout the text.
 * 
 * <br><br>
 * <b>Requirements:</b><br>
 * Needs information on each gene (such as synonyms), this requires a loaded GeneRepository.
 *
 * @author Joerg
 */
public class NameValidationFilter implements Filter {
	
	/**
	 * 
	 */
	public NameValidationFilter () { }


	/**
	 * 
	 */
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {
		Iterator<RecognizedEntity> unidentifiedGeneNames = context.getUnidentifiedEntities().iterator();
		while (unidentifiedGeneNames.hasNext()) {
			
			RecognizedEntity recognizedGeneName = unidentifiedGeneNames.next();
			Set<String> idCandidates = context.getIdCandidates(recognizedGeneName);

			TreeSet<String> names = new TreeSet<String>();
			for (String id: idCandidates) {
				Gene gene = geneRepository.getGene(id);
				if (gene != null) {
					for (String name: gene.getNames())
						names.add(name.toLowerCase().trim());
				}
			}

			String myName = recognizedGeneName.getName().toLowerCase();
			// escape certain characters so that myName can later be used in reg.exes
			myName = StringHelper.espaceString(myName);
			
			//if (myName.matches(".*[\\s\\-0-9].*"))
			if (myName.matches(".*[0-9A-Z].*") && myName.length() > 4)
				continue;
			if (names.contains(myName))
				continue;
			//System.out.println("#NVF: " + myName + " not found in DB (IDs: " + idCandidates + ")");
			
			// does this gene only occur as name-1 in the dictionary?
			boolean foundOnlyWithOne = false;
			for (String name: names) {
				if (name.matches(myName + "[\\s\\-\\_]?([1IiAa]|alpha)")) {
					foundOnlyWithOne = true;
				}
			}
			if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
				System.out.println("#NVF: found only with -1: " + recognizedGeneName.getName());

			// if yes, then check if this gene is also mentioned using any other name
			if (foundOnlyWithOne) {
				String text = recognizedGeneName.getText().plainText.toLowerCase();
				
				boolean foundAnotherName = false;
				for (String name: names) {
					name = StringHelper.espaceString(name);
					if (text.matches(".*(^|[\\s\\(\\[])" + name + "([\\s\\,\\.\\)\\]\\;\\:]|$).*")) {
						foundAnotherName = true;
						if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
							System.out.println("#NVF: found another name: " + name);
						break;
					}
				}
				
				// remove this gene name if there was no other reference than
				// the initial, uncertain one that lacked the -1
				if (!foundAnotherName) {
					if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
						System.out.println("#NVF: Removing " + recognizedGeneName.getName() + " in " + recognizedGeneName.getText().PMID);
					context.removeEntitiesHavingName(recognizedGeneName.getName(), recognizedGeneName.getText());
				}
				
			}
		}
	}
	
	
	/**
	 * For testing purposes only.
	 * @param args
	 */
	public static void main (String[] args) {
//		String test = "hallo spin 133";
//		String x = test.substring(6, 10);
//		System.out.println(x);
		
		String myOriginalName = "glutamate/h(+) symporter";
		String myName = StringHelper.espaceString(myOriginalName);
		System.out.println(myOriginalName);
		System.out.println(myName);
		
		String name = "glutamate/h(+) symporter alpha";
		if (name.matches(myName + "[\\s\\-\\_]?([1IiAa]|alpha)"))
			System.out.println(true);
		else
			System.out.println(false);
		
		String text = "This text contains " + myOriginalName + ".";
		if (text.matches(".*(^|[\\s\\(\\[])" + myName + "([\\s\\,\\.\\)\\]\\;\\:]|$).*"))
			System.out.println(true);
		else
			System.out.println(false);
	}
	
}
