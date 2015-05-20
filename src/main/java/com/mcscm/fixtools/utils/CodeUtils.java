package com.mcscm.fixtools.utils;

import com.mcscm.fixtools.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class CodeUtils {

    public static final int RADIX = 10;

    public static void put(ByteBuffer bb, String str) {
        int pos = bb.position();
        pos = put(bb, str, pos);
        bb.position(pos);
    }

    public static int put(ByteBuffer bb, String str, int offset) {
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            bb.put(offset + i, (byte) ch);
        }
        return offset + str.length();
    }


    public static void put(ByteBuffer bb, int val) {
        int pos = bb.position();
        pos = put(bb, val, pos);
        bb.position(pos);
    }

    public static int put(ByteBuffer bb, int i, int offset) {

        int index = (i < 0) ?
                stringSize(-i) + 1
                : stringSize(i);

        int q, r;
        int pos = index;
        byte sign = 0;

        if (i < 0) {
            sign = 45;
            i = -i;
        }

        // Generate two digits per iteration
        while (i >= 65536) {
            q = i / 100;
            // really: r = i - (q * 100);
            r = i - ((q << 6) + (q << 5) + (q << 2));
            i = q;
            bb.put(offset + (--pos), DigitOnes[r]);
            bb.put(offset + (--pos), DigitTens[r]);
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i <= 65536, i);
        for (; ; ) {
            q = (i * 52429) >>> (16 + 3);
            r = i - ((q << 3) + (q << 1));  // r = i-(q*10) ...
            bb.put(offset + (--pos), digits[r]);
//            buf[--pos] = digits[r];
            i = q;
            if (i == 0) break;
        }
        if (sign != 0) {
            bb.put(offset + (--pos), sign);
        }
        return offset + index;
    }


    public static void put(ByteBuffer bb, long val) {
        int pos = bb.position();
        pos = put(bb, val, pos);
        bb.position(pos);
    }


    public static int put(ByteBuffer bb, long i, int offset) {
        int index = (i < 0) ?
                stringSize(-i) + 1
                : stringSize(i);

        long q;
        int r;
        int pos = index;
        byte sign = 0;

        if (i < 0) {
            sign = 45;
            i = -i;
        }

        // Get 2 digits/iteration using longs until quotient fits into an int
        while (i > Integer.MAX_VALUE) {
            q = i / 100;
            // really: r = i - (q * 100);
            r = (int) (i - ((q << 6) + (q << 5) + (q << 2)));
            i = q;
            bb.put(offset + (--pos), DigitOnes[r]);
            bb.put(offset + (--pos), DigitTens[r]);
        }

        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int) i;
        while (i2 >= 65536) {
            q2 = i2 / 100;
            // really: r = i2 - (q * 100);
            r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
            i2 = q2;
            bb.put(offset + (--pos), DigitOnes[r]);
            bb.put(offset + (--pos), DigitTens[r]);
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i2 <= 65536, i2);
        for (; ; ) {
            q2 = (i2 * 52429) >>> (16 + 3);
            r = i2 - ((q2 << 3) + (q2 << 1));  // r = i2-(q2*10) ...
            bb.put(offset + (--pos), digits[r]);
            i2 = q2;
            if (i2 == 0) break;
        }
        if (sign != 0) {
            bb.put(offset + (--pos), sign);
        }

        return offset + index;
    }


    public static byte[] intToBytes(int i) {
        int size = (i < 0) ? stringSize(-i) + 1
                : stringSize(i);
        byte[] bytes = new byte[size];
        getBytes(i, size, bytes);
        return bytes;
    }

    public static String getString(ByteBuffer bb, int offset, int length) {
        bb.position(offset);
        byte[] bytes = new byte[length];
        bb.get(bytes);
        return new String(bytes);
//        return ""; //todo implement
    }

    public static int getInt(ByteBuffer bb, int offset, int length) {
        return parseInt(bb, offset, length);
//        bb.position(offset);
//        byte[] bytes = new byte[length];
//        bb.get(bytes);
//        return Integer.parseInt(new String(bytes));
    }

    public static long getLong(ByteBuffer bb, int offset, int length) {
        return parseLong(bb, offset, length);
//        bb.position(offset);
//        byte[] bytes = new byte[length];
//        bb.get(bytes);
//        return Long.parseLong(new String(bytes));
    }

    public static double getDouble(ByteBuffer bb, int offset, int length) {
        bb.position(offset);
        byte[] bytes = new byte[length];
        bb.get(bytes);
        return Double.parseDouble(new String(bytes));
//        return 0; //todo implement
    }


    public static char getChar(ByteBuffer bb, int offset, int length) {
        bb.position(offset);
        return (char) bb.get();
//        return 0;  //todo implement
    }


    public static void getBytes(int i, int index, byte[] buf) {
        int q, r;
        int pos = index;
        byte sign = 0;

        if (i < 0) {
            sign = 45;
            i = -i;
        }

        // Generate two digits per iteration
        while (i >= 65536) {
            q = i / 100;
            // really: r = i - (q * 100);
            r = i - ((q << 6) + (q << 5) + (q << 2));
            i = q;
            buf[--pos] = DigitOnes[r];
            buf[--pos] = DigitTens[r];
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i <= 65536, i);
        for (; ; ) {
            q = (i * 52429) >>> (16 + 3);
            r = i - ((q << 3) + (q << 1));  // r = i-(q*10) ...
            buf[--pos] = digits[r];
            i = q;
            if (i == 0) break;
        }
        if (sign != 0) {
            buf[--pos] = sign;
        }
    }


    final static byte[] digits = {
            48, 49, 50, 51, 52, 53, 54, 55, 56,
            57, 97, 98, 99, 100, 101, 102, 103,
            104, 105, 106, 107, 108, 109, 110,
            111, 112, 113, 114, 115, 116, 117,
            118, 119, 120, 121, 122
    };


    final static byte[] DigitOnes = {
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57
    };


    final static byte[] DigitTens = {
            48, 48, 48, 48, 48, 48, 48, 48, 48, 48,
            49, 49, 49, 49, 49, 49, 49, 49, 49, 49,
            50, 50, 50, 50, 50, 50, 50, 50, 50, 50,
            51, 51, 51, 51, 51, 51, 51, 51, 51, 51,
            52, 52, 52, 52, 52, 52, 52, 52, 52, 52,
            53, 53, 53, 53, 53, 53, 53, 53, 53, 53,
            54, 54, 54, 54, 54, 54, 54, 54, 54, 54,
            55, 55, 55, 55, 55, 55, 55, 55, 55, 55,
            56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
            57, 57, 57, 57, 57, 57, 57, 57, 57, 57
    };


    final static int[] sizeTable = {9, 99, 999, 9999, 99999, 999999, 9999999,
            99999999, 999999999, Integer.MAX_VALUE};

    // Requires positive x
    public static int stringSize(int x) {
        for (int i = 0; ; i++)
            if (x <= sizeTable[i])
                return i + 1;
    }

    static int stringSize(long x) {
        long p = 10;
        for (int i = 1; i < 19; i++) {
            if (x < p)
                return i;
            p = 10 * p;
        }
        return 19;
    }

    public static String toString(ByteBuffer bb) {
        bb.position(0);
        return StandardCharsets.US_ASCII.decode(bb).toString().trim();
    }

    public static String toString(ByteBuffer bb, int offset, int length) {
        byte[] bytes = new byte[length];
        bb.position(offset);
        bb.get(bytes);
        return new String(bytes);
    }

    public static void empty(ByteBuffer bb) {
        for (int i = 0; i < bb.capacity(); i++) {
            bb.put(i, (byte) 0);
        }
    }


    public static final int LIMIT_10 = Math.abs(Integer.MIN_VALUE) / 10;

    public static int parseInt(ByteBuffer bb, int offset, int len)
            throws NumberFormatException {

        int result = 0;
        boolean negative = false;
        int i = 0;
        int limit = -Integer.MAX_VALUE;
        int multmin;
//        int multmin = -LIMIT_10;
        int digit;

        char firstChar = (char) bb.get(offset + i);
        if (firstChar < '0') { // Possible leading "+" or "-"
            if (firstChar == '-') {
                negative = true;
                limit = Integer.MIN_VALUE;
//                    multmin = LIMIT_10;
            } else if (firstChar != '+')
                throw new NumberFormatException();

            if (len == 1) // Cannot have lone "+" or "-"
                throw new NumberFormatException();
            i++;
        }

        multmin = limit / RADIX;
        while (i < len) {
            // Accumulating negatively avoids surprises near MAX_VALUE
            digit = Character.digit((char) bb.get(offset + i++), RADIX);
            if (digit < 0) {
                throw new NumberFormatException();
            }
            if (result < multmin) {
                throw new NumberFormatException();
            }
            result *= RADIX;
            if (result < limit + digit) {
                throw new NumberFormatException();
            }
            result -= digit;
        }

        return negative ? result : -result;
    }


    public static long parseLong(ByteBuffer bb, int offset, int len)
            throws NumberFormatException {


        long result = 0;
        boolean negative = false;
        int i = 0;
        long limit = -Long.MAX_VALUE;
        long multmin;
        int digit;

        if (len > 0) {
            char firstChar = (char) bb.get(offset + i);
            if (firstChar < '0') { // Possible leading "+" or "-"
                if (firstChar == '-') {
                    negative = true;
                    limit = Long.MIN_VALUE;
                } else if (firstChar != '+')
                    throw new NumberFormatException();

                if (len == 1) // Cannot have lone "+" or "-"
                    throw new NumberFormatException();
                i++;
            }
            multmin = limit / RADIX;
            while (i < len) {
                // Accumulating negatively avoids surprises near MAX_VALUE
                digit = Character.digit((char) bb.get(offset + i++), RADIX);
                if (digit < 0) {
                    throw new NumberFormatException();
                }
                if (result < multmin) {
                    throw new NumberFormatException();
                }
                result *= RADIX;
                if (result < limit + digit) {
                    throw new NumberFormatException();
                }
                result -= digit;
            }
        } else {
            throw new NumberFormatException();
        }
        return negative ? result : -result;
    }


    public static void copyTo(ByteBuffer src, ByteBuffer dest, int offset, int length) {
        //todo add overflow checks
        for (int i = offset; i < length; i++) {
            dest.put(src.get(i));
        }
    }

    //  ----------------------------------------------------

    public static FIXMessage decodeMessage(ByteBuffer bb, int offset,
                                           MessageHeader header,
                                           MessageTrailer trailer,
                                           MessageFactory factory) {
        if (header == null || factory == null) return null;

        int start = offset;
        offset = header.decode(bb, offset);
        offset = header.subHeader.decode(bb, offset);

        FIXMessage message = factory.create(header.subHeader.msgType);
        offset = message.decode(bb, offset);

        trailer.decode(bb, offset);

        int sum = CodeUtils.calcCheckSum(bb, start, header.bodyLength + header.subHeader.pos - start);

        if (trailer.checkSum != sum) {
            throw new CoderException("Wrong checksum!");
        }

        return message;
    }

    public static final ByteBuffer TEMP_BUF = ByteBuffer.allocate(1024);

    public static void encodeMessage(ByteBuffer bb, int offset,
                                     FIXMessage mes,
                                     MessageHeader header,
                                     MessageTrailer trailer) {
        bb.position(offset);

        header.subHeader.msgType = mes.getType();

        TEMP_BUF.position(0);
        header.subHeader.encode(TEMP_BUF);
        mes.encode(TEMP_BUF);
        header.bodyLength = TEMP_BUF.position();
        header.encode(bb);
//        bb.put(TEMP_BUF);
        copyTo(TEMP_BUF, bb, 0, header.bodyLength);
        trailer.checkSum = CodeUtils.calcCheckSum(bb, offset, bb.position());
        trailer.encode(bb);
    }


    public static int calcCheckSum(ByteBuffer bb, int offset, int length) {
        int cks = 0;
        for (int i = offset; i < offset + length; i++) {
            cks += bb.get(i);
        }

        return cks & 255;
    }



}
