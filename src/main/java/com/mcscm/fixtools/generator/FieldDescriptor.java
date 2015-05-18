package com.mcscm.fixtools.generator;

import static java.lang.String.format;

public class FieldDescriptor {
    public final int tag;
    public final String name;
    public final String fieldName;
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
    }

    public void appendProperty(StringBuilder sb, String indent, String className, String packageName) {
        sb.append(indent).append("public ");
        if (type == FieldType.NUMINGROUP) {
            sb.append("List");
            if (groupClass != null) {
                sb.append("<").append(groupClass).append(">");
            }
        } else {
            if (!enumField) {
                sb.append(type.javaType);
            } else {
                if (className.equals(name)) {
                    sb.append(packageName).append(".enums.").append(name);
                } else {
                    sb.append(name);
                }
            }
        }
        sb.append(" ").append(fieldName);

//            if (type == Type.NUMINGROUP) {
//                sb.append(" = new ArrayList<>()");
//            }
        sb.append(";\n");
    }

    public void appendFieldConstant(StringBuilder sb, String indent) {
        sb.append(indent);
        sb.append(String.format("public static final byte[] TAG_%S = \"%d\".getBytes();\n", name, tag));
    }

    public void appendPropertyAccess(StringBuilder sb, String indent) {
        if (type == FieldType.NUMINGROUP) {
            sb.append(indent).append("public void add").append(name)
                    .append("(").append(name).append(" ").append(fieldName).append(") {\n");

            sb.append(indent).append("    if (this.").append(fieldName).append(" == null) {\n");
            sb.append(indent).append("        this.").append(fieldName).append(" = new ArrayList<>();\n");
            sb.append(indent).append("    }\n");

            sb.append(indent).append("    this.").append(fieldName).append(".add(").append(fieldName).append(");\n");

            sb.append(indent).append("}\n");

        }
    }

    public void appendEncode(StringBuilder sb, String indent, char sep) {

        String nullValue = enumField ? "null" : type.nullValue;
        sb.append(format(
                indent + "if (%s != %s) {\n",
                fieldName, nullValue));

        sb.append(indent).append("    sb.append(\"").append(tag).append("=\")");
        sb.append(".append(");
        if (type == FieldType.UTCTIMESTAMP || type == FieldType.TZTIMESTAMP) {
            sb.append("DateFormatter.formatAsDateTime(").append(fieldName).append(")");

        } else if (type == FieldType.UTCDATE || type == FieldType.UTCDATEONLY || type == FieldType.LOCALMKTDATE) {
            sb.append("DateFormatter.formatAsDate(").append(fieldName).append(")");

        } else if (type == FieldType.UTCTIMEONLY || type == FieldType.TIME || type == FieldType.TZTIMEONLY) {
            sb.append("DateFormatter.formatAsTime(").append(fieldName).append(")");

        } else if (type == FieldType.NUMINGROUP) {
            sb.append("this.").append(fieldName).append(".size()");
        } else if (enumField) {
            sb.append(fieldName).append(".value");
        } else {
            sb.append(fieldName);
        }
        sb.append(")");
        sb.append(".append('").append(sep).append("')").append(";\n");

        if (type == FieldType.NUMINGROUP) {
            sb.append(format(
                    indent + "    for (%s it: this.%s) {\n" +
                            indent + "        sb.append(it.encode());\n" +
                            indent + "    }\n",
                    name, fieldName
            ));
        }

        sb.append(indent).append("}\n");
    }


    public void appendEncode2(StringBuilder sb, String indent) {
        String nullValue = enumField ? "null" : type.nullValue;
        sb.append(format(
                indent + "if (%s != %s) {\n",
                fieldName, nullValue));

        String mthd = "";
        switch (type.javaType) {
            case "String":
//                mthd = "buf.put(%s.getBytes())";
                mthd = "EncodeUtils.put(buf, %s)";
                break;
            case "int":
            case "List":
//                mthd = "buf.put(Integer.toString(%s).getBytes())";
                mthd = "EncodeUtils.put(buf, %s)";
                break;
            case "long":
                mthd = "buf.put(Long.toString(%s).getBytes())";
                break;
            case "double":
                mthd = "buf.put(Double.toString(%s).getBytes())";
                break;
            case "char":
                mthd = "buf.put(Character.toString(%s).getBytes())";
                break;
            case "boolean":
                mthd = "buf.put(Boolean.toString(%s).getBytes())";
                break;
            case "Date":
                if (type == FieldType.UTCTIMESTAMP || type == FieldType.TZTIMESTAMP) {
                    mthd = "buf.put(DateFormatter.formatAsDateTime(%s).getBytes())";

                } else if (type == FieldType.UTCDATE || type == FieldType.UTCDATEONLY || type == FieldType.LOCALMKTDATE) {
                    mthd = "buf.put(DateFormatter.formatAsDate(%s).getBytes())";

                } else if (type == FieldType.UTCTIMEONLY || type == FieldType.TIME || type == FieldType.TZTIMEONLY) {
                    mthd = "buf.put(DateFormatter.formatAsTime(%s).getBytes())";
                }
                break;
        }

        String arg;
        if (type == FieldType.NUMINGROUP) {
            arg = fieldName + ".size()";
        } else if (enumField) {
            arg = fieldName + ".value";
        } else {
            arg = fieldName;
        }

        String putMthd = String.format(mthd, arg);
        sb.append(String.format(
//                indent + "    buf.put(\"%d\".getBytes());\n" +
                indent + "    buf.put(TAG_%S);\n" +
                        indent + "    buf.put(EQ);\n" +
//                        indent + "    buf.put(%s.getBytes());\n"
                        indent + "    %s;\n"
                , name, putMthd));

        if (type == FieldType.NUMINGROUP) {
            sb.append(format(
                    indent + "    for (%s it: this.%s) {\n" +
                            indent + "        it.encode2(buf);\n" +
                            indent + "    }\n",
                    name, fieldName
            ));
        }

        sb.append(indent).append("    buf.put(SEP);\n");
        sb.append(indent).append("}\n");
    }

}
