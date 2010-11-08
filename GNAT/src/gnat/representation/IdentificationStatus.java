package gnat.representation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Keep track of the identification status for a recognized gene name: current/remaining candidate IDs,
 * and whether it has been finally identified.
 * 
 * @author Conrad
 * 
 */
public class IdentificationStatus {
	
	/** Whether or not this entity has been ultimately identified, thus no further disambiguation
	 *  steps are required. */
	boolean identified = false;
	
	/** (Currently remaining) entity candidate IDs. */
	Set<String> idCandidates = new HashSet<String>();
	
	/** Keeps track of all IDs that have ever been assigned (but possibly removed as candidates
	 *  in later steps). */
	Set<String> originalCandidateIds = new HashSet<String>();

	/** */
	String id;

	public IdentificationStatus() { }

	public IdentificationStatus (Collection<String> idCandidates) {
		this.idCandidates.clear();
		this.idCandidates.addAll(idCandidates);
    }

	public IdentificationStatus (String[] idCandidates) {
		for (String id : idCandidates) {
			this.idCandidates.add(id);
        }
    }

	public void markAsIdentified() {
        identified = true;
    }

	public void markAsIdentified (String id) {
        identified = true;
        this.id = id;
    }

	public boolean isIdentified() {
		return identified;
	}

	public Set<String> getIdCandidates(){
		return idCandidates;
	}

	public String getId(){
		return id;
	}
	
	public Set<String> getOriginalIdCandidates(){
		return originalCandidateIds;
	}

	public void setIdCandidates (Collection<String> ids){
		idCandidates.clear();
		idCandidates.addAll(ids);
		originalCandidateIds.addAll(ids);
	}

	public void addIdCandidate (String id){
		idCandidates.add(id);
		originalCandidateIds.add(id);
	}

	public void addIdCandidates (String[] ids){
		for (String id: ids)
			addIdCandidate(id);
	}

	public void addIdCandidates(Collection<String> longFormIds) {
		idCandidates.addAll(longFormIds);
		originalCandidateIds.addAll(longFormIds);
    }
	
	public void removeIdCandidate(String id){
		idCandidates.remove(id);
	}

}
