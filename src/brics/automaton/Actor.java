package brics.automaton;

import java.io.Serializable;

/**
 * An actor handles matches when parsing strings using an ActionAutomaton.
 *
 * @author cplake
 *
 */
public interface Actor extends Serializable
{
	public void act(char[] chars, int matchStartIndex, int matchEndIndex, StringBuffer outputBuffer);

	public void merge(Actor actor);
}
