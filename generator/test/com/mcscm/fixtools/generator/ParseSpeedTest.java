package com.mcscm.fixtools.generator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParseSpeedTest {

    final XmlParser parser;
    private long sum;
    private final Map<String, Element> componentsMap = new HashMap<>();

    public ParseSpeedTest(String xmlIn) throws IOException, SAXException, ParserConfigurationException, XPathException {
        parser = new XmlParser(xmlIn);
        parser.prepareComponentsMap(componentsMap);
    }

    public int calcNodes(String expr) throws XPathException {
        NodeList nodes = (NodeList) parser.eval(expr);
        int cnt = 0;
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            if (node == null) continue;
            String name = node.getAttribute("name");
            if (name != null) {
                cnt++;
            }
        }
        return cnt;
    }

    public long traveseNodes(Element node) throws XPathException {
        sum = 0;
        forEach(node, 0, (f, i) -> sum++);
        return sum;
    }

    int forEach(Element node, int idx, ProtocolGenerator.Consumer<String> func) throws XPathException {
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            final Node item = node.getChildNodes().item(i);
            if (item == null || !(item instanceof Element)) continue;

            final Element el = (Element) item;
            final String elName = el.getNodeName();

            if ("field".equals(elName)) {
                final String fieldName = el.getAttribute("name");

                func.accept(fieldName, idx++);

            } else if ("component".equals(elName)) {
                final String compName = el.getAttribute("name");
//                final NodeList childNodes = (NodeList) parser.eval("/fix/components/component[@name='" + compName + "']");
//                for (int j = 0; j < childNodes.getLength(); j++) {
//                    final Node childItem = childNodes.item(j);
//                    if (childItem == null || !(childItem instanceof Element)) continue;
//                    idx = forEach((Element) childItem, idx, func);
//                }
                final Element childItem = componentsMap.get(compName);
                if (childItem != null) {
                    idx = forEach(childItem, idx, func);
                }
            } else if ("group".equals(elName)) {
                final String fieldName = el.getAttribute("name");
                func.accept(fieldName, idx++);
            }
        }
        return idx;
    }


    public double testExpression(String expr) throws XPathException {
        List<Long> results = new ArrayList<>();
        long sm = 0;
        for (int i = -100; i < 1000; i++) {
            long start = System.nanoTime();
            int cnt = calcNodes(expr);
            if (i > 0) {
                results.add(System.nanoTime() - start);
            }
            sm += cnt;
        }
        System.out.println("sm = " + sm);

        return results.stream().mapToLong(i -> i).average().getAsDouble();
    }

    public double testTravers(String expr) throws XPathException {
        NodeList nodes = (NodeList) parser.eval(expr);

        List<Long> results = new ArrayList<>();
        long sm = 0;
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            long start = System.nanoTime();
            long cnt = traveseNodes(node);
            if (i > 0) {
                results.add(System.nanoTime() - start);
            }
            sm += cnt;
        }
        System.out.println("sm = " + sm);
        return results.stream().mapToLong(i -> i).average().getAsDouble();
    }

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, XPathException {
        final String xmlIn = "./data/FIX50.xml";

        ParseSpeedTest test = new ParseSpeedTest(xmlIn);


//        System.out.printf("/fix/fields/field: %f, nanos", test.testExpression("/fix/fields/field"));
//        System.out.printf("/fix/fields/field: %f, nanos", test.testExpression("/fix/fields/field"));
//        System.out.printf("/fix/fields/field: %f, nanos", test.testExpression("/fix/fields/field"));
//        System.out.printf("/fix/fields/field: %f, nanos", test.testExpression("/fix/fields/field"));
        System.out.printf("travers: %f, nanos", test.testTravers("/fix/messages/message"));


    }
}
