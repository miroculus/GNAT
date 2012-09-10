package gnat.analysis;

import gnat.representation.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

public class SpeciesIndicators {

	public static String[] mesh_names = {"Humans", "Drosophila", "Mice", "Rats", "Yeasts", "Arabidopsis"};
	
	//public static Map<String, Map<String, Integer>> species_to_tokenCounts = new HashMap<String, Map<String, Integer>>();
	public static Map<String, Set<String>> speciesObservedTokens = new HashMap<String, Set<String>>();
	public static Map<String, Map<String, Integer>> speciesObservedTokens_wCount = new HashMap<String, Map<String, Integer>>();
	
	public static void main (String[] args) {
		
		String dir = args[0];
		File DIR = new File(dir);
		String[] filelist = DIR.list();

		for (String filename: filelist) {
		
			// xml is a StringBuilder that contains single articles,
			// it will get reset for each new article encountered in the multi-article XML
			StringBuilder xml = new StringBuilder();
			// store the title, also gets reset for each new article encountered in the XML
			String title = "";
			String text  = "";
			Set<String> species = new HashSet<String>();
			// simply parse the XML file line by line, handling each article separately
			try {
				BufferedReader br  = null;
				if (filename.endsWith(".gz")) {
					//System.err.println("Opening a GZipped files");
					InputStream fileStream = new FileInputStream(dir + "/" + filename);
					InputStream gzipStream = new GZIPInputStream(fileStream);
					Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
					br = new BufferedReader(decoder);
				} else {
					br = new BufferedReader(new FileReader(filename));
				}
				
				String line;
				// parse the XML file, extract each individual article
				while ((line = br.readLine()) != null) {
					
					// the XML file will contain (potentially) multiple abstracts/text
					// start a new individual PubmedArticle, discard old lines
					if (line.matches(".*<(PubmedArticle|MedlineCitation)[\\s\\>].*")) {
						xml.setLength(0);
						xml.append(line);
						xml.append("\n");
						continue;
					}
	
					// keep appending lines for to current individual XML article
					xml.append(line);
					xml.append("\n");
					if (line.matches(".*<ArticleTitle>.*</ArticleTitle>.*")) {
						title = line.replaceFirst("^.*<ArticleTitle>(.*)</ArticleTitle>.*$", "$1");
					}
					
					// find out which species this text is relevant for
					for (String mesh_name: mesh_names) {
						if (line.matches(".*DescriptorName.*>" + mesh_name + "(\\/.*)?<.*"))
							species.add(mesh_name);
					}
	
					if (line.matches(".*</(PubmedArticle|MedlineCitation)>.*")) {
						if (species.size() > 0) {
							Text aText = new Text("unknown"); // dangerous; make sure to set ID immediately after!
							aText.setPlainFromXml(xml.toString());
							aText.title = title;
							
							text = aText.getPlainText();
							String[] tokens_a = text.split("(^|[\\;\\.\\,\\!\\:\\)\\]\\']*)[\\s\\=\\/\\:]+([\\(\\[\\']*|$)");
							Set<String> tokens = new HashSet<String>();
							for (String tok: tokens_a) {
								tok = tok.replaceFirst("\\W+$", "").replaceFirst("^\\W+", "").replaceFirst("\\'s$", "");
								if (!tok.matches("\\d.*")) // any numeral, range, percentage, numner+unit, ...
								if (tok.length() > 2)
								if (tok.matches(".*[a-z]+.*"))
								//if (!tok.matches("[A-Z][a-z][a-z]"))            // over-simplified amino acids
								if (!tok.matches("[A-Za-z]\\d[A-Za-z]"))          // simple molecules such as H2O
								if (!tok.matches(".*[\\(\\)\\[\\]\\&\"\\'±×≤≥\\+\\?\\s].*")) // special chars or white space
								if (!tok.matches(".*(lt|gt|amp)(\\;.*|$)"))       // XML escaped special chars
								if (!tok.matches("[A-Z]+\\-?\\d*\\-[a-z]+"))
								if (!tok.matches("^(http|www\\.)"))
								if (!tok.matches(".*\\.(edu|com|gov|org).*"))
								if (!tok.endsWith("-fold"))
								if (tok.indexOf("--") == -1)
									tokens.add(tok);
							}
							//
//							//Map<String, Integer> tokenCounts = species_to_tokenCounts.get()
//							for (String mesh_name: mesh_names) {
//								// get species-specific tokens from previous texts
//								Set<String> specificTokens = speciesSpecificTokens.get(mesh_name);
//								if (specificTokens == null) {
//									specificTokens = new HashSet<String>();
//									speciesSpecificTokens.put(mesh_name, specificTokens);
//								}
//								// add or remove tokens from the current text:
//								// if the species also occurs in this text, add the tokens: could be species-specific
//								if (species.contains(mesh_name)) {
//									specificTokens.addAll(tokens);
//								// if the species does not occur in this text, remove the tokens: not specific anymore
//								} else {
//									specificTokens.removeAll(tokens);
//								}
//							}

							for (String mesh_name: species) {
								Set<String> specificTokens = speciesObservedTokens.get(mesh_name);
								if (specificTokens == null) {
									specificTokens = new HashSet<String>();
									speciesObservedTokens.put(mesh_name, specificTokens);
								}
								specificTokens.addAll(tokens);
							}
						}

						// reset buffers etc.
						xml.setLength(0);
						species.clear();
					}
				}
				br.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

		//
		for (String mesh_name: mesh_names) {
			Set<String> specificTokens = speciesObservedTokens.get(mesh_name);
			if (specificTokens == null) {
				System.out.println("No tokens for species " + mesh_name);
				continue;
			}
			Set<String> speciesSpecificTokens = new TreeSet<String>();
			speciesSpecificTokens.addAll(specificTokens);
			//System.out.println("#Tokens found for " + mesh_name + ":");
			//System.out.println(speciesSpecificTokens);
			//System.out.println("----------");
			
			for (String other_mesh: mesh_names) {
				if (other_mesh.equals(mesh_name)) continue;
				Set<String> otherTokens = speciesObservedTokens.get(other_mesh);
				System.out.println("#Removing " + otherTokens.size() + " tokens found in " + other_mesh);
				speciesSpecificTokens.removeAll(otherTokens);
				//break;
			}
			
			System.out.println("#Tokens specific for " + mesh_name + ":");
			System.out.println(speciesSpecificTokens);
//			for (String tok: speciesSpecificTokens) {
//				System.out.println("'"+tok+"'");
//			}
			System.out.println("==========");
		}

	}
	
}
