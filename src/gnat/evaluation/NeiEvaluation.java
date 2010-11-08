package gnat.evaluation;

import gnat.representation.Context;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Offers to run the BioCreative2 GN task python script to evaluate a context of identified genes.
 *
 */
public class NeiEvaluation
{


	/**
	 * Returns an array of length 3 with f-measure at index 0, precision at index 1, and recall at index 2.
	 *
	 * @throws IOException */
	public static float[] evaluate (String scoringScript, Context predictedContext, String goldList, String predictionFile) throws IOException{
		predictedContext.toIdentifiedGeneListInBioCreativeFormat(predictionFile);
		return evaluate(scoringScript, predictionFile, goldList);
	}

	/**
	 * Returns an array of length 3 with f-measure at index 0, precision at index 1, and recall at index 2.
	 *
	 * @throws IOException */
	public static float[] evaluate_perlscript (String scoringScript, Context predictedContext, String goldList, String predictionFile) throws IOException{
		predictedContext.toIdentifiedGeneListInBioCreativeFormat(predictionFile);
		return evaluate_perlscript(scoringScript, predictionFile, goldList);
	}


	/**
	 * Runs the BioCreative 2 GN task python evaluation script and returns an array of length 3 with f-measure at index 0, precision at index 1, and recall at index 2.
	 *
	 * @throws IOException */
	public static float[] evaluate(String scoringScript, String testFile, String goldFile) throws IOException{
		float[] results = new float[3];

		Process process = Runtime.getRuntime().exec("python " + scoringScript + " "+goldFile + " " + testFile+"");

		InputStreamThread errorStreamThread = new InputStreamThread(process.getErrorStream());
		errorStreamThread.start();

		InputStreamThread inputStreamThread = new InputStreamThread(process.getInputStream());
		inputStreamThread.start();
	    while(!inputStreamThread.ready);

	    BufferedReader reader = new BufferedReader(new StringReader(inputStreamThread.getInput()));
	    String line = reader.readLine();
	    while(!line.startsWith("F-Measure"))
	    	line = reader.readLine();

		String[] parts = line.split(" ");
		results[0] = Float.parseFloat(parts[parts.length-1]);

		while(!line.startsWith("Precision"))
	    	line = reader.readLine();
		parts = line.split(" ");
		results[1] = Float.parseFloat(parts[parts.length-1]);

		while(!line.startsWith("Recall"))
	    	line = reader.readLine();
		parts = line.split(" ");
		results[2] = Float.parseFloat(parts[parts.length-1]);

		return results;
	}

	/**
	 * Runs the BioCreative 2 GN task python evaluation script and returns an array of length 3 with f-measure at index 0, precision at index 1, and recall at index 2.
	 *
	 * @throws IOException */
	public static float[] evaluate_perlscript(String scoringScript, String testFile, String goldFile) throws IOException{
		float[] results = new float[3];

		Process process = Runtime.getRuntime().exec("perl " + scoringScript + " "+goldFile + " " + testFile+"");

		InputStreamThread errorStreamThread = new InputStreamThread(process.getErrorStream());
		errorStreamThread.start();

		InputStreamThread inputStreamThread = new InputStreamThread(process.getInputStream());
		inputStreamThread.start();
	    while(!inputStreamThread.ready);

	    BufferedReader reader = new BufferedReader(new StringReader(inputStreamThread.getInput()));
	    String line = reader.readLine();
	    while(!line.startsWith("Total Precision"))
	    	line = reader.readLine();

		System.out.println(line);

		return results;
	}


	/**
	 * Returns an array of length 3 with f-measure at index 0, precision at index 1, and recall at index 2.
	 *
	 * @throws IOException */
	public static double[] evaluate (Context predictedContext, String goldList) throws IOException{
		Map<String, Set<String>> predictedGeneMap = predictedContext.toIdentifiedGeneMap();
		Map<String, Set<String>> goldGeneMap = goldListToMap(goldList);

		FileWriter tpWriter = new FileWriter("tps.txt");


		double tp = 0;
		double fp = 0;
		double fn = 0;

		for (String pmId : predictedGeneMap.keySet()) {
			Set<String> predictedGenes = predictedGeneMap.get(pmId);
			Set<String> goldGenes = goldGeneMap.get(pmId);
			if(goldGenes==null){
				goldGenes = new HashSet<String>();
			}

			tpWriter.write(pmId);

			for (String predictedGeneId : predictedGenes) {
				if(goldGenes.contains(predictedGeneId)){
					tp++;
					tpWriter.write("\t"+predictedGeneId);
				}else{
					fp++;
				}
            }

			tpWriter.write("\n");
        }

		for (String pmId : goldGeneMap.keySet()) {
			Set<String> goldGenes = goldGeneMap.get(pmId);
			Set<String> predictedGenes = predictedGeneMap.get(pmId);
			if(predictedGenes==null){
				predictedGenes = new HashSet<String>();
			}
			for (String goldGeneId : goldGenes) {
				if(!predictedGenes.contains(goldGeneId)){
					fn++;
				}
            }
        }

		tpWriter.close();

//		System.out.println("Evaluation: TPs = "+tp);
//		System.out.println("Evaluation: FPs = "+fp);
//		System.out.println("Evaluation: FNs = "+fn);

		double pre = tp / (tp+fp);
		double rec = tp / (tp+fn);
		double fm = 2*pre*rec / (pre+rec);

//		System.out.println("Evaluation: precision = "+pre);
//		System.out.println("Evaluation: recall = "+rec);
//		System.out.println("Evaluation: fmeasure = "+fm);

		return new double[]{fm, pre, rec};
	}


	/**
	 * Returns a mapping from text IDs to sets of gene IDs that are correct according to the gold list file.
	 *
	 * @throws IOException */
	public static Map<String, Set<String>> getTruePositives(Context predictedContext, String goldList) throws IOException
    {
		Map<String, Set<String>> predictedGeneMap = predictedContext.toIdentifiedGeneMap();
		Map<String, Set<String>> goldGeneMap = goldListToMap(goldList);

		Map<String, Set<String>> truePositives = new HashMap<String, Set<String>>();

		for (String pmId : predictedGeneMap.keySet()) {
			Set<String> predictedGenes = predictedGeneMap.get(pmId);
			Set<String> goldGenes = goldGeneMap.get(pmId);
			if(goldGenes==null){
				goldGenes = new HashSet<String>();
			}

			for (String predictedGeneId : predictedGenes) {
				if(goldGenes.contains(predictedGeneId)){
					Set<String> trues = truePositives.get(pmId);
					if(trues==null){
						trues = new HashSet<String>();
						truePositives.put(pmId, trues);
					}
					trues.add(predictedGeneId);
				}
            }

        }

		return truePositives;
    }

	/**
	 * Returns an array of length 3 with f-measure at index 0, precision at index 1, and recall at index 2.
	 *
	 * @throws IOException */
	public static double[] evaluateSingleText (Context predictedContext, String pmId, String goldList) throws IOException{
		Map<String, Set<String>> predictedGeneMap = predictedContext.toIdentifiedGeneMap();
		Map<String, Set<String>> goldGeneMap = goldListToMap(goldList);

		double tp = 0;
		double fp = 0;
		double fn = 0;

		Set<String> predictedGenes = predictedGeneMap.get(pmId);
		Set<String> goldGenes = goldGeneMap.get(pmId);
		if(goldGenes==null){
			goldGenes = new HashSet<String>();
		}

		for (String predictedGeneId : predictedGenes) {
			if(goldGenes.contains(predictedGeneId)){
				tp++;
			}else{
				fp++;
			}
        }

		if (predictedGenes != null)
			for (String goldGeneId : goldGenes)
				if(!predictedGenes.contains(goldGeneId))
					fn++;

//		double pre = tp / (tp+fp);
//		double rec = tp / (tp+fn);
//		double fm = 2*pre*rec / (pre+rec);
//
//		System.out.println("Evaluation: precision = "+pre);
//		System.out.println("Evaluation: recall = "+rec);
//		System.out.println("Evaluation: fmeasure = "+fm);

		//return new double[]{fm, pre, rec};
		return new double[]{tp, fp, fn};
	}

	/***/
	private static Map<String, Set<String>> goldListToMap(String goldList) throws IOException
    {
		Map<String, Set<String>> geneMap = new HashMap<String, Set<String>>();
	    BufferedReader reader = new BufferedReader(new FileReader(goldList));
	    String line = null;
	    while((line=reader.readLine())!=null){
	    	String[] parts = line.split("\t");
	    	String pmId = parts[0];
	    	String geneId = parts[1];
	    	Set<String> genesInText = geneMap.get(pmId);
	    	if(genesInText==null){
	    		genesInText = new HashSet<String>();
	    		geneMap.put(pmId, genesInText);
	    	}
	    	genesInText.add(geneId);
	    }
	    reader.close();
	    return geneMap;
    }

	/***/
	private static class InputStreamThread extends Thread{
		BufferedReader bufferedReader;
		StringBuffer stringBuffer = new StringBuffer();
		boolean ready = false;

		public InputStreamThread(InputStream inputStream){
			bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
		}

		public void run(){
			String line;
			try {
	            while((line=bufferedReader.readLine())!=null){
	            	stringBuffer.append(line+"\n");
	            	System.out.println(line);
	            }
            }
            catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
            ready = true;
		}

		public String getInput(){
			return stringBuffer.toString();
		}
	}

}
