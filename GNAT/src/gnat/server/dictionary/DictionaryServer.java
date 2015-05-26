package gnat.server.dictionary;

import gnat.ISGNProperties;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;

import org.jdom.JDOMException;


/**
 * A dictionary server that runs as a stand-alone application. Incoming requests are annotated using a dictionary.
 * <br><br>
 * A requested text must be enclosed by "<text> ... </text>". Multiple texts in one request are allowed.
 * The request must not contain more than one line, that is, no line breaks are allowed.
 * <br><br>
 * For each text, a list of entities (one list per line) will be returned. Each entity is represented
 * by an 'entity' element, with attributes 'ids', 'startIndex', and 'endIndex'; the actual match is
 * stored as the content of the entity element; example:<br>
 * &nbsp; {@code <entity ids="123;789" startIndex="51" endIndex="53">p21</entity>}<br>
 * This encoding of entities is done by {@link DictionaryActor#act(char[], int, int, StringBuffer)},
 * and at least additional attributes might be added in future releases.
 * 
 * @author Conrad, Joerg
 *
 * */
public class DictionaryServer extends Server {

	private Dictionary dictionary;
	private int logLevel = 0;
	protected static String stopPassphrase = "ShutDownNow.";
	static {
		if (ISGNProperties.get("stopDictPassphrase") != null) stopPassphrase = ISGNProperties.get("stopDictPassphrase");
	}

	public DictionaryServer(int port, String dictionaryDir) throws ClassCastException, IOException, ClassNotFoundException
	{
		this(port, new Dictionary(dictionaryDir));
	}

	public DictionaryServer(int port, Dictionary dictionary) throws IOException
	{
		this.dictionary = dictionary;
		serverSocket = new ServerSocket(port);
		serverSocket.setSoTimeout(1000);
	}

	public void run() {
		stopped = false;
		while (!stopped) {
			try {
				Socket socket = serverSocket.accept();
				ServiceThread serviceThread = new ServiceThread(socket);
				serviceThread.start();
				serviceThread.logLevel = logLevel;
				if (logLevel > 0)
					System.out.println("DictionaryServer.run(): new client logged on");
			}
			catch (java.io.IOException ioe) { // ignore
			}
		}
	}

	private class ServiceThread extends Thread
	{
		Socket socket;
		BufferedReader bufferedReader;
		BufferedWriter bufferedWriter;
		int logLevel = 0;

		public ServiceThread(Socket socket) throws IOException
		{
			this.socket = socket;
			this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
			this.bufferedWriter = new BufferedWriter( new OutputStreamWriter( new BufferedOutputStream( socket.getOutputStream()), "UTF-8") );
		}

		public void run()
		{
			try {
				// read input text
				String input = bufferedReader.readLine();
				if (input != null) {

					if (input.equals("<stop passphrase=\"" + stopPassphrase + "\" />")) {
						System.out.println("Server was asked to stop. Passphrase correct. Exiting.");
						System.exit(0);
					}
					
					StringBuffer outputBuffer = new StringBuffer();

					int textTagBeginIndex = input.indexOf("<text");
					while (textTagBeginIndex != -1) {
						int textTagEndIndex = input.indexOf("</text>", textTagBeginIndex);

						if (textTagEndIndex < 0) {
							System.out.println("Bad text ending: </text> is missing. Stopping annotation for this request.");
							break;
						}

						String singleText = input.substring(textTagBeginIndex, textTagEndIndex);
						singleText = singleText.substring(singleText.indexOf(">") + 1);
						Set<String> entries = dictionary.getIdentifiedEntries(singleText);
						outputBuffer.append("<text>");
						for (String string : entries) {
							outputBuffer.append(string);
						}
						outputBuffer.append("</text>");

						textTagBeginIndex = input.indexOf("<text", textTagEndIndex);
					}

					// write back
					bufferedWriter.write(outputBuffer.toString());
					bufferedWriter.newLine();
					bufferedWriter.flush();
				}

			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Stopping service thread");
			}
			
			// close
			try {
				if (logLevel > 0)
					System.out.println("DictionaryServer.ServiceThread: closing connection to client.");
				bufferedReader.close();
				bufferedWriter.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	static final int MIN_PORT_NUMBER = 100;
	static final int MAX_PORT_NUMBER = 100000;


	/**
	 * Check if the specified port is available on localhost.
	 * @param port the port to check for availability
	 */
	public static boolean port_available (int port) {
	    if (port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
	        throw new IllegalArgumentException("Dictionary server:" +
	        		" invalid start port: " + port +
	        		", has to be between " + MIN_PORT_NUMBER + " and " + MAX_PORT_NUMBER);
	    }

	    ServerSocket ss = null;
	    DatagramSocket ds = null;
	    try {
	        ss = new ServerSocket(port);
	        ss.setReuseAddress(true);
	        ds = new DatagramSocket(port);
	        ds.setReuseAddress(true);
	        return true;
	    } catch (IOException e) {
	    } finally {
	        if (ds != null) {
	            ds.close();
	        }

	        if (ss != null) {
	            try {
	                ss.close();
	            } catch (IOException e) {
	                /* should not be thrown */
	            }
	        }
	    }

	    return false;
	}
	
	
	/**
	 *
	 * @param args
	 * @throws IOException
	 * @throws JDOMException
	 * @throws ClassNotFoundException
	 * @throws ClassCastException
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 2 || args.length > 3) {
			System.out.println("Usage: java DictionaryServer <port> <automataDirectory> {-v=<verbosity>} {-pass=<passphrase>}");
			System.out.println("Parameters:");
			System.out.println("  port               -  port that the dictionary server will be listening on");
			System.out.println("  automataDirectory  -  path to pre-compiled finite state automaton");
			System.out.println("Options:");
			System.out.println("  -v                 -  log level, default: 0");
			System.out.println("  -pass              -  passphrase to stop a running dictionary server");
			System.exit(1);
		}
		
		int port = Integer.parseInt(args[0]);
		
		if (!port_available(port)) {
			System.err.println("Cannot start dictionary server: port " + port + " is already in use.");
			System.exit(2);
		}
		
		File DIR = new File(args[1]);
		if (!DIR.exists() || !DIR.canRead()) {
			System.err.println("#ERROR cannot access directory " + args[1]);
			System.exit(2);
		}
		
		DictionaryServer dictionaryServer = new DictionaryServer(port, args[1]);
		if (args.length > 2) {
			for (int a = 2; a < args.length; a++) {
				//String log = args[2];
				if (args[a].matches("\\-\\-?v(erbosity)?=(\\d+)")) {
					dictionaryServer.logLevel = Integer.parseInt(args[a].replaceFirst("^\\-\\-?v(erbosity)?=(\\d+)$", "$2"));	
				} else if (args[a].matches("\\-\\-?p(assphrase)?=(.+)")) {
					dictionaryServer.stopPassphrase = args[a].replaceFirst("^\\-\\-?p(assphrase)?=(.+)$", "$2");	
				}
			}
		}		

		Thread thread = new Thread(dictionaryServer);
		thread.start();
	}

}

