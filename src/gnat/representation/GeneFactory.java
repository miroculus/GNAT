package gnat.representation;

import gnat.utils.FileHelper;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Provides static factory methods to obtain {@link gnat.representation.Gene}s and {@link gnat.representation.GeneRepository}s.
 * 
 * 
 * @author Joerg
 *
 */
public class GeneFactory {

	/** Directory in which to search for gene data files. */
	public static String dataDirectory = ".";

	/** Files and ordering in which to search for genes. Basic name is the gene's ID. */
	public static String[] fileExtensions = { ".obj", ".xml", ".dat" };

	/**
	 * @deprecated because not in use
	 * @param FILE
	 */
	@Deprecated public static GeneContextModel loadModelFromPlainTextFile(File FILE) {
		
		GeneContextModel gcm = new GeneContextModel();// ID);

		String[] lines = FileHelper.readFromFile(FILE);
		for (String line : lines) {// int i = 0; i < lines.length; i++) {
			// String line = lines[i];
			if (line.trim().length() == 0)
				continue;
			if (line.startsWith("#"))
				continue;

			String type = line.replaceFirst("^(.+?)\t(.*)?", "$1");
			line = line.replaceFirst("^(.+?)\t(.*)?", "$2");
			String[] cols = line.split("\t");

			if (type.equals("ID")) {
				System.out.println("# found ID " + cols[0]);
				gcm.ID = cols[0];

			}
			else if (type.equals("GC")) {
				// System.out.println("# found " + cols.length + " GO codes");
				gcm.addGOCodes(cols);

			}
			else if (type.equals("GT")) {
				// System.out.println("# found " + cols.length + " GO terms");
				gcm.addGOTerms(cols);

			}
			else if (type.equals("DIS")) {
				// System.out.println("# found " + cols.length + " diseases");
				gcm.addDiseases(cols);

			}
			else if (type.equals("FCT")) {
				// System.out.println("# found " + cols.length + " functions");
				gcm.addFunctions(cols);

			}
			else if (type.equals("KW")) {
				// System.out.println("# found " + cols.length + " keywords");
				gcm.addKeywords(cols);

			}
			else if (type.equals("LOC")) {
				// System.out.println("# found " + cols.length + " locations (subcellular, chromosomal, ..)");
				gcm.addLocations(cols);

			}
			else if (type.equals("OS")) {
				// System.out.println("# found " + cols.length + " species");
				// gcm.addOrigin(cols);

			}
			else if (type.equals("EC")) {
				// System.out.println("# found " + cols.length + " enzymatic activity (first entry only!)");
				gcm.addEnzymaticActivity(cols[0]);

			}
			else if (type.equals("TS")) {
				// System.out.println("# found " + cols.length + " tissue specifity");
				// gcm.addTissues(cols);

			}
			else if (type.equals("PTM")) {
				// System.out.println("# found " + cols.length + " post-translational modification");
				// gcm.addPostTranslationalModification(cols);

			}
			else if (type.equals("MUT")) {
				// System.out.println("# found " + cols.length + " known SNPs");
				// gcm.addKnownSNPs(cols);

			}
			else if (type.equals("INT")) {
				// System.out.println("# found " + cols.length + " interaction partners");
				// gcm.addInteractionPartners(cols);

			}
			else if (type.equals("EGS")) {
				// System.out.println("# found " + cols.length + " EntrezGene summary (first entry only!)");
				gcm.addEntrezGeneSummary(cols[0]);

			}
			else if (type.equals("GRIF")) {
				// System.out.println("# found " + cols.length + " gene RIFs");
				gcm.addGeneRIFs(cols);

			}
			else {
				System.err.println("Unknown annotation: " + type);
			}

		} // for each line


		return gcm;
	}

	/**
	 * Tries to load the data on a gene from a file. Searches for either an object, XML, or data file (.obj, .xml, .dat,
	 * respectively) in the current data directory. Stops after the first such file was found (order: obj, xml, dat).
	 * Assumes that all files for gene have the gene's ID as the basic file name, plus the according extension. <br>
	 * <br>
	 * Returns null if no such file was found.
	 *
	 * @deprecated because not in use
	 * @param ID
	 * @return
	 */
	@Deprecated public static Gene loadGeneForID(String ID)
	{
		Gene gene = null;

		// try all known file extensions
		for (int e = 0; e < fileExtensions.length; e++) {
			String filename = dataDirectory + ID + fileExtensions[e];
			File FILE = new File(filename);
			if (FILE.exists() && FILE.isFile()) {
				gene = loadGeneFromFile(FILE);
				// System.out.println("## gene.ID = " + gene.ID);
				if (gene != null && gene.isValid())
					break;
			}
		}

		return gene;
	}

	/**
	 * @deprecated because not in use
	 * @param FILE
	 * @return
	 */
	@Deprecated public static Gene loadGeneFromFile(File FILE)
	{
		Gene gene = null;
		String extension = FileHelper.getFileExtension(FILE);

		//
		if (extension.equals("obj")) {
			// System.out.println("## Trying to load object file");
			gene = readGeneFromObjectFile(FILE);
			if (gene == null) {
				System.err.println("# Error reading gene from object file (" + FILE.getName() + ")");
			}
			else {
				// System.out.println("# Loaded gene from object file, " + FILE.getName());
				return gene;
			}

			//
		}
		else if (extension.equals("xml")) {
			// System.out.println("## Trying to load xml file");

			//
		}
		else if (extension.equals("dat")) {
			// System.out.println("## Trying to load data file");

			gene = new Gene();

			GeneContextModel gcm = loadModelFromPlainTextFile(FILE);
			if (gcm.ID.equals("-1")) {
				gene.ID = guessIDFromFilename(FILE.getName());
				// System.out.println("## Guessing ID from filename: " + gene.ID);
				gcm.ID = gene.ID;
			}
			else {
				// System.out.println("## Setting ID according to GCM: " + gcm.ID);
				gene.ID = gcm.ID;
			}
			gene.setContextModel(gcm);
			gene.hasModel = gcm.isValid();

			if (!gene.hasModel()) {
				System.err.println("# No proper context model for gene " + gene.ID);
			}
			else {
				// System.out.println("# Loaded gene from plain text file, " + FILE.getName());
				if (gene.isValid()) {
					File objFile = new File(dataDirectory + gene.ID + ".obj");
					System.out.println("# Writing gene to object file, " + objFile.getName());
					writeGeneToObjectFile(gene, objFile);
				}
			}

		}
		else {
			System.err.println("# Don't know how to handle files of type '" + extension + "' (" + FILE.getName() + ")");
		}

		return gene;
		// first, try to load it from an object file that contains a Gene
		/*
		 * Gene gene = readGeneFromObjectFile(FILE); // if this was not successful, treat the input file as plain text
		 * and create a new Gene if (gene == null) { gene = new Gene(); GeneContextModel gcm =
		 * loadModelFromPlainTextFile(FILE); if (gcm.ID.equals("-1")) { gene.ID = guessIDFromFilename(FILE.getName());
		 * gcm.ID = gene.ID; } else { gene.ID = gcm.ID; } gene.setContextModel(gcm); gene.hasModel = gcm.isValid(); if
		 * (!gene.hasModel()) { System.err.println("# No proper context model for gene " + gene.ID); } else {
		 * System.out.println("# Loaded gene from plain text file, " + FILE.getName()); if (gene.isValid()) { File
		 * objFile = new File(dataDirectory + gene.ID + ".obj"); System.out.println("# Writing gene to object file, " +
		 * objFile.getName()); writeGeneToObjectFile(gene, objFile); } } } else { System.out.println("# Loaded gene from
		 * object file, " + FILE.getName()); } return gene;
		 */
	}


	/**
	 * @param filename
	 * @return
	 */
	public static String guessIDFromFilename(String filename)
	{
		String ID = filename;
		ID = ID.replaceFirst("^(.+?)\\..+$", "$1");
		return ID;
	}

	/**
	 * @param IDs
	 * @return
	 */
	@Deprecated public static LinkedList<Gene> loadGenesFromDirectory(String[] IDs)
	{
		LinkedList<Gene> allgenes = new LinkedList<Gene>();

		// get list of file names
		// File DIR = new File(dataDirectory);
		// String[] list = DIR.list();

		/*
		 * TreeSet<String> ids = new TreeSet<String>(); for (String id: IDs) ids.add(id);
		 */

		// get list of genes in this directory; each gene can have multiple files
		// (obj, xml, dat), try to read them in this order (by taking only the first
		// such file that was found)
		/*
		 * LinkedList<String> idlist = new LinkedList<String>(); String ID; for (String name: list) { ID =
		 * guessIDFromFilename(name); if (!ids.contains(ID)) continue; if (!idlist.contains(ID)) idlist.add(ID); }
		 */
		// System.out.println("# IDs in directory = " + idlist.size());
		// load all genes for which we found an ID
		for (String id : IDs) {
			Gene gene = loadGeneForID(id);
			allgenes.add(gene);
		}

		return allgenes;
	}

	/**
	 * @return
	 */
	@Deprecated public static LinkedList<Gene> loadAllGenesFromDirectory()
	{
		LinkedList<Gene> allgenes = new LinkedList<Gene>();

		// get list of file names
		File DIR = new File(dataDirectory);
		String[] list = DIR.list();

		// get list of genes in this directory; each gene can have multiple files
		// (obj, xml, dat), try to read them in this order (by taking only the first
		// such file that was found)
		LinkedList<String> idlist = new LinkedList<String>();
		String ID;
		for (String name : list) {
			ID = guessIDFromFilename(name);
			if (!idlist.contains(ID))
				idlist.add(ID);
		}
		// System.out.println("# IDs in directory = " + idlist.size());

		// load all genes for which we found an ID
		for (String id : idlist) {
			Gene gene = loadGeneForID(id);
			allgenes.add(gene);
		}

		return allgenes;
	}
	
	
	/**
	 * 
	 * @param tsv
	 * @return
	 */
	public static Gene makeGeneFromTsv (String tsv) {
		String[] cols = tsv.split("\t");
		Gene gene = new Gene(cols[0]);
		
		if (cols[1].matches("\\d+")) gene.taxon = Integer.parseInt(cols[1]);
		else gene.taxon = -1;
		
		String geneRefs = cols[2];
		if (geneRefs != null && geneRefs.length() > 0) gene.officialSymbol = geneRefs;
		
		String names   = cols[3];
		if (names!=null && names.length() > 0)
			for (String name : names.split("\\;\\s"))
				gene.addName(name);

		GeneContextModel gcm = new GeneContextModel();

		String chrLoc = cols[4];
		if (chrLoc != null && chrLoc.length() > 0) gcm.addLocations(chrLoc.split("\\; "));

		String summary = cols[5];
		if (summary != null && summary.length() > 0) gcm.addEntrezGeneSummary(summary);

		String geneRifs = cols[6];
		if (geneRifs != null && geneRifs.length() > 0) gcm.addGeneRIFs(geneRifs.split("\\; "));

		String pmids = cols[7];
		if (pmids != null && pmids.length() > 0) gcm.addPubMedReferences(pmids.split("\\; "));


		String mass = cols[8];
		if (mass != null && mass.length() > 0) gcm.addProteinMass(mass.split("\\; "));

		String length = cols[9];
		if (length != null && length.length() > 0) gcm.addProteinLengths(length.split("\\; "));

		String mutation = cols[10];
		if (mutation != null && mutation.length() > 0) gcm.addProteinMutations(mutation.split("\\; "));

		String domain = cols[11];
		if (domain != null && domain.length() > 0) gcm.addProteinDomains(domain.split("\\; "));

		String subcell = cols[12];
		if (subcell != null && subcell.length() > 0) gcm.addSubcellularLocations(subcell.split("\\; "));

		String tissue = cols[13];
		if (tissue != null && tissue.length() > 0) gcm.addTissues(tissue.split("\\; "));
		
		
		String interact = cols[14];
		if (interact != null && interact.length() > 0) gcm.addProteinInteractions(interact.split("\\; "));
		
		String function = cols[15];
		if (function != null && function.length() > 0) gcm.addFunctions(function.split("\\; "));
		
		String disease = cols[16];
		if (disease != null && disease.length() > 0) gcm.addDiseases(disease.split("\\; "));
		
		
		String goCodes = cols[17];
		if (goCodes != null && goCodes.length() > 0) gcm.addGOCodes(goCodes.split("\\;"));

		String keywords = cols[18];
		if (keywords != null && keywords.length() > 0) gcm.addKeywords(keywords.split("\\;"));
		
		

		gene.setContextModel(gcm);
		gene.hasModel = gcm.isValid();
		
		return gene;
	}


	/**
	 * 
	 * @return
	 */
	@Deprecated public static GeneRepository loadGeneRepositoryFromDirectory()
	{
		GeneRepository geneRepository = new GeneRepository();
		List<Gene> genes = loadAllGenesFromDirectory();
		geneRepository.addGenes(genes);
		return geneRepository;
	}

	/**
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws ClassNotFoundException
	 */
	public static GeneRepository loadGeneRepository (String entrezGeneOneSynonymPerLineFile) throws FileNotFoundException, IOException, ClassNotFoundException {
		return loadGeneRepositoryFromEntrezGeneObjectFiles(entrezGeneOneSynonymPerLineFile, null, null, null, null, null, null, null, null, null, null, null, null, null);
	}


	/****/
	public static GeneRepository loadGeneRepositoryFromPropertiesFile(String propertiesFile) throws FileNotFoundException, IOException, ClassNotFoundException{
		Properties properties = new Properties();
		properties.load(new FileInputStream(new File(propertiesFile)));

		String geneRifs = properties.getProperty("humanGeneRifs");
		String goIds = properties.getProperty("humanGOIds");
		String goTerms = properties.getProperty("humanGOTerms");
		String summaries = properties.getProperty("humanSummaries");
		String diseases = properties.getProperty("humanDiseases");
		String functions = properties.getProperty("humanFunctions");
		String keywords = properties.getProperty("humanKeywords");
		String locations = properties.getProperty("humanLocations");
		String tissues = properties.getProperty("humanTissues");
		String pmids = properties.getProperty("humanPMIDs");
		String pmutations = properties.getProperty("humanProteinMutations");
		String plengths = properties.getProperty("humanProteinLengths");
		String pdomains = properties.getProperty("humanProteinDomains");

		String lexicon = properties.getProperty("humanGeneNameLexicon");

		return loadGeneRepositoryFromEntrezGeneObjectFiles(lexicon, geneRifs, goIds, goTerms, summaries, diseases, functions, keywords, locations, tissues, pmids, pmutations, plengths, pdomains);
	}

	/**
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	public static GeneRepository loadGeneRepositoryFromEntrezGeneObjectFiles (
			String entrezGeneOneSynonymPerLineFile,
			String geneRifs,
			String goIds,
			String goTerms,
			String summaries,
			String diseases,
			String functions,
			String keywords,
			String locations,
			String tissues,
			String pmids,
			String pmutations,
			String plengths,
			String domains
			)
					throws FileNotFoundException, IOException, ClassNotFoundException {

		Map<String, Set<String>> geneIdToNames = new HashMap<String, Set<String>>();
	    BufferedReader reader = new BufferedReader(new FileReader(entrezGeneOneSynonymPerLineFile));
	    String line = null;
	    while((line=reader.readLine())!=null){
	    	String[] parts = line.split("\t");

	    	if(parts.length<=1)
	    		continue;

	    	String geneId = parts[0];
	    	String name = parts[1].trim();

	    	Set<String> names = geneIdToNames.get(geneId);
	    	if(names==null){
	    		names = new HashSet<String>();
	    		geneIdToNames.put(geneId, names);
	    	}

	    	names.add(name);
	    }
	    reader.close();


		Map<String, GeneContextModel> contextModelTable = new HashMap<String, GeneContextModel>();

		HashMap<String, String[]> objectTable;


		ObjectInputStream ois;

		// generifs
		if(geneRifs!=null){
			ois = new ObjectInputStream(new FileInputStream(geneRifs));
			objectTable = (HashMap<String, String[]>) ois.readObject();
			ois.close();
			for (String geneId : geneIdToNames.keySet()) {
				String[] rifs = objectTable.get(geneId);
				if(rifs!=null){
					GeneContextModel gcm = contextModelTable.get(geneId);
					if (gcm == null) {
						gcm = new GeneContextModel();
						contextModelTable.put(geneId, gcm);
					}
					gcm.addGeneRIFs(rifs);
				}
			}
		}

		// goIds
		if(goIds!=null){
			ois = new ObjectInputStream(new FileInputStream(goIds));
			objectTable = (HashMap<String, String[]>) ois.readObject();
			ois.close();
			for (String geneId : geneIdToNames.keySet()) {
				String[] goCodes = objectTable.get(geneId);
				if(goCodes!=null){
					GeneContextModel gcm = contextModelTable.get(geneId);
					if (gcm == null) {
						gcm = new GeneContextModel();
						contextModelTable.put(geneId, gcm);
					}
					gcm.addGOCodes(goCodes);
				}
			}
		}

		// goTerms
		if(goTerms!=null){
			ois = new ObjectInputStream(new FileInputStream(goTerms));
			objectTable = (HashMap<String, String[]>) ois.readObject();
			ois.close();
			for (String geneId : geneIdToNames.keySet()) {
				String[] goTermLabels = objectTable.get(geneId);
				if(goTermLabels!=null){
					GeneContextModel gcm = contextModelTable.get(geneId);
					if (gcm == null) {
						gcm = new GeneContextModel();
						contextModelTable.put(geneId, gcm);
					}
					gcm.addGOTerms(goTermLabels);
				}
			}
		}

		// summaries
		if(summaries!=null){
			ois = new ObjectInputStream(new FileInputStream(summaries));
			objectTable = (HashMap<String, String[]>) ois.readObject();
			ois.close();
			for (String geneId : geneIdToNames.keySet()) {
				String[] summaryArray = objectTable.get(geneId);
				if(summaryArray!=null && summaryArray.length>0){
					GeneContextModel gcm = contextModelTable.get(geneId);
					if (gcm == null) {
						gcm = new GeneContextModel();
						contextModelTable.put(geneId, gcm);
					}
					gcm.addEntrezGeneSummary(summaryArray[0]);
				}
			}
		}

		// diseases
		if(diseases!=null){
			ois = new ObjectInputStream(new FileInputStream(diseases));
			objectTable = (HashMap<String, String[]>) ois.readObject();
			ois.close();
			for (String geneId : geneIdToNames.keySet()) {
				String[] diseaseArray = objectTable.get(geneId);
				if(diseaseArray!=null){
					GeneContextModel gcm = contextModelTable.get(geneId);
					if (gcm == null) {
						gcm = new GeneContextModel();
						contextModelTable.put(geneId, gcm);
					}
					gcm.addDiseases(diseaseArray);
				}
			}
		}

		// functions
		if(functions!=null){
			ois = new ObjectInputStream(new FileInputStream(functions));
			objectTable = (HashMap<String, String[]>) ois.readObject();
			ois.close();
			for (String geneId : geneIdToNames.keySet()) {
				String[] functionArray = objectTable.get(geneId);
				if(functionArray!=null){
					GeneContextModel gcm = contextModelTable.get(geneId);
					if (gcm == null) {
						gcm = new GeneContextModel();
						contextModelTable.put(geneId, gcm);
					}
					gcm.addFunctions(functionArray);
				}
			}
		}

		// keywords
		if(keywords!=null){
			ois = new ObjectInputStream(new FileInputStream(keywords));
			objectTable = (HashMap<String, String[]>) ois.readObject();
			ois.close();
			for (String geneId : geneIdToNames.keySet()) {
				String[] keywordArray = objectTable.get(geneId);
				if(keywordArray!=null){
					GeneContextModel gcm = contextModelTable.get(geneId);
					if (gcm == null) {
						gcm = new GeneContextModel();
						contextModelTable.put(geneId, gcm);
					}
					gcm.addKeywords(keywordArray);
				}
			}
		}

		// locations
		if(locations!=null){
			ois = new ObjectInputStream (new FileInputStream(locations));
			objectTable = (HashMap<String, String[]>) ois.readObject();
			ois.close();
			for (String geneId : geneIdToNames.keySet()) {
				String[] locationArray = objectTable.get(geneId);
				if(locationArray!=null){
					GeneContextModel gcm = contextModelTable.get(geneId);
					if (gcm == null) {
						gcm = new GeneContextModel();
						contextModelTable.put(geneId, gcm);
					}
					gcm.addLocations(locationArray);
				}
			}
		}

		// tissues
		if(tissues!=null){
			ois = new ObjectInputStream (new FileInputStream(tissues));
			objectTable = (HashMap<String, String[]>) ois.readObject();
			ois.close();
			for (String geneId : geneIdToNames.keySet()) {
				String[] tissueArray = objectTable.get(geneId);
				if(tissueArray!=null){
					GeneContextModel gcm = contextModelTable.get(geneId);
					if (gcm == null) {
						gcm = new GeneContextModel();
						contextModelTable.put(geneId, gcm);
					}
					gcm.addTissues(tissueArray);
				}
			}
		}

		// references to PubMed
		if(pmids!=null){
			ois = new ObjectInputStream (new FileInputStream(pmids));
			objectTable = (HashMap<String, String[]>) ois.readObject();
			ois.close();
			for (String geneId : geneIdToNames.keySet()) {
				String[] pmidArray = objectTable.get(geneId);
				if(pmidArray!=null){
					GeneContextModel gcm = contextModelTable.get(geneId);
					if (gcm == null) {
						gcm = new GeneContextModel();
						contextModelTable.put(geneId, gcm);
					}
					gcm.addPubMedReferences(pmidArray);
				}
			}
		}

		// mutations
		if(pmutations!=null){
			ois = new ObjectInputStream (new FileInputStream(pmutations));
			objectTable = (HashMap<String, String[]>) ois.readObject();
			ois.close();
			for (String geneId : geneIdToNames.keySet()) {
				String[] mutationArray = objectTable.get(geneId);
				if(mutationArray!=null){
					GeneContextModel gcm = contextModelTable.get(geneId);
					if (gcm == null) {
						gcm = new GeneContextModel();
						contextModelTable.put(geneId, gcm);
					}
					gcm.addProteinMutations(mutationArray);
				}
			}
		}

		// protein length
		if(plengths!=null){
			ois = new ObjectInputStream (new FileInputStream(plengths));
			objectTable = (HashMap<String, String[]>) ois.readObject();
			ois.close();
			for (String geneId : geneIdToNames.keySet()) {
				String[] lengthArray = objectTable.get(geneId);
				if(lengthArray!=null){
					GeneContextModel gcm = contextModelTable.get(geneId);
					if (gcm == null) {
						gcm = new GeneContextModel();
						contextModelTable.put(geneId, gcm);
					}
					gcm.addProteinLengths(lengthArray);
				}
			}
		}

		// domains
		if(domains!=null){
			ois = new ObjectInputStream (new FileInputStream(domains));
			objectTable = (HashMap<String, String[]>) ois.readObject();
			ois.close();
			for (String geneId : geneIdToNames.keySet()) {
				String[] domainArray = objectTable.get(geneId);
				if(domainArray!=null){
					GeneContextModel gcm = contextModelTable.get(geneId);
					if (gcm == null) {
						gcm = new GeneContextModel();
						contextModelTable.put(geneId, gcm);
					}
					gcm.addProteinDomains(domainArray);
				}
			}
		}


		GeneRepository geneRepository = new GeneRepository();

		for (String geneId : geneIdToNames.keySet()) {
			GeneContextModel gcm = contextModelTable.get(geneId);
			Set<String> names = geneIdToNames.get(geneId);
			Gene gene = new Gene(geneId);
			gene.setNames(names);
			if(gcm!=null){
				gene.setContextModel(gcm);
			}else{
				gene.setContextModel(new GeneContextModel());
			}
			geneRepository.addGene(gene);
        }

		return geneRepository;
	}

	/**
	 * Sets the directory that contains all data objects (files with gene info)
	 *
	 * @param dir
	 */
	public static void setDataDirectory(String dir)
	{
		dataDirectory = dir;
	}

	public static Gene readGeneFromObjectFile(File FILE)
	{
		Gene result = null;
		FileInputStream fis = null;
		ObjectInputStream in = null;
		try {
			fis = new FileInputStream(FILE);
			in = new ObjectInputStream(fis);
			result = (Gene) in.readObject();
			in.close();
		}
		catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}
		catch (StreamCorruptedException sce) {
			// sce.printStackTrace();
			return null;
		}
		catch (EOFException ee) {
			// ee.printStackTrace();
			return null;
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return result;
	}

	/**
	 * @param gene
	 * @return
	 */
	public static boolean writeGeneToObjectFile(Gene gene, File FILE)
	{
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			fos = new FileOutputStream(FILE);
			out = new ObjectOutputStream(fos);
			out.writeObject(gene);
			out.close();
		}
		catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}


	/**
	 * 
	 * @param geneRepository
	 * @param FILE
	 * @return
	 */
	public static boolean writeGeneRepositoryToObjectFile (GeneRepository geneRepository, File FILE) {
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			fos = new FileOutputStream(FILE);
			out = new ObjectOutputStream(fos);
			out.writeObject(geneRepository);
			out.close();
		}
		catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}


	/**
	 * 
	 * @param FILE
	 * @return
	 */
	public static GeneRepository loadGeneRepositoryFromFile (File FILE) {
		GeneRepository result = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE));
			result = (GeneRepository)ois.readObject();
			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

}
