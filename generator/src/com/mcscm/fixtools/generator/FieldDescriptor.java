package com.mcscm.fixtools.generator;


import java.nio.ByteBuffer;
import java.util.Arrays;

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

        String bytes = Arrays.toString(intToBytes(tag))
                .replace("[", "{")
                .replace("]", "}");
        sb.append(String.format("public static final byte[] TAG_%S = %s; //%d\n", name, bytes, tag));
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


    public void appendEncodeBB(StringBuilder sb, String indent) {
        String nullValue = enumField ? "null" : type.nullValue;
        sb.append(format(
                indent + "if (%s != %s) {\n",
                fieldName, nullValue));

        String mthd = "";
        if (enumField) {
            mthd = "buf.put(" + fieldName + ".bytes)";
        } else {
            switch (type.javaType) {
                case "String":
                    mthd = "CodeUtils.put(buf, %s)";
                    break;
                case "int":
                case "List":
                    mthd = "CodeUtils.put(buf, %s)";
                    break;
                case "long":
                    mthd = "CodeUtils.put(buf, %s)";
                    break;
                case "double":
                    mthd = "CodeUtils.put(buf, Double.toString(%s))";
                    break;
                case "char":
                    mthd = "CodeUtils.put(buf, Character.toString(%s))";
                    break;
                case "boolean":
//                    mthd = "buf.put(Boolean.toString(%s).getBytes())";
                    mthd = "CodeUtils.put(buf, %s ? \"Y\" : \"N\")";
                    break;
                case "Date":
                    if (type == FieldType.UTCTIMESTAMP || type == FieldType.TZTIMESTAMP) {
                        mthd = "CodeUtils.put(buf, DateFormatter.formatAsDateTime(%s))";

                    } else if (type == FieldType.UTCDATE || type == FieldType.UTCDATEONLY || type == FieldType.LOCALMKTDATE) {
                        mthd = "CodeUtils.put(buf, DateFormatter.formatAsDate(%s))";

                    } else if (type == FieldType.UTCTIMEONLY || type == FieldType.TIME || type == FieldType.TZTIMEONLY) {
                        mthd = "CodeUtils.put(buf, DateFormatter.formatAsTime(%s))";
                    }
                    break;
            }
        }

        String arg;
        if (type == FieldType.NUMINGROUP) {
            arg = fieldName + ".size()";
        } else {
            arg = fieldName;
        }

        String putMthd = String.format(mthd, arg);
        sb.append(String.format(
                indent + "    buf.put(TAG_%S);\n" +
                        indent + "    buf.put(EQ);\n" +
                        indent + "    %s;\n"
                , name, putMthd));

        if (type == FieldType.NUMINGROUP) {
            sb.append(format(
                            indent + "    buf.put(SEP);\n" +
                            indent + "    for (%s it: this.%s) {\n" +
                            indent + "        it.encode(buf);\n" +
                            indent + "    }\n" +
                            indent + "    if (this.%s.isEmpty()) buf.put(SEP);\n"
                    , name, fieldName, fieldName
            ));
        } else {
            sb.append(indent).append("    buf.put(SEP);\n");
        }
        sb.append(indent).append("}\n");
    }


    public String decodeMethod(String bufParam, String offsetParam, String lengthParam) {
        String decode = String.format(decodeSimple(), bufParam, offsetParam, lengthParam);

        if (enumField) {
            return name + ".getByValue(" + decode + ")";
        } else {
            return decode;
        }
    }

    private String decodeSimple() {
        switch (type.javaType) {
            case "String":
                return "CodeUtils.getString(%s, %s, %s)";
            case "int":
            case "List":
                return "CodeUtils.getInt(%s, %s, %s)";
            case "long":
                return "CodeUtils.getLong(%s, %s, %s)";
            case "double":
                return "CodeUtils.getDouble(%s, %s, %s)";
            case "char":
                return "CodeUtils.getChar(%s, %s, %s)";
            case "boolean":
                return "\"Y\".equals(CodeUtils.getString(%s, %s, %s))";
            case "Date":
                if (type == FieldType.UTCTIMESTAMP || type == FieldType.TZTIMESTAMP) {
                    return "DateFormatter.parseDateTime(CodeUtils.getString(%s, %s, %s))";

                } else if (type == FieldType.UTCDATE || type == FieldType.UTCDATEONLY || type == FieldType.LOCALMKTDATE) {
                    return "DateFormatter.parseDate(CodeUtils.getString(%s, %s, %s))";

                } else if (type == FieldType.UTCTIMEONLY || type == FieldType.TIME || type == FieldType.TZTIMEONLY) {
                    return "DateFormatter.parseTime(CodeUtils.getString(%s, %s, %s))";
                }
                break;
        }
        return "";
    }

    public static byte[] intToBytes(int i) {
        return ByteBuffer.allocate(4).putInt(i).array();
    }


}
