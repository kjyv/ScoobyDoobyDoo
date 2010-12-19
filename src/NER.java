import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.aliasi.chunk.AbstractCharLmRescoringChunker;
import com.aliasi.chunk.CharLmRescoringChunker;
import com.aliasi.chunk.ChunkerEvaluator;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.AbstractExternalizable;

/*
TODO:




 */


class NER {
	static final int NUM_CHUNKINGS_RESCORED = 64;
	static final int MAX_N_GRAM = 12;
	static final int NUM_CHARS = 256;
	static final double LM_INTERPOLATION = MAX_N_GRAM; // default behavior
	static File modelFile, contentFile;	// contentFile interpeted as training file or test file, depending on args[0]

	public static void main(String[] args) throws IOException {
		/*
		 * usage:
		 * 	train [trainFile] [modelOutputFile]
		 * 	test [testFile] [modelInputFile]
		 */
		if(args.length != 3 || (!args[0].equals("train") && !args[0].equals("test")))
		{
			System.out.println("wrong arguments");
			printUsage();
			System.exit(1);
		}
		String mode = args[0];
		
		contentFile = new File(args[1]);
		modelFile = new File(args[2]);
		if(mode.equals("train") && modelFile.exists())
		{
			modelFile.delete();
		}
		modelFile.createNewFile();
		//File devFile = new File(args[2]);
		
		if(contentFile==null || modelFile==null || !contentFile.isFile() || !modelFile.isFile())
		{
			System.out.println("file not found");
			printUsage();
			System.exit(1);
		}
		
		System.out.print("preprocessing corpus file ... ");	System.out.flush();
		preprocessContentFile(contentFile);
		System.out.println("done.");	System.out.flush();
		System.gc();
		
		if(mode.toLowerCase().equals("train"))
		{
			System.out.print("training ... "); System.out.flush();
			train();
			System.out.println("done."); System.out.flush();
		}
		else
		{
			try {
				evaluate();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void train()
	{
		TokenizerFactory factory = IndoEuropeanTokenizerFactory.INSTANCE;
		// CharLmRescoringChunker is best, but slowest: http://alias-i.com/lingpipe/demos/tutorial/ne/read-me.html
		CharLmRescoringChunker chunkerEstimator = new CharLmRescoringChunker(
				factory, NUM_CHUNKINGS_RESCORED, MAX_N_GRAM, NUM_CHARS,
				LM_INTERPOLATION);

		Conll2002ChunkTagParser parser = new Conll2002ChunkTagParser();
		parser.setHandler(chunkerEstimator);

		try {
			parser.parse(contentFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//parser.parse(devFile);

		try {
			AbstractExternalizable.compileTo(chunkerEstimator, modelFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void evaluate() throws IOException, ClassNotFoundException
	{
		AbstractCharLmRescoringChunker chunker = (AbstractCharLmRescoringChunker) AbstractExternalizable
				.readObject(modelFile);

		ChunkerEvaluator evaluator = new ChunkerEvaluator(chunker);
		evaluator.setVerbose(true);

		Conll2002ChunkTagParser parser = new Conll2002ChunkTagParser();
		parser.setHandler(evaluator);

		parser.parse(contentFile);

		System.out.println(evaluator.toString());
	}
	
	public static void preprocessContentFile(File file) throws IOException
	{
		//tags other than [B|I]-[protein|dna|rna] should be ignored => preprocess train file to replace other tags with O
		String fileContent = readFileAsString(file);
		fileContent = fileContent.replaceAll("(B-cell_line|B-cell_type|I-cell_line|I-cell_type)\n", "O\n");
		OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file));
		out.write(fileContent);
		out.close();
	}
	
	private static String readFileAsString(File file) throws java.io.IOException
	{
		StringBuffer fileData = new StringBuffer(1000);
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			fileData.append(buf, 0, numRead);
		}
		reader.close();
		return fileData.toString();
	}
	
	public static void printUsage()
	{
		System.out.println("usage:\ntrain [trainFile] [modelOutputFile]\ntest [testFile] [modelInputFile]");
	}
}