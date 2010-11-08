// ©2006 Transinsight GmbH - www.transinsight.com - All rights reserved.
package brics.automaton;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * Expansion of the RunAutomaton that offers a run method where actors write to an output buffer.
 *
 */
@SuppressWarnings("serial")
public class ActionAutomaton extends RunAutomaton
{
	/**
	 * See constructor in RunAutomaton.
	 * */
	public ActionAutomaton(Automaton automaton)
    {
	    super(automaton);
    }

	/**
	 * See constructor in RunAutomaton.
	 * */
	public ActionAutomaton(Automaton automaton, boolean tableize)
    {
	    super(automaton, tableize);
    }

	/**
	 * Calls super constructor and adds the actor to all accept states of this automtaton.
	 * */
	public ActionAutomaton(Automaton automaton, boolean tableize, Actor actor)
    {
	    super(automaton, tableize);
	    Set<State> acceptStates = automaton.getAcceptStates();
	    for (State state : acceptStates) {
	        state.setActor(actor);
        }
    }

	/**
	 * Parses the given string s. All actors write to the output buffer.
	 * */
	public boolean run(String s, StringBuffer actorOutput, StartAndEndOfTokenDeterminer tokenDeterminer){
		boolean match = false;

		List<Waypoint> waypoints = new LinkedList<Waypoint>();
		char[] chars = s.toCharArray();
		int currentState = super.getInitialState();
		StringBuffer currentMatch = new StringBuffer();
		int startAt = 0;
		for (int i=startAt;i<chars.length;i++) {

			if(currentMatch.length()==0 && !tokenDeterminer.startOfToken(chars, i)){	// dont start parsing in the middle of a token
				while(i<chars.length && !tokenDeterminer.startOfToken(chars, i) ){
					i++;
				}
				startAt = i;
				if(i==chars.length){
					return match;
				}
			}

			char c = chars[i];
	        int nextState = super.step(currentState, c);
	        //System.out.println("i:"+i+" c="+c+", nextState:"+nextState+" , matchLength="+currentMatch.length());

	        if(nextState==-1)	// dead end
	        {
	        	i = startAt;
	        	startAt++;

	        	// any waypoints reached?
	        	if(waypoints.size()>0)
	        	{
	        		Waypoint lastWaypoint = waypoints.get(waypoints.size()-1);
	        		State state = super.getState(lastWaypoint.getAcceptState());
	        		if(state.getActor()!=null){
	        			state.getActor().act(chars, lastWaypoint.getCharIndexFrom(), lastWaypoint.getCharIndexTo(), actorOutput);
	        		}
	        		startAt = lastWaypoint.getCharIndexTo();
	        		i = startAt;
	        	}

		       	currentState = super.getInitialState();
		       	currentMatch.setLength(0);
		       	waypoints.clear();
	        }
	        else
	        {
	        	currentMatch.append(c);
	        	currentState = nextState;
	        	if(super.isAccept(currentState) && tokenDeterminer.endOfToken(chars, i) )// end of token reached?
	        	{
	        		//System.out.println("match!");
	        		int charIndexFrom = i-currentMatch.length() + 1;
	        		int charIndexTo = i;
	        		Waypoint waypoint = new Waypoint(currentState, charIndexFrom, charIndexTo);
	        		waypoints.add(waypoint);
	        		match = true;
	        		startAt = i;
	        	}
	        }
        }

		// anything left?
		if(waypoints.size()>0){
			Waypoint lastWaypoint = waypoints.get(waypoints.size()-1);
    		State state = super.getState(lastWaypoint.getAcceptState());
    		if(state.getActor()!=null){
    			state.getActor().act(chars, lastWaypoint.getCharIndexFrom(), lastWaypoint.getCharIndexTo(), actorOutput);
    		}
		}

		return match;
	}


	/**
	 * Class for storing information on matching substrings when performing method run.
	 * */
	private class Waypoint{
		int acceptState;
		int charIndexFrom;
		int charIndexTo;
		public Waypoint(int acceptState, int charIndexFrom, int charIndexTo)
        {
	        super();
	        this.acceptState = acceptState;
	        this.charIndexFrom = charIndexFrom;
	        this.charIndexTo = charIndexTo;
        }
		public int getAcceptState()
        {
        	return acceptState;
        }

		public int getCharIndexFrom()
        {
        	return charIndexFrom;
        }

		public int getCharIndexTo()
        {
        	return charIndexTo;
        }
	}

}