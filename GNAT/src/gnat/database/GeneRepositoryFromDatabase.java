package gnat.database;

import gnat.ISGNProperties;
import gnat.representation.Gene;
import gnat.representation.GeneContextModel;
import gnat.representation.GeneRepository;
import gnat.utils.ArrayHelper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Retrieves information on a set of genes from a database and compiles
 * them in to a GeneRepository.
 *
 *
 * @author Joerg
 */

public class GeneRepositoryFromDatabase {

	/** */
	Connection userConnection = null;
	Statement userStatement = null;
	ResultSet userResultset = null;
	boolean openConnection = false;

	public int verbosity = 0;


	/**
	 *
	 */
	public GeneRepositoryFromDatabase () {
		try {
			Class.forName(ISGNProperties.get("dbDriver"));
			userConnection = DriverManager.getConnection(
					ISGNProperties.get("dbAccessUrl"),
					ISGNProperties.get("dbUser"),
					ISGNProperties.get("dbPass")
			);
			userStatement = userConnection.createStatement();
			openConnection = true;
		} catch (java.sql.SQLException sqle) {
			sqle.printStackTrace();
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		} catch (Exception e) {
			System.err.println("Exception: unknown host?");
		}
	}
	
	
	/**
	 * 
	 * @param dummy
	 */
	public GeneRepositoryFromDatabase (boolean dummy) {
	}


	/**
	 *
	 * @param table
	 * @param column
	 * @param id
	 * @return
	 */
	public String[] getValues (String table, String column, String id) {
		Vector<String> temp = new Vector<String>();

		try {
			userResultset = userStatement.executeQuery("SELECT " +column + " FROM " + table + " WHERE ID="+id);
			while (userResultset.next()) {
				temp.add(userResultset.getString(column));
			}
		} catch (SQLException sqe) {
			sqe.printStackTrace();
		}

		String[] values = new String[temp.size()];
		for (int t = 0; t < temp.size(); t++)
			values[t] = temp.get(t);
		return values;
	}


	/**
	 * Returns a single gene from the database.
	 * @param id
	 * @return
	 */
	public Gene getGene (String id) {
		TreeSet<String> ids = new TreeSet<String>();
		ids.add(id);
		GeneRepository grep = getGeneRepository(ids);
		return grep.getGene(id);
	}


	/**
	 * Returns a set of genes from the database, stored in a GeneRepository.
	 * @param ids
	 * @return
	 */
	public GeneRepository getGeneRepository (TreeSet<String> ids) {
		Map<String, GeneContextModel> contextModelTable = new HashMap<String, GeneContextModel>();

		if (verbosity > 0 && ids.size() > 1)
			if (ids.size() <= 25)
				System.out.println("# Getting GeneRepository for " + ids + " ...");
			else
				System.out.println("# Getting GeneRepository for " + ids.size() + " genes.");

		int counter = 0;
		float perc = (float)ids.size() / 10.0f;
		int perc10 = Math.round(perc);
		Iterator<String> idIt = ids.iterator();
		while (idIt.hasNext()) {
			counter++;
			if (verbosity > 1 && perc10 > 0f && counter % perc10 == 0) {
				System.err.print(" [" + ((float)counter/(float)perc10*10.0f) + "%]");
			}
			String geneId = idIt.next();
			String[] values;

//			values = getValues("GR_GOTerm", "term", geneId);
//			if (values != null && values.length > 0){
//				GeneContextModel gcm = contextModelTable.get(geneId);
//				if (gcm == null) {
//					gcm = new GeneContextModel();
//					contextModelTable.put(geneId, gcm);
//				}
//				gcm.addGOTerms(values);
//			}

			values = getValues("GR_GOID", "GOID", geneId);
			if (values != null && values.length > 0){
				GeneContextModel gcm = contextModelTable.get(geneId);
				if (gcm == null) {
					gcm = new GeneContextModel();
					contextModelTable.put(geneId, gcm);
				}
				gcm.addGOCodes(values);
			}

			values = getValues("GR_ChrLocation", "location", geneId);
			if (values != null && values.length > 0){
				GeneContextModel gcm = contextModelTable.get(geneId);
				if (gcm == null) {
					gcm = new GeneContextModel();
					contextModelTable.put(geneId, gcm);
				}
				gcm.addLocations(values);
			}

			values = getValues("GR_GeneRIF", "generif", geneId);
			if (values != null && values.length > 0){
				GeneContextModel gcm = contextModelTable.get(geneId);
				if (gcm == null) {
					gcm = new GeneContextModel();
					contextModelTable.put(geneId, gcm);
				}
				gcm.addGeneRIFs(values);
			}

//			values = getValues("GR_GeneRef", "generef", geneId);
//			if (values != null && values.length > 0){
//				GeneContextModel gcm = contextModelTable.get(geneId);
//				if (gcm == null) {
//					gcm = new GeneContextModel();
//					contextModelTable.put(geneId, gcm);
//				}
//				gcm.addGeneRefs(values);
//			}
//
//			values = getValues("GR_UniProtID", "UID", geneId);
//			if (values != null && values.length > 0){
//				GeneContextModel gcm = contextModelTable.get(geneId);
//				if (gcm == null) {
//					gcm = new GeneContextModel();
//					contextModelTable.put(geneId, gcm);
//				}
//				gcm.addUniProtIDs(values);
//			}

			values = getValues("GR_PubMedID", "PMID", geneId);
			if (values != null && values.length > 0){
				GeneContextModel gcm = contextModelTable.get(geneId);
				if (gcm == null) {
					gcm = new GeneContextModel();
					contextModelTable.put(geneId, gcm);
				}
				gcm.addPubMedReferences(values);
			}

			values = getValues("GR_Summary", "summary", geneId);
			if (values != null && values.length > 0){
				GeneContextModel gcm = contextModelTable.get(geneId);
				if (gcm == null) {
					gcm = new GeneContextModel();
					contextModelTable.put(geneId, gcm);
				}
				gcm.addEntrezGeneSummary(values[0]);
			}

			values = getValues("GR_Interactorname", "name", geneId);
			if (values != null && values.length > 0){
				GeneContextModel gcm = contextModelTable.get(geneId);
				if (gcm == null) {
					gcm = new GeneContextModel();
					contextModelTable.put(geneId, gcm);
				}
				gcm.addInteractors(values);
			}

			// TODO: should be resolved to a name later
//			values = getValues("GR_Origin", "taxon", geneId);
//			if (values != null && values.length > 0){
//				GeneContextModel gcm = contextModelTable.get(geneId);
//				if (gcm == null) {
//					gcm = new GeneContextModel();
//					contextModelTable.put(geneId, gcm);
//				}
//				gcm.addOrigin(values);
//			}

			values = getValues("GR_ProteinDisease", "disease", geneId);
			if (values != null && values.length > 0){
				GeneContextModel gcm = contextModelTable.get(geneId);
				if (gcm == null) {
					gcm = new GeneContextModel();
					contextModelTable.put(geneId, gcm);
				}
				gcm.addDiseases(values);
			}

			values = getValues("GR_ProteinDomain", "domain", geneId);
			if (values != null && values.length > 0){
				GeneContextModel gcm = contextModelTable.get(geneId);
				if (gcm == null) {
					gcm = new GeneContextModel();
					contextModelTable.put(geneId, gcm);
				}
				gcm.addProteinDomains(values);
			}

			values = getValues("GR_ProteinFunction", "function", geneId);
			if (values != null && values.length > 0){
				GeneContextModel gcm = contextModelTable.get(geneId);
				if (gcm == null) {
					gcm = new GeneContextModel();
					contextModelTable.put(geneId, gcm);
				}
				gcm.addFunctions(values);
			}

			values = getValues("GR_ProteinKeywords", "keywords", geneId);
			if (values != null && values.length > 0){
				GeneContextModel gcm = contextModelTable.get(geneId);
				if (gcm == null) {
					gcm = new GeneContextModel();
					contextModelTable.put(geneId, gcm);
				}
				gcm.addKeywords(values);
			}

			values = getValues("GR_ProteinLength", "length", geneId);
			if (values != null && values.length > 0){
				GeneContextModel gcm = contextModelTable.get(geneId);
				if (gcm == null) {
					gcm = new GeneContextModel();
					contextModelTable.put(geneId, gcm);
				}
				gcm.addProteinLengths(values);
			}

			values = getValues("GR_ProteinMass", "mass", geneId);
			if (values != null && values.length > 0){
				GeneContextModel gcm = contextModelTable.get(geneId);
				if (gcm == null) {
					gcm = new GeneContextModel();
					contextModelTable.put(geneId, gcm);
				}
				gcm.addProteinMass(values);
			}

			values = getValues("GR_ProteinMutation", "mutation", geneId);
			if (values != null && values.length > 0){
				GeneContextModel gcm = contextModelTable.get(geneId);
				if (gcm == null) {
					gcm = new GeneContextModel();
					contextModelTable.put(geneId, gcm);
				}
				gcm.addProteinMutations(values);
			}

			values = getValues("GR_ProteinTissueSpecificity", "tissue", geneId);
			if (values != null && values.length > 0){
				GeneContextModel gcm = contextModelTable.get(geneId);
				if (gcm == null) {
					gcm = new GeneContextModel();
					contextModelTable.put(geneId, gcm);
				}
				gcm.addTissues(values);
			}

			values = getValues("GR_ProteinSubcellularLocation", "location", geneId);
			if (values != null && values.length > 0){
				GeneContextModel gcm = contextModelTable.get(geneId);
				if (gcm == null) {
					gcm = new GeneContextModel();
					contextModelTable.put(geneId, gcm);
				}
				gcm.addSubcellularLocations(values);
			}

			values = getValues("GR_ProteinInteraction", "interaction", geneId);
			if (values != null && values.length > 0){
				GeneContextModel gcm = contextModelTable.get(geneId);
				if (gcm == null) {
					gcm = new GeneContextModel();
					contextModelTable.put(geneId, gcm);
				}
				gcm.addProteinInteractions(values);
			}

			/*values = getValues("GR_", "", geneId);
			if (values != null && values.length > 0){
				GeneContextModel gcm = contextModelTable.get(geneId);
				if (gcm == null) {
					gcm = new GeneContextModel();
					contextModelTable.put(geneId, gcm);
				}
				gcm.add(values);
			}*/

		} // while more geneIDs


		//
		GeneRepository grep = new GeneRepository();
		idIt = ids.iterator();
		while (idIt.hasNext()) {
			String geneId = idIt.next();
			GeneContextModel gcm = contextModelTable.get(geneId);
			//Set<String> names = geneIdToNames.get(geneId);
			Gene gene = new Gene(geneId);

			String[] values = getValues("GR_Names", "name", geneId);
			gene.addNames(values);
			values = getValues("GR_ProteinNames", "name", geneId);
			gene.addNames(values);

			values = getValues("GR_Origin", "taxon", geneId);
			if (values != null && values.length > 0)
				gene.setTaxon(Integer.parseInt(values[0]));
			else {
				if (verbosity > 1)
					System.err.print("\n#No taxon ID found for gene " + geneId + "! ");
				gene.setTaxon(-1);
			}

			if(gcm!=null){
				gene.setContextModel(gcm);
			}else{
				gene.setContextModel(new GeneContextModel());
			}
			grep.addGene(gene);
        }

		if (verbosity > 1)
			System.out.println();

		return grep;
	}


	/**
	 * Returns the full gene repository for a single species.
	 * @param taxon -- Taxon ID of the species (human=9606, ...)
	 * @return
	 */
	public GeneRepository getGeneRepositoryForSpecies (int taxon) {
		TreeSet<String> ids = new TreeSet<String>();

		try {
			userResultset = userStatement.executeQuery("SELECT ID FROM `GR_Origin` WHERE taxon="+taxon);
			while (userResultset.next()) {
				ids.add(userResultset.getString("ID"));
			}
		} catch (SQLException sqe) {
			System.err.println(sqe.getMessage());
		}

		System.err.println("# GeneRepository for species " + taxon + " contains " + ids.size() + " genes.");

		return getGeneRepository(ids);
	}


	/**
	 * Returns the full gene repository.
	 * @return
	 */
	public GeneRepository getGeneRepository () {
		TreeSet<String> ids = new TreeSet<String>();

		try {
			userResultset = userStatement.executeQuery("SELECT DISTINCT(ID) FROM `GR_Origin`;");
			while (userResultset.next()) {
				ids.add(userResultset.getString("ID"));
			}
		} catch (SQLException sqe) {
			System.err.println(sqe.getMessage());
		}

		System.err.println("# Full GeneRepository contains " + ids.size() + " genes.");

		return getGeneRepository(ids);
	}

	
	/**
	 * 
	 * @param geneIds
	 * @return
	 */
	public GeneRepository getGeneRepositoryDUMMY (Set<Integer> geneIds) {
		GeneRepository grep = new GeneRepository();
		
		for (Integer id : geneIds) {
			String geneId = ""+id;

			GeneContextModel gcm = new GeneContextModel();

			Gene gene = new Gene(geneId);
			gene.addName(geneId);
			gene.setTaxon(9606);
			
			gene.setContextModel(gcm);

			grep.addGene(gene);
		}
		
		return grep;
	}
	
	
	/**
	 * 
	 * @param geneIds
	 * @return
	 */
	public Collection<Gene> getGeneListDUMMY (Set<Integer> geneIds) {
		Collection<Gene> geneList = new HashSet<Gene>();
		
		for (Integer id : geneIds) {
			String geneId = ""+id;

			GeneContextModel gcm = new GeneContextModel();

			Gene gene = new Gene(geneId);
			gene.addName(geneId);
			gene.setTaxon(9606);
			
			gene.setContextModel(gcm);

			geneList.add(gene);
		}
		
		return geneList;
	}
	

	/**
	 * Returns a set of genes from the database, stored in a GeneRepository.
	 * @param ids
	 * @return
	 */
	public Collection<Gene> getGeneRepositoryFAST (Set<Integer> geneIds) {
		Collection<Gene> geneList = new HashSet<Gene>();

		Map<Integer, Set<String>> gene2goIds	= getValues("GR_GOID", "GOID", geneIds);
		Map<Integer, Set<String>> gene2chrLoc 	= getValues("GR_ChrLocation", "location", geneIds);
		Map<Integer, Set<String>> gene2geneRifs = getValues("GR_GeneRIF", "generif", geneIds);
		Map<Integer, Set<String>> gene2pmIds = getValues("GR_PubMedID", "PMID", geneIds);
		Map<Integer, Set<String>> gene2summary = getValues("GR_Summary", "summary", geneIds);
		Map<Integer, Set<String>> gene2interactor = getValues("GR_Interactorname", "name", geneIds);
		Map<Integer, Set<String>> gene2disease = getValues("GR_ProteinDisease", "disease", geneIds);
		Map<Integer, Set<String>> gene2domain = getValues("GR_ProteinDomain", "domain", geneIds);
		Map<Integer, Set<String>> gene2function = getValues("GR_ProteinFunction", "function", geneIds);
		Map<Integer, Set<String>> gene2keywords = getValues("GR_ProteinKeywords", "keywords", geneIds);
		Map<Integer, Set<String>> gene2protLength = getValues("GR_ProteinLength", "length", geneIds);
		Map<Integer, Set<String>> gene2protMass = getValues("GR_ProteinMass", "mass", geneIds);
		Map<Integer, Set<String>> gene2mutation = getValues("GR_ProteinMutation", "mutation", geneIds);
		Map<Integer, Set<String>> gene2tissue = getValues("GR_ProteinTissueSpecificity", "tissue", geneIds);
		Map<Integer, Set<String>> gene2subCell = getValues("GR_ProteinSubcellularLocation", "location", geneIds);
		Map<Integer, Set<String>> gene2protInteraction = getValues("GR_ProteinInteraction", "interaction", geneIds);
		Map<Integer, Set<String>> gene2names = getValues("GR_Names", "name", geneIds);
		Map<Integer, Set<String>> gene2protNames = getValues("GR_ProteinNames", "name", geneIds);
		Map<Integer, Set<String>> gene2taxon = getValues("GR_Origin", "taxon", geneIds);


		//GeneRepository grep = new GeneRepository();
		for (Integer id : geneIds) {

			String geneId = ""+id;

			GeneContextModel gcm = new GeneContextModel();

			Gene gene = new Gene(geneId);

			Set<String> goCodes = gene2goIds.get(id);
			if(goCodes!=null){
				gcm.addGOCodes(ArrayHelper.set2StringArray(goCodes));
			}

			Set<String> chrLoc = gene2chrLoc.get(id);
			if(chrLoc!=null){
				gcm.addLocations(ArrayHelper.set2StringArray(chrLoc));
			}

			Set<String> generifs = gene2geneRifs.get(id);
			if(generifs!=null){
				gcm.addGeneRIFs(ArrayHelper.set2StringArray(generifs));
			}

			Set<String> pmids = gene2pmIds.get(id);
			if(pmids!=null){
				gcm.addPubMedReferences(ArrayHelper.set2StringArray(pmids));
			}

			Set<String> summary = gene2summary.get(id);
			if(summary!=null && summary.size()>0){
				gcm.addEntrezGeneSummary(ArrayHelper.set2StringArray(summary)[0]);
			}

			Set<String> interactors = gene2interactor.get(id);
			if(interactors!=null){
				gcm.addInteractors(ArrayHelper.set2StringArray(interactors));
			}

			Set<String> diseases = gene2disease.get(id);
			if(diseases!=null){
				gcm.addDiseases(ArrayHelper.set2StringArray(diseases));
			}

			Set<String> domains = gene2domain.get(id);
			if(domains!=null){
				gcm.addProteinDomains(ArrayHelper.set2StringArray(domains));
			}

			Set<String> functions = gene2function.get(id);
			if(functions!=null){
				gcm.addFunctions(ArrayHelper.set2StringArray(functions));
			}

			Set<String> keywords = gene2keywords.get(id);
			if(keywords!=null){
				gcm.addKeywords(ArrayHelper.set2StringArray(keywords));
			}

			Set<String> protLength = gene2protLength.get(id);
			if(protLength!=null){
				gcm.addProteinLengths(ArrayHelper.set2StringArray(protLength));
			}

			Set<String> protMass = gene2protMass.get(id);
			if(protMass!=null){
				gcm.addProteinMass(ArrayHelper.set2StringArray(protMass));
			}

			Set<String> mutation = gene2mutation.get(id);
			if(mutation!=null){
				gcm.addProteinMutations(ArrayHelper.set2StringArray(mutation));
			}

			Set<String> tissue = gene2tissue.get(id);
			if(tissue!=null){
				gcm.addTissues(ArrayHelper.set2StringArray(tissue));
			}

			Set<String> subcellLoc = gene2subCell.get(id);
			if(subcellLoc!=null){
				gcm.addSubcellularLocations(ArrayHelper.set2StringArray(subcellLoc));
			}

			Set<String> protInteraction = gene2protInteraction.get(id);
			if(protInteraction!=null){
				gcm.addProteinInteractions(ArrayHelper.set2StringArray(protInteraction));
			}

			Set<String> names = gene2names.get(id);
			if(names!=null){
				for (String name : names) {
	                gene.addName(name);
                }
			}

			Set<String> protNames = gene2protNames.get(id);
			if(protNames!=null){
				for (String name : protNames) {
	                gene.addName(name);
                }
			}

			Set<String> taxonIds = gene2taxon.get(id);
			if(taxonIds!=null && taxonIds.size()>0){
				gene.setTaxon(Integer.parseInt(taxonIds.iterator().next()));
			}
			else {
				//System.out.print("\n#No taxon ID found for gene " + geneId + "! ");
				gene.setTaxon(-1);
			}

			gene.setContextModel(gcm);

			//grep.addGene(gene);
			geneList.add(gene);
        }

		return geneList;
	}
	

	/**
	 *
	 * @param table
	 * @param column
	 * @param geneIds
	 * @return
	 */
	private Map<Integer, Set<String>> getValues (String table, String column, Set<Integer> geneIds) {
		Map<Integer, Set<String>> gene2values = new HashMap<Integer, Set<String>>();
		if (geneIds == null || geneIds.size() == 0) return gene2values;

		try {
			StringBuffer geneIdBuffer = toIdBuffer(geneIds);
			userResultset = userStatement.executeQuery("SELECT ID, " +column + " FROM " + table + " WHERE ID IN ("+geneIdBuffer+")");
			while (userResultset.next()) {
				int geneId = userResultset.getInt(1);
				String value = userResultset.getString(2);

				Set<String> values = gene2values.get(geneId);
				if(values==null){
					values = new HashSet<String>();
					gene2values.put(geneId, values);
				}

				values.add(value);
			}
		} catch (SQLException sqe) {
			sqe.printStackTrace();
		}

		return gene2values;
	}


	/***/
	private StringBuffer toIdBuffer(Set<Integer> ids){
		StringBuffer idBuffer = new StringBuffer();
        for (Integer integer : ids) {
        	if (idBuffer.length() > 0) {
        		idBuffer.append(",");
        	}
        	idBuffer.append(integer);
        }
        return idBuffer;
	}
}
