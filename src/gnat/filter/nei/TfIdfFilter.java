package gnat.filter.nei;

import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.RecognizedEntity;
import gnat.representation.ScoredName;
import gnat.representation.TextRepository;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *	This filter removes all entities with a TF*IDF score below a threshold.
 *
 */
public class TfIdfFilter implements Filter {
	private double threshold;

	private Map<String, Integer> documentFrequencyMap = new HashMap<String, Integer>();

	private Context anotherContext;


	/**
	 *	Creates a new Filter that removes names whose tfidf value is below the given threshold.
	 * */
	public TfIdfFilter(double threshold){
		this.threshold = threshold;
	}


	/**
	 *	Creates a new Filter that removes names whose tfidf value is below the given threshold.
	 * 	DF values are also counted by looking into another context.
	 * */
	public TfIdfFilter (double threshold, Context anotherContext){
		this.threshold = threshold;
		this.anotherContext = anotherContext;
	}


	/**
	 *	Removes all entities with a tfidf score below a threshold from the given context.
	 * */
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {
		
		int total =  context.getUnidentifiedEntities().size();

		List<RecognizedEntity> toRemove = new LinkedList<RecognizedEntity>();

		Map<String, ScoredName> nameMap = new HashMap<String, ScoredName>();

		Iterator<RecognizedEntity> unidentifiedGeneNames = context.getUnidentifiedEntities().iterator();
		while (unidentifiedGeneNames.hasNext())
		{
			RecognizedEntity recognizedGeneName = unidentifiedGeneNames.next();
			Float score = getTFIDF(context, recognizedGeneName);

			ScoredName scoredName = nameMap.get(recognizedGeneName.getName());
			if(scoredName==null){
				scoredName = new ScoredName(recognizedGeneName.getName());
				nameMap.put(recognizedGeneName.getName(), scoredName);
			}
			scoredName.addScore(score);

			if (score < threshold) {
				toRemove.add(recognizedGeneName);
			}
		}

		for (RecognizedEntity name : toRemove) {
	        context.removeRecognizedEntity(name);
        }
		System.out.println(" "+ this.getClass().getSimpleName()+": removed "+toRemove.size()+" names out of "+total);
	}


	/**
	 *
	 * Returns the TFIDF score for an entity in a context.
	 * */
	private float getTFIDF(Context context, RecognizedEntity recognizedEntity)
    {
		int tf = context.getRecognizedEntitiesHavingNameInText(recognizedEntity.getName(), recognizedEntity.getText()).size();
		int df = this.getDocumentFrequency(context, recognizedEntity.getName());

	    return tf / (float)df;
    }


	/**
	 * Returns the document (overall) frequency of a name in this context.
	 * */
	private int getDocumentFrequency(Context context, String name)
	{
		Integer cachedDf = documentFrequencyMap.get(name);
		if(cachedDf==null){
			cachedDf = context.getRecognizedEntitiesHavingName(name).size();
			if(anotherContext!=null){
				cachedDf += anotherContext.getRecognizedEntitiesHavingName(name).size();
			}
			documentFrequencyMap.put(name, cachedDf);
		}

		return cachedDf;
	}


	/**
	 *
	 * The threshold.
	 * */
	public double getThreshold()
    {
    	return threshold;
    }

	/**
	 *
	 * The threshold.
	 * */
	public void setThreshold(double i)
    {
    	this.threshold = i;
    }

}
