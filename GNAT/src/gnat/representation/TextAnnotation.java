package gnat.representation;

/**
 * Represents an annotation that was added based on some evidence in a text.<br>
 * Annotations usually refer to recognized genes, species, GO terms, MeSH terms, etc.
 * 
 * @author hakenbej
 *
 */
public class TextAnnotation {

	/** The text string that caused this annotation, for instance, "apoptosis" in
	 * the text "...was involved in apoptosis during..." */
	private String evidence = "";

	/** */
	private TextRange textRange;

	/** The type of this TextAnnotation, see Type. */
	private Type type;

	/** Predefined types of TextAnnotations, for example, gene, protein, species, drug, MeSH term.<br>
	 *  Values are GENE, PROTEIN, SPECIES, GOTERM, GOCODE, DISEASE, MESHTERM, MESHCODE, DRUG, CHEMICAL,
	 *  and UNKNOWN. */
	public enum Type {UNKNOWN, GENE, PROTEIN, SPECIES, GOTERM, GOCODE, DISEASE, MESHTERM, MESHCODE, DRUG, CHEMICAL;
		public static Type getValue(String string) {
			if (string.equals("gene"))
				return GENE;
			else if (string.equals("protein"))
				return PROTEIN;
			else if (string.equals("species"))
				return SPECIES;
			else if (string.equals("goterm"))
				return GOTERM;
			else if (string.equals("gocode"))
				return GOCODE;
			else if (string.equals("disease"))
				return DISEASE;
			else if (string.equals("meshterm"))
				return MESHTERM;
			else if (string.equals("meshcode"))
				return MESHCODE;
			else if (string.equals("drug"))
				return DRUG;
			else if (string.equals("chemical"))
				return CHEMICAL;
			else
				return UNKNOWN;
		}
	}

	/** Predefined sources for TextAnnotations: see {@link TextAnnotation#source source}.<br>
	 *  Values are MANUAL, PUBMED (which includes MeSH annotations), AUTOMATED, and UNKNOWN. */
	public enum Source {MANUAL, PUBMED, AUTOMATED, UNKNOWN;
		public static Source getValue (String string) {
			if (string.equals("manual"))
				return MANUAL;
			else if (string.equals("human"))
				return MANUAL;
			else if (string.equals("pubmed"))
				return PUBMED;
			else if (string.equals("mesh"))
				return PUBMED;
			else if (string.equals("automated"))
				return AUTOMATED;
			else if (string.equals("automatic"))
				return AUTOMATED;
			return
				UNKNOWN;
		}
	}
	
	/** Source of this annotation: was it assigned manually, or copied from a PubMed/MeSH annotation, or
	 *  using an automated system (such as GNAT, LINNAEUS, etc.).<br>For predefined values, see {@link Source}. */
	private Source source;


	/**
	 * Constructs a new text annotation from a text range and the evidence found in the text, e.g. the text inside the range.
	 * */
	public TextAnnotation(TextRange textRange, String evidence){
		this.textRange = textRange;
		this.evidence = evidence;
	}


	/**
	 * 
	 * @param textRange
	 * @param evidence
	 * @param type
	 */
	public TextAnnotation(TextRange textRange, String evidence, Type type) {
		this.textRange = textRange;
		this.evidence = evidence;
		this.type = type;
	}

	
	public void setSource (String source) {
		this.source = Source.getValue(source);
	}
	
	public Source getSource () {
		return this.source;
	}
	

	/** Two TextAnnotations are equal if their TextRange and Type are equal. */
	@Override
	public boolean equals (Object o) {
		TextAnnotation to = (TextAnnotation)o;
		return (textRange.equals(to.getTextRange()) && type==to.getType());
	}


	@Override
	public int hashCode () {
		int h = 17;
		h = h + 17 * textRange.hashCode();
		h = h + 17 * type.hashCode();
		return h;
	}
	

	public String getEvidence() {
    	return evidence;
    }


	public void setEvidence (String evidence) {
    	this.evidence = evidence;
    }


	public int getBegin() {
		return textRange.getBegin();
	}

	
	public int getEnd() {
		return textRange.getEnd();
	}

	
	public TextRange getTextRange() {
    	return textRange;
    }


	public Type getType() {
    	return type;
    }

	public void setType (Type type) {
		this.type = type;
	}
	
}
