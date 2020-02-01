
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;


public class compareAlgorithms {
	
	public static void querystr(String [] topicTags,PrintWriter query, Similarity simobj,String runtitle) throws IOException, ParseException
	{
		
		for (String t : topicTags)
		{
			String number = (StringUtils.substringBetween(t, "Number:", "<")).trim();
			int n = Integer.parseInt(number);
			String title = (StringUtils.substringBetween(t, "Topic:", "<")).trim().replaceAll("[/,?,(,)]", " ");
			String description = (StringUtils.substringBetween(t,"Description:", "<").trim()).replaceAll("[/,?]", " ");
			Map<String, Float> mapp = new HashMap<String, Float>();
			if(runtitle=="defaultSimilarity-title"||runtitle=="BM25-Title"||runtitle=="dirichilet-Title"||runtitle=="dirichiletlambda-Title")
			{
				relevanceScore(title, n, mapp, query,runtitle ,simobj);
			}
			else
			{
				relevanceScore(description, n, mapp, query, runtitle,simobj);
			}
			
			
		}
	}
	public static void main(String[] args) throws ParseException, IOException {
		Path path = Paths.get("D:\\Search_JAVA\\topics.51-100");
		
		String fileData = new String(Files.readAllBytes(path));
		String[] topicTags = StringUtils.substringsBetween(fileData, "<top>","</top>");
		
		PrintWriter defaultSimilarityshortQuery = new PrintWriter("D:\\Search_JAVA\\classicSimilarityshortQuery.txt","ASCII");
		PrintWriter defaultSimilaritylongquery = new PrintWriter("D:\\Search_JAVA\\classicSimilaritylongQuery.txt","ASCII");
		PrintWriter BM25ShortQuery = new PrintWriter("D:\\Search_JAVA\\BM25SimilarityshortQuery.txt","ASCII");
		PrintWriter BM25LongQuery = new PrintWriter("D:\\Search_JAVA\\BM25SimilaritylongQuery.txt","ASCII");
		PrintWriter lmDirichletSimilarityShortQuery = new PrintWriter("D:\\Search_JAVA\\LMDirichletSimilarityshortQuery.txt","ASCII");
		PrintWriter lmDirichletSimilarityLongQuery = new PrintWriter("D:\\Search_JAVA\\LMDirichletSimilaritylongQuery.txt","ASCII");
		PrintWriter lmJelinekMercerSimilarityShortQuery = new PrintWriter("D:\\Search_JAVA\\LMJelinekMercerSimilarityshortQuery.txt","ASCII");
		PrintWriter lmJelinekMercerSimilarityLongQuery = new PrintWriter("D:\\Search_JAVA\\LMJelinekMercerSimilaritylongQuery.txt","ASCII");
		
		
		Similarity defaultSimilarity = new ClassicSimilarity();
		Similarity BM25 = new BM25Similarity();
		Similarity dirichiletSimilarity = new LMDirichletSimilarity();
		Similarity lmJelinekMercerSimilarity = new LMJelinekMercerSimilarity((float) 0.7);
		
		System.out.println("Similarity search starts");
		querystr(topicTags,defaultSimilarityshortQuery,defaultSimilarity,"defaultSimilarity-title");
		querystr(topicTags,defaultSimilaritylongquery,defaultSimilarity,"defaultSimilarity-description");
		querystr(topicTags,BM25ShortQuery,BM25,"BM25-Title");
		querystr(topicTags,BM25LongQuery,BM25,"BM25-Description");
		querystr(topicTags,lmDirichletSimilarityShortQuery,dirichiletSimilarity,"dirichilet-Title");
		querystr(topicTags,lmDirichletSimilarityLongQuery,dirichiletSimilarity,"dirichilet-Description");
		querystr(topicTags,lmJelinekMercerSimilarityShortQuery,lmJelinekMercerSimilarity,"dirichiletlambda-Title");
		querystr(topicTags,lmJelinekMercerSimilarityLongQuery,lmJelinekMercerSimilarity,"dirichiletlambda-Description");
		System.out.println("Similarity search ends");
		
		
		defaultSimilarityshortQuery.close();
		defaultSimilaritylongquery.close();
		BM25ShortQuery.close();
		BM25LongQuery.close();
		lmDirichletSimilarityShortQuery.close();
		lmDirichletSimilarityLongQuery.close();
		lmJelinekMercerSimilarityShortQuery.close();
		lmJelinekMercerSimilarityLongQuery.close();

	}

	public static void relevanceScore(String docQuery, int num,
			Map<String, Float> mapp, PrintWriter in, String run,
			Similarity similarity) throws IOException, ParseException {

		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("D:\\Search_JAVA\\index")));
		IndexSearcher searcher = new IndexSearcher(reader);
		searcher.setSimilarity(similarity);
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser parser = new QueryParser("TEXT", analyzer);
		Query query = parser.parse(QueryParser.escape(docQuery));
		TopDocs results = searcher.search(query, 1000);
		//int numTotalHits = results.totalHits;
		ScoreDoc[] hits = results.scoreDocs;
		for (int j = 0; j < hits.length; j++) {
			Document doc = searcher.doc(hits[j].doc);
			String key = doc.get("DOCNO");
			if (mapp.containsKey(key)) 
			{
				mapp.put(key, mapp.get(key) + hits[j].score);

			} else 
				
			{
				mapp.put(key, hits[j].score);
			}
		}
		List<Entry<String, Float>> mapSorted = entriesSortedByValues(mapp); 
		for (int k = 0, count = 1; k < mapp.size() && count <= 1000; k++, count++) 
		{ 

			in.println(num+" "+"0"+" "
					+ mapSorted.get(k).getKey() + " " + count + " "
					+ mapSorted.get(k).getValue() + " " + run);
		}
		

		reader.close();
	}

	public static <K, V extends Comparable<? super V>> List<Entry<K, V>> entriesSortedByValues(
			Map<K, V> map) {

		List<Entry<K, V>> entriesSorted = new ArrayList<Entry<K, V>>(
				map.entrySet());

		Collections.sort(entriesSorted, new Comparator<Entry<K, V>>() {
			@Override
			public int compare(Entry<K, V> ent1, Entry<K, V> ent2) {
				return ent2.getValue().compareTo(ent1.getValue());
			}
		});

		return entriesSorted;
	}

	
}
