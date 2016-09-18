import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

public class LuceneTester {

    private String _indexDir = "D:\\Local Repositories\\ConceptIndexer\\data\\index";
    private String _dataDir = "D:\\Local Repositories\\ConceptIndexer\\data\\corpus";

    public static void main(String[] args) {
        LuceneTester tester;
        try {
            tester = new LuceneTester();
            //Enable if regeneration of index is needed
            //tester.createIndex();
            tester.search("fourier transform");
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createIndex() throws IOException{
        Indexer indexer = new Indexer(_indexDir);
        int numIndexed;
        long startTime = System.currentTimeMillis();
        numIndexed = indexer.createIndex(_dataDir, new TextFileFilter());
        long endTime = System.currentTimeMillis();
        indexer.close();
        System.out.println(numIndexed + " files indexed, time taken: " + (endTime-startTime) + " ms");
    }

    private void search(String searchQuery) throws IOException, ParseException{
        Searcher searcher = new Searcher(_indexDir);
        long startTime = System.currentTimeMillis();
        TopDocs hits = searcher.search(searchQuery);
        long endTime = System.currentTimeMillis();

        System.out.println(hits.totalHits + " documents found. Time(ms): " + (endTime - startTime));
        for(ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = searcher.getDocument(scoreDoc);
            System.out.println("File: " + doc.get(LuceneConstants.FILE_PATH));
        }
        searcher.close();
    }
}