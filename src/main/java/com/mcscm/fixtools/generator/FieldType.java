package com.mcscm.fixtools.generator;

public enum FieldType {

    STRING("String", "java.lang.String", "null"),
    CHAR("char", null, "0"),
    PRICE("double", null, "0.0"),
    INT("int", null, "0"),
    AMT("double", null, "0.0"),
    QTY("long", null, "0"),
    CURRENCY("String", "java.lang.String", "null"),
    MULTIPLEVALUESTRING("String", "java.lang.String", "null"),
    EXCHANGE("String", "java.lang.String", "null"),
    UTCTIMESTAMP("Date", "java.util.Date", "null"),
    BOOLEAN("boolean", null, "false"),
    LOCALMKTDATE("Date", "java.util.Date", "null"),
    DATA("String", "java.lang.String", "null"),
    FLOAT("double", null, "0.0"),
    PRICEOFFSET("double", null, "0.0"),
    MONTHYEAR("double", null, "0.0"),
    DAYOFMONTH("int", null, "0"),
    UTCDATEONLY("Date", "java.util.Date", "null"),
    UTCDATE("Date", "java.util.Date", "null"),
    UTCTIMEONLY("Date", "java.util.Date", "null"),
    TIME("Date", "java.util.Date", "null"),
    //    NUMINGROUP("int", null, "0"),
    NUMINGROUP("List", "java.util.List", "null"),
    PERCENTAGE("double", null, "0.0"),
    SEQNUM("int", null, "0"),
    LENGTH("int", null, "0"),
    COUNTRY("String", "java.lang.String", "null"),
    MULTIPLECHARVALUE("String", "java.lang.String", "null"),
    MULTIPLESTRINGVALUE("String", "java.lang.String", "null"),
    TZTIMEONLY("Date", "java.util.Date", "null"),
    TZTIMESTAMP("Date", "java.util.Date", "null"),;

    public String javaType;
    public String javaFullType;
    public String nullValue;

    FieldType(String javaType, String javaFullType, String nullValue) {
        this.javaType = javaType;
        this.javaFullType = javaFullType;
        this.nullValue = nullValue;
    }

    public boolean isJavaPrimitive() {
        return javaFullType == null;
    }


    public static String generateConvertMethod(FieldType tp, String param) {
        switch (tp) {
            case STRING:
            case CURRENCY:
            case MULTIPLEVALUESTRING:
            case MULTIPLESTRINGVALUE:
            case EXCHANGE:
            case DATA:
            case COUNTRY:
                return param;
            case CHAR:
                return param + ".charAt(0)";
            case PRICE:
            case AMT:
            case FLOAT:
            case PRICEOFFSET:
            case PERCENTAGE:
                return "Double.parseDouble(" + param + ")";
            case INT:
            case QTY:
            case MONTHYEAR:
            case DAYOFMONTH:
            case NUMINGROUP:
            case SEQNUM:
            case LENGTH:
                return "Integer.parseInt(" + param + ")";
            case UTCTIMESTAMP:
            case TZTIMESTAMP:
                return "DateFormatter.parseDateTime(" + param + ")";
            case UTCDATE:
            case UTCDATEONLY:
            case LOCALMKTDATE:
                return "DateFormatter.parseDate(" + param + ")";
            case UTCTIMEONLY:
            case TIME:
            case TZTIMEONLY:
                return "DateFormatter.parseTime(" + param + ")";
            case BOOLEAN:
                return "Boolean.valueOf(" + param + ")";

            default:
                return param;
        }
    }

}
