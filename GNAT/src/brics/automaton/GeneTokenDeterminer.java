package brics.automaton;


/**
 * Determines whether a token can potentially refer to a gene name,
 * by making sure that the characters to the left and right are tokens
 * that appear before/after valid gene names.
 * <br><br>
 * As an example, a gene name should be surrounded by white spaces or
 * punctuation, but a gene name cannot appear inside other, larger tokens.
 * 
 * @author Conrad
 *
 */
public class GeneTokenDeterminer implements StartAndEndOfTokenDeterminer
{

	/**
	 * Checks whether the character at position <tt>index</tt> could mark the
	 * end (=last character) of a gene name, given the string in <tt>chars</tt>.
	 * <tt>chars</tt> is a piece of text containing the suspected gene name.
	 * @chars
	 * @index
	 */
	public boolean endOfToken(char[] chars, int index) {
		boolean endOfToken = false;
		
		// gene names can end a sentence (if a sentence end mark is missing).
		if(index==chars.length-1) {
			endOfToken = true;  // end of text -> end of token
		}
		
		// 
		else if (index<chars.length-1) {
			// gene names should be followed by certain punctuation
			if (chars[index+1]==' '  || chars[index+1]==',' || chars[index+1]=='.' || chars[index+1]==':'
				|| chars[index+1]==';' || chars[index+1]==')' || chars[index+1]=='/'
				|| chars[index+1]=='-' || chars[index+1]=='+'
					
				|| (chars[index+1]=='(')  // "KRAS(G12D)"
				//|| (chars[index+1]=='(' && chars[index+2]=='+')
				)
			{
				endOfToken = true;
			}
		}
		
		return endOfToken;
	}


	/**
	 * Checks whether the character at position <tt>index</tt> could mark the
	 * beginning (=first character) of a gene name, given the string in <tt>chars</tt>.
	 * <tt>chars</tt> is a piece of text containing the suspected gene name.
	 * @chars
	 * @index
	 */
	public boolean startOfToken(char[] chars, int index) {
		// gene names cannot start with white spaces or some other punctuation
		if(chars[index]==' ' || chars[index]==':' || chars[index]=='(' || chars[index]=='/')
			return false;	// tokens do not start with token delimiters

		// gene names should be preceded by certain punctuation
		if(index>0 && (chars[index-1]==' ' || chars[index-1]==':' || chars[index-1]=='(' || chars[index-1]== '/'
			 || chars[index-1]== '-'))
			return true;	// token delimiter precedes the index
		
		// gene names can start a sentence etc.
		if(index==0)
			return true;	// start of text and nothing matches above

		return false;
	}

}
