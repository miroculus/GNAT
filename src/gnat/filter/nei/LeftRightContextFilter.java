package gnat.filter.nei;

import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.RecognizedEntity;
import gnat.representation.TextPositionModel;
import gnat.representation.TextRepository;
import gnat.utils.LeftRightContextHelper;
import gnat.utils.MathHelper;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Filters out names by looking at tokens to the left and right of it and comparing their occurrence 
 * against occurrences seen in a training set for TPs and FPs.
 *
 * @author Conrad
 */
public class LeftRightContextFilter implements Filter {

	private TextPositionModel strictFPsModel;
	private TextPositionModel nonStrictFPsModel;

	private double logLikelihoodRatioThreshold;

	private int leftContextLength;
	private int rightContextLength;

	List<Double> observedScores = new LinkedList<Double>();

	int totalFPLeftFrequency;
	int totalFPRightFrequency;
	int totalFPMiddleFrequency;


	int totalTPLeftFrequency;
	int totalTPRightFrequency;
	int totalTPMiddleFrequency;


	/**
	 * 
	 * @param strictFpsModelFile - model file for strict false positives
	 * @param nonStrictFPsModelFile - model file for non-strict true positives
	 * @param logLikelihoodRatioThreshold - threshold for the log likelihood
	 * @param leftContextLength - consider x tokens to the left
	 * @param rightContextLength - consider x tokens to the right
	 */
	public LeftRightContextFilter (
			String strictFpsModelFile, String nonStrictFPsModelFile,
			double logLikelihoodRatioThreshold,
			int leftContextLength, int rightContextLength) {
		
		strictFPsModel = TextPositionModel.loadFromFile(strictFpsModelFile);
		nonStrictFPsModel = TextPositionModel.loadFromFile(nonStrictFPsModelFile);
		this.logLikelihoodRatioThreshold = logLikelihoodRatioThreshold;

		this.leftContextLength = leftContextLength;
		this.rightContextLength = rightContextLength;

		totalFPLeftFrequency = 0;
		Set<String> leftTokens = strictFPsModel.getTokensAtPosition(0);
		for (String string : leftTokens) {
			totalFPLeftFrequency += strictFPsModel.getFrequency(string, 0);
        }
		totalFPRightFrequency = 0;
		Set<String> rightTokens = strictFPsModel.getTokensAtPosition(2);
		for (String string : rightTokens) {
			totalFPRightFrequency += strictFPsModel.getFrequency(string, 2);
        }
		totalFPMiddleFrequency = 0;
		Set<String> nameTokens = strictFPsModel.getTokensAtPosition(1);
		for (String string : nameTokens) {
			totalFPMiddleFrequency += strictFPsModel.getFrequency(string, 1);
        }

		totalTPLeftFrequency = 0;
		leftTokens = nonStrictFPsModel.getTokensAtPosition(0);
		for (String string : leftTokens) {
			totalTPLeftFrequency += nonStrictFPsModel.getFrequency(string, 0);
        }
		totalTPRightFrequency = 0;
		rightTokens = nonStrictFPsModel.getTokensAtPosition(2);
		for (String string : rightTokens) {
			totalTPRightFrequency += nonStrictFPsModel.getFrequency(string, 2);
        }
		totalTPMiddleFrequency = 0;
		nameTokens = nonStrictFPsModel.getTokensAtPosition(1);
		for (String string : nameTokens) {
			totalTPMiddleFrequency += nonStrictFPsModel.getFrequency(string, 1);
        }

	}

	/**
	 * 
	 */
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {
		for (RecognizedEntity recognizedEntity : context.getUnidentifiedEntities()) {
	        String sentence = recognizedEntity.getText().getSentenceAround(recognizedEntity.getBegin()).trim();
	        int sentenceBegin = recognizedEntity.getText().getPlainText().indexOf(sentence);
	        int sentenceEnd = sentenceBegin + sentence.length();

	        if(sentenceBegin==-1){
	        	throw new RuntimeException("Sentence not found in "+recognizedEntity.getText().getPMID()+": '"+sentence+"'");
	        }

	        if(recognizedEntity.getBegin()==-1){
	        	throw new RuntimeException("Invalid text range for name "+recognizedEntity.getName() + " in text "+recognizedEntity.getText().getID());
	        }

	        if(recognizedEntity.getBegin() < sentenceBegin){
	        	throw new RuntimeException("Begin index for name "+recognizedEntity.getName()+ " ("+recognizedEntity.getBegin()+")" + " in text "+recognizedEntity.getText().getID() + " less than for sentence '"+sentence+"' ("+sentenceBegin+")");

	        }

	        String[] leftTokens  = LeftRightContextHelper.getLeftContext(recognizedEntity.getText().getPlainText(), sentenceBegin, recognizedEntity.getBegin(), leftContextLength);
	        String[] rightTokens = LeftRightContextHelper.getRightContext(recognizedEntity.getText().getPlainText(), sentenceEnd, recognizedEntity.getEnd(), rightContextLength);
	        String[] nameTokens = LeftRightContextHelper.getTokens(recognizedEntity.getName());

	        double fpScore = strictFPsModel.getJointProbability(leftTokens, 0, totalFPLeftFrequency);
	        fpScore *= strictFPsModel.getJointProbability(rightTokens, 2, totalFPRightFrequency);
	        fpScore *= strictFPsModel.getJointProbability(nameTokens, 1, totalFPMiddleFrequency);

	        double tpScore = nonStrictFPsModel.getJointProbability(leftTokens, 0, totalTPLeftFrequency);
	        tpScore *= nonStrictFPsModel.getJointProbability(rightTokens, 2, totalTPRightFrequency);
	        tpScore *= nonStrictFPsModel.getJointProbability(nameTokens, 1, totalTPMiddleFrequency);

	        //double logLikelihoodRatio = Math.log(strictScore/nonStrictScore);
//	        if(logLikelihoodRatio > logLikelihoodRatioThreshold){
//	        	context.removeRecognizedEntity(recognizedEntity);
//	        }
	        if(fpScore > tpScore){
	        	context.removeRecognizedEntity(recognizedEntity);
//	        	System.out.println("LeftRightContextFilter: removing "+recognizedEntity.getName()+" at pos "+recognizedEntity.getBegin()+". RightContext="+StringHelper.joinStringArray(rightTokens, " ")+", FPProb="+strictFPsModel.getJointProbability(rightTokens, 2)+", TPProb="+nonStrictFPsModel.getJointProbability(rightTokens, 2)+", fpScore="+fpScore+", tpScore="+tpScore);
//	        	for (String string : rightTokens) {
//	        		System.out.println("Seen in "+string+" FP Model: "+strictFPsModel.seenBefore(string, 2)+", Freq="+strictFPsModel.getFrequency(string, 2)+", relFreq="+strictFPsModel.getRelativeFrequency(string, 2));
//	        		System.out.println("Seen in "+string+" TP Model: "+nonStrictFPsModel.seenBefore(string, 2)+", Freq="+nonStrictFPsModel.getFrequency(string, 2)+", relFreq="+nonStrictFPsModel.getRelativeFrequency(string, 2));
//                }
	        }else{
//	        	System.out.println("LeftRightContextFilter: keeping "+recognizedEntity.getName()+" at pos "+recognizedEntity.getBegin()+". RightContext="+StringHelper.joinStringArray(rightTokens, " ")+", FPProb="+strictFPsModel.getJointProbability(rightTokens, 2)+", TPProb="+nonStrictFPsModel.getJointProbability(rightTokens, 2)+", fpScore="+fpScore+", tpScore="+tpScore);
//	        	System.out.println("RIGHT CONTEXT");
//	        	for (String string : rightTokens) {
//	        		System.out.println("Seen in "+string+" FP Model: "+strictFPsModel.seenBefore(string, 2)+", Freq="+strictFPsModel.getFrequency(string, 2)+", relFreq="+strictFPsModel.getRelativeFrequency(string, 2));
//	        		System.out.println("Seen in "+string+" TP Model: "+nonStrictFPsModel.seenBefore(string, 2)+", Freq="+nonStrictFPsModel.getFrequency(string, 2)+", relFreq="+nonStrictFPsModel.getRelativeFrequency(string, 2));
//                }
//	        	System.out.println("LEFT CONTEXT");
//	        	for (String string : leftTokens) {
//	        		System.out.println("Seen in "+string+" FP Model: "+strictFPsModel.seenBefore(string, 0)+", Freq="+strictFPsModel.getFrequency(string, 0)+", relFreq="+strictFPsModel.getRelativeFrequency(string, 0));
//	        		System.out.println("Seen in "+string+" TP Model: "+nonStrictFPsModel.seenBefore(string, 0)+", Freq="+nonStrictFPsModel.getFrequency(string, 0)+", relFreq="+nonStrictFPsModel.getRelativeFrequency(string, 0));
//                }
	        }
	        //observedScores.add(logLikelihoodRatio);
        }
	}

	
	public void showMeanScore(){
		System.out.println(MathHelper.mean(observedScores));
	}

}