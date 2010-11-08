package gnat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public abstract class ServiceHandler implements HttpHandler {

	int logLevel = 0;
	
	
	/**
	 * Parses the request (GET or POST) and extracts key/value pairs stored in a {@link Request} object.
	 * @param exchange
	 * @return
	 */
	public Request getQuery (HttpExchange exchange) {
		Request query = null;
		
		String requestMethod = exchange.getRequestMethod();
		// GET request?
		if (requestMethod.equalsIgnoreCase("GET")) {
			URI requestedUri = exchange.getRequestURI();
	        String queryString = requestedUri.getRawQuery();
			
	        if (queryString != null) {
				//System.out.println("--accepted valid GET request");
		        //System.out.println("--query: " + query);
		        query = parseQuery(queryString);
	        	
	        }
			
		// POST request?
		} else if (requestMethod.equalsIgnoreCase("POST")) {
			InputStream request = exchange.getRequestBody();
			if (request != null) {
				StringBuffer buffer = new StringBuffer();
				
				try {
					Reader reader = new BufferedReader(new InputStreamReader(request, "UTF-8"));
					int n;
					while ((n = reader.read()) != -1) {
						buffer.append((char)n);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				query = parseQuery(buffer.toString());
				
			} else {
				//System.out.println("Empty request.");
			}
					
		// not a GET or POST request?
		} else {
			return null;
		}
		
		return query;
	}
	
	
	/**
	 * 
	 * @param query
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public Request parseQuery (String query) {
		Request retQuery = new Request();

		if (query != null) {
			String[] pairs = query.split("[&]");
			for (String pair: pairs) {
				String param[] = pair.split("[=]");

				String key = null;
				String value = null;

				try {
					if (param.length > 0) {
						key = URLDecoder.decode(param[0],
								System.getProperty("file.encoding"));
					}

					if (param.length > 1) {
						value = URLDecoder.decode(param[1],
								System.getProperty("file.encoding"));
					}
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					return null;
				}

				if (key != null && value != null) {
					retQuery.setValue(key, value);
				} else if (key != null) {
					retQuery.setValue(key, "true");
				}
			}
		}

		return retQuery;
	}
}


