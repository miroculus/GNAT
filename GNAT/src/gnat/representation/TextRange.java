// ©2006 Transinsight GmbH - www.transinsight.com - All rights reserved.
package gnat.representation;


/**
 * TODO describe me
 *
 */
public class TextRange
{
	private int begin;
	private int end;

	public TextRange(int begin, int end){
		this.begin = begin;
		this.end = end;
	}

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

	@Override
	public boolean equals(Object o)
	{
		TextRange tr = (TextRange)o;
		return (tr.getBegin()==begin && tr.getEnd()==end);
	}
}
