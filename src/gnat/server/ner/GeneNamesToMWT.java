package gnat.server.ner;

import gnat.utils.FileHelper;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * Generates an MWT file from a list of protein or gene names.
 * <br>
 * Input: file with names and synonyms, one per line, fields tab-separated:<pre>
 * Q15942  Zyxin-2
 * Q15942  ZYX
 * Q62523  Zyxin
 * Q62523  Zyx</pre>
 *
 *
 * Call:<br/>
 * time java -Xmx1500M -cp bin de.hu.wbi.textmining.ner.GeneNamesToMWT ~/Databases/UniProt/names4ids.txt ~/Projects/NER/stop9376.txt dictionary.mwt
 *
 *
 *
 * @author Joerg Hakenberg, Conrad Plake
 *
 */

public class GeneNamesToMWT {

	/**
	 *
	 * @param args
	 * @throws IOException
	 */
	public static void main (String[] args) throws IOException {
		if (args.length == 0 || args.length > 3) {
			System.out.println("GeneNamesToMWT -- Creates an MWT file from a list of protein names");
			System.out.println();
			System.out.println("GeneNamesToMWT <species> <namefile> <outfile>");//[<stopwords>]");
			System.out.println();
			System.out.println("  <species> -- h|m|d|y|a for human, mouse, fly, yeast, or any respectively");
			System.out.println("  <namefile> -- name of the file containing IDs and names, one pair per line");
			System.out.println("  <outfile> -- name of a file to write to");
			System.exit(0);
		}

		String species = args[0];
		if(!species.matches("h|m|d|y|a")){
			System.out.println("Species parameter must be h|m|f|y|a for human, mouse, fly, yeast, or any respectively");
		}
		String nameFile = args[1];
		System.out.println("Name file: "+nameFile);
		String outfile = args[2];
		System.out.println("Outfile: "+outfile);

		String[] namelist = FileHelper.readFromFile(nameFile);

		HashMap<String, Set<String>> names2idset = new HashMap<String, Set<String>>();
		// go through the entire list and get all potential IDs for each candidate name
		for (String line: namelist) {
			if(line.split("\t").length!=2){
				System.err.println("GeneNamesToMWT.main(): invalid line '"+line+"'");
				continue;
			}
			String ID = line.split("\t")[0];
			String name = line.split("\t")[1];

			// if the name is encapsulated in quotation marks, remove them
			// !happens in the BC2 masterlist
			if (name.matches("^\"(.+)\"$"))
				name = name.replaceFirst("^\"(.+)\"$", "$1");
			// remove " as first character
			if (name.startsWith("\""))
				name = name.substring(1);
			// remove " as last character
			if (name.endsWith("\""))
				name = name.substring(0, name.length()-1);

			// remove single numbers
			if (name.matches("\\d+"))
				continue;

			// remove anything that contains potentially misleading XML markup
			if(name.contains("<") || name.contains(">") || name.contains("&")){
				continue;
			}

			// cut trailing strange characters
			// !happens in the BC2 masterlist
			if(name.endsWith("@") || name.endsWith("#")){
				name = name.substring(0, name.length()-1);
			}
			name = name.replaceAll("\\s*[@#]", " ");

			Set<String> newIDs;
			if (names2idset.containsKey(name)) {
				newIDs = names2idset.get(name);
			} else {
				newIDs = new HashSet<String>();
			}
			newIDs.add(ID);
			names2idset.put(name, newIDs);
		}

		// for each name/ID pair, get the regex for the name
		Map<String, String> regex2ids = null;
//		if(species.equals("h")){
//			regex2ids = de.hu.wbi.textmining.ner.human.HumanTerm2Regex.getMapFromRegexToIDs(names2idset, new HashSet<String>());
//		}else if(species.equals("m")){
//			regex2ids = de.hu.wbi.textmining.ner.mouse.MouseTerm2Regex.getMapFromRegexToIDs(names2idset, new HashSet<String>());
//		}else if(species.equals("d")){
//			regex2ids = de.hu.wbi.textmining.ner.fly.FlyTerm2Regex.getMapFromRegexToIDs(names2idset, new HashSet<String>());
//		}else if(species.equals("y")){
//			regex2ids = de.hu.wbi.textmining.ner.yeast.YeastTerm2Regex.getMapFromRegexToIDs(names2idset, new HashSet<String>());
//		}else if(species.equals("a")){
			regex2ids = gnat.server.ner.Term2Regex.getMapFromRegexToIDs(names2idset, new HashSet<String>());
//		}


		// start the MWT file
		FileWriter writer = new FileWriter(outfile);

		//String tagname = "z:uniprot";
		//System.out.println("<?xml version=\"1.0\"?>\n<mwt>");
		writer.write("<?xml version=\"1.0\"?>\n<mwt>\n");
		//System.out.println("<template><" + tagname + " ids=\"%1\">%0</"	+ tagname + "></template>");
		//writer.write("<template><" + tagname + " ids=\"%1\">%0</"	+ tagname + "></template>\n");

		// sort all regexes and print the MWT entries
		Vector<String> keys = new Vector<String>(regex2ids.keySet());
		Collections.sort(keys);
		Iterator<String> it = keys.iterator();
		while (it.hasNext()) {
			String regex =  (String)it.next();
			String ids = regex2ids.get(regex);
			//System.out.println("<r p1=\"" + ids + "\">" + regex + "</r>");
			writer.write("<r p1=\"" + ids + "\">" + regex + "</r>\n");
		}

		// end of MWT file
		//System.out.println("</mwt>");
		writer.write("</mwt>\n");

		writer.close();

	}

}
