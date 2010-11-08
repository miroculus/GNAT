package gnat.database.go;

import gnat.ISGNProperties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

/**
 *
 * Handles the connection to the Gene Ontology database. Provides some standard queries.
 *
 *
 *
 * @author Joerg Hakenberg
 *
 */

public class GOAccess {


	public GOAccess () {
		this(ISGNProperties.get("dbDriver"), ISGNProperties.get("dbAccessUrl"),
				ISGNProperties.get("dbUser"), ISGNProperties.get("dbPass"), true);
	}


	/**
	 * This constructor opens a connection to the DB immediately.
	 *
	 */
	public GOAccess (String driver, String url, String user, String pass) {
		this(driver, url, user, pass, true);
	}


	/**
	 * This constructor lets you decide whether to immediately open a connection to
	 * the database or do it later on (using <code>open()</code>).
	 *
	 * @param immediateOpen - if true, immediately opens a connection
	 */
	public GOAccess (String driver, String url, String user, String pass, boolean immediateOpen) {
		if (immediateOpen)
			openConnection = open(driver, url, user, pass);
	}


	/**
	 * Closes all open statements and connections.
	 * @return
	 */
	public boolean close () {
		try {
			//if (resultset != null) resultset.close();
			if (userStatement != null) userStatement.close();
			userStatement = null;
			if (userConnection != null) userConnection.close();
			userConnection = null;
			//
		//	if (goStatement != null) goStatement.close();
		//	if (goConnection != null) goConnection.close();
		} catch (java.sql.SQLException sqle) {
			return false;
		}
		openConnection = false;
		return true;
	}


	/**
	 * Returns the direct (closest) distance between two GO terms (accession numbers) in
	 * the ontology tree.
	 *
	 * @param acc1
	 * @param acc2
	 * @return
	 */
	public int getDirectDistance (String acc1, String acc2) {
		if (!isGOAccessionNumber(acc1))
			acc1 = makeGOAccessionNumber(acc1);
		if (!isGOAccessionNumber(acc2))
			acc2 = makeGOAccessionNumber(acc2);

		if (acc1.equals(acc2)) return 0;

		int directDistance = Integer.MAX_VALUE;

		try {
			userResultset = userStatement.executeQuery("SELECT * FROM " + dbGo2GoTable +
					" WHERE parent=\"" + acc1 + "\" AND child=\"" + acc2 + "\"");
			if (userResultset.first()) {
				directDistance = userResultset.getInt("distance");
				return directDistance;
			}
			userResultset = userStatement.executeQuery("SELECT * FROM " + dbGo2GoTable +
					" WHERE parent=\"" + acc2 + "\" AND child=\"" + acc1 + "\"");
			if (userResultset.first()) {
				directDistance = userResultset.getInt("distance");
				return directDistance;
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}

		return directDistance;
	}


	/**
	 * Calculates the distance of two GO terms (IDs).
	 *
	 * Formula:<pre>
	 *                            min.pathlen (g1, g2)
	 * dist(g1,g2) = --------------------------------------------
	 *                 max.depth(g1, g2) + min.pathlen (g1, g2)
	 * </pre>
	 *
	 * @param acc1
	 * @param acc2
	 * @return
	 */
	public float getDistance (String acc1, String acc2) {
//		if (!isGOAccessionNumber(acc1))
//			acc1 = makeGOAccessionNumber(acc1);
//		if (!isGOAccessionNumber(acc2))
//			acc2 = makeGOAccessionNumber(acc2);

		if (acc1.equals(acc2)) return 0.0f;

		//boolean foundLCA = false;
		float distance = Float.POSITIVE_INFINITY;

		boolean found = false;

		int directDistance = -1;
		int depthLCA = -1; // == depthParent
		int depthTerm1 = -1;
		int depthTerm2 = -1;


		if (verbosity > 3) {
			System.out.print("# " + acc1 + "/" + acc2 + " ");
		}
		/*
		 *
		 * IDEA:    directDistance / (depthLCA + depthTerm1 + depthTerm2)
		 *
		 *
		 */


		// if distance to GO:0005575 (cellular_component) <= 8, return 1.0f, high distance

		/*try {
			// first, check if one is a direct parent of the other
			// (in GeneOntology_GO2GO table)

			// term 1 parent of term?
			userResultset = userStatement.executeQuery("SELECT * FROM " + dbGo2GoTable +
					" WHERE parent=\"GO:0005575\" AND child=\"" + acc1 + "\"");
			if (userResultset.first()) {
				return 1.0f;
				//directDistance = userResultset.getInt("distance");
				//if (directDistance <= 8) return 1.0f;
			}
			userResultset = userStatement.executeQuery("SELECT * FROM " + dbGo2GoTable +
					" WHERE parent=\"GO:0005575\" AND child=\"" + acc2 + "\"");
			if (userResultset.first()) {
				return 1.0f;
				//directDistance = userResultset.getInt("distance");
				//if (directDistance <= 8) return 1.0f;
			}
		} catch (SQLException sqle) {
			//System.err.println("# SQLE");
			sqle.printStackTrace();
		}*/

		/*



		 direct = 0                 =>   dist = 0

		 direct = 1, down in tree   =>   dist = 0.1

		 direct = 1, up in tree     =>   dist = 0.9

		 direct = 5, down in tree   =>   dist = 0.3

		 direct = 5, up in tree     =>   dist = 0.7

		 direct = 9, down in tree   =>   dist = 0.4

		 direct = 9, up in tree     =>   dist = 0.6


		 */


		// catch SQLException, return POS_INFTY on any
		try {
			// first, check if one is a direct parent of the other
			// (in GeneOntology_GO2GO table)

			// term 1 parent of term?
			userResultset = userStatement.executeQuery("SELECT * FROM " + dbGo2GoTable +
					" WHERE parent=\"" + acc1 + "\" AND child=\"" + acc2 + "\"");
			if (userResultset.first()) {
				directDistance = userResultset.getInt("distance");

				// check info on parent term 1
				userResultset = userStatement.executeQuery("SELECT * FROM " + dbGo2GoTable +
						" WHERE parent=\"all\" AND child=\"" + acc1 + "\"");
				if (userResultset.first()) {
					depthTerm1 = userResultset.getInt("distance");
					depthTerm2 = depthTerm1 + directDistance;
					depthLCA = depthTerm1;
					found = true;
				}

			// term 2 parent of term 1?
			} else {
				if (verbosity > 3) {
					System.out.print(", no p/c");
				}

				userResultset = userStatement.executeQuery("SELECT * FROM " + dbGo2GoTable +
						" WHERE parent=\"" + acc2 + "\" AND child=\"" + acc1 + "\"");

				if (userResultset.first()) {
					directDistance = userResultset.getInt("distance");

					// check info on parent term 2
					userResultset = userStatement.executeQuery("SELECT * FROM " + dbGo2GoTable +
							" WHERE parent=\"all\" AND child=\"" + acc2 + "\"");
					if (userResultset.first()) {
						depthTerm2 = userResultset.getInt("distance");
						depthTerm1 = depthTerm2 + directDistance;
						depthLCA = depthTerm2;
						found = true;
					}
				} else
					if (verbosity > 3) {
						System.out.print(", no c/p");
					}
			}


			// no direct parent/child relation?
			if (!found) {

				// then they might be siblings and might have a common LCA (not ROOT!)

				// stored as term1 / term2 in the DB?
				userResultset = userStatement.executeQuery("SELECT * FROM " + dbLCAtable
						+ " WHERE goid1=\"" + acc1 + "\" AND goid2=\"" + acc2 + "\"");
				// get info on this tuple if it exists
				if (userResultset.first()) {
					depthLCA = userResultset.getInt("depthlca");

					int distanceFromLCATerm1 = userResultset.getInt("dist1");
					int distanceFromLCATerm2 = userResultset.getInt("dist2");
					directDistance = distanceFromLCATerm1 + distanceFromLCATerm2;
					depthTerm1 = depthLCA + distanceFromLCATerm1;
					depthTerm2 = depthLCA + distanceFromLCATerm2;
					found = true;

				} else {
					if (verbosity > 3) {
						System.out.print(", no c1/c2");
					}

					// no such tuple? then maybe stored as term2 / term1 in the DB...
					userResultset = userStatement.executeQuery("SELECT * FROM " + dbLCAtable
							+ " WHERE goid1=\"" + acc2 + "\" AND goid2=\"" + acc1 + "\"");
					// get info for this tuple if it exists
					if (userResultset.first()) {
						depthLCA = userResultset.getInt("depthlca");

						int distanceFromLCATerm2 = userResultset.getInt("dist1");
						int distanceFromLCATerm1 = userResultset.getInt("dist2");
						directDistance = distanceFromLCATerm1 + distanceFromLCATerm2;
						depthTerm1 = depthLCA + distanceFromLCATerm1;
						depthTerm2 = depthLCA + distanceFromLCATerm2;
						found = true;
					} else
						if (verbosity > 3) {
							System.out.print(", no c2/c1");
						}
				}
			} // if not found


			if (found) {

				distance = (float)directDistance /
					( (float)depthLCA + (float)depthTerm1 + (float)depthTerm2 );

			}


		} catch (SQLException sqle) {
			//System.err.println("# SQLE");
			sqle.printStackTrace();
		} catch (NullPointerException npe) {
			// DB not accessible?
			//npe.printStackTrace();
			System.err.println("#DB inaccessible?");
			//return Float.NEGATIVE_INFINITY;
		}

		if (verbosity > 3) {
			System.out.println(" => " + distance);
		}

		return distance;

	}


	/**
	 * Gets the minimal depth of a GO term (given by an accession number).
	 *
	 * @param acc
	 * @return minimal distance to ROOT (&quot;all&quot;
	 */
	public int getGOAccMinimalDepth (String acc) {
		if (!isGOAccessionNumber(acc))
			acc = makeGOAccessionNumber(acc);

		try {
			userResultset = userStatement.executeQuery("SELECT MIN(distance) FROM " + dbGo2GoTable
					+ " WHERE child=\"" + acc + "\" AND parent=\"all\"");
			if (userResultset.first()) {
				String res = userResultset.getString("MIN(distance)");
				return Integer.parseInt(res);

			} else
				return -1;

		} catch (java.sql.SQLException sqle) {
			sqle.printStackTrace();
			return -1;
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			return -1;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}


	/**
	 * Returns the first GO term that corresponds to the given accession number.
	 * Accession numbers start with "GO:" and then are followed by seven digits:
	 * "GO:0016563". In case <tt>acc</tt> does not fit this scheme, but is a
	 * number nevertheless, adds "GO:" and the necessary amount of zeros.
	 *
	 * @param acc
	 * @return
	 */
	public String getGOTermForAcc (String acc) {
		// check GO acc. number format
//		if (!isGOAccessionNumber(acc))
//			acc = makeGOAccessionNumber(acc);
//		String[] res = getGOTermsForAcc(acc);
//		if (res != null)
//			return res[0];
		return "--- no term found ---";
	}


	/**
	 * Returns all GO term that correspond to the given accession number.
	 * Accession numbers start with "GO:" and then are followed by seven
	 * digits: "GO:0016563". In case <tt>acc</tt> does not fit this scheme,
	 * but is a number nevertheless, adds "GO:" and the necessary amount of
	 * zeros.
	 * @param acc
	 * @return
	 */
	public String[] getGOTermsForAcc (String acc) {
		// check GO acc. number format
		if (!isGOAccessionNumber(acc))
			acc = makeGOAccessionNumber(acc);

		Vector<String> result = new Vector<String>();
		try {
			goResultset = goStatement.executeQuery("SELECT * FROM " + dbTableTerm +
					" WHERE acc=\"" + acc + "\"");
			if (goResultset.first()) {
				String res = goResultset.getString("name");
				result.add(res);
				while (goResultset.next()) {
					res = goResultset.getString("name");
					result.add(res);
				}
			} else
				return null;

		} catch (java.sql.SQLException sqle) {
			sqle.printStackTrace();
			return new String[]{null, null};
		} catch (Exception e) {
			e.printStackTrace();
			return new String[]{null, null};
		}

		String[] results = new String[result.size()];
		for (int i = 0; i < result.size(); i++)
			results[i] = result.get(i);
		return results;
	}


	/**
	 * Gets any parent GO acccession numbers for this accession number.
	 * @param acc
	 * @return
	 */
	public String[] getParentsForGOAccessionNumber (String acc) {
		if (!isGOAccessionNumber(acc))
			acc = makeGOAccessionNumber(acc);

		Vector<String> result = new Vector<String>();
		try {
			userResultset = userStatement.executeQuery("SELECT * FROM " + dbGo2GoTable +
					" WHERE child=\"" + acc + "\"");
			if (userResultset.first()) {
				String res = userResultset.getString("parent");
				result.add(res);
				while (userResultset.next()) {
					res = userResultset.getString("parent");
					result.add(res);
				}
			} else
				return null;

		} catch (java.sql.SQLException sqle) {
			sqle.printStackTrace();
			return new String[]{null, null};
		} catch (Exception e) {
			e.printStackTrace();
			return new String[]{null, null};
		}

		String[] results = new String[result.size()];
		for (int i = 0; i < result.size(); i++)
			results[i] = result.get(i);
		return results;
	}


	/**
	 * Gets any parent GO acccession numbers for this accession number having exactly
	 * the specified distance.
	 * @param acc
	 * @param distance
	 * @return
	 */
	public String[] getParentsForGOAccessionNumber (String acc, int distance) {
		if (!isGOAccessionNumber(acc))
			acc = makeGOAccessionNumber(acc);

		Vector<String> result = new Vector<String>();
		try {
			userResultset = userStatement.executeQuery("SELECT * FROM " + dbGo2GoTable +
					" WHERE child=\"" + acc + "\" and distance=\"" + distance + "\"");
			if (userResultset.first()) {
				String res = userResultset.getString("parent");
				result.add(res);
				while (userResultset.next()) {
					res = userResultset.getString("parent");
					result.add(res);
				}
			} else
				return null;

		} catch (java.sql.SQLException sqle) {
			//System.out.println(sqle);
			//System.err.println("No term found for " + acc);
			sqle.printStackTrace();
			return new String[]{null, null};
		} catch (Exception e) {
			e.printStackTrace();
			return new String[]{null, null};
		}

		String[] results = new String[result.size()];
		for (int i = 0; i < result.size(); i++)
			results[i] = result.get(i);
		return results;
	}


	/**
	 * Checks whether the given string number is a valid GO accession
	 * number: "GO:" followed by seven digits. Does not check if this
	 * number actually appears in the current version of GO!
	 * @param acc - the String to test
	 * @return true if acc is a valid GO accession number
	 */
	public static boolean isGOAccessionNumber (String acc) {
		if (acc.matches("GO:[0-9]{7}")) return true;
		if (acc.equals("all")) return true;
		if (acc.startsWith("obsolete_")) return true;
		return false;
	}


	/**
	 * Checks if an open connection to the DB exists. If not, call <code>open()</code> to
	 * open a connection.
	 *
	 * @return
	 */
	public boolean isOpen () {
		if (userConnection == null) return false;
		try {
			if (userConnection.isClosed()) return false;
		} catch (Exception e) {
			return false;
		}

		return openConnection;
	}


	/**
	 * Transforms the given number into a GO accession number.
	 * Adds "GO:" and the necessary number of zeros, until the
	 * number has seven digits.
	 * <br/>
	 * Returns "GO:0000000" if the number initially had more
	 * than seven digits already.
	 * @param number
	 * @return
	 */
	public static String makeGOAccessionNumber (int number) {
		String result = String.valueOf(number);
		if (result.length() > 7) return "GO:0000000";
		while (result.length() < 7)
			result = "0" + result;
		return "GO:" + result;
	}


	/**
	 * Transforms the given number into a GO accession number.
	 * Adds "GO:" and the necessary number of zeros, until the
	 * number has seven digits.
	 * <br/>
	 * Returns "GO:0000000" if <tt>number</tt> initially had more
	 * than seven digits already, or if the <tt>number</tt> was not
	 * a number at all.
	 * @param number
	 * @return
	 */
	public static String makeGOAccessionNumber (String number) {
		if (number.toLowerCase().trim().equals("all")) return "all";
		if (number.toLowerCase().trim().startsWith("obsolete_")) return number.toLowerCase().trim();
		String result = number;
		if (!number.matches("[0-9]{1,7}")) return "GO:0000000";
		while (result.length() < 7)
			result = "0" + result;
		return "GO:" + result;
	}


	/**
	 * Opens a connection to the database. If a connection is already open, closes
	 * this connection first and then opens a new one.
	 * @return
	 */
	public boolean open (String driver, String url, String user, String pass) {
		
		//if (true) return false;
		
		boolean success = true;

		// is a connection already open?
		if (isOpen())
			close();

		try {
			Class.forName(driver);
			userConnection = DriverManager.getConnection(
					url,
					user,
					pass);
			userStatement = userConnection.createStatement();
		} catch (java.sql.SQLException sqle) {
			sqle.printStackTrace();
			success = false;
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
			success = false;
		//} catch (UnknownHostException ukh) {
		} catch (Exception e) {
			System.err.println("Exception: unknown host?");
		}
		if (success) openConnection = true;
		return success;
	}


	/**
	 * For testing purposes only.
	 * @param args
	 */
	public static void main (String[] args) {
		//String[] accs = {"GO:0006915", "GO:0019904", "GO:0005515", "GO:0008219", "GO:0000001", "GO:0000002"};

		GOAccess ga = new GOAccess(ISGNProperties.get("dbDriver"), ISGNProperties.get("dbAccessUrl"),
				ISGNProperties.get("dbUser"), ISGNProperties.get("dbPass"));

		// apoptosis <--> activated T cell apoptosis
//		String acc1 = "GO:0006915";//"GO:0002867";
//		String acc2 = "GO:0006924";//"GO:0002516";
		String acc1 = "6915";//"GO:0002867";
		String acc2 = "6924";//"GO:0002516";
		float dist = 0.0f;

		dist = ga.getDistance(acc1, acc2);
		System.out.println("# Distance '"
				+ ga.getGOTermForAcc(acc1) + "'/'"
				+ ga.getGOTermForAcc(acc2) + "' = " + dist + " (p/c, d=1, l=6, should be low)");

		// apoptotic program <--> inflammatory cell apoptosis
		acc1 = "GO:0008632";
		acc2 = "GO:0006925";
		dist = ga.getDistance(acc1, acc2);
		System.out.println("# Distance '"
				+ ga.getGOTermForAcc(acc1) + "'/'"
				+ ga.getGOTermForAcc(acc2) + "' = " + dist + " (siblings, d=1+1, l=6, should be low)");

		acc1 = "GO:0009987";
		acc2 = "GO:0006925";
		dist = ga.getDistance(acc1, acc2);
		System.out.println("# Distance '"
				+ ga.getGOTermForAcc(acc1) + "'/'"
				+ ga.getGOTermForAcc(acc2) + "' = " + dist + " (p/c, d=7, l=2, c=7, should be mediocre)");

		acc1 = "GO:0009987";
		acc2 = "GO:0001906";
		dist = ga.getDistance(acc1, acc2);
		System.out.println("# Distance '"
				+ ga.getGOTermForAcc(acc1) + "'/'"
				+ ga.getGOTermForAcc(acc2) + "' = " + dist + " (siblings, d=1+1, l=1, should be mediocre)");

	 	// cytokinesis <--> regulation of leukocyte mediated cytotoxicity
		acc1 = "GO:0000910";
		acc2 = "GO:0001910";
		dist = ga.getDistance(acc1, acc2);
		System.out.println("# Distance '"
				+ ga.getGOTermForAcc(acc1) + "'/'"
				+ ga.getGOTermForAcc(acc2) + "' = " + dist + " (siblings, d=3+3, l=1, should be mediocre)");

		// finally, close all connections
		ga.close();
	}

	String dbGo2GoTable = "`GeneOntology_GO2GO`";
	String dbLCAtable = "`GeneOntology_LCA`";
	String dbTableTerm = "`term`";

	Connection goConnection = null;
	Statement goStatement = null;
	ResultSet goResultset = null;

	Connection userConnection = null;
	Statement userStatement = null;
	ResultSet userResultset = null;

	boolean openConnection = false;

	int verbosity = 0;
}
