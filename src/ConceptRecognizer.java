import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.text.BreakIterator;
import java.util.*;


public class ConceptRecognizer {

    //_conceptTree stores the concept ontology
    private ArrayList<ConceptNode> _conceptTree;

    public ConceptRecognizer(String xmlPath) throws IOException, SAXException, ParserConfigurationException {
        _conceptTree = parseXML(xmlPath);
    }

    public void parseCorpus(String corpusDirPath, String parseDirPath, FileFilter filter) throws IOException {
        //get all files in the corpus directory
        File[] files = new File(corpusDirPath).listFiles();

        for (File file : files) {
            if(!file.isDirectory() && !file.isHidden() && file.exists() && file.canRead() && filter.accept(file)){
                parseFile(file, parseDirPath);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseFile(File file, String parseDirPath) throws IOException {

        BreakIterator sentenceIterator = BreakIterator.getSentenceInstance(Locale.ENGLISH);

        //read the whole file into a string
        String content = new Scanner(file).useDelimiter("\\Z").next();
        sentenceIterator.setText(content);

        FileWriter fw = new FileWriter(parseDirPath + file.getName());

        //set sentence counter to 0
        int counter = 0;
        int lastIndex = sentenceIterator.first();

        //iterate through each sentence in the file
        while (lastIndex != BreakIterator.DONE) {
            int firstIndex = lastIndex;
            lastIndex = sentenceIterator.next();

            if (lastIndex != BreakIterator.DONE) {
                String sentence = content.substring(firstIndex, lastIndex);

                //create a json object for each sentence
                counter++;
                JSONObject senJsonObj = new JSONObject();

                //create key-value pairs for senid, mentions, filepath
                senJsonObj.put("senid", counter);
                senJsonObj.put("mentions", parseSentence(sentence));
                senJsonObj.put("filepath", file.getAbsolutePath());

                //create sentence tokens and store into the key "tokens"
                List<String> senTokenList = Arrays.asList(sentence.split("\\s+"));
                JSONArray senArray = new JSONArray();
                senArray.addAll(senTokenList);
                senJsonObj.put("tokens", senArray);

                //write json object to file
                fw.write(senJsonObj.toJSONString() + "\n");
            }
        }

        fw.close();

        //print out path for the written file
        System.out.println(parseDirPath + file.getName());
    }

    private JSONArray parseSentence(String sentence) {
        JSONArray mentionList = new JSONArray();
        List<String> senTokenList = Arrays.asList(sentence.toLowerCase().split("\\s+"));

        //traverse each concept node to extract concepts contained in the sentence
        for (ConceptNode node : _conceptTree) {
            mentionList = appendMentionList(mentionList, senTokenList, node);
        }

        //print mentionList for debugging
        //System.out.println(Arrays.toString(mentionList.toArray()));
        return mentionList;
    }

    @SuppressWarnings("unchecked")
    private JSONArray appendMentionList(JSONArray mentionList, List<String> senTokenList, ConceptNode node) {
        //iterate through each keyword for the current node
        //get all start/end word position pairs in the sentence for each keyword
        //update the mentionList
        for (String kw : node.getKeywords()) {
            List<String> kwTokenList = Arrays.asList(kw.toLowerCase().split("\\s+"));
            int kwIndex = -1;
            do {
                kwIndex = getIndexOfSubList(senTokenList, kwTokenList, kwIndex+1);
                if (kwIndex != -1) {
                    //Check whether the start/end word position pair has been identified for some concept before
                    //return -1 if it has not been identified before
                    int mentionIndex = indexOfMention(mentionList, kwIndex, kwIndex + kwTokenList.size());

                    if (mentionIndex == -1) { //if the start/end word position pair has not been identified for any concept
                        JSONObject obj = new JSONObject();
                        obj.put("start", kwIndex);
                        obj.put("end", kwIndex + kwTokenList.size());
                        ArrayList<String> labels = new ArrayList<>();
                        labels.add(node.getFullName());
                        obj.put("labels", labels);
                        mentionList.add(obj);
                    } else { //if the start/end word position pair has been identified for some concept
                        JSONObject obj = (JSONObject)mentionList.get(mentionIndex);
                        ArrayList<String> labels = (ArrayList)obj.get("labels");
                        labels.add(node.getFullName());
                        obj.put("labels",labels);
                    }
                }
            } while (kwIndex != -1);
        }

        //iterate through each child node
        //recursively extract concepts from child nodes that contained in the sentence
        //update mentionList
        for (ConceptNode cnode : node.getChildren()) {
            mentionList = appendMentionList(mentionList, senTokenList, cnode);
        }
        return mentionList;
    }

    //return the index of the start/end word position pair if it exists in the mentionList
    //return -1 if not
    private int indexOfMention(JSONArray array, int fromIndex, int toIndex) {
        Iterator iterator = array.iterator();
        int counter = 0;
        while (iterator.hasNext()) {
            JSONObject obj = (JSONObject) iterator.next();
            if (fromIndex == (int)obj.get("start") && toIndex == (int)obj.get("end")) {
                return counter;
            }
            counter++;
        }
        return -1;
    }

    //return the index of the first occurrence of target list in the source list, starting from fromIndex
    private int getIndexOfSubList(List<?> source, List<?> target, int fromIndex) {
        List<?> subSrc = source.subList(fromIndex, source.size());
        int position = Collections.indexOfSubList(subSrc, target);
        if (position == -1) {
            return -1;
        } else {
            return position + fromIndex;
        }
    }


    private ArrayList<ConceptNode> parseXML(String xmlPath) throws ParserConfigurationException, IOException, SAXException {
        //return the parse result to conceptList
        ArrayList<ConceptNode> conceptList = new ArrayList<>();

        //get the DOM builder factory
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        //get the DOM builder
        DocumentBuilder builder = factory.newDocumentBuilder();

        //document contains the complete XML as a tree
        Document document = builder.parse(new FileInputStream(xmlPath));

        //iterate through the first-level nodes
        NodeList firstLevelNodeList = document.getDocumentElement().getChildNodes();
        for (int i = 0; i < firstLevelNodeList.getLength(); i++) {
            Node node = firstLevelNodeList.item(i);
            if (node instanceof Element) {
                //Call parseElement() to convert a node into a ConceptNode object
                ConceptNode conceptNode = parseElement((Element)node, null);
                conceptList.add(conceptNode);
            }
        }
        return conceptList;
    }

    private ConceptNode parseElement (Element element, ConceptNode parent) {
        ConceptNode conceptNode = new ConceptNode();

        conceptNode.name = element.getAttributes().getNamedItem("name").getNodeValue();
        conceptNode.keywords = element.getFirstChild().getTextContent().trim().split("\\s*,\\s*");
        conceptNode.children = new ArrayList<>();
        conceptNode.parent = parent;

        NodeList childNodes = element.getChildNodes();
        for (int j = 0; j < childNodes.getLength(); j++) {
            Node cNode = childNodes.item(j);
            if (cNode instanceof Element) {
                ConceptNode cConceptNode = parseElement((Element)cNode, conceptNode);
                conceptNode.children.add(cConceptNode);
            }
        }

        return conceptNode;
    }

    private class ConceptNode {
        private String name;
        private String[] keywords;
        private ArrayList<ConceptNode> children;
        private ConceptNode parent;

        private String getFullName() {
            String fullname = "";
            ArrayList<ConceptNode> pnodes = new ArrayList<>();
            ConceptNode tempPNode = parent;
            while (tempPNode != null) {
                pnodes.add(tempPNode);
                tempPNode = tempPNode.getParent();
            }
            for (int i = pnodes.size()-1; i >= 0; i--) {
                fullname = fullname.concat(pnodes.get(i).getName() + "_");
            }
            fullname = fullname.concat(name);
            return fullname;
        }

        private String getName() { return name; }

        private ArrayList<ConceptNode> getChildren() {
            return children;
        }

        private String[] getKeywords() {
            return keywords;
        }

        private ConceptNode getParent() {
            return parent;
        }
    }
}
