package gnat.representation;


/**
 * Represents an identified gene, i.e. a recognized entity associated with a gene.
 * */
public class IdentifiedGene implements Comparable<IdentifiedGene>
{
	private RecognizedEntity recognizedEntity;
	private Gene gene;
	private Float confidenceScore;

	/**
	 * Creates a new identified gene from a recognized entity and a gene.
	 * The confidence value is set to 1.0f by default.
	 * */
	public IdentifiedGene(RecognizedEntity recognizedEntity, Gene gene)
    {
		this(recognizedEntity, gene, 1.0f);
    }


	/**
	 * Creates a new identified gene from a recognized entity and a gene together with a confidence score.
	 * */
	public IdentifiedGene(RecognizedEntity recognizedEntity, Gene gene, Float score)
    {
		this.recognizedEntity = recognizedEntity;
	    this.gene = gene;
	    this.confidenceScore = score;
    }

	/**
	 * Returns the gene.
	 * */
	public Gene getGene()
    {
	    return gene;
    }

	/**
	 * Returns the confidence score.
	 * */
	public Float getConfidenceScore()
	{
		return confidenceScore;
	}

	/**
	 * Returns the originally recognized entity.
	 * */
	public RecognizedEntity getRecognizedEntity()
	{
		return recognizedEntity;
	}

	/**
	 * Returns the gene name.
	 * */
	public String getName(){
		return recognizedEntity.getName();
	}
	
	
	@Override public boolean equals (Object o) {
		if (!(o instanceof IdentifiedGene)) return false;
		IdentifiedGene i = (IdentifiedGene)o;
		return this.compareTo(i) == 0;
	}
	
		
	public int compareTo (IdentifiedGene o) {
		IdentifiedGene other = (IdentifiedGene)o;
		if (!this.getGene().getID().equals(other.getGene().getID()))
			return this.getGene().getID().compareTo(other.getGene().getID());
		
		if (!this.getName().equals(other.getName()))
			return this.getName().compareTo(other.getName());
		
		if (this.getRecognizedEntity().getBegin() != other.getRecognizedEntity().getBegin())
			return this.getRecognizedEntity().getBegin() - other.getRecognizedEntity().getBegin();
		
		return 0;//this.hashCode() - o.hashCode();
	}

	@Override public String toString () {
		return this.gene.getID() + " (" + this.gene.getNames() + ")";
	}
	
}
