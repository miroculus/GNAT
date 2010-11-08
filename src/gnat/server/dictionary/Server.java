package gnat.server.dictionary;

import java.net.ServerSocket;

/**
 * 
 * 
 * 
 * @author Joerg
 *
 */
public abstract class Server implements Runnable {

	ServerSocket serverSocket;
	boolean stopped = false;
	int serverPort;
	long starttime;


	/**
	 * 
	 */
	public void status () {
		if (stopped)
			System.out.println("Server has been stopped.");
		System.out.println("Server has been running on port " + serverPort + " for " + (System.currentTimeMillis() - starttime) + "ms.");
	}


	/**
	 * 
	 */
	public void stop () {
		try {
			serverSocket.close();
			stopped = true;
		} catch (java.io.IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
