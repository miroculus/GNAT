package gnat.tests;

import gnat.ISGNProperties;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Abstract class that provides core functions to test entire text processing
 * pipelines.
 * 
 * 
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 *
 */
public abstract class PipelineTest {

	
	
	/**
	 * Checks basic settings in the configuration required for a database connection 
	 * (dbUser, dbAccessUrl, ...).<br>
	 * Checks whether a connection to the DB can be established, and whether a few
	 * mandatory tables exists.
	 * 
	 * @return false if any of the tests failed
	 */
	static boolean testDatabase () {
		boolean configOk = true;
		
		String dbUser = ISGNProperties.get("dbUser");
		String dbPass = ISGNProperties.get("dbPass");
		String dbAccessUrl = ISGNProperties.get("dbAccessUrl");
		String dbDriver = ISGNProperties.get("dbDriver");
		
		if (dbUser == null || dbPass == null || dbAccessUrl == null || dbDriver == null ||
			dbUser.length() == 0 || dbPass.length() == 0 || dbAccessUrl.length() == 0 || dbDriver.length() == 0) {
			System.err.println("Configuration file " + ISGNProperties.getPropertyFilename() + ":\nSet values " +
					"for the entries dbUser/dbPass/dbAccessUrl/dbDriver to reflect your local configuration.");
			configOk = false;
		}
		
		System.out.println("#Testing database at " + dbAccessUrl);
		
		// check DB connectivity / queries
		try {
			Class.forName(ISGNProperties.get("dbDriver"));
			Connection conn = DriverManager.getConnection(
					ISGNProperties.get("dbAccessUrl"),
					ISGNProperties.get("dbUser"),
					ISGNProperties.get("dbPass"));
			Statement stm = conn.createStatement();
			ResultSet rs = stm.executeQuery("DESCRIBE GR_Origin");
			// the following fields have to be present in the GR_Origin table (for others we don't care):
			Set<String> expectedFields = new HashSet<String>();
			expectedFields.add("ID");
			expectedFields.add("taxon");
			while (rs.next()) {
				String fieldName = rs.getString("Field");
				expectedFields.remove(fieldName);
			}
			// succcess if all required fields could be found in that table
			if (expectedFields.size() == 0) {
				System.out.println("#Test: database test ok.");
			} else {
				System.out.println("#Test: database test failed -- missing table or fields!");
				System.out.println("#Test: requiring table `GR_Origin` with fields `ID` and `taxon`");
				configOk = false;
			}
			
		} catch (SQLException sqle) {
			System.err.println("#Test: database test failed:");
			System.err.println(sqle.getMessage());
			configOk = false;
		} catch (ClassNotFoundException e) {
			System.err.println("#Test: database test failed:");
			System.err.println("Failed to load the database driver '" + ISGNProperties.get("dbDriver") + "'");
			configOk = false;
		}

		//if (configOk)
		//	System.out.println("#Database test successful.");

		return configOk;
	}
	
	


	/**
	 * Read the expected results from an existing file.
	 * @param filename
	 * @return all lines from <tt>filename</tt>, expect comments and empty lines
	 */
	static List<String> getExpectedOutput (String filename) {
		List<String> result = new LinkedList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#") || line.length() == 0 || line.matches("[\\s\\t]+")) continue;
				result.add(line);
			}
			br.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return result;
	}
}
