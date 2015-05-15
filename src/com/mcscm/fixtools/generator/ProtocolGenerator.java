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


        String tab = tab(level);
        String tab2 = tab + "\t";

        addImports(imports, node);


        StringBuilder bodySb = new StringBuilder();

        int processed = forEach(node, 0, (f, i) -> f.appendProperty(bodySb, tab2));

        bodySb.append(tab2).append("private final BitSet parsed = new BitSet(").append(processed).append(");\n");
        bodySb.append("\n");

        generateMethods(bodySb, node, tab2);
        generateSubClasses(bodySb, imports, node, level);

        //end of class
        bodySb.append(tab).append("}\n");

        //-------------------------------------
        StringBuilder rootSb = new StringBuilder();

        //class start
        String classname = node.getAttribute("name");
        if (!inner) {
            generatePackage(rootSb);
            rootSb.append("//Generated source\n\n");
            rootSb.append(generateImports(imports)).append("\n");

            rootSb.append("public class ").append(classname).append(" implements FIXMessage {\n\n");

        } else {
            rootSb.append(tab).append("public static class ").append(classname).append(" {\n\n");
        }

        rootSb.append(bodySb);

        return rootSb.toString();
    }

    private String generateEnum(String name, FieldType type, List<EnumValue> enumValues) {
        StringBuilder sb = new StringBuilder();

        sb.append("package ").append(javaPackage).append(".enums").append(";\n\n");
        sb.append("//Generated source\n\n");

        sb.append("public enum ").append(name).append(" {\n");
        String tab = "\t";
        String tab2 = tab + "\t";
        String tab3 = tab2 + "\t";

        String javaType = type.javaType;
        if (type == FieldType.NUMINGROUP) {
            javaType = "int";
        }

        final boolean charType = "char".equals(javaType);
        final boolean stringType = "String".equals(javaType);

        for (EnumValue e : enumValues) {
            sb.append(tab).append(e.description).append("(");
            if (charType) sb.append("'");
            if (stringType) sb.append("\"");
            sb.append(e.code);
            if (charType) sb.append("'");
            if (stringType) sb.append("\"");
            sb.append(")");
            sb.append(",\n");
        }

        sb.append(";\n");
        sb.append(tab).append("public ").append(javaType).append(" value;\n\n");
        sb.append(tab).append(name).append("(").append(javaType).append(" value) {\n");
        sb.append(tab2).append("this.value = value;\n");
        sb.append(tab).append("}\n\n");

        sb.append(tab).append("public static ").append(name).append(" getByValue(").append(javaType).append(" value) {\n");
        sb.append(tab2).append("for (").append(name).append(" e: values()) {\n");
        sb.append(tab3).append("if (e.value == value) return e;\n");
        sb.append(tab2).append("}\n");
        sb.append(tab2).append("return null;\n");
        sb.append(tab).append("}\n");


        sb.append("}\n");
        return sb.toString();
    }

    private void generatePackage(StringBuilder sb) {
        sb.append("package ").append(javaPackage).append(";\n\n");
    }

    private void addImports(Set<String> imports, final Element node) {
        forEach(node, 0, (f, i) -> {
            if (f.enumField) {
                imports.add(javaPackage + ".enums." + f.name);
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
    }

    private String generateImports(Set<String> imports) {
        StringBuilder sb = new StringBuilder();
        imports.stream().map(i -> "import " + i + ";\n").forEach(sb::append);
        return sb.toString();
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

            if (f.type == FieldType.NUMINGROUP) {
                sb.append(tab4).append("int items = Integer.valueOf(value);\n");
                sb.append(tab4).append("int groupEnd = end + 1;\n");
                sb.append(tab4).append("for (int i = 0; i < items; i++) {\n");
                sb.append(tab5).append(f.name).append(" item = new ").append(f.name).append("();\n");
                sb.append(tab5).append("groupEnd = item.decode(fixmes, groupEnd);\n");
                sb.append(tab5).append("add").append(f.name).append("(item);\n");
                sb.append(tab4).append("}\n");
                sb.append(tab4).append("end = groupEnd - 1;\n");


            } else {
                String assignment = FieldType.generateConvertMethod(f.type, "value");
                if (f.enumField) {
                    assignment = f.name + ".getByValue(" + assignment + ")";
                }
                sb.append(tab4).append(f.fieldName).append(" = ")
                        .append(assignment).append(";\n");
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

    public static String tab(int level) {
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


    public static class FieldDescriptor {
        public final int tag;
        public final String name;
        public final String fieldName;
        public final String fieldNameSingle;
        public final FieldType type;
        public String groupClass;
        public final boolean enumField;

        public FieldDescriptor(int tag, String name, FieldType type, boolean enumField) {
            this.tag = tag;
            this.name = name;
            this.type = type;
            this.enumField = enumField;
            if (Character.isUpperCase(name.charAt(0))) {
                fieldName = Character.toLowerCase(name.charAt(0)) + name.substring(1);
            } else {
                fieldName = name;
            }

            if (type == FieldType.NUMINGROUP) {
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
            if (type == FieldType.NUMINGROUP) {
                sb.append("List");
                if (groupClass != null) {
                    sb.append("<").append(groupClass).append(">");
                }
            } else {
                if (!enumField) {
                    sb.append(type.javaType);
                } else {
                    sb.append(name);
                }
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
            if (type == FieldType.NUMINGROUP) {
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

            String nullValue = enumField ? "null" : type.nullValue;

            sb.append(tab).append("if (").append(fieldName)
                    .append(" != ").append(nullValue)
                    .append(") {\n");


            sb.append(tab2).append("sb.append(\"").append(tag).append("=\")");
            sb.append(".append(");
            if (type == FieldType.UTCTIMESTAMP) {
                sb.append("DateFormatter.formatAsDateTime(").append(fieldName).append(")");

            } else if (type == FieldType.UTCDATE || type == FieldType.UTCDATEONLY || type == FieldType.LOCALMKTDATE) {
                sb.append("DateFormatter.formatAsDate(").append(fieldName).append(")");

            } else if (type == FieldType.UTCTIMEONLY || type == FieldType.TIME) {
                sb.append("DateFormatter.formatAsTime(").append(fieldName).append(")");

            } else if (type == FieldType.NUMINGROUP) {
                sb.append("this.").append(fieldName).append(".size()");
            } else if (enumField) {
                sb.append(fieldName).append(".value");
            } else {
                sb.append(fieldName);
            }
            sb.append(")");
            sb.append(".append(\"").append(sep).append("\")").append(";\n");

            if (type == FieldType.NUMINGROUP) {
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
