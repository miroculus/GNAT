package gnat.server.ner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Does nothing else than converting a lexicon in the format<br>
 * <tt>ID -tab- Name1 -tab- Name2 -tab- Name3 ...</tt><br>
 * to a list with a single tuple <tt>ID -tab- Name<tt> per line, or vice versa.
 * 
 *
 */

public class ConvertDictionaryToIDNamePairs {

	public static void main (String args[]) {
		if (args.length != 3) {
			System.out.println("Converts a lexicon with ID and multiple names per line into one ID/name pair per line or vice versa");
			System.out.println("Parameters: {1|2} <infile> <outfile>");
			System.out.println(" 1 - transform into 1-name-per-line format");
			System.out.println(" 2 - transform into multiple-names-per-line format");
			System.out.println("For 2, identical IDs have to occur in consecutive lines!");
		}
		
		if (!args[0].matches("[12]")) {
			System.err.println("Unknown parameter '" + args[0] + "', expected either '1' or '2'.");
			System.exit(2);
		}
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(args[1]));
			BufferedWriter out = new BufferedWriter(new FileWriter(args[2]));
			String line;
			
			if (args[0].equals("1")) {
				// expect an ID with multiple names per line, tab-separated
				while ((line = in.readLine()) != null) {
					String[] cols = line.split("\t");
					for (int c = 1; c < cols.length; c++) {
						out.append(cols[0] + "\t" + cols[c]);
						out.newLine();
					}
				}
				
			} else if (args[0].equals("2")) {
				// expect one ID/name pair per line, tab-separated
				String oldPMID = "-1";
				LinkedList<String> names = new LinkedList<String>();
				while ((line = in.readLine()) != null) {
					String[] cols = line.split("\t");
					if (cols.length < 2) {
						System.err.println("Too few columns (" + oldPMID + "-1): "+ line);
						continue;
					}
					if (cols[0].equals(oldPMID))
						names.add(cols[1]);
					else if (!oldPMID.equals("-1")) {
						out.append(oldPMID);
						for (String name: names)
							out.append("\t" + name);
						out.newLine();
						names.clear();
						names.add(cols[1]);
					}
					oldPMID = cols[0];
				}
				out.append(oldPMID);
				for (String name: names)
					out.append("\t" + name);
				out.newLine();
				names.clear();
			}
			
			out.close();
			in.close();
		} catch (IOException ioe) {
			System.err.println(ioe.getMessage());
		}
		
	}
	
}
