package gnat.representation;


import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Context for NEI processing.<br>
 * This context keeps track of recognized entities and their identification status.
 * <br><br>
 * Provides several methods to sort/print/store predicted results in various formats.
 *
 */

public class Context {

	/** Maps entities that have been recognized to their identification status, that is, whether they have
	 *  been identified already, and/or which candidate ID(s) remain. */
	private Map<RecognizedEntity, IdentificationStatus> identificationStatusMap;
	/** Contains all genes that have been identified. */
	private List<IdentifiedGene> asGeneIdentifiedEntities;
	/** A mapping from PubMed IDs to gene names contained in each article. */
	private HashMap<String, TreeSet<String>> pmid2allNames = new HashMap<String, TreeSet<String>>();


	/**
	 * Creates a new and empty context.
	 * */
	public Context(){
		this.asGeneIdentifiedEntities = new LinkedList<IdentifiedGene>();
		this.identificationStatusMap = new Hashtable<RecognizedEntity, IdentificationStatus>();
		this.pmid2allNames = new HashMap<String, TreeSet<String>>();
		//this.allNames = new TreeSet<String>();
	}


	/**
	 * Creates a new context with a set of recognized entities and their candidate identifiers.
	 * */
	public Context(Map<RecognizedEntity, Set<String>> recognizedEntitiesWithCandidateIds){
		this();
		for (Entry<RecognizedEntity, Set<String>> recognizedEntities : recognizedEntitiesWithCandidateIds.entrySet()) {
			addRecognizedEntity(recognizedEntities.getKey(), recognizedEntities.getValue());
        }
	}


	/**
	 * Resets all information on genes, identification status, and PubMed to name mappings.
	 */
	public void clear () {
		this.asGeneIdentifiedEntities.clear();
		this.identificationStatusMap.clear();
		this.pmid2allNames.clear();
	}


	/**
	 * Returns a mapping from text id to sets of recognized gene names.
	 * */
	public Map<String, Set<RecognizedEntity>> getRecognizedEntitiesAsMap(){
		Map<String, Set<RecognizedEntity>> textIdToEntities = new HashMap<String, Set<RecognizedEntity>>();
		Set<RecognizedEntity> recognizedGeneNames = getRecognizedEntities();
		for (RecognizedEntity name : recognizedGeneNames) {
	        Set<RecognizedEntity> namesForText = textIdToEntities.get(name.getText().getID());
	        if(namesForText==null){
	        	namesForText = new HashSet<RecognizedEntity>();
	        	textIdToEntities.put(name.getText().getID(), namesForText);
	        }
	        namesForText.add(name);
		}
		return textIdToEntities;
	}


	/**
	 * 	Returns an iterator of already identified genes.
	 * */
	public Iterator<IdentifiedGene> getIdentifiedGenesIterator(){
		return asGeneIdentifiedEntities.iterator();
	}

	/**
	 * 	Returns a list of identified genes in this context.
	 * */
	public List<IdentifiedGene> getEntitiesIdentifiedAsGene(){
		return asGeneIdentifiedEntities;
	}

	/**
	 * 	Returns a set of recognized entities in this context.
	 * */
	public Set<RecognizedEntity> getRecognizedEntities(){
		Set<RecognizedEntity> entities = new HashSet<RecognizedEntity>();
		for (RecognizedEntity entity : identificationStatusMap.keySet()) {
			entities.add(entity);
        }
		return entities;
	}


	/**
	 * 	Returns a set of texts containing recognized entities in this context.
	 * */
	public Set<Text> getTexts(){
		Set<Text> textSet = new HashSet<Text>();
		Set<RecognizedEntity> recognizedEntities = this.getRecognizedEntities();
		for (RecognizedEntity name : recognizedEntities) {
			textSet.add(name.getText());
        }
		return textSet;
	}


	/**
	 * 	Adds a recognized entity to this context.
	 * */
	public void addRecognizedEntity(RecognizedEntity recognizedEntity){
//		System.err.print("# New RE " + recognizedEntity.getName() + " ");
		if (identificationStatusMap.containsKey(recognizedEntity)) {
			// keep as is: no new IDs
		} else
			identificationStatusMap.put(recognizedEntity, new IdentificationStatus());
		
		TreeSet<String> names;
		if (pmid2allNames.containsKey(recognizedEntity.getText().getPMID())) {
			names = pmid2allNames.get(recognizedEntity.getText().getPMID());
		} else {
			names = new TreeSet<String>();
		}
		names.add(recognizedEntity.getName());
		pmid2allNames.put(""+recognizedEntity.getText().getPMID(), names);
	}

	/**
	 * 	Adds a recognized entity together with a bunch of id candidates to this context.
	 * */
	public void addRecognizedEntity(RecognizedEntity recognizedEntity, Set<String> idCandidates){
		if (identificationStatusMap.containsKey(recognizedEntity)) {
			IdentificationStatus old = identificationStatusMap.get(recognizedEntity);
			old.addIdCandidates(idCandidates);
			identificationStatusMap.put(recognizedEntity, old);
		} else
			identificationStatusMap.put(recognizedEntity, new IdentificationStatus(idCandidates));
		
		TreeSet<String> names;
		if (pmid2allNames.containsKey(recognizedEntity.getText().PMID)) {
			names = pmid2allNames.get(recognizedEntity.getText().PMID);
		} else {
			names = new TreeSet<String>();
		}
		names.add(recognizedEntity.getName());
		pmid2allNames.put(""+recognizedEntity.getText().getPMID(), names);
		//allNames.add(recognizedEntity.getName() + "/" + recognizedEntity.getText().PMID + "/"
		//		+ recognizedEntity.getBegin() + "/" + recognizedEntity.getEnd());
		
		/*if (identificationStatusMap.containsKey(recognizedEntity)) {
			System.err.print("# Adding IDs to existing RE " + recognizedEntity.getName() + ": ");
			IdentificationStatus idStatus = identificationStatusMap.get(recognizedEntity);
			System.err.print(idStatus.getIdCandidates());
			idStatus.addIdCandidates(idCandidates);
			System.err.println(" => " + idStatus.getIdCandidates());
			identificationStatusMap.put(recognizedEntity, idStatus);
		} else {
			System.err.print("# New RE.1 " + recognizedEntity.getName() + " (" 
					+ recognizedEntity.getTextRange().getBegin()
					+ "-" + recognizedEntity.getTextRange().getEnd() + ")");
			identificationStatusMap.put(recognizedEntity, new IdentificationStatus(idCandidates));
		}*/
	}

	/**
	 * 	Adds a recognized entity together with a bunch of id candidates to this context.
	 * */
	public void addRecognizedEntity(RecognizedEntity recognizedEntity, String[] idCandidates){
		if (identificationStatusMap.containsKey(recognizedEntity)) {
			IdentificationStatus old = identificationStatusMap.get(recognizedEntity);
			old.addIdCandidates(idCandidates);
			identificationStatusMap.put(recognizedEntity, old);
		} else
			identificationStatusMap.put(recognizedEntity, new IdentificationStatus(idCandidates));
		
		TreeSet<String> names;
		if (pmid2allNames.containsKey(recognizedEntity.getText().PMID)) {
			names = pmid2allNames.get(recognizedEntity.getText().PMID);
		} else {
			names = new TreeSet<String>();
		}
		names.add(recognizedEntity.getName());
		pmid2allNames.put(""+recognizedEntity.getText().PMID, names);
	}

	
	/**
	 * 	Adds a recognized entity together with a bunch of id candidates to this context.
	 * */
	public void addRecognizedEntity1(RecognizedEntity recognizedEntity, String[] idCandidates){
		String nameKey = recognizedEntity.getName() + "/" + recognizedEntity.getText().PMID + "/"
			+ recognizedEntity.getBegin() + "/" + recognizedEntity.getEnd();
		String cands = "";
		for (String cnd: idCandidates)
			cands += " " + cnd;
		//System.out.println("#1stNER: adding " + nameKey + " " + cands);
		
		if (identificationStatusMap.containsKey(recognizedEntity)) {
			System.out.println("#Duplicate!");
			IdentificationStatus old = identificationStatusMap.get(recognizedEntity);
			//System.out.println("#Old IDs: ");
			old.addIdCandidates(idCandidates);
			identificationStatusMap.put(recognizedEntity, old);
		} else
			identificationStatusMap.put(recognizedEntity, new IdentificationStatus(idCandidates));
		
		//if (!allNames.contains(nameKey)) {
		  //System.out.println("#RemNER: adding " + nameKey + " " + cands);
		  //allNames.add(nameKey);
		  //identificationStatusMap.put(recognizedEntity, new IdentificationStatus(idCandidates));
		//}
		TreeSet<String> names;
		if (pmid2allNames.containsKey(recognizedEntity.getText().PMID)) {
			names = pmid2allNames.get(recognizedEntity.getText().PMID);
		} else {
			names = new TreeSet<String>();
		}
		names.add(recognizedEntity.getName());
		pmid2allNames.put(""+recognizedEntity.getText().PMID, names);
		
		
		/*if (identificationStatusMap.containsKey(recognizedEntity)) {
			System.err.print("# Adding IDs to existing RE " + recognizedEntity.getName() + ": ");
			IdentificationStatus idStatus = identificationStatusMap.get(recognizedEntity);
			System.err.print(idStatus.getIdCandidates());
			idStatus.addIdCandidates(idCandidates);
			System.err.println(" => " + idStatus.getIdCandidates());
			identificationStatusMap.put(recognizedEntity, idStatus);
		} else {
			System.err.print("# New RE.2 " + recognizedEntity.getName() + " ("
					+ recognizedEntity.getTextRange().getBegin()
					+ "-" + recognizedEntity.getTextRange().getEnd() + ")");
			identificationStatusMap.put(recognizedEntity, new IdentificationStatus(idCandidates));
		}*/
	}

	/**
	 * 	Adds a recognized entity together with a bunch of id candidates to this context.
	 * */
	public void addRecognizedEntity2(RecognizedEntity recognizedEntity, String[] idCandidates){
		String nameKey = recognizedEntity.getName() + "/" + recognizedEntity.getText().PMID + "/"
				+ recognizedEntity.getBegin() + "/" + recognizedEntity.getEnd();
		String cands = "";
		for (String cnd: idCandidates)
			cands += " " + cnd;
		
		if (pmid2allNames.containsKey(recognizedEntity.getText().PMID)) {
			boolean found = false;
			TreeSet<String> names = pmid2allNames.get(recognizedEntity.getText().PMID);
			for (String name: names) {
				if (name.indexOf(recognizedEntity.getName()) >= 0
					|| (recognizedEntity.getName().indexOf(name)) >= 0) {
					System.out.println("#2ndNER: duplicate " + nameKey + " " + cands);
					found = true;
					break;
				}
			}
			if (!found) {
				System.out.println("#2ndNER: adding new " + nameKey + " " + cands);
				if (identificationStatusMap.containsKey(recognizedEntity)) {
					IdentificationStatus old = identificationStatusMap.get(recognizedEntity);
					old.addIdCandidates(idCandidates);
					identificationStatusMap.put(recognizedEntity, old);
				} else
					identificationStatusMap.put(recognizedEntity, new IdentificationStatus(idCandidates));
			} else {
				System.out.println("#2ndNER: adding IDs " + nameKey + " " + cands);
				IdentificationStatus idStatus = identificationStatusMap.get(recognizedEntity);
				if (idStatus != null) {
					System.out.print("#Old IDs: " + idStatus.getIdCandidates());
					idStatus.addIdCandidates(idCandidates);
					System.out.println("#All IDs: " + idStatus.getIdCandidates());
					identificationStatusMap.put(recognizedEntity, idStatus);
				} else
					System.out.println("#Nullpointer!");
			}
		} else {
			System.out.println("#2ndNER: adding new " + nameKey + " " + cands);
			if (identificationStatusMap.containsKey(recognizedEntity)) {
				IdentificationStatus old = identificationStatusMap.get(recognizedEntity);
				old.addIdCandidates(idCandidates);
				identificationStatusMap.put(recognizedEntity, old);
			} else
				identificationStatusMap.put(recognizedEntity, new IdentificationStatus(idCandidates));
		}
	}


	/**
	 * 	Associates a recognized entity with a gene. Thus, this entity gets identified as a gene.
	 * */
	public void identifyAsGene(RecognizedEntity recognizedEntity, Gene gene, Float confidenceScore) {
		//System.out.println("Identifying ...");
		IdentifiedGene identifiedGene = new IdentifiedGene(recognizedEntity, gene, confidenceScore);
		asGeneIdentifiedEntities.add(identifiedGene);

		IdentificationStatus identificationStatus = identificationStatusMap.get(recognizedEntity);
		if(identificationStatus==null){
			identificationStatus = new IdentificationStatus();
			identificationStatusMap.put(recognizedEntity, identificationStatus);
		}
		identificationStatus.markAsIdentified(gene.getID());
	}


	/**
	 * Returns all recognized entities in this context mapped to their identification status.
	 * */
	public Map<RecognizedEntity, IdentificationStatus> getRecognizedEntitiesWithIdentificationStatus(){
		return Collections.unmodifiableMap(identificationStatusMap);
	}


	/**
	 * 	Removes a recognized entity from this context.
	 */
	public void removeRecognizedEntity(RecognizedEntity recognizedEntity)
    {
		identificationStatusMap.remove(recognizedEntity);
    }

	/**
	 * Removes all recognized entities having the given name and appearing in the given text.
	 */
	public void removeEntitiesHavingName(String name, Text text)
	{
		Set<RecognizedEntity> toRemove = new HashSet<RecognizedEntity>();

		Set<RecognizedEntity> recognizedEntities = this.getRecognizedEntitiesInText(text);
		for (RecognizedEntity recognizedEntity : recognizedEntities) {
			if(recognizedEntity.getName().equals(name)){
				toRemove.add(recognizedEntity);
			}
        }

		for (RecognizedEntity recognizedEntity : toRemove) {
			removeRecognizedEntity(recognizedEntity);
        }
    }


	/**
	 * Returns a list of recognized entities that have not yet been identified.
	 */
	public List<RecognizedEntity> getUnidentifiedEntities()
    {
		List<RecognizedEntity> unidentifiedEntities = new LinkedList<RecognizedEntity>();
	    for(Entry<RecognizedEntity,IdentificationStatus> recognizedGeneNameStatus : identificationStatusMap.entrySet()){
	    	if(!recognizedGeneNameStatus.getValue().isIdentified()){
	    		unidentifiedEntities.add(recognizedGeneNameStatus.getKey());
	    	}
	    }
	    return unidentifiedEntities;

    }

	/**
	 * Returns the identification status of a recognized entity.
	 * */
	public IdentificationStatus getIdentificationStatus(RecognizedEntity recognizedEntity){
		return identificationStatusMap.get(recognizedEntity);
	}


	/**
	 * Returns a set of candidate ids for a recognized entity.
	 * */
	public Set<String> getIdCandidates(RecognizedEntity recognizedEntity){
		Set<String> candidates = new HashSet<String>();
		IdentificationStatus identificationStatus = identificationStatusMap.get(recognizedEntity);
		if(identificationStatus!=null){
			candidates = identificationStatus.getIdCandidates();
		}
		return candidates;
	}


	/**
	 * 	Returns all entities recognized in the given text.
	 * */
	public Set<RecognizedEntity> getRecognizedEntitiesInText(Text text)
    {
		Set<RecognizedEntity> entitiesInText = new HashSet<RecognizedEntity>();
		for(RecognizedEntity recognizedEntity : this.getRecognizedEntities()){
	    	if(recognizedEntity.getText().getID().equals(text.getID())){
	    		entitiesInText.add(recognizedEntity);
	    	}
	    }
		return entitiesInText;
    }

	/**
	 * 	Returns an entity appearing in the given text at the exact position.
	 * 	Do not call extensively!
	 * */
	public RecognizedEntity getRecognizedEntityInTextAtPosition(Text text, TextRange range)
    {
	    RecognizedEntity recognizedEntityAtPosition = null;

	    Set<RecognizedEntity> entitiesInText = getRecognizedEntitiesInText(text);
	    for (RecognizedEntity entity : entitiesInText) {
	        if(entity.getAnnotation().getTextRange().equals(range)){
	        	recognizedEntityAtPosition = entity;
	        	break;
	        }
        }

	    return recognizedEntityAtPosition;
    }

	/**
	 * Returns a list of entities appearing in the given text and having the given name.
	 * */
	public List<RecognizedEntity> getRecognizedEntitiesHavingNameInText(String name, Text text){
		List<RecognizedEntity> entitiesInText = new LinkedList<RecognizedEntity>();
		for(RecognizedEntity recognizedEntity : this.getRecognizedEntities()){
	    	if(recognizedEntity.getText().getID().equals(text.getID()) && recognizedEntity.getName().equals(name)){
	    		entitiesInText.add(recognizedEntity);
	    	}
	    }
		return entitiesInText;
	}

	/**
	 * Returns all entities having the given name.
	 * */
	public List<RecognizedEntity> getRecognizedEntitiesHavingName(String name){
		List<RecognizedEntity> entitiesWithName = new LinkedList<RecognizedEntity>();
		for(RecognizedEntity recognizedEntity : identificationStatusMap.keySet()){
	    	if(recognizedEntity.getName().equals(name)){
	    		entitiesWithName.add(recognizedEntity);
	    	}
	    }
		return entitiesWithName;
	}


	/**
	 * Returns a mapping from text IDs to sets of identified genes.
	 * */
	public Map<String, Set<String>> toIdentifiedGeneMap()
    {
		Map<String, Set<String>> geneMap  = new HashMap<String, Set<String>>();
	    List<IdentifiedGene> identifiedGenes = this.getEntitiesIdentifiedAsGene();
	    for (IdentifiedGene gene : identifiedGenes) {
	    	String textId = gene.getRecognizedEntity().getText().getID();
	    	Set<String> genesInText = geneMap.get(textId);
	    	if(genesInText==null){
	    		genesInText = new HashSet<String>();
	    		geneMap.put(textId, genesInText);
	    	}
	    	genesInText.add(gene.getGene().getID());
        }
	    return geneMap;
    }
	
	
	/**
	 * Returns a list of strings that represent the result in BioCreative GN format:
	 * a tab-separated list with columns for text ID (PubMed ID), gene ID (EntrezGene),
	 * gene name as found in the text, a confidence score, start position, and end position.
	 * @return
	 */
	public List<String> getIdentifiedGeneList () {
		List<String> result = new LinkedList<String>();
	    List<IdentifiedGene> identifiedGenes = this.getEntitiesIdentifiedAsGene();
	    for (IdentifiedGene gene : identifiedGenes) {
	    	String line = 
	    		gene.getRecognizedEntity().getText().getID()
	    			+"\t"+gene.getGene().getID()
	    			+"\t"+gene.getName()
	    			+"\t"+gene.getConfidenceScore()
	    			+"\t"+gene.getRecognizedEntity().getBegin()
	    			+"\t"+gene.getRecognizedEntity().getEnd()
	    			+"\t"+gene.getGene().getTaxon()
	    			;
	    	result.add(line);
        }

	    return result;
    }
	
	
	/**
	 * Returns a list of strings that represent the result in TSV format:
	 * a tab-separated list with columns for text ID (PubMed ID), gene ID (EntrezGene),
	 * gene name(s) as found in the text, a confidence score, and species.
	 * <br><br>
	 * This list is sorted by text ID and gene ID; thus all duplicate gene IDs (per text)
	 * are removed, even if they occur at different positions in the text.
	 * @see #getIdentifiedGeneListInBioCreativeFormat()
	 * @return
	 */
	public List<String> getIdentifiedGeneList_SortedByTextAndId () {
		List<String> result = new LinkedList<String>();
		
	    List<IdentifiedGene> identifiedGenes = this.getEntitiesIdentifiedAsGene();
	    List<IdentifiedGene> uniqIdentifiedGenes = new LinkedList<IdentifiedGene>();
	    
	    // remove duplicate genes: same text ID, same gene ID,
	    // but maybe different name and start/end position (thus IdentifiedGene.equal() does not work here).
	    TreeSet<String> textIds = new TreeSet<String>();
	    Map<String, TreeSet<String>> textToGeneIds = new HashMap<String, TreeSet<String>>();
	    for (IdentifiedGene gene : identifiedGenes) {
	    	String textId = gene.getRecognizedEntity().getText().getID();
	    	//String name   = gene.getName();
	    	String geneId = gene.getGene().getID();
	    	
	    	textIds.add(textId);
	    	TreeSet<String> geneIds = textToGeneIds.get(textId);
	    	
	    	if (geneIds == null)
	    		geneIds = new TreeSet<String>();
	    	
	    	if (geneIds.contains(geneId)) {
	    		
	    	} else {
		    	geneIds.add(geneId);
		    	textToGeneIds.put(textId, geneIds);
		    	uniqIdentifiedGenes.add(gene);
	    	}
	    }
	    
	    Collections.sort(uniqIdentifiedGenes, new IdentifiedGeneIdComparator());
	    		//new IdentifiedGeneNameComparator());
	    
	    for (IdentifiedGene gene : uniqIdentifiedGenes) {
	    	String line = 
	    		gene.getRecognizedEntity().getText().getID()
	    		+"\t"+gene.getGene().getID()
	    		+"\t"+gene.getName()
	    		+"\t"+gene.getConfidenceScore()
	    		+"\t"+gene.getGene().getTaxon()
	    		;
	    	result.add(line);
        }

	    return result;
    }

	
	
	/**
	 * 
	 * @param text
	 * @return
	 */
	public String getInlineAnnotations (Text text) {
		StringBuilder result = new StringBuilder();
		
		// <src:GNAT ENREZGENE=“2064“ pt=“ERBB2“>her-2</src:GNAT>
		
		//text.plainText
		
		
		return result.toString();
	}
	

	/**
	 * Writes all currently identified genes in this context to a file; the file will be 
	 * overwritten if it exists already.
	 * @see #appendIdentifiedGeneList_SortedByPosition(String)
	 * @throws IOException
	 * */
	public void writeIdentifiedGeneList_SortedByPosition (String outfile) throws IOException {
		FileWriter writer = new FileWriter(outfile);
		List<String> lines = toIdentifiedGeneList_SortedByPosition();
		for (String line: lines)
			writer.write(line + "\n");
		writer.close();
    }
	
	
	/**
	 * Appends all currently identified genes in this context to a file of identified genes.
	 * The file will be created if it does not exist yet.
	 * @see #writeIdentifiedGeneList_SortedByPosition(String)
	 * @throws IOException
	 * */
	public void appendIdentifiedGeneList_SortedByPosition (String outfile) throws IOException {
		FileWriter writer = new FileWriter(outfile, true);
		List<String> lines = toIdentifiedGeneList_SortedByPosition();
		for (String line: lines)
			writer.write(line + "\n");
		writer.close();
    }
	
	
	/**
	 * 
	 * <br><br>
	 * Note: currently sorts by start position, even across texts! So this is useful
	 * only if results for a single text are involved.
	 * 
	 * @return
	 */
	public List<String> toIdentifiedGeneList_SortedByPosition () {
		List<String> geneList = new LinkedList<String>();
		
		//TreeSet<String> addedGenes = new TreeSet<String>();
		HashMap<Integer, TreeSet<IdentifiedGene>> allGenes = new HashMap<Integer, TreeSet<IdentifiedGene>>();
		Vector<Integer> begins = new Vector<Integer>();
		List<IdentifiedGene> identifiedGenes = this.getEntitiesIdentifiedAsGene();
	    for (IdentifiedGene gene : identifiedGenes) {
	    	if (!begins.contains(gene.getRecognizedEntity().getBegin()))
	    		begins.add(gene.getRecognizedEntity().getBegin());
	    	TreeSet<IdentifiedGene> genes = allGenes.get(gene.getRecognizedEntity().getBegin());
	    	if (genes == null) {
	    		genes = new TreeSet<IdentifiedGene>();
	    		allGenes.put(gene.getRecognizedEntity().getBegin(), genes);
	    	}
	    	genes.add(gene);
	    	
	    	//if (allGenes.containsKey(gene.getRecognizedEntity().getBegin())) {
	    	//	//TODO: merge annotations for this form of output
	    	//} else {
	    	//	
	    	//	genes.add(gene);
	    	//	allGenes.put(gene.getRecognizedEntity().getBegin(), genes);
	    	//}
	    }
	    Collections.sort(begins);
	    Collections.reverse(begins);
	    
	    for (int begin: begins) {
	    	TreeSet<IdentifiedGene> genes = allGenes.get(begin);
	    	String ids = "";
	    	String taxa = "";
	    	for (IdentifiedGene gene: genes) {
	    		if (ids.length() == 0)
	    			ids = gene.getGene().getID();
	    		else
	    			ids += ";" + gene.getGene().getID();
	    		
	    		if (taxa.length() == 0)
	    			taxa = ""+gene.getGene().taxon;
	    		else
	    			taxa += ";" + gene.getGene().taxon;
	    	}
	    	IdentifiedGene gene = genes.first();
	    	String line = gene.getRecognizedEntity().getText().getID()
				+"\t"+ids
				+"\t"+gene.getRecognizedEntity().getName()
				+"\t"+gene.getConfidenceScore()
				+"\t"+gene.getRecognizedEntity().getBegin()
				+"\t"+gene.getRecognizedEntity().getEnd()
				+"\t"+taxa;
	    	geneList.add(line);
	    }
	    
	    return geneList;
	}

	
	/**
	 * Writes a file with all currently identified genes in BioCreative-tab-delimited-format:
	 * text ID, gene ID, gene name as mentioned in the text, confidence score.
	 * 
	 * @throws IOException
	 * */
	public void toIdentifiedGeneListInBioCreativeFormat (String outfile) throws IOException {
		FileWriter writer = new FileWriter(outfile);
	    List<IdentifiedGene> identifiedGenes = this.getEntitiesIdentifiedAsGene();
	    for (IdentifiedGene gene : identifiedGenes) {
	    	String line = 
	    		gene.getRecognizedEntity().getText().getID()
	    			+"\t"+gene.getGene().getID()
	    			+"\t"+gene.getName()
	    			+"\t"+gene.getConfidenceScore();
	    	writer.write(line+"\n");
        }

	    writer.close();
    }
	

	/**
	 * Sorts a set of recognized entities using a RecognizedEntityComparator.
	 * */
	public static List<RecognizedEntity> sortRecognizedEntities (Set<RecognizedEntity> entities) {
		List<RecognizedEntity> entityList = toList(entities);
		Collections.sort(entityList, new RecognizedEntityComparator());
		return entityList;
	}


	/**
	 * Transforms a set of entities into a list.
	 * */
	public static List<RecognizedEntity> toList(Set<RecognizedEntity> regognizedEntities){
		List<RecognizedEntity> list = new ArrayList<RecognizedEntity>(regognizedEntities.size());
		for (RecognizedEntity entity : regognizedEntities) {
	        list.add(entity);
        }
		return list;
	}
	

	/**
	 * A comparator to compare entities by their text range begin index. If both have equal begin index,
	 * the name length is compared. Sorts by text ID first.
	 * */
	public static class RecognizedEntityComparator implements Comparator<RecognizedEntity>{
		public int compare(RecognizedEntity arg0, RecognizedEntity arg1) {
			
			// try text ID first
			String text0 = arg0.getText().getID();
			String text1 = arg1.getText().getID();
			int compare = text0.compareTo(text1);
			if (compare != 0) return compare;
			
			// now try start position
			int beginArg0 = arg0.getAnnotation().getTextRange().getBegin();
			int beginArg1 = arg1.getAnnotation().getTextRange().getBegin();
			compare = new Integer(beginArg0).compareTo(beginArg1);
			if (compare != 0) return compare;

			// finally use length
			compare = new Integer(arg0.getName().length()).compareTo(arg1.getName().length());
			return compare;
		}
	}

	
	/**
	 * A comparator to compare identified genes by their text ID, gene name, and gene ID.
	 * */
	public static class IdentifiedGeneNameComparator implements Comparator<IdentifiedGene>{
		public int compare (IdentifiedGene arg0, IdentifiedGene arg1) {
			// try text ID first
			String text0 = arg0.getRecognizedEntity().getText().getID();
			String text1 = arg1.getRecognizedEntity().getText().getID();
			int compare = text0.compareTo(text1);
			if (compare != 0) return compare;
			
			compare = arg0.getName().compareTo(arg1.getName());
			if (compare != 0) return compare;
			
			compare = arg0.getGene().getID().compareTo(arg0.getGene().getID());
			return compare;
		}
	}
	
	
	/**
	 * A comparator to compare identified genes by their text ID and gene ID (disregards gene names).
	 * */
	public static class IdentifiedGeneIdComparator implements Comparator<IdentifiedGene>{
		public int compare (IdentifiedGene arg0, IdentifiedGene arg1) {
			// try text ID first
			String text0 = arg0.getRecognizedEntity().getText().getID();
			String text1 = arg1.getRecognizedEntity().getText().getID();
			int compare = text0.compareTo(text1);
			if (compare != 0) return compare;

			compare = arg0.getGene().getID().compareTo(arg0.getGene().getID());
			return compare;
		}
	}
}
