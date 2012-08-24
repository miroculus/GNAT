package gnat.preprocessing;

import gnat.ConstantsNei;
import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.Text;
import gnat.representation.TextRepository;

import java.util.Collection;
import java.util.Set;

import brics.automaton.ActionAutomaton;
import brics.automaton.Actor;
import brics.automaton.Automaton;
import brics.automaton.BasicOperations;
import brics.automaton.GeneTokenDeterminer;
import brics.automaton.RegExp;
import brics.automaton.State;

/**
 * Rewrites name ranges in texts, for instance, replacing "freac-1 to freac-3" with "freac-1, freac-2, and freac-3".
 * <br><br>
 * Note that thereby the original text is changed, replacing each Text's plain text value.
 * <br><br>
 * This expander should be called prior any gene name recognition filter!
 * 
 * @author Conrad, J&ouml;rg
 */
public class NameRangeExpander implements Filter {
	
	ActionAutomaton actionAutomaton;

	/***/
	public NameRangeExpander() {

		RegExp rangePattern = new RegExp(RangeActor.nameRangePattern);
		Automaton a1 = rangePattern.toAutomaton();
		Set<State> acceptStates = a1.getAcceptStates();
		for (State state : acceptStates) {
			state.setActor(new RangeActor());
		}

		RegExp andPattern = new RegExp(AndActor.nameAndPattern);
		Automaton a2 = andPattern.toAutomaton();
		acceptStates = a2.getAcceptStates();
		for (State state : acceptStates) {
			state.setActor(new AndActor());
		}
		
		RegExp and3Pattern = new RegExp(And3Actor.nameAnd3Pattern);
		Automaton a2b = and3Pattern.toAutomaton();
		acceptStates = a2b.getAcceptStates();
		for (State state : acceptStates) {
			state.setActor(new And3Actor());
		}
		
		RegExp and4Pattern = new RegExp(And4Actor.nameAnd4Pattern);
		Automaton a2c = and4Pattern.toAutomaton();
		acceptStates = a2c.getAcceptStates();
		for (State state : acceptStates) {
			state.setActor(new And4Actor());
		}

		RegExp slashPattern = new RegExp(SlashActor.nameSlashPattern);
		Automaton a3 = slashPattern.toAutomaton();
		acceptStates = a3.getAcceptStates();
		for (State state : acceptStates) {
			state.setActor(new SlashActor());
		}

		actionAutomaton = new ActionAutomaton(
			BasicOperations.union(
				BasicOperations.union( 
					BasicOperations.union(
						BasicOperations.union(a1, a2),
						a3),
					a2b),
				a2c));
			/*BasicOperations.union( 
				BasicOperations.union(
					BasicOperations.union(a1, a2),
				a3),
			a2b));*/
			/*BasicOperations.union(
				BasicOperations.union(a1, a2),
				a3)
			);*/
	}
	

	@Override
	public void filter(Context context, TextRepository textRepository, GeneRepository geneRepository) {
		Collection<Text> texts = textRepository.getTexts();
		for (Text text : texts) {
			expandText(text);
		}
	}


	/**
	 * Checks for name ranges in the {@link Text}'s plain text and replaces it with expanded ranges.
	 * @see Text#getPlainText()
	 * @see Text#setPlainText(String)
	 * @param text - the text to expand
	 */
	public void expandText (Text text){
		String plain = text.getPlainText();
		String expanded = expandText(plain);
		//if (!expanded.equals(plain))
		//	System.out.println("=====\nOriginal text:\n" + plain + "\n-----\nExpanded text:\n" + expanded + "=====");
		text.setPlainText(expanded);
	}


	/***/
	public String expandText(String plainText){
		StringBuffer output = new StringBuffer();
		actionAutomaton.run(plainText, output, new GeneTokenDeterminer());

		String newText = plainText;

		if (output.length() > 0) {

			String expandedText = plainText;

			int matchStart = output.toString().indexOf("<$");

			while(matchStart!=-1){

				int comma = output.toString().indexOf(",", matchStart);
				int matchEnd = output.toString().indexOf(">", comma);

				int rangeBeginIndex = Integer.parseInt(output.toString().substring(matchStart + 2, comma));
				int rangeEndIndex = Integer.parseInt(output.toString().substring(comma + 1, matchEnd));

				String oldPhrase = plainText.substring(rangeBeginIndex, rangeEndIndex+1);
				String newPhrase = output.toString().substring(matchEnd + 1, output.toString().indexOf("</$>", matchEnd));

				expandedText = expandedText.replace(oldPhrase, newPhrase);

				matchStart = output.toString().indexOf("<$", matchEnd);
			}

			newText = expandedText;
		}

		return newText;
	}

	/**
	 * Test
	 */
	public static void main(String[] args)
	{
		NameRangeExpander nameRangeExpander = new NameRangeExpander();
		StringBuffer output = new StringBuffer();

		String plainTexts[] = {" GalNAc-T1 and -T2 ",
			" freac-1 to freac-7 ",
			"that the hHR23A and -B also in",
			" hMAD-3 and -4 ",
			" synapsins I and II ",
			" MURF1, 2 and 3 ",
			" MURF-1, -2, and -3 ",
			" MURF-I, -II and -IV ",
			" MURF-I, II and IV ",
			" SGK1, SGK2 and SGK3 ",
			" HIPK2, RAD51 and p53 ",
			" 463, 264, and 124 ",
			" I, II, and III ",
			" Vps26, 29, and 35 ",
			" cofactors A, D, E, and C ",
			" cofactors-A, -D, -E, and -C ",
			" FA proteins A, C, G and F ",
			" GlcNAc6ST-1, -2, and -3 ",
			" SMADs 1, 5, and 8 ",
			" SMADs 2 and 3 ",
			" HDACs 1 and 2 ",
			"at 3q27-29 ",
			" Biochemistry 37:6033-6040, 1998) "
		};

		
		for (String plainText: plainTexts) {
		
			System.out.println("Plain Text: " + plainText);
	
			nameRangeExpander.actionAutomaton.run(plainText, output, new GeneTokenDeterminer());
	
			if (output.length() > 0) {
				StringBuffer expandedText = new StringBuffer();
	
				int matchStart = output.toString().indexOf("<$");
				int comma = output.toString().indexOf(",", matchStart);
				int matchEnd = output.toString().indexOf(">", comma);
	
				String rangeBeginIndex = output.toString().substring(matchStart + 2, comma);
				String rangeEndIndex = output.toString().substring(comma + 1, matchEnd);
	
				String newPhrase = output.toString().substring(matchEnd + 1, output.toString().indexOf("</$>", matchEnd));
	
				System.out.println(newPhrase);
	
				expandedText.append(plainText.substring(0, Integer.parseInt(rangeBeginIndex)));
				expandedText.append(newPhrase);
				expandedText.append(plainText.substring(Integer.parseInt(rangeEndIndex) + 1));
	
				System.out.println("Expanded Text: " + expandedText);
			}
			
			System.out.println();
			
			output.setLength(0);
		}
	}

	/***/
	@SuppressWarnings("serial")
	static class RangeActor implements Actor
	{
		// e.g. freak-1 to freak-3 -> freak-1, freak-2, and freak-3

		static String nameRangePattern = "(([A-Za-z0-9]+ )?([A-Za-z0-9]+)[ ]?[0-9]+[ ]?(to|\\-)[ ]?[0-9]+" +
									"|" + "([A-Za-z0-9]+ )?[A-Za-z0-9\\-]+[0-9]+[ ]?(to|\\-)[ ]?[A-Za-z0-9\\-]+[0-9]+)";

		public void act(final char[] chars, final int matchStartIndex, final int matchEndIndex, final StringBuffer outputBuffer)
		{
			boolean appendName = true;
			StringBuffer name = new StringBuffer();
			String nameRangeFromString = "";
			String nameRangeToString = "";

			boolean nameRangeFromParsed = false;

			for (int i = matchStartIndex; i <= matchEndIndex; i++) {
				char c = chars[i];
				if (Character.isDigit(c) && i<chars.length-1 && !Character.isLetter(chars[i+1])) {
					if (nameRangeFromParsed == false) {
						nameRangeFromString += c;
					}
					else {
						nameRangeToString += c;
					}
					appendName = false; // end of name
				}
				else if (nameRangeFromString.length() > 0) {
					nameRangeFromParsed = true;
				}

				if (appendName) {
					name.append(c);
				}
			}

			int nameRangeFrom;
            int nameRangeTo;
            try {
	            nameRangeFrom = Integer.parseInt(nameRangeFromString);
	            nameRangeTo = Integer.parseInt(nameRangeToString);
            }
            catch (NumberFormatException e) {
	            System.err.println(e.getMessage());
	            return;
            }

			String nameString = name.toString();
			//System.out.println(nameString);

			if (nameRangeTo <= nameRangeFrom
							|| nameRangeTo-nameRangeFrom > 20
							|| nameRangeFrom > 99
							|| nameString.contains("amino acid")
							|| nameString.contains("residues")
							|| nameString.matches("(day|of) ")
							|| nameString.contains("region")
							|| nameString.matches(".*\\s\\d+[pq]") // chromosomal location: "3q27-29"
							|| nameString.endsWith(":")            // citation, "Vol:PageRange"
							|| nameString.equals("")
							|| nameString.contains("chromosome")) {
				return;
			}

			outputBuffer.append("<$" + matchStartIndex + "," + matchEndIndex + ">");
			for (int i = nameRangeFrom; i <= nameRangeTo; i++) {
				outputBuffer.append(name.toString() + i);
				if (i < nameRangeTo) {
					outputBuffer.append(", ");
				}
				if (i == nameRangeTo - 1) {
					outputBuffer.append("and ");
				}

			}
			outputBuffer.append("</$>");
		}

		public void merge(final Actor actor)
		{

		}
	}

	/***/
	@SuppressWarnings("serial")
	static class AndActor implements Actor
	{
		// e.g.: GalNAc-T1 and -T2, hHR23A and -B

		static String nameAndPattern =
			//"([A-Za-z0-9\\-]+([0-9]+| I+|A) and [\\-][A-Za-z]*([0-9]+|I+|B)" +
			//"|[A-Za-z0-9\\-]+([0-9]+| I+|A) and ([0-9]+|I+|B))";
			"([A-Za-z0-9\\-]+( ?[0-9]+| I+|A) and [\\-][A-Za-z]*([0-9]+|I+|B)" +
			"|[A-Za-z0-9\\-]+( ?[0-9]+| I+|A) and ([0-9]+|I+|B))";

		public void act(final char[] chars, final int matchStartIndex, final int matchEndIndex, final StringBuffer outputBuffer)
		{
			
			//System.out.println("here: ");
			
			StringBuffer match = new StringBuffer();
			String firstName = null;
			String ending = "";

			for (int i = matchStartIndex; i <= matchEndIndex; i++) {
				char c = chars[i];
				match.append(c);
				if(match.toString().endsWith(" and ")){
					firstName = match.subSequence(0, match.length() - 5).toString();
				}else if(firstName!=null && c!='-'){
					ending += c;
				}
			}

			if(firstName.matches("([A-Z]+|[a-z]+)s( ?[0-9]+| I+|A)")){
				firstName = firstName.replaceFirst("([A-Z]+|[a-z]+)s( ?[0-9]+| I+|A)", "$1$2");
				//firstName = firstName.substring(0, firstName.length()-1);
			}
			if(ending.length()>firstName.length())
				return;
			
			//System.out.println("firstname: '" + firstName + "'");
			//String basename = firstName.replaceFirst("( ?[0-9]+| I+|A)$", "");
			String baseName = "";
			if (firstName.matches("^(.+)([\\s\\-]).*?$"))
				baseName = firstName.replaceFirst("^(.+)([\\s\\-]).*?$", "$1$2");
			else
				baseName = firstName.substring(0, firstName.length()-ending.length());
			//System.out.println("basename: '" + baseName + "'");

			if (baseName.matches("(day|of|at|positions?|(para)?segment|residues?|amino[ -]?acids?|repeats?) ")
					|| baseName.equals("-")
					|| ending.length() > baseName.length())
				return;

			outputBuffer.append("<$" + matchStartIndex + "," + matchEndIndex + ">");
			outputBuffer.append(firstName);
			outputBuffer.append(" and ");
			//outputBuffer.append(firstName.substring(0, firstName.length()-ending.length()) + ending);
			outputBuffer.append(baseName + ending);
			outputBuffer.append("</$>");

			//System.out.println("ANDRANGE match="+outputBuffer);
		}

		public void merge(final Actor actor)
		{

		}
	}
	
	
	/***/
	@SuppressWarnings("serial")
	static class And3Actor implements Actor
	{
		// e.g.: MURF1, 2, and 3

		static String nameAnd3Pattern = 
			"([A-Za-z]+[A-Za-z0-9\\-]+?)([ \\-]*)([0-9]+|[IVX]+|[ivx]+|[AaBbCcDd])\\, [\\-]?([0-9]+|([IVX]+|[ivx]+)|[BbCcDdEe])\\,? and [\\-]?([0-9]+|([IVX]+|[ivx]+)|[CcDdEeFfGg])"
			;// "|[A-Za-z]+[A-Za-z0-9\\-]+?([0-9]+|[\\s\\-]([IVX]+|[ivx]+)|[AaBbCcDd])\\, ([0-9]+|([IVX]+|[ivx]+)|[BbCcDdEe])\\,? and ([0-9]+|([IVX]+|[ivx]+)|[CcDdEeFfGg]))";

		public void act(final char[] chars, final int matchStartIndex, final int matchEndIndex, final StringBuffer outputBuffer)
		{
			//StringBuffer match = new StringBuffer();
			//String firstName = null;
			//String ending = "";
			
			StringBuffer fullbuf = new StringBuffer();
			for (int i = matchStartIndex; i <= matchEndIndex; i++)
				fullbuf.append(chars[i]);
			String full = fullbuf.toString();
			
			String basename = "";
			String inter = "";
			String number1 = "";
			String number2 = "";
			String number3 = "";

			if (full.matches("^([A-Za-z]+?[A-Za-z0-9\\-]+?)([\\s\\-]*)([0-9]+|[IVX]+|[ivx]+|[A-Z])\\, [\\-]?([0-9]+|[IVX]+|[ivx]+|[A-Z])\\,? and [\\-]?([0-9]+|[IVX]+|[ivx]+|[A-Z])$")) {
				basename = full.replaceFirst("^([A-Za-z]+?[A-Za-z0-9\\-]+?)([\\s\\-]*)([0-9]+|[IVX]+|[ivx]+|[A-Z])\\, [\\-]?([0-9]+|[IVX]+|[ivx]+|[A-Z])\\,? and [\\-]?([0-9]+|[IVX]+|[ivx]+|[A-Z])$", "$1");
				inter =    full.replaceFirst("^([A-Za-z]+?[A-Za-z0-9\\-]+?)([\\s\\-]*)([0-9]+|[IVX]+|[ivx]+|[A-Z])\\, [\\-]?([0-9]+|[IVX]+|[ivx]+|[A-Z])\\,? and [\\-]?([0-9]+|[IVX]+|[ivx]+|[A-Z])$", "$2");
				number1 =  full.replaceFirst("^([A-Za-z]+?[A-Za-z0-9\\-]+?)([\\s\\-]*)([0-9]+|[IVX]+|[ivx]+|[A-Z])\\, [\\-]?([0-9]+|[IVX]+|[ivx]+|[A-Z])\\,? and [\\-]?([0-9]+|[IVX]+|[ivx]+|[A-Z])$", "$3");
				number2 =  full.replaceFirst("^([A-Za-z]+?[A-Za-z0-9\\-]+?)([\\s\\-]*)([0-9]+|[IVX]+|[ivx]+|[A-Z])\\, [\\-]?([0-9]+|[IVX]+|[ivx]+|[A-Z])\\,? and [\\-]?([0-9]+|[IVX]+|[ivx]+|[A-Z])$", "$4");
				number3 =  full.replaceFirst("^([A-Za-z]+?[A-Za-z0-9\\-]+?)([\\s\\-]*)([0-9]+|[IVX]+|[ivx]+|[A-Z])\\, [\\-]?([0-9]+|[IVX]+|[ivx]+|[A-Z])\\,? and [\\-]?([0-9]+|[IVX]+|[ivx]+|[A-Z])$", "$5");
				
				//System.err.println("basename="+basename+", inter="+inter);
				
				/*String baseName = full.replaceFirst("^([A-Za-z0-9\\-]+)([\\s\\-]?(?:[0-9]+|(?:[IVX]+|[ivx]+)|[AaBbCcDd]))\\, .*$", "$1");
				String number1 = full.replaceFirst("^([A-Za-z0-9\\-]+)([\\s\\-]?(?:[0-9]+|(?:[IVX]+|[ivx]+)|[AaBbCcDd]))\\, .*$", "$2");
				String number2 = full.replaceFirst("^([A-Za-z0-9\\-]+)([\\s\\-]?(?:[0-9]+|(?:[IVX]+|[ivx]+)|[AaBbCcDd]))\\, [\\-]?([A-Za-z]*(?:[0-9]+|(?:[IVX]+|[ivx]+)|B))\\,? and .*$", "$3");
				String number3 = full.replaceFirst("^([A-Za-z0-9\\-]+)([\\s\\-]?(?:[0-9]+|(?:[IVX]+|[ivx]+)|[AaBbCcDd]))\\, [\\-]?([A-Za-z]*(?:[0-9]+|(?:[IVX]+|[ivx]+)|B))\\,? and [\\-]?([A-Za-z]*(?:[0-9]+|I+|[CcDdEeFfGg]))$", "$4");
				System.err.println(baseName);
				System.err.println(number1);
				System.err.println(number2);
				System.err.println(number3);*/
			//} else if (full.matches("[A-Za-z]+[A-Za-z0-9\\-]+?([0-9]+| I+|[AaBbCcDd])\\, ([0-9]+|I+|[BbCcDdEe])\\,? and ([0-9]+|(?:[IVX]+|[ivx]+)|[CcDdEeFfGg])")) {
				//System.err.println("here2");
			//	name1 = full.replaceFirst("^([A-Za-z]+[A-Za-z0-9\\-]+?)([0-9]+|[\\s\\-](?:[IVX]+|[ivx]+)|[AaBbCcDd])\\, ([0-9]+|(?:[IVX]+|[ivx]+)|[BbCcDdEe])\\,? and ([0-9]+|(?:[IVX]+|[ivx]+)|[CcDdEeFfGg])$", "$1$2");
			//	name2 = full.replaceFirst("^([A-Za-z]+[A-Za-z0-9\\-]+?)([0-9]+|[\\s\\-](?:[IVX]+|[ivx]+)|[AaBbCcDd])\\, ([0-9]+|(?:[IVX]+|[ivx]+)|[BbCcDdEe])\\,? and ([0-9]+|(?:[IVX]+|[ivx]+)|[CcDdEeFfGg])$", "$1$3");
			//	name3 = full.replaceFirst("^([A-Za-z]+[A-Za-z0-9\\-]+?)([0-9]+|[\\s\\-](?:[IVX]+|[ivx]+)|[AaBbCcDd])\\, ([0-9]+|(?:[IVX]+|[ivx]+)|[BbCcDdEe])\\,? and ([0-9]+|(?:[IVX]+|[ivx]+)|[CcDdEeFfGg])$", "$1$4");
			} else {
				if (ConstantsNei.verbosityAtLeast(ConstantsNei.OUTPUT_LEVELS.WARNINGS))
					System.out.println("#NameRangeExpander: error case And3.1: no match, no expansion?");
			}
			
			if (!sanityCheck(basename, new String[]{number1, number2, number3})) {
				outputBuffer.append("<$" + matchStartIndex + "," + matchEndIndex + ">");
				outputBuffer.append(full);
				outputBuffer.append("</$>");
			} else {
				String expanded = getCombination(new String[]{basename, inter, number1, number2, number3});
				if (basename.endsWith("s") && basename.length() >= 4) {
					basename = basename.substring(0, basename.length()-1);
					expanded += ", or " + getCombination(new String[]{basename, inter, number1, number2, number3});
				}
				//System.err.println("#Expanding '" + full + "' to '" + expanded + "'");
				outputBuffer.append("<$" + matchStartIndex + "," + matchEndIndex + ">");
				outputBuffer.append(expanded);
				outputBuffer.append("</$>");
			}

			//System.out.println("ANDRANGE match="+outputBuffer);
		}

		public void merge(final Actor actor)
		{

		}
	}
	

	/***/
	@SuppressWarnings("serial")
	static class And4Actor implements Actor
	{
		// e.g.: MURF1, 2, and 3

		//static String nameAnd4Pattern ="(([A-Za-z]+[A-Za-z0-9\\-]+?)([\\s\\-]?([0-9]+|([IVX]+|[ivx]+)|[AaBbCcDd]))\\, [\\-]?([0-9]+|([IVX]+|[ivx]+)|[BbCcDdEe])\\, [\\-]?([0-9]+|([IVX]+|[ivx]+)|[CcDdEeFfGgHh])\\,? and [\\-]?([0-9]+|([IVX]+|[ivx]+)|[CcDdEeFfGgHhIiJj])" +
		//		"|[A-Za-z]+[A-Za-z0-9\\-]+?([0-9]+|[\\s\\-]([IVX]+|[ivx]+)|[AaBbCcDd])\\, ([0-9]+|([IVX]+|[ivx]+)|[BbCcDdEe])\\, ([0-9]+|([IVX]+|[ivx]+)|[CcDdEeFfGg])\\,? and ([0-9]+|([IVX]+|[ivx]+)|[CcDdEeFfGgHhIiJj]))";
		static String nameAnd4Pattern = "([A-Za-z0-9\\-]+[A-Z0-9][A-Za-z0-9\\-]* )?[A-Za-z0-9\\-]+[A-Za-z]+[ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|([IVX]+|[ivx]+)|[[A-Za-z])\\, [ \\-]*([0-9]+|([IVX]+|[ivx]+)|[[A-Za-z])\\,? and [ \\-]*([0-9]+|([IVX]+|[ivx]+)|[[A-Za-z])";
		//static String nameAnd4Pattern = "[A-Za-z]+ [Aa]\\, [Dd]\\, [Ee]\\, and [Cc]";
		
		public void act(final char[] chars, final int matchStartIndex, final int matchEndIndex, final StringBuffer outputBuffer)
		{
			//StringBuffer match = new StringBuffer();
			//String firstName = null;
			//String ending = "";
			
			StringBuffer fullbuf = new StringBuffer();
			for (int i = matchStartIndex; i <= matchEndIndex; i++)
				fullbuf.append(chars[i]);
			String full = fullbuf.toString();
			
			String basename = "";
			String inter = "";
			String number1 = "";
			String number2 = "";
			String number3 = "";
			String number4 = "";
			
			// FA proteins 1, 2, 3, and 4
			if (full.matches("([A-Za-z0-9\\-]+[A-Z0-9][A-Za-z0-9\\-]* )([A-Za-z0-9\\-]+[A-Za-z]+)([ \\-]*)([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\,? and [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])")) {
				basename = full.replaceFirst("^([A-Za-z0-9\\-]+[A-Z0-9][A-Za-z0-9\\-]* )([A-Za-z0-9\\-]+[A-Za-z]+)([ \\-]*)([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\,? and [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])$", "$1$2");
				inter = full.replaceFirst("^([A-Za-z0-9\\-]+[A-Z0-9][A-Za-z0-9\\-]* )([A-Za-z0-9\\-]+[A-Za-z]+)([ \\-]*)([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\,? and [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])$", "$3");
				
				number1 = full.replaceFirst("^([A-Za-z0-9\\-]+[A-Z0-9][A-Za-z0-9\\-]* )([A-Za-z0-9\\-]+[A-Za-z]+)([ \\-]*)([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\,? and [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])$", "$4");
				number2 = full.replaceFirst("^([A-Za-z0-9\\-]+[A-Z0-9][A-Za-z0-9\\-]* )([A-Za-z0-9\\-]+[A-Za-z]+)([ \\-]*)([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\,? and [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])$", "$5");
				number3 = full.replaceFirst("^([A-Za-z0-9\\-]+[A-Z0-9][A-Za-z0-9\\-]* )([A-Za-z0-9\\-]+[A-Za-z]+)([ \\-]*)([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\,? and [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])$", "$6");
				number4 = full.replaceFirst("^([A-Za-z0-9\\-]+[A-Z0-9][A-Za-z0-9\\-]* )([A-Za-z0-9\\-]+[A-Za-z]+)([ \\-]*)([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\,? and [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])$", "$7");
			// Bcl-2, 3, 5, and 7
			} else if (full.matches("([A-Za-z0-9\\-]+[A-Za-z]+)([ \\-]*)([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\,? and [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])")) {
				//System.err.println("here2");
				basename = full.replaceFirst("^([A-Za-z0-9\\-]+[A-Za-z]+)([ \\-]*)([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\,? and [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])$", "$1");
				inter = full.replaceFirst("^([A-Za-z0-9\\-]+[A-Za-z]+)([ \\-]*)([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\,? and [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])$", "$2");
				
				number1 = full.replaceFirst("^([A-Za-z0-9\\-]+[A-Za-z]+)([ \\-]*)([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\,? and [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])$", "$3");
				number2 = full.replaceFirst("^([A-Za-z0-9\\-]+[A-Za-z]+)([ \\-]*)([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\,? and [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])$", "$4");
				number3 = full.replaceFirst("^([A-Za-z0-9\\-]+[A-Za-z]+)([ \\-]*)([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\,? and [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])$", "$5");
				number4 = full.replaceFirst("^([A-Za-z0-9\\-]+[A-Za-z]+)([ \\-]*)([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\, [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])\\,? and [ \\-]*([0-9]+|[IVX]+|[ivx]+|[A-Za-z])$", "$6");
			} else {
				System.err.println("??");
			}

			// check base name and numbers for validity
			if (!sanityCheck(basename, new String[]{number1, number2, number3, number4})) {
				outputBuffer.append("<$" + matchStartIndex + "," + matchEndIndex + ">");
				outputBuffer.append(full);
				outputBuffer.append("</$>");
			} else {
				// produce the final expanded text
				String expanded = getCombination(new String[]{basename, inter, number1, number2, number3, number4});
				if (basename.endsWith("s") && basename.length() >= 4) {
					basename = basename.substring(0, basename.length()-1);
					expanded += ", or " + getCombination(new String[]{basename, inter, number1, number2, number3, number4});
				}
				//System.err.println("#Expanding '" + full + "' to '" + expanded + "'");
				outputBuffer.append("<$" + matchStartIndex + "," + matchEndIndex + ">");
				outputBuffer.append(expanded);
				outputBuffer.append("</$>");
			}

			//System.out.println("ANDRANGE match="+outputBuffer);
		}

		public void merge(final Actor actor)
		{

		}
	}
	

	/***/
	@SuppressWarnings("serial")
	static class SlashActor implements Actor
	{
		// e.g. BMP2/4 -> BMP2 and BMP4
		// !but not BMP2/BMP4 -> BMP2 and BMP4

		static String nameSlashPattern ="[A-Za-z0-9\\-]+/[0-9]{1,3}";

		public void act(final char[] chars, final int matchStartIndex, final int matchEndIndex, final StringBuffer outputBuffer)
		{
			StringBuffer match = new StringBuffer();
			String firstName = null;
			String ending = "";

			for (int i = matchStartIndex; i <= matchEndIndex; i++) {
				char c = chars[i];
				match.append(c);
				if(match.toString().endsWith("/")){
					firstName = match.subSequence(0, match.length() - 1).toString();
				}else if(firstName!=null && c!='-'){
					ending += c;
				}
			}

			//System.out.println("SLASHACTOR: match="+match);

			if(ending.length()>firstName.length())
				return;

			outputBuffer.append("<$" + matchStartIndex + "," + matchEndIndex + ">");
			outputBuffer.append(firstName);
			outputBuffer.append(" and ");
			outputBuffer.append(firstName.substring(0, firstName.length()-ending.length()) + ending);
			outputBuffer.append("</$>");
		}

		public void merge(final Actor actor)
		{

		}
	}
	
	
	/**
	 * Combines the given arguments into an expanded output string.
	 * First argument has to be the base name of the gene, the second argument the
	 * intermediate string (dash, white space, can also be empty). All following
	 * arguments are the numbers (digits, Roman, letters) of each individual gene,
	 * which get attached to the base name and the intermediate string.
	 * For two names, joins them with an "and"; for more than two names, joins
	 * them with ",", and an ", and" for the last instance.<br><br>
	 * Example: call with {"MURF", "-", "1", "2", "3"}<br>
	 * Result: "MURF-1, MURF-2, and MURF-3".
	 * @param args
	 * @return
	 */
	static String getCombination (String[] args) {
		String result = args[0] + args[1] + args[2];
		if (args.length == 4)
			result += " and " + args[0] + args[1] + args[3];
		else {
			for (int i = 3; i < args.length-1; i++)
				result += ", " + args[0] + args[1] + args[i];
			result += ", and " + args[0] + args[1] + args[args.length-1];
		}
		return result;
	}


	/**
	 * Checks the given base name and numbers if they make sense; returns false if the case should
	 * be skipped and therefore not expanded.
	 * <ul>
	 * <li>the base name should look like a gene name
	 * <li>the numbers should be of the same type (digits, letters, or Roman) and case
	 * <li>the numbers should be in ascending order
	 * <li>no number should not be zero
	 * </ul>
	 * There are cases in the BC2 set where the order was not maintained =&gt; skip that particular check!<br>
	 * 
	 * @param basename
	 * @param numbers
	 * @return
	 */	
	static boolean sanityCheck (String basename, String[] numbers) {
		if (basename.matches("(of|or|and|for|with)")) return false;
		if (basename.matches("(chromosome|protein|domain)s?")) return false;
		
		boolean arabic = numbers[0].matches("[1-9][0-9]*");
		if (arabic) {
			for (int i = 1; i < numbers.length; i++)
				if (!numbers[i].matches("[1-9][0-9]*")) return false;
		}
		
		boolean lowercase = numbers[0].matches("[a-z]");
		if (lowercase) {
			for (int i = 1; i < numbers.length; i++)
				if (!numbers[i].matches("[a-z]")) return false;
		}
		
		boolean uppercase = numbers[0].matches("[A-Z]");
		if (uppercase) {
			for (int i = 1; i < numbers.length; i++)
				if (!numbers[i].matches("[A-Z]")) return false;
		}
		
		boolean romanL = numbers[0].matches("[ivx]+");
		if (romanL) {
			for (int i = 1; i < numbers.length; i++)
				if (!numbers[i].matches("[ivx]+")) return false;
		}
		
		boolean romanU = numbers[0].matches("[IVX]+");
		if (romanU) {
			for (int i = 1; i < numbers.length; i++)
				if (!numbers[i].matches("[IVX]+")) return false;
		}
		
		if (arabic) {
			for (int i = 1; i < numbers.length; i++) {
				if (Integer.parseInt(numbers[i-1]) >= Integer.parseInt(numbers[i])) {
					if (ConstantsNei.verbosityAtLeast(ConstantsNei.OUTPUT_LEVELS.WARNINGS))
						System.out.println("#NameRangeExpander: spurious order of numbers: " + numbers[i-1] + " -> " + numbers[i] + " in basename='" + basename + "'");
					//return false;
				}
			}
		}
		
		if (uppercase || lowercase) {
			for (int i = 1; i < numbers.length; i++) {
				if (numbers[i-1].compareTo(numbers[i]) >= 0) {
					if (ConstantsNei.verbosityAtLeast(ConstantsNei.OUTPUT_LEVELS.WARNINGS))
						System.out.println("#NameRangeExpander: spurious order of numbers: " + numbers[i-1] + " -> " + numbers[i] + " in basename='" + basename + "'");
					//return false;
				}
			}
		}
		
		// TODO: check Roman numerals for ascending order
		
		return true;
	}
	
}
