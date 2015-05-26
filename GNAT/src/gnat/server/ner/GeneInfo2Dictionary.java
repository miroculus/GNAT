package gnat.server.ner;

import gnat.utils.HashHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;


/**
 * Reads the NCBI Entrez Gene's gene_info(.gz) file and extracts names of genes and their synonyms.
 * <br><br>
 * Format of the result:<br>
 *  &nbsp; GeneID [tab] name1 [tab] name2 ..                  (without the whitespaces)<br>
 * or<br>
 *  &nbsp; GeneID [tab] TaxID [tab] name1 [tab] name2 ..      (without the whitespaces)
 * <br>
 * 
 * @author Joerg
 *
 */

public class GeneInfo2Dictionary {
	
	static HashMap<String, Integer> nameCounts = new HashMap<String, Integer>();
	static String FILE_PRINT_COUNTS = "counts.txt";
	
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	static boolean remove (String name) {
		return name.toLowerCase().matches(
				"(trna|ncrna|misc.?rna|misc.?dna|hypothetical protein|predicted protein|predicted gene|dna segment" +
				"|similar to predicted protein" +
				"|pseudo|unknown|pseudogene|truncated|expressed" +
				"|putative protein of unknown function|protein of unknown function" +
				"|conserved hypothetical protein|protein with unknown function|putative" +
				"|(protein|dna|rna|gtp|atp|nad|nadh|lipid|nucleic acid)[\\-\\s]binding( protein)?" +
				//"|protein[\\s\\-]binding( protein)?|dna[\\s\\-]binding( protein)?|rna[\\s\\-]binding( protein)?|nucleic acid binding( protein)?" +
				//"|gtp[\\s\\-]binding( protein)?|atp[\\s\\-]binding( protein)?|lipid[\\s\\-]binding( protein)?|nad[\\s\\-]binding( protein)?" +
				"|transcription factor|transcriptional regulator|transcription regulator|putative transcriptional regulator" +
				"|regulatory protein" +
				"|transporter|putative transporter|cell division cycle|cell division protein|kinase|subtype|isolate" +
				"|alpha subunit|beta subunit|gamma subunit|delta subunit|epsilon subunit|small subunit" +
				"|member .|group .|isoform .|class .|family .|subfamily ." +
				"|. subunit|subunit .|subunit (\\d+|[ivx]+)|alpha|beta|gamma|alpha .|two component" +
				"|newentry|etc. is unspecified|or when different.*" +
				//"|putative.*|hypothetical.*|putative.*ase" +  // getting more aggressive here
				//"|protein kinase" +
				"|\\d|[a-z]|.)");
	}
	
	
	/**
	 * 
	 * @param names
	 * @return
	 */
	static Collection<String> validate (Collection<String> names) {
		Set<String> validated = new HashSet<String>();
		
		for (String name: names) {
			// some entries are lists of synonyms -> split
			Set<String> list = new LinkedHashSet<String>();
			if (name.matches(".*(\\;\\s|\\|).*")) {
				String[] split = name.split("(\\;\\s|\\|)");
				for (String s: split)
					list.add(s);
			} else
				list.add(name);
			
			for (String entry: list) {
				if (!remove(entry))
					validated.add(entry);
			}
		}
		
		return validated;
	}
	
	
	/**
	 * 
	 * @param args
	 */
	public static void main (String[] args) {
		if (args.length == 0 || (args.length > 0 && args[0].toLowerCase().matches("\\-\\-?h(elp)?"))) {
			System.out.println("Call with path to EntrezGene info file (gene_info) as 1st parameter.");
			System.out.println("Options:");
			System.out.println("  --species <id>, -s <id>  -  restrict to entries for a single species (NCBI taxon ID)");
			System.out.println("  --withTaxon, -t          -  print taxon ID as 2nd column in the output");
			System.out.println("  --count, -c              -  print counts of synonyms to counts.txt");
			System.exit(1);
		}
		
		int forTaxon = -1;
		boolean withTaxon = false;
		boolean printCounts = false;
		
		for (int a = 1; a < args.length; a++) {
			if (args[a].toLowerCase().matches("\\-\\-?(withtaxon|wt|t|taxon)"))
				withTaxon = true;
			else if (args[a].toLowerCase().matches("\\-\\-?(counts?|c)"))
				printCounts = true;
			else if (args[a].toLowerCase().matches("\\-\\-?s(pecies)?"))
				if (args.length < a+1 || !args[a+1].matches("\\d+")) {
					System.err.println("Parameter " + args[a] + " expects a taxon ID to follow");
					System.exit(1);
				}
				else
					forTaxon = Integer.parseInt(args[++a]);
		}
				
		try {
			BufferedReader br = null;
			if (args[0].endsWith(".gz")) {
				InputStream fileStream = new FileInputStream(args[0]);
				InputStream gzipStream = new GZIPInputStream(fileStream);
				Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
				br = new BufferedReader(decoder);
			} else {
				br = new BufferedReader(new FileReader(args[0]));
			}			
			String line = "";
			while ((line = br.readLine()) != null) {
				String[] cols = line.split("\t");
				String[] temp;
				int gid;
				int tid;
				Collection<String> syns = new TreeSet<String>();
				
				// get taxon ID
				if (cols[0].matches("\\d+"))
					tid = Integer.parseInt(cols[0]);
				else continue;
				if (forTaxon >= 0 && tid != forTaxon) continue;
				
				// get gene ID
				if (cols[1].matches("\\d+"))
					gid = Integer.parseInt(cols[1]);
				else continue;
				
				// get gene symbol
				if (!cols[2].equals("-"))
					syns.add(cols[2]);
				
				if (cols[1].equals("NEWENTRY") && cols[2].startsWith("Record to support"))
					continue;
				
				// get synonyms
				if (!cols[4].equals("-")) {
					// "Mort/FADD"
					if (cols[4].indexOf("/") > 0 && cols[4].matches("[A-Za-z0-9\\-]+\\/[A-Za-z0-9\\-]+")) {
						syns.add(cols[4].split("\\/")[0]);
						syns.add(cols[4].split("\\/")[1]);
					} else if (cols[4].indexOf("|") > 0) {
						temp = cols[4].split("\\|");
						for (String t: temp)
							syns.add(t);
					} else
						syns.add(cols[4]);
				}
				
				// get the description---sometimes is a gene/protein/enzyme name
				// - remove:
				//   tRNA
				//   hypothetical protein
				//   similar to predicted protein
				// - use, but sometimes pre-processing required:
				//   hypothetical protein LOC100206002
				//   NIF3 family protein
				//   cobS protein, putative
				//   similar to FADD
				//   CG12297 gene product from transcript CG12297-RA
				//   CASP8 and FADD-like apoptosis regulator
				//   chromosomal replication initiator protein DnaA
				// - use, but split:
				//   ?:   ISDvu2, transposase OrfB
				//   !:   FAD7 (FATTY ACID DESATURASE 7); omega-3 fatty acid desaturase
				if (!cols[8].equals("-")) {
					if (cols[8].matches(".*\\;\\s.*")) {
						// split at ";"
						String[] names = cols[8].split("\\;\\s");
						for (String n: names)
							syns.add(n);
					} else
						syns.add(cols[8]);
				}
				
				// get official symbol
				if (!cols[10].equals("-"))
					syns.add(cols[10]);
				
				// get official full name
				if (!cols[11].equals("-"))
					syns.add(cols[11]);
				
				// get other designators
				if (!cols[13].equals("-")) {
					temp = cols[13].split("\\|");
					for (String t: temp)
						syns.add(t);
				}
				
				// remove some spurious names, check again for entries that are lists
				syns = validate(syns);
				
				// print the tab-separated list
				if (syns.size() > 0) {
					System.out.print(gid);
					if (withTaxon)
						System.out.print("\t" + tid);
					for (String syn: syns) {
						System.out.print("\t" + syn);
						
						// count how often we found this synonym in the entire gene-info list
						if (printCounts) {
							int cnt = 1;
							if (nameCounts.containsKey(syn))
								cnt = nameCounts.get(syn) + 1;
							nameCounts.put(syn, cnt);
						}
					}
					System.out.println();
				}
				
			}
			
			br.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		
		if (printCounts) {
			System.err.println("Printing synonym counts to " + FILE_PRINT_COUNTS + " ...");
			List<String> namesSortedByCount = HashHelper.sortByIntegerValue(nameCounts);
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PRINT_COUNTS));
				for (String name: namesSortedByCount) {
					bw.append(name);
					bw.append("\t" + nameCounts.get(name));
					bw.newLine();
				}
				bw.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		
	}
	
}
