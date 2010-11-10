package gnat.tests;

import gnat.ISGNProperties;
import gnat.utils.StringHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.ac.man.entitytagger.Mention;

/**
 * Tests the connection to a Linnaeus instance.
 * <br><br>
 * Sends a fixed short text and compares the received answer to the expected result.<br>
 * Server and port for the Linnaeus instance are set in the entry 'linnaeusUrl' in ISGNProperties.
 * 
 * For more information on Linnaeus, see <a href="http://linnaeus.sourceforge.net">linnaeus.sourceforge.net</a>.
 * <br><br>
 * Start this test with scripts/testLinnaeusConnection.sh
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 *
 */
public class LinnaeusTest {

	/**
	 * 
	 * @param args
	 */
	public static void main (String[] args) {
		System.out.println("Testing the connection to a Linnaeus tagger instance.");
		
		System.out.println("Reading setting for 'linnaeusUrl' from " + ISGNProperties.getPropertyFilename());
		String serverAddr = "localhost";
		int serverPort    = 80;
		String addr  = ISGNProperties.get("linnaeusUrl");
		if (addr.matches(".+\\:\\d+")) {
			serverAddr   = addr.replaceFirst("^(.+)(\\:\\d+)$", "$1");
			String sPort = addr.replaceFirst("^(.+)(\\:\\d+)$", "$2");
			serverPort = Integer.parseInt(sPort.replaceFirst("^\\:(\\d+)$", "$1"));
		} else {
			serverAddr = addr;
			serverPort = 80;
		}
		System.out.println("Should be running on " + serverAddr + ", port " + serverPort);
		
		
		String text = "This sentence talks about human and murine genes.";
		Set<String> expectedResults = new HashSet<String>();
		expectedResults.add("'human', 26..31, ID='species:ncbi:9606'");
		expectedResults.add("'murine', 36..42, ID='species:ncbi:10090'");
		int correctResults = 0;
		Set<String> receivedResults = new HashSet<String>();
		
		try {
			Socket socket = new Socket(serverAddr, serverPort);
			System.out.println("#LT: successfully connected via socket.");
			
			ObjectOutputStream outputStream = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			outputStream.flush();
			
			outputStream.writeObject(text);
			outputStream.writeBoolean(false); // not a Document, otherwise: writeBoolean(doc != null)
			outputStream.flush();			
			System.out.println("#LT: sent '" + text + "'");
			
			
			ObjectInputStream inputStream = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
			List<Mention> matches = (List<Mention>) inputStream.readObject();

			
			System.out.println("#LT: received " + matches.size() + " matches.");
			for (Mention mention: matches) {
				String[] ids = mention.getIds();
				String idString = StringHelper.joinStringArray(ids, ";");
				System.out.println("#LT: received mention '" + mention.getText() + "', " + mention.getStart() + ".." + mention.getEnd() + ", " +
						"ID='" + idString + "'");
				
				String result = "'" + mention.getText() + "', " + mention.getStart() + ".." + mention.getEnd() + ", " +
					"ID='" + idString + "'";
				receivedResults.add(result);
				if (expectedResults.contains(result))
					correctResults++;
			}

			socket.close();
			
			if (expectedResults.size() == matches.size()
				&& correctResults == expectedResults.size())
				System.out.println("Test successful!");
			else {
				System.out.println("Test failed!");
				System.out.println("Expected results (independent of order): ");
				for (String e: expectedResults)
					System.out.println(e);
				System.out.println("-----\nReceived: ");
				for (String r: receivedResults)
					System.out.println(r);
			}
			
			return;
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (java.net.SocketException e) {
			System.err.println("#LT: - Remote server unreachable!");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		System.out.println("Test failed!");
		
	}
}
