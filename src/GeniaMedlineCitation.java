import java.util.List;

public class GeniaMedlineCitation {
	String pmid;
	List<String> body;	// list of tokens
	public GeniaMedlineCitation(int pmid, List<String> body )
	{
		this.pmid = String.valueOf(pmid);
		this.body = body;
	}
}
