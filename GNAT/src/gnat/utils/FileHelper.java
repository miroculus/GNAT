/* Created on 16.07.2004 */

package gnat.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Provides different static methods to handle in- and output from and to files.
 *
 *
 * @author Joerg Hakenberg
 */
public class FileHelper {

	/**
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException */
	public static BufferedReader getBufferedFileReader(String filename) throws UnsupportedEncodingException, FileNotFoundException{
		return new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
	}


	/**
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException */
	public static BufferedWriter getBufferedFileWriter(String filename) throws UnsupportedEncodingException, FileNotFoundException{
		return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"));
	}

	/***/
	public static void concatenateFiles(String filename1, String filename2, String outfilename) throws FileNotFoundException, UnsupportedEncodingException{
		String[] lines1 = FileHelper.readFromFile(filename1);
		String[] lines2 = FileHelper.readFromFile(filename2);
		
		PrintWriter pw = new PrintWriter(outfilename, "UTF-8");
		for (String string : lines1) {
			pw.write(string+"\n");
		}
		for (String string : lines2) {
			pw.write(string+"\n");
		}
		pw.close();
	}
	

	/**
	 * @param FILE
	 * @return
	 */
	public static String getFileExtension(File FILE) {
		return FILE.getName().replaceFirst("^.+\\.(.+?)$", "$1");
	}
	
	
	/**
	 * Reads a Java object from the given file.
	 * @param FILE
	 * @return
	 */
	public static Object readObjectFromFile (File FILE) {
		Object result = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE));
			result = ois.readObject();
			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}


	/**
	 * Writes a Java object to the given file.
	 * @param o
	 * @param FILE
	 */
	public static void writeObjectToFile (Object o, File FILE) {
		try {
			FileOutputStream fos = null;
			ObjectOutputStream out = null;
			fos = new FileOutputStream(FILE);
			out = new ObjectOutputStream(fos);
			out.writeObject(o);
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * Reads a file into a string array, one line per element.
	 * @param filename - the file to read
	 * @return content as strin array
	 */
	public static String[] readFromFile (File file) {
		List<String> content = new LinkedList<String>();
		try {
			FileInputStream fis = new FileInputStream(file);
			BufferedReader in = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
			String line;
			while ((line = in.readLine()) != null)
				content.add(line);
			in.close();
			in = null;
			fis.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
		String[] result = new String[content.size()];
		for (int i = 0; i < content.size(); i++) {
			result[i] = (String)content.get(i);
		}
		content.clear();
		content = null;
		return result;
	}

	/**
	 * @throws IOException
	 */
	public static Set<String> readFileIntoSet (String filename, boolean toLowerCase) throws IOException {
		Set<String> s = new HashSet<String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
		String line;
		while ((line = in.readLine()) != null)
			if(toLowerCase)
				s.add(line.toLowerCase());
			else
				s.add(line);

		in.close();
		in = null;
		return s;
	}

	/**
	 */
	public static Set<String> readFileIntoSet (String filename, boolean toLowerCase, boolean plusFirstCharacterUpperCaseVariant) {
		Set<String> s = new HashSet<String>();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;
			while ((line = in.readLine()) != null){
				if(line.length()>0 && !line.startsWith("#")){
					if(toLowerCase){
						line = line.toLowerCase();
						s.add(line);
					}
					else{
						s.add(line);
					}
					if(plusFirstCharacterUpperCaseVariant){
						char firstChar = Character.toUpperCase(line.charAt(0));
						s.add(firstChar + line.substring(1));
					}
				}
			}
			in.close();
			in = null;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
		return s;
	}


	/**
	 * Reads a file into a string array, one line per element.
	 * @param filename - the file to read
	 * @return content as string array
	 */
	public static String[] readFromFile (String filename) {
		return readFromFile(new File(filename));
	}


	/**
	 * Reads a file into a string array, one line per element, p to <tt>limit</tt> lines.
	 * @param filename - the file to read
	 * @param limit - read max. <tt>limit</tt> lines
	 * @return content as strin array, with a maximum of <tt>limit</tt> fields
	 */
	public static String[] readFromFile (String filename, int limit) {
		List<String> content = new LinkedList<String>();
		int counter = 0;
		try {
			FileInputStream fis = new FileInputStream(filename);
			BufferedReader in = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
			String line;
			while (((line = in.readLine()) != null) && (counter <= limit)) {
				content.add(line);
				counter++;
			}
			in.close();
			in = null;
			fis.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
		String[] result = new String[content.size()];
		for (int i = 0; i < content.size(); i++) {
			result[i] = (String)content.get(i);
		}
		content.clear();
		content = null;
		return result;
	}


	/**
	 * Reads the given file into a list, one element for each line
	 *
	 * @param filename - file to read from
	 * @return - the file content as a vector
	 */
	public static List<String[]> readFileIntoList (String filename, String lineDelimiter) {
		List<String[]> v = new LinkedList<String[]>();
		try {
			FileInputStream fis = new FileInputStream(filename);
			BufferedReader in = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
			String line;
			while ((line = in.readLine()) != null)
				v.add(line.split(lineDelimiter));
			in.close();
			in = null;
			fis.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
		return v;
	}


	/**
	 * Write the string array into a file, each element on a separate line
	 * @param content
	 * @param filename
	 * @return boolean
	 */
	public static boolean writeToFile (String[] content, String filename) {
		try {
			PrintWriter pw = new PrintWriter(filename, "UTF-8");

			if (content.length > 0) {
				// write all but the last element into the file,
				// with trailing newlines
				for (int i = 0; i < (content.length - 1); i++) {
					pw.write(content[i] + "\n");
				}
				// write the last element, without newline
				pw.write(content[content.length]);
			}

			pw.close();
			pw = null;

		} catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Write the string array into a file, each element on a separate line
	 * @param content
	 * @param filename
	 * @return boolean
	 */
	public static boolean writeSetToFile (Set<String> content, String filename) {
		try {

			List<String> contentList = new ArrayList<String>(content.size());
			for (String string : content) {
				contentList.add(string);
            }
			Collections.sort(contentList);


			PrintWriter pw = new PrintWriter(filename, "UTF-8");

			if (content.size() > 0) {
				// write all but the last element into the file,
				// with trailing newlines
				for (String string : contentList) {
					pw.write(string + "\n");
				}
			}

			pw.close();
			pw = null;

		} catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}

		return true;
	}


	/**
	 * Write the vector into a file, each element on a separate line.
	 * @param content - the file's content as a list
	 * @param filename - the name of the file
	 * @return boolean - true if writing was successful
	 */
	public static boolean writeToFile (List<String> content, String filename) {
		try {

			PrintWriter pw = new PrintWriter(filename, "UTF-8");

			if (content.size() > 0) {
				// write all but the last element into the file,
				// with trailing newlines
				for (int i = 0; i < (content.size() - 1); i++) {
					pw.write(content.get(i) + "\n");
				}
				// write the last element, without newline
				pw.write(content.get(content.size() - 1));
			}

			pw.close();
			pw = null;

		} catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}

		return true;
	}


	/**
	 * Returns the names of all files found in the specified directory
	 *
	 * @param dirname
	 * @return String[]
	 */
	public static String[] getFilenamesFromDirectory (String dirname) {
		File dir = new File(dirname);
		String[] files = dir.list(
			new FilenameFilter() {
				public boolean accept(File dir, String name){
					return true;
				}
			}
		);
		return files;
	}


	/**
	 * Returns the names of all files found in the specified directory. If <tt>recursively</tt>
	 * is true, returns files in subdirectories as well. Uses the current setting for
	 * the file filter expression (change/get it using the methods
	 * <tt>setFilefilterExpression(String)</tt> and <tt>getFilefilterExpression()</tt>), and
	 * returns file names fitting this expression only (no directories etc.).
	 *
	 * @param dirname
	 * @param recursively
	 * @return String[]
	 */
	public static String[] getFilenamesFromDirectory (String dirname, boolean recursively) {
		if (!recursively) {
			File dir = new File(dirname);
			return dir.list(onlyFilenamesFilterWithExpression);
		} else {
			return listFiles(new File(dirname), (List<String>)new ArrayList<String>());
		}
	}


	/**
	 * Returns the names of all files found in the specified directory and matching the
	 * given expression. If <tt>recursively</tt> is true, returns files in subdirectories
	 * as well.
	 *
	 * @param dirname
	 * @param recursively
	 * @param expression
	 * @return String[]
	 */
	public static String[] getFilenamesFromDirectory (String dirname, boolean recursively, String expression) {
		String storeFilter = getFilefilterExpression();
		setFilefilterExpression(expression);
		String[] ret = getFilenamesFromDirectory(dirname, recursively);
		setFilefilterExpression(storeFilter);
		return ret;
	}


	/**
	 * Sets a new value for the default file filter expression.<br>
	 * Default is &quot;.*&quot;
	 *
	 * @param exp
	 */
	public static void setFilefilterExpression (String exp) {
		filefilterExpression = exp;
	}


	/**
	 * Returns the current setting for the file filter expression.
	 *
	 * @return String
	 */
	public static String getFilefilterExpression () {
		return filefilterExpression;
	}


	/**
	 *  Returns an array containing all files in this directory and any subdirectory.
	 * @param directory
	 * @param filenames
	 * @return String[] - list of files
	 */
	public static String[] listFiles (File directory, List<String> filenames) {
		File[] dirFiles = directory.listFiles();
		for (int i = 0; i < dirFiles.length; i++) {
			File file = dirFiles[i];

			if ( ! file.isDirectory() ) {
				filenames.add( file.getAbsolutePath() );
			}
			else {
				listFiles(file, filenames);
			}
		}
		String[] files = new String[filenames.size()];
		return (String[])filenames.toArray(files);
	}


	/**
	 * This file filter accepts only normal files, e.g. no directories. See java.io.File.isFile().
	 */
	static FilenameFilter onlyFilenamesFilter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			File thefile = new File(dir.getAbsolutePath() + "/" + name);
			return thefile.isFile();
		}
	};

	/**
	 * This file filter accepts only normal files that additionally match a file filter expression. Get and set
	 * the current file filter expression with <tt>(g|s)etFilefilterExpression</tt>. Default: &quot;<tt>.*</tt>&quot;
	 */
	static FilenameFilter onlyFilenamesFilterWithExpression = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			File thefile = new File(dir.getAbsolutePath() + "/" + name);
			return (thefile.isFile() && name.matches(filefilterExpression));
		}
	};

	/**
	 * This file filter accepts only directories. See  java.io.File.isDirectory().
	 */
	static FilenameFilter onlyDirectorynamesFilter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			File thefile = new File(dir.getAbsolutePath() + "/" + name);
			return thefile.isDirectory();
		}
	};

	private static String filefilterExpression = ".*";

}

