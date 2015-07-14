package com.mcscm.fixtools.generator;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class XmlParser {

    private final Document doc;
    private XPath name;
    private Element rootEl;
    private final Map<String, XPathExpression> exprMap = new HashMap<>();

    public XmlParser(InputStream in)
            throws ParserConfigurationException,
            IOException,
            SAXException {
        doc = createBuilder().parse(in);
        rootEl = doc.getDocumentElement();
    }

    public XmlParser(String docPath) throws
            ParserConfigurationException,
            IOException,
            SAXException {
        doc = createBuilder().parse(docPath);
        rootEl = doc.getDocumentElement();
    }

    public static Node getChild(Node node, String name) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String field = child.getNodeName();
            if (name.equals(field)) {
                return child;
            }
        }

        return null;
    }

    public void addNode(Node node) {
        Element el = doc.getDocumentElement();
        el.appendChild(doc.importNode(node, true));
    }

    public Element getRootEl() {
        return rootEl;
    }

    private DocumentBuilder createBuilder() throws ParserConfigurationException {
        XPathFactory xPathFactory = XPathFactory.newInstance();
        name = xPathFactory.newXPath();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // never forget this!
        return factory.newDocumentBuilder();
    }


    public Object eval(String expr) throws XPathException {
        return eval(expr, XPathConstants.NODESET);
    }

    public Object eval(String expr, QName qName) throws XPathExpressionException {
        XPathExpression e = getXPathExpression(expr);
        if (e == null) return null;
        return e.evaluate(doc, qName);
    }

    private XPathExpression getXPathExpression(String expr) {
        XPathExpression e = exprMap.get(expr);
        if (e == null) {
            try {
                e = name.compile(expr);
            } catch (XPathException xe) {
                xe.printStackTrace();
                return null;
            }
            exprMap.put(expr, e);
        }
        return e;
    }

    public void writeXmlFile(String filename) {
        Writer out = null;
        try {
            Transformer tf = TransformerFactory.newInstance().newTransformer();
            tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tf.setOutputProperty(OutputKeys.INDENT, "yes");
            out = new FileWriter(filename);
            tf.transform(new DOMSource(doc), new StreamResult(out));


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }


    public void setValueToElement(String elName, String value) {
        try {
            NodeList nodeList = (NodeList) eval("//*");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element el = (Element) nodeList.item(i);
                if (el.getNodeName().equals(elName)) {
                    el.setTextContent(value);
                }
            }
        } catch (XPathException e) {
            e.printStackTrace();
        }
    }

    public void setNodeValue(String value, String... attr) {
        if (attr.length == 1) {
            setNodeValue(attr[0], value);
        } else if (attr.length == 2) {
            setNodeValue(attr[0], attr[1], value);
        }
    }

    public void setNodeValue(String finderAttr, String changeAttr, String value) {
        try {
            NodeList nodeList = (NodeList) eval(finderAttr);
            for (int n = 0; n < nodeList.getLength(); n++) {
                Node node = nodeList.item(n);
                if (node.hasAttributes()) {
                    NamedNodeMap ats = node.getAttributes();
                    for (int j = 0; j < ats.getLength(); j++) {
                        Node atr = ats.item(j);
                        if (atr.getNodeName().equals(changeAttr)) {
                            atr.setNodeValue(value);
                        }
                    }
                }
            }
        } catch (XPathException e) {
            e.printStackTrace();
        }
    }

    public void setNodeValue(String exp, String value) {
        try {
            NodeList nodeList = (NodeList) eval(exp);
            for (int n = 0; n < nodeList.getLength(); n++) {
                nodeList.item(n).setTextContent(value);
            }
        } catch (XPathException e) {
            e.printStackTrace();
        }
    }


    public static boolean validateAgainstXSD(InputStream xml, InputStream xsd) {
        try {
            SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new StreamSource(xsd));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(xml));
            return true;
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            return false;
        }
    }

    public void prepareComponentsMap(Map<String, Element> map) throws XPathException {
        final NodeList childNodes = (NodeList) eval("/fix/components/component");
        for (int j = 0; j < childNodes.getLength(); j++) {
            final Node childItem = childNodes.item(j);
            if (childItem == null || !(childItem instanceof Element)) continue;
            final Element el = (Element) childItem;
            String name = el.getAttribute("name");
            map.put(name, el);
        }
    }
}
