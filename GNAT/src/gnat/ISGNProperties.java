package gnat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

public class ISGNProperties {

	/** */
	private static final long serialVersionUID = -1379194214752346825L;

	/** */
	private static String FILE_PROPERTIES = "isgn_properties.xml";

	/** */
	private static Properties myProperties = new Properties();

	/** Load properties from file when first instantiated. */
	static {
		try {
			myProperties.loadFromXML(new FileInputStream(new File (FILE_PROPERTIES)));
		} catch (FileNotFoundException e) {
			System.err.println("Unable to load properties file: " + FILE_PROPERTIES + ", make sure to load manually using ISGNProperties.loadProperties(File).");
		} catch (InvalidPropertiesFormatException e) {
			System.err.println("Property file has invalid format.");
			System.exit(2);
		} catch (IOException ioe) {
			System.err.println(ioe.getMessage());
			System.exit(2);
		}
	}
	
	public static void loadProperties(File file){
		try {
			myProperties.loadFromXML(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			System.err.println("Unable to load properties file: " + file.getAbsolutePath());
			System.exit(2);
			//System.err.println("Creating an emtpy properties file.");
		} catch (InvalidPropertiesFormatException e) {
			System.err.println("Property file " + file.getAbsolutePath() + " has invalid format.");
			System.exit(2);
		} catch (IOException ioe) {
			System.err.println(ioe.getMessage());
			System.exit(2);
		}
	}
	
	public static void loadProperties(InputStream is){
		try {
			myProperties.loadFromXML(is);
		} catch (InvalidPropertiesFormatException e) {
			System.err.println("Property file has invalid format.");
			System.exit(2);
		} catch (IOException ioe) {
			System.err.println(ioe.getMessage());
			System.exit(2);
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public static String getPropertyFilename () {
		return FILE_PROPERTIES;
	}
	
	
	/**
	 * 
	 */
	public static Set<Integer> getDefaultSpecies () {
		Set<Integer> taxa = new LinkedHashSet<Integer>();
		String defString = myProperties.getProperty("defaultSpecies");
		if (!defString.matches("\\d+([\\s\\,\\;]+\\d+)*")) {
			System.err.println("List of default species in " + FILE_PROPERTIES + " is in an invalid format. Use a comma-separated list of NCBI taxonomy IDs.");
			return taxa;
		}
		String[] defList = defString.split("[\\s\\,\\;]+");
		for (String d: defList)
			taxa.add(Integer.parseInt(d));
		return taxa;
	}


	/**
	 *
	 * @param key
	 */
	public static String get (String key) {
		return getProperty(key);
	}


	/**
	 *
	 * @param key
	 */
	public static String getProperty (String key) {
		return myProperties.getProperty(key);
	}


	/**
	 *
	 */
	public static void store () {
		String comment = "Date: " + System.currentTimeMillis() + "ms";
		try {
			myProperties.storeToXML(new FileOutputStream(new File(FILE_PROPERTIES)), comment);
		} catch (IOException ioe) {
			System.err.println(ioe.getMessage());
			System.exit(2);
		}
	}


	/**
	 *
	 * @param key
	 * @param value
	 */
	public static void set (String key, String value) {
		setProperty(key, value);
	}


	/**
	 *
	 * @param key
	 * @param value
	 */
	public static void setProperty (String key, String value) {
		myProperties.setProperty(key, value);
	}


	/**
	 *
	 */
	public static void store (String comment) {
		try {
			myProperties.storeToXML(new FileOutputStream(new File(FILE_PROPERTIES)), comment);
		} catch (IOException ioe) {
			System.err.println(ioe.getMessage());
			System.exit(2);
		}
	}


	/**
	 *
	 * @param args
	 */
	public static void main (String[] args) {
		myProperties.setProperty("myname", "test");
		//store();
		System.out.println(get("dbUser"));
	}

}
