
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;


public class easySearch {
	public static void main(String[] args) throws Exception {
		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("D:\\Search_JAVA\\index")));
		IndexSearcher searcher=new IndexSearcher(reader);
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser parser = new QueryParser("TEXT", analyzer);
		@SuppressWarnings("resource")
		Scanner in = new Scanner(System. in);
		System.out.println("Enter a string to search for");
		String queryString = in.nextLine();
		//String queryString="police";
		Query query = parser.parse(queryString);
		
		PrintWriter out = new PrintWriter("D:\\Search_JAVA\\easySearchOutput.txt","ASCII");
		
		Set<Term> queryTerms = new LinkedHashSet<Term>();
		searcher.createNormalizedWeight(query, false).extractTerms(queryTerms);
		Map<String,Float> mapp=new HashMap<String, Float>();
		ClassicSimilarity dSimi=new ClassicSimilarity();
		List<LeafReaderContext> leafContexts = reader.getContext().reader().leaves();
		
		for (int i = 0; i < leafContexts.size(); i++) {
			LeafReaderContext leafContext=leafContexts.get(i);
			int startDocNo=leafContext.docBase;
			
			for(Term t : queryTerms){
				PostingsEnum de = MultiFields.getTermDocsEnum(leafContext.reader(),"TEXT", new BytesRef(t.text()));
				int d;
					while(de!= null &&(d=de.nextDoc())!=PostingsEnum.NO_MORE_DOCS)
					{
						String DocId = leafContext.reader().document(de.docID()).get("DOCNO");
						int docNum=de.docID()+startDocNo;
						float normLength=dSimi.decodeNormValue(leafContext.reader().getNormValues("TEXT").get(d));
						float doclen = 1/(normLength*normLength);
						float termFreq=(de.freq()/doclen);
						int docFreq=reader.docFreq(new Term("TEXT",t.text()));
						float inverseDocFreq=(float)Math.log10(1+(reader.maxDoc()/docFreq));
						float relevanceScore=termFreq*inverseDocFreq;
//						System.out.println("term " + t.text() + " occurs " +de.freq() +" time(s) in doc " + docNum );
//						out.println("term " + t.text() + " occurs " +de.freq() +" time(s) in doc " + docNum);
						String keyVal = docNum+" "+DocId;
						if(mapp.containsKey(keyVal))
						{
							mapp.put(keyVal,mapp.get(keyVal)+relevanceScore);
						}
						else
						{
							mapp.put(keyVal, relevanceScore);
						}
				
			}
			
				
		}
	}
		int c = 0;
		for(Map.Entry<String, Float> entry: mapp.entrySet())
		{ 
			
			String[] arr = entry.getKey().split(" ", 2);
			System.out.println("Doc no :" +" "+arr[0]+"|| "+"Doc real Id:"+" "+arr[1]+ " relevance score is " +entry.getValue());
			out.println("Doc no :" +" "+arr[0]+"|| "+"Doc real Id:"+" "+arr[1]+ " relevance score is " +entry.getValue());
			c++;
		}
		System.out.println("Total number of documents TF-IDF score is calculated for query string exists is "+c);
		out.close();
	}
}