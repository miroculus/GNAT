/*
 * Created on May 5, 2005
 */

package gnat.preprocessing.sentences;

/**
 * Splits a text into sentences based on a set of regular expression that recognize
 * and disambiguate potential sentence end marks.
 * 
 * @author Joerg Hakenberg
 *
 */

public class SentenceSplitterRegex implements SentenceSplitter {

	/** List of possible section headings.<br>
	 *  Particularly in abstracts, these often are embedded within the text, not showing line breaks etc. */
	public static final String abstractHeadings = "Aim|AIM|Background|BACKGROUND|Conclusion" +
			"|CONCLUSION|Design|DESIGN|Finding|FINDING|Interpretation|INTERPRETATION|Method|METHOD" +
			"|Motivation|MOTIVATION|Objective|OBJECTIVE|Patient|PATIENT|Purpose|PURPOSE|Result|RESULT" +
			"|Setting|SETTING|Study|STUDY";

	/**
	 * Splits a text around each punctuation mark (.!?;:) followed by space, newline, or return.
	 * @param text - input text
	 * @return list of sentences
	 */
	public String[] naivSplit (String text) {
		String[] list = text.split("[\r\n]+");
    	String content = list[0];
    	for (int i = 1; i < list.length; i++)
    		content += " "  + list[i];
    	text = content;
    	return text.split("[\\.\\!\\?\\:\\;][\\s\r\n]+");
	}


	/**
	 * Splits a text into sentences.<br>
	 * <br>
	 * Tries to preserve ordering and number of characters in a string, so it all the original
	 * source can be re-engineered from the resulting array of sentences. Works if you join
	 * all sentences with &quot;. &quot; (dot whitespace) --- it only means that question and
	 * quotation marks, colon and semicolon all get replaced by a dot. Exceptions are:<br>
	 * - Adds one white space for 'sutpid' cases with literature references: 
	 * &quot;..vaccination.8 However, ..&quot; gets &quot;..vaccination 8. However, ..&quot;
	 *  
	 *  
	 * @param text
	 * @return String[] - the list of sentences
	 */
    public String[] split (String text) {
    	// transform multi-line texts into single line texts
    	String[] list = text.split("[\r\n]+");
    	String content = list[0];
    	for (int i = 1; i < list.length; i++) content += " "  + list[i];
    	text = content;

    	// replace all duplicate white spaces - might occur when a multi-line text was transformed into a one-line text
    	text = text.replaceAll("\\t", " ");
    	text = text.replaceAll("\\s\\s", " ");

    	// check for and remove foreign language title markup (Medline specific!): enclosed in square brackets "[text text text]"
    	boolean foreignLanguageTitle = false;
    	if (text == null || text.length() == 0)
    		return new String[]{""};
    	if (text.charAt(0) == '[' && text.charAt(text.length()-1) == ']') {
    		foreignLanguageTitle = true;
    		text = text.substring(1, text.length() - 1);
    	}

    	// naively, mark all possible ends-of-sentences (. ; ! ?), followed by a white space and an 
    	// upper case letter, number, or enclosing marks (" ' ( [ { ); keep the puntucation 
    	text = text.replaceAll("([\\.\\!\\?\\;\\:])\\s([A-Z0-9\"\''\\[\\(\\{])", "$1###SPLIT###$2");
    	
    	// analyze each text for headings (Background, Conclusions, etc.)
    	text = text.replaceAll("(^|\\.\\s*)((?:" + 
    			abstractHeadings +
    			")[sS]?)(?:\\s?(?:\\/|AND|and)\\s?(?:(?:" +
    			abstractHeadings + 
    			")[sS]?))?(\\:)?\\s*([A-Z][a-z]*)",
    			"$1###SPLIT###$2$3###SPLIT###$4");
    	
    	//
    	text = text.replaceAll("([a-z]{3,})\\.(The|This|We|Our|In)\\s", "$1.###SPLIT###$2 ");
    	
    	// literature references in text (sometime appear after the dot: ".. vaccination.8 However, .."
    	// =any English looking word (-ending) directly followed by a sentence end mark,
    	// then the referenced index number(s) "3-5, 8", then a white space and then the start of
    	// an English looking word at the beginning of a new sentence (upper then lower)
    	// !!! ADDS A WHITE SPACE TO THE STRING!!!
    	text = text.replaceAll("([a-z]{2,})\\.([\\d\\s\\-\\,]+)\\s([A-Z][a-z])", "$1 $2.###SPLIT###$3");

    	// now some false positive filtering
    	// all false marks <punct><mark> should be replaced by <punct><space>
    	// ".###SPLIT###" -> ". "
    	//
    	// single upper case letters (most time) belong to the following sentence part:  .. D. mel, Trevor P. Jackson, Dr. P. Peng, ..
    	// same for Dr., Mr., Prof.
    	text = text.replaceAll("(\\s[A-Z]|Dr|Drs|Prof|Profs|Mr|Mrs|Ms|ca|vs)\\.###SPLIT###", "$1. ");
    	text = text.replaceAll("([a-z]\\s[\\d\\-]+\\s[A-Z])\\.\\s([A-Z])", "$1.###SPLIT###$2");

    	// one or two words on their own most probably belong to the sentence before
    	//text = text.replaceAll("###SPLIT###([A-Za-z0-9]+)(\\s(?:[A-Za-z0-9]+))?(###SPLIT###|\\.|$)", " $1$2$3");
    	
    	// no sentence boundary within open brackets ( )
    	text = text.replaceAll("(\\([^\\)]*?[\\.\\!\\?\\;\\:])###SPLIT###", "$1 ");
    	// for some reason, we need to do this multiple times... why??? TODO put into one expression
    	text = text.replaceAll("(\\([^\\)]*[\\.\\!\\?\\;\\:])###SPLIT###", "$1 ");
    	text = text.replaceAll("(\\([^\\)]*[\\.\\!\\?\\;\\:])###SPLIT###", "$1 ");
    	text = text.replaceAll("(\\([^\\)]*[\\.\\!\\?\\;\\:])###SPLIT###", "$1 ");
    	text = text.replaceAll("(\\([^\\)]*[\\.\\!\\?\\;\\:])###SPLIT###", "$1 ");
    	text = text.replaceAll("(\\([^\\)]*[\\.\\!\\?\\;\\:])###SPLIT###", "$1 ");
    	text = text.replaceAll("(\\([^\\)]*[\\.\\!\\?\\;\\:])###SPLIT###", "$1 ");
    	text = text.replaceAll("(\\([^\\)]*[\\.\\!\\?\\;\\:])###SPLIT###", "$1 ");
    	// same for "xxx.xxx)"
    	text = text.replaceAll("([\\.\\;\\!\\?\\:])###SPLIT###([^\\(]*\\))", "$1 $2");
    	text = text.replaceAll("([\\.\\;\\!\\?\\:])###SPLIT###([^\\(]*\\))", "$1 $2");
    	text = text.replaceAll("([\\.\\;\\!\\?\\:])###SPLIT###([^\\(]*\\))", "$1 $2");
    	
    	// same for [ ]
    	text = text.replaceAll("(\\[[^\\]]*?[\\.\\!\\?\\;\\:])###SPLIT###", "$1 ");
    	text = text.replaceAll("(\\[[^\\]]*[\\.\\!\\?\\;\\:])###SPLIT###", "$1 ");
    	text = text.replaceAll("(\\[[^\\]]*[\\.\\!\\?\\;\\:])###SPLIT###", "$1 ");
    	text = text.replaceAll("(\\[[^\\]]*[\\.\\!\\?\\;\\:])###SPLIT###", "$1 ");
    	text = text.replaceAll("(\\[[^\\]]*[\\.\\!\\?\\;\\:])###SPLIT###", "$1 ");
    	text = text.replaceAll("(\\[[^\\]]*[\\.\\!\\?\\;\\:])###SPLIT###", "$1 ");
    	text = text.replaceAll("(\\[[^\\]]*[\\.\\!\\?\\;\\:])###SPLIT###", "$1 ");
       	text = text.replaceAll("(\\[[^\\]]*[\\.\\!\\?\\;\\:])###SPLIT###", "$1 ");
       	// same for "xxx.xxx]"
    	text = text.replaceAll("([\\.\\;\\!\\?])###SPLIT###([^\\[]*\\])", "$1 $2");
    	text = text.replaceAll("([\\.\\;\\!\\?])###SPLIT###([^\\[]*\\])", "$1 $2");
    	text = text.replaceAll("([\\.\\;\\!\\?])###SPLIT###([^\\[]*\\])", "$1 $2");


       	// split when sentence ends within quotation: ...referred to here as the "hairpin model." Here is another...
       	text = text.replaceAll("([\\.\\!\\?\\;][\"])\\s*", "$1###SPLIT###");

    	//text = text.replaceAll("(\\s[A-Z]\\.)###SPLIT###", "$1 ");

    	// remove splits appearing before lower case letters, but not after ;
    	//text = text.replaceAll("[^\\;]###SPLIT###([a-z])", " $1");
    	// remove splits appearing before lower case letters, even not after ;
        //text = text.replaceAll("###SPLIT###([a-z])", " $1");

    	// remove punctuation and white spaces at end of each sentence and at end of string
       	// TODO should remain intact, but CISTagger adds a new "." to each sentence end,
       	// thus we would have duplicate end marks! Solve this in CISTagger/RunTagger
    	//text = text.replaceAll("([\\!\\?\\;\\.\\s\\:]*)###SPLIT###", "###SPLIT###");
    	//text = text.replaceAll("([\\!\\?\\;\\.\\s\\:]*)$", "");

    	// re-insert enclosing markup for foreign language title
    	if (foreignLanguageTitle) text = "[ " + text + " ]";

    	// remove empty sentences
    	// occurs at begin of abstract text sometimes => "<SENT><tagged/></SENT>
    	// could be handled in another way to speed up things
    	// => find the real error -- might be in Java.split() !!!
    	/*LinkedList<String> sentencelist = new LinkedList<String>();
    	String[] sentences = text.split("###SPLIT###");
    	for (String sent: sentences) {
    		if (sent.length() > 0)
    			sentencelist.add(sent);
    	}
    	sentences = new String[sentencelist.size()];
    	for (int l = 0; l < sentencelist.size(); l++)
    		sentences[l] = sentencelist.get(l);
    	return sentences;*/
    	
    	if (text.startsWith("###SPLIT###"))
    		text = text.substring(11, text.length());
    	if (text.endsWith("###SPLIT###"))
    		text = text.substring(0, text.length() - 11);
    	
		return text.split("###SPLIT###");
    }


    /**
     * For testing purposes only
     * @param args
     */
    public static void main (String[] args) {
    	SentenceSplitter splitter = new SentenceSplitterRegex();
    	
    	String testtext = "BACKGROUND: The Carpal Tunnel Syndrome Instrument (CTSI) is a disease-specific, "+
    		"self-administered questionnaire that consists of a symptom severity scale (SS) and a functional status "+
    		"scale (FS). The CTSI was cross-culturally adapted and developed by the Impairment Evaluation Committee, "+
    		"Japanese Society for Surgery of the Hand (JSSH). The purpose of this study was to test the reliability, "+
    		"validity, and responsiveness of the Japanese version of the CTSI (CTSI-JSSH). METHODS: A consecutive "+
    		"series of 87 patients with carpal tunnel syndrome completed the CTSI-JSSH, the JSSH version of the Disability "+
    		"of the Arm, Shoulder, and Hand questionnaire (DASH-JSSH), and the 36-Item Short-Form Health Survey (SF-36).";
    	
    	String we = "The aim of the study was to investigate the hypothesis that consumption of fish liver increases cancer risk in humans due to increased intake of persistent organic pollutants (POPs). This study is based on data from the Norwegian Women and Cancer Study (NOWAC). The study has a prospective cohort design with questionnaire data from 64 285 randomly selected Norwegian women (aged 40-70 at baseline) and linkage to the Norwegian Cancer Registry. Cox proportional hazards regression was used to calculate risk ratios associated with consumption of fish liver and total cancer and cancer in breast, uterus, and colon. Fish liver consumption was, after adjusting for known risk factors, associated with a significant reduced risk for total cancer (RR = 0.92, 95% CI: 0.85, 0.99), and non-significant reduced risk for breast cancer (RR = 0.90, 95% CI: 0.78, 1.04), and colon cancer (RR = 0.82, 95% CI: 0.63, 1.07). Relative risk for uterus cancer was 0.82 (95% CI: 0.61-1.12). No significant dose-response effect was found for frequency of fish liver consumption (when divided into three intake groups) and total cancer, nor for any of the other cancer sites.We have concluded that in Norwegian women, fish liver consumption was not associated with an increased cancer risk in breast, uterus, or colon. In contrast, a decreased risk for total cancer was found.";
    	
    	String text = "###SPLIT###Hallo dies ist ein Satz###SPLIT###";
    	text = testtext;
    	text = we;
    	
    	if (text.startsWith("###SPLIT###"))
    		text = text.substring(11, text.length());
    	if (text.endsWith("###SPLIT###"))
    		text = text.substring(0, text.length() - 11);
    	System.out.println(text);
    	
    	String[] sentences = splitter.split(text);
    	for (String sent: sentences)
    		System.out.println("'" + sent + "'");
    }

}
