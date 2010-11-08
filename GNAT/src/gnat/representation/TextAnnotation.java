package gnat.representation;

public class TextAnnotation {

	/** The text string that caused this annotation, for instance, "apoptosis" in
	 * the text "...was involved in apoptosis during..."
	 */
	private String evidence = "";

	private TextRange textRange;

	private int type;

	public static final int TYPE_UNKNOWN = -1;
	//public static final int TYPE_GO = 0;//split into CC,BP,MF?
	public static final int TYPE_GENE = 1;
	public static final int TYPE_PROTEIN = 2;
	public static final int TYPE_SPECIES = 3;


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
	public TextAnnotation(TextRange textRange, String evidence, int type) {
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


	public int getType()
    {
    	return type;
    }
}
