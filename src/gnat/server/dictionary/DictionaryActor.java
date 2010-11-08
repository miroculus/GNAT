package gnat.server.dictionary;

import gnat.utils.ArrayHelper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import brics.automaton.Actor;


/**
 * An actor for dictionaries to handle matches when parsing strings using an ActionAutomaton. Every match is tagged with an ID.
 *
 * @author cplake
 *
 */
public class DictionaryActor implements Actor
{

	/**
     *
     */
    private static final long serialVersionUID = 6977567139237724509L;

	public static String SEPARATOR = "\n";

	private String idString;


	/**
	 * A new actor for dictionaries using the idString to tag matches.
	 * */
	public DictionaryActor(String idString){
		this.idString = idString;
	}


	/**
	 * Acts on the given match by writing its start and position as well as the actor's identifier to the given output buffer in xml-like format.
	 * */
	public void act(char[] chars, int matchStartIndex, int matchEndIndex, StringBuffer outputBuffer)
    {
		if(outputBuffer.length()>0){
			outputBuffer.append(SEPARATOR);
		}
		outputBuffer.append("<entity ids=\"");
		outputBuffer.append(idString);
		outputBuffer.append("\" startIndex=\"");
		outputBuffer.append(matchStartIndex);
		outputBuffer.append("\" endIndex=\"");
		outputBuffer.append(matchEndIndex);
		outputBuffer.append("\">");
		for(int i=matchStartIndex;i<=matchEndIndex;i++){
			outputBuffer.append(chars[i]);
		}
		outputBuffer.append("</entity>");
    }

	/**
	 *	Merges the specified actor into this actor by merging both id strings delimited by semicolon.
	 */
	public void merge (Actor actor) {
		DictionaryActor dact = (DictionaryActor)actor;
		if(this.idString.equals(dact.idString)){
			return;
		}

		String[] myIDs = idString.split(";");
		String[] otherIDs = dact.idString.split(";");
		Set<String> newIds = new HashSet<String>();
		Collections.addAll(newIds, myIDs);
		Collections.addAll(newIds, otherIDs);
		idString = ArrayHelper.joinStringArray(ArrayHelper.set2StringArray(newIds), ";");
	}
}
