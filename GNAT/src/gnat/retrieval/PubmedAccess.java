package gnat.retrieval;

import gnat.preprocessing.sentences.SentenceSplitter;
import gnat.preprocessing.sentences.SentenceSplitterRegex;
import gnat.preprocessing.tokenization.PreTokenizer;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;


/**
 * Routines to query PubMed: keyword searches that return a list of PubMed IDs.
 * 
 * 
 * @author Joerg Hakenberg
 *
 */

public class PubmedAccess {			
	
	/** Namespace for Ali Baba tags: z*/
	public static final String NAMESPACE = "z";
	/** URI mapping for Ali Baba namespace: http://siegfried.informatik.hu-berlin.de */
	public static final String URI_NAMESPACE = "http://siegfried.informatik.hu-berlin.de";
	
	/** To get a PubMed eUtils query, put together the QUERYTERM-URI, the query string,
	 *  the RETMAXEXTENSION and the corresponding limit (if needed) */
	public static final String URI_EUTILS_PUBMEDIDSFORQUERYTERM =
		"http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&usehistory=y&term=";
	/** To get a PubMed eUtils query, put together the QUERYTERM-URI, the query string,
	 *  the RETMAXEXTENSION and the corresponding limit (if needed) */
	public static final String URI_EUTILS_PUBMEDIDSFORQUERYTERM_RETMAXEXTENSION = "&retmax=";
	
	/** Link to retrieve a set of PubMed citations for given IDs. Append the IDs, comma separated, to this string. */
	public static String URI_PUBMED_EUTILS_GETCITATIONS = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed"
			+ "&retmode=xml&rettype=abstract&id=";


	/** */
	public static final String URI_PUBMED_EUTILS_ELINK_RELATEDARTICLES = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/elink.fcgi?db=pubmed&cmd=neighbor&id=";
	
	static SentenceSplitter splitter = new SentenceSplitterRegex();
	
	
	/**
	 * Returns an array of abstract texts for a given array of Pubmed Ids. If
	 * unknown ids are encountered, the length of the array returned varies from
	 * the given array of ids, ie. it is of shorter length. Concatenates title
	 * and abstract text with a single white space.
	 * @param pubmedIDs
	 * @return String[]
	 */
	public static String[] getAbstracts (String[] pubmedIDs) {
		String content = getOriginalCitations(pubmedIDs);
		return PubmedAccess.getAbstractsFromXML(content);
	}


	/**
	 * Returns an array of abstract texts for a given array of Pubmed Ids. If
	 * unknown ids are encountered, the length of the array returned varies from
	 * the given array of ids, ie. it is of shorter length.
	 * @param pubmedIDs
	 * @return String[]
	 */
	public static String[][] getAbstractsAsTitleAndText (String[] pubmedIDs) {
		String content = getOriginalCitations(pubmedIDs);
		return PubmedAccess.getAbstractsAsTitleAndTextFromXML(content);

	}
	
	
	/**
	 * Fetches the citations for the given PubMed IDs and returns the plain result.
	 * @param pubmedIDs
	 * @return String
	 */
	public static String getOriginalCitations (String[] pubmedIDs) {
		StringBuffer query = new StringBuffer();
		for (int i = 0; i < pubmedIDs.length; i++)
			query.append(pubmedIDs[i] + ",");

		ResourceHelper rh = new ResourceHelper();
		/*String content = rh.getURLContent(Constants.URI_PUBMED_EUTILS_GETCITATIONS
				+ query.toString());*/
		String URL = URI_PUBMED_EUTILS_GETCITATIONS + query.toString();
		
		//int responseCode = rh.getResponseCode(URL)/100;
		String content = rh.getURLContent(URL);
		
		//if((responseCode!=1) && (responseCode!=2) && (responseCode!=3)){
		//	ErrorHelper.eUtils_down=true;
		//	ErrorHelper.eUtils_response_code=responseCode;
		//}
		
		return content;
	}
	
	
	/**
	 * Parses the given content, a MedlineCitationSet, and returns a 2D-array containing
	 * titles and abstract texts.
	 * @param content
	 * @return String[][]
	 */
	@SuppressWarnings("rawtypes")
	public static String[][] getAbstractsAsTitleAndTextFromXML (String content) {
		String[][] abs = new String[0][0];
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(new StringReader(content));
			Element root = doc.getRootElement();
			List articleList = root.getChildren("PubmedArticle");
			abs = new String[articleList.size()][2];
			Iterator articleIt = articleList.iterator();
			for (int i = 0; articleIt.hasNext(); i++) {
				Element pubmedArticleElement = (Element) articleIt.next();
				Element titleElement = pubmedArticleElement.getChild("MedlineCitation").getChild("Article").getChild("ArticleTitle");
				Element abstractElement = pubmedArticleElement.getChild("MedlineCitation").getChild("Article").getChild("Abstract");

				abs[i][0] = titleElement.getTextTrim();
				if (abstractElement != null) {
					Element abstractText = abstractElement.getChild("AbstractText");
					abs[i][1] = abstractText.getTextTrim();
				} else {
					abs[i][1] = "";
				}
			}
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return abs;
	}

	
	/**
	 * Parses the given content, a MedlineCitationSet, and returns a Document.<br>
	 * <br>
	 * Splits all texts (title and abstract) into sentences (element 'SENT') and adds
	 * attributes for PubMed ID and sentence ID to each sentence.
	 * @param content
	 * @return Document
	 * @throws IOException
	 * @throws JDOMException
	 */
	@SuppressWarnings("rawtypes")
	public static Document getAbstractsAsDocument (String content) {
		Document document = null;
		SAXBuilder builder = new SAXBuilder();

		try {
			document = builder.build(new StringReader(content));
			Element root = document.getRootElement();

			root.addNamespaceDeclaration(Namespace.getNamespace(NAMESPACE, URI_NAMESPACE));
			List articleList = root.getChildren("PubmedArticle");
			Iterator articleIt = articleList.iterator();
			for (int i = 0; articleIt.hasNext(); i++) {
				Element pubmedArticleElement = (Element) articleIt.next();
				Element medlineCitElement = pubmedArticleElement.getChild("MedlineCitation");
				String pmId = medlineCitElement.getChildTextTrim("PMID");
				Element titleElement = medlineCitElement.getChild("Article").getChild("ArticleTitle");
				Element abstractElement = pubmedArticleElement.getChild("MedlineCitation").getChild("Article").getChild("Abstract");

				// get Title
				String title = titleElement.getTextTrim();
				titleElement.setText("");
				Element textElement = new Element("text");
				titleElement.addContent(textElement);
				//String[] sentences = SentenceSplitter.split(title);
				String[] sentences = new String[]{title};
				int sentenceCounter = 0;
				for (int s = 0; s < sentences.length; s++) {
					// sentences[s] = XMLUtil.escapeXML(sentences[s]);
					Element sentElement = new Element("SENT");
					sentElement.setAttribute(new Attribute("PMID", pmId));
					sentElement.setAttribute(new Attribute("SID", ""+sentenceCounter));
					Element plainElement = new Element("plain");
					//sentences[s] = PreTokenizer.pretokenize(sentences[s]);
					//plainElement.setText(Tokenizer.tokenizeWithXMLMarkup(sentences[s]));
					plainElement.setText(PreTokenizer.pretokenize(sentences[s]));
					sentElement.addContent(plainElement); 				
					textElement.addContent(sentElement);
					sentenceCounter++;
				}

				// get Abstract
				if (abstractElement != null) {
					Element abstractTextElement = abstractElement.getChild("AbstractText");
					String abs = abstractTextElement.getTextTrim();
					abstractTextElement.setText("");
					textElement = new Element("text");
					abstractTextElement.addContent(textElement);
					//System.out.println("# PMA: abs = '" + abs + "'");
					sentences = splitter.split(abs);
					for (int s = 0; s < sentences.length; s++) {
						if (sentences[s].length() > 0) {
							// sentences[s] = XMLUtil.escapeXML(sentences[s]);
							//System.out.println("# PMA: sentence = '" + sentences[s] + "'");
							Element sentElement = new Element("SENT");
							sentElement.setAttribute(new Attribute("PMID", pmId));
							sentElement.setAttribute(new Attribute("SID", ""+sentenceCounter));
							Element plainElement = new Element("plain");
							plainElement.setText(PreTokenizer.pretokenize(sentences[s]));
							sentElement.addContent(plainElement);
							textElement.addContent(sentElement);
							sentenceCounter++;
						}
					}
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (JDOMException je) {
			je.printStackTrace();
		}

		return document;
	}


	/**
	 * Parses the given content, a MedlineCitationSet, and returns a Document.<br>
	 * Just adds the namespace, no sentence splitting, XML reduction, etc.
	 * @param content
	 * @return Document
	 * @throws IOException
	 * @throws JDOMException
	 */
	public static Document getAbstractsAsDocumentWithAnnotations (String content) {
		Document document = null;
		SAXBuilder builder = new SAXBuilder();

		try {
			document = builder.build(new StringReader(content));
			Element root = document.getRootElement();
			root.addNamespaceDeclaration(Namespace.getNamespace(NAMESPACE, URI_NAMESPACE));
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (JDOMException je) {
			je.printStackTrace();
		}

		return document;
	}


	public static String getPubMedIdFromXML (String xml) {
		String[] abs = new String[0];
		try {
			SAXBuilder builder = new SAXBuilder();
			
			Document doc = builder.build(new StringReader(xml));
			//Element root = doc.getRootElement(); // root should be "PubmedArticle"
			
			List articleList = null;
			// two cases: ROOT is an ArticleSet or an Article
			Element root = doc.getRootElement();
			if (root.getName().equals("MedlineCitationSet"))
				articleList = root.getChildren("MedlineCitation");
			else if (root.getName().equals("PubmedArticleSet"))
				articleList = root.getChildren("PubmedArticle");
			else if (root.getName().equals("PubmedArticle"))
				articleList = root.getChildren("MedlineCitation");
			else {
				articleList = new LinkedList();
				articleList.add(root);
			}
			
			//List articleList = root.getChildren("MedlineCitation");
			abs = new String[articleList.size()];
			Iterator articleIt = articleList.iterator();
			for (int i = 0; articleIt.hasNext(); i++) {
				Element pubmedArticleElement = (Element) articleIt.next();
				Element pmidElement = pubmedArticleElement.getChild("PMID");
				return pmidElement.getTextTrim();
			}
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "-1";
	}
	
	
	/**
	 * Returns an array of abstract texts from a MedlineCitationSet XML. Concatenates
	 * title and text with a single white space.
	 * @param xml
	 * @return String[]
	 */
	@SuppressWarnings("rawtypes")
	public static String[] getAbstractsFromXML (String xml) {
		//System.out.println("analyzing XML=>>" +xml + "<<");
		String[] abs = new String[0];
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(new StringReader(xml));
			
			List citationList = null;
			// two cases: ROOT is an ArticleSet or an Article
			Element root = doc.getRootElement();
			if (root.getName().equals("MedlineCitationSet")) {
				citationList = root.getChildren("MedlineCitation");
			// in a PubmedArticleSet ...
			} else if (root.getName().equals("PubmedArticleSet")) {
				// ... each entry is a PubmedArticle
				List articleList = root.getChildren("PubmedArticle");
				Iterator articleIt = articleList.iterator();
				for (int i = 0; articleIt.hasNext(); i++) {
					Element pubmedArticleElement = (Element) articleIt.next();
					// and MedlineCitation are childs of PubmedArticle
					Element citationElement = pubmedArticleElement.getChild("MedlineCitation");
					citationList.add(citationElement);
				}
				//articleList = root.getChildren("PubmedArticle");
			} else if (root.getName().equals("PubmedArticle"))
				citationList = root.getChildren("MedlineCitation");
			else {
				citationList = new LinkedList();
				citationList.add(root);
			}
			// was:
//			if (root.getName().equals("MedlineCitationSet")) {
//				citationList = root.getChildren("MedlineCitation");
//			} else if (root.getName().equals("PubmedArticleSet")) {
//				//citationList = root.getChildren("PubmedArticle");
//			} else if (root.getName().equals("PubmedArticle"))
//				citationList = root.getChildren("MedlineCitation");
//			else {
//				citationList = new LinkedList();
//				citationList.add(root);
//			}
			
			
			//System.err.println("#ROOT="+root.getName());
			
			//List articleList = root.getChildren("MedlineCitation");
			abs = new String[citationList.size()];
			Iterator articleIt = citationList.iterator();
			for (int i = 0; articleIt.hasNext(); i++) {
				Element pubmedArticleElement = (Element) articleIt.next();
				Element titleElement = pubmedArticleElement.getChild("Article").getChild("ArticleTitle");
				Element abstractElement = pubmedArticleElement.getChild("Article").getChild("Abstract");

				abs[i] = titleElement.getTextTrim();
				//System.err.println("#ADDING " + titleElement.getTextTrim());
				if (abstractElement != null) {
					//Element abstractText = abstractElement.getChild("AbstractText");
					//abs[i] += " " + abstractText.getTextTrim();
					
					List<Element> abstractTexts = abstractElement.getChildren("AbstractText");
					for (Element abstractText: abstractTexts) {
						abs[i] += " " + abstractText.getTextTrim();						
						//System.err.println("#ADDING " + abstractText.getTextTrim());
					}
					
				}

				// escape XML characters again, since getTextTrim() de-escapes them :(
				abs[i] = abs[i].replaceAll("&([A-Za-z]+[\\,\\.\\:\\!\\?\\(\\)\\s\\[\\]\\/])", "&amp;$1");
				//abs[i] = abs[i].replaceAll("&perce?nt", "%");
				abs[i] = abs[i].replaceAll("<", "&lt;");
				abs[i] = abs[i].replaceAll(">", "&gt;");
				
				//System.err.println("#ABS"+i+"="+abs[i]+"\n-----");
			}
			
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return abs;
	}


	/**
	 * Returns an array of PubMed IDs that are related to the given PubMed ID (or comma separated list
	 * of PubMed IDs), as calculated by PubMed Related Articles.
	 * @param pubmedIds
	 * @param max
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public static String[] getRelatedPubmedIdsForPubmedIds (String pubmedIds, int max) throws IOException {
		ResourceHelper rh = new ResourceHelper();//"true", proxyIP, proxyPort);
		String content = rh.getURLContent(
				//"http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term="
				//		+ query + "&retmax=" + max + "&usehistory=y"
				URI_PUBMED_EUTILS_ELINK_RELATEDARTICLES + pubmedIds
			);

		String[] pmIds = new String[0];
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(new StringReader(content));
			Element root = doc.getRootElement();
			// go down the document tree until we find the set of IDs
			Element linkSet = root.getChild("LinkSet");
			LinkedList<String> pmids = new LinkedList<String>();
			Element id;
			if (linkSet != null) {
				Element linkSetDb = linkSet.getChild("LinkSetDb");
				if (linkSetDb != null) {
					List links = linkSetDb.getChildren("Link");
					if (links != null) {
						Iterator linkIt = links.iterator();
						for (int i = 0; linkIt.hasNext(); i++) {
							id = ((Element)linkIt.next()).getChild("Id");
							pmids.add(id.getTextTrim());
							// return only a limited set of IDs, given by max
							if (pmids.size() == max) break;
						}
					}
				}
			}
			pmIds = new String[pmids.size()];
			for (int c = 0; c < pmids.size(); c++)
				pmIds[c] = pmids.get(c);
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}

		return pmIds;
	}


	/**
	 * Checks the query in PubMed eUtils and returns the number of citations matching the 
	 * query according to PubMed.
	 * @param query
	 * @return
	 * @throws IOException
	 */
	public static int getNumberOfPubMedIDsForQuery (String query) {
		//System.err.println("# encoding");
		try {
			query = URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		//System.err.println("# contructing Rh");
		ResourceHelper rh = new ResourceHelper();//false);//"true", proxyIP, proxyPort);
		String URL = URI_EUTILS_PUBMEDIDSFORQUERYTERM + query;
		//System.err.println("# getting content");
		String content = rh.getURLContent(URL);
		//System.err.println("# done");

		int number = 0;
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(new StringReader(content));
			Element root = doc.getRootElement();
			Element countElement = root.getChild("Count");
			if (countElement != null)
				number = Integer.parseInt(countElement.getTextTrim());
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			//throw e;
		}

		return number;
	}


	/**
	 * Checks the query in PubMed eUtils and returns the number of citations matching the 
	 * query according to PubMed.
	 * @param query
	 * @param setProxy
	 * @param proxyIP
	 * @param proxyPort
	 * @return
	 * @throws IOException
	 */
	public static int getNumberOfPubMedIDsForQuery (String query, String setProxy, String proxyIP, String proxyPort) {
		try {
			query = URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		ResourceHelper rh = new ResourceHelper();//setProxy, proxyIP, proxyPort);
		String URL = URI_EUTILS_PUBMEDIDSFORQUERYTERM + query;
		//System.err.println("# getting URL");
		String content = rh.getURLContent(URL);
		//System.err.println("# done");

		int number = 0;
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(new StringReader(content));
			Element root = doc.getRootElement();
			Element countElement = root.getChild("Count");
			if (countElement != null)
				number = Integer.parseInt(countElement.getTextTrim());
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			//throw e;
		}

		return number;
	}


	/**
	 * Returns an array of Pubmed IDs for a given Pubmed query.
	 * 
	 * @param query - the query, a keyword search, as entered in AliBaba by the user
	 * @param max - max. number of ids to return
	 */
	@SuppressWarnings("rawtypes")
	public static String[] getPubmedIDsForQuery (String query, int max) throws IOException {
		try {
			query = URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		ResourceHelper rh = new ResourceHelper();//"true", proxyIP, proxyPort);
		String URL = URI_EUTILS_PUBMEDIDSFORQUERYTERM + query +
					 URI_EUTILS_PUBMEDIDSFORQUERYTERM_RETMAXEXTENSION + max;
		String content = rh.getURLContent(URL);
		//System.out.println("# Content.length=" + content.length() + " for query '" + URL + "'");

		String[] pmIds = new String[0];//{"11259443"};
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(new StringReader(content));
			Element root = doc.getRootElement();
			Element idList = root.getChild("IdList");
			if (idList != null) {
				List ids = idList.getChildren("Id");
				if (ids != null) {
					pmIds = new String[ids.size()];
					Iterator idIt = ids.iterator();
					for (int i = 0; idIt.hasNext(); i++) {
						pmIds[i] = ((Element) idIt.next()).getTextTrim();
					}
				}
			}
		} catch (JDOMException e) {
			System.err.println("# Error parsing content of eUtils.getPubMedIDsForQuery. Content='" + content + "'");
			System.out.println("# Error parsing content of eUtils.getPubMedIDsForQuery. Content='" + content + "'");
			//e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}

		return pmIds;
	}


}
