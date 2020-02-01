
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;



import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class searchTRECtopics {
	public static void querystr(String [] topicTags,PrintWriter query, String runtitle) throws IOException, ParseException
	{
		for (String t : topicTags)
		{
			String number = (StringUtils.substringBetween(t, "Number:", "<")).trim();
			System.out.println(Integer.parseInt(number));
			String title = (StringUtils.substringBetween(t, "Topic:", "<")).trim().replaceAll("[/,?,(,)]", " ");
			String description = (StringUtils.substringBetween(t,"Description:", "<").trim()).replaceAll("[/,?]", " ");
			Map<String, Float> mapp = new HashMap<String, Float>();
			if(runtitle == "run-title") {
				relevanceScore(title, Integer.parseInt(number), mapp, query, runtitle);
			}
			else {
				relevanceScore(description, Integer.parseInt(number), mapp, query, runtitle);
			}
			
		}
	}
	public static void main(String[] args) throws IOException, ParseException 
	{
		Path path = Paths.get("D:\\Search_JAVA\\topics.51-100");
		PrintWriter shortQuery = new PrintWriter("D:\\Search_JAVA\\topicShortQuery.txt","ASCII");
		PrintWriter longQuery = new PrintWriter("D:\\Search_JAVA\\topicLongQuery.txt","ASCII");
		String allFiles = new String(Files.readAllBytes(path));
		String[] topicTags = StringUtils.substringsBetween(allFiles, "<top>","</top>");
		
		System.out.println("Calculating relevance score for short query");
		querystr(topicTags,shortQuery,"run-title");
		System.out.println("Relevance score for short query completed\n");

		System.out.println("Calculating relevance score for long query");
		querystr(topicTags,longQuery,"run-desciption");
		System.out.println("Relevance score for long query completed");
		
		shortQuery.close();
		longQuery.close();
	}

	public static void relevanceScore(String queryString, int num, Map<String, Float> mapp, PrintWriter in, String run)
			throws IOException, ParseException 
	{

		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("D:\\Search_JAVA\\index")));
		IndexSearcher searcher = new IndexSearcher(reader);
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser parser = new QueryParser("TEXT", analyzer);
		Query query = parser.parse(QueryParser.escape(queryString));
		Set<Term> queryTerms = new LinkedHashSet<Term>();
		searcher.createNormalizedWeight(query, false).extractTerms(queryTerms);
		ClassicSimilarity dSimi = new ClassicSimilarity();
		List<LeafReaderContext> leafContexts = reader.getContext().reader().leaves();
		//float total = reader.maxDoc();
		for (Term t : queryTerms) {
			for (int i = 0; i < leafContexts.size(); i++) {
				LeafReaderContext leafContext = leafContexts.get(i);
				int startDocNo = (leafContext.docBase);
				PostingsEnum de = MultiFields.getTermDocsEnum(leafContext.reader(), "TEXT", new BytesRef(t.text()));
				@SuppressWarnings("unused")
				int d;
				while ((de != null && (d = de.nextDoc()) != PostingsEnum.NO_MORE_DOCS)) {
					String DocId = leafContext.reader().document(de.docID()).get("DOCNO");
					String docNumber = de.docID()+startDocNo+"$"+DocId+"$";
					float normLength = dSimi.decodeNormValue(leafContext.reader().getNormValues("TEXT").get(de.docID()));
					float doclen = 1/(normLength*normLength);
					float termFreq = (de.freq() /doclen);
					int docFreq = reader.docFreq(new Term("TEXT", t.text()));
					float inverseDocFreq = (float) Math.log10((1 + (reader.maxDoc() / docFreq )));
					float tfidf = termFreq * inverseDocFreq;
					String dockey = docNumber+'*'+queryString+'*'+'/'+num+'/';
					if (mapp.containsKey(dockey))
					{
						mapp.put(dockey, mapp.get(dockey) + (tfidf));
					} 
					else 
					{
						mapp.put(dockey, ((tfidf)));
					}
				}
			}
		}

		Map<String, Float> sorted = mapSortByValue(mapp);

		int count = 1;
		for (Entry<String, Float> entry1 : sorted.entrySet()) {
			String docid = StringUtils.substringBetween(entry1.getKey(), "$","$");
			String querynumber = StringUtils.substringBetween(entry1.getKey(),"/", "/");
			in.println(querynumber + " 0 " +docid + " " + (count++) + " " + entry1.getValue() + " " + run);
			if (count == 1001) 
			{
				break;
			}
		}
		reader.close();
	}

	public static Map<String, Float> mapSortByValue(Map<String, Float> mapp) {
		List<Entry<String, Float>> list = new LinkedList<Entry<String, Float>>(mapp.entrySet());
		Collections.sort(list, new Comparator<Entry<String, Float>>() 
		{
			public int compare(Entry<String, Float> str1, Entry<String, Float> str2) 
			{
				return str2.getValue().compareTo(str1.getValue());
			}
		});
		
		Map<String, Float> keyValues = new LinkedHashMap<String, Float>();
		for (Entry<String, Float> entry : list) 
			
		{
			keyValues.put(entry.getKey(), entry.getValue());
		}

		return keyValues;
	}

}