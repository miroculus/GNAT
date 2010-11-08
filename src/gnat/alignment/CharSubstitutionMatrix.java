package gnat.alignment;



/**
 * This class is used by alignment methods.
 * It is a modified version of the levenshtein distance. Here, upper/lower case mismatches as well as mismatches between whitespace and hyphen get less penalty.
 * The maximum penalty is set to -1.5f.
 * */
public class CharSubstitutionMatrix extends SubstitutionMatrix {

	public float getScore(Word w1, Word w2){

		if(w1.token.equals(w2.token)){
			return 1;
		}

		if( w1.token.toLowerCase().equals(w2.token.toLowerCase()) ){
			return 0.8f;
		}

		if( (w1.token.equals(" ") && w2.token.equals("-")) || (w1.token.equals("-") && w2.token.equals(" ")) ){
			return 0.9f;
		}

		if( (w1.token.equals("I") && w2.token.equals("1")) || (w1.token.equals("1") && w2.token.equals("I")) ){
			return 0.9f;
		}

		if( (w1.equals(Alignment.WORDGAP) && w2.token.equals(" ")) || (w2.equals(Alignment.WORDGAP) && w1.token.equals(" ")) ){
			return 0.9f;
		}

		if( (w1.equals(Alignment.WORDGAP) && !w2.equals(Alignment.WORDGAP) && w2.token.equals("-")) || (w2.equals(Alignment.WORDGAP) && !w1.equals(Alignment.WORDGAP) && w1.token.equals("-")) ){
			return 0.9f;
		}

		if( w1.equals(Alignment.WORDGAP) || w2.equals(Alignment.WORDGAP) ){
			return -1f;
		}

		return -1.5f;
	}

}
