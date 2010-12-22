import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.InputSource;

import com.aliasi.chunk.BioTagChunkCodec;
import com.aliasi.chunk.Chunking;
import com.aliasi.chunk.TagChunkCodec;
import com.aliasi.chunk.TagChunkCodecAdapters;

import com.aliasi.corpus.ObjectHandler;
import com.aliasi.corpus.StringParser;

import com.aliasi.tag.LineTaggingParser;
import com.aliasi.tag.Tagging;

/**
 * Here's a corpus sample from CoNLL 2002 test file <code>ned.testa</code>,
 * which is encoded in the character set ISO-8859-1 (aka Latin1):
 * 
 * <blockquote><pre>...
 * de Art O
 * orde N O
 * . Punc O
 * 
 * -DOCSTART- -DOCSTART- O
 * Met Prep O
 * tien Num O
 * miljoen Num O
 * komen V O
 * we Pron O
 * , Punc O
 * denk V O
 * ik Pron O
 * , Punc O
 * al Adv O
 * een Art O
 * heel Adj O
 * eind N O
 * . Punc O
 * 
 * Dirk N B-PER
 * ...
 * </pre></blockquote>
*/
public class GeniaParser extends StringParser<ObjectHandler<GeniaMedlineCitation>> {
    
	static final Pattern pMEDLINE_HEADING = Pattern.compile("###MEDLINE:(\\d+)");
	
    static final String TOKEN_TAG_LINE_REGEX
        = "(\\S+)\\s(\\S+\\s)?(O|[B|I]-\\S+)"; // token ?posTag entityTag
    
    public String pid;
   
    public void parseString(char[] cs, int start, int end) {
        String in = new String(cs,start,end-start);       
    }
    
    public void parse(FileInputStream fileIn) throws IOException {
    	//read file and call handle per paragraph starting with ###MEDLINE 
        DataInputStream in = new DataInputStream(fileIn);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line = "";
        GeniaMedlineCitation citation = new GeniaMedlineCitation(0, "");
        Matcher m;
		while ((line = br.readLine()) != null){
			m = pMEDLINE_HEADING.matcher(line);
			if (m.find()) {
				if (citation.body.length() > 0) {
					this.getHandler().handle(citation);
					citation.body = "";
				}
				citation.pmid = m.group(1);
			} else { citation.body += line+" "; }
		}
		//handle last one
		this.getHandler().handle(citation);

    }
}
