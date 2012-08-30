package gnat.filter.nei;

import gnat.ConstantsNei;
import gnat.ISGNProperties;
import gnat.filter.Filter;
import gnat.filter.ner.GnatServiceNer;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.IdentificationStatus;
import gnat.representation.RecognizedEntity;
import gnat.representation.Text;
import gnat.representation.TextAnnotation;
import gnat.representation.TextRepository;
import gnat.server.GnatService.Tasks;
import gnat.utils.FileHelper;
import gnat.utils.StringHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Checks if the given gene is indeed discussed together (same sentence, same clause, ...)
 * with the given species.<br>
 * <br>
 * Input: gene ID, taxon ID, sentence, previous sentence, sentence0=title, sentence1=first sentence, abstract<br>
 * Output: true or false...<br>
 * <br>
 * Removes wrongly assigned gene IDs the candidate IDs of a RecognizedGeneName. 
 * <br>
 * 
 * Ideas:
 * <ul>
 * <li>if the gene occurs in a sentence where another species is mentioned but not the given one,
 * invalidate the ID
 * <li>
 * </ul>
 * 
 * <br><br>
 * <b>Requirements:</b><br>
 * Needs information on each gene (such as species), thus requires a loaded GeneRepository.
 * 
 * @author Joerg
 */
public class SpeciesValidationFilter implements Filter {

	/** Maps a taxon ID to it's parent taxon ID. */
	private static Map<Integer, Integer> parentTable = new HashMap<Integer, Integer>();
	private GnatServiceNer speciesRecognizer;

	/**
	 * 
	 */
	public SpeciesValidationFilter () {
		//System.out.println("#SVF: loading parent table from " + parentTableFile);
		String[] lines = FileHelper.readFromFile(ISGNProperties.get("taxonParentTable"));
		for (String line: lines) {
			if (line.startsWith("#")) continue;
			String[] cols = line.split("\t");
			parentTable.put(Integer.parseInt(cols[0]), Integer.parseInt(cols[1]));
		}
		
		this.speciesRecognizer = new GnatServiceNer(Tasks.SPECIES_NER);
	}


	/**
	 * 
	 * 
	 * @param context
	 */
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {
		if (geneRepository == null || geneRepository.size() == 0) {
			System.err.println("#SpeciesValidation: requires loaded GeneRepository; skippping this filter.");
			return;
		}
		
		//if (true) return;
		// get all yet unidentified genes
		Iterator<RecognizedEntity> unidentifiedGeneNames = context.getUnidentifiedEntities().iterator();
		while (unidentifiedGeneNames.hasNext()) {
			RecognizedEntity recognizedGeneName = unidentifiedGeneNames.next();
			Text text = recognizedGeneName.getText();
			TextAnnotation annotation = recognizedGeneName.getAnnotation();
			String sentence = text.getSentenceAround(annotation.getTextRange().getBegin());
			String plaintext = text.plainText;
			//String name = recognizedGeneName.getName();
		
			IdentificationStatus identificationStatus = context.getIdentificationStatus(recognizedGeneName);
			Set<String> geneIdCandidates = identificationStatus.getIdCandidates();
			
			TreeSet<Integer> taxa = new TreeSet<Integer>();
			
			// split text into sentences
			
			// get all sentences that contain the exact spelling of the gene name
			
			// first, check in the same sentence
			taxa.addAll(speciesRecognizer.textToTaxIDs(sentence).keySet());
			
			// single tax ID in that sentence?
			// => kick all genes from other species
			//if (sentence.indexOf("ATRX") >= 0)
			//	System.out.println("#ATRX: " + taxa + " " + sentence);
			
			if (taxa.size() == 0) {
				// TODO: if no species were mentioned in this sentence

			} else if (taxa.size() == 1) {
				// if exactly one species was mentioned in this sentence
				// remove all genes referring to other species
				
				int keepThisTax = taxa.first();
				int parent = keepThisTax;
				if (parentTable.containsKey(keepThisTax))
					parent = parentTable.get(keepThisTax);
				System.out.println("#SVF: Found a single species "+keepThisTax+"/"+parent+" in sentence " + sentence);
				
				TreeSet<String> removeGeneIds = new TreeSet<String>();
				Iterator<String> it = geneIdCandidates.iterator();
				while (it.hasNext()) {
					String gid = it.next();
					int currentTax = geneRepository.getGene(gid).getTaxon();
					if (currentTax != keepThisTax && currentTax != parent) {
						System.out.println("#SVF: removing gene " + gid + " (tax: " + currentTax + ")");
						removeGeneIds.add(gid);
					} else {
						System.out.println("#SVF: keeping gene " + gid + " (tax: " + currentTax + ")");
					}
				}

				for (String remove: removeGeneIds)
					identificationStatus.removeIdCandidate(remove);
				
				//if (removeGeneIds.size() == geneIdCandidates.size())
				//	System.out.println("#SVF: Removed all IDs for "+name+" in sentence " + sentence);
				//else
				//	System.out.println("#SVF: kept IDs for "+name+": "  + identificationStatus.getIdCandidates());
				//geneIdCandidates.removeAll(removeGeneIds);

			} else {
				if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
					System.out.println("#SVF: found multiple species in sentence '" + sentence + "' " + taxa);
				// check directly in front of name: mouse XYZ, human and mouse XYZ
				// idea: go leftward until something other than a species name or a comman appears
				// take care of multi-word species names ("d. mel.", "e. coli k12")
				Map<Integer, Set<String>> taxNames = speciesRecognizer.//textToTaxIDsAndNames(sentence);
					textToTaxIDs(sentence);
				
				// check immediately after name: XYZ in human, XYZ in HEK cells
				//String before = getWordsBefore(sentence, recognizedGeneName.getBegin(), 1);
				Set<Integer> npSpecies =
					getSpeciesFromImmediatelyBefore(plaintext, recognizedGeneName.getBegin(), taxNames);
				
				// TODO solve the "mouse ABC1 and DEF2" case ... 
				// TODO ... maybe containsOnlyValidSpeciesAndConjunctions should check for Ands, Conjunctions, ...
				// TODO ... Species, and ProperNouns=NounsWithSymbolOrNumberOrNoVowel (gene name 'snk'!)
				
				// remove candidate ID only if the gene could definitely be resolved to a species
				if (npSpecies.size() > 0) {
					if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
						System.out.println("#SVF: for " + recognizedGeneName.getName() + ", species in NP are " + npSpecies);
									
					TreeSet<Integer> parents = new TreeSet<Integer>();
					for (int tax: npSpecies)
						if (parentTable.containsKey(tax))
							parents.add(parentTable.get(tax));
					
					TreeSet<String> removeGeneIds = new TreeSet<String>();
					Iterator<String> it = geneIdCandidates.iterator();
					while (it.hasNext()) {
						String gid = it.next();
						int currentTax = geneRepository.getGene(gid).getTaxon();
						
						if (npSpecies.contains(currentTax) || parents.contains(currentTax)){//&& currentTax != parent) {
							if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
								System.out.println("#SVF: keeping gene " + gid + " (tax: " + currentTax + ")");
						} else {
							if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
								System.out.println("#SVF: removing gene " + gid + " (tax: " + currentTax + ")");
							removeGeneIds.add(gid);
						}
					}
	
					for (String remove: removeGeneIds)
						identificationStatus.removeIdCandidate(remove);
				} else { // if npSpecies=0
					if (taxNames.size() > 0) {
						
						if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.DEBUG) >= 0)
							System.out.println("#SVF: for " + recognizedGeneName.getName() + ", species in sentence are " + taxNames.keySet());
						
					}
				}
			}
		}
	}

	/**
	 * Returns the <tt>words</tt> words to the left of the given position. 
	 * Returns as many words as possible, i.e., if there are not enough
	 * words on the left, returns everything from the beginning to the given
	 * position.
	 * 
	 * @param text
	 * @param position
	 * @param words
	 */
	public static String getWordsBefore (String text, int position, int words) {
		String temp = text.substring(0, position);
		temp = temp.trim();
		String term = "";
		
		int w = 0;
		while (w < words) {
			if (temp.lastIndexOf(" ") == -1) {
				term = text.substring(0, position).trim();
				break;
			}
			term = temp.substring(temp.lastIndexOf(" ")) + term;
			temp = temp.substring(0, temp.lastIndexOf(" "));
			temp = temp.trim();
			w++;
		}
		
		return term.trim();
	}
	
	
	/**
	 * Returns the <tt>word</th>th word to the left of the given position.
	 * Returns the empty string if no such word exists (i.e., the text
	 * does not contain enough words to the left.)
	 * 
	 * @param text
	 * @param position
	 * @param word
	 */
	public static String getWordBefore (String text, int position, int word) {
		String temp = text.substring(0, position);
		temp = temp.trim();
		String term = "";
		
		int w = 0;
		while (w < word) {
			if (temp.lastIndexOf(" ") == -1)
				return "";
			term = temp.substring(temp.lastIndexOf(" "));// + term;
			temp = temp.substring(0, temp.lastIndexOf(" "));
			temp = temp.trim();
			w++;
		}
		
		return term.trim();
	}

	
	/**
	 * 
	 * @param sentence
	 * @param position
	 * @param taxNames
	 * @return
	 */
	public static Set<Integer> getSpeciesFromImmediatelyBefore (String sentence, int position, Map<Integer, Set<String>> taxNames) {
		Set<Integer> vtaxa;
		Set<Integer> lastvtaxa = new TreeSet<Integer>();
		int w = 1;
		String before;
		while (w < 50) {
			
			before = getWordsBefore(sentence, position, w);
			//System.out.println("Before: '" + before + "'");
			vtaxa = containsOnlyValidSpeciesAndConjunctions(before, taxNames);
			//System.out.println(vtaxa);
			
			if (vtaxa.size() == 0) {
				before = getWordsBefore(sentence, position, w+1);
				//System.out.println("Before: '" + before + "'");
				vtaxa = containsOnlyValidSpeciesAndConjunctions(before, taxNames);
				//System.out.println(vtaxa);
				
				if (vtaxa.size() == 0)
					break;
				else {
					lastvtaxa = vtaxa;
					w++;
				}
			} else
				lastvtaxa = vtaxa;
			
			w++;
		}
		
		return lastvtaxa;
	}


	/**
	 * 
	 * @param taxNames
	 * @param name
	 * @return
	 */
	public static TreeSet<Integer> isValidSpeciesName (HashMap<Integer, TreeSet<String>> taxNames, String name) {
		TreeSet<Integer> result = new TreeSet<Integer>();
		for (int tax: taxNames.keySet()) {
			if (taxNames.get(tax).contains(name))
				result.add(tax);
		}
		return result;
	}


	/**
	 * 
	 * @param text
	 * @param taxNames
	 * @return
	 */
	public static Set<Integer> containsOnlyValidSpeciesAndConjunctions (String text, Map<Integer, Set<String>> taxNames) {
		HashMap<String, Integer> name2tax = new HashMap<String, Integer>();
		Vector<String> names = new Vector<String>();
		for (int tax: taxNames.keySet()) {
			for (String name: taxNames.get(tax)) {
				//System.out.println("Adding " + name);
				names.add(name);
				name2tax.put(name, tax);
			}
		}
		
		TreeSet<Integer> containsTaxIds = new TreeSet<Integer>();
		
		List<String> list = names.subList(0, names.size());
		Collections.sort(list, StringHelper.stringLengthComparatorDescending);
		
		String copy = text;
		for (String name: list) {
			name = StringHelper.espaceString(name);
			if (copy.indexOf(name) >= 0)
				containsTaxIds.add(name2tax.get(name));
			copy = copy.replaceAll(name, "");
		}
		copy = copy.replaceAll("\\s\\s", " ");
		copy = copy.replaceAll("([\\,\\.\\;\\:\\)\"\'])\\s", " $1 ");
		copy = copy.replaceAll("\\s([(\\)\\]\"\'])", " $1 ");
		
		copy = copy.replaceAll("(\\,\\s|\\s?and\\s?)", "");
		
		if (copy.trim().length() == 0)
			return containsTaxIds;
		else
			return new TreeSet<Integer>();
	}

	
	/**
	 * 
	 * @param args
	 */
	public static void main (String[] args) {
		//String sentence ="The two G-protein coupled prokineticin receptors, PK-R1 and PK-R2, were expressed in rat dorsal root ganglia (DRG) and in dorsal quadrants of spinal cord (DSC) and bound Bv8 and the mammalian orthologue, EG-VEGF, with high affinity.";
		//String sentence = "The mice, mouse, and murine gene is here.";
		//String sentence = "The recombinant cytokines such as interleukin-3 and thrombopoietin also activate prk mRNA expression in MO7e cells.";
		//TreeSet<Integer> taxa = recognizer.textToTaxIDs(sentence);
		//System.out.println(taxa);

		GnatServiceNer speciesRecognizer = new GnatServiceNer(Tasks.SPECIES_NER);

		String text = "The human amino-terminal portion has the feature of the catalytic domain of a serine/threonine kinase and shows strong homology to mouse fnk and other polo family kinases including mouse snk, HEL cell and murine plk, Drosophila polo, and yeast Cdc5.";
		Map<Integer, Set<String>> taxNames = speciesRecognizer.textToTaxIDs(text);
		Set<Integer> taxa = speciesRecognizer.textToTaxIDs(text).keySet();
		System.out.println("#SVF: found multiple species in sentence '" + text + "' " + taxa);
		// check directly in front of name: mouse XYZ, human and mouse XYZ
		// idea: go leftward until something other than a species name or a comman appears
		// take care of multi-word species names ("d. mel.", "e. coli k12")
		System.out.println(taxNames);

		
		String[] rgnames = new String[]{"fnk", "snk", "plk", "polo", "Cdc5"};
		
		for (String rgname: rgnames) {
			int begin =  text.indexOf(rgname);
			
			Set<Integer> lastvtaxa = getSpeciesFromImmediatelyBefore(text, begin, taxNames);
		
		/*
//		int begin1 = text.indexOf("fnk");
//		int begin2 = text.indexOf("snk");
//		int begin3 = text.indexOf("plk");
//		int begin4 = text.lastIndexOf("polo");
//		int begin5 = text.indexOf("Cdc5");

		// check immediately after name: XYZ in human, XYZ in HEK cells
		String before = "";
		/*String test = "";
		TreeSet<Integer> validSpecies = new TreeSet<Integer>();
		int w = 1;
		int x = 1;
		while (w < 50) {
			String bef = getWordBefore(text, begin1, w);
			if (bef == "")
				break;
			
			TreeSet<Integer> taxs = isValidSpeciesName(taxNames, bef);
			if (taxs.size() > 0) {
				validSpecies.addAll(taxs);
			}
			
			
			
			w++;
		}*//*

			TreeSet<Integer> vtaxa;
			TreeSet<Integer> lastvtaxa = null;
			int w = 1;
			while (w < 50) {
				
				before = getWordsBefore(text, begin, w);
				System.out.println("Before: '" + before + "'");
				vtaxa = containsOnlyValidSpeciesAndConjunctions(before, taxNames);
				System.out.println(vtaxa);
				
				if (vtaxa.size() == 0) {
					before = getWordsBefore(text, begin, w+1);
					System.out.println("Before: '" + before + "'");
					vtaxa = containsOnlyValidSpeciesAndConjunctions(before, taxNames);
					System.out.println(vtaxa);
					
					if (vtaxa.size() == 0)
						break;
					else {
						lastvtaxa = vtaxa;
						w++;
					}
				} else
					lastvtaxa = vtaxa;
				
				w++;
			}*/
		
			System.out.println("Final species for " + rgname + ": " +lastvtaxa);
		}
		
	}
	
}
