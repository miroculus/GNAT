package gnat.database.populate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class UpdateFromUniprotXml {

	public static String DB_DRIVER  = "org.gjt.mm.mysql.Driver";
	public static String DB_CONNECT = "jdbc:mysql://db2.hpc.mssm.edu:3306/var_VarImpact"; // = ISGNProperties.get("dbAccessUrl");
	public static String DB_USER    = "hakenj01"; // = ISGNProperties.get("dbUser");
	public static String DB_PASS    = "mypass7";  // = ISGNProperties.get("dbPass");
	//public static String DB_        = "";
	public static String DB_SCHEMA_ENTREZGENE  = "kb_NCBI";
	public static String DB_SCHEMA_UNIPROT     = "kb_uniprot";
	public static String DB_GENE_RAW_XML       = "gene_xml";
	public static String DB_UNIPROT_RAW_XML    = "raw_xml";
	public static String DB_DESTINATION_SCHEMA = "var_VarImpact";

	/* get UniProt Acc for Gene ID:
SELECT
`gene_xml`.`gene_id` AS `gene_id`,
extractvalue(`gene_xml`.`xml`,'//Gene-commentary/Gene-commentary_products/Gene-commentary/Gene-commentary_source/Other-source/Other-source_src/Dbtag[Dbtag_db="UniProtKB/Swiss-Prot"]/Dbtag_tag/Object-id/Object-id_str/text()') AS `uacc`
FROM `gene_xml` ; -- where gene_id=1;
*/
	

/*
From UniProt using the UniProt accession ("P12345"):
CREATE ALGORITHM=UNDEFINED DEFINER=`hakenj01`@`%` SQL SECURITY DEFINER VIEW `v_annotation_function`
AS SELECT
   `raw_xml`.`acc` AS `acc`,extractvalue(`raw_xml`.`xml`,'/entry/comment[@type="function"]/text/text()') AS `function`
FROM `raw_xml`;
*/

	
	/**
	 * 
	 * @param args
	 */
	public static void main (String[] args) throws SQLException, ClassNotFoundException {
		
		Class.forName(DB_DRIVER);
		Connection conn = DriverManager.getConnection(DB_CONNECT, DB_USER, DB_PASS);
		
		Map<Integer, Set<String>> gene_to_proteins = new LinkedHashMap<Integer, Set<String>>();
		
		// get all gene IDs
		//
		//ResultSet rs_gene_ids = statement1.executeQuery("SELECT gene_id FROM " + DB_SCHEMA_ENTREZGENE + "." + DB_GENE_RAW_XML + " ORDER BY gene_id ASC");

		// get UniProt Accession codes for each gene ID
		Statement statement1 = conn.createStatement();
		ResultSet rs_id_map = statement1.executeQuery("SELECT "
				+ " `gene_xml`.`gene_id` AS `gene_id`, "
				+ " extractvalue(`gene_xml`.`xml`,'//Gene-commentary/Gene-commentary_products/Gene-commentary/Gene-commentary_source/Other-source/Other-source_src/Dbtag[Dbtag_db=\"UniProtKB/Swiss-Prot\"]/Dbtag_tag/Object-id/Object-id_str/text()') AS `uacc`"
				+ " FROM `" + DB_SCHEMA_ENTREZGENE + "`.`gene_xml`"
				//+ " WHERE gene_id <= 25"
				+ " ORDER BY `gene_id` ASC");
		while (rs_id_map.next()) {
			int gene_id = rs_id_map.getInt("gene_id");
			String uacc = rs_id_map.getString("uacc");
			Set<String> uaccs = null;
			if (gene_to_proteins.containsKey(gene_id))
				uaccs = gene_to_proteins.get(gene_id);
			else
				uaccs = new HashSet<String>();
			uaccs.add(uacc);
			gene_to_proteins.put(gene_id, uaccs);
		}
		rs_id_map.close();
		
		//updateProteinFunction(conn, gene_to_proteins);
		//updateProteinDisease(conn, gene_to_proteins);
		updateProteinKeywords(conn, gene_to_proteins);
		
		
		
//		Statement statement4 = conn.createStatement();
//		while (rs_gene_ids.next()) {
//			int gene_id = rs_gene_ids.getInt("gene_id");
//			ResultSet terms = statement2.executeQuery(
//					  "SELECT `gene_id` AS `gene_id`,"
//					+ " extractvalue(`xml`, '/Entrezgene/Entrezgene_summary') AS `summary`"
//					+ " FROM `" + DB_GENE_RAW_XML + "`"
//					+ " WHERE `gene_id` = " + gene_id);
//			if (terms.first() && terms.getString("summary") != null) {
//				
//				String summary = terms.getString("summary").replaceFirst("\\s?\\[[Pp]rovided by .+?\\]$", "");
//				System.out.println(terms.getInt("gene_id") + "\t" + summary);
//				if (summary != null && summary.length() > 1)
//					statement4.execute("REPLACE INTO `" + DB_DESTINATION_SCHEMA + "`.GR_Summary (`ID`, `summary`) VALUES (" + gene_id + ", \"" + summary + "\")");
//			}
//				
//			//if (terms.getInt("gene_id") > 10) break;
//			//if (gene_id > 50) break;
//		}
//		
//		rs_gene_ids.close();
		rs_id_map.close();
		statement1.close();
		conn.close();
		
	}

	
	/**
	 * 
	 * @param conn
	 * @param gene_to_proteins
	 * @throws SQLException
	 */
	public static void updateProteinKeywords (Connection conn, Map<Integer, Set<String>> gene_to_proteins) throws SQLException  {
		Statement statement2 = conn.createStatement();
		Statement statement3 = conn.createStatement();
		Statement statement4 = conn.createStatement();
		for (int gene_id : gene_to_proteins.keySet()) {
			System.out.println(gene_id);
			Set<String> all_terms = new LinkedHashSet<String>();
			Set<String> uaccs = gene_to_proteins.get(gene_id);
			for (String uacc : uaccs) {
				ResultSet rs_keywords = statement2.executeQuery(
//					  "SELECT `acc` AS `acc`,"
//					+ " extractvalue(`xml`, '/entry/comment[@type=\"disease\"]/disease/name"
//					+ "|/entry/comment[@type=\"disease\"]/disease/acronym"
//					+ "|/entry/comment[@type=\"disease\"]/disease/description"
//					+ "|/entry/comment[@type=\"disease\"]/text') AS `disease`"
//					+ " FROM `" + DB_SCHEMA_UNIPROT + "`.`raw_xml` WHERE acc=\"" + uacc + "\"");
						"SELECT `acc`, EXTRACTVALUE_ALL ("
						        + "(SELECT `xml` FROM `" + DB_SCHEMA_UNIPROT + "`.`raw_xml`"
								+ " WHERE acc=\"" + uacc + "\"),"
								+ " '//keyword', '|\n') as `keyword`"
								+ " FROM `" + DB_SCHEMA_UNIPROT + "`.`raw_xml` WHERE acc=\"" + uacc + "\""
					);
				while (rs_keywords.next()) {
					String t = rs_keywords.getString("keyword");
					//if (t != null && t.length() > 0 && !t.equalsIgnoreCase("Not known.") && !t.equalsIgnoreCase("Not yet known."))
						all_terms.add(t);
				}
			}
			if (all_terms.size() > 0) {
				statement3.execute("DELETE FROM var_VarImpact.GR_ProteinKeywords WHERE ID=" + gene_id);
				for (String t : all_terms) {
					t = t.replaceAll("\"", "''");
					String[] ks = t.split("\\|\n");
					for (String k : ks) {
						statement4.execute("INSERT IGNORE INTO `" + DB_DESTINATION_SCHEMA + "`.GR_ProteinKeywords"
							+ " (`ID`, `keyword`)"
							+ " VALUES (" + gene_id + ", \"" + k + "\")");
						//System.out.println(gene_id + "\t" + uaccs + "\t" + k);
					}
				}
			}
		}
		statement2.close();
		statement3.close();
		statement4.close();
	}
	
	
	/**
	 * 
	 * @param conn
	 * @param gene_to_proteins
	 * @throws SQLException
	 */
	public static void updateProteinDisease (Connection conn, Map<Integer, Set<String>> gene_to_proteins) throws SQLException  {
		Statement statement2 = conn.createStatement();
		Statement statement3 = conn.createStatement();
		Statement statement4 = conn.createStatement();
		for (int gene_id : gene_to_proteins.keySet()) {
			Set<String> all_terms = new LinkedHashSet<String>();
			Set<String> uaccs = gene_to_proteins.get(gene_id);
			for (String uacc : uaccs) {
				ResultSet rs_function_terms = statement2.executeQuery(
					  "SELECT `acc` AS `acc`,"
					+ " extractvalue(`xml`, '/entry/comment[@type=\"disease\"]/disease/name"
					+ "|/entry/comment[@type=\"disease\"]/disease/acronym"
					+ "|/entry/comment[@type=\"disease\"]/disease/description"
					+ "|/entry/comment[@type=\"disease\"]/text') AS `disease`"
					+ " FROM `" + DB_SCHEMA_UNIPROT + "`.`raw_xml` WHERE acc=\"" + uacc + "\"");
				while (rs_function_terms.next()) {
					String t = rs_function_terms.getString("disease");
					//if (t != null && t.length() > 0 && !t.equalsIgnoreCase("Not known.") && !t.equalsIgnoreCase("Not yet known."))
						all_terms.add(t);
				}
			}
			if (all_terms.size() > 0) {
				statement3.execute("DELETE FROM var_VarImpact.GR_ProteinDisease WHERE ID=" + gene_id);
				for (String t : all_terms) {
					t = t.replaceAll("\"", "''");
					statement4.execute("INSERT IGNORE INTO `" + DB_DESTINATION_SCHEMA + "`.GR_ProteinDisease (`ID`, `disease`) VALUES (" + gene_id + ", \"" + t + "\")");
					//System.out.println(gene_id + "\t" + uaccs + "\t" + t);
				}
			}
		}
		statement2.close();
		statement3.close();
		statement4.close();
	}
	
	
	/**
	 * 
	 * @param conn
	 * @param gene_to_proteins
	 * @throws SQLException
	 */
	public static void updateProteinFunction (Connection conn, Map<Integer, Set<String>> gene_to_proteins) throws SQLException {
		Statement statement2 = conn.createStatement();
		Statement statement3 = conn.createStatement();
		Statement statement4 = conn.createStatement();
		for (int gene_id : gene_to_proteins.keySet()) {
			Set<String> all_terms = new LinkedHashSet<String>();
			Set<String> uaccs = gene_to_proteins.get(gene_id);
			for (String uacc : uaccs) {
				ResultSet rs_function_terms = statement2.executeQuery(
					  "SELECT `acc` AS `acc`,"
					+ " extractvalue(`xml`,'/entry/comment[@type=\"function\"]/text/text()') AS `function`"
					+ " FROM `" + DB_SCHEMA_UNIPROT + "`.`raw_xml` WHERE acc=\"" + uacc + "\"");
				while (rs_function_terms.next()) {
					String t = rs_function_terms.getString("function");
					if (t != null && t.length() > 0 && !t.equalsIgnoreCase("Not known.") && !t.equalsIgnoreCase("Not yet known."))
						all_terms.add(t);
				}
			}
			if (all_terms.size() > 0) {
				statement3.execute("DELETE FROM var_VarImpact.GR_ProteinFunction WHERE ID=" + gene_id);
				for (String t : all_terms) {
					t = t.replaceAll("\"", "''");
					statement4.execute("INSERT IGNORE INTO `" + DB_DESTINATION_SCHEMA + "`.GR_ProteinFunction (`ID`, `function`) VALUES (" + gene_id + ", \"" + t + "\")");
					System.out.println(gene_id + "\t" + t);
				}
			}
		}
	}


}
