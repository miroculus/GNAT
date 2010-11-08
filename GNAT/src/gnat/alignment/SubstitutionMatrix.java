package gnat.alignment;

/**
 * Interface for any substitution matrix.
 * 
 * @author Joerg Hakenberg
 *
 */

public abstract class SubstitutionMatrix {

	public abstract float getScore (Word original, Word replacement);
	
}
