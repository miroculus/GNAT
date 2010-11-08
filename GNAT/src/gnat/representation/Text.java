package gnat.representation;

import gnat.preprocessing.sentences.SentenceSplitter;
import gnat.preprocessing.sentences.SentenceSplitterRegex;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Represents a text object, with source text, text context model, and annotations.
 *
 * <br>
 * <br>
 * TODO: each annotation should alter the text context model! For instance, a new GO term
 * was annotated, then the context vector for GO terms/codes should be adapted. Or, a gene
 * was annotated (with a high confidence), than this should be added as (potential) interaction
 * partners. Or, a locations (chromosomal or subcellular) was identified, then this should be
 * added to the context vector that represents this type of annotations. This improves the
 * comparison of (gene) context against this text's context (more precise annotations).
 *
 *
 * @author Joerg Hakenberg
 */

public class Text {

	/** */
	TextContextModel model;


	/** */
	public String ID = "-1";


	/** */
	public String plainText = "";


	public int PMID = -1;


	/** All species that have been found in this text. */
	public Set<Integer> taxonIDs = new TreeSet<Integer>();
	/** */
	public Map<Integer, Set<String>> taxonIdsToNames = new HashMap<Integer, Set<String>>();


	public LinkedList<String> sentences = new LinkedList<String>();


	public LinkedList<Integer> sentenceIds = new LinkedList<Integer>();

	SentenceSplitter splitter = new SentenceSplitterRegex();

	/** */
	public boolean sentencesInXML = false;


	/** */
	public Text (String id){
		this.ID = id;
		
		if (id.matches("\\d+"))
			this.PMID = Integer.parseInt(id);	// default
	}


	/** */
	public Text (String id, String plainText){
		this(id);
		setPlainText(plainText);
		setSentences(splitter.split(plainText), false);

		TextContextModel tcm = new TextContextModel();
		tcm.addPlainText(plainText);
		this.setContextModel(tcm);
	}


	/**
	 * 
	 * @param id
	 * @param sentences
	 * @param xml
	 */
	public Text (String id, Vector<String> sentences, boolean xml) {
		this(id);
		setSentences(sentences, xml);
		sentencesInXML = xml;

		TextContextModel tcm = new TextContextModel();
		tcm.addPlainText(this.getPlainText());
		this.setContextModel(tcm);
	}


	/**
	 * 
	 * @param id
	 * @param sentences
	 */
	public Text (String id, Vector<String> sentences) {
		this(id);
		setSentences(sentences, false);

		TextContextModel tcm = new TextContextModel();
		tcm.addPlainText(this.getPlainText());
		this.setContextModel(tcm);
	}


	/** */
	public LinkedList<TextAnnotation> annotations = new LinkedList<TextAnnotation>();


	/**
	 * Merges the text's current and the new content model.
	 * @param model
	 */
	public void addToContextModel (ContextModel model) {
		this.model.mergeWithOtherModel(model);
	}


	/**
	 *
	 * @return
	 */
	public TextContextModel getContextModel () {
		return this.model;
	}


	/**
	 *
	 * @param model
	 */
	public void setContextModel (TextContextModel model) {
		this.model = model;
	}


	/**
	 * 
	 * @param taxonIDs
	 */
	public void setTaxonIDs (Set<Integer> taxonIDs) {
		this.taxonIDs = taxonIDs;
	}


	/**
	 * Returns the character at the specified text position.
	 * */
	public char getCharAt(int index){
		if (index < 0) return ' ';
		if (index > getPlainText().length() - 1) return ' ';
		return getPlainText().charAt(index);
	}


	/**
	 * 
	 * @return
	 */
	public String getID() {
	    return ID;
    }


	/**
	 * 
	 * @return
	 */
	public boolean hasPMID () {
		return //true;//
			ID.matches("[0-9]+");
	}


	public int length()
    {
	    return getPlainText().length();
    }

	/**
	 * Returns the (approximate) length of the text in tokens. Splits at every
	 * single white space and returns the number of elements generated that way.
	 * @return
	 */
	public int lengthInTokens () {
		return getPlainText().split(" ").length;
	}
	
	
	/**
	 * Adds species and their names (as they occurred in this text) to the map
	 * {@link #taxonIdsToNames}.
	 * @param map
	 */
	public void addTaxonToNameMap (Map<Integer, Set<String>> map) {
		for (int taxon: map.keySet()) {
			if (taxonIdsToNames.containsKey(taxon)) {
				Set<String> oldNames = taxonIdsToNames.get(taxon);
				oldNames.addAll(map.get(taxon));
				taxonIdsToNames.put(taxon, oldNames);
			} else {
				taxonIdsToNames.put(taxon, map.get(taxon));
			}
		}
	}


	public void setPlainText(String text){
		this.plainText = text;
	}


	public String getPlainText()
    {
	    return plainText;
    }


	public String getFileName()
    {
		// TODO
	    return null;

    }

	/**
	 * Returns the sentence that contains the given position.<br>
	 * If the hits a sentence boundary, returns the sentence before the given position.
	 * If the position hits a white space after a sentence boundary, returns the sentence following the position.<br>
	 * Returns the sentence including the end mark.
	 * <br>
	 * @param position
	 * @return
	 */
	public String getSentenceAround (int position) {
		//System.err.println("text is '" + this.plainText + "'");
		
		if (position < 0) return null;
		String temp = getPlainText() + " ";
		if (position > temp.length()-2) return null; // -2 b/c + " "
		String copy = temp;

		if (position < temp.indexOf(". ") + 1) return temp.substring(0, temp.indexOf(". ")+1);

		int endpos = temp.indexOf(". ", position) + 1;
		//if (endpos == -1) endpos = temp.length();
		if (endpos == 0) endpos = temp.length();
//		System.out.println(endpos);

		temp = temp.substring(0, endpos);
//		System.out.println(temp);

		int startpos = temp.lastIndexOf(". ") + 2;
		
		//System.out.println("temp-6: '" + temp.substring(startpos-6) + "'");
		if (startpos > 5 && temp.substring(startpos-6).matches("( e\\.g|\\.\\sal|.\\svs)\\.\\s.*"))
			startpos = temp.lastIndexOf(". ", startpos-6);
		
		//System.out.println();
		
//		System.out.println(startpos);
		if (startpos < 0) startpos = 0;

		if(startpos>position) startpos = position;

		return copy.
			substring(startpos,
				endpos);
	}


	/**
	 * TODO: currently, a TextRepository can only contain one text with an 'unknown' ID (=-1)
	 */
	@Override
	public int hashCode() {
		return this.ID.hashCode();
	}

	@Override
	public boolean equals(Object anObject) {
		boolean equal = false;
		if(anObject instanceof Text && anObject.hashCode()==this.hashCode()){
			equal = true;
		}
		return equal;
	}

	
	/**
	 * 
	 * @return
	 */
	public int getPMID() {
    	return PMID;
    }


	/**
	 * 
	 * @param pmid
	 */
	public void setPMID (int pmid) {
    	PMID = pmid;
    }


	/**
	 * 
	 * @param sentences
	 * @param xml - true if sentences are in XML format: &lt;SENT SID="123"&gt;&lt;plain|tagged&gt;...
	 */
	public void setSentences (Collection<String> sentences, boolean xml) {
		this.sentences = new LinkedList<String>();
		this.sentences.addAll(sentences);

		StringBuffer plain = new StringBuffer();
		for (String sent: sentences) {
			if (xml) {
				int sid = getSentenceID(sent);
				if (sid == -1)
					sid = sentenceIds.size();
				sentenceIds.add(sid);
				sent = xmlToPlainSentence(sent);
			} else {
				sentenceIds.add(sentenceIds.size());
			}
			plain.append(sent + " ");
		}

		setPlainText(plain.toString());
	}


	/**
	 * 
	 * @param sentences
	 * @param xml - true if sentences are in XML format: &lt;SENT SID="123"&gt;&lt;plain|tagged&gt;...
	 */
	public void setSentences (String[] sentences, boolean xml) {
		Vector<String> sents = new Vector<String>();
		for (String sent: sentences)
			sents.add(sent);
		setSentences(sents, xml);
	}


	/**
	 * 
	 * @param xmlSentence
	 * @return
	 */
	public int getSentenceID (String xmlSentence) {
		if (xmlSentence.matches(".*<SENT[^>]+(SID|sid)=\"(\\d+)\".*"))
			return Integer.parseInt(xmlSentence.replaceFirst(".*<SENT[^>]+(SID|sid)=\"(\\d+)\".*", "$2"));
		else
			return -1;
	}
	
	
	/**
	 * Returns the surrounding five sentences from a given sentence index.
	 * @param index
	 * @return
	 */
	public String getSurroundingPlainText (int index) {
		StringBuffer plain = new StringBuffer();
		int max = index+5;
		//if (max > size()-1) max = size()-1;
		for (int i = index-5; i <= max; i++) {
			if (i == size()) break;
			if (i >= 0) {
				String sent = getSentence(i);
				if (sentencesInXML || sent.indexOf("<") >= 0)
					sent = xmlToPlainSentence(sent);
				plain.append(sent + " ");
			} else
				max++;
		}
		return plain.toString();
	}


	/**
	 * 
	 * @param xmlSentence
	 * @return
	 */
	public String xmlToPlainSentence (String xmlSentence) {
		//System.err.println("Xml2plain: '" + xmlSentence + "'");
		xmlSentence = xmlSentence.replaceAll("<([A-Za-z0-9\\_\\:]+)(\\s[^>]*)?>(.*?)</\\1>", "$3");
		xmlSentence = xmlSentence.replaceAll("<([A-Za-z0-9\\_\\:]+)(\\s[^>]*)?>(.*?)</\\1>", "$3");
		xmlSentence = xmlSentence.replaceAll("<([A-Za-z0-9\\_\\:]+)(\\s[^>]*)?>(.*?)</\\1>", "$3");
		xmlSentence = xmlSentence.replaceAll("<([A-Za-z0-9\\_\\:]+)(\\s[^>]*)?>(.*?)</\\1>", "$3");
		xmlSentence = xmlSentence.replaceAll("<([A-Za-z0-9\\_\\:]+)(\\s[^>]*)?>(.*?)</\\1>", "$3");
		//System.err.println("           '" + xmlSentence + "'");
		return xmlSentence;
	}


	/**
	 * 
	 * @return
	 */
	public int size () {
		return sentences.size();
	}
	
	
	/**
	 * 
	 * @param sentence
	 * @param xml
	 */
	public void addSentence (String sentence, boolean xml) {
		if (xml) {
			int sid = getSentenceID(sentence);
			if (sid == -1)
				sid = sentenceIds.size();
			sentenceIds.add(sid);
			sentences.add(xmlToPlainSentence(sentence));
		} else {
			int sid = sentenceIds.size();
			sentenceIds.add(sid);
			sentences.add(sentence);
		}
	}


	/**
	 * 
	 * @param sentence
	 */
	public void addSentence (String sentence) {
		addSentence(sentence, false);
	}


	/**
	 * 
	 * @param sid
	 * @param sentence
	 */
	public void addSentence (int sid, String sentence) {
		addSentence(sid, sentence, false);
	}


	/**
	 * 
	 * @param sid
	 * @param sentence
	 */
	public void addSentence (int sid, String sentence, boolean xml) {
		sentenceIds.add(sid);
		addSentence(sentence, xml);
	}


	/**
	 * 
	 * @param index
	 * @return
	 */
	public String getSentence (int index) {
		if (index >= 0)
			return sentences.get(index);
		else if (index < 0) {
			int i = sentenceIds.size() + index;
			if (i >= 0)
				return sentences.get(i);
		}
		return null;
	}


	/**
	 * 
	 * @param sid 
	 * @return
	 */
	public String getSentenceBySentenceID (int sid) {
		return sentences.get(sid2index(sid));
	}


	/**
	 * 
	 * @param sid
	 * @return
	 */
	public int sid2index (int sid) {
		for (int i = 0; i < sentenceIds.size(); i++)
			if (sentenceIds.get(i) == sid)
				return i;
		return -1;
	}


	/**
	 * Returns the sentence ID for the given index position of a sentence.
	 * If the index is negative, returns the i-th last sentence (-1 returns the last,
	 * -2 the pre-last, and so on).
	 * @param index
	 * @return
	 */
	public int index2sid (int index) {
		if (sentenceIds.size() >= 0 && sentenceIds.size() > index) {
			return sentenceIds.get(index);
		} else if (index < 0) {
			int i = sentenceIds.size() + index;
			if (i >= 0)
				return sentenceIds.get(i);
		}// else
			return -1;
	}


	/**
	 * For testing only.
	 * @param args
	 */
	public static void main (String[] args) {
		/*Text text = new Text();
		text.setPlainText("Segregation data on LW in families of informative males show that the LW (Landsteiner-Wiener) blood group locus is closely linked to the complement C3 locus and to the locus for the Lutheran blood group. This finding also confirms the presence of a larger linkage group on chromosome 19, including now the loci for apoE, Le, C3, LW, Lu, Se, H, PEPD, myotonic dystrophy (DM), neurofibromatosis (NF) and familial hypercholesterolemia (FHC). Linkage of LW with the Lewis blood group locus could not be definitely established by the present family data, but small positive scores between LW and Le suggest that the Le locus is situated outside the C3-LW region.");

		// first sentence end mark ('.') is at 202
		// 2nd sentence (This ..) starts at 204 ('T')
		// 654 = 'o' in final word, "... region."
		// 656 = final '.'

		int pos = 656;
		System.out.println(text.getCharAt(pos-2) + "" + text.getCharAt(pos-1) + "" + text.getCharAt(pos) + "" + text.getCharAt(pos+1) + "" + text.getCharAt(pos+2));
		System.out.println("--'"  + text.getSentenceAround(pos) + "'--");
		*/
		Text text = new Text("1");
		System.out.println(text.xmlToPlainSentence("<SENT PMID=\"10464305\" SID=\"291\"><plain>Furthermore the specific activity of the <z:uniprot ids=\"P09516\">50-kDa protein</z:uniprot> increases on association of <z:uniprot ids=\"P12398\">mtHSP70</z:uniprot>.</plain></SENT>"));
	}
	
}
