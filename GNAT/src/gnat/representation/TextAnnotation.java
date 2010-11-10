package gnat.representation;

public class TextAnnotation {

	/** The text string that caused this annotation, for instance, "apoptosis" in
	 * the text "...was involved in apoptosis during..."
	 */
	private String evidence = "";

	private TextRange textRange;

	private Type type;

	public enum Type {UNKNOWN, GENE, PROTEIN, SPECIES, GOTERM, GOCODE, DISEASE, MESHTERM, MESHCODE;
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
			//else if (string.equals("goterm"))
			//	return GOTERM;
			else
				return UNKNOWN;
		}
	}


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


	@Override
	public boolean equals(Object o)
	{
		TextAnnotation to = (TextAnnotation)o;
		return (textRange.equals(to.getTextRange()) && type==to.getType());
	}


	public String getEvidence()
    {
    	return evidence;
    }


	public void setEvidence(String evidence)
    {
    	this.evidence = evidence;
    }


	public int getBegin(){
		return textRange.getBegin();
	}

	public int getEnd(){
		return textRange.getEnd();
	}

	public TextRange getTextRange()
    {
    	return textRange;
    }


	public Type getType()
    {
    	return type;
    }
}
