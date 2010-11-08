/*
 * Created on 13.12.2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

package gnat.retrieval;

import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Provides methods to access internet URLs.<br>
 * 
 * <pre>
 * ResourceHelper rh = new ResourceHelper(&quot;true&quot;, &quot;my.proxy.com&quot;, &quot;3128&quot;);
 * 
 * String webpage = rh.getURLContent(&quot;http://sun.java.com&quot;);
 * // .. do something with the content ..
 * </pre>
 * 
 * @author Conrad Plake, Joerg Hakenberg, Marcus Pankalla
 * 
 */

public class ResourceHelper {

	private String proxySet = "true";

	private String proxyHost = "141.20.27.175";

	private String proxyPort = "3128";

	public String userAgent = "Mozilla/1.0 Linux";

	public String httpReferer = "http://www.ask.com/";

	public String httpAgent = "ResourceHelper/0.90 Linux";

	/**
	 * 
	 * Constructs a new ResourceHelper and (re)sets the system properties. See
	 * <tt>resetSystemProperties</tt>.
	 */
	public ResourceHelper() {
		//this.resetSystemProperties();
	}

	/**
	 * Constructs a new ResourceHelper and (re)sets the system properties if
	 * <tt>resetSystemProperties</tt> is set to true.
	 * 
	 * @param resetSystemProperties
	 */
	public ResourceHelper(boolean resetSystemProperties) {
		if (resetSystemProperties)
			this.resetSystemProperties();
	}

	/**
	 * Constructs a new ResourceHelper according to the given settings for a
	 * proxy.
	 * 
	 * @param proxySet -
	 *            either string &quot;true&quot; or &quot;false&quot;
	 * @param proxyHost -
	 *            hostname of IP of the proxy
	 * @param proxyPort -
	 *            port number where to contact the proxy
	 */
	public ResourceHelper(String proxySet, String proxyHost, String proxyPort) {
		this.proxySet = proxySet;
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
		this.resetSystemProperties();
	}


	/**
	 * Opens an HttpURLConntection to the specified URL and gets the content
	 * located there.
	 * 
	 * @param URL
	 *            the URL to fetch
	 * @return String - the URL's content
	 */
	public String getURLContent (String URL) {
		int runs = 2;
		StringBuffer content = new StringBuffer();
		for (int r = 0; r < runs; r++) {
			try {
				URL url = new URL(URL);
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				DataInputStream di = null;
				di = new DataInputStream(con.getInputStream());
	
				LineNumberReader lnr = new LineNumberReader(new InputStreamReader(di));
				String line = "";
				while ((line = lnr.readLine()) != null)
					content.append(line + "\n");
				lnr.close();

				con.disconnect();
				break;
			} catch (Exception e) {
				System.err.println(e.getMessage());
				System.err.println("For URL: '" + URL + "'");
				if (r < runs)
					System.err.println("Trying again...");
			}
		}
		return content.toString();
	}


	/**
	 * Opens an HttpURLConntection to the specified URL and gets the content
	 * located there.
	 * 
	 * @param URL
	 *            the URL to fetch
	 * @param readTimeOut
	 *            connection time out
	 * @return String - the URL's content
	 */
	public String getURLContent (String URL, int readTimeOut) {
		StringBuffer content = new StringBuffer();
		try {
			URL url = new URL(URL);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setReadTimeout(readTimeOut);
			DataInputStream di = null;
			di = new DataInputStream(con.getInputStream());
			LineNumberReader lnr = new LineNumberReader(new InputStreamReader(di));
			String line = "";
			while ((line = lnr.readLine()) != null)
				content.append(line + "\n");
			lnr.close();
			con.disconnect();
		} catch (java.net.SocketTimeoutException se) {
			System.err.println("#RH: connection timeout for " + URL);
		} catch (Exception e) {
			System.err.println("#RH: error fetching content from " + URL);
		}
		return content.toString();
	}

	/**
	 * Resets the system properties and overwrites them with the values
	 * currently set for this ResourceHelper. This overwrites
	 * <tt>proxySet, proxyHost, proxyPort, httpclient.useragent, http.referer, http.agent</tt>.<br>
	 * It might be necessary to call this method whenever other classes/programs
	 * set other values.
	 */
	public void resetSystemProperties() {
		System.getProperties().put("proxySet", proxySet);
		System.getProperties().put("proxyHost", proxyHost);
		System.getProperties().put("proxyPort", proxyPort);
		System.getProperties().put("httpclient.useragent", userAgent);
		System.getProperties().put("http.referer", httpReferer);
		System.getProperties().put("http.agent", httpAgent);
	}

	/**
	 * Clears all proxy settings, so that a direct connection to the internet
	 * can be established.
	 */
	public void setDirectConnection() {
		setProxySet("false");
		setProxyHost("");
		setProxyPort("");
	}

	/**
	 * Toggles the system parameter <tt>proxySet</tt>, either
	 * &quot;true&quot; (default) or &quot;false&quot;. This tells the system
	 * whether to use a proxy or not. Set the proxy using setProxyHost(String)
	 * and setProxyPort(String).
	 * 
	 * @param set -
	 *            either true or false, as a String
	 */
	public void setProxySet(String set) {
		proxySet = set;
	}

	/**
	 * Sets a new value for the system property <tt>proxyHost</tt>. Might be
	 * a URL, IP, or &quot;localhost&quot;, depending on your systems
	 * environment.
	 * 
	 * @param host -
	 *            the new proxy host, a URL or IP
	 */
	public void setProxyHost(String host) {
		proxyHost = host;
	}

	/**
	 * Sets a new value for the system property <tt>proxyPort</tt>,
	 * corresponding to <tt>proxyHost</tt>.
	 * 
	 * @param port -
	 *            the new proxy port
	 */
	public void setProxyPort(String port) {
		proxyPort = port;
	}

	/**
	 * Returns the current setting for the system property <tt>proxySet</tt>.
	 * 
	 * @return current setting
	 */
	public String getProxySet() {
		return proxySet;
	}

	/**
	 * Returns the current setting for the system property <tt>proxHost</tt>.
	 * 
	 * @return current proxy host
	 */
	public String getProxyHost() {
		return proxyHost;
	}

	/**
	 * Returns the current setting for the system property <tt>proxyPort</tt>.
	 * 
	 * @return current proxy port
	 */
	public String getProxyPort() {
		return proxyPort;
	}

}