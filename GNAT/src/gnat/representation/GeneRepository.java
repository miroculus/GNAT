package gnat.representation;

import gnat.database.GeneRepositoryFromDatabase;
import gnat.utils.FileHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * For every gene, GNAT stores a plethora of information obtained from sources such as NCBI Entrez Gene, UniProt, and GOA. GNAT disambiguates 
 * genes that share the same synonym when such a mention is encountered in a document. For example, the name "p21" can refer to (human) CDKN1A, TCEAL1, and NSG1, 
 * among others. The data in the Gene Repository are used to determine which gene a specific publication is referring to, by comparing statements in the text to 
 * information known about all potential candidate genes. For instance, the text might talk about a molecular function that is known for one of the candidates
 * but not the others, or a tissue specificity, or involvement in disease.
 * <br/>
 * 
 * 
 * @author Joerg Hakenberg
 */
public class GeneRepository {//implements Serializable {

	/**
     *
     */
    //private static final long serialVersionUID = -3413086732527472215L;

	/** Maps gene ids (string) to genes */
	public Map<String, Gene> geneMap = new HashMap<String, Gene>();


	/**
	 * Adds a single Gene to this repository, if it was not alreay known (by ID).
	 * @param gene
	 */
	public void addGene (Gene gene) {
		if (!geneMap.containsKey(gene.getID())) {
			geneMap.put(gene.getID(), gene);
		} else {
			System.err.println("#GeneRep: Duplicate gene ID: " + gene.ID + ", keeping older gene.");
		}
	}


	/**
	 * Returns a collection of all Genes in this repository.
	 */
	public Collection<Gene> getGenes(){
		return geneMap.values();
	}


	/**
	 * Returns the Gene for the given ID.
	 * @param id
	 * @return
	 */
	public Gene getGene(String id) {
	    return geneMap.get(id);
    }


	/**
	 * Returns a collection of Genes for the given IDs.
	 * @param ids
	 * @return
	 */
	public Collection<Gene> getGenes (Collection<String> ids) {
		LinkedList<Gene> result = new LinkedList<Gene>();
		for (String id: ids) {
			if (geneMap.containsKey(id))
				result.add(geneMap.get(id));
		}
		return result;
	}


	/**
	 * Returns a map of synonyms mapped to lists of gene ids.
	 * */
	public Map<String, List<String>> getSingleWordSynonyms(){
		Map<String, List<String>> synonymMap = new HashMap<String, List<String>>();	 // maps a synonym to a list of gene ids

		for (Gene  gene : getGenes()) {
			for (String synonym : gene.getNames()) {
				if(synonym.split(" ").length==1){
					List<String> geneIdList = synonymMap.get(synonym);
					if(geneIdList==null){
						geneIdList = new LinkedList<String>();
						synonymMap.put(synonym, geneIdList);
					}
					geneIdList.add(synonym);
				}
            }
        }

		return synonymMap;
	}

	/**
	 * Adds all Genes to the repository
	 * @param genes
	 */
	public void addGenes (Collection<Gene> genes) {
		for (Gene gene: genes)
			addGene(gene);
	}


	/**
	 * Returns the current size (number of Genes) of this repository.
	 * @return
	 */
	public int size () {
		return geneMap.size();
	}


	/**
	 * For testing purposes.
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main (String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
		GeneRepositoryFromDatabase grepper = new GeneRepositoryFromDatabase();
		if (args.length > 1) {
			GeneRepository geneRepository = null;
			if (args[0].equals("-tax")) {
				System.out.println("#Enough memory? >25,000 genes => 10GB");
				if (args[1].matches("\\d+"))
					geneRepository = grepper.getGeneRepositoryForSpecies(Integer.parseInt(args[1]));
				else if (args[1].matches("all"))
					geneRepository = grepper.getGeneRepository();
				else {
					System.out.println("Unknown parameter: -tax " + args[1]);
					System.out.println("  possible values:      all | <taxon ID>");
					System.exit(2);
				}
				System.out.println("#Writing file...");
				FileHelper.writeObjectToFile(geneRepository, new File("geneRepository" + args[1] + ".object"));
			}	
		} else {
			System.out.println("Parameters:");
			System.out.println("  -tax <taxon ID>   --   species for which to build the repository");
			System.out.println("    -tax 9606       --   writes gene repository for human");
			System.out.println("    -tax all        --   writes gene repository for all genes in the DB");
			System.exit(1);
		}

		/*GeneRepository geneRepository = GeneFactory.loadGeneRepositoryFromEntrezGeneObjectFiles(
				"entrezGeneLexicon_oneSynPerLine.txt", "entrezGeneObjects/geneRifs.object",
				"entrezGeneObjects/goIDs.object", "entrezGeneObjects/goTerms.object",
				"entrezGeneObjects/summaries.object", "entrezGeneObjects/uDiseases.object",
				"entrezGeneObjects/uFunctions.object", "entrezGeneObjects/uKeywords.object",
				"entrezGeneObjects/locations.object", "entrezGeneObjects/uTissues.object",
				"entrezGeneObjects/pmIDs.object", "entrezGeneObjects/uMutations.object",
				"entrezGeneObjects/uLength.object", "entrezGeneObjects/uDomains.object");
		FileHelper.writeObjectToFile(geneRepository, new File("entrezGeneObjects/geneRepository.object"));
		*/
		
	}

}
