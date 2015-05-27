package gnat.tests;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Test a single dictionary server by sending a text and printing the server's response.
 * <br><br>
 * Assumes the server is running on 'localhost' and requires a port to connect to. For now,
 * will send a short default plain text for annotation, which contains a few gene symbols
 * and GO terms.
 * 
 * 
 * @author Joerg Hakenberg
 */
public class TestDictionaryServer {

	public static void main (String[] args) {
		
		String plaintext = "Apoptosis inhibitor TRIAP1 is a novel effector of drug resistance. Novel-miR-144 (miR-98), was found to target three apoptotic genes (TP53, CASP3, FASL).";
		
		StringBuffer buffer = new StringBuffer();
		buffer.append("<text>");
		buffer.append(plaintext);
		buffer.append("</text>");
		
		String serverName = "localhost";
			
		if (args.length == 0 || !args[0].matches("\\d+")) {
			System.err.println("Need dictionary server port as parameter!");
			System.exit(1);
		}
		
		int serverPort = Integer.parseInt(args[0]);
		
		// open a socket, send the text, receive annotations
		Socket socket;
		BufferedReader dictionaryReader;
		BufferedWriter dictionaryWriter;
		try {
			socket = new Socket(serverName, serverPort);
			dictionaryReader = new BufferedReader(new InputStreamReader(
					new BufferedInputStream(socket.getInputStream() ), "UTF-8") );
			dictionaryWriter = new BufferedWriter(new OutputStreamWriter(
					new BufferedOutputStream(socket.getOutputStream()), "UTF-8") );

			// send the text to the server ...
			dictionaryWriter.write(buffer.toString());
			dictionaryWriter.newLine();
			dictionaryWriter.flush();
			
			// ...and read the response
			String annotatedTexts = dictionaryReader.readLine();
			System.out.println("#Annotations as sent by the server:\n" + annotatedTexts);
			
			dictionaryReader.close();
			dictionaryWriter.close();
			socket.close();
		} catch (UnknownHostException e) {
			System.err.println("#RunDictionaries: " + e.getMessage());
		} catch (java.net.SocketException e) {
			System.err.println("#RunDictionaries: Remote dictionary server unreachable!" +
				" [" + serverName + ":" + serverPort + "]");
		} catch (IOException e) {
			System.err.println("#RunDictionaries: " + e.getMessage());
		}
	}
}
