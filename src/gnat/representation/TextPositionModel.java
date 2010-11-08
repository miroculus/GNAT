package gnat.representation;

import gnat.utils.FileHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores frequencies of tokens at different positions.
 * A position can be defined arbitrary, e.g. 0 denotes left, 1 denotes middle, and 2 denotes right
 *
 * Comment: does a Laplace correction (adding one) for unseen events/strings
 * 
 * @author Conrad
 * */
public class TextPositionModel implements Serializable
{

	/**
    *	A position model for text.
    *	Stores which tokens occur at which positions and their frequency at this position.
    */
	private static final long serialVersionUID = -2135944868878778606L;

	private int positions;


	// stores which tokens occur at which positions and their frequency at this position.
	private ArrayList<Map<String, Integer>> stringFrequenciesAtPosition;


	/**
	 * @param positions the number of total positions in a text where tokens can occurr.
	 * */
	public TextPositionModel(int positions){
		this.positions = positions;
		stringFrequenciesAtPosition = new ArrayList< Map<String,Integer> >(positions);
		for(int i=0;i<positions;i++){
			stringFrequenciesAtPosition.add(new HashMap<String, Integer>());
		}
	}


	/**
	 * Adds one to the frequency for this string at the given position.
	 * */
	public void increaseFrequencyForStringAtPosition(String string, int position){
		Integer frequency = stringFrequenciesAtPosition.get(position).get(string);
		if(frequency==null){
			frequency = new Integer(1);	// add one to every event (Laplace correction)
		}
		frequency++;
		stringFrequenciesAtPosition.get(position).put(string, frequency);
	}



	/**
	 * Returns true if this token was never observed before at the specified position.
	 * */
	public boolean seenBefore(String token, int position){
		return stringFrequenciesAtPosition.get(position).get(token)!=null;
	}


	/**
	 * Returns how often this string occurs at the given position.
	 * The minimum frequence is 1, so that unseen events have probability > 0 (Laplace correction)
	 * */
	public int getFrequency(String string, int position){
		Integer freq = stringFrequenciesAtPosition.get(position).get(string);
		if(freq==null){
			freq = new Integer(1);
		}
		return freq;
	}

	/**
	 * Returns the frequency for a string at a position normalized by the frequency for all tokens at this position.
	 * */
	public double getRelativeFrequency(String string, int position){
		int totalFrequency = 0;
		Map<String, Integer> stringFrequencies = stringFrequenciesAtPosition.get(position);
		for (String s : stringFrequencies.keySet()) {
			totalFrequency += getFrequency(s, position);
        }
		int frequency  = getFrequency(string, position);
		return (double) (frequency / (double) totalFrequency);
	}


	/**
	 * Returns the frequency for a string at a position normalized by the frequency for all tokens at this position.
	 * */
	public double getRelativeFrequency(String string, int position, int totalFrequencyAtPosition){
		int frequency  = getFrequency(string, position);
		return (double) (frequency / (double) totalFrequencyAtPosition);
	}


	/**
	 * Returns the probability of this sequence represented by this model.
	 * Note that the array length L should match the number of positions N in this model.
	 * If L is greater than N, only the first N elements are processed.
	 * If L is less than N, the complete array is processed without looking at the last N-L positions in this model.
	 * */
	public double getSequenceProbability(String[] strings){
		double logScore = 0;
		for(int i=0; i<strings.length && i<positions; i++){
			double relFreq = getRelativeFrequency(strings[i], i);
			logScore += Math.log(relFreq);
        }
		return Math.pow(Math.E, logScore);
	}

	/**
	 * Returns the joint probability that these strings occur at position i.
	 * */
	public double getJointProbability(Set<String> strings, int position){
		double logScore = 0;
		for(String s : strings){
			double relFreq = getRelativeFrequency(s, position);
			logScore += Math.log(relFreq);
        }
		return Math.pow(Math.E, logScore);
	}

	/**
	 * Returns the joint probability that these strings occur at position i.
	 * */
	public double getJointProbability(String[] strings, int position){
		double logScore = 0;
		for(String s : strings){
			double relFreq = getRelativeFrequency(s, position);
			logScore += Math.log(relFreq);
        }
		return Math.pow(Math.E, logScore);
	}

	/**
	 * Returns the joint probability that these strings occur at position i.
	 * */
	public double getJointProbability(String[] strings, int position, int totalFrequencyAtPosition){
		double logScore = 0;
		for(String s : strings){
			double relFreq = getRelativeFrequency(s, position, totalFrequencyAtPosition);
			logScore += Math.log(relFreq);
        }
		return Math.pow(Math.E, logScore);
	}

	/**
	 * Returns the max. probability of one the strings occuring at position i.
	 * */
	public double getSupremumProbability(Set<String> strings, int position){
		double sup = 0;
		for(String s : strings){
			double relFreq = getRelativeFrequency(s, position);
			sup = Math.max(sup, relFreq);
        }
		return sup;
	}

	/**
	 * Returns the max. probability of one the strings occurring at position i.
	 * */
	public double getSupremumProbability(String[] strings, int position){
		double sup = 0;
		for(String s : strings){
			double relFreq = getRelativeFrequency(s, position);
			sup = Math.max(sup, relFreq);
        }
		return sup;
	}


	/**
	 * Writes an object of this class to a file.
	 * */
	public static void writeToFile(TextPositionModel textPositionModel, String filename){
		FileHelper.writeObjectToFile(textPositionModel, new File(filename));
	}

	/**
	 * Loads an object of this class from file.
	 * */
	public static TextPositionModel loadFromFile(String filename){
		return (TextPositionModel) FileHelper.readObjectFromFile(new File(filename));
	}


	/**
	 * Returns a list of tokens that occur at the given position sorted by their frequency.
	 * */
	public List<StringIntegerPair> getTokensSortedByFrequency(int position){
		Set<Map.Entry<String, Integer>> mapEntries = stringFrequenciesAtPosition.get(position).entrySet();

		List<StringIntegerPair> entryList = new ArrayList<StringIntegerPair>(mapEntries.size());

		for (Map.Entry<String, Integer> entry : mapEntries) {
			entryList.add(new StringIntegerPair(entry.getKey(), entry.getValue()));
        }

		Collections.sort(entryList);
		Collections.reverse(entryList);

		return entryList;
	}


	/**
	 * Returns a set of tokens that occur at the given position.
	 * */
	public Set<String> getTokensAtPosition(int position){
		return stringFrequenciesAtPosition.get(position).keySet();
	}


	/***/
	public static class StringIntegerPair implements Comparable<StringIntegerPair>{

		private String string;
		private Integer integer;

		public StringIntegerPair(String s, Integer i){
			this.string = s;
			this.integer = i;
		}

		public int compareTo(StringIntegerPair arg0)
        {
	        return integer.compareTo(arg0.getInteger());
        }

		public Integer getInteger()
        {
        	return integer;
        }

		public String getString()
        {
        	return string;
        }

	}
}
