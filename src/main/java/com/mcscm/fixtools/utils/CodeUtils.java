package com.mcscm.fixtools.utils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

public class CodeUtils {

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
        bb.position(offset);
        byte[] bytes = new byte[length];
        bb.get(bytes);
        return Integer.parseInt(new String(bytes));
//        return 0; //todo implement
    }

    public static long getLong(ByteBuffer bb, int offset, int length) {
        bb.position(offset);
        byte[] bytes = new byte[length];
        bb.get(bytes);
        return Long.parseLong(new String(bytes));
//        return 0; //todo implement
    }


    public static char getChar(ByteBuffer bb, int offset, int length) {
        bb.position(offset);
        return bb.getChar();
//        return 0; //todo implement
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

    public static void empty(ByteBuffer bb) {
        for (int i = 0; i < bb.capacity(); i++) {
            bb.put(i, (byte) 0);
        }
    }

    public static void main(String[] args) {
        final int times = 100;
        Random r = new Random();
        ByteBuffer bb = ByteBuffer.allocate(100);
//        System.out.println("pos: " + bb.position());
//        put(bb, 124);
//        System.out.println("pos: " + bb.position());
//        put(bb, 10005000L);
//        System.out.println("pos: " + bb.position());
//        put(bb, 1);
//        System.out.println("pos: " + bb.position());
//        put(bb, " test number");
//        System.out.println("pos: " + bb.position());
//
//        System.out.println(toString(bb));

        for (int i = 0; i < times; i++) {
            long val = r.nextLong();
//            int val = r.nextInt();
            empty(bb);
            put(bb, val, 0);
            String text = toString(bb);
            System.out.println(val + "\t" + text + "\t" + Long.toString(val).equals(text));
//            System.out.println(Arrays.toString(Long.toString(val).toCharArray()));
//            System.out.println(Arrays.toString(text.toCharArray()));
//            System.out.println();
        }
    }

    public static void test(byte[] bytes) {

    }

}
