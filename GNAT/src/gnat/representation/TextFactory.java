package gnat.representation;

import gnat.ConstantsNei;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A text factory reads input from files and generates Text objects from them.
 * This includes extracting the TextContextModel for each text.
 *
 * @author Joerg Hakenberg
 *
 */

public class TextFactory {


	/**
	 * Loads a text repository from the given directory. Also loads GO terms
	 * annotated to texts. The argument <tt>directory</tt> may also point to a single
	 * file, in which case the text repository will contain only a single text.
	 * <br><br>
	 * Considers only files that have the extension *.txt.
	 *
	 * @param directory
	 * @param goCodes
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Deprecated public static TextRepository loadTextRepositoryFromDirectory (String directory, String goCodes) {
		// start a new repository
		TextRepository textRepository = new TextRepository();
		Set<String> textIds = new TreeSet<String>();

		// first, read all plain texts from the directory
		File INPUTS = new File(directory);
		
		// if the argument is a directory, load all files (*.txt) in there
		if (INPUTS.isDirectory()) {
			//System.err.println("Param is a directory.");
			if (!directory.endsWith("/"))
				directory += "/";
			// get all files in the directory
			String[] inputfiles = INPUTS.list();
			// try to load all files that end with .txt
			for (String filename: inputfiles) {
				if (filename.endsWith("txt")){
					Text text = loadTextFromFile(directory + filename);
					textRepository.addText(text);
					textIds.add(text.ID);
				}
			}
		// if the argument is a single file, load this single file
		} else {
			Text text = loadTextFromFile(directory);
			textRepository.addText(text);
			textIds.add(text.ID);
		}

		// now, add annotations found in the 'object' directory and from other sources (DBs)
		// maps each text ID to a context model
		Map<String, TextContextModel> contextModelTable = new HashMap<String, TextContextModel>();
		// maps text IDs to plain data arrays
		HashMap<String, String[]> objectTable = new HashMap<String, String[]>();

		String[] gocodefiles = new String[]{goCodes};
		File GOCODES = new File(goCodes);
		if (GOCODES.isDirectory()) {
			gocodefiles = GOCODES.list();
			for (int g = 0; g < gocodefiles.length; g++)
				gocodefiles[g] = goCodes + "/" + gocodefiles[g];
		}
		
		ObjectInputStream ois;
		try {
			for (String gocodefile: gocodefiles) {
				ois = new ObjectInputStream(new FileInputStream(gocodefile));
				HashMap<String, String[]> tempTable = (HashMap<String, String[]>) ois.readObject();
				for (String key: tempTable.keySet()) {
					objectTable.put(key, tempTable.get(key));
				}
				ois.close();
			}
			
			// go through all known texts and check if a corresponding annotation is there
			for (String textId : textIds) {
				String[] gocodes = objectTable.get(textId);
				if(gocodes!=null){
					TextContextModel tcm = contextModelTable.get(textId);
					if (tcm == null) {
						tcm = new TextContextModel();
						contextModelTable.put(textId, tcm);
					}
					tcm.addCodes(gocodes, GeneContextModel.CONTEXTTYPE_GOCODES);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}


		// finally, change all texts in the repository (only plain texts now) and
		// add the data from the table of context models

		
		
		for (String textId : textIds) {
			//System.out.println("Xchk"+textId);
			TextContextModel tcm = contextModelTable.get(textId);
			Text text = textRepository.getText(textId);
			if (tcm != null) {
				text.addToContextModel(tcm);
			} else {
				//text.setContextModel(new TextContextModel());
			}
			textRepository.setText(textId, text);
        }
		
		//System.out.println("#Xcheck: ");
		//Iterator<Text> it = textRepository.getTexts().iterator();
		//while (it.hasNext()) {
		//	Text t = it.next();
		//	System.out.println("#TCM: " +t.getContextModel().getContextVectorForType(ContextModel.CONTEXTTYPE_TEXT));
		//}

		return textRepository;
	}
	
	
	/**
	 * Loads a text repository from the given directories. Considers only files with the filename 
	 * extension "txt" files ("*.txt").
	 *
	 * @param directories
	 * @return
	 */
	public static TextRepository loadTextRepositoryFromDirectories (String... directories) {
		List<String> listOfDirectories = new LinkedList<String>();
		
		for (String dir: directories)
			listOfDirectories.add(dir);
			
		return loadTextRepositoryFromDirectories(listOfDirectories);
	}
	
	
	/**
	 * Loads a text repository from the given directories. Considers only files with the filename 
	 * extension "txt" files ("*.txt").
	 *
	 * @param directories
	 * @return
	 */
	public static TextRepository loadTextRepositoryFromDirectories (Collection<String> directories) {
		TextRepository textRepository = new TextRepository();

		for (String dir: directories) {
			
			if (!dir.endsWith("/")) dir += "/";
	
			Set<String> textIds = new TreeSet<String>();
	
			// read all plain texts from the directory
			File DIR = new File(dir);
			if (DIR.exists()) {
				String[] files = DIR.list();
				for (String filename : files) {
					if (filename.endsWith("txt")){
						Text text = loadTextFromFile(dir + filename);
						textRepository.addText(text);
						textIds.add(text.ID);
					}
				}
			}
		}
		
		if (ConstantsNei.OUTPUT_LEVEL.compareTo(ConstantsNei.OUTPUT_LEVELS.STATUS) >= 0)
			System.out.println("#TextRepository loaded with " + textRepository.size() + " texts.");

		return textRepository;
	}


	/*/*
	 * Loads a text repository from the given directory. Also loads GO terms
	 * annotated to texts. Loads texts for the given IDs only.
	 *
	 * @param directory
	 * @param goCodes
	 * @param ids
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Deprecated public static TextRepository loadTextRepositoryFromDirectoryXXX (
			String directory,
			String goCodes,
			String[] ids
			) {

		if(!directory.endsWith("/")){
			directory += "/";
		}

		TextRepository textRepository = new TextRepository();
		Set<String> loadedTextIds = new TreeSet<String>();
		Set<String> wantedTextIDs = new TreeSet<String>();
		wantedTextIDs.addAll(Arrays.asList(ids));

		// first, read all plain texts from the directory
		File dir = new File(directory);
		String[] files = dir.list();
		for (String filename : files) {
			if(filename.endsWith("txt")){
				String id = filename.replaceFirst("^(\\d+?)\\.txt$", "$1");
				if (wantedTextIDs.contains(id)) {
					Text text = loadTextFromFile(directory+filename);
					textRepository.addText(text);
					//String ID = filename.replaceFirst("(.+)\\.txt", "$1");
					loadedTextIds.add(text.ID);
				}
			}
		}

		// now, add annotations found in the 'object' directory and from other sources (DBs)
		// maps each text ID to a context model
		Map<String, TextContextModel> contextModelTable = new HashMap<String, TextContextModel>();
		// maps text IDs to plain data arrays
		HashMap<String, String[]> objectTable;

		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(new FileInputStream(goCodes));
			objectTable = (HashMap<String, String[]>) ois.readObject();
			ois.close();
			// go through all known texts and check if a corresponding annotation is there
			for (String textId : loadedTextIds) {
				String[] gocodes = objectTable.get(textId);
				if(gocodes!=null){
					TextContextModel tcm = contextModelTable.get(textId);
					if (tcm == null) {
						tcm = new TextContextModel();
						contextModelTable.put(textId, tcm);
					}
					tcm.addCodes(gocodes, GeneContextModel.CONTEXTTYPE_GOCODES);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}


		// finally, change all texts in the repository (only plain texts now) and
		// add the data from the table of context models
		for (String textId : loadedTextIds) {
			TextContextModel tcm = contextModelTable.get(textId);
			Text text = textRepository.getText(textId);
			if (tcm != null) {
				text.addToContextModel(tcm);
			} else {
				text.setContextModel(new TextContextModel());
			}
			textRepository.setText(textId, text);
        }

		return textRepository;
	}


	/**
	 * Gets a {@link Text} from the given filename. The {@link Text}'s ID will be the filename minus
	 * its extension.
	 * 
	 * @param filename
	 * @return
	 */
	public static Text loadTextFromFile (String filename) {
		// remove the extension from the filename to get an ID
		String id = filename.replaceFirst("^(.+)\\..*?$", "$1");
		
		StringBuffer text = new StringBuffer();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = br.readLine()) != null) {
				text.append(line);
			}
			br.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		// finally construct the Text to be returned
		Text aText = new Text(id, text.toString());

		// every Text needs a context model
		TextContextModel tcm = new TextContextModel(aText.ID);
		tcm.addPlainText(aText.getPlainText());

		// add the extracted context model to the text
		aText.setContextModel(tcm);

		return aText;
	}

}
