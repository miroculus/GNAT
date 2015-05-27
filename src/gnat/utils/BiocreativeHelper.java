package gnat.utils;

import gnat.ISGNProperties;
import gnat.preprocessing.NameRangeExpander;
import gnat.preprocessing.sentences.SentenceSplitter;
import gnat.preprocessing.sentences.SentenceSplitterRegex;
import gnat.preprocessing.tokenization.SimpleTokenizer;
import gnat.representation.Context;
import gnat.representation.Evidence;
import gnat.representation.FeatureVector;
import gnat.representation.Gene;
import gnat.representation.GeneRepository;
import gnat.representation.IdentificationStatus;
import gnat.representation.RecognizedEntity;
import gnat.representation.ScoredName;
import gnat.representation.Text;
import gnat.representation.TextAnnotation;
import gnat.representation.TextFactory;
import gnat.representation.TextRange;
import gnat.representation.TextRepository;
import gnat.representation.VectorGenerator;
import gnat.retrieval.PubmedAccess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A helper offering various methods useful for generating gene profiles used in the Biocreative GN task.
 */
public class BiocreativeHelper {

	public static String stopwordsFilename = ISGNProperties.get("stopWords");
	public static Set<String> STOPWORDS;
	static{
		try {
	        STOPWORDS = FileHelper.readFileIntoSet(stopwordsFilename, false);
        }
        catch (IOException e) {
        	System.out.println("BiocreativeHelper: No Stop words available! File '"+stopwordsFilename+"' not found");
        	//e.printStackTrace();
        }
	}
	
	static SentenceSplitter splitter = new SentenceSplitterRegex();

	public static Set<Integer> musMusculusTaxIds;
	static{
		musMusculusTaxIds = new HashSet<Integer>();
		musMusculusTaxIds.add(10090);	// Mus musculus
		musMusculusTaxIds.add(35531);	// Mus musculus bactrianus
		musMusculusTaxIds.add(10091);	// Mus musculus castaneus
		musMusculusTaxIds.add(10092);	// Mus musculus domesticus
		musMusculusTaxIds.add(80274);	// Mus musculus gentilulus
		musMusculusTaxIds.add(179238);// Mus musculus homourus
		musMusculusTaxIds.add(57486);	// Mus musculus molossinus
		musMusculusTaxIds.add(39442);	// Mus musculus musculus
		musMusculusTaxIds.add(210727);// Mus musculus praetextus
		musMusculusTaxIds.add(46456);	// Mus musculus wagneri
	}

	public static Set<Integer> yeastTaxIds;
	static{
		yeastTaxIds = new HashSet<Integer>();
		yeastTaxIds.add(4932);	//
		yeastTaxIds.add(285006);	//
		yeastTaxIds.add(41870);	//
		yeastTaxIds.add(307796);	//

	}

	public static Set<Integer> flyTaxIds;
	static{
		flyTaxIds = new HashSet<Integer>();
		flyTaxIds.add(7227);

	}


	public static Set<Integer> humanTaxIds;
	static{
		humanTaxIds = new HashSet<Integer>();
		humanTaxIds.add(9606);

	}


	/**
	 * @throws IOException */
	public static Set<Integer> getDiscontinuedGenes(String ncbiGeneHistoryFile) throws IOException{
		Set<Integer> geneIds = new HashSet<Integer>();

		BufferedReader reader = new BufferedReader(new FileReader(ncbiGeneHistoryFile));
		String line;
		while((line=reader.readLine())!=null){
			if(line.startsWith("#"))
				continue;

			String[] cols = line.split("\t");

			String currentId = cols[1];
			Integer discontinuedId = Integer.parseInt(cols[2]);

			if(currentId.equals("-"))
				geneIds.add(discontinuedId);

		}
		reader.close();

		return geneIds;
	}


	/**
	 * @throws IOException
	 * @throws FileNotFoundException */
	public static void writeTextToGoAccessionObject(Context goAnnotatedContext, String outfile) throws FileNotFoundException, IOException{

		Map<String, String[]> textToGoCodes = new HashMap<String, String[]>();

		Set<Text> texts = goAnnotatedContext.getTexts();
		for (Text text : texts) {
			Set<String> goCodeSet = new HashSet<String>();
			Set<RecognizedEntity> terms = goAnnotatedContext.getRecognizedEntitiesInText(text);
			for (RecognizedEntity name : terms) {
				IdentificationStatus identificationStatus = goAnnotatedContext.getIdentificationStatus(name);
		        Set<String> candidateIds = identificationStatus.getIdCandidates();
		        for (String string : candidateIds) {
		        	if(string.startsWith("G")){
		        		goCodeSet.add("GO:"+string.substring(1));
		        		//System.out.println("Found GO code for text "+text.getID()+": "+string);
		        	}
	            }
            }

	        textToGoCodes.put(text.getID(), ArrayHelper.set2StringArray(goCodeSet));
        }

		ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(new File(outfile)));
		writer.writeObject(textToGoCodes);
		writer.close();
	}


	/**
	 * @throws IOException */
	public static void getPubmedIdsForTexts(String textDirectory, String outputFile) throws IOException{
		Map<String, String> textToPMID = new HashMap<String, String>();
//		String[] mappingLines = FileHelper.readFromFile(outputFile);
//		for (String string : mappingLines) {
//	        String textId = string.split("\t")[0];
//	        String pmId = string.split("\t")[1];
//	        if(!pmId.equals("-")){
//	        	textToPMID.put(textId, pmId);
//	        }
//        }

		FileWriter writer = new FileWriter(outputFile);

		TextRepository textRepository = TextFactory.loadTextRepositoryFromDirectories(textDirectory);
		for (Text text : textRepository.getTexts()) {
			if(textToPMID.containsKey(text.getID())){
				writer.write(text.getID()+"\t"+textToPMID.get(text.getID())+"\n");
				System.out.println("Skipping text "+text.getID());
			}else{
				String plainText = text.getPlainText();
				String[] sentences = splitter.split(plainText);
				boolean found = false;
				for (String sentence : sentences) {
					String[] result = PubmedAccess.getPubmedIDsForQuery("\""+sentence+"\"", 2);
					if(result!=null && result.length==1){
						String pmid = result[0];
						writer.write(text.getID()+"\t"+pmid+"\n");
						System.out.println("Found "+pmid+" for text "+text.getID());
						found = true;
						break;
					}
                }
				if(!found){
					writer.write(text.getID()+"\t-\n");
					System.err.println("No pmId found for text "+text.getID());
				}
			}
        }

		writer.close();
	}


	/***/
	@SuppressWarnings("unchecked")
    public static void showContent(String objectFile){
		Map<String, String[]> map = (Map<String, String[]>) FileHelper.readObjectFromFile(new File(objectFile));
		for (String key : map.keySet()) {
	        String[] data = map.get(key);
	        System.out.println("KEY = '"+key+"'");
		    for (String string : data) {
		    	System.out.println("\tDATAROW = '"+string+"'");
	        }
        }
	}

	/***/
	@SuppressWarnings("unchecked")
    public static void showContentOfGo2Go(String objectFile){
		Map<String, Float> map = (Map<String, Float>) FileHelper.readObjectFromFile(new File(objectFile));
		for (String key : map.keySet()) {
	        Float value = map.get(key);
	        System.out.println("KEY = '"+key+"', VALUE='"+value+"'");
        }
	}


	public static Map<String, String> getTextToPMIDMapping(String textToPMIdMappingFile) throws IOException{
		Map<String, String> devtextToPMId = new HashMap<String, String>();

		BufferedReader reader = new BufferedReader(new FileReader(textToPMIdMappingFile));
		String line;
		while((line=reader.readLine())!=null){
			String devtext = line.split("\t")[0];
			String pmid = line.split("\t")[1];
			devtextToPMId.put(devtext, pmid);
		}
		reader.close();

		return devtextToPMId;
	}


	/**
	 * @throws IOException */
	public static void generateEntrezGeneSummaryObject(Set<String> geneIds, String geneInfoFile, String outfile) throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader(new File(geneInfoFile)));

		Map<String, String[]> geneToSummary = new HashMap<String, String[]>();

		String line;
		while((line=reader.readLine())!=null){
			if(!line.startsWith("#")){
				String[] columns = line.split("\t");
				String geneId = columns[1];
		        String summary = columns[8];
		        if(geneIds.contains(geneId)){
		        	geneToSummary.put(geneId, new String[]{summary});
		        }
			}
		}
		reader.close();

		FileHelper.writeObjectToFile(geneToSummary, new File(outfile));
	}


	/**
	 * @throws IOException */
	public static void generateEntrezGeneGeneRifObject(Set<String> geneIds, String generifsFile, String outfile) throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader(new File(generifsFile)));

		Map<String, Set<String>> geneToGeneRIF = new HashMap<String, Set<String>>();


		String line;
		while((line=reader.readLine())!=null){
			if(!line.startsWith("#")){
				String[] columns = line.split("\t");
				String geneId = columns[1];
		        String geneRif = columns[4];
		        if(geneIds.contains(geneId)){
		        	Set<String> geneRifs = geneToGeneRIF.get(geneId);
		        	if(geneRifs==null){
		        		geneRifs = new HashSet<String>();
		        		geneToGeneRIF.put(geneId, geneRifs);
		        	}
		        	geneRifs.add(geneRif);
		        }
			}
		}
		reader.close();

		Map<String, String[]> geneIdToGeneRIFArray = new HashMap<String, String[]>();

		for (String geneId : geneToGeneRIF.keySet()) {
	        Set<String> geneRifs = geneToGeneRIF.get(geneId);
	        geneIdToGeneRIFArray.put(geneId, ArrayHelper.set2StringArray(geneRifs));
        }

		FileHelper.writeObjectToFile(geneIdToGeneRIFArray, new File(outfile));
	}


	/**
	 * @throws IOException */
	public static void generateEntrezGeneToUniprotObject(Set<Integer> geneIds, String gene2accessionFile, String outfile) throws IOException{
		Map<Integer, Set<String>> geneIdToUniProtId = getEntrezGeneToUniprotMapping(geneIds, gene2accessionFile);

		Map<Integer, String[]> geneIdToUniProtIdArray = new HashMap<Integer, String[]>();

		for (Integer geneId : geneIdToUniProtId.keySet()) {
	        Set<String> uniprotIds = geneIdToUniProtId.get(geneId);
	        geneIdToUniProtIdArray.put(geneId, ArrayHelper.set2StringArray(uniprotIds));
        }

		FileHelper.writeObjectToFile(geneIdToUniProtIdArray, new File(outfile));
	}


	/**
	 * @throws IOException */
	public static Map<Integer, Set<String>> getEntrezGeneToUniprotMapping(Set<Integer> geneIds, String gene2accessionFile) throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader(new File(gene2accessionFile)));

		Map<Integer, Set<String>> geneIdToUniProtId = new HashMap<Integer, Set<String>>();

		String line;
		while((line=reader.readLine())!=null){
			if(!line.startsWith("#")){
				String[] columns = line.split("\t");
				Integer geneId = Integer.parseInt(columns[1]);
				if(geneIds==null || geneIds.contains(geneId))
				{
					String proteinAccession = columns[5];
					if(looksLikeSwissProtAccession(proteinAccession))
					{
			        	if(proteinAccession.indexOf('.')!=-1)
			        	{
			        		proteinAccession = proteinAccession.substring(0, proteinAccession.indexOf('.'));
			        	}
			        	Set<String> uniprotIds = geneIdToUniProtId.get(geneId);
			        	if(uniprotIds==null){
			        		uniprotIds = new HashSet<String>();
			        		geneIdToUniProtId.put(geneId, uniprotIds);
			        	}
			        	uniprotIds.add(proteinAccession);
			        }
				}
			}
		}
		reader.close();

		return geneIdToUniProtId;
	}

	/**
	 * @throws IOException */
	public static Map<Integer, Set<String>> getEntrezGeneToUniprotMappingByTaxIds(Set<Integer> taxIds, String gene2accessionFile) throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader(new File(gene2accessionFile)));

		Map<Integer, Set<String>> geneIdToUniProtId = new HashMap<Integer, Set<String>>();

		String line;
		while((line=reader.readLine())!=null){
			if(!line.startsWith("#")){
				String[] columns = line.split("\t");
				Integer taxId = Integer.parseInt(columns[0]);
				if(taxIds.contains(taxId))
				{
					Integer geneId = Integer.parseInt(columns[1]);
			        String proteinAccession = columns[5];
			        if(looksLikeSwissProtAccession(proteinAccession))
			        {
			        	if(proteinAccession.indexOf('.')!=-1)
			        	{
			        		proteinAccession = proteinAccession.substring(0, proteinAccession.indexOf('.'));
			        	}
			        	Set<String> uniprotIds = geneIdToUniProtId.get(geneId);
			        	if(uniprotIds==null){
			        		uniprotIds = new HashSet<String>();
			        		geneIdToUniProtId.put(geneId, uniprotIds);
			        	}
			        	uniprotIds.add(proteinAccession);
			        }
				}
			}
		}
		reader.close();

		return geneIdToUniProtId;
	}

	/***/
	public static boolean looksLikeSwissProtAccession(String proteinAccession)
	{
		if(proteinAccession.startsWith("P") || proteinAccession.startsWith("Q") || proteinAccession.startsWith("O"))
			return true;
		else
			return false;
	}

	/***/
	public static Set<String> getAccessionNumbers(String synonymsFile) throws IOException{
		Set<String> accessions = new HashSet<String>();
		BufferedReader reader = new BufferedReader(new FileReader(synonymsFile));
		String line;
		while((line=reader.readLine())!=null){
			String id = line.split("\t")[0];
			accessions.add(id);
		}
		reader.close();
		return accessions;
	}


	/***/
	public static void generateAccessionNumberFile(String synonymListFile, String outfile) throws IOException{
		FileWriter writer = new FileWriter(outfile);
		BufferedReader reader = new BufferedReader(new FileReader(synonymListFile));
		String line;
		while((line=reader.readLine())!=null){
			String id = line.split("\t")[0];
			writer.write(id+"\n");
		}
		writer.close();
		reader.close();
	}


	/**
	 * @throws IOException */
	public static void generateYeastToEntrezGeneIdMapping(String yeastIdFile, String entrezGeneInfoFile, String outfile) throws IOException{

		Map<String, String> yeastIdToEntrezGeneId = new HashMap<String, String>();

		Set<String> yeastIds = FileHelper.readFileIntoSet(yeastIdFile, false);

		String[] geneInfoLines = FileHelper.readFromFile(new File(entrezGeneInfoFile));
		for (String string : geneInfoLines) {
			if(!string.startsWith("#")){
				String[] columns = string.split("\t");
		        if(columns[5].startsWith("SGD")){
		        	String[] idArray = columns[5].split("\\|");
		        	String yeastId = idArray[0];
		        	yeastId = yeastId.substring(4);
		        	char firstChar = yeastId.charAt(0);
		        	yeastId = yeastId.substring(3);
		        	yeastId = firstChar + yeastId;
		        	if(yeastIds.contains(yeastId)){
		        		String entrezGeneId = columns[1];
		        		yeastIdToEntrezGeneId.put(yeastId, entrezGeneId);
		        	}
		        }
			}
        }

		FileWriter writer = new FileWriter(outfile);
		for (String yeastId : yeastIdToEntrezGeneId.keySet()) {
			String entrezId = yeastIdToEntrezGeneId.get(yeastId);
	        writer.write(yeastId+"\t"+entrezId+"\n");
        }
		writer.close();
	}

	/**
	 * @throws IOException */
	public static void generateFlyToEntrezGeneIdMapping(String flyIdFile, String entrezGeneInfoFile, String outfile) throws IOException{

		Map<String, String> flyIdToEntrezGeneId = new HashMap<String, String>();

		Set<String> flyIds = FileHelper.readFileIntoSet(flyIdFile, false);

		String[] geneInfoLines = FileHelper.readFromFile(new File(entrezGeneInfoFile));
		for (String string : geneInfoLines) {
			if(!string.startsWith("#")){
				String[] columns = string.split("\t");
		        if(columns[5].startsWith("FLYBASE")){
		        	String id = columns[5].split(":")[1];
		        	if(flyIds.contains(id)){
		        		//System.out.println(string);
		        		String entrezGeneId = columns[1];
		        		flyIdToEntrezGeneId.put(id, entrezGeneId);
		        	}
		        }
			}
        }

		FileWriter writer = new FileWriter(outfile);
		for (String flyId : flyIdToEntrezGeneId.keySet()) {
			String entrezId = flyIdToEntrezGeneId.get(flyId);
	        writer.write(flyId+"\t"+entrezId+"\n");
        }
		writer.close();
	}


	/***/
	@SuppressWarnings("unchecked")
    public static void enrichYeastSynonymList(String synonymListFile, String yeastToEntrezGeneFile, String yeastToUniprotObjectFile, String yeastEntrezGeneInfoFile, String uniprotNamesFile, String outfile) throws IOException{

		Map<String, Set<String>> yeastIdToSynonyms = new HashMap<String, Set<String>>();

		System.out.println("reading info files...");

		Map<String, Integer> yeastIdToEntrezGeneId = getYeastToEntrezGeneMap(yeastToEntrezGeneFile);
		Map<String, String[]> yeastToUniprotIds = (Map<String, String[]>) FileHelper.readObjectFromFile(new File(yeastToUniprotObjectFile));
		//showContent(yeastToUniprotObjectFile);

		Map<Integer, Set<String>> entrezGeneIdToSynonyms = getEntrezGeneSynonyms(yeastEntrezGeneInfoFile, null);
		Map<String, Set<String>> uniprotIdToSynonyms = getUniProtSynonyms(uniprotNamesFile);


		System.out.println("starting...");

		BufferedReader reader = new BufferedReader(new FileReader(synonymListFile));
		String line;
		while((line=reader.readLine())!=null){
			String id = line.split("\t")[0];
			Set<String> synonyms = yeastIdToSynonyms.get(id);
			if(synonyms==null){
				synonyms = new HashSet<String>();
				yeastIdToSynonyms.put(id, synonyms);
			}

			String[] yeastSynonyms = line.split("\t");
			for(int i=1;i<yeastSynonyms.length;i++){
				synonyms.add(yeastSynonyms[i]);
			}

			Integer entrezGeneId = yeastIdToEntrezGeneId.get(id);
			if(entrezGeneId!=null){
				Set<String> entrezGeneSynonyms = entrezGeneIdToSynonyms.get(entrezGeneId);
				if(entrezGeneSynonyms!=null){
					for (String string : entrezGeneSynonyms) {
	                    synonyms.add(string);
                    }
				}
			}

			String[] uniprotIds = yeastToUniprotIds.get(id);
			if(uniprotIds!=null){
				for (String uniprotId : uniprotIds) {
					//System.out.println("checking with uniprotid="+uniprotId);
					Set<String> uniprotSynonyms = uniprotIdToSynonyms.get(uniprotId);
					if(uniprotSynonyms!=null){
						for (String string : uniprotSynonyms) {
							synonyms.add(string);
							System.out.println("Adding synonym from Uniprot: "+string);
                        }
					}
                }
			}


		}
		reader.close();

		FileWriter writer = new FileWriter(outfile);
		for (String yeastId : yeastIdToSynonyms.keySet()) {
			Set<String> synonyms = yeastIdToSynonyms.get(yeastId);
			for (String string : synonyms) {
	            writer.write(yeastId+"\t"+string+"\n");
            }
        }
		writer.close();
	}


	/***/
	@SuppressWarnings("unchecked")
    public static void enrichFlySynonymList(String synonymListFile, String flyToEntrezGeneFile, String flyToUniprotObjectFile, String entrezGeneInfoFile, String uniprotNamesFile, String outfile) throws IOException{

		Map<String, Set<String>> flyIdToSynonyms = new HashMap<String, Set<String>>();

		System.out.println("reading info files...");

		Map<String, Integer> flyIdToEntrezGeneId = getFlyToEntrezGeneMap(flyToEntrezGeneFile);
		Map<String, String[]> flyToUniprotIds = (Map<String, String[]>) FileHelper.readObjectFromFile(new File(flyToUniprotObjectFile));

		Map<Integer, Set<String>> entrezGeneIdToSynonyms = getEntrezGeneSynonyms(entrezGeneInfoFile, null);
		Map<String, Set<String>> uniprotIdToSynonyms = getUniProtSynonyms(uniprotNamesFile);


		System.out.println("starting...");

		BufferedReader reader = new BufferedReader(new FileReader(synonymListFile));
		String line;
		while((line=reader.readLine())!=null){
			String id = line.split("\t")[0];
			Set<String> synonyms = flyIdToSynonyms.get(id);
			if(synonyms==null){
				synonyms = new HashSet<String>();
				flyIdToSynonyms.put(id, synonyms);
			}

			String[] flySynonyms = line.split("\t");
			for(int i=1;i<flySynonyms.length;i++){
				synonyms.add(flySynonyms[i]);
			}

			Integer entrezGeneId = flyIdToEntrezGeneId.get(id);
			if(entrezGeneId!=null){
				Set<String> entrezGeneSynonyms = entrezGeneIdToSynonyms.get(entrezGeneId);
				if(entrezGeneSynonyms!=null){
					for (String string : entrezGeneSynonyms) {
	                    synonyms.add(string);
                    }
				}
			}

			String[] uniprotIds = flyToUniprotIds.get(id);
			if(uniprotIds!=null){
				for (String uniprotId : uniprotIds) {
					//System.out.println("checking with uniprotid="+uniprotId);
					Set<String> uniprotSynonyms = uniprotIdToSynonyms.get(uniprotId);
					if(uniprotSynonyms!=null){
						for (String string : uniprotSynonyms) {
							synonyms.add(string);
							System.out.println("Adding synonym from Uniprot: "+string);
                        }
					}
                }
			}


		}
		reader.close();

		FileWriter writer = new FileWriter(outfile);
		for (String flyId : flyIdToSynonyms.keySet()) {
			Set<String> synonyms = flyIdToSynonyms.get(flyId);
			for (String string : synonyms) {
	            writer.write(flyId+"\t"+string+"\n");
            }
        }
		writer.close();
	}



	/***/
	public static void enrichMouseSynonymList(String synonymListFile, String mouseToEntrezGeneAndUniProtFile, String entrezGeneMouseFile, String uniprotNamesFile, String outfile) throws IOException{

		Map<String, Set<String>> mouseIdToSynonyms = new HashMap<String, Set<String>>();

		Map<String, Integer> mouseIdToEntrezGeneId = getMouseToEntrezGeneMap(mouseToEntrezGeneAndUniProtFile);
		Map<String, Set<String>> mouseIdToUniProtIds = getMouseToUniProtMap(mouseToEntrezGeneAndUniProtFile);
		Map<Integer, Set<String>> entrezGeneIdToSynonyms = getEntrezGeneSynonyms(entrezGeneMouseFile, null);
		Map<String, Set<String>> uniprotIdToSynonyms = getUniProtSynonyms(uniprotNamesFile);


		BufferedReader reader = new BufferedReader(new FileReader(synonymListFile));
		String line;
		while((line=reader.readLine())!=null){
			String id = line.split("\t")[0];
			Set<String> synonyms = mouseIdToSynonyms.get(id);
			if(synonyms==null){
				synonyms = new HashSet<String>();
				mouseIdToSynonyms.put(id, synonyms);
			}

			String[] mgdSynonyms = line.split("\t");
			for(int i=1;i<mgdSynonyms.length;i++){
				synonyms.add(mgdSynonyms[i]);
			}

			Integer entrezGeneId = mouseIdToEntrezGeneId.get(id);
			if(entrezGeneId!=null){
				Set<String> entrezGeneSynonyms = entrezGeneIdToSynonyms.get(entrezGeneId);
				if(entrezGeneSynonyms!=null){
					for (String string : entrezGeneSynonyms) {
	                    synonyms.add(string);
                    }
				}
			}

			Set<String> uniprotIds = mouseIdToUniProtIds.get(id);
			if(uniprotIds!=null){
				for (String uniprotId : uniprotIds) {
					Set<String> uniprotSynonyms = uniprotIdToSynonyms.get(uniprotId);
					if(uniprotSynonyms!=null){
						for (String string : uniprotSynonyms) {
							synonyms.add(string);
                        }
					}
                }
			}


		}
		reader.close();

		FileWriter writer = new FileWriter(outfile);
		for (String mouseId : mouseIdToSynonyms.keySet()) {
			Set<String> synonyms = mouseIdToSynonyms.get(mouseId);
			for (String string : synonyms) {
	            writer.write(mouseId+"\t"+string+"\n");
            }
        }
		writer.close();
	}


	/**
	 * Returns a mapping from EntrezGene Id to synonyms.
	 * */
	public static Map<Integer, Set<String>> getEntrezGeneSynonyms(String entrezGeneInfoFile, Set<Integer> taxIds) throws IOException{
		Map<Integer, Set<String>> entrezGeneIdToSynonyms = new HashMap<Integer, Set<String>>();

		BufferedReader reader = new BufferedReader(new FileReader(entrezGeneInfoFile));
		String line;
		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				continue;
			}

			String[] columns = line.split("\t");

			Integer taxId = Integer.parseInt(columns[0]);
			if(taxIds!=null && !taxIds.contains(taxId)){
				continue;
			}

			Integer entrezGeneId = Integer.parseInt(columns[1]);
			Set<String> synonyms = entrezGeneIdToSynonyms.get(entrezGeneId);
			if(synonyms==null){
				synonyms = new HashSet<String>();
				entrezGeneIdToSynonyms.put(entrezGeneId, synonyms);
			}

			String symbol = columns[2];
			String[] symbolArray = symbol.split("[\\|;]");
			for (String string : symbolArray) {
				synonyms.add(string.trim());
            }

			String[] synonymArray = columns[4].split("[\\|;]");
			for (String string : synonymArray) {
				synonyms.add(string.trim());
            }

			String[] descriptionArray = columns[8].split("[\\|;]");
			for (String string : descriptionArray) {
				synonyms.add(string.trim());
            }

			String symbolFromNomenclature = columns[10];
			String[] symbolFromNomenclatureArray = symbolFromNomenclature.split("[\\|;]");
			for (String string : symbolFromNomenclatureArray) {
				synonyms.add(string.trim());
            }

			String fullNameFromNomenclature = columns[11];
			String[] fullNameFromNomenclatureArray = fullNameFromNomenclature.split("[\\|;]");
			for (String string : fullNameFromNomenclatureArray) {
				synonyms.add(string.trim());
            }

			String otherDesignation = columns[13];
			String[] otherDesignationArray = otherDesignation.split("[\\|;]");
			for (String string : otherDesignationArray) {
				synonyms.add(string.trim());
            }

			synonyms.remove("-");

		}
		reader.close();

		return entrezGeneIdToSynonyms;
	}

	/**
	 * Returns a mapping from EntrezGene Id to synonyms.
	 * */
	public static Map<Integer, String> getEntrezGeneSymbols(String entrezGeneInfoFile, Set<Integer> geneIds) throws IOException{
		Map<Integer, String> entrezGeneIdToSymbol = new HashMap<Integer, String>();

		BufferedReader reader = new BufferedReader(new FileReader(entrezGeneInfoFile));
		String line;
		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				continue;
			}

			String[] columns = line.split("\t");

			Integer entrezGeneId = Integer.parseInt(columns[1]);
			if(!geneIds.contains(entrezGeneId))
				continue;

			String symbol = columns[2];
			entrezGeneIdToSymbol.put(entrezGeneId, symbol);

		}
		reader.close();

		return entrezGeneIdToSymbol;
	}


	/**
	 * Returns a mapping from EntrezGene Id to synonyms.
	 * */
	public static Set<String> getEntrezGeneSynonymsForGeneId(String entrezGeneInfoFile, String geneId) throws IOException{
		Set<String> synonyms = new HashSet<String>();

		BufferedReader reader = new BufferedReader(new FileReader(entrezGeneInfoFile));
		String line;
		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				continue;
			}

			String[] columns = line.split("\t");

			String entrezGeneId = columns[1];

			if(entrezGeneId.equals(geneId)){

				String symbol = columns[2];
				String[] symbolArray = symbol.split("[\\|;]");
				for (String string : symbolArray) {
					synonyms.add(string.trim());
	            }

				String[] synonymArray = columns[4].split("[\\|;]");
				for (String string : synonymArray) {
					synonyms.add(string.trim());
	            }

				String symbolFromNomenclature = columns[10];
				String[] symbolFromNomenclatureArray = symbolFromNomenclature.split("[\\|;]");
				for (String string : symbolFromNomenclatureArray) {
					synonyms.add(string.trim());
	            }

				String fullNameFromNomenclature = columns[11];
				String[] fullNameFromNomenclatureArray = fullNameFromNomenclature.split("[\\|;]");
				for (String string : fullNameFromNomenclatureArray) {
					synonyms.add(string.trim());
	            }

				String otherDesignation = columns[13];
				String[] otherDesignationArray = otherDesignation.split("[\\|;]");
				for (String string : otherDesignationArray) {
					synonyms.add(string.trim());
	            }

				synonyms.remove("-");
				break;
			}

		}
		reader.close();

		return synonyms;
	}


	/**
	 * Returns a mapping from EntrezGene Id to Taxon Id.
	 * */
	public static Map<String, String> getEntrezGeneToOrigin(String entrezGeneFile) {
		Map<String,String> entrezGeneIdToSynonyms = new HashMap<String, String>();

		try {
	        BufferedReader reader = new BufferedReader(new FileReader(entrezGeneFile));
	        String line;
	        while((line=reader.readLine())!=null){
	        	if(line.startsWith("#")){
	        		continue;
	        	}
	        	String[] columns = line.split("\t");
	        	String taxId = columns[0];
	        	String entrezGeneId = columns[1];
	        	entrezGeneIdToSynonyms.put(entrezGeneId, taxId);
	        }
	        reader.close();
        }
        catch (FileNotFoundException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
        catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }

		return entrezGeneIdToSynonyms;
	}

	/**
	 * @throws IOException
	 *
	 * */
	public static Map<Integer, String> getEntrezGeneToDescription(Set<Integer> geneIds, String entrezGeneFile) throws IOException {
		Map<Integer,String> entrezGeneIdToDescription = new HashMap<Integer, String>();

	    BufferedReader reader = new BufferedReader(new FileReader(entrezGeneFile));
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			}
			String[] columns = line.split("\t");
			Integer entrezGeneId = Integer.parseInt(columns[1]);
			if (geneIds == null || geneIds.contains(entrezGeneId)) {
				String description = columns[8];
				if (!description.equals("-")) {
					entrezGeneIdToDescription.put(entrezGeneId, description);
				}
			}
		}
		reader.close();


		return entrezGeneIdToDescription;
	}


	/**
	 * @throws IOException
	 *
	 * */
	public static Map<Integer, String> getEntrezGeneToDescriptionByTaxIds(Set<Integer> taxIds, String entrezGeneFile) throws IOException {
		Map<Integer,String> entrezGeneIdToDescription = new HashMap<Integer, String>();

	    BufferedReader reader = new BufferedReader(new FileReader(entrezGeneFile));
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			}
			String[] columns = line.split("\t");
			Integer taxId = Integer.parseInt(columns[0]);
			if (taxIds.contains(taxId)) {
				Integer entrezGeneId = Integer.parseInt(columns[1]);
				String description = columns[8];
				if (!description.equals("-")) {
					entrezGeneIdToDescription.put(entrezGeneId, description);
				}
			}
		}
		reader.close();


		return entrezGeneIdToDescription;
	}


	/**
	 * @throws IOException
	 *
	 * */
	public static Set<Integer> getEntrezGeneIds(Set<Integer> taxIds, String entrezGeneFile) throws IOException {
		Set<Integer> entrezGeneIds = new HashSet<Integer>();

	    BufferedReader reader = new BufferedReader(new FileReader(entrezGeneFile));
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			}
			String[] columns = line.split("\t");
			Integer taxId = Integer.parseInt(columns[0]);
			if (taxIds.contains(taxId)) {
				Integer entrezGeneId = Integer.parseInt(columns[1]);
				entrezGeneIds.add(entrezGeneId);
			}
		}
		reader.close();

		return entrezGeneIds;
	}


	/**
	 * @throws IOException
	 *
	 * */
	public static List<Integer> getEntrezGeneIdsAsList(Set<Integer> taxIds, String entrezGeneFile) throws IOException {
		List<Integer> entrezGeneIds = new LinkedList<Integer>();

	    BufferedReader reader = new BufferedReader(new FileReader(entrezGeneFile));
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			}
			String[] columns = line.split("\t");
			Integer taxId = Integer.parseInt(columns[0]);
			if (taxIds.contains(taxId)) {
				Integer entrezGeneId = Integer.parseInt(columns[1]);
				entrezGeneIds.add(entrezGeneId);
			}
		}
		reader.close();

		return entrezGeneIds;
	}


	/**
	 * @throws IOException
	 *
	 * */
	public static Map<Integer,Map<Integer, String>> getEntrezGeneToGeneRifsByTaxIds(Set<Integer> taxIds, String generifsFile) throws IOException {
		Map<Integer,Map<Integer, String>> gene2generifs = new HashMap<Integer,Map<Integer, String>>();

	    BufferedReader reader = new BufferedReader(new FileReader(generifsFile));
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			}
			String[] columns = line.split("\t");
			Integer taxId = Integer.parseInt(columns[0]);
			if (taxIds.contains(taxId) && columns.length==5) {
				Integer geneId = Integer.parseInt(columns[1]);
				String[] pmIds = columns[2].split("\\,");
				String text = columns[4];
				Map<Integer, String> geneRifsMapForGene = gene2generifs.get(geneId);
				if(geneRifsMapForGene==null){
					geneRifsMapForGene = new HashMap<Integer, String>();
					gene2generifs.put(geneId, geneRifsMapForGene);
				}
				for (String pmId : pmIds) {
					geneRifsMapForGene.put(Integer.parseInt(pmId), text);
                }
			}
		}
		reader.close();


		return gene2generifs;
	}


	/**
	 * @throws IOException
	 *
	 * */
	public static Map<Integer, Integer> getEntrezGeneToTaxonomy(Set<Integer> geneIds, String entrezGeneFile) throws IOException {
		Map<Integer,Integer> gene2taxonomy = new HashMap<Integer, Integer>();

	    BufferedReader reader = new BufferedReader(new FileReader(entrezGeneFile));
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			}
			String[] columns = line.split("\t");
			Integer entrezGeneId = Integer.parseInt(columns[1]);
			if (geneIds == null || geneIds.contains(entrezGeneId)) {
				Integer taxonomyId = Integer.parseInt(columns[0]);
				gene2taxonomy.put(entrezGeneId, taxonomyId);
			}
		}
		reader.close();


		return gene2taxonomy;
	}

	/**
	 * @throws IOException
	 *
	 * */
	public static Map<Integer, Integer> getEntrezGeneToTaxonomyForTaxIds(Set<Integer> taxIds, String entrezGeneFile) throws IOException {
		Map<Integer,Integer> gene2taxonomy = new HashMap<Integer, Integer>();

	    BufferedReader reader = new BufferedReader(new FileReader(entrezGeneFile));
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			}
			String[] columns = line.split("\t");
			Integer taxId = Integer.parseInt(columns[0]);
			if(taxIds.contains(taxId)){
				Integer entrezGeneId = Integer.parseInt(columns[1]);
				gene2taxonomy.put(entrezGeneId, taxId);
			}
		}
		reader.close();


		return gene2taxonomy;
	}

	/**
	 * Returns a mapping from EntrezGene Id to Taxon Id.
	 */
	public static Map<Integer, Integer> getEntrezGeneToOMIMId(String geneinfoFile) {
		Map<Integer,Integer> entrezGeneIdToOMIMId = new HashMap<Integer, Integer>();

		try {
	        BufferedReader reader = new BufferedReader(new FileReader(geneinfoFile));
	        String line;
	        while((line=reader.readLine())!=null){
	        	if(line.startsWith("#")){
	        		continue;
	        	}
	        	String[] columns = line.split("\t");
	        	Integer entrezGeneId = Integer.parseInt(columns[1]);
	        	String xrefs = columns[5];
	        	if(!xrefs.equals("-")){
	        		for (String string : xrefs.split("\\|")) {
	        			if(string.startsWith("MIM:")){
	        				entrezGeneIdToOMIMId.put( entrezGeneId,
	        								Integer.parseInt(string.substring(string.indexOf(':')+1)) );
	        			}
                    }
	        	}
	        }
	        reader.close();
        }
        catch (FileNotFoundException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
        catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }

		return entrezGeneIdToOMIMId;
	}

	/**
	 * Returns a mapping from uniprot Id to synonyms.
	 * */
	public static Map<String, Set<String>> getUniProtSynonyms(String uniprotFile) throws IOException{
		Map<String, Set<String>> uniprotIdToSynonyms = new HashMap<String, Set<String>>();

		BufferedReader reader = new BufferedReader(new FileReader(uniprotFile));
		String line;
		while((line=reader.readLine())!=null){
			String[] columns = line.split("\t");
			String uniprotId = columns[0];
			Set<String> synonyms = uniprotIdToSynonyms.get(uniprotId);
			if(synonyms==null){
				synonyms = new HashSet<String>();
				uniprotIdToSynonyms.put(uniprotId, synonyms);
			}

			synonyms.add(columns[1]);

		}
		reader.close();

		return uniprotIdToSynonyms;
	}



	/**
	 * Returns a mapping from MGI to EntrezGene id.
	 * */
	public static Map<String, Integer> getMouseToEntrezGeneMap(String mouseToEntrezGeneAndUniProtFile) throws IOException{
		Map<String, Integer> mouseToEntrezGeneMap = new HashMap<String, Integer>();

		BufferedReader reader = new BufferedReader(new FileReader(mouseToEntrezGeneAndUniProtFile));
		String line;
		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				continue;
			}

			String[] columns = line.split("\t");
			if(columns.length>2){
				String mouseId = columns[0];
				Integer entrezGeneId = Integer.parseInt(columns[2]);
				mouseToEntrezGeneMap.put(mouseId, entrezGeneId);
			}
		}
		reader.close();

		return mouseToEntrezGeneMap;
	}

	/**
	 * Returns a mapping from SGD ID to EntrezGene id.
	 * */
	public static Map<String, String> getEntrezGeneToYeastMap(String yeastToEntrezGeneFile) throws IOException{
		Map<String, String> entrezGeneToYeastMap = new HashMap<String, String>();

		BufferedReader reader = new BufferedReader(new FileReader(yeastToEntrezGeneFile));
		String line;
		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				continue;
			}

			String[] columns = line.split("\t");
			if(columns.length>1){
				String yeastId = columns[0];
				String entrezGeneId = columns[1];
				entrezGeneToYeastMap.put(entrezGeneId, yeastId);
			}
		}
		reader.close();

		return entrezGeneToYeastMap;
	}

	/**
	 * Returns a mapping from FLYBASE ID to EntrezGene id.
	 * */
	public static Map<String, String> getEntrezGeneToFlyMap(String flyToEntrezGeneFile) throws IOException{
		Map<String, String> entrezGeneToFlyMap = new HashMap<String, String>();

		BufferedReader reader = new BufferedReader(new FileReader(flyToEntrezGeneFile));
		String line;
		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				continue;
			}

			String[] columns = line.split("\t");
			if(columns.length>1){
				String flyId = columns[0];
				String entrezGeneId = columns[1];
				entrezGeneToFlyMap.put(entrezGeneId, flyId);
			}
		}
		reader.close();

		return entrezGeneToFlyMap;
	}

	/**
	 * Returns a mapping from MGI to EntrezGene id.
	 * */
	public static Map<String, Integer> getYeastToEntrezGeneMap(String yeastToEntrezGeneFile) throws IOException{
		Map<String, Integer> yeasToEntrezGeneMap = new HashMap<String, Integer>();

		BufferedReader reader = new BufferedReader(new FileReader(yeastToEntrezGeneFile));
		String line;
		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				continue;
			}

			String[] columns = line.split("\t");
			if(columns.length>1){
				String yeastId = columns[0];
				Integer entrezGeneId = Integer.parseInt(columns[1]);
				yeasToEntrezGeneMap.put(yeastId, entrezGeneId);
			}
		}
		reader.close();

		return yeasToEntrezGeneMap;
	}

	/**
	 * Returns a mapping from MGI to EntrezGene id.
	 * */
	public static Map<String, Integer> getFlyToEntrezGeneMap(String flyToEntrezGeneFile) throws IOException{
		Map<String, Integer> flyToEntrezGeneMap = new HashMap<String, Integer>();

		BufferedReader reader = new BufferedReader(new FileReader(flyToEntrezGeneFile));
		String line;
		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				continue;
			}

			String[] columns = line.split("\t");
			if(columns.length>1){
				String flyId = columns[0];
				Integer entrezGeneId = Integer.parseInt(columns[1]);
				flyToEntrezGeneMap.put(flyId, entrezGeneId);
			}
		}
		reader.close();

		return flyToEntrezGeneMap;
	}

	/**
	 * Returns a mapping from MGI to EntrezGene id.
	 * */
	public static Map<String, String> getEntrezGeneToMouseMap(String mouseToEntrezGeneAndUniProtFile) throws IOException{
		Map<String, String> entrezGeneToMouseMap = new HashMap<String, String>();

		BufferedReader reader = new BufferedReader(new FileReader(mouseToEntrezGeneAndUniProtFile));
		String line;
		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				continue;
			}

			String[] columns = line.split("\t");
			if(columns.length>2){
				String mouseId = columns[0];
				String entrezGeneId = columns[2];
				entrezGeneToMouseMap.put(entrezGeneId, mouseId);
			}
		}
		reader.close();

		return entrezGeneToMouseMap;
	}

	/**
	 * Returns a mapping from MGI to a set of uniprot Ids.
	 * */
	public static Map<String, Set<String>> getMouseToUniProtMap(String mouseToEntrezGeneAndUniProtFile) throws IOException{
		Map<String, Set<String>> mouseToUniProtMap = new HashMap<String, Set<String>>();

		BufferedReader reader = new BufferedReader(new FileReader(mouseToEntrezGeneAndUniProtFile));
		String line;
		while((line=reader.readLine())!=null){
			String[] columns = line.split("\t");
			if(columns.length>3){
				String mouseId = columns[0];
				String uniprotId = columns[3];
				Set<String> uniprotIds = mouseToUniProtMap.get(mouseId);
				if(uniprotIds==null){
					uniprotIds = new HashSet<String>();
					mouseToUniProtMap.put(mouseId, uniprotIds);
				}
				uniprotIds.add(uniprotId);
			}
		}
		reader.close();

		return mouseToUniProtMap;
	}


	/**
	 * Extracts EntrezGene entries related to mouse. Takes the gene_info file availabe from the EntrezGene FTP server.
	 *
	 * */
	public static void generateEntrezGeneMouseFile(String geneInfoFile, String outfile) throws IOException{
		FileWriter writer = new FileWriter(outfile);
		BufferedReader reader = new BufferedReader(new FileReader(geneInfoFile));
		String line;
		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				writer.write(line+"\n");
				continue;
			}

			String[] columns = line.split("\t");
			String taxId = columns[0];
			if(musMusculusTaxIds.contains(taxId)){
				writer.write(line+"\n");
			}
		}
		writer.close();
		reader.close();
	}


	/**
	 * @throws IOException */
	public static void generateGeneRifObjectForMouse(String mouseToEntrezGeneAndUniProtFile, String generifsFile, String outfile) throws IOException{

		Map<String, String> entrezGeneIdToMouseId = getEntrezGeneToMouseMap(mouseToEntrezGeneAndUniProtFile);

		Map<String, Set<String>> mouseIdsToGeneRifs = new HashMap<String, Set<String>>();

		BufferedReader reader = new BufferedReader(new FileReader(generifsFile));
		String line;
		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				continue;
			}
			String[] columns = line.split("\t");
			String taxId = columns[0];

			if(musMusculusTaxIds.contains(taxId)){
				String geneId = columns[1];
				String text = columns[4];

				String mouseId = entrezGeneIdToMouseId.get(geneId);
				if(mouseId!=null){
					Set<String> geneRifs = mouseIdsToGeneRifs.get(mouseId);
					if(geneRifs==null){
						geneRifs = new HashSet<String>();
						mouseIdsToGeneRifs.put(mouseId, geneRifs);
					}

					geneRifs.add(text);
				}

			}

		}
		reader.close();


		Map<String, String[]> mouseIdsToGeneRifArray = new HashMap<String, String[]>();
		for (String mouseId : mouseIdsToGeneRifs.keySet()) {
			Set<String> geneRifs = mouseIdsToGeneRifs.get(mouseId);
			String[] array = ArrayHelper.set2StringArray(geneRifs);
			mouseIdsToGeneRifArray.put(mouseId, array);
        }

		ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(new File(outfile)));
		writer.writeObject(mouseIdsToGeneRifArray);
		writer.close();
	}


	public static void generateUniprotIdObjectFileForMouse(String mouseToEntrezGeneIdAndUniProtIdsFile, String outfile) throws IOException{
		Map<String, Set<String>> mouseIdToUniProtIds = new HashMap<String, Set<String>>();

		BufferedReader reader = new BufferedReader(new FileReader(mouseToEntrezGeneIdAndUniProtIdsFile));
		String line;
		while((line=reader.readLine())!=null){
			String[] columns = line.split("\t");
			if(columns.length>3){
				String mouseId = columns[1];
				String uniprotId = columns[3];
				Set<String> uniprotIds = mouseIdToUniProtIds.get(mouseId);
				if(uniprotIds==null){
					uniprotIds = new HashSet<String>();
					mouseIdToUniProtIds.put(mouseId, uniprotIds);
				}
				uniprotIds.add(uniprotId);
			}
		}
		reader.close();


		Map<String, String[]> mouseIdToUniProtIdArray = new HashMap<String, String[]>();
		for (String mouseId : mouseIdToUniProtIds.keySet()) {
			Set<String> uniprotIds = mouseIdToUniProtIds.get(mouseId);
			String[] array = ArrayHelper.set2StringArray(uniprotIds);
			mouseIdToUniProtIdArray.put(mouseId, array);
        }

		ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(new File(outfile)));
		writer.writeObject(mouseIdToUniProtIdArray);
		writer.close();


	}


	public static void generateUniprotIdObjectFileForFly(String uniprotFile, String flyIdFile, String outfile) throws IOException{

		Map<String, String[]> flyIdToUniprotIds = new HashMap<String, String[]>();

		Set<String> flyIds = FileHelper.readFileIntoSet(flyIdFile, false);

		BufferedReader reader = new BufferedReader(new FileReader(uniprotFile));
		String line;

		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				continue;
			}

			if(line.startsWith("ID")){

				// new record
				boolean isRelevantRecord = false;
				Set<String> uniprotIds = new HashSet<String>();
				String flyId = null;

				List<String> recordLines = new LinkedList<String>();

				while(!line.startsWith("//")){
					recordLines.add(line);
					if(line.startsWith("AC")){
						line = line.substring(2);
						String[] lineParts = line.split(";");
						for (String string : lineParts) {
	                        uniprotIds.add(string.trim());
                        }
					}

					if(line.startsWith("DR")){
						line = line.substring(2);
						String[] lineParts = line.split(";");
						for (String string : lineParts) {
	                        if(flyIds.contains(string.trim())){
	                        	isRelevantRecord = true;
	                        	flyId = string.trim();
	                        }
                        }
					}
					line = reader.readLine();
				}
				recordLines.add(line);

				if(isRelevantRecord){
					System.out.println("Found record for "+flyId);
					String[] idsHaving = flyIdToUniprotIds.get(flyId);
					if(idsHaving==null){
						idsHaving = new String[0];
					}
					idsHaving = ArrayHelper.joinTwoStringArraysIntoOne(idsHaving, ArrayHelper.set2StringArray(uniprotIds));
					flyIdToUniprotIds.put(flyId, idsHaving);
				}
			}

		}
		reader.close();

		FileHelper.writeObjectToFile(flyIdToUniprotIds, new File("entrezGeneObjects/Fly/uIDs.object"));
	}


	public static void generateUniprotIdObjectFileForYeast(String uniprotFile, String yeastIdFile, String outfile) throws IOException{

		Map<String, String[]> yeastIdToUniprotIds = new HashMap<String, String[]>();

		Set<String> yeastIds_short = FileHelper.readFileIntoSet(yeastIdFile, false);
		Set<String> yeastIds = new HashSet<String>();
		for (String string : yeastIds_short) {
			yeastIds.add(prolongYeastId(string));
        }

		BufferedReader reader = new BufferedReader(new FileReader(uniprotFile));
		String line;

		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				continue;
			}

			if(line.startsWith("ID")){

				// new record
				boolean isRelevantRecord = false;
				Set<String> uniprotIds = new HashSet<String>();
				String sgdId = null;

				List<String> recordLines = new LinkedList<String>();

				while(!line.startsWith("//")){
					recordLines.add(line);
					if(line.startsWith("AC")){
						line = line.substring(2);
						String[] lineParts = line.split(";");
						for (String string : lineParts) {
	                        uniprotIds.add(string.trim());
                        }
					}

					if(line.startsWith("DR")){
						line = line.substring(2);
						String[] lineParts = line.split(";");
						for (String string : lineParts) {
	                        if(yeastIds.contains(string.trim())){
	                        	isRelevantRecord = true;
	                        	sgdId = string.trim();
	                        }
                        }
					}
					line = reader.readLine();
				}
				recordLines.add(line);

				if(isRelevantRecord){
					System.out.println("Found record for "+sgdId);
					String[] idsHaving = yeastIdToUniprotIds.get(sgdId);
					if(idsHaving==null){
						idsHaving = new String[0];
					}
					idsHaving = ArrayHelper.joinTwoStringArraysIntoOne(idsHaving, ArrayHelper.set2StringArray(uniprotIds));
					yeastIdToUniprotIds.put(compressYeastId(sgdId), idsHaving);
				}
			}

		}
		reader.close();

		FileHelper.writeObjectToFile(yeastIdToUniprotIds, new File("entrezGeneObjects/Yeast/uIDs.object"));
	}


	/***/
	public static String prolongYeastId(String shortId){
		char firstChar = shortId.charAt(0);
		String longId = firstChar + "00" + shortId.substring(1);
		return longId;
	}

	/***/
	public static String compressYeastId(String longId){
		char firstChar = longId.charAt(0);
		String shortId = firstChar + longId.substring(3);
		return shortId;
	}


	/**
	 * @throws IOException */
	public static void generatePMIDObjectForMouse(String mouseToEntrezGeneAndUniProtFile, String generifsFile, String gene2pubmedFile, String outfile) throws IOException{

		Map<String, String> entrezGeneIdToMouseId = getEntrezGeneToMouseMap(mouseToEntrezGeneAndUniProtFile);

		Map<String, Set<String>> mouseIdToPMIDs = new HashMap<String, Set<String>>();

		BufferedReader reader = new BufferedReader(new FileReader(generifsFile));
		String line;
		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				continue;
			}
			String[] columns = line.split("\t");
			String taxId = columns[0];

			if(musMusculusTaxIds.contains(taxId)){
				String geneId = columns[1];
				String pmId = columns[2];

				String mouseId = entrezGeneIdToMouseId.get(geneId);
				if(mouseId!=null){
					Set<String> pmIds = mouseIdToPMIDs.get(mouseId);
					if(pmIds==null){
						pmIds = new HashSet<String>();
						mouseIdToPMIDs.put(mouseId, pmIds);
					}
					pmIds.add(pmId);
				}
			}
		}
		reader.close();

		reader = new BufferedReader(new FileReader(gene2pubmedFile));
		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				continue;
			}
			String[] columns = line.split("\t");
			String taxId = columns[0];

			if(musMusculusTaxIds.contains(taxId)){
				String geneId = columns[1];
				String pmId = columns[2];
				String mouseId = entrezGeneIdToMouseId.get(geneId);
				if(mouseId!=null)
				{
					Set<String> pmIds = mouseIdToPMIDs.get(mouseId);
					if(pmIds==null){
						pmIds = new HashSet<String>();
						mouseIdToPMIDs.put(mouseId, pmIds);
					}
					pmIds.add(pmId);
				}
			}
		}
		reader.close();

		Map<String, String[]> mouseIdToPMIDArray = new HashMap<String, String[]>();
		for (String mouseId : mouseIdToPMIDs.keySet()) {
			Set<String> pmIds = mouseIdToPMIDs.get(mouseId);
			String[] array = ArrayHelper.set2StringArray(pmIds);
			mouseIdToPMIDArray.put(mouseId, array);
        }

		ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(new File(outfile)));
		writer.writeObject(mouseIdToPMIDArray);
		writer.close();
	}

	/**
	 * @throws IOException */
	public static void generatePMIDObjectForHuman(String humanEntrezGeneIdsFile, String generifsFile, String gene2pubmedFile, String outfile) throws IOException{

		Set<String> humanEntrezGeneIds = FileHelper.readFileIntoSet(humanEntrezGeneIdsFile, false);

		Map<String, Set<String>> humanEntrezGeneIdToPMIDs = new HashMap<String, Set<String>>();

		BufferedReader reader = new BufferedReader(new FileReader(generifsFile));
		String line;
		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				continue;
			}
			String[] columns = line.split("\t");
			String geneId = columns[1];
			String pmId = columns[2];
			if(humanEntrezGeneIds.contains(geneId)){
				Set<String> pmIds = humanEntrezGeneIdToPMIDs.get(geneId);
				if(pmIds==null){
					pmIds = new HashSet<String>();
					humanEntrezGeneIdToPMIDs.put(geneId, pmIds);
				}
				pmIds.add(pmId);
			}
		}
		reader.close();

		reader = new BufferedReader(new FileReader(gene2pubmedFile));
		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				continue;
			}
			String[] columns = line.split("\t");
			String geneId = columns[1];
			String pmId = columns[2];
			if(humanEntrezGeneIds.contains(geneId))
			{
				Set<String> pmIds = humanEntrezGeneIdToPMIDs.get(geneId);
				if(pmIds==null){
					pmIds = new HashSet<String>();
					humanEntrezGeneIdToPMIDs.put(geneId, pmIds);
				}
				pmIds.add(pmId);
			}
		}
		reader.close();

		Map<String, String[]> humanEntrezGeneIdToPMIDArray = new HashMap<String, String[]>();
		for (String geneId : humanEntrezGeneIdToPMIDs.keySet()) {
			Set<String> pmIds = humanEntrezGeneIdToPMIDs.get(geneId);
			String[] array = ArrayHelper.set2StringArray(pmIds);
			humanEntrezGeneIdToPMIDArray.put(geneId, array);
        }

		ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(new File(outfile)));
		writer.writeObject(humanEntrezGeneIdToPMIDArray);
		writer.close();
	}

	/**
	 * @throws IOException */
	public static void generatePMIDObjectForYeast(String yeastToEntrezGeneFile, String generifsFile, String gene2pubmedFile, String outfile) throws IOException{

		Map<String, String> entrezGeneIdToYeastId = getEntrezGeneToYeastMap(yeastToEntrezGeneFile);

		Map<String, Set<String>> yeastIdToPMIDs = new HashMap<String, Set<String>>();

		BufferedReader reader = new BufferedReader(new FileReader(generifsFile));
		String line;
		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				continue;
			}
			String[] columns = line.split("\t");
			String taxId = columns[0];

			if(yeastTaxIds.contains(taxId)){
				String geneId = columns[1];
				String pmId = columns[2];

				String yeastId = entrezGeneIdToYeastId.get(geneId);
				if(yeastId!=null){
					Set<String> pmIds = yeastIdToPMIDs.get(yeastId);
					if(pmIds==null){
						pmIds = new HashSet<String>();
						yeastIdToPMIDs.put(yeastId, pmIds);
					}
					pmIds.add(pmId);
				}
			}
		}
		reader.close();

		reader = new BufferedReader(new FileReader(gene2pubmedFile));
		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				continue;
			}
			String[] columns = line.split("\t");
			String taxId = columns[0];

			if(yeastTaxIds.contains(taxId)){
				String geneId = columns[1];
				String pmId = columns[2];
				String yeastId = entrezGeneIdToYeastId.get(geneId);
				if(yeastId!=null)
				{
					Set<String> pmIds = yeastIdToPMIDs.get(yeastId);
					if(pmIds==null){
						pmIds = new HashSet<String>();
						yeastIdToPMIDs.put(yeastId, pmIds);
					}
					pmIds.add(pmId);
				}
			}
		}
		reader.close();

		Map<String, String[]> yeastIdToPMIDArray = new HashMap<String, String[]>();
		for (String yeastId : yeastIdToPMIDs.keySet()) {
			Set<String> pmIds = yeastIdToPMIDs.get(yeastId);
			String[] array = ArrayHelper.set2StringArray(pmIds);
			yeastIdToPMIDArray.put(yeastId, array);
        }

		ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(new File(outfile)));
		writer.writeObject(yeastIdToPMIDArray);
		writer.close();
	}

	/**
	 * @throws IOException */
	public static void generatePMIDObjectForFly(String flyToEntrezGeneFile, String generifsFile, String gene2pubmedFile, String outfile) throws IOException{

		Map<String, String> entrezGeneIdToFlyId = getEntrezGeneToFlyMap(flyToEntrezGeneFile);

		Map<String, Set<String>> flyIdToPMIDs = new HashMap<String, Set<String>>();

		BufferedReader reader = new BufferedReader(new FileReader(generifsFile));
		String line;
		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				continue;
			}
			String[] columns = line.split("\t");
			String taxId = columns[0];

			if(flyTaxIds.contains(taxId)){
				String geneId = columns[1];
				String pmId = columns[2];

				String flyId = entrezGeneIdToFlyId.get(geneId);
				if(flyId!=null){
					Set<String> pmIds = flyIdToPMIDs.get(flyId);
					if(pmIds==null){
						pmIds = new HashSet<String>();
						flyIdToPMIDs.put(flyId, pmIds);
					}
					pmIds.add(pmId);
				}
			}
		}
		reader.close();

		reader = new BufferedReader(new FileReader(gene2pubmedFile));
		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				continue;
			}
			String[] columns = line.split("\t");
			String taxId = columns[0];

			if(flyTaxIds.contains(taxId)){
				String geneId = columns[1];
				String pmId = columns[2];
				String flyId = entrezGeneIdToFlyId.get(geneId);
				if(flyId!=null)
				{
					Set<String> pmIds = flyIdToPMIDs.get(flyId);
					if(pmIds==null){
						pmIds = new HashSet<String>();
						flyIdToPMIDs.put(flyId, pmIds);
					}
					pmIds.add(pmId);
				}
			}
		}
		reader.close();

		Map<String, String[]> flyIdToPMIDArray = new HashMap<String, String[]>();
		for (String yeastId : flyIdToPMIDs.keySet()) {
			Set<String> pmIds = flyIdToPMIDs.get(yeastId);
			String[] array = ArrayHelper.set2StringArray(pmIds);
			flyIdToPMIDArray.put(yeastId, array);
        }

		ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(new File(outfile)));
		writer.writeObject(flyIdToPMIDArray);
		writer.close();
	}


	/**
	 * @throws IOException */
	public static void generateGOCodeObjectForMouse(String geneAssociationFileForMouse, String outfile) throws IOException{

		Map<String, Set<String>> mouseIdToGoCodes = new HashMap<String, Set<String>>();

		BufferedReader reader = new BufferedReader(new FileReader(geneAssociationFileForMouse));
		String line;
		while((line=reader.readLine())!=null){
			if(line.startsWith("!")){
				continue;
			}
			String[] columns = line.split("\t");
//			System.out.println("# columns = "+columns.length);

//			for (String string : columns) {
//	            System.out.println(string);
//            }

			String mgi = columns[1];
			String goCode = columns[4];
			//System.out.println("GOCode="+goCode);

			Set<String> goCodes = mouseIdToGoCodes.get(mgi);
			if(goCodes==null){
				goCodes = new HashSet<String>();
				mouseIdToGoCodes.put(mgi, goCodes);
			}
			goCodes.add(goCode);
		}
		reader.close();

		Map<String, String[]> mouseIdToGOCodeArray = new HashMap<String, String[]>();
		for (String geneId : mouseIdToGoCodes.keySet()) {
			Set<String> goCodes = mouseIdToGoCodes.get(geneId);
			String[] array = ArrayHelper.set2StringArray(goCodes);
			mouseIdToGOCodeArray.put(geneId, array);
        }

		ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(new File(outfile)));
		writer.writeObject(mouseIdToGOCodeArray);
		writer.close();
	}



	/**
	 * Extracts uniprot entries related to mouse.
	 *
	 * */
	public static void generateUniProtMouseFile(String uniprotFile, String outfile) throws IOException{
		FileWriter writer = new FileWriter(outfile);
		BufferedReader reader = new BufferedReader(new FileReader(uniprotFile));
		String line;

		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				writer.write(line+"\n");
				continue;
			}

			if(line.startsWith("ID")){
				// new record
				boolean isMouseRecord = false;
				List<String> recordLines = new LinkedList<String>();

				while(!line.startsWith("//")){
					recordLines.add(line);
					if(line.startsWith("OS")){
						String lowerCaseLine = line.toLowerCase();
						if(lowerCaseLine.contains("mus musculus")){
							isMouseRecord = true;
						}
					}
					line = reader.readLine();
				}
				recordLines.add(line);

				if(isMouseRecord){
					for (String string : recordLines) {
	                    writer.write(string+"\n");
                    }
				}
			}

		}
		writer.close();
		reader.close();
	}

	/**
	 * Extracts uniprot entries related to fruit fly.
	 *
	 * */
	public static void generateUniProtFruitFlyFile(String uniprotFile, String outfile) throws IOException{
		FileWriter writer = new FileWriter(outfile);
		BufferedReader reader = new BufferedReader(new FileReader(uniprotFile));
		String line;

		while((line=reader.readLine())!=null){
			if(line.startsWith("#")){
				writer.write(line+"\n");
				continue;
			}

			if(line.startsWith("ID")){
				// new record
				boolean isFlyRecord = false;
				List<String> recordLines = new LinkedList<String>();

				while(!line.startsWith("//")){
					recordLines.add(line);
					if(line.startsWith("OS")){
						String lowerCaseLine = line.toLowerCase();
						if(lowerCaseLine.contains("drosophila melanogaster")){
							isFlyRecord = true;
						}
					}
					line = reader.readLine();
				}
				recordLines.add(line);

				if(isFlyRecord){
					for (String string : recordLines) {
	                    writer.write(string+"\n");
                    }
				}
			}

		}
		writer.close();
		reader.close();
	}


	/**
	 * @throws IOException */
	public static void generateGeneIdsToPMIds(String trainingGeneList, String outfile) throws IOException{
		Map<String, Set<String>> geneIdsToPMIds = new HashMap<String, Set<String>>();
		BufferedReader reader = new BufferedReader(new FileReader(trainingGeneList));
		String line;
		while((line=reader.readLine())!=null){
			String[] parts = line.split("\t");
			String pmid = parts[0];
			String geneId = parts[1];
			Set<String> pmIds = geneIdsToPMIds.get(geneId);
			if(pmIds==null){
				pmIds = new HashSet<String>();
				geneIdsToPMIds.put(geneId, pmIds);
			}
			pmIds.add(pmid);
		}
		reader.close();

		FileWriter writer = new FileWriter(outfile);
		for (String geneId : geneIdsToPMIds.keySet()) {
			Set<String> pmIds = geneIdsToPMIds.get(geneId);
			writer.write(geneId);
			for (String pmId : pmIds) {
				writer.write("\t"+pmId);
            }
			writer.write("\n");
        }
		writer.close();
	}

	/**
	 * @throws IOException */
	public static Map<String, Set<String>> geneIdsToPMIds(String genelist) throws IOException{
		Map<String, Set<String>> geneIdsToPMIds = new HashMap<String, Set<String>>();
		BufferedReader reader = new BufferedReader(new FileReader(genelist));
		String line;
		while((line=reader.readLine())!=null){
			String[] parts = line.split("\t");
			String pmid = parts[0];
			String geneId = parts[1];
			Set<String> pmIds = geneIdsToPMIds.get(geneId);
			if(pmIds==null){
				pmIds = new HashSet<String>();
				geneIdsToPMIds.put(geneId, pmIds);
			}
			pmIds.add(pmid);
		}
		reader.close();

		return geneIdsToPMIds;
	}


	/***/
	public static Map<String, Set<String>> pmIdsToGeneIds(String genelist) throws IOException
    {
		Map<String, Set<String>> pmIdsToGeneIds = new HashMap<String, Set<String>>();
		BufferedReader reader = new BufferedReader(new FileReader(genelist));
		String line;
		while((line=reader.readLine())!=null){
			String[] parts = line.split("\t");
			String pmid = parts[0];
			String geneId = parts[1];
			Set<String> geneIds = pmIdsToGeneIds.get(pmid);
			if(geneIds==null){
				geneIds = new HashSet<String>();
				pmIdsToGeneIds.put(pmid, geneIds);
			}
			geneIds.add(geneId);
		}
		reader.close();

		return pmIdsToGeneIds;
    }


	/***/
	public static List<String> getTokensFromTextStopWordFiltered(String text){
		List<String> tokenList = new LinkedList<String>();
		String[] tokens = SimpleTokenizer.tokenize(text);
		for(int j=0;j<tokens.length;j++){
			tokens[j] = tokens[j].trim();

			if(tokens[j].equals(".") || tokens[j].equals(",") || tokens[j].equals(")") || tokens[j].equals("(") || tokens[j].equals(";")){
				continue;
			}

			if(tokens[j].endsWith("."))
				tokens[j] = tokens[j].substring(0, tokens[j].length()-1);

			if(tokens[j].endsWith(","))
				tokens[j] = tokens[j].substring(0, tokens[j].length()-1);

			if(tokens[j].endsWith(";"))
				tokens[j] = tokens[j].substring(0, tokens[j].length()-1);

			if(tokens[j].endsWith(")"))
				tokens[j] = tokens[j].substring(0, tokens[j].length()-1);

			if(tokens[j].startsWith("("))
				tokens[j] = tokens[j].substring(1);

			if(!STOPWORDS.contains(tokens[j])){
				tokenList.add(tokens[j]);
			}
		}
		return tokenList;
	}


	/***/
	public static String[] getTokensFromTextAsArrayStopWordFiltered(String text){
		List<String> tokenList = BiocreativeHelper.getTokensFromTextStopWordFiltered(text);
		String[] tokens = new String[tokenList.size()];
		int i=0;
		for (String token : tokenList) {
	        tokens[i++] = token;
        }
		return tokens;
	}



	/**
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws ClassNotFoundException */
	@SuppressWarnings("unchecked")
    public static Map<String, String[]> getEntrezGeneObjectMap(String entrezGeneObjectFile) throws FileNotFoundException, IOException, ClassNotFoundException{
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(entrezGeneObjectFile));
		Map<String, String[]> table = (HashMap<String, String[]>) ois.readObject();
		ois.close();
		return table;
	}


    /**
     * Returns a lexicon, one entry per line, as mapping from name to id set.
     * @throws IOException
     * */
    public static Map<String, Set<String>> getEntrezGeneLexicon(String lexiconFilename, String exclusionsNameFile, boolean toLowerCase) throws IOException{
    	Map<String, Set<String>> name2ids = new HashMap<String, Set<String>>();
    	String[] rows = FileHelper.readFromFile(new File(lexiconFilename));

    	Set<String> exclusions = new HashSet<String>();
    	if(exclusionsNameFile!=null){
    		exclusions = FileHelper.readFileIntoSet(exclusionsNameFile, toLowerCase);
    	}

    	for (String row : rows) {
			String[] colums = row.split("\t");
			String geneId = colums[0];
			String name = colums[1];
			if(toLowerCase)
				name = name.toLowerCase();
			if(exclusions.contains(name))
				continue;
			Set<String> ids = name2ids.get(name);
			if(ids==null){
				ids = new HashSet<String>();
				name2ids.put(name, ids);
			}
			ids.add(geneId);
		}

    	return name2ids;
    }

    /**
     * Returns a lexicon, one entry per line, as mapping from name to id set.
     * @throws IOException
     * */
    public static Map<String, Set<String>> getEntrezGeneLexicon_ID2NAMES(String lexiconFilename, String exclusionsNameFile, boolean toLowerCase) throws IOException{
    	Map<String, Set<String>> id2names = new HashMap<String, Set<String>>();
    	String[] rows = FileHelper.readFromFile(new File(lexiconFilename));

    	Set<String> exclusions  =  new HashSet<String>();
    	if(exclusionsNameFile!=null){
    		exclusions  = FileHelper.readFileIntoSet(exclusionsNameFile, toLowerCase);
    	}

    	for (String row : rows) {
			String[] colums = row.split("\t");
			String geneId = colums[0];
			String name = colums[1];
			if(toLowerCase)
				name = name.toLowerCase();
			if(exclusions.contains(name))
				continue;
			Set<String> names = id2names.get(geneId);
			if(names==null){
				names = new HashSet<String>();
				id2names.put(geneId, names);
			}
			names.add(name);
		}

    	return id2names;
    }


    public static void writeLexiconToFile(Map<String, Set<String>> lexicon, String outfile) throws IOException{
    	FileWriter writer = new FileWriter(outfile);

    	for (String key : lexicon.keySet()) {
    		Set<String> values = lexicon.get(key);
    		for (String string : values) {
	            writer.write(key+"\t"+string+"\n");
            }
        }
    	writer.close();
    }


    /***/
    public static void writeStatistics(Map<String, ScoredName> scoredNames, String filename) {

    	Map<Float, Set<String>> scoreToNames = new HashMap<Float, Set<String>>();

    	List<Float> scoreList = new LinkedList<Float>();

    	for (String name : scoredNames.keySet()) {
    		ScoredName scoredName = scoredNames.get(name);
    		Float avg = scoredName.getAverageScore();


    		Set<String> names = scoreToNames.get(avg);
    		if(names==null){
    			names = new HashSet<String>();
    			scoreToNames.put(avg, names);
    		}
    		names.add(name);
    		if(!scoreList.contains(avg)){
    			scoreList.add(avg);
    		}
        }

    	Collections.sort(scoreList);


    	try {
	        FileWriter writer = new FileWriter(filename);
	        for (Float score : scoreList) {
	            Set<String> names = scoreToNames.get(score);

	            writer.write(score+"\t");
	            for (String name : names) {
	            	writer.write(name+"\t");
	            }
	            writer.write("\n");
	        }
	        writer.close();
        }
        catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }


    }

    /**
     * Maps PMIDs to a set of contained Gene IDs.
     * */
    public static Map<String, Set<String>> getGoldTPs(String trainingGeneList) throws IOException{
    	Map<String, Set<String>> truePositives = new HashMap<String, Set<String>>();
    	String[] lines = FileHelper.readFromFile(new File(trainingGeneList));
    	for (String line : lines) {
    		if(line.length()>0) {
    			String[] parts = line.split("\t");
	    		String pmId = parts[0];
	    		String geneId = parts[1];

	    		Set<String> genesInText = truePositives.get(pmId);
	    		if(genesInText==null){
	    			genesInText = new HashSet<String>();
	    			truePositives.put(pmId, genesInText);
	    		}
	    		genesInText.add(geneId);
    		}
    	}
    	return truePositives;
    }


    /**
     * Maps PMIDs to a set of contained Gene IDs.
     * */
    public static Map<String, Set<Evidence>> getGoldEvidences(String trainingGeneList) throws IOException{
    	Map<String, Set<Evidence>> truePositives = new HashMap<String, Set<Evidence>>();
    	NameRangeExpander nameRangeExpander = new NameRangeExpander();
    	String[] lines = FileHelper.readFromFile(new File(trainingGeneList));
    	for (String line : lines) {
    		if(line.length()>0) {
    			String[] parts = line.split("\t");
	    		String pmId = parts[0];
	    		String geneId = parts[1];
	    		String evidence = " "+parts[2]+" ";
	    		Set<Evidence> evidencesInText = truePositives.get(pmId);
	    		if(evidencesInText==null){
	    			evidencesInText = new HashSet<Evidence>();
	    			truePositives.put(pmId, evidencesInText);
	    		}
	    		String expandedEvidence = nameRangeExpander.expandText(evidence);
//	    		if(!evidence.equals(expandedEvidence)){
//	    			System.out.println("BioCreativeHelper.geGoldEvidences(): expanded evidence is "+expandedEvidence+ " for evidence: "+evidence);
//	    		}
	    		evidencesInText.add(new Evidence(pmId, geneId, expandedEvidence));
    		}
    	}
    	return truePositives;
    }


    /***/
    public static void writeTFDFScores(Map<RecognizedEntity, Set<String>> geneIdMap, Map<RecognizedEntity, float[]> tfDfMap, String nameScoreFile, String thresholdCurveFile, String trainingGenelist, double tfIdfThreshold) {
    	try {

    		System.out.println("WRITING TFIDF SCORES");

    		int tpsAboveThreshold = 0;
    		int tpsBelowThreshold = 0;

    		int fpsAboveThreshold = 0;
    		int fpsBelowThreshold = 0;

    		double maxTF = 0;
    		double maxDF = 0;

	    	Map<String, Set<String>> truePositives = getGoldTPs(trainingGenelist);

	    	// WRITE TPS
	    	Map<String, Set<String>> textToNamesMap = new HashMap<String, Set<String>>();
	        FileWriter writer = new FileWriter(nameScoreFile+".tps");
	        for (RecognizedEntity recognizedGeneName : tfDfMap.keySet()) {

	        	Set<String> names = textToNamesMap.get(recognizedGeneName.getText().getID());
	        	if(names==null){
	        		names = new HashSet<String>();
	        		textToNamesMap.put(recognizedGeneName.getText().getID(), names);
	        	}
	        	if(names.contains(recognizedGeneName.getName())){
	        		continue;
	        	}
	        	names.add(recognizedGeneName.getName());

	        	float[] scores = tfDfMap.get(recognizedGeneName);
	        	if(scores[0]>maxTF){
	        		maxTF = scores[0];
	        	}
	        	if(scores[1]>maxDF){
	        		maxDF = scores[1];
	        	}
	            Set<String> genesInText = truePositives.get(recognizedGeneName.getText().getID());
	            Set<String> genesForName = geneIdMap.get(recognizedGeneName);
	            String containee = contains(genesInText, genesForName);
	            if(containee!=null){

	            	if(scores[2]>tfIdfThreshold)
	            		tpsAboveThreshold++;
	            	else
	            		tpsBelowThreshold++;

//	            	if(scores[1]>1000){
//	            		System.out.println("TP! "+recognizedGeneName.getName()+", TFIDF="+scores[2]);
//	            	}

		            writer.write("" +scores[0]);
		            writer.write(" "+scores[1]);
		            writer.write(" "+scores[2]);
		            writer.write(" "+recognizedGeneName.getText().getID());
		        	writer.write(" "+recognizedGeneName.getName());
		            writer.write("\n");
	            }
            }
	        writer.close();


	        // WRITE FPS
	        textToNamesMap = new HashMap<String, Set<String>>();
	        writer = new FileWriter(nameScoreFile+".fps");
	        for (RecognizedEntity recognizedGeneName : tfDfMap.keySet()) {

	        	Set<String> names = textToNamesMap.get(recognizedGeneName.getText().getID());
	        	if(names==null){
	        		names = new HashSet<String>();
	        		textToNamesMap.put(recognizedGeneName.getText().getID(), names);
	        	}
	        	if(names.contains(recognizedGeneName.getName())){
	        		continue;
	        	}
	        	names.add(recognizedGeneName.getName());


	        	float[] scores = tfDfMap.get(recognizedGeneName);
	        	if(scores[0]>maxTF){
	        		maxTF = scores[0];
	        	}
	        	if(scores[1]>maxDF){
	        		maxDF = scores[1];
	        	}
	        	Set<String> genesInText = truePositives.get(recognizedGeneName.getText().getID());
		        Set<String> genesForName = geneIdMap.get(recognizedGeneName);
		        String containee = contains(genesInText, genesForName);
	            if(containee==null){
	            	if(scores[2]>tfIdfThreshold)
	            		fpsAboveThreshold++;
	            	else
	            		fpsBelowThreshold++;

	            	writer.write("" +scores[0]);
		            writer.write(" "+scores[1]);
		            writer.write(" "+scores[2]);
		            writer.write(" "+recognizedGeneName.getText().getID());
		        	writer.write(" "+recognizedGeneName.getName());
		            writer.write("\n");
	            }
            }
	        writer.close();

	        // write threshold line
//	        writer = new FileWriter(tfIdfThreshold+"_"+thresholdCurveFile);
//	        for(int i=0;i<=maxTF + 10;i++){
//	        	double x = i;
//	        	double y = i / tfIdfThreshold;
//	        	writer.write(x+"\t"+y+"\n");
//	        }
//	        writer.close();

//	        writer = new FileWriter(tfIdfThreshold+0.1+"_"+thresholdCurveFile);
//	        for(int i=0;i<=maxTF;i++){
//	        	double x = i;
//	        	double y = i / (tfIdfThreshold+0.1);
//	        	writer.write(x+"\t"+y+"\n");
//	        }
//	        writer.close();
//
//	        writer = new FileWriter(tfIdfThreshold+0.2+"_"+thresholdCurveFile);
//	        for(int i=0;i<=maxTF;i++){
//	        	double x = i;
//	        	double y = i / (tfIdfThreshold+0.2);
//	        	writer.write(x+"\t"+y+"\n");
//	        }
//	        writer.close();

	        System.out.println("MAX TF = "+maxTF);
	        System.out.println("MAX DF = "+maxDF);
	        System.out.println("TPs above threshold: "+tpsAboveThreshold);
	        System.out.println("TPs below threshold: "+tpsBelowThreshold);
	        System.out.println("FPs above threshold: "+fpsAboveThreshold);
	        System.out.println("FPs below threshold: "+fpsBelowThreshold);
        }
        catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }

    }

    /***/
	public static Map<String, Set<String>> getTrainingExcerpts(String goldList) throws IOException
    {
		Map<String, Set<String>> geneMap = new HashMap<String, Set<String>>();
	    BufferedReader reader = new BufferedReader(new FileReader(goldList));
	    String line = null;
	    while((line=reader.readLine())!=null){
	    	String[] parts = line.split("\t");
	    	String pmId = parts[0];
	    	String geneId = parts[1];
	    	String key = pmId+";"+geneId;
	    	Set<String> genesInText = geneMap.get(key);
	    	if(genesInText==null){
	    		genesInText = new HashSet<String>();
	    		geneMap.put(key, genesInText);
	    	}
	    	for(int i=2;i<parts.length;i++){
	    		genesInText.add(parts[i]);
	    	}
	    }
	    reader.close();
	    return geneMap;
    }


    /***/
    private static String contains(Set<String> genesInText, Set<String> genesForName){
    	if(genesInText==null)
    		return null;

    	for (String string : genesForName) {
	        if(genesInText.contains(string)){
	        	return string;
	        }
        }
    	return null;
    }


    /**
	 * Prints out:
	 * 	number of genes,
	 * 	ambiguous number of genes,
	 * 	avg. number of names per gene,
	 * 		variance of names per gene,
	 * avg. number of genes per name,
	 * 		variance of genes per name,
	 * 	avg. number of tokens per name
	 * 		variance of tokens per name,
	 * 	genes with max number of synonyms
     * @throws IOException
	 * */
	public static void showStatistics(GeneRepository geneRepository) throws IOException {
		Gene geneWithMaxNames = null;

		float numberOfNames = 0;
		float averageNamesPerGene = 0;
		float varianceNamesPerGene = 0;

		float numberOfNameTokens = 0;
		float averageTokensPerName = 0;
		float varianceTokensPerName = 0;

		float maxNumberOfNamesForGene = 0;

		float averageGenesPerName = 0;

		float ambiguousNumOfGenes = 0;

		int numberOfGenesWithOnlyOneName = 0;

		float numberOfGenes;
		Collection<Gene> genes = geneRepository.getGenes();
		numberOfGenes = genes.size();

		Map<String, List<Gene>> nameToGenesMap = new HashMap<String, List<Gene>>();

		Set<String> totalNames = new HashSet<String>();

		// means
		for (Gene gene : genes) {

			Set<String> names = gene.getNames();

			if(names.size()==1){
				numberOfGenesWithOnlyOneName++;
			}

			totalNames.addAll(names);

			averageNamesPerGene += names.size();

			for (String name : names) {
				String[] tokens = name.split(" ");
				numberOfNameTokens += tokens.length;
				List<Gene> genesForThisName = nameToGenesMap.get(name);
				if(genesForThisName==null){
					genesForThisName = new LinkedList<Gene>();
					nameToGenesMap.put(name, genesForThisName);
				}

				//genesForThisName.add(gene);
				if(!genesForThisName.contains(gene)){
					genesForThisName.add(gene);
				}else{
					System.err.println("Gene "+gene.getID()+" contains name "+name+" more than once!");
				}
			}

			if (names.size() > maxNumberOfNamesForGene) {
				maxNumberOfNamesForGene = names.size();
				geneWithMaxNames = gene;
			}
		}

		numberOfNames = totalNames.size();

		averageNamesPerGene /= numberOfGenes;
		averageTokensPerName = numberOfNameTokens / numberOfNames;

		// variances
		for (Gene gene : genes)
		{
			Set<String> names = gene.getNames();
			for (String name : names) {
				String[] tokens = name.split(" ");
				varianceTokensPerName += Math.pow(tokens.length-averageTokensPerName, 2);
			}
			varianceNamesPerGene += Math.pow(names.size()-averageNamesPerGene, 2);
		}

		varianceNamesPerGene /= numberOfGenes;
		varianceTokensPerName /= numberOfNames;


		// ambiguity
		Set<Gene> ambiguousGenes = new HashSet<Gene>();
		for (String name : nameToGenesMap.keySet()) {

			List<Gene> genesForThisName = nameToGenesMap.get(name);

			averageGenesPerName += genesForThisName.size();

			if(genesForThisName.size()==0){
				System.err.println("No genes for name "+name);
			}

			if(name.equals("human")){
				System.out.println("Genes for human: "+genesForThisName.size());
			}
			if(name.equalsIgnoreCase("hypothetical")){
				System.out.println("Genes for hypothetical: "+genesForThisName.size());
			}

			if(genesForThisName.size()>1){
				for (Gene gene : genesForThisName) {
	                ambiguousGenes.add(gene);
                }
			}
        }

		FileWriter writer = new FileWriter("ambiguousGenes.txt");
		for (Gene gene : ambiguousGenes) {
			writer.write(gene.getID()+"\n");
        }
		writer.close();

		averageGenesPerName /= numberOfNames;
		ambiguousNumOfGenes = ambiguousGenes.size();


		System.out.println("Total number of genes in this lexicon = "+numberOfGenes);

		System.out.println("Total number of unique names in this lexicon = "+numberOfNames);

		System.out.println("Total number of genes with only one name = "+numberOfGenesWithOnlyOneName);

		System.out.println("Ambiguous number of genes in this lexicon = "+ambiguousNumOfGenes);

		System.out.println("Average number of genes per name = "+ averageGenesPerName);

		//System.out.println("Max number of ambigue names for a gene = "+ maxNumberOfAmbigueNamesForGene + ", GeneId = "+geneWithMaxNumOfAmbiguousNames.getID());

		System.out.println("Average number of names per gene = "+ averageNamesPerGene + ", variance = "+varianceNamesPerGene);

		System.out.println("Average number of tokens per name = "+ averageTokensPerName + ", variance = "+varianceTokensPerName);

		System.out.println("Max number of names for a gene = "+ maxNumberOfNamesForGene + ", GeneId = "+geneWithMaxNames.getID());

	}


	/**
	 * @throws IOException **/
	public static void testTextSimilarity(TextRepository trainingTextRepository, TextRepository noisyTextRepository, String trainingGeneList, String noisyTrainGeneList) throws IOException
	{
		Map<String, Set<String>> geneIdsToPMIds = geneIdsToPMIds(trainingGeneList);
		Map<String, Set<String>> geneIdsToNoisyPMIds = geneIdsToPMIds(noisyTrainGeneList);

		// initialize generator
		VectorGenerator generator = new VectorGenerator();
		List<String> tokenList = new LinkedList<String>();
		Collection<Text> texts = trainingTextRepository.getTexts();
		for (Text text : texts) {
			String[] tokens =BiocreativeHelper.getTokensFromTextAsArrayStopWordFiltered(text.getPlainText());
			for (String token : tokens)	{
				tokenList.add(token);
            }
        }
		Collection<Text> noisyTexts = noisyTextRepository.getTexts();
		for (Text text : noisyTexts) {
			String[] tokens = BiocreativeHelper.getTokensFromTextAsArrayStopWordFiltered(text.getPlainText());
			for (String token : tokens)	{
				tokenList.add(token);
            }
        }

		//tokenList = normalize(tokenList);

		generator.initializeOverallFrequencies(tokenList);


		// make text vectors
		Map<String, FeatureVector> trainingVectors = new HashMap<String, FeatureVector>();
		Map<String, FeatureVector> noisyTrainingVectors = new HashMap<String, FeatureVector>();

		texts = trainingTextRepository.getTexts();
		for (Text text : texts) {
			String[] tokens = BiocreativeHelper.getTokensFromTextAsArrayStopWordFiltered(text.getPlainText());
			FeatureVector vector = generator.generateFeatureVector(tokens, text.getID());
			trainingVectors.put(text.getID(), vector);

        }
		noisyTexts = noisyTextRepository.getTexts();
		for (Text text : noisyTexts) {
			String[] tokens = BiocreativeHelper.getTokensFromTextAsArrayStopWordFiltered(text.getPlainText());
			FeatureVector vector = generator.generateFeatureVector(tokens, text.getID());
			noisyTrainingVectors.put(text.getID(), vector);
        }

		// for every gene:
		//	1. avg distance of its training texts to its noisy texts
		// 	2. avg distance of its training texts to all other noisy texts

		Map<String, List<Double>> selfDistances = new HashMap<String, List<Double>>();
		Map<String, List<Double>> otherDistances = new HashMap<String, List<Double>>();

		int genesTested = 0;

		for (String geneId : geneIdsToPMIds.keySet()) {
			Set<String> pmIds = geneIdsToPMIds.get(geneId);
			Set<String> noisyPmIds = geneIdsToNoisyPMIds.get(geneId);

			if(noisyPmIds==null){
				// no noisy texts for this gene; skipping it
				continue;
			}

			genesTested++;

			Set<String> otherNoisyPmIds = getPmIdsNotForThisKey(geneIdsToNoisyPMIds, geneId);

			List<Double> selfDistanceList = new LinkedList<Double>();
			List<Double> otherDistanceList = new LinkedList<Double>();

			for (String pmId : pmIds) {
				FeatureVector vector = trainingVectors.get(pmId);
				for (String noisyPmId : noisyPmIds) {
					FeatureVector noisyVector = noisyTrainingVectors.get(noisyPmId);
					double dist = vector.cosineDistance(noisyVector);
					selfDistanceList.add(dist);
				}
				for (String otherNoisyPmId : otherNoisyPmIds) {
					FeatureVector otherNoisyVector = noisyTrainingVectors.get(otherNoisyPmId);
					double dist = vector.cosineDistance(otherNoisyVector);
					otherDistanceList.add(dist);
				}
            }

			selfDistances.put(geneId, selfDistanceList);
			otherDistances.put(geneId, otherDistanceList);
        }


		// t-test
		List<Double> controlValues = new LinkedList<Double>();
		List<Double> testValues = new LinkedList<Double>();

		for (String geneId : selfDistances.keySet()) {
			List<Double> selfDistanceList = selfDistances.get(geneId);
			List<Double> otherDistanceList = otherDistances.get(geneId);

			controlValues.add(MathHelper.mean(selfDistanceList));
			testValues.add(MathHelper.mean(otherDistanceList));
		}

		double controlMean = MathHelper.mean(controlValues);
		double testMean = MathHelper.mean(testValues);
		double controlVariance = MathHelper.variance(controlValues);
		double testVariance = MathHelper.variance(testValues);

		System.out.println("testMean="+testMean+", testVariance="+testVariance+", controlMean="+controlMean+", controlVariance="+controlVariance);

		double tValue = (testMean - controlMean) / Math.sqrt( (testVariance/(float)testValues.size()) + (controlVariance/(float)controlValues.size()) );
		int degreeOfFreedom = genesTested - 2;

		System.out.println("T-Value="+tValue+", df="+degreeOfFreedom);

	}

	/**
	 * @throws IOException *
	 * @throws ClassNotFoundException */
	public static void testTextSimilarityWithEntrezGeneDescriptions(TextRepository trainingTextRepository, String trainingGeneList, Map<String, String> geneIdsToDescription) throws IOException, ClassNotFoundException
	{
		Map<String, Set<String>> geneIdsToPMIds = geneIdsToPMIds(trainingGeneList);

		// initialize generator
		VectorGenerator generator = new VectorGenerator();
		List<String> tokenList = new LinkedList<String>();
		Collection<Text> texts = trainingTextRepository.getTexts();
		for (Text text : texts) {
			String[] tokens = BiocreativeHelper.getTokensFromTextAsArrayStopWordFiltered(text.getPlainText());
			for (String token : tokens)	{
				tokenList.add(token);
            }
        }
		Collection<String> descsriptions = geneIdsToDescription.values();
		for (String descsriptionArray : descsriptions) {
			String[] tokens = BiocreativeHelper.getTokensFromTextAsArrayStopWordFiltered(descsriptionArray);
			for (String token : tokens)	{
				tokenList.add(token);
            }
        }

		//tokenList = normalize(tokenList);

		generator.initializeOverallFrequencies(tokenList);


		// make text vectors
		Map<String, FeatureVector> trainingVectors = new HashMap<String, FeatureVector>();	// pmId to vector
		Map<String, FeatureVector> descriptionVectors = new HashMap<String, FeatureVector>(); // geneId to vector

		texts = trainingTextRepository.getTexts();
		for (Text text : texts) {
			String[] tokens = BiocreativeHelper.getTokensFromTextAsArrayStopWordFiltered(text.getPlainText());
			FeatureVector vector = generator.generateFeatureVector(tokens, text.getID());
			trainingVectors.put(text.getID(), vector);
        }
		for (String geneId : geneIdsToDescription.keySet()) {
			String description = geneIdsToDescription.get(geneId);
			String[] tokens = BiocreativeHelper.getTokensFromTextAsArrayStopWordFiltered(description);
			FeatureVector vector = generator.generateFeatureVector(tokens, geneId);
			descriptionVectors.put(geneId, vector);
        }

		// for every gene:
		//	1. avg distance of its training texts to its summary
		// 	2. avg distance of its training texts to all other summaries

		Map<String, List<Double>> selfDistances = new HashMap<String, List<Double>>();
		Map<String, List<Double>> otherDistances = new HashMap<String, List<Double>>();

		int genesTested = 0;
		for (String geneId : geneIdsToPMIds.keySet()) {
			Set<String> pmIds = geneIdsToPMIds.get(geneId);
			FeatureVector descriptionVector = descriptionVectors.get(geneId);
			if(descriptionVector==null){
				// skip genes without a description
				continue;
			}

			genesTested++;

			Set<String> otherGenes = getGenesNotForThisKey(geneIdsToDescription, geneId);

			List<Double> selfDistanceList = new LinkedList<Double>();
			List<Double> otherDistanceList = new LinkedList<Double>();

			for (String pmId : pmIds) {
				FeatureVector textVector = trainingVectors.get(pmId);
				double dist = textVector.cosineDistance(descriptionVector);
				selfDistanceList.add(dist);

				for (String otherGeneId : otherGenes) {
					FeatureVector otherDescriptionVector = descriptionVectors.get(otherGeneId);
					double otherDist = textVector.cosineDistance(otherDescriptionVector);
					otherDistanceList.add(otherDist);
				}
            }

			selfDistances.put(geneId, selfDistanceList);
			otherDistances.put(geneId, otherDistanceList);
        }


		// t-test
		List<Double> controlValues = new LinkedList<Double>();
		List<Double> testValues = new LinkedList<Double>();

		for (String geneId : selfDistances.keySet()) {
			List<Double> selfDistanceList = selfDistances.get(geneId);
			List<Double> otherDistanceList = otherDistances.get(geneId);

			//controlValues.add(MathHelper.mean(selfDistanceList));
			//testValues.add(MathHelper.mean(otherDistanceList));
			controlValues.addAll(selfDistanceList);
			testValues.addAll(otherDistanceList);
		}

		double controlMean = MathHelper.mean(controlValues);
		double testMean = MathHelper.mean(testValues);
		double controlVariance = MathHelper.variance(controlValues);
		double testVariance = MathHelper.variance(testValues);

		System.out.println("testMean="+testMean+", testVariance="+testVariance+", controlMean="+controlMean+", controlVariance="+controlVariance);

		double tValue = (testMean - controlMean) / Math.sqrt( (testVariance/(float)testValues.size()) + (controlVariance/(float)controlValues.size()) );
		int degreeOfFreedom = genesTested - 2;

		System.out.println("T-Value="+tValue+", df="+degreeOfFreedom);

	}


	/**
	 * Removes all stop words.
	 * */
	public static String[] normalizeTokenArray(String[] tokens)
    {
	    List<String> normalizedTokenList = new LinkedList<String>();
	    for (String string : tokens) {
	    	if(!STOPWORDS.contains(string)){
	    		normalizedTokenList.add(string);
	    	}
	    }

	    String[] normedTokenArray = new String[normalizedTokenList.size()];
	    int i=0;
	    for (String string : normalizedTokenList) {
	    	normedTokenArray[i++] = string;
	    }

	    return normedTokenArray;
    }

//	/**
//	 * Removes all stop words.
//	 * */
//	public static List<String> normalize(List<String> tokenList)
//    {
//	    List<String> normalizedTokenList = new LinkedList<String>();
//	    for (String string : tokenList) {
//	    	if(!STOPWORDS.contains(string)){
//	    		normalizedTokenList.add(string);
//	    	}
//	    }
//	    return normalizedTokenList;
//    }


	/***/
	private static Set<String> getPmIdsNotForThisKey(Map<String, Set<String>> pmIdMap, String key){
		Set<String> otherPmIds = new HashSet<String>();

		for (String	geneId : pmIdMap.keySet()) {
			if(!geneId.equals(key)){
				Set<String> pmIds = pmIdMap.get(geneId);
				otherPmIds.addAll(pmIds);
			}
        }

		return otherPmIds;
	}

	/***/
	private static Set<String> getGenesNotForThisKey(Map<String, String> geneDescriptions, String key){
		Set<String> otherGeneIds = new HashSet<String>();

		for (String	geneId : geneDescriptions.keySet()) {
			if(!geneId.equals(key)){
				otherGeneIds.add(geneId);
			}
        }

		return otherGeneIds;
	}

	public static Map<String, String> unify(Map<String, String[]> summaries, Map<String, String[]> geneRifs)
    {
		Map<String, String> geneDescriptions = new HashMap<String, String>();

		for(String geneId : summaries.keySet()){
			String summary = summaries.get(geneId)[0];
			geneDescriptions.put(geneId, summary);
		}

		for(String geneId : geneRifs.keySet()){
			String description = geneDescriptions.get(geneId);

			String geneRifString = "";
			String[] geneRifArray = geneRifs.get(geneId);
			for (String string : geneRifArray) {
				geneRifString += string +" ";
            }

			if(description==null){
				description = geneRifString;
			}else{
				description = description + " " + geneRifString;
			}

			geneDescriptions.put(geneId, description);
		}

	    return geneDescriptions;
    }

	/***/
	public static String toString(String[] stringArray)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < stringArray.length; i++) {
			sb.append(stringArray[i] + " ");
		}
		return sb.toString().trim();
	}


	/**
	 * @throws IOException
	 * */
	public static Context toContext(String oldPredictionFile, TextRepository textRepository) throws IOException{
		Context context = new Context();

		NameRangeExpander nameRangeExpander = new NameRangeExpander();

		BufferedReader reader = new BufferedReader(new FileReader(oldPredictionFile));
		String line=null;

		while((line=reader.readLine())!=null){
			String[] parts = line.split("\t");
			String pmId = parts[0];
			String geneId = parts[1];
			String excerpt = parts[2];
			String confidence = parts[3];
			int startIndex = Integer.parseInt(parts[4]);
			int length = Integer.parseInt(parts[5]);
			String annotator = parts[6];

			Text text = textRepository.getText(pmId);

			nameRangeExpander.expandText(text);

			List<RecognizedEntity> namesSoFar = context.getRecognizedEntitiesHavingNameInText(excerpt, text);
			if(namesSoFar.size()==0){
				RecognizedEntity recognizedGeneName = new RecognizedEntity(text, new TextAnnotation(new TextRange(startIndex, startIndex+length+1), excerpt));
				context.addRecognizedEntity(recognizedGeneName, new String[]{geneId});
			}
			else if(namesSoFar.size()==1){
				context.getIdentificationStatus(namesSoFar.get(0)).addIdCandidate(geneId);
			}
			else{
				System.out.println("DOUBLE NAMES!! in "+pmId);
			}

//			String excerptByIndex = plainText.substring(startIndex, startIndex+length+1);
//			if(!excerpt.equals(excerptByIndex)){
//				System.out.println("PMID="+pmId);
//				System.out.println("Excerpt='"+excerpt+"'");
//				System.out.println("ExcerptByIndex='"+excerptByIndex+"'");
//			}

		}
		reader.close();

		return context;
	}


	/**
	 * TODO describe me!
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception
	{
//		TextRepository textRepository = TextFactory.loadTextRepositoryFromDirectory("data/abs_train/", "textObjects/pmGoAccessionNumbers.object");
//		toContext("data/annotations9034.tab", textRepository);

		//generateEntrezGeneMouseFile("data/gene_info", "data/gene_info_mouse.txt");
		//generateUniProtMouseFile("data/uniprot_sprot_2007_04_11.dat", "data/uniprot_sprot_2007_04_11_mouse.dat");

		//enrichMouseSynonymList("data/Mouse/mouse_synonyms.list", "data/Mouse/mouse_to_EntrezGene_and_UniProt.txt", "data/Mouse/gene_info_mouse.txt", "data/uniprot_sprot_synonyms_20070411.txt", "data/Mouse/mouse_synonyms.ext.list");
		//enrichYeastSynonymList("data/Yeast/yeast_synonyms.list", "data/Yeast/yeast_to_entrezGene.txt", "entrezGeneObjects/Yeast/uIDs.object", "data/gene_info", "data/uniprot_sprot_synonyms_20070411.txt", "data/Yeast/yeast_synonyms.ext.list");
		//enrichFlySynonymList("data/Fly/fly_synonyms.list", "data/Fly/fly_to_entrezGene.txt", "entrezGeneObjects/Fly/uIDs.object", "data/gene_info", "data/uniprot_sprot_synonyms_20070411.txt", "data/Fly/fly_synonyms.ext.txt");

		//generateGOCodeObjectForMouse("/data/cplake/workspace/BioCreative3/data/2004/Task1B/Mouse/gene_association.mgi", "/data/cplake/workspace/BioCreative3/entrezGeneObjects/Mouse/goIDs.object");
		//generateUniprotIdObjectFileForMouse("data/2004/Task1B/Mouse/train/mouse_to_EntrezGene_and_UniProt.txt", "/data/cplake/workspace/BioCreative3/entrezGeneObjects/Mouse/uIDs.object");
		//generatePMIDObjectForMouse("data/2004/Task1B/Mouse/train/mouse_to_EntrezGene_and_UniProt.txt", "data/generifs_basic", "data/gene2pubmed", "/data/cplake/workspace/BioCreative3/entrezGeneObjects/Mouse/pmIDs.object");
		//generateGeneRifObjectForMouse("data/2004/Task1B/Mouse/train/mouse_to_EntrezGene_and_UniProt.txt", "data/generifs_basic", "/data/cplake/workspace/BioCreative3/entrezGeneObjects/Mouse/geneRifs.object");

		//generatePMIDObjectForYeast("data/2004/Task1B/Yeast/train/yeast_to_entrezGene.txt", "data/generifs_basic", "data/gene2pubmed", "/data/cplake/workspace/BioCreative3/entrezGeneObjects/Yeast/pmIDs.object");
		//generatePMIDObjectForHuman("humanIds.list", "data/generifs_basic", "data/gene2pubmed", "/data/cplake/workspace/BioCreative3/entrezGeneObjects/pmIDs.object");
		//generatePMIDObjectForFly("data/Fly/fly_to_entrezGene.txt", "data/generifs_basic", "data/gene2pubmed", "entrezGeneObjects/Fly/pmIDs.object");

		//showContentOfGo2Go("data/go2go.object");

		//getPubmedIdsForTexts("data/Fly/test/text/", "data/Fly/alias_fly_testing_filename2pmid.list");

		//generateUniprotIdObjectFileForYeast("data/uniprot_sprot_2007_04_11.dat", "data/2004/Task1B/Yeast/train/yeastIds.list", "entrezGeneObjects/Yeast/uIDs.object");
		//generateUniprotIdObjectFileForFly("data/uniprot_sprot_2007_04_11.dat", "data/Fly/flyIds.list", "entrezGeneObjects/Fly/uIDs.object");

		//showContent("entrezGeneObjects/Human/uIDs.object");

		//generateAccessionNumberFile("human_synonyms.list", "humanIds.list");

		//generateYeastToEntrezGeneIdMapping("data/2004/Task1B/Yeast/train/yeastIds.list", "data/gene_info", "data/2004/Task1B/Yeast/train/yeast_to_entrezGene.txt");
		//generateFlyToEntrezGeneIdMapping("data/Fly/flyIds.list", "data/gene_info", "data/Fly/fly_to_entrezGene.txt");


		//BiocreativeHelper.showStatistics(ConradsMain.geneRepository);

		System.out.println(getDiscontinuedGenes("../Resources/EntrezGene/gene_history").size());

//
//		//TextRepository noisyTextRepository = TextFactory.loadTextRepositoryFromDirectory("data/abs_noisy/", "textObjects/pmGoAccessionNumbers.object");
//		//testTextSimilarity(textRepository, noisyTextRepository, "training.genelist", "noisytrain.genelist");
//
//		Map<String, String[]> summaries = getEntrezGeneObjectMap("entrezGeneObjects/summaries.object");
//		Map<String, String[]> geneRifs = getEntrezGeneObjectMap("entrezGeneObjects/geneRifs.object");
//		Map<String, String> geneDescriptions = unify(summaries, geneRifs);
//		testTextSimilarityWithEntrezGeneDescriptions(textRepository, "training.genelist", geneDescriptions);
	}
}
