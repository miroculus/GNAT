package gnat.filter.ner;

import gnat.ConstantsNei;
import gnat.ISGNProperties;
import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.RecognizedEntity;
import gnat.representation.Text;
import gnat.representation.TextAnnotation;
import gnat.representation.TextRange;
import gnat.representation.TextRepository;
import gnat.server.GnatService;
import gnat.utils.ArrayHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

/**
 * A filter that contacts an externally running {@link gnat.server.GnatService} to
 * annotate text(s) with named entities. Where this service is running has to be
 * specified in {@link gnat.ISGNProperties} via its property file.
 * 
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 */
public class GnatServiceNer implements Filter {
	
	/** For every text in the repository, will run gene NER for only the species
	 *  in <tt>limitedToTaxa</tt>, even if the text was assigned more/others.<br>
	 *  {@link #excludeTaxa} overwrites <tt>limitedToTaxa</tt>. */
	Set<Integer> limitedToTaxa;
	
	/** Runs the service for all species except the ones stored in <tt>excludeTaxons</tt>,
	 *  even if these were assigned to a text.<br>
	 *  <tt>excludeTaxa</tt> overwrites {@link #limitedToTaxa}. */
	Set<Integer> excludeTaxa;
	
	/** If a text does not have a taxon assigned, the filter can look up a list of default
	 *  species in the properties file if <tt>true</tt>; otherwise, no gene NER will be run
	 *  for a text with no species if <tt>false</tt>. Default: false.*/
	public boolean useDefaultSpecies = false;
	
	/**
	 * 
	 * @param tasks - a list of annotation tasks to perform on the texts
	 */
	public GnatServiceNer (GnatService.Tasks... tasks) {
		limitedToTaxa = new HashSet<Integer>();
		excludeTaxa   = new HashSet<Integer>();
		
		for (GnatService.Tasks task: tasks) {
			// TODO if any Task needs initialization
		}
		
	}
	
	
	public void setLimitedToTaxa (int... taxa) {
		limitedToTaxa.clear();
		for (int taxon: taxa)
			limitedToTaxa.add(taxon);
	}
	
	
	public void setExcludeTaxa (int... taxa) {
		excludeTaxa.clear();
		for (int taxon: taxa)
			excludeTaxa.add(taxon);
	}
	
	
	/**
	 * 
	 */
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {
		if (textRepository == null) 
			throw new RuntimeException("No text repository set.");
		
		for (Text text : textRepository.getTexts()) {
			Set<Integer> taxa = text.taxonIDs;
			String speciesList = "";
			
			// no species assigned to this text yet? load default species
			if (useDefaultSpecies) {
				if (taxa == null || taxa.size() == 0)
					taxa = ISGNProperties.getDefaultSpecies();
			}
			
			taxa.retainAll(limitedToTaxa);
			
			taxa.removeAll(excludeTaxa);

			
			// still 0? then there was an error with the default species
			if (taxa == null || taxa.size() == 0) {
				if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.STATUS) >= 0)
					System.out.println("#GnatServiceNER: No valid species for this text (" + text.ID + "); skipping.");
				continue;
			}
			
			
			speciesList = ArrayHelper.joinArray(taxa.toArray(new Integer[taxa.size()]), ",");
			//System.out.println("#GnatServiceNer: speclist: " + speciesList);
			
			try {
				// Construct data
			    String data = URLEncoder.encode("returntype", "UTF-8") + "=" + URLEncoder.encode("tsv", "UTF-8")
			    	  + "&" + URLEncoder.encode("species", "UTF-8")    + "=" + URLEncoder.encode(speciesList, "UTF-8")
			    	  + "&" + URLEncoder.encode("text", "UTF-8")       + "=" + URLEncoder.encode(text.getPlainText(), "UTF-8");
	
			    //System.out.println("Call for text " + text.getID() + ":");
			    //System.out.println("  SERVER/?" + data);
			    
			    // Send data
			    URL url = new URL(ISGNProperties.getProperty("gnatServiceUrl"));
			    URLConnection conn = url.openConnection();
			    conn.setDoOutput(true);
			    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			    wr.write(data);
			    wr.flush();
	
			    // Get the response
			    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			    String line;
			    while ((line = rd.readLine()) != null)			    	
			    	addRecognizedEntity(context, text, line);

			    wr.close();
			    rd.close();

			} catch (UnsupportedEncodingException use) {
				use.printStackTrace();
			} catch (MalformedURLException mue) {
				mue.printStackTrace();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		    
		}
	}
	
	
	/**
	 * Handles NER through an online {@link GnatService}, for a restricted set of species. The set of
	 * species is further limited by the taxa assigned to a Text.<br>
	 * If a Text has a species assigned that is not in <tt>restrictedTaxa</tt>, will not run the 
	 * service for that species on that Text.
	 * 
	 * @param context
	 * @param textRepository
	 * @param geneRepository
	 * @param taxa
	 */
	public void filterX (Context context, TextRepository textRepository, GeneRepository geneRepository, Set<Integer> restrictedTaxa) {
		if (textRepository == null) 
			throw new RuntimeException("No text repository set.");
	}
	
	
	/**
	 * Adds an entity described in <tt>annotation</tt> to the <tt>context</tt>.
	 * <br>
	 * Format of annotations: tab-separated, as returned by the online GnatService, with fields for
	 * text ID, text cross-reference (source), entity tye (=gene), entity subtype (=species of the gene),
	 * entity candidate ID(s) [semi-colon separated], start position, end position, mention as found in the text.
	 * 
	 * 
	 * @param annotation
	 */
	void addRecognizedEntity (Context context, Text text, String annotation) {
		if (annotation.startsWith("<error") || annotation.startsWith("<message")) return;
		
		String[] cols = annotation.split("\t");
		//System.out.println("Annotation: " + annotation);
		
		if (cols.length < 8){
			if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.WARNINGS) >= 0)
				System.err.println("Warning: annotation '" + annotation + "' could not be split into at least 8 columns. [GnatServiceNER.java, addRecognizedEntity]");
			return;
		}
		
		//String textId   = cols[0];
		//String textXref = cols[1];
		String type     = cols[2]; // e.g., "gene", "goterm", "species"
		//String subtype  = cols[3]; // e.g., species for a gene, as taxon ID: "9606"; or GeneOntology branch
		String idString = cols[4]; // gene ID
		int startIndex  = Integer.parseInt(cols[5]);
		int endIndex    = Integer.parseInt(cols[6]);
		String evidence = cols[7];
		//float score     = Float.parseFloat(cols[8]);
		
		TextRange position = new TextRange(startIndex, endIndex);
		TextAnnotation.Type eType = TextAnnotation.Type.getValue(type);
		String[] ids = idString.split("\\s*[\\;\\,]\\s*");
		
		RecognizedEntity recognizedGeneName = new RecognizedEntity(text, new TextAnnotation(position, evidence, eType));
		context.addRecognizedEntity1(recognizedGeneName, ids);
	}
	
}
