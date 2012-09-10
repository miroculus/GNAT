package gnat.comparison;

import gnat.ISGNProperties;
import gnat.database.go.GOAccess;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Implementation of a GOTermSimilarity measure to compare GO codes for texts
 * against GO codes for genes, comparable to Schlicker et al. This class uses a
 * GOAccess to access a database that contains shortest distances and Lowest Common
 * Ancestors (LCA) for GO terms.
 * <br>
 * <br>
 * Similarity/distance is based on the shortest path using two terms' LCA and
 * the depth of this LCA in the overall hierarchy.
 * 
 * 
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 */
public class GOTermSimilarity implements Serializable {

	/** */
	private static final long serialVersionUID = 5368860031806091643L;

	/** */
	public int verbosity = 0;

	/** Stores the distances between two GO codes. Key: "GO:0005575;GO:0001695". */
	private Map<String, Float> goTermDistances = new HashMap<String, Float>();

	/** Where to find the existing GO term distances. Default: go2go.obj.<br>
	 * Format: HashMap&lt;String, Float&gt;, see <code>goTermDistances</code>. */
	public String go2gofile = "data/go2go.object";

	private int initiallyLoadedSimilarities = 0;
	private boolean computedNewSimilarity = false;

	private static GOAccess goAccess;

	
	/**
	 * Accesses the database that contains LCA information on GO terms
	 * when distances were not already calculated before. These distances
	 * are stored in <tt>go2gofile</tt>.  If set to false, uses distances
	 * from that file only and returns NEG.INF for unknown distances.
	 */
	private boolean useDatabase = true;


	/**
	 *
	 */
	public GOTermSimilarity (GOAccess go) {
		goAccess = go;
	}


	/**
	 * Closes the current connection to the DB. Should be called after this class is used.
	 *
	 */
	public void closeDBConnection () {
		if (goAccess != null)
			if (goAccess.isOpen())
				goAccess.close();
	}


	/**
	 *	Returns the shortest distance between two GO codes in the Gene Ontology.
	 *
	 * @param goCode1
	 * @param goCode2
	 * @return
	 */
	public float getDistance (String goCode1, String goCode2) {
		goCode1 = goCode1.replaceAll("^GO\\:0+", "");
		goCode2 = goCode2.replaceAll("^GO\\:0+", "");

		if (goCode1.equals(goCode2)) return 0.0f;

		String key = goCode1 + ";" + goCode2;

		//if(verbosity>3){
		//	System.out.println(this.getClass().getSimpleName()+": getDistance for key='"+key+"'");
		//}

		if (goTermDistances.containsKey(key)) {
			return goTermDistances.get(key);
		}
		key = goCode2 + ";" + goCode1;
		if (goTermDistances.containsKey(key))
			return goTermDistances.get(key);

		if (!useDatabase)
			return Float.NEGATIVE_INFINITY;

		
		//System.err.println("#GTS: getting distance");
		float dist = goAccess.getDistance(goCode1, goCode2);

		if(verbosity>3){
			System.out.println(this.getClass().getSimpleName()+": getDistance: computed new similarity = "+dist);
		}

		key = goCode1 + ";" + goCode2;
		goTermDistances.put(key, dist);

		computedNewSimilarity = true;

		return dist;
	}


	/**
	 *	Returns the similarity of two GO codes.
	 *
	 * @param goCode1
	 * @param goCode2
	 * @return
	 */
	public static float getSimilarity (String goCode1, String goCode2) {
		if (goCode1.equals(goCode2)) return 1.0f;

		//GOAccess goAccess = new GOAccess();
		float dist = goAccess.getDistance(goCode1, goCode2);

		return 1.0f - dist;
	}


	/**
	 *	Returns a score between to lists of GO codes based on the average of pairwise comparison.
	 *
	 * @param codes1
	 * @param codes2
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public float getGOSimilarity (LinkedList<String> codes1, LinkedList<String> codes2) {
		//System.err.println("Calling GTS.getGOSim");
		//System.err.println("#Scoring GO codes for gene vs text:");
		//System.err.println("# codes 1: " + codes1);
		//System.err.println("# codes 2: " + codes2);
		
		// maps a GO-Code pair to its distance
		Hashtable<String, Float> pair2distance = new Hashtable<String, Float>();

		// contain all GO codes for each list as a set
		TreeSet<String> set1 = new TreeSet<String>();
		TreeSet<String> set2 = new TreeSet<String>();

		// get the scores for all pairs
		for (String go1: codes1) {
			set1.add(go1);

			for (String go2: codes2) {
				set2.add(go2);

				float distance = getDistance(go1, go2);
				//if (gosim > sim) sim = gosim;

				String pair = go1 + ";" + go2;
				pair2distance.put(pair, new Float(distance));

			}
		}

		// now sort the results by distance
		Set<Map.Entry<String, Float>> set = pair2distance.entrySet();
		Map.Entry[] entries = (Map.Entry[]) set.toArray(new Map.Entry[set.size()]);
		//FloatComparator fc = new FloatComparator();
		Arrays.sort(entries, new Comparator() {
			public int compare(Object o1, Object o2) {
				Object v1 = ((Map.Entry) o1).getValue();
				Object v2 = ((Map.Entry) o2).getValue();
				return ((Float) v1).compareTo((Float)v2);  // v1, v2 => lowest first
			}
		});

		float average = 0.0f;
		float count = 0.0f;

//		for (int e = 0; e < entries.length; e++) {
//			String key = (String)entries[e].getKey();
//			float dist = pair2distance.get(key).floatValue();
//			//if (verbosity > 3) {
//			//	System.err.println("# pair " + key + ": sim=" + (1.0f-dist));
//			//}
//		}

		// get the best fitting pairs of myID and pID, remove them from the sets,
		// until one set is empty
		for (int e = 0; e < entries.length; e++) {
			String key = (String)entries[e].getKey();
			float dist = pair2distance.get(key).floatValue();
			if (dist == Float.POSITIVE_INFINITY)
				break;

			//System.out.println("# GO check for " + key + " = " + dist);

			String[] ids = key.split(";");
			if (!set1.contains(ids[0]) || !set2.contains(ids[1]))
				continue;

			average += dist;
			count += 1.0f;


			set1.remove(ids[0]);
			set2.remove(ids[1]);

			if (set1.size() == 0 || set2.size() == 0)
				break;
		}

		float score = (1 - (average / count));
		
		//System.err.println("# score = " + score);

		return score;
	}


	/**
	 *	Loads precomputed GO terms distances from a serialized object file (Map<String, Float>).
	 * @param filename
	 */
	@SuppressWarnings("unchecked")
	public void loadGOTermDistances (String filename) {
		go2gofile = filename;

		FileInputStream fis = null;
		ObjectInputStream in = null;
		File FILE = new File(filename);
		try {
			fis = new FileInputStream(FILE);
			in = new ObjectInputStream(fis);
			goTermDistances = (HashMap<String, Float>)in.readObject();
//			for (String key : goTermDistances.keySet()) {
//				if(goTermDistances.get(key)< Double.POSITIVE_INFINITY){
//					System.err.println("# key = "+key+", dist = "+goTermDistances.get(key));
//				}
//            }
			in.close();
		} catch (java.io.FileNotFoundException fnfe) {
			//cnfe.printStackTrace();
			System.err.println("#ERROR no such file: " + go2gofile + ", starting with empty set of GO-to-GO distances.");
			goTermDistances = new HashMap<String, Float>();
		} catch (ClassNotFoundException cnfe) {
			System.err.println("#ERROR opening " + go2gofile + ": unexpected content");
			//cnfe.printStackTrace();
			goTermDistances = new HashMap<String, Float>();
		} catch (java.io.StreamCorruptedException sce) {
			System.err.println("#ERROR opening " + go2gofile + ": " + sce.getMessage());
			sce.printStackTrace();
			//return null;
			goTermDistances = new HashMap<String, Float>();
		} catch (java.io.EOFException ee) {
			System.err.println("#ERROR opening " + go2gofile + ": " + ee.getMessage());
			//ee.printStackTrace();
			//return null;
			goTermDistances = new HashMap<String, Float>();
		} catch (java.io.IOException ioe) {
			System.err.println("#ERROR opening " + go2gofile + ": " + ioe.getMessage());
			//ioe.printStackTrace();
			goTermDistances = new HashMap<String, Float>();
		}
		initiallyLoadedSimilarities = goTermDistances.size();

		if(verbosity>0)
			System.err.println("# Loaded GO term distances from disk, #pairs: " + initiallyLoadedSimilarities);
	}


	/**
	 * Returns a similarity of the two sets based on the most similar pair
	 * that was found.
	 *
	 * @param codes1
	 * @param codes2
	 * @return
	 */
	public static float scoreMostSimilarGOCodes (LinkedList<String> codes1, LinkedList<String> codes2) {
		float sim = 0.0f;
		for (String go1: codes1) {
			for (String go2: codes2) {
				float gosim = getSimilarity(go1, go2);
				if (gosim > sim) sim = gosim;
			}
		}

		return sim;
	}


	/**
	 *
	 * Writes all go term distances to file.
	 *
	 * @return
	 */
	public boolean writeGOTermDistances () {
		if (computedNewSimilarity || (initiallyLoadedSimilarities < goTermDistances.size())) {
			//System.err.println("#Calling writeGO (1)");
			if(verbosity>0)
				System.err.println("# Writing GO term distances to disk, #pairs: " + goTermDistances.size());
			FileOutputStream fos = null;
			ObjectOutputStream out = null;
			File FILE = new File(go2gofile);
			try {
				fos = new FileOutputStream(FILE);
				out = new ObjectOutputStream(fos);
				out.writeObject(goTermDistances);
				out.close();
			} catch(IOException ex) {
				ex.printStackTrace();
				return false;
			}
			return true;
		}
		else{
			//System.err.println("#Calling writeGO (2)");
			if(verbosity>0)
				System.err.println("# No new GO term similarities. Skip writing.");
			return true;
		}
	}



	/**
	 *
	 * @param args
	 */
	public static void main (String[] args) {
		LinkedList<String> codes1 = new LinkedList<String>();
		LinkedList<String> codes2 = new LinkedList<String>();

		String[] gene = {"3677", "3723", "166", "5515", "6310", "6281", "8380", "398", "6355", "6350", "5634"};
		String[] text = {"GO:0003779", "GO:0003680", "GO:0005576", "GO:0008380", "GO:0005488", "GO:0016021",
				"GO:0005622", "GO:0005856", "GO:0005623"};
		//# norm.overlap=0.0, cos=0.0, splittedCos=0.0, go.sim=0.8996803, max=0.8996803

		GOTermSimilarity goScorer = new GOTermSimilarity(new GOAccess(ISGNProperties.get("dbDriver"), ISGNProperties.get("dbAccessUrl"),
				ISGNProperties.get("dbUser"), ISGNProperties.get("dbPass")));
		goScorer.loadGOTermDistances("data/go2go.obj");

		for (String c: gene)
			codes1.add(c);
		for (String c: text)
			codes2.add(c);

		System.out.println("# Sim: " + goScorer.getGOSimilarity(codes1, codes2));

		goScorer.writeGOTermDistances();

	}
}
