// ©2006 Transinsight GmbH - www.transinsight.com - All rights reserved.
package brics.automaton;


/**
 * TODO describe me
 *
 */
public class NoopActor implements Actor
{
	/**
     *
     */
    private static final long serialVersionUID = 7137457256934070851L;
	private String name="";

	public NoopActor(){

	}

	public NoopActor(String name){
		this.name = name;
	}


	public void act(char[] chars, int matchStartIndex, int matchEndIndex, StringBuffer outputBuffer)
	{
		for(int i=matchStartIndex;i<=matchEndIndex;i++){
			outputBuffer.append(chars[i]);
		}
		System.out.println("NoopActor.act(): My name is "+name+". Text is: '"+outputBuffer+"'");
	}

	/**
	 * Does nothing.
	 */
	public void merge (Actor actor) {}
}
