package gnat.database.go;

import gnat.ISGNProperties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;

/**
 * 
 *
 * Call:
 * java -classpath bin:lib/mysql-connector-java-5.0.3-bin.jar de.tud.biotec.databases.go.GetLCAUsingSQL
 * java -classpath bin:lib/mysql-connector-java-5.0.3-bin.jar -Xmx1500M de.tud.biotec.databases.go.GetLCAUsingSQL > output
 *
 *
 * @author Joerg Hakenberg
 *
 */

public class GetLCAUsingSQL {

	public static void writeSQLCommands (int from, int to, int len) {
		
		for (int i = from; i < (to+1); i++) {
			String suffix = String.valueOf(i);
			while (suffix.length() < len)
				suffix = "0" + suffix;
			
			System.out.println(
					  "INSERT INTO `GeneOntology_CommonParents` (\n"
					+ "  SELECT g1.parent as parent, g1.child as child1, g2.child as child2,"
					+ "    g1.distance as dist1, g2.distance as dist2\n"
					+ "  FROM\n"
					+ "    `GeneOntology_GO2GO` as g1, `GeneOntology_GO2GO` as g2\n"
					+ "  WHERE\n"
					+ "    g1.child LIKE \"%" + suffix + "\"\n"
					+ "    AND g1.child != g2.child\n"
					+ "    AND g1.parent = g2.parent\n"
					+ "  );");
			System.out.println();
			
		}
	}
	
	
	public static void main (String[] args) {

		writeSQLCommands(Integer.parseInt(args[0]),
				Integer.parseInt(args[1]),
				Integer.parseInt(args[2]));
		System.exit(0);
		
		//Vector<String> result = new Vector<String>();
		Calendar nowCal;
		Date nowDate;
		
		Connection connection = null;
		Statement statement = null;
		ResultSet resultset = null;
		try {
			
			
			Class.forName(ISGNProperties.get("dbDriver"));
			connection = DriverManager.getConnection(
					ISGNProperties.get("dbAccessUrl"),
					ISGNProperties.get("dbUser"), ISGNProperties.get("dbPass"));
			statement = connection.createStatement();
			
			for (int i = 6; i < 0011; i++) {
				nowCal = Calendar.getInstance();
				nowDate = nowCal.getTime();
				String suffix = String.valueOf(i);
				while (suffix.length() < 4)
					suffix = "0" + suffix;
				System.err.println("# Getting suffix " + suffix + " at " + nowDate.toString());
				
				/*resultset = statement.executeQuery(
						"SELECT g1.parent AS parent, g1.child AS child1, g2.child AS child2, g1.distance AS dist1, g2.distance AS dist2"
						+ "  FROM " + dbGo2GoTable + " AS g1, "
						+ dbGo2GoTable + " AS g2"
						+ "  WHERE g1.child LIKE \"%" + suffix + "\""
						+ "  AND g1.child != g2.child"
						+ "  AND g1.parent = g2.parent");*/
				
				//resultset = statement.executeQuery(
				statement.executeUpdate(
						  "INSERT INTO `GeneOntology_CommonParents` ("
						+ "  SELECT g1.parent as parent, g1.child as child1, g2.child as child2,"
						+ "    g1.distance as dist1, g2.distance as dist2"
						+ "  FROM"
						+ "    `GeneOntology_GO2GO` as g1, `GeneOntology_GO2GO` as g2"
						+ "  WHERE"
						+ "    g1.child LIKE \"%" + suffix + "\""
						+ "    AND g1.child != g2.child"
						+ "    AND g1.parent = g2.parent"
				);

				/*if (resultset.first()) {
					String res = resultset.getString("parent")
						+ ";" + resultset.getString("child1")
						+ ";" + resultset.getString("child2")
						+ ";" + resultset.getString("dist1")
						+ ";" + resultset.getString("dist2");
					System.out.println(res);
					//result.add(res);
					while (resultset.next()) {
						res = resultset.getString("parent")
							+ ";" + resultset.getString("child1")
							+ ";" + resultset.getString("child2")
							+ ";" + resultset.getString("dist1")
							+ ";" + resultset.getString("dist2");
						System.out.println(res);
						//result.add(res);
					}
				}*/
			} // for suffixes 001..999
			
		} catch (java.sql.SQLException sqle) {
			sqle.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (resultset != null) resultset.close();
			} catch (Exception e) { }
			try {
				if (null != statement) statement.close();
			} catch (Exception e) { }
			try {
				if (connection != null) connection.close();
			} catch (Exception e) { }
		}
		
		/*nowCal = Calendar.getInstance();
		nowDate = nowCal.getTime();
		System.err.println("# Starting output at " + nowDate.toString());
		for (int i = 0; i < result.size(); i++)
			System.out.println(result.get(i));*/
		
	}
	
	static String dbCommonParentTable = "`GeneOntology_CommonParents`";
	static String dbGo2GoTable = "`GeneOntology_GO2GO`";
	
}
