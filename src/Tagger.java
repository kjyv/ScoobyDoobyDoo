import com.aliasi.chunk.AbstractCharLmRescoringChunker;
import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.chunk.HmmChunker;
import com.aliasi.corpus.ObjectHandler;

import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;

import com.aliasi.tag.Tagging;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.AbstractExternalizable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;


public class Tagger {

	static final double CHUNK_SCORE = 1.0;
	static StringBuilder result;

	/**
	 * @param args
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		long startTime = System.currentTimeMillis();

		GeniaParser parser = new GeniaParser();
		result = new StringBuilder("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n<results>\n");
		GeniaHandler handler = new GeniaHandler(result);
		parser.setHandler(handler);
		if (args.length > 0)
		{
			FileInputStream fileIn = new FileInputStream(args[0]);
			parser.parse(fileIn);
		} else {
			System.out.println("Error: Please supply a genia input file.");
			System.exit(0);
		}

		result.append("</results>");
		System.out.println(result);
		System.err.println("\ngenes tagged: " + handler.numFoundGenes);
		System.err.println("time: " + (System.currentTimeMillis() - startTime)
				/ 1000.0 + "s");
	}

	static class GeniaHandler implements ObjectHandler<GeniaMedlineCitation> {
		long numFoundGenes = 0L;

		HashSet<String> stopSet = new HashSet<String>(),
				geneSet = new HashSet<String>();
		TreeSet<String> tokenSet = new TreeSet<String>();

		String pmid;
		StringBuilder result;
		TokenizerFactory factory;
		
		HmmDecoder POSDecoder, NERDecoder;	// part-of-speech tagging hidden markov model decoder and NER-decoder
		
		Pattern pGt;
		Pattern pLt;
		
		public GeniaHandler(StringBuilder result) throws IOException, ClassNotFoundException {			
			String line;
		
			// read in gene dictionary
			BufferedReader geneNames = new BufferedReader(new FileReader(
					"genenames-2.txt"));
			while ((line = geneNames.readLine()) != null) {
				if (line.length() == 0)
					continue;
				geneSet.add(line);
			}
			geneNames.close();
			
			// load models
			FileInputStream fileIn = new FileInputStream("pos-en-bio-genia.HiddenMarkovModel");
			ObjectInputStream objIn = new ObjectInputStream(fileIn);
			HiddenMarkovModel hmm = (HiddenMarkovModel) objIn.readObject();
			objIn.close(); fileIn.close();
			this.POSDecoder = new HmmDecoder(hmm);
			
			fileIn = new FileInputStream("ne-en-bio-genetag.HmmChunker");
			objIn = new ObjectInputStream(fileIn);
			HmmChunker hmmChunker = (HmmChunker) objIn.readObject();
			objIn.close(); fileIn.close();
			this.NERDecoder = hmmChunker.getDecoder();
		
			factory = new RegExTokenizerFactory("(-|'|\\d|\\p{L})+|\\S");

			this.result = result;
		}

		public String tag(List<String> text) throws Exception {
			/*
			 * actual tagging method
			 * in here we want
			 * - tokenization
			 * - gene tagging with dictionary
			 * - POS
			 * - gene tagging with language(medline) model (NER)
			 * 
			 * returns input with tags added
			 */
			
			StringBuilder localResult = new StringBuilder();
			//int numFoundGenes = 0;
			
			// Part Of Speech (POS) - tagging
			Tagging<String> POSTagging = POSDecoder.tag(text);	// contains lists of tokens and assigned tags

			// Named Entity Recognition (NER) decoder
			Tagging<String> NERTagging = NERDecoder.tag(text);

			//NER chunker
			AbstractCharLmRescoringChunker chunker = (AbstractCharLmRescoringChunker) AbstractExternalizable
			.readObject(new File("test_chunker"));
   
			Chunking chunking = chunker.chunk(join(text, " "));
			System.out.println("Chunking=" + chunking);
			for (Chunk chunk : chunking.chunkSet()){
			    System.out.println(chunk.toString() + "[" + chunk.type() + "]");
			}
		
			// iterate through POS and NER lists and geneSet to determine genes
			int numTokens = text.size();
			List<String> currentGene = new LinkedList<String>();
			String tag;
			for(int tokenIndex = 0; tokenIndex < numTokens; tokenIndex++)
			{
				tag = NERTagging.tag(tokenIndex);
				String currentToken = NERTagging.token(tokenIndex);
					if (tag.equals("MM_O") || tag.equals("WW_O_GENE")){
						handleNEREndOfGene(currentGene, currentToken, tokenIndex, POSTagging, NERTagging, localResult);
						if(tokenIndex > 0)
						{
							localResult.append(" ");
						}
						localResult.append(escapeXML(currentToken));
					}
					// NER determined within-gene token and // NER determined beginning-of-gene token
					else if (tag.equals("I_GENE") || tag.equals("B_GENE") || tag.equals("W_GENE") ||
							tag.equals("M_GENE") || tag.equals("E_GENE")) {	
						
						currentGene.add(currentToken);
						if(tokenIndex == numTokens - 1)
						{
							// a gene was found, but there is no more loop to handle it. so do it here
							handleNEREndOfGene(currentGene, currentToken, tokenIndex, POSTagging, NERTagging, localResult);
						}
					}
					/*else 
						throw new Exception("unknown tag: " + tag + " for token " + currentToken + " with index " + tokenIndex);
					*/
			}
			return localResult.toString();
		}
		
		// function to avoid duplicate code
		public void handleNEREndOfGene(List<String> currentGene, String currentToken, int tokenIndex, Tagging<String> POSTagging, Tagging<String> NERTagging, StringBuilder localResult)
		{
			if(isGene(currentGene,
					POSTagging.tags().subList(tokenIndex, tokenIndex+currentGene.size()),
					NERTagging.tags().subList(tokenIndex, tokenIndex+currentGene.size())
				))
			{
				String gene = escapeXML(join(currentGene, " "));
				localResult.append(" <gene>").append(gene).append("</gene>");
				currentGene.clear();
				this.numFoundGenes++;
			}
		}
		
		// NER proposed tokens as a gene. use POSTags (and NERTags?) and geneSet to double-check
		public boolean isGene(List<String> tokens, List<String> POSTags, List<String> NERTags)
		{
			if(tokens.size() == 0)
				return false;
			boolean atLeastOneKnownGene = false, atLeastOneNoun = false;
			for(int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++)
			{
				String token = tokens.get(tokenIndex);
				String POSTag = POSTags.get(tokenIndex);
				if(POSTag.equals("NN") || POSTag.equals("NNP"))
					atLeastOneNoun = true;
				if(geneSet.contains(token))
					atLeastOneKnownGene = true;
			}
			return atLeastOneKnownGene || atLeastOneNoun;
		}
		
		public String join(Collection<String> s, String delimiter)
		{
	        StringBuffer buffer = new StringBuffer();
	        Iterator<String> iter = s.iterator();
	        while (iter.hasNext()) {
	            buffer.append(iter.next());
	            if (iter.hasNext()) {
	                buffer.append(delimiter);
	            }
	        }
	        return buffer.toString();
	    }
		
		public static String escapeXML(String toBeEscaped)
		{
			return toBeEscaped.replaceAll("&", "&amp;").replaceAll(">", "&gt;").replaceAll("<", "&gt;");
		}


		public void handle(GeniaMedlineCitation citation)
		{	
			result.append("<MedlineCitation>\n");

			pmid = citation.pmid;
			result.append("<PMID>" + pmid + "</PMID>\n");
			result.append("<AbstractText>\n");
			
			try {
				result.append(tag(citation.body));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			
			result.append("\n</AbstractText>\n");
			result.append("</MedlineCitation>\n");
		}
		
		public void delete(String pmid) {
			throw new UnsupportedOperationException(
					"not expecting deletes. found pmid=" + pmid);
		}
	}
}
