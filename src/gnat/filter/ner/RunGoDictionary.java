package gnat.filter.ner;

import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneContextModel;
import gnat.representation.GeneRepository;
import gnat.representation.Text;
import gnat.representation.TextContextModel;
import gnat.representation.TextRepository;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * Runs a local or remote Dictionary tagger for GeneOntology terms on each text.<br>
 * Recognized GO codes are stored in each Text's ContextModel, with the model type
 * GeneContextModel.CONTEXTTYPE_GOCODES.
 * <br><br>
 * If static annotations of PubMed IDs etc. to GO codes are available, use a GOFilter to
 * load these from a relational database.
 * 
 * @see GOFilter
 *
 * @author Conrad, Joerg
 */
public class RunGoDictionary implements Filter {

	private String serverName;
	private int serverPort;


	/**
	 * Creates a recognizer that calls the given server at the given port.
	 * */
	public RunGoDictionary(String serverName, int serverPort) {
		this.serverName = serverName;
		this.serverPort = serverPort;
	}

	/**
	 * Processes all texts in this context by calling the remote server and adds GO codes found in each text to its context model.
	 * */
	public void filter(Context context, TextRepository textRepository, GeneRepository geneRepository) {
		filter(context.getTexts());
	}

	/**
	 * Processes all texts in the text repository by calling the remote server and adds the outcome, i.e. recognized entities and candidate ids, to the context.
	 * */
	private void filter (Collection<Text> texts) {
		try {

			Socket socket = new Socket(serverName, serverPort);
			BufferedReader dictionaryReader = new BufferedReader( new InputStreamReader( new BufferedInputStream(socket.getInputStream() ), "UTF-8") );
			BufferedWriter dictionaryWriter = new BufferedWriter( new OutputStreamWriter( new BufferedOutputStream( socket.getOutputStream()), "UTF-8") );

			StringBuffer buffer = new StringBuffer();
			for (Text text : texts) {
				buffer.append("<text>");
				buffer.append(text.getPlainText());
				buffer.append("</text>");
			}

			dictionaryWriter.write(buffer.toString());
			dictionaryWriter.newLine();
			dictionaryWriter.flush();
			String annotatedTexts = dictionaryReader.readLine();

			//System.out.println("#RGO: annot. text is '" + annotatedTexts + "'");
			
			List<String> entities = this.extractEntityTags(annotatedTexts);
			for (Text text : texts) {
				String entityString = entities.remove(0);
				addGOTermAccessions(text, entityString);
			}

			dictionaryReader.close();
			dictionaryWriter.close();
			socket.close();
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
		}
		catch (java.net.SocketException e) {
			System.err.println("RemoteEntityRecognition::filter() - Remote dictionary server unreachable!");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Returns a list of entity mark-ups found in annotated texts.
	 * */
	private List<String> extractEntityTags(String annotatedTexts)
	{
		List<String> entities = new LinkedList<String>();

		int textTagBeginIndex = annotatedTexts.indexOf("<text");
		while (textTagBeginIndex != -1) {
			int textTagEndIndex = annotatedTexts.indexOf("</text>", textTagBeginIndex);
			String singleAnnotatedText = annotatedTexts.substring(textTagBeginIndex, textTagEndIndex);
			String annotations = singleAnnotatedText.substring(singleAnnotatedText.indexOf(">") + 1);
			if (annotations == null || annotations.length() == 0) {
				entities.add(" ");
			}
			else {
				entities.add(annotations);
			}
			textTagBeginIndex = annotatedTexts.indexOf("<text", textTagEndIndex);
		}

		return entities;
	}


	/**
	 * Adds entities found as markup in the given annotated text to the context.
	 * */
	private void addGOTermAccessions(Text text, String annotatedText)
	{
		List<String> goCodes = new ArrayList<String>();

		int geneTagBeginIndex = annotatedText.indexOf("<entity");
		while (geneTagBeginIndex != -1) {
			int geneTagEndIndex = annotatedText.indexOf("</entity>", geneTagBeginIndex);
			String annotatedTerm = annotatedText.substring(geneTagBeginIndex, geneTagEndIndex);
			String term = annotatedTerm.substring(annotatedTerm.indexOf(">") + 1);
			int idOpenQuot = annotatedTerm.indexOf("\"");
			int idCloseQuot = annotatedTerm.indexOf("\"", idOpenQuot + 1);
			int startIndexOpenQuot = annotatedTerm.indexOf("\"", idCloseQuot + 1);
			int startIndexCloseQuot = annotatedTerm.indexOf("\"", startIndexOpenQuot + 1);
			int endIndexOpenQuot = annotatedTerm.indexOf("\"", startIndexCloseQuot + 1);
			int endIndexCloseQuot = annotatedTerm.indexOf("\"", endIndexOpenQuot + 1);
			int startIndex = Integer.parseInt(annotatedTerm.substring(startIndexOpenQuot+1, startIndexCloseQuot));
			int endIndex = Integer.parseInt(annotatedTerm.substring(endIndexOpenQuot+1, endIndexCloseQuot));

			String idString = annotatedTerm.substring(idOpenQuot + 1, idCloseQuot);
			String[] ids = idString.split(";");
			for (String id : ids) {
				if (id.startsWith("G")) {
					Integer intId = Integer.parseInt( id.substring(1) );
					goCodes.add(""+intId);
				}
            }

			geneTagBeginIndex = annotatedText.indexOf("<entity", geneTagEndIndex);
		}

		TextContextModel textContextModel = text.getContextModel();
		textContextModel.addCodes(goCodes,  GeneContextModel.CONTEXTTYPE_GOCODES);
	}
}
