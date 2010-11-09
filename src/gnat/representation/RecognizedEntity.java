package gnat.representation;



/**
 * Represents a recognized entity in a text.
 *
 */
public class RecognizedEntity
{
	private Text text;
	private TextAnnotation textAnnotation;


	/**
	 * Creates a new entity recognized in the given text under position denoted by the given annotation.
	 * */
	public RecognizedEntity(Text text, TextAnnotation textAnnotation)
    {
		this.text = text;
		this.textAnnotation = textAnnotation;
    }

	/**
	 * Returns the the text in which this entity was recognized.
	 * */
	public Text getText()
    {
	    return text;
    }

	/**
	 * Returns the entity's position range in the text.
	 * */
	public TextRange getTextRange(){
		return textAnnotation.getTextRange();
	}

	/**
	 * Returns the entity's start position in the text.
	 * */
	public int getBegin(){
		return textAnnotation.getBegin();
	}

	/**
	 * Returns the entity's end position in the text.
	 * */
	public int getEnd(){
		return textAnnotation.getEnd();
	}

	/**
	 * Returns the text annotation for this entity.
	 * */
	public TextAnnotation getAnnotation()
	{
		return textAnnotation;
	}

	/**
	 * Returns the evidence from the text.
	 * */
	public String getName()
    {
	    return textAnnotation.getEvidence();
    }

	@Override
	public String toString(){
		return getName();
	}

	public void setText(Text text)
    {
	    this.text = text;

    }


}
