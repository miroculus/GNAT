package gnat.database.populate;

import gnat.ISGNProperties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class FillGoTerms {

	public static void main (String[] args) throws SQLException, ClassNotFoundException {
		Class.forName("org.gjt.mm.mysql.Driver");
		Connection conn = DriverManager.getConnection(
				"jdbc:mysql://db2.ib.hpc.mssm.edu:3306/kb_NCBI",//ISGNProperties.get("dbAccessUrl"),
				"hakenj01",//ISGNProperties.get("dbUser"),
				"mypass7"//ISGNProperties.get("dbPass")
		);
		Statement statement1 = conn.createStatement();
		Statement statement2 = conn.createStatement();
		//Statement statement3 = conn.createStatement();
		Statement statement4 = conn.createStatement();
		
		
		//356 357 36 37
		
		ResultSet rs_gene_ids = statement1.executeQuery("SELECT DISTINCT gene_id FROM kb_NCBI.gene_xml ORDER BY gene_id ASC");
		while (rs_gene_ids.next()) {
			int gene_id = rs_gene_ids.getInt("gene_id");
			System.out.println(gene_id);
			
			ResultSet rs = statement2.executeQuery("CALL kb_NCBI.getGOterms( (select xml from gene_xml where gene_id = " + gene_id + ") )");
			if (rs.next()) {
				//String term = rs.getString("term").replaceAll("\"", "'");
				//System.out.println(term);
				
				//statement3.execute("DELETE FROM var_VarImpact.GR_GOTerm WHERE ID = " + gene_id);
				//if (!term.equalsIgnoreCase("biological_process") && !term.equalsIgnoreCase("molecular_function") && !term.equalsIgnoreCase("cellular_component"))
				//	statement4.execute("INSERT IGNORE INTO var_VarImpact.GR_GOTerm (ID, term) VALUES (" + gene_id + ", \"" + term + "\")");
				
				rs.beforeFirst();
				while (rs.next()) {
					String term = rs.getString("term").replaceAll("\"", "'");
					//System.out.println(rs.getString("term"));
					if (!term.equalsIgnoreCase("biological_process") && !term.equalsIgnoreCase("molecular_function") && !term.equalsIgnoreCase("cellular_component"))
						statement4.execute("INSERT IGNORE INTO var_VarImpact.GR_GOTerm (ID, term) VALUES (" + gene_id + ", \"" + term + "\")");
				}
				
			}

			rs.close();
			
			//if (gene_id > 10) break;
		}
		
		rs_gene_ids.close();
		conn.close();
		
	}
	
}
