package uk.ac.man.entitytagger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import martin.common.Misc;

/**
 * Class for representing a text match, containing the matched text, document coordinates and normalized IDs. 
 * @author Martin
 *
 */
public class Mention implements Comparable<Mention>, Serializable {
	private static final long serialVersionUID = 6741123952662841857L;
	public static final String COMMENT_SEPARATOR = "|";

	private int start, end;
	private String text = null;
	private String[] ids;
	private String comment="";
	private String docid="";
	private Double[] probabilities;
	private String[] idLines;

	public class SimpleMention{
		private String[] ids;
		private Double[] probabilities;
		private int start,end;
		private String text;

		public SimpleMention(String[] ids, Double[] probabilities, int start, int end, String text){
			this.ids = ids;
			this.probabilities = probabilities;
			this.start = start;
			this.end = end;
			this.text = text;
		}

		/**
		 * @return the ids
		 */
		public String[] getIds() {
			return ids;
		}
		/**
		 * @return the probabilities
		 */
		public Double[] getProbabilities() {
			return probabilities;
		}
		/**
		 * @return the start
		 */
		public int getStart() {
			return start;
		}
		/**
		 * @return the end
		 */
		public int getEnd() {
			return end;
		}
		/**
		 * @return the text
		 */
		public String getText() {
			return text;
		}
	}

	public Mention(String[] ids){
		this(ids,-1,-1,null);
	}

	public Mention clone(){
		Mention m = new Mention(getIdsWithLineNumbers().clone(), start, end, text);
		m.setProbabilities(probabilities != null ? probabilities.clone() : null);
		m.setComment(comment);
		m.setDocid(docid);
		return m;
	}

	public Mention(String id, int start, int end, String text){
		this(new String[]{id}, start, end, text);
	}

	/**
	 * Saves a list of mentions to a stream, in BioCreative 2 format, suitable for evaluation.
	 * @param outStream
	 * @param mentions The mentions, from a _single_ document.
	 * @param restrictBySpecies if not null, any mentions not from the specified species will be ignored
	 * @param toSpeciesMap map from mention ids to species ids, used if restrictBySpecies is not null
	 */
	public static void saveToStreamInBCFormat(BufferedWriter outStream, List<Mention> mentions, String restrictBySpecies, Map<String,String> toSpeciesMap){
		if (restrictBySpecies != null && toSpeciesMap == null)
			throw new IllegalStateException("If restrictBySpecies != null, toSpeciesMap also have to be != null (you most likely forgot to specify the file containing mappings from gene/protein entity ids to species ids.)");

		try{
			for (Mention m : mentions){
				Map<String,Map<String,Integer>> counts = new HashMap<String,Map<String,Integer>>();

				for (String id : m.getIds()){
					if (restrictBySpecies == null || !toSpeciesMap.get(id).equals(restrictBySpecies)){
						if (!counts.containsKey(id))
							counts.put(id, new HashMap<String,Integer>());
						if (!counts.get(id).containsKey(m.getText()))
							counts.get(id).put(m.getText(), 1);
						else
							counts.get(id).put(m.getText(), counts.get(id).get(m.getText()) + 1); 
					}					
				}

				for (String id : counts.keySet()){
					Map<String,Integer> map = counts.get(id);
					String max = null;
					for (String k : map.keySet())
						if (max == null || map.get(k) > map.get(max))
							max = k;

					outStream.write(m.getDocid() + "\t" + id + "\t" + max + "\t" + map.get(max) + "\n");
				}
			}
		} catch (Exception e){
			System.err.println(e);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public Mention(String[] ids, int start, int end, String text){
		this.ids = ids;
		this.start = start;
		this.end = end;
		this.text = text;

		if (ids == null || ids.length == 0)
			throw new IllegalStateException();

		idLines = new String[ids.length];

		for (int i = 0; i < ids.length; i++){
			if (ids[i].contains(COMMENT_SEPARATOR)){
				String[] fields = ids[i].split("\\"+COMMENT_SEPARATOR);
				if (fields.length == 2){
					this.ids[i] = fields[0];
					idLines[i] = fields[1];
				}
			}
		}
	}

	/**
	 * @param id
	 * @return whether the Match contains the given id
	 */
	public boolean containsID(String id){
		for (int i = 0; i < this.ids.length; i++)
			if (this.ids[i].equals(id))
				return true;
		return false;
	}

	public SimpleMention simplify(){
		return new SimpleMention(ids, probabilities, start, end, text);
	}

	/**
	 * If containsID(id) is true, will set ids to id only (any other ids will be deleted), otherwise throws an exception
	 * @param id
	 * @throws IllegalStateException if containsID(id) == false
	 */
	public void disambiguate(String id){
		if (!containsID(id))
			throw new IllegalStateException("Disambiguated ids not found in Match entry");

		this.ids = new String[]{id};
		this.probabilities = null;		
	}

	/**
	 * @return a String on the format "id<tab>doc id<tab>start<tab>end<tab>text<tab>comment" (without citation marks)
	 */
	public String toString(){
		String start_local = start != -1 ? ""+start : "";
		String end_local = end != -1 ? ""+end : "";
		String text_local = text != null ? text : "";
		String comment_local = comment != null ? comment : "";

		return getIdsToString() + "\t" + docid + "\t" + start_local + "\t" + end_local + "\t" + text_local + "\t" + comment_local;
	}

	public String[] getIds(){
		return ids;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	/**
	 * @return getIds().length > 1
	 */
	public boolean isAmbigous(){
		return ids.length > 1;
	}

	public boolean equals(Object o){
		Mention m = (Mention) o;
		return (m.toString().equals(this.toString()));
	}

	/**
	 * @param m1
	 * @param m2
	 * @return whether m1 and m2 overlaps
	 */
	public static boolean overlaps(Mention m1, Mention m2){
		if (m1.getDocid() == null && m2.getDocid() != null)
			return false;
		if (m1.getDocid() != null && m2.getDocid() == null)
			return false;
		if (m1.getDocid() != null && !m1.getDocid().equals(m2.getDocid()))
			return false;

		int s1 = m1.getStart();
		int s2 = m2.getStart();
		int e1 = m1.getEnd();
		int e2 = m2.getEnd();

		return (s1 >= s2 && s1 < e2) || (s2 >= s1 && s2 < e1);		
	}

	/**
	 * @param m2
	 * @return whether the match overlaps with m2
	 */
	public boolean overlaps(Mention m2){
		return Mention.overlaps(this,m2);
	}

	/**
	 * Loads matches using loafFromFile(file,validDocumentIDs,conversionMap) and then splits them up to allow access by document ID.
	 * Using the resulting HashMap, the list of matches for a specific document can quickly be retrieved.
	 * @param file
	 * @param validDocumentIDs
	 * @param conversionMap
	 * @return a HashMap mapping document IDs to a list of matches for that document. 
	 */
	public static Map<String,List<Mention>> loadFromFileToHash(File file, Set<String> validDocumentIDs, String restrictPostfix, HashMap<String, String> conversionMap){
		Map<String,List<Mention>> res = new HashMap<String,List<Mention>>();
		List<Mention> matches = loadFromFile(file, validDocumentIDs, restrictPostfix, conversionMap);

		for (Mention m : matches){
			if (!res.containsKey(m.getDocid()))
				res.put(m.getDocid(), new ArrayList<Mention>());

			res.get(m.getDocid()).add(m);
		}

		return res;		
	}

	public static List<Mention> loadFromFile(File file){
		return loadFromFile(file,null,null,null);
	}

	/**
	 * Will load a set of matches from a file, constrained such that only matches from documents in validDocumentIDs are returned. If conversionMap isn't null, matches can also be returned if validDocumentIDs.contains(conversionMap.get(documentID_for_the_match)) is true.
	 * @param file
	 * @param validDocumentIDs
	 * @param conversionMap
	 * @return the set of matches with valid document IDs
	 */
	public static List<Mention> loadFromFile(File file, Set<String> validDocumentIDs, String restrictPostfix, HashMap<String,String> conversionMap){
		try{
			BufferedReader inStream = new BufferedReader(new FileReader(file));

			ArrayList<Mention> matches = new ArrayList<Mention>();

			String line = inStream.readLine();

			Pattern splitPattern = Pattern.compile("\\t");

			while (line != null){
				if (!line.startsWith("#")){
					String[] fields = splitPattern.split(line, -1);

					String id = fields[1];
					boolean valid = false;

					if (validDocumentIDs == null && restrictPostfix == null)
						valid = true;
					else if (restrictPostfix != null && id.endsWith(restrictPostfix))
						valid = true;
					else if (validDocumentIDs != null && validDocumentIDs.contains(id))
						valid = true;
					else if (conversionMap != null && conversionMap.containsKey(id) && validDocumentIDs != null && validDocumentIDs.contains(conversionMap.get(id)))
						valid = true;

					if (valid){
						int s = fields[2].length() > 0 ? Integer.parseInt(fields[2]) : -1;
						int e = fields[3].length() > 0 ? Integer.parseInt(fields[3]) : -1;

						String[] isp;
						Double[] probs;
						if (fields[0].contains("|")){
							String[] sp = fields[0].split("\\|");
							isp = new String[sp.length];
							probs = new Double[sp.length];

							for (int i = 0; i < sp.length; i++){
								if (sp[i].contains("?")){
									String[] f2 = sp[i].split("\\?");
									isp[i] = f2[0];
									if (f2.length > 1)
										probs[i] = Double.parseDouble(f2[1]);
								} else {
									isp[i] = sp[i];
								}
							}
						} else {
							isp = new String[]{fields[0]};
							probs = new Double[]{null};
						}

						Mention m = new Mention(isp,s,e,fields[4]);
						m.setDocid(id);
						m.setProbabilities(probs);

						if (fields.length > 5)
							m.setComment(fields[5]);

						matches.add(m);
					}
				}

				line = inStream.readLine();
			}

			inStream.close();
			return matches;
		} catch (Exception e){
			System.err.println(e);
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	/**
	 * Saves the list of matches to file, sorted by their start and end coordinates.
	 * @param matches
	 * @param file
	 */
	public static void saveToFile(ArrayList<Mention> matches, File file){
		try{
			matches = Misc.sort(matches);
			BufferedWriter outStream = new BufferedWriter(new FileWriter(file));
			outStream.write("#entity id\tdocument\tstart\tend\ttext\tcomment\n");
			for (int i = 0; i < matches.size(); i++)
				outStream.write(matches.get(i).toString() + "\n");

			outStream.close();
		} catch (Exception e){
			System.err.println(e);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public int compareTo(Mention o){
		if (this.docid != null && o.docid != null && !this.docid.equals(o.docid))
			return this.docid.compareTo(o.docid);

		if (start != o.getStart())
			return new Integer(start).compareTo(o.getStart());

		return new Integer(end).compareTo(o.getEnd());
	}

	public void setStart(int start) {
		this.start = start;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	/**
	 * @return the text
	 */
	public String getText() {
		return text;
	}

	/**
	 * @return extra data that can be associated with the object
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment extra data that can be associated with the object
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * @return the document id
	 */
	public String getDocid() {
		return docid;
	}

	/**
	 * @param docid the document id to set
	 */
	public void setDocid(String docid) {
		this.docid = docid;
	}

	/**
	 * @return the probabilities that might be associated with the IDs (this might not exist, in which case some/all might be null) 
	 */
	public Double[] getProbabilities() {
		return probabilities;
	}

	/**
	 * @param probabilities an array of probabilities associated with the IDs, need to be the same number as the IDs
	 */
	public void setProbabilities(Double[] probabilities) {
		if (probabilities != null && probabilities.length != ids.length)
			throw new IllegalStateException("Different number of probabilities and ids");

		this.probabilities = probabilities;
	}

	/**
	 * @param text the text to set
	 */
	public void setText(String text) {
		this.text = text;
	}

	public String getMostProbableID() {
		int max = 0;

		if (probabilities != null){
			double maxprob = probabilities[max] != null ? probabilities[max] : 0;
			for (int i = 0; i < probabilities.length; i++)
				if (probabilities[i] != null && probabilities[i] > maxprob){
					max = i;
					maxprob = probabilities[i];
				}
		}

		return ids[max];
	}

	/**
	 * Saves the match to a database using PreparedStatements. 
	 * @param pstmt_match Statement for inserting the match (exclusive entity ids and probabilities) to a database, with the following fields: 1: document, 2: start, 3: end, 4: text, 5: comment 
	 * @param pstmt_id Statement for inserting the ids and probabilities to a database, with the following fields: 1: match database id, 2: entity id, 3: probability 
	 * @return the id of the inserted match
	 */
	public int saveToDB(PreparedStatement pstmt_match, PreparedStatement pstmt_id) {
		try{
			pstmt_match.setString(1, this.getDocid());
			pstmt_match.setInt(2, this.getStart());
			pstmt_match.setInt(3, this.getEnd());

			if (this.getText() != null)
				pstmt_match.setString(4, this.getText());
			else
				pstmt_match.setNull(4, java.sql.Types.NULL);

			if (this.getComment() != null)
				pstmt_match.setString(5, this.getComment());
			else
				pstmt_match.setNull(5, java.sql.Types.NULL);

			pstmt_match.execute();

			ResultSet rs = pstmt_match.getGeneratedKeys();

			int id = -1;

			if (rs.next())
				id = rs.getInt(1);
			else
				throw new IllegalStateException("SQL insert did not generate a new auto_increment value");

			for (int i = 0; i < ids.length; i++){
				String s = ids[i];
				pstmt_id.setInt(1, id);
				pstmt_id.setString(2, s);

				if (probabilities != null && probabilities[i] != null)
					pstmt_id.setDouble(3, probabilities[i]);
				else
					pstmt_id.setNull(3, java.sql.Types.NULL);
				pstmt_id.addBatch();
			}
			pstmt_id.executeBatch();

			return id;
		} catch (Exception e){
			System.err.println(e);
			e.printStackTrace();
			System.exit(-1);
		}
		return -1;
	}

	/**
	 * @param ids the ids to set
	 */
	public void setIds(String[] ids) {
		this.ids = ids;
	}

	public String getMostProbableIDWithIdLine() {
		int max = 0;

		if (ids.length > 1 && probabilities != null){
			double maxprob = probabilities[max] != null ? probabilities[max] : 0;
			for (int i = 0; i < probabilities.length; i++)
				if (probabilities[i] != null && probabilities[i] > maxprob){
					max = i;
					maxprob = probabilities[i];
				}
		}

		String res = idLines[max] == null || idLines.length < max ? ids[max] : ids[max] + COMMENT_SEPARATOR + idLines[max];
		return res;
	}

	public String[] getIdsWithLineNumbers() {
		if (idLines == null) 
			return ids;

		String[] res = new String[ids.length];

		for (int i = 0; i < ids.length; i++)
			res[i] = idLines[i] == null ? ids[i] : ids[i] + COMMENT_SEPARATOR + idLines[i];

			return res;
	}

	public String getIdsToString() {
		String sp = ""+ids[0];

		if (probabilities != null && probabilities.length > 0 && probabilities[0] != null)
			sp += "?" + probabilities[0];

		for (int i = 1; i < ids.length; i++){
			sp += "|" + ids[i];

			if (probabilities != null && probabilities.length > i && probabilities[i] != null)
				sp += "?" + probabilities[i];
		}

		return sp;
	}

	public static Mention findClosestMention(List<Mention> speciesMentions, int pos) {
		Mention m = null;
		int dist = Integer.MAX_VALUE;
		for (Mention m2 : speciesMentions)
			if (m == null || dist > Math.abs((m2.getStart()+m2.getEnd())/2 - pos)){
				m = m2;
				dist = Math.abs((m2.getStart()+m2.getEnd())/2 - pos);				
			}
		return m;		
	}
}
