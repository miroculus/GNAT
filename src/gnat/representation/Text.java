package gnat.representation;

import gnat.preprocessing.sentences.SentenceSplitter;
import gnat.preprocessing.sentences.SentenceSplitterRegex;
import gnat.retrieval.PubmedAccess;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

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

	public enum IdTypes {PMID, PMC, FILENAME, UNKNOWN};
	public enum SourceTypes {PLAIN, XML, MEDLINE_XML, MEDLINES_XML, PUBMED_XML, PUBMEDS_XML, UNKNOWN};
	
	/** */
	TextContextModel model;

	/** */
	public String ID = "-1";

	public IdTypes idType = IdTypes.UNKNOWN;
	
	public SourceTypes sourceType = SourceTypes.UNKNOWN;

	/** */
	public String title = "";
	
	/** */
	public String plainText = "";
	
	/** */
	public String originalXml = "";
	
	/** */
	public String annotatedXml = "";
	
	/** A JDOM Document representation of this text.<br>Will be set only if an XML input was used,
	 * for example via {@link TextFactory#loadTextFromMedlineXmlfile(String)}.<br>
	 * To update the XML
	 * content with annotations, use {@link #replaceTitleInDocument(String)} and {@link #replaceAbstractInDocument(String)}.<br>
	 * Print the current Document to a file using {@link #toXmlFile(String)}.
	 * */
	public Document jdocument; 
	
	/** */
	public String filename = "";

	/** */
	public int PMID = -1;


	/** All species that have been found in this text. */
	public Set<Integer> taxonIDs = new TreeSet<Integer>();
	/** */
	public Map<Integer, List<String>> taxonIdsToNames = new HashMap<Integer, List<String>>();


	public LinkedList<String> sentences = new LinkedList<String>();

	public LinkedList<Integer> sentenceIds = new LinkedList<Integer>();

	SentenceSplitter splitter = new SentenceSplitterRegex();

	/** If this flag is set, treats every incoming sentence as XML-formatted. {@link #getSurroundingPlainText(int))} 
	 *  will then strip the sentence off all XML tags. Flag can be set using the constructor {@link #Text(String, List, boolean)}. */
	public boolean sentencesInXML = false;
	
	/** */
	public LinkedList<TextAnnotation> annotations = new LinkedList<TextAnnotation>();


	/** */
	public Text (String id){
		setID(id);
	}


	/** */
	public Text (String id, String plainText){
		this(id);
		setPlainText(plainText);
		// TODO the text is read line-by-line, adding line breaks as there were -- for sentence splitting, these should be removed
		// TODO however, a white space needs to be introduced, potentially setting off the positions that need to be reported for the final output
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
	public Text (String id, List<String> sentences, boolean xml) {
		this(id);
		setSentences(sentences, xml);
		sentencesInXML = xml;

		TextContextModel tcm = new TextContextModel();
		tcm.addPlainText(this.getPlainText());
		this.setContextModel(tcm);
	}


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
	 * Returns the character at the specified text position.
	 * */
	public char getCharAt(int index){
		if (index < 0) return ' ';
		if (index > getPlainText().length() - 1) return ' ';
		return getPlainText().charAt(index);
	}


	/**
	 * 
	 * @param id
	 * @param sentences
	 */
	public Text (String id, List<String> sentences) {
		this(id);
		setSentences(sentences, false);

		TextContextModel tcm = new TextContextModel();
		tcm.addPlainText(this.getPlainText());
		this.setContextModel(tcm);
	}

	
	/**
	 * 
	 * @return
	 */
	public String getID () {
	    return ID;
    }


	/**
	 * 
	 * @param id
	 */
	public void setID (String id) {
		this.ID = id;
		this.idType = IdTypes.UNKNOWN;
		
		if (id.matches("\\d+")) {
			setPMID(Integer.parseInt(id));	         // default: numerical ID only
			idType = IdTypes.PMID;
		} else if (id.matches("\\d+\\.[a-z]+")) {          // PMID plus file extension
			String pm = id.replaceFirst("^(\\d+)\\.[a-z]+$", "$1");
			setPMID(Integer.parseInt(pm));
			idType = IdTypes.PMID;
		} else if (id.matches(".*\\/\\d+")) {            // path plus PMID, no file extension
			String pm = id.replaceFirst("^.*\\/(\\d+)$", "$1");
			setPMID(Integer.parseInt(pm));
			idType = IdTypes.PMID;
		} else if (id.matches(".*\\/\\d+\\.[a-z]+")) {   // path plus PMID plus file extension
			String pm = id.replaceFirst("^.*\\/(\\d+)\\.[a-z]+$", "$1");
			setPMID(Integer.parseInt(pm));
			idType = IdTypes.PMID;
		
		} else if (id.toLowerCase().matches("pmc\\d+")) {
			idType = IdTypes.PMC;
		}
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
	 * @return
	 */
	public boolean hasPMID () {
		return (PMID >= 1);
	}


	/**
	 * Returns the length of the plain text.
	 * @return
	 */
	public int length() {
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
	
	
	
	///////////////
	// Species information
	// Methods to set and get taxa that are discussed in this text.
	///////////////
	
	/**
	 * Sets a collection of taxa as recognized in this text.
	 * @param taxonIDs
	 */
	public void setTaxonIDs (Set<Integer> taxonIDs) {
		this.taxonIDs = taxonIDs;
	}

	
	/**
	 * Adds one taxon ID as a recognized species in this text.
	 * @param taxonId
	 */
	public void addTaxonId (int taxonId) {
		this.taxonIDs.add(taxonId);
	}
	

	/**
	 * Add a species occurrence (identified by an NER step, for example, or other heuristics)
	 * to this Text.<br><tt>name</tt> is the name of the species, as found in the text, but can
	 * also be a synonym etc. <tt>taxon</tt> refers to NCBI Taxonomy ID of the species, for instance,
	 * 9606 for human (H. sap), or 10090 for mouse (M. mus).
	 * @param taxon
	 * @param name
	 */
	public void addTaxonWithName (int taxon, String name) {
		List<String> allNames = taxonIdsToNames.get(taxon);
		if (allNames == null)
			allNames = new LinkedList<String>();
		
		taxonIDs.add(taxon);
		allNames.add(name);
		
		taxonIdsToNames.put(taxon, allNames);
	}


	/**
	 * Returns a set of taxon IDs occurring with the highest frequency in
	 * the text. The set can contain multiple IDs, if all these taxa occur
	 * with the same frequency. Most often, it will contain only one ID.
	 * @return
	 */
	public Set<Integer> getMostFrequentTaxons () {
		Set<Integer> result = new TreeSet<Integer>();
		
		// determine most frequent taxon(s)
		int max = 0;
		for (int tax: taxonIdsToNames.keySet()) {
			if (taxonIdsToNames.get(tax).size() > max) {
				result.clear();
				result.add(tax);
				max = taxonIdsToNames.get(tax).size();
			} else if (taxonIdsToNames.get(tax).size() == max) {
				result.add(tax);
			}
		}
		
		return result;
	}
	
	
	/**
	 * Returns the frequency with which one taxon was observed in this text
	 * (number of occurrences of a name that refer to this taxon) .
	 * @param taxon
	 * @return
	 */
	public int getTaxonFrequency (int taxon) {
		if (taxonIdsToNames.containsKey(taxon))
			return taxonIdsToNames.get(taxon).size();
		else return 0;
	}


	/**
	 * Adds species and their names (as they occurred in this text) to the map
	 * {@link #taxonIdsToNames}.
	 * @param map
	 */
	public void addTaxonToNameMap (Map<Integer, List<String>> map) {
		for (int taxon: map.keySet()) {
			if (taxonIdsToNames.containsKey(taxon)) {
				List<String> oldNames = taxonIdsToNames.get(taxon);
				oldNames.addAll(map.get(taxon));
				taxonIdsToNames.put(taxon, oldNames);
				taxonIDs.add(taxon);
			} else {
				taxonIdsToNames.put(taxon, map.get(taxon));
				taxonIDs.add(taxon);
			}
		}
	}

	
	///////////////

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
	 * TODO: currently, a {@link TextRepository} can only contain one text with an 'unknown' ID (=-1)
	 */
	@Override
	public int hashCode() {
		return this.ID.hashCode();
	}

	
	/** Checks for identify of two Texts (this and another one) using the text IDs.<br>
	 *  Therefore, a {@link TextRepository} can only contain one text with an "unknown" ID (-1). */
	@Override
	public boolean equals (Object anObject) {
		boolean equal = false;
		if(anObject instanceof Text && anObject.hashCode()==this.hashCode()){
			equal = true;
		}
		return equal;
	}

	
	/**
	 * Sentence 0 would typically be the title of a text.
	 * @param index
	 * @param plainText
	 */
	public void setSentence (int index, String plainText) {
		if (sentences == null)
			sentences = new LinkedList<String>();
		
		while (sentences.size() < (index+1))
			sentences.add("");
		
		sentences.set(0, plainText);
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

	
	///////////////
	// XML
	// Methods to handle the XML version of this Text, including a JDOM Document representation 
	// Fields: jdocument, originalXml, plainText
	///////////////
	
	/**
	 * 
	 * @param xml
	 */
	public void setPlainFromXml (String xml) {
		this.originalXml = xml;
		plainText = PubmedAccess.getAbstractsFromXML(xml)[0];
	}


	/**
	 * Uses the current content of {@link #originalXml} to extract the plain text from 
	 * the XML. Currently, assumes PubMed XML format and grabs only the content of
	 * ArticleTitle and AbstractText, see {@link PubmedAccess#getAbstractsFromXML(String)}.
	 * @param xml
	 */
	public void setPlainFromXml () {
		plainText = PubmedAccess.getAbstractsFromXML(this.originalXml)[0];
	}

	
	/**
	 * 
	 * @param xmlSentence
	 * @return
	 */
	public static String xmlToPlainSentence (String xmlSentence) {
		System.err.println("Xml2plain: '" + xmlSentence + "'");
		if (xmlSentence.matches("[\\s\\t]*<[^>]+>[\\s\\t]*"))
			xmlSentence = "";
		else {
			xmlSentence = xmlSentence.replaceAll("<([A-Za-z0-9\\_\\:]+)(\\s[^>]*)?>(.*?)</\\1>", "$3");
			xmlSentence = xmlSentence.replaceAll("<([A-Za-z0-9\\_\\:]+)(\\s[^>]*)?>(.*?)</\\1>", "$3");
			xmlSentence = xmlSentence.replaceAll("<([A-Za-z0-9\\_\\:]+)(\\s[^>]*)?>(.*?)</\\1>", "$3");
			xmlSentence = xmlSentence.replaceAll("<([A-Za-z0-9\\_\\:]+)(\\s[^>]*)?>(.*?)</\\1>", "$3");
			xmlSentence = xmlSentence.replaceAll("<([A-Za-z0-9\\_\\:]+)(\\s[^>]*)?>(.*?)</\\1>", "$3");
		}
		System.err.println("           '" + xmlSentence + "'");
		return xmlSentence.trim();
	}
	

	/**
	 * Writes the current XML content, as given in the JDOM Document {@link #jdocument},
	 * into the file specified by <tt>filename</tt>.<br>
	 * If {@link #jdocument} is not set, does not write anything to a file.
	 * <br><b>Note:</b> if the text in {@link #annotatedXml} was changed, for instance, using
	 * {@link #annotateXmlTitle(String)} or {@link #annotateXmlAbstract(String)}, you need to
	 * run {@link #buildJDocumentFromAnnotatedXml()} to propagate those changes to the
	 * the {@link #jdocument}!
	 * @param filename
	 */
	public void toXmlFile (String filename) {
		// if the filename contains a folder, make sure this folder exists
		if (filename.indexOf("/") >= 0) {
			String dir = filename.replaceFirst("^(.+)\\/(.+?)$", "$1");
			File DIR = new File(dir);
			DIR.mkdirs();
		}
		if (jdocument == null) return;
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(jdocument);
			StreamResult result = new StreamResult(new File(filename)); 
			transformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}

	
//	/**
//	 * Returns the current XML content, as given in the JDOM Document {@link #jdocument},
//	 * as a string.<br>
//	 * If {@link #jdocument} is not set, returns null.
//	 * <br><b>Note:</b> if the text in {@link #annotatedXml} was changed, for instance, using
//	 * {@link #annotateXmlTitle(String)} or {@link #annotateXmlAbstract(String)}, you need to
//	 * run {@link #buildJDocumentFromAnnotatedXml()} to propagate those changes to the
//	 * the {@link #jdocument}!
//	 * @param filename
//	 */
//	public String toXmlString () {
//		if (jdocument == null) return "";
//		TransformerFactory transformerFactory = TransformerFactory.newInstance();
//		Transformer transformer;
//		try {
//			Writer outWriter = new StringWriter();
//			transformer = transformerFactory.newTransformer();
//			DOMSource source = new DOMSource(jdocument);
//			//StreamResult result = new StreamResult(new File(filename)); 
//			StreamResult result = new StreamResult( outWriter );  
//			transformer.transform(source, result);
//			return outWriter.toString();
//		} catch (TransformerConfigurationException e) {
//			e.printStackTrace();
//		} catch (TransformerException e) {
//			e.printStackTrace();
//		}
//		return null;
//	}
//	
	
	/**
	 * Returns the current XML content, as given in the JDOM Document {@link #jdocument},
	 * as a string.<br>
	 * If {@link #jdocument} is not set, returns an empty string.<br>
	 * <br><b>Note:</b> if the text in {@link #annotatedXml} was changed, for instance, using
	 * {@link #annotateXmlTitle(String)} or {@link #annotateXmlAbstract(String)}, you need to
	 * run {@link #buildJDocumentFromAnnotatedXml()} to propagate those changes to the
	 * the {@link #jdocument}!
	 * @param filename
	 */
	public String toXmlString () {
		if (jdocument == null) return "";

		String output = "";
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(jdocument), new StreamResult(writer));
			output = writer.getBuffer().toString();//.replaceAll("\n|\r", "");
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}

		return output;
	}

	
	/**
	 * Replaces the current title of this Text with an annotated one (XML format).<br>
	 * Affects only the value of {@link #annotatedXml}. 
	 * <br><b>Note:</b> changes {@link #annotatedXml} only, need to run {@link #buildJDocumentFromAnnotatedXml()}
	 * to change the {@link #jdocument} that will be used in {@link #toXmlFile(String)} and {@link #toXmlString()}!
	 * @param annotatedTitle
	 */
	public void annotateXmlTitle (String annotatedTitle) {
		if (annotatedXml.length() == 0)
			annotatedXml = originalXml;
		String full = annotatedXml;
		Pattern p = Pattern.compile("<ArticleTitle>.*</ArticleTitle>", Pattern.UNIX_LINES | Pattern.MULTILINE);
		//System.err.println("!!!testing title!!!");
		//System.err.println(full);
		Matcher m = p.matcher(full);
		//if (m.matches())
		//	System.err.println("!!!matches-title!!!");
		full = m.replaceFirst("<ArticleTitle>" + annotatedTitle + "</ArticleTitle>");
		
		annotatedXml = full;
	}
	
	
	/**
	 * Replaces the current abstract of this Text with an annotated one (XML format).<br>
	 * Affects only the value of {@link #annotatedXml}.
	 * <br><b>Note:</b> changes {@link #annotatedXml} only, need to run {@link #buildJDocumentFromAnnotatedXml()}
	 * to change the {@link #jdocument} that will be used in {@link #toXmlFile(String)} and {@link #toXmlString()}!
	 * @param annotatedTitle
	 */
	public void annotateXmlAbstract (String annotatedAbstract) {
		if (annotatedXml.length() == 0)
			annotatedXml = originalXml;
		String full = annotatedXml;
		
		/////
		// TODO PubMed abstracts can be structured, for instance,
		// <Abstract>
		//   <AbstractText Label="METHODS" NlmCategory="METHODS"> ... </AbstractText>
		//   <AbstractText Label="RESULTS" NlmCategory="RESULTS"> ... </AbstractText>
		// </Abstract>
		// the code below will replace all these with one single <AbstractText> ... </AbstractText> element
		// which does not have those section/label information anymore!
		//
		// find and replace the first occurrence of any <AbstractText ..> ... </AbstractText> element
		Pattern p = Pattern.compile("<AbstractText[^>]*>.*</AbstractText>", Pattern.UNIX_LINES | Pattern.MULTILINE);
		Matcher m = p.matcher(full);
		//if (m.matches()) {
			try {
				full = m.replaceFirst("<AbstractText>" + annotatedAbstract + "</AbstractText>");
			} catch (IndexOutOfBoundsException i) {
				System.err.println("### " + i.getMessage() + "\n# " + annotatedAbstract+"\n###");
			} catch (IllegalArgumentException i) {
				System.err.println("### " + i.getMessage() + "\n# " + annotatedAbstract+"\n###");
			}
			// find and remove all further <AbstractText ..> elements 
			Pattern p2 = Pattern.compile("<AbstractText[^>]+>.*</AbstractText>", Pattern.UNIX_LINES | Pattern.MULTILINE);
			Matcher m2 = p2.matcher(full);
		//	if (m2.matches())
				full = m2.replaceAll("");
		//}
		//
		/////

		annotatedXml = full;
	}
	
	
	/**
	 * 
	 * @param prefix
	 */
	public void addPrefixToXml (String prefix) {
		Pattern p = Pattern.compile("<PubmedArticle", Pattern.DOTALL);
		Matcher m = p.matcher(annotatedXml);
		annotatedXml = m.replaceFirst("<PubmedArticle xmlns:" + prefix + "=\"http://gnat.sourceforge.net\"");
		
		m = p.matcher(originalXml);
		originalXml = m.replaceFirst("<PubmedArticle xmlns:" + prefix + "=\"http://gnat.sourceforge.net\"");
	}
	
	
	/**
	 * Generates a new DOM Document ({@link #jdocument}} based on the current value
	 * of {@link #annotatedXml}.<br>
	 * If {@link #annotatedXml} is not set (length = 0), will use {@link #originalXml}.
	 */
	public void buildJDocumentFromAnnotatedXml () {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    factory.setNamespaceAware(true);
	    DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			if (annotatedXml.length() > 0) {
				// some PubMed "XMLs" still have a non-escaped ampersand in there
				// ampersand
				annotatedXml = annotatedXml.replaceAll("&\\s", "&amp; ");
				annotatedXml = annotatedXml.replaceAll("&([A-Za-z0-9]\\W)", "&amp;$1");
				// less/greater than
				// "0<n<1"
				annotatedXml = annotatedXml.replaceAll("(\\d)\\<([A-Za-z])\\<(\\d)", "$1&lt;$2&lt;$3");
				annotatedXml = annotatedXml.replaceAll("\\<([\\d\\.\\s])", "&lt;$1");
				//annotatedXml = annotatedXml.replaceAll("(\\W?)\\<(\\W)", "$1&lt;$2"); // this will replace ALL '<'/'>' in the entire XML document
				//annotatedXml = annotatedXml.replaceAll("(\\W?)\\>(\\W)", "$1&gt;$2");
				annotatedXml = annotatedXml.replaceAll("([Pp])\\s\\<\\s", "$1 &lt; ");
				annotatedXml = annotatedXml.replaceAll("([Pp])\\s\\>\\s", "$1 &gt; ");
				annotatedXml = annotatedXml.replaceAll("([Pp])\\s\\<\\s0", "$1 &lt; 0");
				annotatedXml = annotatedXml.replaceAll("([Pp])\\<(\\s|0)", "$1&lt;$2");
				// special cases:
				annotatedXml = annotatedXml.replaceAll("\\<([A-Za-z])\\(", "&lt;$1(");     // T(x)<T(y)
				annotatedXml = annotatedXml.replaceAll("\\<mixed\\sH\\.", "&lt;mixed H."); // special case in Lupus/IBD...
				annotatedXml = annotatedXml.replaceAll("\\<or=", "&lt;or=");
				annotatedXml = annotatedXml.replaceAll("\\<\\/=", "&lt;/="); // in PubMed 9618483 "share </=50% identity"
				//
				annotatedXml = annotatedXml.replaceAll("\\s\\>([\\d\\.\\s])", " &gt;$1");
				//annotatedXml = annotatedXml.replaceAll("±", "&plusmn;");
				//annotatedXml = annotatedXml.replaceAll("±", "plus/minus");
				//annotatedXml = annotatedXml.replaceAll("([Pp]\\s*<)", "$1&lt;");

				//System.err.println("-----");
				//System.err.println(annotatedXml);
				//System.err.println("-----");
		    	jdocument = builder.parse(new ByteArrayInputStream(annotatedXml.getBytes()));
			} else {
				jdocument = builder.parse(new ByteArrayInputStream(originalXml.getBytes()));
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (org.xml.sax.SAXParseException e) {
			System.err.println("##### ERROR building XML doc from ");
			System.err.println(annotatedXml);
			System.err.println("#####");
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
