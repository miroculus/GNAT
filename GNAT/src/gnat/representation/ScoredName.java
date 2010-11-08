// ©2006 Transinsight GmbH - www.transinsight.com - All rights reserved.
package gnat.representation;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO describe me
 *
 */
public class ScoredName
{
	String name;
	List<Float> scores = new ArrayList<Float>();

	public ScoredName(String name){
		this.name = name;
	}

	public void addScore(float score){
		scores.add(score);
	}

	public List<Float> getScores(){
		return scores;
	}

	public float getAverageScore()
    {
	    float avg = 0;
	    for (Float score : scores) {
	        avg += score;
        }
	    return avg/(float)scores.size();
    }
}
