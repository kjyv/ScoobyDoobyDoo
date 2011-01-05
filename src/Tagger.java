import com.aliasi.chunk.Chunking;
import com.aliasi.corpus.ObjectHandler;

import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;
import com.aliasi.lingmed.medline.parser.Article;
import com.aliasi.lingmed.medline.parser.Abstract;
import com.aliasi.lingmed.medline.parser.MedlineCitation;
import com.aliasi.lingmed.medline.parser.MedlineHandler;
import com.aliasi.lingmed.medline.parser.MedlineParser;
import com.aliasi.lingmed.medline.parser.OtherAbstract;

import com.aliasi.tag.Tagging;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.StopTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.xml.sax.InputSource;

public class Tagger {

	static final double CHUNK_SCORE = 1.0;
	static StringBuilder result;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
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
		
		Pattern pGt;
		Pattern pLt;
		
		public GeniaHandler(StringBuilder result) throws IOException {			
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

			// set up tokenizers
			/*TokenizerFactory factory = IndoEuropeanTokenizerFactory.INSTANCE;
			LowerCaseTokenizerFactory lcFactory = new LowerCaseTokenizerFactory(factory);

			StopTokenizerFactory stopFactory = new StopTokenizerFactory(
					lcFactory, stopSet);*/
			
			factory = new RegExTokenizerFactory("(-|'|\\d|\\p{L})+|\\S");

			this.result = result;
		}

		public String tag(String text) throws IOException, ClassNotFoundException {
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
			
			//escape characters that conflict with xml tags
			text = text.replaceAll("&", "&amp;").replaceAll(">", "&gt;").replaceAll("<", "&gt;");
			
			StringBuilder localResult = new StringBuilder(text);
			int numFoundGenes = 0;
			char[] cs = text.toCharArray();

			FileInputStream fileIn = new FileInputStream("pos-en-bio-genia.HiddenMarkovModel");
			ObjectInputStream objIn = new ObjectInputStream(fileIn);
			HiddenMarkovModel hmm = (HiddenMarkovModel) objIn.readObject();
			objIn.close();
			HmmDecoder decoder = new HmmDecoder(hmm);
			
			Tokenizer tokenizer = factory.tokenizer(cs, 0, cs.length);
		    String[] tokens = tokenizer.tokenize();
			List<String> tokenList = Arrays.asList(tokens);
			
			Tagging<String> tagging = decoder.tag(tokenList);
			
			//ugly but have to tokenize again to iterate 
			tokenizer = factory.tokenizer(cs, 0, cs.length);

			int i = 0;  //token counter
			for (String token : tokenizer) {
				//get pos tag for current token
				String tag = tagging.tag(i);
				if (geneSet.contains(token) ){ //|| (tag.equals("NN") || tag.equals("NNP"))) {
					//System.out.print(tagging.token(i) + "_" + tag + "\n");
					
					//insert gene tag into text
					localResult.insert(tokenizer.lastTokenStartPosition()
							+ numFoundGenes * 13, "<gene>");
					localResult.insert(tokenizer.lastTokenEndPosition()
							+ numFoundGenes * 13 + 6, "</gene>");
					numFoundGenes++;
				}
				++i;
			}
			this.numFoundGenes += numFoundGenes;
			return localResult.toString();
		}

		@Override
		public void handle(GeniaMedlineCitation citation) {	
			result.append("<MedlineCitation>\n");

			pmid = citation.pmid;
			result.append("<PMID>" + pmid + "</PMID>\n");
			// System.out.println("processing pmid=" + id);

			result.append("<ArticleTitle>\n");
			result.append("\n</ArticleTitle>\n");

			result.append("<AbstractText>\n");
			
			try {
				result.append(tag(citation.body));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
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
