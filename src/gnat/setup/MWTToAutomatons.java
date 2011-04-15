package gnat.setup;

import gnat.server.dictionary.Dictionary;
import gnat.server.dictionary.DictionaryActor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import brics.automaton.Actor;
import brics.automaton.Automaton;
import brics.automaton.BasicOperations;
import brics.automaton.RegExp;
import brics.automaton.State;

import martin.common.ArgParser;
import martin.common.Misc;
import martin.common.StreamIterator;

public class MWTToAutomatons {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ArgParser ap = new ArgParser(args);

		int maxAutomatonsPerFile = ap.getInt("maxAutomatonsPerFile",10000);
		int report = ap.getInt("report", -1);
		File mwtFile = ap.getFile("mwt");
		File outDir = ap.getFile("outDir");
		int[] species = ap.getInts("species");

		Map<String,String> toSpeciesMap = Misc.loadMap(ap.getFile("toSpecies"));
		Set<String> excludeTerms = Misc.loadStringSetFromFile(ap.getFile("excludeTerms")); 

		for (int s : species){
			System.out.println("Processing species " + s + "...\n");
			System.out.println("Loading regular expressions...\n");
			Map<String,String> regexps = loadRegexps(mwtFile, s, toSpeciesMap);
			if (regexps != null && regexps.size() > 0){
				System.out.println("Done, loaded " + regexps.size() + " expressions.\n");
				System.out.println("Creating automatons, maximum " + maxAutomatonsPerFile + " automatons per file.\n");
				run(regexps, s, outDir, maxAutomatonsPerFile, excludeTerms, report);
				System.out.println("Completed.\n");
			}
		}
	}

	private static void run(Map<String, String> regexps, int s, File outDir,
			int maxAutomatonsPerFile, Set<String> excludeTerms, int report) {

		List<Automaton> automata = new LinkedList<Automaton>();

		int c = 0;
		int automataCount = 0;

		outDir = new File(outDir, ""+s);
		outDir.mkdir();

		for (String ids : regexps.keySet()){

			String regex = regexps.get(ids); 

			try{
				RegExp regexp = new RegExp(regex);
				Automaton entryAutomaton = regexp.toAutomaton();
				Set<String> excludeNamesMatchingRegex = excludeTerms != null ? Dictionary.getMatchingWords(entryAutomaton, excludeTerms) : new HashSet<String>();
				entryAutomaton = Dictionary.pruneAutomaton(entryAutomaton, excludeNamesMatchingRegex);
				if(!BasicOperations.isEmpty(entryAutomaton) && !BasicOperations.isEmptyString(entryAutomaton)){
					Actor actor = new DictionaryActor(ids);
					Set<State> acceptStates = entryAutomaton.getAcceptStates();
					for (State state : acceptStates) {
						state.setActor(actor);
					}
					automata.add(entryAutomaton);
				} else {
					System.out.println("Dictionary: regex '"+regex+"' was cleaned out.");
				}
			}
			catch (IllegalArgumentException iae){
				System.out.println("Dictionary: error creating regex for '"+regex+"'. message was: "+iae.getMessage());
			}

			if (automata.size() % maxAutomatonsPerFile == 0){
				Automaton automaton = Automaton.union(automata);
				automaton.determinize();
				storeAutomaton(new File(outDir, "dictionary" + automataCount++), automaton);

				automata.clear();
			}

			if (report != -1 && ++c % report == 0)
				System.out.println("Created " + c + " automatons.\n");

		}

		if(automata.size()>0){
			Automaton automaton = Automaton.union(automata);
			automaton.determinize();
			storeAutomaton(new File(outDir, "dictionary" + automataCount++), automaton);
			automata.clear();
		}


	}

	private static void storeAutomaton(File file, Automaton automaton) {
		try{
			FileOutputStream fos = new FileOutputStream(file, false);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			automaton.store(oos);
			oos.close();
			fos.close();
		} catch (Exception e){
			System.err.println(e);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static Map<String,String> loadRegexps(File mwtFile, int species,
			Map<String, String> toSpeciesMap) {

		Map<String,String> aux = new HashMap<String,String>();

		StreamIterator data = new StreamIterator(mwtFile);

		for (String s : data)
			if (s.startsWith("<r ") && s.endsWith("</r>")){
				int ids_start = 7;
				int ids_end = s.indexOf('"', ids_start+1);

				int regex_start = ids_end +2;
				int regex_end = s.length() - 4;

				if (ids_end > ids_start && regex_end > regex_start){
					String[] ids = s.substring(ids_start, ids_end).split(";");
					boolean speciesMatch = false;
					for (String id : ids)
						if (toSpeciesMap.containsKey(id) && Integer.parseInt(toSpeciesMap.get(id)) == species)
							speciesMatch = true;

					if (speciesMatch)
						aux.put(s.substring(ids_start,ids_end),s.substring(regex_start,regex_end));
				}

			}

		return aux;
	}
}
