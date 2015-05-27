// (c)2006 Transinsight GmbH - www.transinsight.com - All rights reserved.
package gnat.database.go;

import gnat.ISGNProperties;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * TODO describe me
 *
 */
public class ImportGO2GO
{

	/**
	 * TODO describe me!
	 *
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException
	{
		
		Class.forName(ISGNProperties.get("dbDriver"));
		Connection connection = DriverManager.getConnection(ISGNProperties.get("dbAccessUrl"),
				ISGNProperties.get("dbUser"), ISGNProperties.get("dbPass"));
		Statement statement = connection.createStatement();

		BufferedReader reader = new BufferedReader(new FileReader(new File("./GeneOntology_GO2GO.csv")));
		String line;
		while((line=reader.readLine())!=null){
			String[] data = line.split(";");

			String parent = data[0];
			String child = data[1];

			if(parent.startsWith("obsolete") || child.startsWith("obsolete")){
				continue;
			}

			Integer distance = Integer.parseInt(data[2]);

			Integer parentId = null;
			Integer childId = null;

			if(parent.equals("all")){
				parentId = 0;
			}
			else{
				parentId = Integer.parseInt(parent.substring(3));
			}

			childId = Integer.parseInt(child.substring(3));

			statement.execute("INSERT INTO GeneOntology_GO2GO (parent, child, distance) VALUES ("+parentId+","+childId+","+distance+")");

		}

		reader.close();
		statement.close();
		connection.close();
	}

}
