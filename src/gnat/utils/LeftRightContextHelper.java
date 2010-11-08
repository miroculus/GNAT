package gnat.utils;

public class LeftRightContextHelper
{

	/***/
	public static String[] getLeftContext(String text, int sentenceBeginIndex, int nameBeginIndex, int tokens){
		String context = text.substring(sentenceBeginIndex, nameBeginIndex);
        String[] array = getTokens(context);
        String[] leftTokens = new String[Math.min(array.length, tokens)];
        for (int i=0;i<Math.min(array.length, tokens);i++) {
        	leftTokens[leftTokens.length-1 - i] = array[array.length-1 - i];
        }
        return leftTokens;
	}

	/***/
	public static String[] getRightContext(String text, int sentenceEndIndex, int nameEndIndex, int tokens){
		if(nameEndIndex+1 > sentenceEndIndex){
			sentenceEndIndex = nameEndIndex+1;
        }
		String context = text.substring(nameEndIndex+1, sentenceEndIndex);
        String[] array = getTokens(context);
        String[] rightTokens = new String[Math.min(array.length, tokens)];
        for (int i=0;i<Math.min(array.length, tokens);i++) {
        	rightTokens[i] = array[i];
        }
        return rightTokens;
	}

	/***/
	public static String[] getTokens(String text){
        return ArrayHelper.trimArray(text.split("[ |\\,|\\.]"), BiocreativeHelper.STOPWORDS);
	}

}
