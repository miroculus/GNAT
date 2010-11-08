package gnat.representation;

public class Evidence {
	String pmId;
	String geneId;
	String evidence;
	public Evidence(String pmId, String geneId, String evidence) {
		super();
		this.pmId = pmId;
		this.geneId = geneId;
		this.evidence = evidence;
	}
	public String getPmId() {
		return pmId;
	}
	public void setPmId(String pmId) {
		this.pmId = pmId;
	}
	public String getGeneId() {
		return geneId;
	}
	public void setGeneId(String geneId) {
		this.geneId = geneId;
	}
	public String getEvidence() {
		return evidence;
	}
	public void setEvidence(String evidence) {
		this.evidence = evidence;
	}
	
	
}
