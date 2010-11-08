package gnat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

public class ServiceProperties {

	/** */
	private static final long serialVersionUID = -1379316294752164047L;

	/** */
	private static String FILE_PROPERTIES = "service_properties.xml";
	
	/** */
	private static Properties myProperties = new Properties();

	/** Load properties from file when first instantiated. */
	static {
		try {
			myProperties.loadFromXML(new FileInputStream(new File (FILE_PROPERTIES)));
		} catch (FileNotFoundException e) {
			System.err.println("Unable to load properties file: " + FILE_PROPERTIES);
			System.exit(2);
			//System.err.println("Creating an emtpy properties file.");
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
}
