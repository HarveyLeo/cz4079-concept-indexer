import java.io.IOException;
import java.nio.file.Paths;

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
import org.apache.lucene.store.FSDirectory;


class Searcher {

    private IndexSearcher _indexSearcher;
    private QueryParser _queryParser;
    private IndexReader _indexReader;

    Searcher(String indexDirectoryPath) throws IOException{
        _indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDirectoryPath)));
        _indexSearcher = new IndexSearcher(_indexReader);
        _queryParser = new QueryParser(LuceneConstants.CONTENTS, new StandardAnalyzer());
    }

    TopDocs search(String searchQuery) throws IOException, ParseException{
        Query query;
        query = _queryParser.parse(searchQuery);
        return _indexSearcher.search(query, LuceneConstants.MAX_SEARCH);
    }

    Document getDocument(ScoreDoc scoreDoc) throws IOException{
        return _indexSearcher.doc(scoreDoc.doc);
    }

    void close() throws IOException{
        _indexReader.close();
    }
}