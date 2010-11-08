package gnat.representation;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a gene: ID, names, and annotations in a ContextModel.
 * 
 * 
 * @author Joerg
 *
 */
public class Gene implements Serializable {

	private static final long serialVersionUID = -1560208725952346825L;
	
	/** Set of known names and synonyms for this gene. */
	private Set<String> names = new HashSet<String>();
	
	public String officialSymbol = "";


	/** */
	public String ID = "-1";

	/** */
	int taxon = -1;

	/** */
	private GeneContextModel model;


	/** */
	boolean hasModel = false;


	/***/
	public Gene(){
	}

	/***/
	public Gene(String id){
		this.ID = id;
	}

	/***/
	public Gene(String id, GeneContextModel geneContextModel){
		this.ID = id;
		this.setContextModel(geneContextModel);
	}

	/**
	 *
	 * @return
	 */
	public GeneContextModel getContextModel () {
		return model;
	}

	/***/
	public void addName(String name){
		names.add(name);
	}

	public Set<String> getNames(){
		return names;
	}


	public void setNames(Set<String> names){
		this.names = names;
	}

	public void addNames(String[] names){
		for (String name: names)
			addName(name);
	}
	
	public void setTaxon (int tax) {
		this.taxon = tax;
	}
	
	public int getTaxon () {
		return taxon;
	}

	/**
	 *
	 * @return
	 */
	public boolean hasModel () {
		return hasModel;
	}


	/**
	 *
	 * @return
	 */
	public boolean isValid () {
		return hasModel();
	}


	/**
	 *
	 * @param model
	 */
	public void setContextModel (GeneContextModel model) {
		this.model = model;
		hasModel = (this.model!=null);
	}


	public String getID()
	{
	    return ID;
    }


}
