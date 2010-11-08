package gnat.database.go;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeSet;

/**
 * Computes the lowest common ancestor for two GO terms
 *
 * Call:<br/>
 * java -classpath bin -Xmx1750M de.tud.biotec.databases.go.CalculateLCAs GeneOntology_GO2GO.csv distances.lca.csv
 *
 *
 * @author Joerg Hakenberg
 *
 */

public class CalculateLCAs {

	public static HashMap<String, Integer> distancesChild2Parents = new HashMap<String, Integer>();
	public static HashMap<String, TreeSet<String>> childs2Parents = new HashMap<String, TreeSet<String>>();
	public static TreeSet<String> allIDs = new TreeSet<String>();
	public static TreeSet<String> done = new TreeSet<String>();

	/**
	 *
	 * @param child
	 * @param parent
	 * @return
	 */
	public static int getDistance (String child, String parent) {
		String key = child + ";" + parent;
		if (distancesChild2Parents.containsKey(key))
			return distancesChild2Parents.get(child + ";" + parent);
		else
			return Integer.MAX_VALUE;
	}


	/**
	 *
	 * @param child1
	 * @param child2
	 * @param parent
	 * @return
	 */
	public static int getCombinedDistance (String child1, String child2, String parent) {
		//int result = getDistance(child1, "all") + getDistance(child2, "all");
		int dist1 = getDistance(child1, parent);
		int dist2 = getDistance(child2, parent);
		//if ((dist1 + dist2) < result)
		//	result = (dist1 + dist2);
		//return result;
		return (dist1 + dist2);
	}


	/**
	 *
	 * @param parents1
	 * @param parents2
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static TreeSet<String> findCommonAncestors (TreeSet<String> parents1, TreeSet<String> parents2) {
		if (parents1 == null || parents1.size() == 0
				|| parents2 == null || parents2.size() == 0)
			return new TreeSet<String>();

		TreeSet<String> result = (TreeSet<String>)parents1.clone();
		result.retainAll(parents2);
		return result;
	}


	/**
	 *
	 * @param child1
	 * @param child2
	 * @return
	 */
	public static String findLowestCommonAncestors (String child1, String child2) {
		TreeSet<String> parents1 = childs2Parents.get(child1);
		TreeSet<String> parents2 = childs2Parents.get(child2);
		TreeSet<String> commonAncestors = findCommonAncestors(parents1, parents2);
		int mindist = Integer.MAX_VALUE;
		String lca = "all";
		for (String ca : commonAncestors) {
			int dist = getCombinedDistance(child1, child2, ca);
			if (dist < mindist) {
				mindist = dist;
				lca = ca;
			}
		}
		return lca;
	}


	/**
	 *
	 * @param args
	 */
	public static void main (String[] args) {
		//Calendar nowCal = Calendar.getInstance();
		//Date nowDate = nowCal.getTime();
		//System.err.println("# Start reading file:  " + nowDate.toString());

		String file = args[0];
		String outfile = args[1];
		distancesChild2Parents = new HashMap<String, Integer>();
		childs2Parents = new HashMap<String, TreeSet<String>>();
		try {
			String line;
			BufferedReader br = new BufferedReader(new FileReader(new File(file)));
			while ((line = br.readLine()) != null) {
				// line= parent;child;dist
				String[] cols = line.split(";");
				String parent = cols[0];
				String child = cols[1];
				int dist = Integer.parseInt(cols[2]);

				allIDs.add(parent);
				allIDs.add(child);

				// keep the smallest distance between any child and parent
				// key= child;parent
				String key = child + ";" + parent;
				if (distancesChild2Parents.containsKey(key)) {
					int olddist = distancesChild2Parents.get(key).intValue();
					if (dist < olddist)
						distancesChild2Parents.put(key, new Integer(dist));
				} else {
					distancesChild2Parents.put(key, new Integer(dist));
				}

				// keep all parents for all childs for easier lookup
				if (childs2Parents.containsKey(child)) {
					TreeSet<String> parents = childs2Parents.get(child);
					parents.add(parent);
					childs2Parents.put(child, parents);
				} else {
					TreeSet<String> parents = new TreeSet<String>();
					parents.add(parent);
					childs2Parents.put(child, parents);
				}

			}
			br.close();
		} catch (IOException ioe) { }

		System.err.println("# Found " + distancesChild2Parents.size() + " distances");
		System.err.println("# Found " + childs2Parents.size() + " childs with parents");

		int pairs = allIDs.size() * allIDs.size();
		int paircounter = 0;
		int oldpercenti = -100;
		int part = 1;

		File OUTFILE = new File(outfile + ".part" + part);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(OUTFILE));

			for (String child1 : allIDs) {
				// do not store distances for ROOT
				if (child1.startsWith("obsolete_"))
					continue;
				if (child1.startsWith("all"))
					continue;

				for (String child2 : allIDs) {
					paircounter++;
					float percent = (float) paircounter / (float) pairs * 100;
					int percenti = Math.round(percent);
					if ((percenti % 5) == 0) {
						if (percenti != oldpercenti) {
							System.err.print(" [" + percenti + "%]");
							oldpercenti = percenti;
							bw.close();
							part++;
							OUTFILE = new File(outfile + ".part" + part);
							bw = new BufferedWriter(new FileWriter(OUTFILE));
						}
					}

					if (child1.equals(child2))
						continue;

					if (child2.startsWith("obsolete_"))
						continue;
					if (child2.startsWith("all"))
						continue;

					if (done.contains(child2))
						continue;

					// do not store direct paths when one is the parent of
					// another
					String key = child1 + ";" + child2;
					if (distancesChild2Parents.containsKey(key))
						continue;
					// if (done.contains(key)) continue;
					key = child2 + ";" + child1;
					if (distancesChild2Parents.containsKey(key))
						continue;
					// if (done.contains(key)) continue;

					// do not store "all" as LCA
					String tlca = findLowestCommonAncestors(child1, child2);
					if (tlca.equals("all"))
						continue;
					int dist1 = getDistance(child1, tlca);
					int dist2 = getDistance(child2, tlca);
					int dlca = getDistance(tlca, "all");
					if (dlca == Integer.MAX_VALUE)
						dlca = 0;
					int dist = getCombinedDistance(child1, child2, tlca);
					if (dist < Integer.MAX_VALUE) {
						bw.write(child1.substring(3) + ";" + child2.substring(3) + ";" + tlca.substring(3) + ";" + dist1 + ";" + dist2 + ";" + dlca + "\n");
						//System.out.println(child1 + ";" + child2 + ";" + tlca + ";" + dist1 + ";" + dist2 + ";" + dlca);
					}

					// store only one direction
					//done.add(key);

				} // for child2

				done.add(child1);
			} // for child1

			bw.close();

		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

	}
}
