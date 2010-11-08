package brics.automaton;

public interface StartAndEndOfTokenDeterminer
{

	/**
	 * Returns true if the index points to a postion where a token begins.
	 * */
	public boolean startOfToken(char[] chars, int index);

	/**
	 * Returns true if the index points to the end of a token.
	 *
	 * */
	public boolean endOfToken(char[] chars, int index);
}
