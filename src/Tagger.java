import com.aliasi.chunk.Chunking;
import com.aliasi.corpus.ObjectHandler;

import com.aliasi.lingmed.medline.parser.Article;
import com.aliasi.lingmed.medline.parser.Abstract;
import com.aliasi.lingmed.medline.parser.MedlineCitation;
import com.aliasi.lingmed.medline.parser.MedlineHandler;
import com.aliasi.lingmed.medline.parser.MedlineParser;
import com.aliasi.lingmed.medline.parser.OtherAbstract;

import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.StopTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
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

		public void handle(MedlineCitation citation) {
			result.append("<MedlineCitation>\n");

			pmid = citation.pmid();
			result.append("<PMID>" + pmid + "</PMID>\n");
			// System.out.println("processing pmid=" + id);

			Article article = citation.article();		
			String titleText = article.articleTitleText();

			result.append("<ArticleTitle>\n");
			addText(titleText);
			result.append("\n</ArticleTitle>\n");

			Abstract abstrct = article.abstrct();
			String abstractText;
			if (abstrct != null
					&& !(abstractText = abstrct.textWithoutTruncationMarker())
							.equals("")) {
				result.append("<AbstractText>\n");
				addText(abstractText);
				result.append("\n</AbstractText>\n");
			}
			
			OtherAbstract oAbstracts[] = citation.otherAbstracts();
			if (oAbstracts.length > 0){
				//result.append("<OtherAbstract>\n");
				for(OtherAbstract oAbstract : oAbstracts){
					abstractText = oAbstract.text();
					result.append("<AbstractText>\n");
					addText(abstractText);
					result.append("\n</AbstractText>");
				}
				//result.append("\n</OtherAbstract>\n");
			}
			
			result.append("</MedlineCitation>\n");
		}

		public void delete(String pmid) {
			throw new UnsupportedOperationException(
					"not expecting deletes. found pmid=" + pmid);
		}

		public void addText(String text) {
			text = text.replaceAll("&", "&amp;").replaceAll(">", "&gt;").replaceAll("<", "&gt;");
			
			StringBuilder localResult = new StringBuilder(text);
			int numFoundGenes = 0;
			char[] cs = text.toCharArray();

			Tokenizer tokenizer = factory.tokenizer(cs, 0, cs.length);
			for (String token : tokenizer) {
				//tokenSet.add(token);
				if (geneSet.contains(token)) {
					// System.out.println(token);
					// System.out.println("start: " +
					// tokenizer.lastTokenStartPosition());
					localResult.insert(tokenizer.lastTokenStartPosition()
							+ numFoundGenes * 13, "<gene>");
					localResult.insert(tokenizer.lastTokenEndPosition()
							+ numFoundGenes * 13 + 6, "</gene>");
					numFoundGenes++;
				}
			}
			this.numFoundGenes += numFoundGenes;
			result.append(localResult);
		}

		@Override
		public void handle(GeniaMedlineCitation citation) {
			System.out.println(citation.pmid);
		}
	}
}
