package gnat.representation;

/**
 * A position (start and end) in a text that marks a region of interest,
 * for example, containing evidence for a {@link TextAnnotation}.
 *
 */
public class TextRange
{
	private int begin;
	private int end;

	/** Construct a new text range, identified by the given start and end position. */
	public TextRange(int begin, int end){
		this.begin = begin;
		this.end = end;
	}

	/** Checks whether or not this TextRange encompasses <tt>range</tt>. <tt>range</tt> has to lie inside of
	 *  this TextRange, but start and/or end positions can be identical. */
	public boolean contains(TextRange range)
    {
       return (range.begin>=this.begin && range.end<=this.end);
    }

	public int getBegin()
    {
    	return begin;
    }

	public int getEnd()
    {
    	return end;
    }

	public void setEnd(int end){
		this.end = end;
	}

	/** Two TextRanges are identical if both their {@link #begin begins} and {@link #end ends} are identical. */
	@Override
	public boolean equals(Object o)
	{
		TextRange tr = (TextRange)o;
		return (tr.getBegin()==begin && tr.getEnd()==end);
	}
}
