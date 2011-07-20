package gnat.filter.ner;

import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.Text;
import gnat.representation.TextRepository;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO Not fully implemented yet!!!
 * <br><br><br>
 * 
 * Handles post-processing of recognized species, currently tuned towards the 
 * {@link AliBabaSpeciesNer}: disambiguates species mentions ("cancer") and
 * maps -based on heuristics- genera and families to most likely individual
 * species or strains (for which then genes can be sought).
 * 
 * 
 * A workflow should look like this:
 * <ul>
 * <li>Get a text annotated for species using AskAliBaba; <tt></tt>
 * <li><tt>annotatedTextToMap</tt> -- extract the set of species as a map to evidence strings;
 * <li><tt>removeSpeciesWithAmbiguousNames</tt> -- remove species for which only ambiguous names were used from that map;
 * <li>get the keySet from the remaining map
 * <li><tt>getIndirectReferences</tt> -- add all indirect references (cell lines etc.)
 * <li><tt>normalizeSpecies</tt> -- remove, if wanted, general mentions such as kingdoms, families, etc. 
 * (&quot;mammalian&quot;, &quot;viruses&quot;)
 * </ul>
 * 
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 *
 */
public class SpeciesPostProcessing implements Filter {

	@Override
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {
		for (Text text : textRepository.getTexts()) {
			
			Map<Integer, List<String>> id2names = text.taxonIdsToNames;
			id2names = removeSpeciesWithAmbiguousNames(id2names);
			
			// normalize the taxon IDs (murinae -> M.mus etc.)
			id2names = normalizeTaxonIDs(id2names);
			
			//System.out.println("pmid2tax");
			// add species found in MeSH terms assoc. with this PMID
			//if (result.size() == 0)
			//	result.addAll(pmidToTaxIDsFromMeSH(pmid));
			
			//System.out.println("norm");
			// .. normalize again
			// [forgot why we have to call it twice, but it makes a difference!]
			//result = normalizeTaxonIDs(result);

			text.taxonIdsToNames = id2names;
			text.taxonIDs = id2names.keySet();
		}
	}
	
	
	/**
	 * Checks a taxon ID and returns more specific/more general IDs for
	 * certain cases. Returns an array of IDs that should replace the 
	 * initial taxon ID. If the array is empty, the annotation should
	 * be removed.<br>
	 * Examples:
	 * <ul>
	 * <li>murinae, murine, and mice =&gt; M. musculus
	 * <li>rat and rats =&gt; R. norvegicus
	 * <li>viruses &gt; too general, delete
	 * <li>rodents =&gt; ambiguous, add M. mus and R. norv
	 * <li>Drosophila =&gt; D. mel
	 * </ul>
	 * TODO: place mappings mammals =&gt; human here, depending on user's needs
	 * TODO: store these mappings in a configuration file
	 * @param tax
	 * @return
	 */
	public int[] normalizeTaxonID (int tax) {
		List<Integer> res = new LinkedList<Integer>();

		// from general to specific:
		//Murinae 39107 => 10090 || mice => m.mus		
		if (tax == 39107 || tax == 10095)
			res.add(10090);

		//Rattus ("rat") => R. norvegicus || rats => r.n.
		else if (tax == 10114 || tax == 10118) 
			res.add(10116);

		// Drosophila => D.mel
		else if (tax == 32281)
			res.add(7227);

		// Viruses -> too general, drop
		else if (tax == 10239)
			return new int[]{};
		
		// homo (has only this one synonym) -> drop
		else if (tax == 9605)
			return new int[]{};

		// from general to multiple specific species
		// rodents => add mouse and rat???
		else if (tax == 9989) {
			res.add(10090);
			res.add(10116);
		}
		
		// E. coli: we use two dictionaries, from E. coli and its strain K12
		else if (tax == 562) {
			res.add(562);
			res.add(83333);
		}


		// from specific to general:
		// s.pombe strain 972h- => s.pombe
		else if (tax == 284812)
			res.add(4896);

		if (res.size() == 0)
			res.add(tax);
		

		int[] result = new int[res.size()];
		for (int i = 0; i < res.size(); i++)
			result[i] = res.get(i);
		return result;
	}


	/**
	 * Remove some spurious mentions of species --- mostly false positives from
	 * the initial annotator. For example, remove the species "cancers" (6754) if
	 * it was not mentioned with any other name (Crustacea, crab) in the same text.
	 * Assumption in this case: it is highly likely a reference to neoplasms.
	 * 
	 * TODO: put these rules into a configuration file
	 * 
	 * @param species
	 * @return
	 */
	private Map<Integer, List<String>> removeSpeciesWithAmbiguousNames (Map<Integer, List<String>> species) {
		Map<Integer, List<String>> result = new HashMap<Integer, List<String>>();
		
		for (int tax: species.keySet()) {
			List<String> names = species.get(tax);
			boolean remove = false;

			// cancer 6754 => most of the time an FP (breast cancer, etc.)
			if (tax == 6754) {
				// assume it is a wrong annotation
				remove = true;
				for (String name: names) {
					// but if any other than the ambiguous name was
					// also found, the species is indeed correct
					if (!name.matches("[Cc]ancers?"))
						remove = false;
				}
			}
			
			// dito for codon
			else if (tax == 79338) {
				remove = true;
				for (String name: names) {
					if (!name.matches("codon"))
						remove = false;
				}
			}
			
			// glycine
			else if (tax == 3846) {
				remove = true;
				for (String name: names) {
					if (!name.matches("glycine"))
						remove = false;
				}
			}
			
			// bears
			else if (tax == 9641) {
				remove = true;
				for (String name: names) {
					if (!name.matches("bears"))
						remove = false;
				}
			}
			
			// bears
			else if (tax == 9855) {
				remove = true;
				for (String name: names) {
					if (!name.matches("axis"))
						remove = false;
				}
			}
			
			// monitors 8555
			else if (tax == 8555) {
				remove = true;
				for (String name: names) {
					if (!name.matches("monitors?"))
						remove = false;
				}
			}
			
			// pan 9596
			else if (tax == 9596) {
				remove = true;
				for (String name: names) {
					if (!name.matches("pans?"))
						remove = false;
				}
			}
			
			// thymus 8555
			else if (tax == 49990) {
				remove = true;
				for (String name: names) {
					if (!name.matches("thymus"))
						remove = false;
				}
			}

			// wrong annotation? don't put it in the answer
			if (remove)
				continue;
			else
				result.put(tax, names);
		}
		
		return result;
	}
	
	
//	/**
//	 * Removes tax IDs from a set, for which no genes exists: kingdoms,
//	 * families, etc. Replaces too general species with more specific ones,
//	 * for instance, murinae with M.mus. Calls <tt>normalizeTaxonID</tt> for each ID.
//	 * 
//	 * @see #normalizeTaxonID(int)
//	 * @param species
//	 * @return
//	 */
//	public TreeSet<Integer> normalizeTaxonIDs (TreeSet<Integer> ids) {
//		TreeSet<Integer> result = new TreeSet<Integer>();
//		for (int tax: ids) {
//			int taxa[] = normalizeTaxonID(tax);	
//			for (int t: taxa)
//				result.add(t);
//		}
//		return result;
//	}


	/**
	 * Removes tax IDs from a mapping of IDs to names for which no genes exists:
	 * kingdoms, families, etc.
	 * 
	 * @see #normalizeTaxonID(int)
	 * @param ids2names
	 * @return
	 */
	public Map<Integer, List<String>> normalizeTaxonIDs (Map<Integer, List<String>> ids2names) {
		Map<Integer, List<String>> result = new HashMap<Integer, List<String>>();
		
		for (int tax: ids2names.keySet()) {
			int taxa[] = normalizeTaxonID(tax);
			
			if (taxa.length == 0) {
				//result.remove(tax);
			} else if (taxa.length == 1) {
				if (tax == taxa[0])
					result.put(tax, ids2names.get(tax));
				else {
					List<String> copy = ids2names.get(tax);
					List<String> copy2 = ids2names.get(taxa[0]);
					if (copy2 != null)
						copy.addAll(copy2);
					result.put(taxa[0], copy);
					//result.remove(tax);
				}
			} else {
				List<String> copy = ids2names.get(tax);
				//result.remove(tax);
				for (int i = 0; i < taxa.length; i++) {
					result.put(taxa[i], copy);
				}
			}

		}
		
		return result;
	}
	

}
