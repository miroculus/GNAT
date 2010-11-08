package gnat.filter.ner;

import gnat.ISGNProperties;
import gnat.filter.Filter;
import gnat.representation.Context;
import gnat.representation.GeneContextModel;
import gnat.representation.GeneRepository;
import gnat.representation.Text;
import gnat.representation.TextRepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Adds GO code annotations to texts, loading each annotation from a relational database.
 * <br>
 * Recognized GO codes are stored in each Text's ContextModel, with the model type
 * GeneContextModel.CONTEXTTYPE_GOCODES.
 * <br><br>
 * To perform online GO term recognition, run a RunGoDictionary filter.
 * <br><br>
 * The relational database is specified in the ISGNProperties entries dbUser, dbPass, dbAccessUrl, dbDriver.
 * The name of the table stored in the entry "pubmedToGoCodeTable". Required fields in this table are 
 * 'pubmedId' and 'goCode'.
 * 
 *  
 * @see RunGoDictionary
 * @author Conrad, Joerg
 */
public class GOFilter implements Filter {

	private Connection connection;

	/**
	 * Creates a new GOFilter by opening a connection to the specified database. The database can be
	 * specified via ISGNProperties, see entries for dbUser, dbPass, dbAccessUrl, and dbDriver.
	 * */
	public GOFilter (String dbURL, String user, String password){
		try {
			Class.forName(ISGNProperties.get("dbDriver"));
			connection = DriverManager.getConnection(ISGNProperties.get("dbAccessUrl"),
					ISGNProperties.get("dbUser"), ISGNProperties.get("dbPass"));
		} catch (java.sql.SQLException sqle) {
			sqle.printStackTrace();
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}
	}


	/**
	 * Releases the database connection. Call if this filter is not used anymore.
	 * */
	public void dispose(){
		try {
	        connection.close();
        }
        catch (SQLException e) {
	        e.printStackTrace();
        }
	}

	
	/**
	 *  Adds GO code annotations to texts. GO codes for PMIDs are looked up in a relational database.
	 * */
	public void filter (Context context, TextRepository textRepository, GeneRepository geneRepository) {
		Set<Text> texts = context.getTexts();
		for (Text text : texts) {
			annotateText(text);
        }
	}


	/**
	 *	Annotates this text by looking up its PMID in a database and adding the GO codes for this PMID to the text's context model.
	 * */
	private void annotateText (Text text) {
		try {
	        Statement statement = connection.createStatement();

	        //String query = "SELECTR tids.fragment from termids as tids, recent_annotations_efdb7a68e1eeaf0f0f25c80f5cd50b71 as ra where tids.namespaceid=2 AND tids.termid=ra.termid AND ra.documentid="+text.getID();

	        if (!text.hasPMID()) return;
	        
	        int pmid = text.getPMID();
	        
	        String query = "SELECT goCode from `" + 
	        	ISGNProperties.get("pubmedToGoCodeTable") + "` WHERE pubmedId=" + pmid + "";
	        
	        ResultSet resultSet = statement.executeQuery(query);

	        List<String> goFragments = new LinkedList<String>();
	        while (resultSet.next()){
	        	String fragment = resultSet.getString("goCode");
	        	goFragments.add(fragment);
	        }

		    text.getContextModel().addCodes(goFragments, GeneContextModel.CONTEXTTYPE_GOCODES);

		    //System.out.println(this.getClass().getSimpleName()+": "+goFragmentArray.size()+" codes added for PMID:"+text.getID());

		    statement.close();
        } catch (SQLException e) {
	        e.printStackTrace();
        }
    }

}
