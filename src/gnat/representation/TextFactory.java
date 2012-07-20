package gnat.representation;

import gnat.ConstantsNei;
import gnat.retrieval.PubmedAccess;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import uk.ac.man.documentparser.dataholders.Document;
import uk.ac.man.documentparser.input.DocumentIterator;

/**
 * A text factory reads input from files and generates Text objects from them.
 * This includes extracting the TextContextModel for each text.
 *
 * @author Joerg Hakenberg
 *
 */

public class TextFactory {

	/**
	 * Loads a text repository from the given directories.<br>
	 * Supported file formats (identified by extensions) are .txt, .xml, and .medline.xml.
	 *
	 * @param directories
	 * @return
	 */
	public static TextRepository loadTextRepositoryFromDirectories (Collection<String> directories) {
		TextRepository textRepository = new TextRepository();

		for (String dir: directories) {
			//Set<String> textIds = new TreeSet<String>();
	
			// read all plain texts from the directory
			File DIR = new File(dir);
			if (DIR.exists() && DIR.canRead()) {
				if (DIR.isDirectory()) {
					if (!dir.endsWith("/")) dir += "/";					
					// get the list of all files in that directory, load each if it is a .txt file
					String[] files = DIR.list();
					for (String filename : files) {
						if (filename.endsWith(".txt")){
							Text text = loadTextFromFile(dir + filename);
							textRepository.addText(text);
						} else if (filename.endsWith(".medline.xml")){
							Text text = loadTextFromMedlineXmlfile(dir + filename);
							textRepository.addText(text);
						} else if (filename.endsWith(".medlines.xml")){
							textRepository.addTexts(loadTextsFromMedlineSetXmlfile(dir + filename));
						} else if (filename.endsWith(".xml")) {
							Text text = loadTextFromXmlfile(dir + filename);
							textRepository.addText(text);
						}
					}

				} else {
					// load an individual file
					Text text = loadTextFromFile(dir);
					textRepository.addText(text);
					//textIds.add(text.ID);
				}
			}
		}
		
		if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.STATUS) >= 0)
			System.out.println("#TextRepository loaded with " + textRepository.size() + " texts.");

		return textRepository;
	}
	
	
	/**
	 * Loads a text repository from the given directories. Convenience methods that calls
	 * {@link #loadTextRepositoryFromDirectories(Collection)}.<br>
	 * Supported file formats (identified by extensions) are .txt, .xml, and .medline.xml.
	 *
	 * @param directories
	 * @return
	 */
	public static TextRepository loadTextRepositoryFromDirectories (String... directories) {
		List<String> listOfDirectories = new LinkedList<String>();
		
		for (String dir: directories)
			listOfDirectories.add(dir);
			
		return loadTextRepositoryFromDirectories(listOfDirectories);
	}


	/**
	 * Loads the given documents into the text repository.
	 * @param documents
	 * @return
	 */
	public static TextRepository loadTextRepository (DocumentIterator documents) {
		TextRepository textRepository = new TextRepository();

		for (Document d : documents){
			Text t = new Text(d.getID(), d.toString());
			textRepository.addText(t);
		}
		
		return textRepository;
	}


	/**
	 * Gets a {@link Text} from the given filename. The {@link Text}'s ID will be the filename minus
	 * its extension.
	 * 
	 * @param filename
	 * @return
	 */
	public static Text loadTextFromFile (String filename) {
		// remove the extension from the filename to get an ID
		String id = filename.replaceFirst("^(.+)\\..*?$", "$1");
		
		//System.out.println("id: "+ id);
		
		StringBuilder xml = new StringBuilder();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = br.readLine()) != null) {
				xml.append(line);
				xml.append("\n");
			}
			br.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		Text aText = new Text(id);
		aText.setPlainFromXml(xml.toString());

		// every Text needs a context model
		TextContextModel tcm = new TextContextModel(aText.ID);
		tcm.addPlainText(aText.getPlainText());

		// add the extracted context model to the text
		aText.setContextModel(tcm);

		return aText;
	}
	
	
	/**
	 * Gets a {@link Text} from the given filename. The {@link Text}'s ID will be the filename minus
	 * its extension.
	 * 
	 * @param filename
	 * @return
	 */
	public static Text loadTextFromXmlfile (String filename) {
		// remove the extension from the filename to get an ID
		String id = filename.replaceFirst("^(.+)\\..*?$", "$1");
		
		//System.out.println("id: "+ id);
		
		StringBuilder plaintext = new StringBuilder();
		StringBuilder xmltext   = new StringBuilder();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = br.readLine()) != null) {
				xmltext.append(line);
				xmltext.append("\n");
				
				plaintext.append(Text.xmlToPlainSentence(line));
				plaintext.append("\n");
			}
			br.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		// finally construct the Text to be returned
		Text aText = new Text(id, plaintext.toString());
		aText.originalXml = xmltext.toString();
		aText.sentencesInXML = true;

		// every Text needs a context model
		TextContextModel tcm = new TextContextModel(aText.ID);
		tcm.addPlainText(aText.getPlainText());

		// add the extracted context model to the text
		aText.setContextModel(tcm);

		return aText;
	}
	
	
	/**
	 * Gets a {@link Text} from the given filename.<br>
	 * The assumed format is MEDLINE XML ("PubmedArticle"), and the plain text will be taken from
	 * the ArticleTitle and AbstractText elements only.
	 * 
	 * @param filename
	 * @return
	 */
	public static Text loadTextFromMedlineXmlfile (String filename) {
		// remove the extension from the filename to get an ID
		String id = filename.replaceFirst("^(.*\\/)?(.+?)\\..*?$", "$2");
		
		//System.out.println("id: "+ id);
		
		StringBuilder xml = new StringBuilder();
		String title = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = br.readLine()) != null) {
				xml.append(line);
				xml.append("\n");
				
				if (line.matches(".*<ArticleTitle>.*</ArticleTitle>.*")) {
					title = line.replaceFirst("^.*<ArticleTitle>(.*)</ArticleTitle>.*$", "$1");
				}
			}
			br.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		Text aText = new Text(id);
		aText.setPlainFromXml(xml.toString());
		aText.title = title;

		String pmid = PubmedAccess.getPubMedIdFromXML(xml.toString());
		if (pmid != null && !pmid.equals("-1") && pmid.matches("\\d+")) {
			aText.setPMID(Integer.parseInt(pmid));
			aText.ID = pmid;
		}
		
		// every Text needs a context model
		TextContextModel tcm = new TextContextModel(aText.ID);
		tcm.addPlainText(aText.getPlainText());

		// add the extracted context model to the text
		aText.setContextModel(tcm);
		
		//aText.jdocument = PubmedAccess.getAbstractsAsDocument(aText.originalXml);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

	    factory.setNamespaceAware(true);
	    DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
		    aText.jdocument = builder.parse(new ByteArrayInputStream(aText.originalXml.getBytes()));
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return aText;
	}
	
	
	/**
	 * Gets a set of {@link Text Texts} from the given file.<br>
	 * The assumed format is MEDLINE XML ("PubmedArticleSet"), and the plain text will be taken from
	 * the ArticleTitle and AbstractText elements only.
	 * 
	 * @param filename
	 * @return
	 */
	public static Collection<Text> loadTextsFromMedlineSetXmlfile (String filename) {
		// remove the extension from the filename to get an ID
		String id = filename.replaceFirst("^(.*\\/)?(.+?)\\..*?$", "$2");
		
		List<Text> temp_texts = new LinkedList<Text>();
		
		StringBuilder xml = new StringBuilder();
		String title = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = br.readLine()) != null) {
				
				if (line.matches(".*<PubmedArticle.*")) {
					xml.setLength(0);
					xml.append(line);
					xml.append("\n");
					continue;
				}
				
				xml.append(line);
				xml.append("\n");
				if (line.matches(".*<ArticleTitle>.*</ArticleTitle>.*")) {
					title = line.replaceFirst("^.*<ArticleTitle>(.*)</ArticleTitle>.*$", "$1");
				}
				
				if (line.matches(".*</PubmedArticle>.*")) {
					Text aText = new Text(id);
					aText.setPlainFromXml(xml.toString());
					aText.title = title;

					String pmid = PubmedAccess.getPubMedIdFromXML(xml.toString());
					if (pmid != null && !pmid.equals("-1") && pmid.matches("\\d+")) {
						aText.setPMID(Integer.parseInt(pmid));
						aText.ID = pmid;
					}
					
					// every Text needs a context model
					TextContextModel tcm = new TextContextModel(aText.ID);
					tcm.addPlainText(aText.getPlainText());

					// add the extracted context model to the text
					aText.setContextModel(tcm);
					
					//aText.jdocument = PubmedAccess.getAbstractsAsDocument(aText.originalXml);
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

				    factory.setNamespaceAware(true);
				    DocumentBuilder builder;
					try {
						builder = factory.newDocumentBuilder();
					    aText.jdocument = builder.parse(new ByteArrayInputStream(aText.originalXml.getBytes()));
					} catch (ParserConfigurationException e) {
						e.printStackTrace();
					} catch (SAXException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					temp_texts.add(aText);
					//System.err.println("ADDED text " + aText.ID);
					
					// reset buffer
					xml.setLength(0);
				}
				
			}
			br.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		return temp_texts;
	}
}
