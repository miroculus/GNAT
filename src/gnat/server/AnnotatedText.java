package gnat.server;

import java.util.LinkedList;
import java.util.List;

public class AnnotatedText extends RequestedText {

	/** Stores the annotations for this text. */
	List<String> annotations;
	
	
	/**
	 * Constructs an AnnotatedText based on the source Text, copying
	 * its <tt>id</tt>, <tt>xref</tt>, <tt>title</tt>, and <tt>text</tt> fields.
	 * @param text
	 */
	public AnnotatedText (RequestedText text) {
		this.id    = text.id;
		this.xref  = text.xref;
		this.title = text.title;
		this.text  = text.text;
		annotations = new LinkedList<String>();
	}
	
	
	/**
	 * Adds a single annotation.
	 * @param annotation
	 */
	public void addAnnotation (String annotation) {
		annotations.add(annotation);
	}
	
	
	/**
	 * Returns the list of all current annotations.
	 * @return
	 */
	public List<String> getAnnotations () {
		return this.annotations;
	}
	
	
	/**
	 * 
	 */
	public void clearAnnotations () {
		this.annotations.clear();
	}
	
	
	/**
	 * Returns a tab-separated list of annotations for this text.<br><br>
	 * Format: text-id [tab] text-xref [tab] annotation-type [tab] <em>annotation-data...</em><br>
	 * <em>annotation-data</em> varies for different annotation-types.
	 * For example, for the type <tt>entity</tt>, the data consist of
	 * <ul>
	 * <li>entity type ("gene"),
	 * <li>subtype ("9606" to depiect a gene of species 9606),
	 * <li>ID(s) of this entity,
	 * <li>start position,
	 * <li>end position, and
	 * <li>entity mention (name as it occurs in the text). 
	 * @return
	 */
	public String toTsv () {
		StringBuffer out = new StringBuffer();
		
		for (String annotation: annotations) {
			out.append(this.id);
			out.append("\t");
			out.append(this.xref);
			out.append("\t");
			
			if (annotation.startsWith("<entity")) {
				String type = annotation.replaceFirst("^<entity.* type=\"(.*?)\".*$", "$1");
				if (type.equals(annotation)) out.append("-");
				else out.append(type);
				out.append("\t");

				String subtype = annotation.replaceFirst("^<entity.* subtype=\"(.*?)\".*$", "$1");
				if (subtype.equals(annotation)) out.append("-");
				else out.append(subtype);
				out.append("\t");

				String ids = annotation.replaceFirst("^<entity.* ids=\"(.*?)\".*$", "$1");
				if (ids.equals(annotation)) out.append("-");
				else out.append(ids);
				out.append("\t");

				String startIndex = annotation.replaceFirst("^<entity.* startIndex=\"(.*?)\".*$", "$1");
				if (startIndex.equals(annotation)) out.append("-");
				else out.append(startIndex);
				out.append("\t");

				String endIndex = annotation.replaceFirst("^<entity.* endIndex=\"(.*?)\".*$", "$1");
				if (endIndex.equals(annotation)) out.append("-");
				else out.append(endIndex);
				out.append("\t");
				
				String name = annotation.replaceFirst("^<entity.*>(.*?)</entity>$", "$1");
				if (name.equals(annotation)) out.append("-");
				else out.append(name);				
				out.append("\t");
			
				String score = annotation.replaceFirst("^<entity.* score=\"(.*?)\".*$", "$1");
				if (score.equals(annotation)) out.append("-");
				else out.append(score);
				out.append("\n");

			// TODO handle other types of annotations
			} else {
				out.append(annotation);
				out.append("\n");
			}
		}
		
		return out.toString();
	}
	
	
	/**
	 * Returns an XML-formatted copy of this text and its annotations.<br><br>
	 * The text is wrapped in <tt>&lg;text&gt;</tt> tags, with <tt>id</tt> and <tt>xref</tt>
	 * as attributes. The <tt>&lg;title&gt;</tt> will always be includes, as well as
	 * <tt>&lg;annotations&gt;</tt>, if available. The <tt>&lg;plaintext&gt;</tt> will
	 * be included according to the value of <tt>includeFullText</tt>.
	 * @param includeFullText - include the full text in the XML or not
	 * @return
	 */
	public String toXml (boolean includeFullText) {
		StringBuffer out = new StringBuffer();
		
		out.append("<text id=\"");
		out.append(this.id);
		out.append("\" xref=\"");
		out.append(this.xref);
		out.append("\">\n");
		
		if (this.title != null && this.title.length() > 0) {
			out.append("<title>");
			out.append(this.title);
			out.append("</title>\n");
		}
		
		if (includeFullText && this.text != null && this.text.length() > 0) {
			out.append("<plaintext>");
			out.append(this.text);
			out.append("</plaintext>\n");
		}
		
		if (this.annotations.size() > 0) {
			out.append("<annotations>\n");
			for (String annotation: this.getAnnotations()) {
				out.append(annotation);
				out.append("\n");
			}
			out.append("</annotations>\n");
		}
		
		out.append("</text>");
		
		return out.toString();
	}
}
