package com.mcscm.fixtools.generator;

public enum Type {
    STRING("String", "java.lang.String", "null"),
    CHAR("char", null, "0"),
    PRICE("double", null, "0.0"),
    QTY("long", null, "0"),
    INT("int", null, "0"),
    UTCTIMESTAMP("Date", "java.util.Date", "null");

    public String javaType;
    public String javaFullType;
    public String nullValue;

    Type(String javaType, String javaFullType, String nullValue) {
        this.javaType = javaType;
        this.javaFullType = javaFullType;
        this.nullValue = nullValue;
    }

    public boolean isJavaPrimitive() {
        return javaFullType == null;
    }


    public static String genereateConvertMethod(Type tp, String param) {
        switch (tp) {
            case STRING:
                return param;
            case CHAR:
                return param + ".charAt(0)";
            case PRICE:
                return "Double.parseDouble(" + param + ")";
            case QTY:
                return "Long.parseLong(" + param + ")";
            case INT:
                return "Integer.parseInt(" + param + ")";
            case UTCTIMESTAMP:
                return "DateFormatter.parse(" + param + ")";


        }

        return null;
    }

}
