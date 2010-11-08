package gnat.server;

/**
 * Represents a text from a user request.
 * <br><br>
 * If the text was requested via PubMed ID, PubMedCentral ID, etc., the RequestedText will contain the respective IDs and
 * sources.
 * <br><br>
 * TODO Redundant: should use gnat.representation.Text instead of a this new text object
 * 
 * @author Joerg
 *
 */
public class RequestedText {
	/** ID of this text, as used in the database <tt>xref</tt>. */
	public String id;
	/** Name of the database the ID refers to: PubMed, Medline, PMC. */
	public String xref;
	/** The actual text. */
	public String text;
	/** Title of the text, if any. */
	public String title;
	
	public RequestedText () { }
	
	public RequestedText (String id, String xref, String text) {
		this.id = id;
		this.xref = xref;
		this.text = text;
		this.title = "Unknown title.";
	}
	
	public RequestedText (String id, String xref, String title, String text) {
		this(id, xref, text);
		this.title = title;
	}
}
