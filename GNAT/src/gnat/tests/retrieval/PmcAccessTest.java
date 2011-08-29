/**
 * 
 */
package gnat.tests.retrieval;

import gnat.retrieval.PmcAccess;

/**
 * 
 * Check that the PmcAccess class works: retrieve articles from PMC via a PMC ID, full texts documents,
 * converted in to plain text.
 * 
 * 
 * @author Joerg Hakenberg
 *
 */
public class PmcAccessTest {

	/**
	 * 
	 * @param args
	 */
	public static void main (String[] args) {
		
		String document = PmcAccess.getArticle(3069089);
		
		System.out.println("Document size:   " + document.length());
	
		String plain = PmcAccess.getPlaintext(document);
		
		System.out.println("Plain text size: " + plain.length());
		
		System.out.println(plain);
		
	}
	
}
