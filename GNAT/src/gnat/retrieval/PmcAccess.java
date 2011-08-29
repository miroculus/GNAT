package gnat.retrieval;

import gnat.utils.StringHelper;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * Download articles from PubMedCentral (PMC) using PMC's Open Access Interface (OAI).<br>
 * Note that OAI returns articles only if they are open access and available as XML.
 * 
 * @author Joerg
 *
 */
public class PmcAccess {

	/** URL to download one full text article from PMC; append the PMC ID (without the "PMC", just the number). */
	public static String PMC_OAI_BASEURL = "http://www.pubmedcentral.nih.gov/oai/oai.cgi" +
			"?metadataPrefix=pmc&verb=GetRecord&identifier=oai:pubmedcentral.nih.gov:";


	/**
	 * Get the XML version of the article with the given PMC-ID.
	 * Returns an article only if it has open access and is available in XML form.
	 * @param pmcId
	 * @return
	 */
	public static String getArticle (int pmcId) {
		ResourceHelper rh = new ResourceHelper();
		
		String url = PMC_OAI_BASEURL + pmcId;
		
		String content = rh.getURLContent(url);
				
		return content;
	}
	
	
	/**
	 * Parses the given XML content of an PMC article and build a org.jdom.Document to return.
	 * @param content
	 * @return Document
	 * @throws IOException
	 * @throws JDOMException
	 */
	public static Document getDocument (String content) {
		Document document = null;
		SAXBuilder builder = new SAXBuilder();

		try {
			document = builder.build(new StringReader(content));
			//Element root = document.getRootElement();
			//root.addNamespaceDeclaration(Namespace.getNamespace("z", "http://www.openarchives.org/OAI/2.0/"));
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (JDOMException je) {
			je.printStackTrace();
		}

		return document;
	}
	
	
	/**
	 * 
	 * @param xml
	 * @return
	 */
	public static String getPlaintext (String xml) {
		String plain = "";

		String[] xmls = xml.split("[\r\n]+");
		
		// TODO error codes - when XML is not available / no OpenAccess
		if (xmls[0].matches(".*<error[^>]* code=.*"))
			return "";
		
		// rewrite the XML w/o namespaces
		xmls[0] = "<OAI-PMH><GetRecord><record><metadata>" +
				  "<article>";
		for (int x = 1; x < xmls.length; x++)
			xmls[x] = xmls[x].replaceAll(" xlink:", " ");
		xml = StringHelper.joinStringArray(xmls, "\n");
		
		Document document = null;
		SAXBuilder builder = new SAXBuilder();

		try {
			document = builder.build(new StringReader(xml));
			Element root = document.getRootElement();
			root.removeAttribute("xmlns");
			root.removeAttribute("xmlns:xsi");
			root.removeAttribute("xsi:schemaLocation");
			document.setRootElement(root);
			
			plain = getPlaintext(document);

		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (JDOMException je) {
			je.printStackTrace();
		}
		
		return plain;
	}
	
	
	/**
	 * Drill down and collect the text parts of an element and its children, childrens' children, and so on.
	 * @param item
	 * @return
	 */
	@SuppressWarnings("unchecked") // for List<Content> contents = item.getContent();
	static String augmentPlaintext (Element item) {
		String text = "";
		
		List<Content> contents = item.getContent();
		for (Content content: contents) {
			//if (content.getClass().equals("org.jdom.Text")) {
			if (content instanceof org.jdom.Text) {
				//System.out.println("found a piece of text: '" + ((Text)content).getValue() + "'");
				if (!((Text)content).getValue().matches("[\r\n\t\\s]+"))
					text += ((Text)content).getValue();
				else
					text += "\n";
			} else if (content instanceof org.jdom.Element) {
				//System.out.println("found a nested element: '" + ((Element)content).getName() + "'");
				text += augmentPlaintext((Element)content);
			} else {
				//System.out.println("found something else: " + content.getClass());
				//text += "";
			}
		}
		
		return text;
	}
	
	
	/**
	 * Extracts the actual text from an PMC article: title, abstract, section; removes all meta-information and XML tags.
	 * TODO Not fully implemented! Returns the title and abstract only.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String getPlaintext (Document document) {
		StringBuffer plain = new StringBuffer();
		
		//
		try {
			// get the title
			XPath xpArticleTitle = XPath.newInstance("/OAI-PMH/GetRecord/record/metadata/article/front/article-meta/title-group/article-title");
			List<Element> titles = xpArticleTitle.selectNodes(document);
			for (Element title: titles) {
				plain.append(title.getText());
				break; // get only the first title if there are multiple
			}
			
			// get the abstract
			XPath xpAbstract = XPath.newInstance("/OAI-PMH//record/metadata/article//article-meta//abstract");
			List<Element> abstracts = xpAbstract.selectNodes(document);
			for (Element abs: abstracts) {
				plain.append(augmentPlaintext(abs));
				break; // get only the first abstract if there are multiple
			}
			
			// get all sections
			XPath xpSections = XPath.newInstance("/OAI-PMH//record/metadata/article//article-meta//body//sec");
			List<Element> sections = xpSections.selectNodes(document);
			for (Element sec: sections) {
				plain.append(augmentPlaintext(sec));
			}
			
		} catch (JDOMException jde) {
			jde.printStackTrace();
		}
				
		return plain.toString();
	}
	
}
