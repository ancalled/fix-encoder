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

            String body = generateClass(node, false, 0);
            Files.write(outDir.resolve(fname), body.getBytes());
        }

    }

    private String generateClass(Element node, boolean inner, int level) {

        StringBuilder sb = new StringBuilder();
        String tab = tab(level);
        String tab2 = tab + "\t";

        //class start
        String classname = node.getAttribute("name");
        if (!inner) {
            generatePackage(sb);
            sb.append("//Generated source\n\n");
            generateImports(sb);
            sb.append("public class ").append(classname).append(" implements FIXMessage {\n\n");

        } else {
            sb.append(tab).append("public static class ").append(classname).append(" {\n\n");
        }

        int processed = forEach(node, 0, (f, i) -> f.appendProperty(sb, tab2));
        sb.append(tab2).append("private final BitSet parsed = new BitSet(").append(processed).append(");\n");
        sb.append("\n");

        generateMethods(sb, node, tab2);
        generateSubClasses(sb, node, level);

        //end of class
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
        sb.append("import java.util.ArrayList;\n");
        sb.append("import java.util.BitSet;\n");
        sb.append("\n");
    }


    private void generateMethods(StringBuilder sb, Element el, String tab) {

        String tab2 = tab + "\t";

        String msgtype = el.getAttribute("msgtype");
        sb.append(tab).append("public String getType() {\n");
        sb.append(tab2).append("return \"").append(msgtype).append("\";\n");
        sb.append(tab).append("}\n\n");

        generatePropertiesAccess(sb, el, tab);
        generateHasValues(sb, el, tab);

        generateEncode(sb, el, tab);
        generateDecode(sb, el, tab);
        sb.append("\n");
    }

    private void generatePropertiesAccess(StringBuilder sb, Element node, String tab) {
        forEach(node, 0, (f, i) -> f.appendPropertyAccess(sb, tab));
        sb.append("\n");
    }

    private void generateHasValues(StringBuilder sb, Element node, String tab) {
        forEach(node, 0, (f, i) -> {
            sb.append(tab).append("public boolean has").append(f.name).append("() {\n");
            sb.append(tab).append("\t").append("return parsed.get(").append(i).append(");\n");
            sb.append(tab).append("}\n\n");
        });
        sb.append("\n");
    }

    private void generateEncode(StringBuilder sb, Element node, String tab) {
        String tab2 = tab + "\t";
        sb.append(tab).append("public String encode() {\n");
        sb.append(tab2).append("final StringBuilder sb = new StringBuilder();\n");

        forEach(node, 0, (f, i) -> f.appendEncode(sb, tab2, fieldSep));

        sb.append("\n").append(tab2).append("return sb.toString();\n");
        sb.append(tab).append("}\n\n");
    }

    private void generateDecode(StringBuilder sb, Element node, String tab) {
        String tab2 = tab + "\t";
        String tab3 = tab2 + "\t";
        String tab4 = tab3 + "\t";
        String tab5 = tab4 + "\t";


        sb.append(tab).append("public int decode(String fixmes) {\n");
        sb.append(tab2).append("return decode(fixmes, 0);\n");
        sb.append(tab).append("}\n\n");


        sb.append(tab).append("public int decode(String fixmes, int fromIdx) {\n");

        sb.append(tab2).append("parsed.clear();\n");
        sb.append(tab2).append("int end, middle, start = fromIdx;\n");
        sb.append(tab2).append("for (;;) {\n");
        sb.append(tab3).append("end = fixmes.indexOf('").append(fieldSep).append("', start);\n");
        sb.append(tab3).append("middle = fixmes.indexOf('=', start);\n");
        sb.append(tab3).append("if (end < 0) break;\n");
        sb.append(tab3).append("int tag = Integer.valueOf(fixmes.substring(start, middle));\n");
        sb.append(tab3).append("String value = fixmes.substring(middle + 1, end);\n");

        forEach(node, 0, (f, i) -> {
            if (i > 0) {
                sb.append(" else ");
            } else {
                sb.append(tab3);
            }
            sb.append("if (tag == ").append(f.tag).append(") {\n");
            sb.append(tab4).append("if (parsed.get(").append(i).append(")) {\n");
            sb.append(tab5).append("end = start;\n");
            sb.append(tab5).append("break;\n");
            sb.append(tab4).append("}\n");
            if (f.type == Type.NUMINGROUP) {

                sb.append(tab4).append("int items = Integer.valueOf(value);\n");
                sb.append(tab4).append("int groupEnd = end + 1;\n");
                sb.append(tab4).append("for (int i = 0; i < items; i++) {\n");
                sb.append(tab5).append(f.name).append(" item = new ").append(f.name).append("();\n");
                sb.append(tab5).append("groupEnd = item.decode(fixmes, groupEnd);\n");
                sb.append(tab5).append("add").append(f.name).append("(item);\n");
                sb.append(tab4).append("}\n");
                sb.append(tab4).append("end = groupEnd - 1;\n");


            } else {
                sb.append(tab4).append(f.fieldName).append(" = ")
                        .append(Type.genereateConvertMethod(f.type, "value")).append(";\n");
            }

            sb.append(tab4).append("parsed.set(").append(i).append(");\n");

            sb.append(tab3).append("}");
        });

        sb.append(" else {\n");
        sb.append(tab4).append("end = start;\n");
        sb.append(tab4).append("break;\n");
        sb.append(tab3).append("}\n");

        sb.append("\n");
        sb.append(tab3).append("start = end + 1;\n");
        sb.append(tab2).append("}\n\n");
        sb.append(tab2).append("return end;\n");

        sb.append(tab).append("}\n\n");
    }

    private void generateSubClasses(StringBuilder sb, Element node, int level) {
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            final Node item = node.getChildNodes().item(i);
            if (item == null || !(item instanceof Element)) continue;

            final Element el = (Element) item;
            final String elName = el.getNodeName();

            if ("group".equals(elName)) {
                String body = generateClass(el, true, level + 1);
                sb.append(body);
                sb.append("\n");

            } else if ("component".equals(elName)) {
                final String compName = el.getAttribute("name");
                final NodeList childNodes = eval("/fix/components/component[@name='" + compName + "']");

                for (int j = 0; j < childNodes.getLength(); j++) {
                    final Node childItem = childNodes.item(j);
                    if (childItem == null || !(childItem instanceof Element)) continue;
                    generateSubClasses(sb, (Element) childItem, level);
                }
            }
        }
    }


    // ---------------------------------------------------------------

    private int forEach(Element node, int idx, Consumer<FieldType> func) {
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
                    idx = forEach((Element) childItem, idx, func);
                }
            } else if ("group".equals(elName)) {
                final String fieldName = el.getAttribute("name");
                final FieldType field = fieldTypes.get(fieldName);
                if (field == null) {
                    throw new GeneratorException("Could not find field: " + fieldName);
                }
                field.groupClass = fieldName;

                func.accept(field, idx++);
            }
        }
        return idx;
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
        public final String fieldNameSingle;
        public final Type type;
        public String groupClass;

        public FieldType(int tag, String name, Type type) {
            this.tag = tag;
            this.name = name;
            this.type = type;
            if (Character.isUpperCase(name.charAt(0))) {
                fieldName = Character.toLowerCase(name.charAt(0)) + name.substring(1);
            } else {
                fieldName = name;
            }

            if (type == Type.NUMINGROUP) {
                if (fieldName.endsWith("ies")) {
                    fieldNameSingle = fieldName;
                } else {
                    fieldNameSingle = fieldName;
                }
            } else {
                fieldNameSingle = null;
            }
        }


        public void appendProperty(StringBuilder sb, String tab) {
            sb.append(tab).append("public ");
            if (type == Type.NUMINGROUP) {
                sb.append("List");
                if (groupClass != null) {
                    sb.append("<").append(groupClass).append(">");
                }
            } else {
                sb.append(type.javaType);
            }
            sb.append(" ").append(fieldName);

//            if (type == Type.NUMINGROUP) {
//                sb.append(" = new ArrayList<>()");
//            }
            sb.append(";\n");
        }

        public void appendPropertyAccess(StringBuilder sb, String tab) {
            String tab2 = tab + "\t";
            String tab3 = tab2 + "\t";
            if (type == Type.NUMINGROUP) {
                sb.append(tab).append("public void add").append(name)
                        .append("(").append(name).append(" ").append(fieldName).append(") {\n");

                sb.append(tab2).append("if (this.").append(fieldName).append(" == null) {\n");
                sb.append(tab3).append("this.").append(fieldName).append(" = new ArrayList<>();\n");
                sb.append(tab2).append("}\n");

                sb.append(tab2).append("this.").append(fieldName).append(".add(").append(fieldName).append(");\n");

                sb.append(tab).append("}\n");

            }
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
            } else if (type == Type.NUMINGROUP) {
                sb.append("this.").append(fieldName).append(".size()");
            } else {
                sb.append(fieldName);
            }
            sb.append(")");
            sb.append(".append(\"").append(sep).append("\")").append(";\n");

            if (type == Type.NUMINGROUP) {
                String tab3 = tab2 + "\t";
                sb.append(tab2).append("for (").append(name).append(" it: this.").append(fieldName).append(") {\n");
                sb.append(tab3).append("sb.append(it.encode());\n");
                sb.append(tab2).append("}\n");
            }

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
//
//        System.out.println(English.plural("entry", 2));
    }
}
