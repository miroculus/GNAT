package gnat.server;

import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpServer;

public abstract class HttpService {

	HttpServer server;
	int logLevel = 0;
	
	enum Modes {STATUS, START, STOP};

	
	/**
	 * Print out status information: port, uptime, etc.
	 */
	public void status () {
		System.out.println("Service status: ");
		// server.getAddress();
	}
	
	
	/**
	 * 
	 */
	public void stop () {
		// stop only if stop request comes from same IP
		
		server.stop(30);
		System.out.println("Server was stopped.");
	}
	
	
	/** Sets the verbosity for status output. */
	public void setLogLevel (int level) {
		logLevel = level;
	}
}



/**
 * Represents a user query, with key/value pairs for all parameters
 * in a GET or POST request.
 * <br><br>
 * All keys are handled case-insensitive, but not the values.
 */
class Request {
	Map<String, String> pairs = new HashMap<String, String>();
	public boolean hasParameter (String key) {
		return pairs.containsKey(key.toLowerCase());
	}
	public String getValue (String key) {
		if (hasParameter(key.toLowerCase()))
			return pairs.get(key.toLowerCase());
		else return null;
	}
	/**
	 * Set the <tt>value</tt> for <tt>key</tt> in this request.<br>
	 * Overwrites older values.<br>
	 * <tt>key</tt> cannot be null or empty, or it will be ignored.
	 * @param key
	 * @param value
	 */
	public void setValue (String key, String value) {
		if (key != null && key.length() > 0)
			pairs.put(key.toLowerCase(), value);
	}
	/**
	 * Used for boolean parameters, sets the value of the <tt>key</tt> to "1".<br>
	 * <tt>key</tt> cannot be null or empty, or it will be ignored.
	 * @param key
	 */
	public void setValue (String key) {
		if (key != null && key.length() > 0)
			pairs.put(key.toLowerCase(), "1");
	}
	public void clear() {
		pairs.clear();
	}
	public void removeParameter (String key) {
		pairs.remove(key.toLowerCase());
	}
	/** Checks if this request has any parameters at all. */
	public boolean isEmpty () {
		return pairs.size() == 0;
	}
	/**
	 * Checks whether the given parameter exists and is set to 
	 * 'true', 'yes', '1', or 'on' (case-insensitive).
	 * @param parameter
	 * @return
	 */
	public boolean isTrue (String key) {
		if (!hasParameter(key)) return false;
		return getValue(key).toLowerCase().matches("(true|yes|y|1|on)");
	}
	/**
	 * Prints all current key/value pairs to STDOUT.
	 */
	public void printAll () {
		for (String key: pairs.keySet())
			System.out.println(key + "=" + pairs.get(key));
	}
}
