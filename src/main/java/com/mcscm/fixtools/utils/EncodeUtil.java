package com.mcscm.fixtools.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

public class EncodeUtil {


    public static void putAsString(ByteBuffer bb, int i) {
        putAsString(bb, i, 0);
    }

    public static void putAsString(ByteBuffer bb, int i, int offset) {

        int index = (i < 0) ? stringSize(-i) + 1
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
            bb.put(offset + index - (--pos), DigitOnes[r]);
            bb.put(offset + index - (--pos), DigitTens[r]);
//            buf[--pos] = DigitOnes[r];
//            buf[--pos] = DigitTens[r];
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i <= 65536, i);
        for (; ; ) {
            q = (i * 52429) >>> (16 + 3);
            r = i - ((q << 3) + (q << 1));  // r = i-(q*10) ...
            bb.put(offset + index - (--pos), digits[r]);
//            buf[--pos] = digits[r];
            i = q;
            if (i == 0) break;
        }
        if (sign != 0) {
            bb.put(offset + index - (--pos), sign);
//            buf[--pos] = sign;
        }
    }


    static void getBytes(int i, int index, byte[] buf) {
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
    static int stringSize(int x) {
        for (int i = 0; ; i++)
            if (x <= sizeTable[i])
                return i + 1;
    }


    public static byte charToByte(char ch) {
        return Character.toString(ch).getBytes()[0];
    }

    public static void main(String[] args) {
        int i = new Random().nextInt();
        System.out.println("i = " + i);

//        int size = (i < 0) ? stringSize(-i) + 1
//                : stringSize(i);
//        byte[] bytes = new byte[size];
//        getBytes(i, size, bytes);
//
//        System.out.println("bytes = " + Arrays.toString(bytes));
//        System.out.println(new String(bytes));
        ByteBuffer bb = ByteBuffer.allocate(1024);
        putAsString(bb, i);
        bb.flip();

        String text = StandardCharsets.US_ASCII.decode(bb).toString();
        System.out.println(text);
    }

}
