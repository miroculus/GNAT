// ©2006 Transinsight GmbH - www.transinsight.com - All rights reserved.
package gnat.representation;


/**
 * TODO describe me
 *
 */
/**
 *
 */
public class ScoredGene implements Comparable<ScoredGene> {

	Float score;
	Gene gene;

	public ScoredGene(Float score, Gene gene){
		this.score = score;
		this.gene = gene;
	}

	public int compareTo(ScoredGene aScoredGene)
    {
        return score.compareTo(aScoredGene.score);
    }

	public Gene getGene()
    {
    	return gene;
    }

	public Float getScore()
    {
    	return score;
    }

}

