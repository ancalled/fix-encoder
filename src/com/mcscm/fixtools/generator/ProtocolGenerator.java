package com.mcscm.fixtools.generator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ProtocolGenerator {

    public static final String DEFAULT_FIELD_SEP = "\\001";

    private final XmlParser parser;
    private final Map<String, FieldType> fieldTypes = new HashMap<>();
    private final String javaPackage;
    private final Path outDir;
    private final String fieldSep;

    public ProtocolGenerator(String xmlIn) throws IOException, SAXException, ParserConfigurationException {
        parser = new XmlParser(xmlIn);
        javaPackage = System.getProperty("package", "org.sample");
        fieldSep = System.getProperty("package", DEFAULT_FIELD_SEP);
        final String userDir = System.getProperty("user.dir");
        final String outHome = System.getProperty("out.home", userDir + "/gen-src");
        outDir = Paths.get(outHome, javaPackage.replace(".", "/"));
        Files.createDirectories(outDir);

    }

    public void generate() throws XPathException, IOException {
        NodeList nodes = (NodeList) parser.eval("/fix/fields/field");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            if (node == null) continue;

            int num = Integer.valueOf(node.getAttribute("number"));
            String name = node.getAttribute("name");
            Type type = Type.valueOf(node.getAttribute("type"));
            fieldTypes.put(name, new FieldType(num, name, type));
        }

        nodes = (NodeList) parser.eval("/fix/messages/message");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            if (node == null) continue;

            String fname = node.getAttribute("name") + ".java";

            String body = generateMessage(node);
            Files.write(outDir.resolve(fname), body.getBytes());
        }

    }

    private String generateMessage(Element node) {
        String name = node.getAttribute("name");

        StringBuilder sb = new StringBuilder();

        generatePackage(sb);
        generateImports(sb);

        sb.append("public class ").append(name).append(" implements FIXMessage {\n");
        sb.append("\n");

        generateFields(sb, node);

        generateInterfaceImpl(sb, node);

        sb.append("}\n");


        return sb.toString();
    }


    private void generatePackage(StringBuilder sb) {
        sb.append("package ").append(javaPackage).append(";\n\n");
    }

    private void generateImports(StringBuilder sb) {
        for (Type type : Type.values()) {
            if (!type.isJavaPrimitive()) {
                sb.append("import ").append(type.javaFullType).append(";\n");
            }
        }

        sb.append("import com.mcscm.fixtools.FIXMessage;\n");
        sb.append("import com.mcscm.fixtools.DateFormatter;\n");
        sb.append("\n");
    }

    private void generateFields(StringBuilder sb, Element node) {
        forEach(node, 0, (f, i) -> f.appendDefine(sb));
    }

    private void generateInterfaceImpl(StringBuilder sb, Element el) {
        String msgtype = el.getAttribute("msgtype");

        sb.append("\n");
        sb.append("\tpublic String getType() {\n").append("\t\treturn \"")
                .append(msgtype).append("\";\n").append("\t}\n");
        sb.append("\n");


        sb.append("\n");
        sb.append("\tpublic String encode() {\n");
        sb.append("\t\tfinal StringBuilder sb = new StringBuilder();\n");
        sb.append("\n\t\t//todo: append headers\n\n");

        generateEncode(sb, el);

        sb.append("\t\t//todo: append tails\n");

        sb.append("\n\t\treturn sb.toString();\n\n");
        sb.append("\t}\n");
        sb.append("\n");

        //-------------

        sb.append("\n");
        sb.append("\tpublic void decode(String fixmes) {\n");

        generateDecode(sb, el, "\t\t");


        sb.append("\t}\n");
        sb.append("\n");
    }

    private void generateEncode(StringBuilder sb, Element node) {
        forEach(node, 0, (f, i) -> f.appendEncode(sb, "\t\t", fieldSep));
    }

    private void generateDecode(StringBuilder sb, Element node, String tab) {
        String tab2 = tab + "\t";
        String tab3 = tab + "\t\t";

        sb.append(tab).append("int end, middle, start = 0;\n");
        sb.append(tab).append("for (;;) {\n");
        sb.append(tab2).append("end = fixmes.indexOf('").append(fieldSep).append("', start);\n");
        sb.append(tab2).append("middle = fixmes.indexOf('=', start);\n");
        sb.append(tab2).append("if (end < 0) break;\n");
        sb.append(tab2).append("int tag = Integer.valueOf(fixmes.substring(start, middle));\n");
        sb.append(tab2).append("String value = fixmes.substring(middle + 1, end);\n");

        forEach(node, 0, (f, i) -> {
            if (i > 0) {
                sb.append(" else ");
            } else {
                sb.append(tab2);
            }
            sb.append("if (tag == ").append(f.tag).append(") {\n");
            sb.append(tab3).append(f.fieldName).append(" = ")
                    .append(Type.genereateConvertMethod(f.type, "value")).append(";\n");
            sb.append(tab2).append("}");
        });

        sb.append("\n");
        sb.append(tab2).append("start = end + 1;\n");
        sb.append(tab).append("}\n");
    }


    // ---------------------------------------------------------------

    private void forEach(Element node, int idx, Consumer func) {
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            final Node item = node.getChildNodes().item(i);
            if (item == null || !(item instanceof Element)) continue;
            Element el = (Element) item;
            String elName = el.getNodeName();

            if ("field".equals(elName)) {
                String fieldName = el.getAttribute("name");
//                String required = el.getAttribute("required");
                FieldType field = fieldTypes.get(fieldName);
                if (field == null) {
                    throw new IllegalStateException("Could not find field: " + fieldName);
                }

                func.accept(field, idx++);

            } else if ("component".equals(elName)) {
                String compName = el.getAttribute("name");
                NodeList childNodes;
                try {
                    childNodes = (NodeList) parser.eval("/fix/components/component[@name='" + compName + "']");
                } catch (XPathException e) {
                    e.printStackTrace();
                    continue;
                }

                for (int j = 0; j < childNodes.getLength(); j++) {
                    final Node childItem = childNodes.item(j);
                    if (childItem == null || !(childItem instanceof Element)) continue;
                    forEach((Element) childItem, idx, func);
                }

            }
        }
    }


    public static class FieldType {
        public final int tag;
        public final String name;
        public final String fieldName;
        public final Type type;

        public FieldType(int tag, String name, Type type) {
            this.tag = tag;
            this.name = name;
            this.type = type;
            if (Character.isUpperCase(name.charAt(0))) {
                fieldName = Character.toLowerCase(name.charAt(0)) + name.substring(1);
            } else {
                fieldName = name;
            }
        }

        public void appendDefine(StringBuilder sb) {
            sb.append("\tpublic ").append(type.javaType).append(" ").append(fieldName).append(";\n");
        }

        public void appendEncode(StringBuilder sb, String tab, String sep) {
            String tab2 = tab + "\t";

            sb.append(tab).append("if (").append(fieldName)
                    .append(" != ").append(type.nullValue)
                    .append(") {\n");

            sb.append(tab2).append("sb.append(\"").append(tag).append("=\")");
            sb.append(".append(");
            if (type == Type.UTCTIMESTAMP) {
                sb.append("DateFormatter.format(").append(fieldName).append(")");
            } else {
                sb.append(fieldName);
            }
            sb.append(")");

            sb.append(".append(\"").append(sep).append("\")").append(";\n");

            sb.append(tab).append("}\n");
        }
    }


    @FunctionalInterface
    public interface Consumer {

        void accept(FieldType ft, int idx);
    }


    public static void main(String[] args) throws Exception {
        ProtocolGenerator generator = new ProtocolGenerator("./data/FIX_test.xml");
        generator.generate();
    }
}
