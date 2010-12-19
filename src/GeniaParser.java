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
public class GeniaParser extends StringParser<ObjectHandler<Chunking>> {
    
    static final String TOKEN_TAG_LINE_REGEX
        = "(\\S+)\\s(\\S+\\s)?(O|[B|I]-\\S+)"; // token ?posTag entityTag
    
    static final String EOS_REGEX
        = "\\A\\Z";         // empty lines
   
    public void parse(char[] cs, int start, int end) {
    }

    public void setHandler(ObjectHandler<Chunking> handler) {
        
    }

}
