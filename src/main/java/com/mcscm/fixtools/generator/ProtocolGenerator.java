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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.String.format;

public class ProtocolGenerator {

    public static final String DEFAULT_FIELD_SEP = "\\001";

    private final XmlParser parser;
    private final Map<String, FieldDescriptor> fieldTypes = new HashMap<>();
    private final String javaPackage;
    private final Path outDir;
    private final Path enumsDir;
    private final String fieldSep;

    public ProtocolGenerator(String xmlIn) throws IOException, SAXException, ParserConfigurationException {
        parser = new XmlParser(xmlIn);
        javaPackage = System.getProperty("package", "org.sample");
        fieldSep = System.getProperty("package", DEFAULT_FIELD_SEP);
        final String userDir = System.getProperty("user.dir");
        final String outHome = System.getProperty("out.home", userDir + "/gen-src");
        outDir = Paths.get(outHome, javaPackage.replace(".", "/"));
        enumsDir = outDir.resolve("enums");
        Files.createDirectories(outDir);
        Files.createDirectories(enumsDir);
    }

    public void generate() throws IOException {
        System.out.println("Parsing types...");
        NodeList nodes = eval("/fix/fields/field");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            if (node == null) continue;

            int num = Integer.valueOf(node.getAttribute("number"));
            String name = node.getAttribute("name");
            String typeStr = node.getAttribute("type");
            typeStr = typeStr.replaceAll("-", "");
            FieldType type = FieldType.valueOf(typeStr);

            List<EnumValue> enumValues = getEnumValues(node);
            boolean enumField = !enumValues.isEmpty();
            if (enumField) {
                System.out.println("\tGenerating enum " + name + "...");
                String body = generateEnum(name, type, enumValues);
                Files.write(enumsDir.resolve(name + ".java"), body.getBytes());
            }

            fieldTypes.put(name, new FieldDescriptor(num, name, type, enumField));
        }

        System.out.println("Parsing messages...");

        nodes = eval("/fix/messages/message");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            if (node == null) continue;

            final String name = node.getAttribute("name");
            String fname = name + ".java";
            System.out.println("\tGenerating class " + name + "...");


            String body = generateClass(node, false, 0, new HashSet<>());
            Files.write(outDir.resolve(fname), body.getBytes());
        }

    }

    private String generateClass(Element node, boolean inner, int level, Set<String> imports) {

        String indent = indent(level);

        String classname = node.getAttribute("name");

        addImports(imports, node, classname);

        StringBuilder bodySb = new StringBuilder();

        generateConstants(bodySb, node, indent);

        int processed = forEach(node, 0, (f, i) -> f.appendProperty(bodySb, indent + "    ", classname, javaPackage));
        System.out.printf("\tgenerated %d class properties\n", processed);

        bodySb.append(indent).append("    private final BitSet parsed = new BitSet(").append(processed).append(");\n");
        bodySb.append("\n");

        generateMethods(bodySb, node, indent + "    ", classname);
        generateSubClasses(bodySb, imports, node, level);

        //end of class
        bodySb.append(indent).append("}\n");

        //-------------------------------------
        StringBuilder rootSb = new StringBuilder();

        //class start
        if (!inner) {
            generatePackage(rootSb);
            rootSb.append("//Generated source\n\n");
            generateImports(rootSb, imports);
            rootSb.append("\n");

            rootSb.append("public class ").append(classname).append(" implements FIXMessage {\n\n");

        } else {
            rootSb.append(indent).append("public static class ").append(classname).append(" {\n\n");
        }

        rootSb.append(bodySb);

        return rootSb.toString();
    }

    private String generateEnum(String name, FieldType type, List<EnumValue> enumValues) {
        StringBuilder sb = new StringBuilder();

        sb.append("package ").append(javaPackage).append(".enums").append(";\n\n");
        sb.append("//Generated source\n\n");

        sb.append("public enum ").append(name).append(" {\n");
        String indent = "\t";

        String javaType = type.javaType;
        if (type == FieldType.NUMINGROUP) {
            javaType = "int";
        }

        final boolean charType = "char".equals(javaType);
        final boolean stringType = "String".equals(javaType);

        for (EnumValue e : enumValues) {
            sb.append(indent).append(e.description).append("(");
            if (charType) sb.append("'");
            if (stringType) sb.append("\"");
            sb.append(e.code);
            if (charType) sb.append("'");
            if (stringType) sb.append("\"");
            sb.append(")");
            sb.append(",\n");
        }

        sb.append(";\n");

        sb.append(format(
                indent + "public %s value;\n\n" +

                        indent + "%s(%s value) {\n" +
                        indent + "    this.value = value;\n" +
                        indent + "}\n\n",
                javaType, name, javaType
        ));

        sb.append(format(
                indent + "public static %s getByValue(%s value) {\n" +
                        indent + "    for (%s e: values()) {\n" +
                        indent + "        if (e.value == value) return e;\n" +
                        indent + "    };\n" +
                        indent + "    return null;\n" +
                        indent + "}\n",
                name, javaType, name
        ));

        sb.append("}\n");
        return sb.toString();
    }

    private void generatePackage(StringBuilder sb) {
        sb.append("package ").append(javaPackage).append(";\n\n");
    }

    private void addImports(Set<String> imports, final Element node, String className) {
        forEach(node, 0, (f, i) -> {
            if (f.enumField) {
                if (!className.equals(f.name)) {
                    imports.add(javaPackage + ".enums." + f.name);
                }

            } else if (!f.type.isJavaPrimitive()) {
                imports.add(f.type.javaFullType);

                if ("java.util.Date".equals(f.type.javaFullType)) {
                    imports.add("com.mcscm.fixtools.DateFormatter");

                } else if (f.type == FieldType.NUMINGROUP) {
                    imports.add("java.util.ArrayList");
                }
            }
        });

        imports.add("com.mcscm.fixtools.FIXMessage");
        imports.add("java.util.BitSet");
        imports.add("java.nio.ByteBuffer");
    }

    private void generateImports(StringBuilder sb, Set<String> imports) {
        imports.stream().map(i -> "import " + i + ";\n").forEach(sb::append);
    }

    private void generateConstants(StringBuilder sb, Element node, String indent) {
        sb.append(indent).append(String.format("    public static final byte[] SEP = \"%s\".getBytes();\n", fieldSep));
        sb.append(indent).append("    public static final byte[] EQ = \"=\".getBytes();\n");

//        forEach(node, 0, (f, i) -> f.appendFieldConstant(sb, indent + "    "));

        sb.append("\n");
    }

    private void generateMethods(StringBuilder sb, Element el, String indent, String className) {

        sb.append(format(
                indent + "public String getType() {\n" +
                        indent + "    return \"%s\";\n" +
                        indent + "}\n\n",
                el.getAttribute("msgtype")));

        generatePropertiesAccess(sb, el, indent);
        generateHasValues(sb, el, indent);
        generateEncodeMethod(sb, el, indent);
        generateEncode2Method(sb, el, indent);
        generateDecodeMethod(sb, el, indent, className);
        sb.append("\n");
    }

    private void generatePropertiesAccess(StringBuilder sb, Element node, String indent) {
        forEach(node, 0, (f, i) -> f.appendPropertyAccess(sb, indent));
    }

    private void generateHasValues(StringBuilder sb, Element node, String indent) {
        forEach(node, 0, (f, i) ->
                sb.append(format(
                        indent + "public boolean has%s () {\n" +
                                indent + "    return parsed.get(%d);\n" +
                                indent + "}\n\n",
                        f.name, i)));
        sb.append("\n");
    }

    private void generateEncodeMethod(StringBuilder sb, Element node, String indent) {

        sb.append(indent).append("public String encode() {\n");
        sb.append(indent).append("    final StringBuilder sb = new StringBuilder();\n");
        forEach(node, 0, (f, i) -> f.appendEncode(sb, indent + "    ", fieldSep));
        sb.append("\n");
        sb.append(indent).append("    return sb.toString();\n");
        sb.append(indent).append("}\n\n");
    }

    private void generateEncode2Method(StringBuilder sb, Element node, String indent) {

        sb.append(indent).append("public void encode2(ByteBuffer buf) {\n");

        forEach(node, 0, (f, i) -> f.appendEncode2(sb, indent + "    "));
        sb.append("\n");

        sb.append(indent).append("}\n\n");
    }

    private void generateDecodeMethod(StringBuilder sb, Element node, String indent, String className) {
        sb.append(format(
                indent + "public int decode(String fixmes) {\n" +
                        indent + "    return decode(fixmes, 0);\n" +
                        indent + "}\n\n" +

                        indent + "public int decode(String fixmes, int fromIdx) {\n" +
                        indent + "    parsed.clear();\n" +
                        indent + "    int end, middle, start = fromIdx;\n" +
                        indent + "    for (;;) {\n" +
                        indent + "        end = fixmes.indexOf('%s', start);\n" +
                        indent + "        middle = fixmes.indexOf('=', start);\n" +
                        indent + "        if (end < 0) break;\n" +
                        indent + "        int tag = Integer.valueOf(fixmes.substring(start, middle));\n" +
                        indent + "        String value = fixmes.substring(middle + 1, end);\n"
                , fieldSep));

        forEach(node, 0, (f, i) -> {
            sb.append(i > 0 ? " else " : indent + "        ");
            sb.append("if (tag == ").append(f.tag).append(") {\n");
            sb.append(indent).append("            if (parsed.get(").append(i).append(")) {\n");
            sb.append(indent).append("                end = start;\n");
            sb.append(indent).append("                break;\n");
            sb.append(indent).append("            }\n");

            if (f.type == FieldType.NUMINGROUP) {
                sb.append(format(
                        indent + "            int items = Integer.valueOf(value);\n" +
                                indent + "            int groupEnd = end + 1;\n" +
                                indent + "            for (int i = 0; i < items; i++) {\n" +
                                indent + "                %s item = new %s();\n" +
                                indent + "                groupEnd = item.decode(fixmes, groupEnd);\n" +
                                indent + "                add%s(item);\n" +
                                indent + "            }\n" +
                                indent + "            parsed.set(%d);\n" +
                                indent + "            end = groupEnd - 1;\n" +
                                indent + "            if (end < 0) break;\n",
                        f.name, f.name, f.name, i));
            } else {
                String assignment = FieldType.generateConvertMethod(f.type, "value");
                if (f.enumField) {
                    String enumName =
                            f.name.equals(className) ?
                                    javaPackage + ".enums." + f.name :
                                    f.name;
                    assignment = enumName + ".getByValue(" + assignment + ")";
                }
                sb.append(format(
                        indent + "            %s = %s;\n" +
                                indent + "            parsed.set(%d);\n",
                        f.fieldName, assignment, i));
            }

            sb.append(indent).append("        }");
        });

        sb.append(" else {\n");
        sb.append(indent).append("            end = start;\n");
        sb.append(indent).append("            break;\n");
        sb.append(indent).append("        }\n");

        sb.append("\n");
        sb.append(indent).append("        start = end + 1;\n");
        sb.append(indent).append("    }\n\n");
        sb.append(indent).append("    return end;\n");

        sb.append(indent).append("}\n\n");
    }

    private void generateSubClasses(StringBuilder sb, Set<String> imports, Element node, int level) {
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            final Node item = node.getChildNodes().item(i);
            if (item == null || !(item instanceof Element)) continue;

            final Element el = (Element) item;
            final String elName = el.getNodeName();

            if ("group".equals(elName)) {
                String body = generateClass(el, true, level + 1, imports);
                sb.append(body);
                sb.append("\n");

            } else if ("component".equals(elName)) {
                final String compName = el.getAttribute("name");
                final NodeList childNodes = eval("/fix/components/component[@name='" + compName + "']");

                for (int j = 0; j < childNodes.getLength(); j++) {
                    final Node childItem = childNodes.item(j);
                    if (childItem == null || !(childItem instanceof Element)) continue;
                    generateSubClasses(sb, imports, (Element) childItem, level);
                }
            }
        }
    }


    // ---------------------------------------------------------------

    private int forEach(Element node, int idx, Consumer<FieldDescriptor> func) {
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            final Node item = node.getChildNodes().item(i);
            if (item == null || !(item instanceof Element)) continue;

            final Element el = (Element) item;
            final String elName = el.getNodeName();

            if ("field".equals(elName)) {
                final String fieldName = el.getAttribute("name");
                final FieldDescriptor field = fieldTypes.get(fieldName);
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
                final FieldDescriptor field = fieldTypes.get(fieldName);
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

    public static String indent(int level) {
        return IntStream.range(0, level).mapToObj(i -> "\t").collect(Collectors.joining());
    }

    public static List<EnumValue> getEnumValues(Node node) {
        List<EnumValue> values = new ArrayList<>();
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            final Node item = node.getChildNodes().item(i);
            if (item == null || !(item instanceof Element)) continue;

            final Element el = (Element) item;
            if ("value".equals(el.getNodeName()) && el.hasAttribute("enum")) {
                values.add(new EnumValue(el.getAttribute("enum"), el.getAttribute("description")));
            }
        }

        return values;
    }


    @FunctionalInterface
    public interface Consumer<T> {

        void accept(T t, int idx);
    }

    public static class EnumValue {
        public final String code;
        public final String description;

        public EnumValue(String code, String description) {
            this.code = code;
            this.description = description;
        }
    }


    public static void main(String[] args) throws Exception {
        final String xmlIn = "./data/FIX_test.xml";
//        final String xmlIn = "./data/FIX50.xml";
        ProtocolGenerator generator = new ProtocolGenerator(xmlIn);
        generator.generate();
    }
}
