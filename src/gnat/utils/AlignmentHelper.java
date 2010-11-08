package gnat.utils;

import gnat.alignment.Alignment;
import gnat.alignment.CharSubstitutionMatrix;
import gnat.alignment.LocalAlignment;
import gnat.alignment.Phrase;
import gnat.representation.Gene;
import gnat.representation.RecognizedEntity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * This class offers static methods to do alignments with strings and gene names.
 * E.g. its used by the AlignmentFilter.
 * 
 * @author Conrad
 */
public class AlignmentHelper {

	// the global alignment algorithm by Needleman-Wunsch
	public static Alignment globalAlignment = new Alignment(new CharSubstitutionMatrix());

	// an extension to the global alignment
	public static LocalAlignment localAlignment = new LocalAlignment(new CharSubstitutionMatrix());



	/**
	 * Returns the subset of strings that align to s with a score above the given threshold according to the given alignment method.
	 * */
	public static Set<String> getAlignedStrings(Alignment alignment, String s, Set<String> stringSet, float minMatchValue)
	{
		Set<String> bestMatchingStrings = new HashSet<String>();
		Phrase stringPhrase = toPhrase(s);
		float stringPhraseSelfAlignedScore = alignment.getAlignmentScore(stringPhrase, stringPhrase);
		Iterator<String> stringListIt = stringSet.iterator();
		while (stringListIt.hasNext()) {
			String aString = stringListIt.next();
			float matchScore = getMatchScore(alignment, stringPhrase, toPhrase(aString), stringPhraseSelfAlignedScore, 1.5f);
			if (matchScore >= minMatchValue) {
				bestMatchingStrings.add(aString);
			}

		}
		return bestMatchingStrings;
	}

	/**
	 * Returns the subset of strings that align to s with a score above the given threshold according to the given alignment method.
	 * */
	public static Set<String> getAlignedStrings(Alignment alignment, Phrase stringPhrase, Set<Phrase> phraseSet, float minMatchValue)
	{
		Set<String> bestMatchingStrings = new HashSet<String>();
		float stringPhraseSelfAlignedScore = alignment.getAlignmentScore(stringPhrase, stringPhrase);
		Iterator<Phrase> phraseIterator = phraseSet.iterator();
		while (phraseIterator.hasNext()) {
			Phrase aPhrase = phraseIterator.next();
			float matchScore = getMatchScore(alignment, stringPhrase, aPhrase, stringPhraseSelfAlignedScore, 1.5f);
			if (matchScore >= minMatchValue) {
				bestMatchingStrings.add(aPhrase.toSingleWordString());
			}
		}
		return bestMatchingStrings;
	}


	/**
	 * Returns a list of genes that align to the gene name with a score above the given 
	 * threshold according to the given alignment method.
	 * */
	public static List<Gene> getAlignedGenes(Alignment alignment, RecognizedEntity geneName, Collection<Gene> geneList, float minMatchValue)
	{
		List<Gene> bestMatchingGenes = new LinkedList<Gene>();
		Phrase geneNamePhrase = toSortedPhrase(geneName.getName());
		float geneNameSelfAlignedScore = alignment.getAlignmentScore(geneNamePhrase, geneNamePhrase);
		Iterator<Gene> geneListIt = geneList.iterator();
		while (geneListIt.hasNext()) {
			Gene aGene = (Gene) geneListIt.next();
			float matchScore = getMatchScore(alignment, aGene, geneNamePhrase, geneNameSelfAlignedScore, 1.5f);

			if (matchScore >= minMatchValue) {
				bestMatchingGenes.add(aGene);
			}
		}
		return bestMatchingGenes;
	}


	/**
	 * Returns the maximum pairwise alignment score of a gene name phrase against all synonyms known to a gene.
	 * The self-aligned score is used for normalizing the outcome.
	 * The length factor determines the maximum difference in length two phrases can have,
	 * e.g. a factor of 2 means if one phrase is 2 times longer the other phrase, they will not be compared.
	 * This factor is to avoid aligning long gene names against short acronyms and therefore saves a lot of computation time.
	 * */
	public static float getMatchScore(Alignment alignment, Gene aGene, Phrase geneNamePhrase, float geneNameSelfAlignedScore, float lengthFactor)
	{
		float score = Integer.MIN_VALUE;
		Iterator<String> synIterator = aGene.getNames().iterator();
		while (synIterator.hasNext()) {
			String synonym = (String) synIterator.next();
			Phrase synonymPhrase = toSortedPhrase(synonym);
			if (geneNamePhrase.length() > lengthFactor * synonymPhrase.length() || synonymPhrase.length() > lengthFactor * geneNamePhrase.length()) {
				continue;
			}

			Float alignmentScore = alignment.getAlignmentScore(geneNamePhrase, synonymPhrase);

			float maxScore;
			if (synonymPhrase.length() > geneNamePhrase.length()) {
				maxScore = alignment.getAlignmentScore(synonymPhrase, synonymPhrase);
			}
			else {
				maxScore = geneNameSelfAlignedScore;
			}
			alignmentScore /= maxScore; // normalize score
			if (alignmentScore > score) {
				score = alignmentScore;
			}
		}
		return score;
	}


	/**
	 * Returns the maximum pairwise alignment score of a string phrase against another string phrase.
	 * The self-aligned score is used for normalizing the outcome.
	 * The length factor determines the maximum difference in length two phrases can have,
	 * e.g. a factor of 2 means if one phrase is 2 times longer the other phrase, they will not be compared.
	 * This factor is to avoid aligning long strings against short ones and therefore saves a lot of computation time.
	 * */
	public static float getMatchScore(Alignment alignment, Phrase stringPhrase, Phrase anotherStringPhrase, float stringPhraseSelfAlignedScore, float lengthFactor)
	{
		if (stringPhrase.length() > lengthFactor * anotherStringPhrase.length() || anotherStringPhrase.length() > lengthFactor * stringPhrase.length()) {
			return Integer.MIN_VALUE;
		}

		float alignmentScore = alignment.getAlignmentScore(stringPhrase, anotherStringPhrase);

		float maxScore;
		if (anotherStringPhrase.length() > stringPhrase.length()) {
			maxScore = alignment.getAlignmentScore(anotherStringPhrase, anotherStringPhrase);
		}
		else {
			maxScore = stringPhraseSelfAlignedScore;
		}
		alignmentScore /= maxScore; // normalize score
		return alignmentScore;
	}


	/**
	 * Transforms a string to a character phrase. Each character becomes a phrase element with a 'CHAR' tag.
	 * Before this, the string is splitted at whitespaces and tokens are sorted alphabetically.
	 * */
	public static Phrase toSortedPhrase(String string)
	{
		// string = string.toLowerCase();
		Phrase phrase = new Phrase();
		string = StringHelper.splitAndSort(string);
		for (int i = 0; i < string.length(); i++) {
			phrase.append("" + string.charAt(i), "CHAR");
		}
		return phrase;
	}


	/**
	 * Transforms a string to a character phrase. Each character becomes a phrase element with a 'CHAR' tag.
	 * */
	public static Phrase toPhrase(String string)
	{
		// string = string.toLowerCase();
		Phrase phrase = new Phrase();
		for (int i = 0; i < string.length(); i++) {
			phrase.append("" + string.charAt(i), "CHAR");
		}
		return phrase;
	}

}
