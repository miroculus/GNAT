package gnat.representation;

import gnat.preprocessing.TextPreprocessor;

import java.util.List;

@SuppressWarnings("serial")
public class TextContextModel extends ContextModel {

	/**
	 *
	 * @param ID
	 */
	public TextContextModel (String ID) {
		super();
		this.ID = ID;
	}


	/**
	 *
	 */
	public TextContextModel () {
		super();
	}


	/**
	 *
	 * @param codes
	 */
	public void addCodes (String[] codes, String specific_type) {
		ContextVector go = new ContextVector(codes, specific_type);
		addContextVector(go);
	}

	/**
	 *
	 * @param codes
	 */
	public void addCodes (List<String> codes, String specific_type) {
		ContextVector go = new ContextVector(codes, specific_type);
		addContextVector(go);
	}


	/**
	 *
	 * @param keywords
	 * @param specific_type
	 */
	public void addKeywords (String[] keywords, String specific_type) {
		ContextVector kws = new
			ContextVector(TextPreprocessor.getEnglishWordTerms(keywords), specific_type);
		addContextVector(kws);
	}


	/**
	 *
	 * @param terms
	 * @param specific_type
	 */
	public void addTerms (String[] terms, String specific_type) {
		ContextVector go = new ContextVector(terms, specific_type);
		addContextVector(go);
	}


	/**
	 *
	 * @param text
	 */
	public void addPlainText (String text) {
		ContextVector plain = new ContextVector(TextPreprocessor.getEnglishWordTokens(text),
				ContextModel.CONTEXTTYPE_TEXT);
		addContextVector(plain);
	}


	public void addDiseases(String[] diseaseArray, String contexttype_disease)
    {
		ContextVector ds = new
		ContextVector(TextPreprocessor.getEnglishWordTerms(diseaseArray), contexttype_disease);
		addContextVector(ds);

    }


	public void addTissues(String[] array, String contexttype_tissue)
    {
		ContextVector ts = new
		ContextVector(TextPreprocessor.getEnglishWordTerms(array), contexttype_tissue);
		addContextVector(ts);
    }


}
