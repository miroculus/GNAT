package gnat.representation;

import gnat.preprocessing.TextPreprocessor;

import java.io.Serializable;

/**
 *
 * @author Joerg Hakenberg
 *
 */


@SuppressWarnings("serial")
public class GeneContextModel extends ContextModel implements Serializable {


	/**
	 *
	 */
	public GeneContextModel () {
		super();
	}


	/**
	 *
	 * @param ID
	 */
	public GeneContextModel (String ID) {
		super();
		this.ID = ID;
	}


	/**
	 *
	 * @param diseases
	 */
	public void addDiseases (String[] diseases) {
		for (String dis: diseases) {
			ContextVector go = new ContextVector(
					TextPreprocessor.getEnglishWordTokens(dis),
					CONTEXTTYPE_DISEASE);
			addContextVector(go);
		}
	}


	/**
	 *
	 * @param summary
	 */
	public void addEntrezGeneSummary (String summary) {
		ContextVector egs = new ContextVector(
				TextPreprocessor.getEnglishWordTokens(summary),
				CONTEXTTYPE_ENTREZGENESUMMARY);
		addContextVector(egs);
	}


	/**
	 *
	 * @param acts
	 */
	public void addEnzymaticActivity (String acts) {
		ContextVector egs = new ContextVector(
				TextPreprocessor.getEnglishWordTokens(acts),
				CONTEXTTYPE_ENZYMEACTIVITY);
		addContextVector(egs);
	}


	/**
	 *
	 * @param rifs
	 */
	public void addGeneRIFs (String[] rifs) {
		for (String rif: rifs) {
			ContextVector rcv = new ContextVector(
					TextPreprocessor.getEnglishWordTokens(rif),
					CONTEXTTYPE_GENERIF);
			addContextVector(rcv);
		}
	}


	/**
	 *
	 * @param fcts
	 */
	public void addFunctions (String[] fcts) {
		for (String fct: fcts) {
			ContextVector fcv = new ContextVector(
				TextPreprocessor.getEnglishWordTokens(fct),
				CONTEXTTYPE_FUNCTION);
			addContextVector(fcv);
		}
	}


	/**
	 *
	 * @param codes
	 */
	public void addGOCodes (String[] codes) {
		ContextVector go = new ContextVector(codes, CONTEXTTYPE_GOCODES);
		addContextVector(go);
	}


	/**
	 *
	 * @param taxa
	 */
	public void addOrigin (String[] taxa) {
		ContextVector go = new ContextVector(taxa, CONTEXTTYPE_ORIGIN);
		addContextVector(go);
	}


	/**
	 *
	 * @param terms
	 */
	public void addGOTerms (String[] terms) {
		ContextVector go = new ContextVector(terms, CONTEXTTYPE_GOTERMS);
		addContextVector(go);
	}


	/**
	 *
	 * @param terms
	 */
	public void addInteractors (String[] terms) {
		ContextVector go = new ContextVector(terms, CONTEXTTYPE_INTERACTORS);
		addContextVector(go);
	}


	/**
	 *
	 * @param keywords
	 */
	public void addKeywords (String[] keywords) {
		ContextVector kws = new ContextVector(
			TextPreprocessor.getEnglishWordTerms(keywords),
			CONTEXTTYPE_KEYWORDS);
		addContextVector(kws);
	}


	/**
	 *
	 * @param locations
	 */
	public void addLocations (String[] locations) {
		ContextVector kws = new
			ContextVector(locations, CONTEXTTYPE_LOCATIONS);
		addContextVector(kws);
	}


	/**
	 *
	 * @param locations
	 */
	public void addSubcellularLocations (String[] locations) {
		ContextVector kws = new
			ContextVector(locations, CONTEXTTYPE_PROTEINSUBCELLLOCATIONS);
		addContextVector(kws);
	}


	/**
	 * 
	 * @param domains
	 */
	public void addProteinDomains (String[] domains) {
		ContextVector ps = new
			ContextVector(domains, CONTEXTTYPE_PROTEINDOMAINS);
		addContextVector(ps);
	}

	
	/**
	 * 
	 * @param interactors
	 */
	public void addProteinInteractions (String[] interactors) {
		ContextVector ps = new
			ContextVector(interactors, CONTEXTTYPE_PROTEININTERACTIONS);
		addContextVector(ps);
	}


	/**
	 * 
	 * @param lengths
	 */
	public void addProteinLengths (String[] lengths) {
		ContextVector ps = new
			ContextVector(lengths, CONTEXTTYPE_PROTEINLENGTHS);
		addContextVector(ps);
	}


	/**
	 * 
	 * @param mass
	 */
	public void addProteinMass (String[] mass) {
		ContextVector ps = new
			ContextVector(mass, CONTEXTTYPE_PROTEINMASS);
		addContextVector(ps);
	}


	/**
	 * 
	 * @param mutations
	 */
	public void addProteinMutations (String[] mutations) {
		ContextVector ps = new
			ContextVector(mutations, CONTEXTTYPE_PROTEINMUTATIONS);
		addContextVector(ps);
	}


	/**
	 * 
	 * @param pmids
	 */
	public void addPubMedReferences (String[] pmids) {
		ContextVector ps = new
			ContextVector(pmids, CONTEXTTYPE_PMIDS);
		addContextVector(ps);
	}


	/**
	 * 
	 * TODO:
	 * Remove certain words from the tissue specificity:<br>
	 * - expressed<br>
	 * - cell (?)<br>
	 * 
	 *
	 * @param tissues
	 */
	public void addTissues (String[] tissues) {
		for (String tissue: tissues) {
			ContextVector tcv = new ContextVector(
				TextPreprocessor.getEnglishWordTokens(tissue),
				CONTEXTTYPE_TISSUE);
			addContextVector(tcv);
		}
	}


	/**
	 * Checks if this context model has at least one context vector; otherwise,
	 * this model is deemed invalid.
	 * @return
	 */
	public boolean isValid () {
		return contexts.size() > 0;
	}


	/**
	 *
	 * @param args
	 */
	public static void main (String[] args) {
		// CASP7_HUMAN
		String[] gene2gocodes= new String[]{
				"GO:0005789", "GO:0031966",  // CC
				"GO:0008234", "GO:0005515",  // MF
				"GO:0008632", "GO:0006508",  // BP
				"GO:0006915"  // apoptosis ADDED!!!
				};
		String[] gene2goterms = new String[]{
				"endoplasmic reticulum membrane", "mitochondrial membrane", // CC
				"cysteine-type peptidase activity", "protein binding",     // MF
				"apoptotic program", "proteolysis and peptidolysis"        // BP
				};
		String gene2entrezgenesummary = "This gene encodes a protein which is a member of the cysteine-aspartic acid protease (caspase) family. Sequential activation of caspases plays a central role in the execution-phase of cell apoptosis. Caspases exist as inactive proenzymes which undergo proteolytic processing at conserved aspartic residues to produce two subunits, large and small, that dimerize to form the active enzyme. The precursor of this caspase is cleaved by caspase 3 and 10. It is activated upon cell death stimuli and induces apoptosis. Alternative splicing results in four transcript variants, encoding three distinct isoforms.";
		//String[] gene2entrezgenesummaryS = gene2entrezgenesummary.split("([\\,\\.])?( |$)+");

		GeneContextModel gene = new GeneContextModel("840");
		gene.addEntrezGeneSummary(gene2entrezgenesummary);
		gene.addGOCodes(gene2gocodes);
		gene.addGOTerms(gene2goterms);

		/*ContextVector go_codes = new ContextVector(gene2gocodes, CONTEXTTYPE_GOCODES);
		gene.addContextVector(go_codes);
		ContextVector eg_sum = new ContextVector(gene2entrezgenesummaryS, CONTEXTTYPE_ENTREZGENESUMMARY);
		gene.addContextVector(eg_sum);*/


		String abs2plain = "Regulation of p53-, Bcl-2- and caspase-dependent signaling pathway in xanthorrhizol-induced apoptosis of HepG2 hepatoma cells. Xanthorrhizol is a sesquiterpenoid compound extracted from the rhizome of Curcuma xanthorrhiza. This study investigated the antiproliferative effect and the mechanism of action of xanthorrhizol on human hepatoma cells, HepG2, and the mode of cell death. An antiproliferative assay using methylene blue staining revealed that xanthorrhizol inhibited the proliferation of the HepG2 cells with a 50% inhibition of cell growth (IC50) value of 4.17 +/- 0.053 microg/ml. The antiproliferative activity of xanthorrhizol was due to apoptosis induced in the HepG2 cells and not necrosis, which was confirmed by the Tdt-mediated dUTP nick end labeling (TUNEL) assay. The xanthorrhizol-treated HepG2 cells showed typical apoptotic morphology such as DNA fragmentation, cell shrinkage and elongated lamellipodia. The apoptosis mediated by xanthorrhizol in the HepG2 cells was associated with the activation of tumor suppressor p53 and down-regulation of antiapoptotic Bcl-2 protein expression, but not Bax. The levels of Bcl-2 protein expression decreased 24-h after treatment with xanthorrhizol and remained lower than controls throughout the experiment, resulting in a shift in the Bax to Bcl-2 ratio thus favouring apoptosis. The processing of the initiator procaspase-9 was detected. Caspase-3 was also found to be activated, but not caspase-7. Xanthorrhizol exerts antiproliferative effects on HepG2 cells by inducing apoptosis via the mitochondrial pathway.";
		//ContextVector abs_plain = new ContextVector(TextPreprocessor.getEnglishWordTokens(abs2plain));

		TextContextModel text = new TextContextModel("17465228");
		text.addPlainText(abs2plain);
		String[] abs2gocodes = new String[]{
				"GO:0016265", // death
				"GO:0006915", // apoptosis
				"GO:0065007", // biological regulation
				"GO:0001906", // cell killing
				"GO:0006309", // DNA fragmentation during apoptosis
				"GO:0007165"  // signal transduction
				};
		String[] abs2goterms = new String[]{
				"death",
				"apoptosis",
				"biological regulation",
				"cell killing",
				"DNA fragmentation during apoptosis",
				"signal transduction",
				"endoplasmic reticulum membrane", // ADDED!!!
				"apoptotic program" // ADDED!!!
				};
		ContextVector abs_go = new ContextVector(abs2gocodes, CONTEXTTYPE_GOCODES);
		ContextVector abs_got = new ContextVector(abs2goterms, CONTEXTTYPE_GOTERMS);
		text.addContextVector(abs_go);
		text.addContextVector(abs_got);

		//float score = gene.compareWithContextVectors(new ContextVector[]{abs_plain, abs_go});
		//System.out.println("Plain text score: " + gene.compareWithContextVector(abs_plain));
		//System.out.println("GO code score:    " + gene.compareWithContextVector(abs_go));

		//System.out.println("Comparison gene vs. text: score=" + gene.compareWithContextModel(text));
	}


	/** Data from EntrezGene: */
	public static String CONTEXTTYPE_GOCODES = "CODES_GO";
	public static String CONTEXTTYPE_GOTERMS = "TERMS_GO";
	public static String CONTEXTTYPE_LOCATIONS = "TERMS_LOCATIONS";
	public static String CONTEXTTYPE_INTERACTORS = "TERMS_INTERACTORS";
	public static String CONTEXTTYPE_PMIDS = "CODES_PMID";
	public static String CONTEXTTYPE_GENERIF = "TEXTS_GENERIF";
	public static String CONTEXTTYPE_GENEREF = "TEXTS_GENEREF";
	public static String CONTEXTTYPE_ORIGIN = "CODES_ORIGIN";
	public static String CONTEXTTYPE_UNIPROTID = "CODES_UNIPROTID";
	public static String CONTEXTTYPE_ENTREZGENESUMMARY = "TEXTS_ENTREZGENESUMMARY";

	/** Data from UniProt: */
	public static String CONTEXTTYPE_PROTEINSUBCELLLOCATIONS = "TERMS_PROTEINSUBCELLLOCATIONS";
	public static String CONTEXTTYPE_PROTEINMUTATIONS = "TERMS_PROTEINMUTATIONS";
	public static String CONTEXTTYPE_PROTEINDOMAINS = "TERMS_PROTEINDOMAINS";
	public static String CONTEXTTYPE_PROTEINLENGTHS = "TERMS_PROTEINLENGTHS";
	public static String CONTEXTTYPE_PROTEINMASS = "TERMS_PROTEINMASS";
	public static String CONTEXTTYPE_PROTEININTERACTIONS = "TERMS_PROTEININTERACTIONS";
	public static String CONTEXTTYPE_KEYWORDS = "TERMS_KEYWORDS";
	public static String CONTEXTTYPE_DISEASE = "TEXTS_DISEASE";
	public static String CONTEXTTYPE_FUNCTION = "TEXTS_FUNCTION";
	public static String CONTEXTTYPE_TISSUE = "TEXTS_TISSUE";
	
	/** Currently not used: */
	public static String CONTEXTTYPE_ENZYMEACTIVITY = "TEXTS_ENZYMEACTIVITY";
	public static String CONTEXTTYPE_MESHCODES = "CODES_MESH";
	public static String CONTEXTTYPE_MESHTERMS = "TERMS_MESH";
}
