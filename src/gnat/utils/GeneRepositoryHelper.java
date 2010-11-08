// ©2008 Transinsight GmbH - www.transinsight.com - All rights reserved.
package gnat.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * TODO describe me
 *
 */
public class GeneRepositoryHelper
{

	static boolean normalize = true;
	
	static TreeSet<Integer> restrictAnalysisToTaxonIDs;
	static {
		restrictAnalysisToTaxonIDs = new TreeSet<Integer>();
		restrictAnalysisToTaxonIDs.add(9606);  // hsap
		restrictAnalysisToTaxonIDs.add(10090); // mmus
		restrictAnalysisToTaxonIDs.add(10116); // rnorv
		restrictAnalysisToTaxonIDs.add(8355); // xlaevis
		restrictAnalysisToTaxonIDs.add(7227); // dmel
		restrictAnalysisToTaxonIDs.add(9031); // ggallus
		restrictAnalysisToTaxonIDs.add(562); // ecoli
		restrictAnalysisToTaxonIDs.add(4932); // scer
		restrictAnalysisToTaxonIDs.add(3702); // athal
		restrictAnalysisToTaxonIDs.add(9913); // btau
		restrictAnalysisToTaxonIDs.add(6239); // celegans
		restrictAnalysisToTaxonIDs.add(3055); // creinh
		restrictAnalysisToTaxonIDs.add(7955); // drerio
		restrictAnalysisToTaxonIDs.add(44689); // ddisco
		restrictAnalysisToTaxonIDs.add(11103); // HCV
		restrictAnalysisToTaxonIDs.add(148305); // mgrisea
		restrictAnalysisToTaxonIDs.add(2104); // mpneu
		restrictAnalysisToTaxonIDs.add(5141); // ncrassa
		restrictAnalysisToTaxonIDs.add(4530); // osativa
		restrictAnalysisToTaxonIDs.add(5833); // pfalci
		restrictAnalysisToTaxonIDs.add(4754); // pcarinii
		restrictAnalysisToTaxonIDs.add(4896); // spom
		restrictAnalysisToTaxonIDs.add(31033); // trubripes
		restrictAnalysisToTaxonIDs.add(4577); // zmays
		restrictAnalysisToTaxonIDs.add(11676); // HIV-1
		restrictAnalysisToTaxonIDs.add(9986); // ocuniculus
		//restrictAnalysisToTaxonIDs.add(); //
	}
	
	
	/**
	 * TODO describe me!
	 *
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static void main(String[] args) throws ClassNotFoundException, SQLException
	{
		Class.forName("org.gjt.mm.mysql.Driver");//com.mysql.jdbc.Driver");

		//computeAverageGeneIdsPerName();
		//computeAverageSpeciesPerName();
		
		
		getAllNames();
		
		
	}
	
	
	
	public static void getAllNames () throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/joerg", "joerg", "joerg9");

//		Map<String, Integer> name2geneIdCount = new HashMap<String, Integer>();

		Statement statement = connection.createStatement();
		ResultSet resultSet = null;
		
		for (int taxon: restrictAnalysisToTaxonIDs) {
		
			String query = "SELECT g.ID, g.name FROM GR_Names AS g";
			//if (restrictAnalysisToTaxonIDs != null && restrictAnalysisToTaxonIDs.size() > 0)
			//	query += ", GR_Origin as o WHERE o.taxon IN " +
			//	restrictAnalysisToTaxonIDs.toString().replaceFirst("\\[", "(").replaceFirst("\\]", ")");
			query += " , GR_Origin as o WHERE g.id=o.id AND o.taxon="+taxon;
			
			resultSet = statement.executeQuery(query);
			//ResultSet resultSet = statement.executeQuery("SELECT gene.ID, gene.name FROM GR_Names gene");
			while(resultSet.next()){
				Integer geneId = resultSet.getInt(1);
				String geneName = resultSet.getString(2);
				//String proteinName = normalize(resultSet.getString(3));
	
	//			if (name2geneIdCount.containsKey(geneName)) {
	//				int ids = name2geneIdCount.get(geneName) + 1;
	//				name2geneIdCount.put(geneName, ids);
	//			} else
	//				name2geneIdCount.put(geneName, 1);
	
	//			geneIds = name2geneIdCount.get(proteinName);
	//			if(geneIds==null){
	//				geneIds = new HashSet<Integer>();
	//				name2geneIdCount.put(proteinName, geneIds);
	//			}
	//			geneIds.add(geneId);
				
				System.out.println(geneId + "\t" + geneName);
			}

			query = "SELECT g.ID, g.name FROM GR_ProteinNames AS g";
			//if (restrictAnalysisToTaxonIDs != null && restrictAnalysisToTaxonIDs.size() > 0)
			//	query += ", GR_Origin as o WHERE o.taxon IN " +
			//	restrictAnalysisToTaxonIDs.toString().replaceFirst("\\[", "(").replaceFirst("\\]", ")");
			query += " , GR_Origin as o WHERE g.id=o.id AND o.taxon="+taxon;
			
			resultSet = statement.executeQuery(query);
			while(resultSet.next()){
				Integer geneId = resultSet.getInt(1);
				//String geneName = normalize(resultSet.getString(2));
				String proteinName = resultSet.getString(2);

//			Set<Integer> geneIds = name2geneIdCount.get(geneName);
//			if(geneIds==null){
//				geneIds = new HashSet<Integer>();
//				name2geneIdCount.put(geneName, geneIds);
//			}
//			geneIds.add(geneId);

//			if (name2geneIdCount.containsKey(proteinName)) {
//				int ids = name2geneIdCount.get(proteinName) + 1;
//				name2geneIdCount.put(proteinName, ids);
//			} else
//				name2geneIdCount.put(proteinName, 1);
			
				System.out.println(geneId + "\t" + proteinName);
			}
		}// for all taxa
		
		resultSet.close();
		statement.close();
		connection.close();

//		int averageIdsPerName = 0;
//		for (String name : name2geneIdCount.keySet()) {
//			Integer idCount = name2geneIdCount.get(name);
//			averageIdsPerName += idCount;
//        }

		//System.out.println( "Average Gene IDs per Name: "+ ((double)averageIdsPerName / (double)name2geneIdCount.size()) );

		//System.out.println("finished");
	}
	


	public static void computeAverageGeneIdsPerName() throws SQLException
	{
		Connection connection = DriverManager.getConnection("jdbc:mysql://wanaheim:3306/user_cplake", "cplake", "cplake");

		Map<String, Set<Integer>> name2geneIdCount = new HashMap<String, Set<Integer>>();

		Statement statement = connection.createStatement();

		ResultSet resultSet = statement.executeQuery("SELECT gene.ID, gene.name, protein.name FROM GR_Names gene, GR_ProteinNames protein");
		//ResultSet resultSet = statement.executeQuery("SELECT gene.ID, gene.name FROM GR_Names gene");
		while(resultSet.next()){
			Integer geneId = resultSet.getInt(1);
			String geneName = resultSet.getString(2);
			String proteinName = resultSet.getString(3);

			Set<Integer> geneIds = name2geneIdCount.get(geneName);
			if(geneIds==null){
				geneIds = new HashSet<Integer>();
				name2geneIdCount.put(geneName, geneIds);
			}
			geneIds.add(geneId);

			geneIds = name2geneIdCount.get(proteinName);
			if(geneIds==null){
				geneIds = new HashSet<Integer>();
				name2geneIdCount.put(proteinName, geneIds);
			}
			geneIds.add(geneId);
		}
		resultSet.close();
		statement.close();
		connection.close();

		int averageIdsPerName = 0;
		for (String name : name2geneIdCount.keySet()) {
			Integer idCount = name2geneIdCount.get(name).size();
			averageIdsPerName += idCount;
        }

		System.out.println( "Average Gene IDs per Name: "+ ((double)averageIdsPerName / (double)name2geneIdCount.size()) );

		System.out.println("finished");
	}


	public static void computeAverageSpeciesPerName() throws SQLException
	{
		Connection connection = DriverManager.getConnection("jdbc:mysql://wanaheim:3306/user_cplake", "cplake", "cplake");

		Map<String, Set<Integer>> name2taxIdCount = new HashMap<String, Set<Integer>>();

		Statement statement = connection.createStatement();

		ResultSet resultSet = statement.executeQuery("SELECT origin.taxon, gene.name FROM GR_Origin origin, GR_Names gene WHERE origin.ID=gene.ID");
		//ResultSet resultSet = statement.executeQuery("SELECT origin.taxon, gene.name, protein.name FROM GR_Origin origin, GR_Names gene, GR_ProteinNames protein");
		while(resultSet.next()){
			Integer taxId = resultSet.getInt(1);
			String geneName = resultSet.getString(2);
			//String proteinName = resultSet.getString(3);

			Set<Integer> taxIds = name2taxIdCount.get(geneName);
			if(taxIds==null){
				taxIds = new HashSet<Integer>();
				name2taxIdCount.put(geneName, taxIds);
			}
			taxIds.add(taxId);

//			taxIds = name2taxIdCount.get(proteinName);
//			if(taxIds==null){
//				taxIds = new HashSet<Integer>();
//				name2taxIdCount.put(proteinName, taxIds);
//			}
//			taxIds.add(taxId);

		}
		resultSet.close();
		statement.close();
		connection.close();

		int averageIdsPerName = 0;
		for (String name : name2taxIdCount.keySet()) {
			Integer idCount = name2taxIdCount.get(name).size();
			averageIdsPerName += idCount;
        }

		System.out.println( "Average Taxa per Name: "+ ((double)averageIdsPerName / (double)name2taxIdCount.size()) );

		System.out.println("finished");
	}
}
