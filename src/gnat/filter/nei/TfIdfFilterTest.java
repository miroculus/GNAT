package gnat.filter.nei;

import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.RecognizedEntity;
import gnat.representation.ScoredName;
import gnat.representation.TextRepository;
import gnat.utils.BiocreativeHelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 	Only for testing!!!
 *
 *	Example filter. Removes gene names with tfidf score below a threshold from the given context.
 *
 */
public class TfIdfFilterTest implements Filter
{
	private double threshold;

	private Map<String, Integer> documentFrequencyMap = new HashMap<String, Integer>();

	private boolean removeGenes = true;

	private Context anotherContext;

	public TfIdfFilterTest(double threshold){
		this(threshold, true);
	}

	public TfIdfFilterTest(double threshold, boolean removeGenes){
		this.threshold = threshold;
		this.removeGenes = removeGenes;
	}

	public TfIdfFilterTest(double threshold, Context anotherContext, boolean removeGenes){
		this.threshold = threshold;
		this.removeGenes = removeGenes;
		this.anotherContext = anotherContext;
	}

	/***/
	public void filter(Context context, TextRepository textRepository, GeneRepository geneRepository)
	{
		int total =  context.getUnidentifiedEntities().size();

		List<RecognizedEntity> toRemove = new LinkedList<RecognizedEntity>();

		Map<String, ScoredName> nameMap = new HashMap<String, ScoredName>();

		Map<RecognizedEntity, float[]> tfDfMap = new HashMap<RecognizedEntity, float[]>();
		Map<RecognizedEntity, Set<String>> geneIds = new HashMap<RecognizedEntity, Set<String>>();

		Iterator<RecognizedEntity> unidentifiedGeneNames = context.getUnidentifiedEntities().iterator();
		while (unidentifiedGeneNames.hasNext())
		{
			RecognizedEntity recognizedGeneName = unidentifiedGeneNames.next();

			Float score = getTFIDF(context, recognizedGeneName);
			Float tf = getTF(context, recognizedGeneName);
			Float df = getDF(context, recognizedGeneName);

			tfDfMap.put(recognizedGeneName, new float[]{tf, df, score});
			geneIds.put(recognizedGeneName, context.getIdCandidates(recognizedGeneName));

			ScoredName scoredName = nameMap.get(recognizedGeneName.getName());
			if(scoredName==null){
				scoredName = new ScoredName(recognizedGeneName.getName());
				nameMap.put(recognizedGeneName.getName(), scoredName);
			}
			scoredName.addScore(score);

//			if (score < threshold && recognizedGeneName.getName().matches(".*[A-Z0-9].*"))
//				System.out.println(recognizedGeneName.getName()+" -> score = "+score);
//			else if (score >= threshold && recognizedGeneName.getName().matches("[a-z\\-\\s]+"))
//				System.out.println(recognizedGeneName.getName()+" -> score = "+score);


			if (score < threshold) {
				toRemove.add(recognizedGeneName);
//				if(recognizedGeneName.getText().getID().equals("8679694")){
//					System.out.println(this.getClass().getName()+" : Throwing out "+recognizedGeneName.getName()+", Score = "+score);
//				}
			}
		}

		if(removeGenes){
			for (RecognizedEntity name : toRemove) {
		        context.removeRecognizedEntity(name);
	        }
			System.out.println(" "+ this.getClass().getSimpleName()+": removed "+toRemove.size()+" names out of "+total);
		}
		else{
			//BiocreativeHelper.writeStatistics(nameMap, "tfidfscores.txt");
			BiocreativeHelper.writeTFDFScores(geneIds, tfDfMap, "gnuplot/tfidfscores.txt", "gnuplot/tfidfthresholdCurve.txt", "data/Human/training.genelist", threshold);
		}
	}

	private float getTF(Context context, RecognizedEntity recognizedGeneName)
    {
		int tf = context.getRecognizedEntitiesHavingNameInText(recognizedGeneName.getName(), recognizedGeneName.getText()).size();
	    return tf;
    }

	private float getDF(Context context, RecognizedEntity recognizedGeneName)
    {
		int df = this.getDocumentFrequency(context, recognizedGeneName.getName());
	    return df;
    }


	private float getTFIDF(Context context, RecognizedEntity recognizedGeneName)
    {
		int tf = context.getRecognizedEntitiesHavingNameInText(recognizedGeneName.getName(), recognizedGeneName.getText()).size();
		int df = this.getDocumentFrequency(context, recognizedGeneName.getName());

//		if(recognizedGeneName.getText().getID().equals("8679694")){
//			System.out.println(this.getClass().getName()+" : "+recognizedGeneName.getName()+"  TF="+tf+", DF="+df);
//		}

	    return tf / (float)df;
    }


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

	public double getThreshold()
    {
    	return threshold;
    }

	public void setThreshold(double i)
    {
    	this.threshold = i;
    }

}
