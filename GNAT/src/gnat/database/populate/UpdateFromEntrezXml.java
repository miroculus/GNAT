package gnat.database.populate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * Update the GNAT database gene repository from XML records for each gene.
 * <br/><br/>
 * Also see {@link gnat.representation.GeneRepository}.
 * 
 * @author Joerg Hakenberg
 */
public class UpdateFromEntrezXml {

	
/* From Entrez Gene XML using the gene ID:
 CREATE ALGORITHM=UNDEFINED DEFINER=`hakenj01`@`%` SQL SECURITY DEFINER VIEW `gene_summary_view`
AS SELECT
   `gene_xml`.`gene_id` AS `gene_id`,extractvalue(`gene_xml`.`xml`,'/Entrezgene/Entrezgene_summary') AS `summary`
FROM `gene_xml`;
 */
	
	public static String DB_DRIVER  = "org.gjt.mm.mysql.Driver";
	public static String DB_CONNECT = "jdbc:mysql://db2.hpc.mssm.edu:3306/kb_NCBI"; // = ISGNProperties.get("dbAccessUrl");
	public static String DB_USER    = "hakenj01"; // = ISGNProperties.get("dbUser");
	public static String DB_PASS    = "mypass7";  // = ISGNProperties.get("dbPass");
	//public static String DB_        = "";
	public static String DB_SCHEMA_ENTREZGENE  = "kb_NCBI";
	public static String DB_SCHEMA_UNIPROT     = "kb_uniprot";
	public static String DB_GENE_RAW_XML       = "gene_xml";
	public static String DB_DESTINATION_SCHEMA = "var_VarImpact";
	
	
	/**
	 * 
	 * @param args
	 */
	public static void main (String[] args) throws SQLException, ClassNotFoundException {
		
		Class.forName(DB_DRIVER);
		Connection conn = DriverManager.getConnection(DB_CONNECT, DB_USER, DB_PASS);
		
		Statement statement1 = conn.createStatement();
		ResultSet rs_gene_ids = statement1.executeQuery("SELECT gene_id FROM " + DB_SCHEMA_ENTREZGENE + "." + DB_GENE_RAW_XML + " where gene_id>=7310 ORDER BY gene_id ASC");
		
		Statement statement2 = conn.createStatement();
		Statement statement4 = conn.createStatement();
		while (rs_gene_ids.next()) {
			int gene_id = rs_gene_ids.getInt("gene_id");
			ResultSet terms = statement2.executeQuery(
					  "SELECT `gene_id` AS `gene_id`,"
					+ " extractvalue(`xml`, '/Entrezgene/Entrezgene_summary') AS `summary`"
					+ " FROM `" + DB_GENE_RAW_XML + "`"
					+ " WHERE `gene_id` = " + gene_id);
			if (terms.first() && terms.getString("summary") != null) {
				
				String summary = terms.getString("summary").replaceFirst("\\s?\\[[Pp]rovided by .+?\\]$", "");
				System.out.println(terms.getInt("gene_id") + "\t" + summary);
				if (summary != null && summary.length() > 1)
					statement4.execute("REPLACE INTO `" + DB_DESTINATION_SCHEMA + "`.GR_Summary (`ID`, `summary`) VALUES (" + gene_id + ", \"" + summary + "\")");
			}
				
			//if (terms.getInt("gene_id") > 10) break;
			//if (gene_id > 50) break;
		}
		
		rs_gene_ids.close();
		statement1.close();
		statement2.close();
		statement4.close();
		conn.close();
		
	}
	
	
	/**
	 * Update the Entrez Gene Summary table.
	 * @param conn
	 * @throws SQLException
	 */
	public void updateSummary (Connection conn) throws SQLException {
		Statement statement1 = conn.createStatement();
		ResultSet rs_gene_ids = statement1.executeQuery("SELECT `gene_id` FROM " + DB_SCHEMA_ENTREZGENE + "." + DB_GENE_RAW_XML + " ORDER BY gene_id ASC");
		
		Statement statement2 = conn.createStatement();
		Statement statement4 = conn.createStatement();
		while (rs_gene_ids.next()) {
			int gene_id = rs_gene_ids.getInt("gene_id");
			ResultSet terms = statement2.executeQuery(
					  "SELECT `gene_id` AS `gene_id`,"
					+ " extractvalue(`xml`, '/Entrezgene/Entrezgene_summary') AS `summary`"
					+ " FROM `" + DB_GENE_RAW_XML + "`"
					+ " WHERE `gene_id` = " + gene_id);
			if (terms.first() && terms.getString("summary") != null) {
				
				String summary = terms.getString("summary").replaceFirst("\\s?\\[[Pp]rovided by .+?\\]$", "");
				System.out.println(terms.getInt("gene_id") + "\t" + summary);
				if (summary != null && summary.length() > 1)
					statement4.execute("REPLACE INTO `" + DB_DESTINATION_SCHEMA + "`.GR_Summary (`ID`, `summary`) VALUES (" + gene_id + ", \"" + summary + "\")");
			}
				
			//if (terms.getInt("gene_id") > 10) break;
			//if (gene_id > 50) break;
		}
		
		rs_gene_ids.close();
		statement1.close();
		statement2.close();
		statement4.close();
	}
}
