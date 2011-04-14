package gnat.filter.ner;

import gnat.ISGNProperties;
import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneRepository;
import gnat.representation.RecognizedEntity;
import gnat.representation.Text;
import gnat.representation.TextAnnotation;
import gnat.representation.TextRange;
import gnat.representation.TextRepository;
import gnat.utils.StringHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import uk.ac.man.entitytagger.Mention;

/**
 * Contacts a running instance of LINNAEUS to annotate species.
 * 
 * 
 * 
 * See <a href="http://linnaeus.sourceforge.net">linnaeus.sourceforge.net</a>.
 * 
 * 
 * @author J&ouml;rg Hakenberg &lt;jhakenberg@users.sourceforge.net&gt;
 *
 */
public class LinnaeusSpeciesServiceNer implements Filter {
	
	String serverAddr;
	int serverPort;
	
	public LinnaeusSpeciesServiceNer () {
		String addr  = ISGNProperties.get("linnaeusUrl");
		if (addr.matches(".+\\:\\d+")) {
			serverAddr   = addr.replaceFirst("^(.+)(\\:\\d+)$", "$1");
			String sPort = addr.replaceFirst("^(.+)(\\:\\d+)$", "$2");
			serverPort = Integer.parseInt(sPort.replaceFirst("^\\:(\\d+)$", "$1"));
		} else {
			serverAddr = addr;
			serverPort = 80;
		}
		//System.out.println("sAddr=" + serverAddr + ", sPort="+serverPort);			
	}
	

	@Override
	public void filter(Context context, TextRepository textRepository, GeneRepository geneRepository) {
		// TODO should use the same socket + streams over and over instead of creating new ones
		
		try {
			for (Text text: textRepository.getTexts()) {
				
				Socket socket = new Socket(serverAddr, serverPort);
				//System.out.println("#LT has socket.");
				
				
				ObjectOutputStream outputStream = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
				outputStream.flush();
				
				String plainText = text.getPlainText();
				//plainText = plainText.replaceAll("[\\r\\n]", " ");
				//plainText = "This is a text about humans and mice.";
				
				outputStream.writeObject(plainText);
				outputStream.writeBoolean(false); // not a Document, otherwise: writeBoolean(doc != null)
				outputStream.flush();
				
				//System.out.println("#LSSN: sent '" + plainText.substring(0, 25) + " [..]'");
				
				//String annotations = (String) reader.readObject();
				
				ObjectInputStream inputStream = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
				List<Mention> matches = (List<Mention>) inputStream.readObject();

				inputStream.close();
				
				//System.out.println("#LSSN: received " + matches.size() + " matches.");
				for (Mention mention: matches) {
					String[] ids = mention.getIds();
					for (int i = 0; i < ids.length; i++) {
						String id = ids[i];
						// IDs from Linnaeus are in the format 'species:ncbi:9606'
						id = id.replaceFirst("^.*?(\\d+)$", "$1");
						ids[i] = id;
					}
					String idString = StringHelper.joinStringArray(ids, ";");
					String annotation = "mention=\"" + mention.getText() + "\" ids=\"" + idString + "\" startIndex=\"" +
					mention.getStart() + "\" endIndex=\"" + mention.getEnd() + "\"";
					
					addRecognizedSpecies(context, text, annotation);
				}
				
				//outputStream.close();
				socket.close();

			}
			
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (java.net.SocketException e) {
			e.printStackTrace();
			System.err.println("#LT: - Remote server unreachable!");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	
	/**
	 * Adds an entity described in <tt>annotation</tt> to the <tt>context</tt>.
	 * <br>
	 * Format of annotations: tab-separated, as returned by the online GnatService, with fields for
	 * text ID, text cross-reference (source), entity tye (=gene), entity subtype (=species of the gene),
	 * entity candidate ID(s) [semi-colon separated], start position, end position, mention as found in the text.
	 * 
	 * 
	 * @param annotation
	 */
	void addRecognizedSpecies (Context context, Text text, String annotation) {
		if (annotation.startsWith("<error")) return;
		
		String evidence   = annotation.replaceFirst("^.*(?:^|\\s)mention=\"([^\"]*)\".*$", "$1");
		String idString   = annotation.replaceFirst("^.*(?:^|\\s)ids=\"([^\"]*)\".*$", "$1");
		String s_startIndex = annotation.replaceFirst("^.*(?:^|\\s)startIndex=\"([^\"]*)\".*$", "$1");
		String s_endIndex   = annotation.replaceFirst("^.*(?:^|\\s)endIndex=\"([^\"]*)\".*$", "$1");
		
		int startIndex = Integer.parseInt(s_startIndex);
		int endIndex   = Integer.parseInt(s_endIndex);
		
		TextRange position = new TextRange(startIndex, endIndex);
		TextAnnotation.Type eType = TextAnnotation.Type.SPECIES;
		String[] ids = idString.split("\\s*[\\;\\,]\\s*");
		for (String id: ids) {
			if (id.matches("\\d+"))
				text.addTaxonWithName(Integer.parseInt(id), evidence);
			else
				System.err.println("The species ID " + id + " must be numeric.");
		}
		
		RecognizedEntity recognizedGeneName = new RecognizedEntity(text, new TextAnnotation(position, evidence, eType));
		context.addRecognizedEntity1(recognizedGeneName, ids);
	}

}
