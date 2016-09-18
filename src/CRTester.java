import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class CRTester {

    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
        //instantiate a concept recognizer
        //the XML ontology tree is parsed
        ConceptRecognizer cr = new ConceptRecognizer("D:\\Local Repositories\\ConceptIndexer\\data\\ontology.xml");

        //recognize concepts in corpus
        //output JSON to the specified directory
        cr.parseCorpus("D:\\Local Repositories\\ConceptIndexer\\data\\corpus", "D:\\Local Repositories\\ConceptIndexer\\data\\cr_output\\", new TextFileFilter());
    }

}
