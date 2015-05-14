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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    public void generate() throws IOException {
        NodeList nodes = eval("/fix/fields/field");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            if (node == null) continue;

            int num = Integer.valueOf(node.getAttribute("number"));
            String name = node.getAttribute("name");
            Type type = Type.valueOf(node.getAttribute("type"));
            fieldTypes.put(name, new FieldType(num, name, type));
        }

        nodes = eval("/fix/messages/message");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            if (node == null) continue;

            String fname = node.getAttribute("name") + ".java";

            String body = generateMessage(node, false, 0);
            Files.write(outDir.resolve(fname), body.getBytes());
        }

    }

    private String generateMessage(Element node, boolean inner, int level) {
        String name = node.getAttribute("name");

        StringBuilder sb = new StringBuilder();
        String tab = tab(level);
        String tab2 = tab + "\t";

        if (!inner) {
            generatePackage(sb);
            sb.append("//Generated source\n");

            generateImports(sb);

            sb.append("public class ").append(name).append(" implements FIXMessage {\n\n");
        } else {
            sb.append(tab).append("public static class ").append(name).append(" {\n\n");
        }

        forEach(node, 0, (f, i) -> f.appendDefine(sb, tab2));

        generateInterfaceImpl(sb, node, tab2);

        generateSubClasess(sb, node, level);
        sb.append(tab).append("}\n");


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


    private void generateInterfaceImpl(StringBuilder sb, Element el, String tab) {
        String msgtype = el.getAttribute("msgtype");

        String tab2 = tab + "\t";

        sb.append("\n");
        sb.append(tab).append("public String getType() {\n").append(tab2).append("return \"")
                .append(msgtype).append("\";\n").append(tab).append("}\n");

        sb.append("\n");
        sb.append(tab).append("public String encode() {\n");
        sb.append(tab2).append("final StringBuilder sb = new StringBuilder();\n");

        generateEncode(sb, el, tab2);

        sb.append("\n").append(tab2).append("return sb.toString();\n");
        sb.append(tab).append("}\n");
        sb.append("\n");

        //-------------

        sb.append("\n");
        sb.append(tab).append("public void decode(String fixmes) {\n");

        generateDecode(sb, el, tab2);

        sb.append(tab).append("}\n");
        sb.append("\n");
    }

    private void generateEncode(StringBuilder sb, Element node, String tab) {
        forEach(node, 0, (f, i) -> f.appendEncode(sb, tab, fieldSep));
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

    private void generateSubClasess(StringBuilder sb, Element node, int level) {
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            final Node item = node.getChildNodes().item(i);
            if (item == null || !(item instanceof Element)) continue;

            final Element el = (Element) item;
            final String elName = el.getNodeName();

            if ("group".equals(elName)) {
                String body = generateMessage(el, true, level + 1);
                sb.append(body);
                sb.append("\n");

            } else if ("component".equals(elName)) {
                final String compName = el.getAttribute("name");
                final NodeList childNodes = eval("/fix/components/component[@name='" + compName + "']");

                for (int j = 0; j < childNodes.getLength(); j++) {
                    final Node childItem = childNodes.item(j);
                    if (childItem == null || !(childItem instanceof Element)) continue;
                    generateSubClasess(sb, (Element) childItem, level);
                }
            }
        }
    }


    // ---------------------------------------------------------------

    private void forEach(Element node, int idx, Consumer<FieldType> func) {
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            final Node item = node.getChildNodes().item(i);
            if (item == null || !(item instanceof Element)) continue;

            final Element el = (Element) item;
            final String elName = el.getNodeName();

            if ("field".equals(elName)) {
                final String fieldName = el.getAttribute("name");
                final FieldType field = fieldTypes.get(fieldName);
                if (field == null) {
                    throw new GeneratorException("Could not find field: " + fieldName);
                }

                func.accept(field, idx++);

            } else if ("component".equals(elName)) {
                final String compName = el.getAttribute("name");
                final NodeList childNodes = eval("/fix/components/component[@name='" + compName + "']");

                for (int j = 0; j < childNodes.getLength(); j++) {
                    final Node childItem = childNodes.item(j);
                    if (childItem == null || !(childItem instanceof Element)) continue;
                    forEach((Element) childItem, idx, func);
                }
            } else if ("group".equals(elName)) {
                final String fieldName = el.getAttribute("name");
                final FieldType field = fieldTypes.get(fieldName);
                if (field == null) {
                    throw new GeneratorException("Could not find field: " + fieldName);
                }

                func.accept(field, idx++);
            }
        }
    }

    private NodeList eval(String expr) {
        try {
            return (NodeList) parser.eval(expr);
        } catch (XPathException e) {
            throw new GeneratorException(e);
        }
    }

    public static String tab(int level) {
        return IntStream.range(0, level).mapToObj(i -> "\t").collect(Collectors.joining());
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

        public void appendDefine(StringBuilder sb, String tab) {
            sb.append(tab).append("public ").append(type.javaType).append(" ").append(fieldName).append(";\n");
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
    public interface Consumer<T> {

        void accept(T t, int idx);
    }


    public static void main(String[] args) throws Exception {
        ProtocolGenerator generator = new ProtocolGenerator("./data/FIX_test.xml");
        generator.generate();
    }
}
