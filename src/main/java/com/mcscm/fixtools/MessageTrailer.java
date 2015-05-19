package com.mcscm.fixtools;

import com.mcscm.fixtools.utils.CodeUtils;
import com.mcscm.fixtools.utils.FieldDecoder;
import com.mcscm.fixtools.utils.RadixTree;

import java.nio.ByteBuffer;
import java.util.*;

public class MessageTrailer {

    public static final byte SEP = 1;
    public static final byte EQ = 61;

    public static final byte[] TAG_CHECKSUM = {49, 48}; //10



    public static final RadixTree<FieldDecoder<MessageTrailer>> TAGS_TREE = new RadixTree<>();

    static {
        TAGS_TREE.add(TAG_CHECKSUM, (bb, o, l, mes) -> {
            mes.checkSum = CodeUtils.getInt(bb, o, l);
            return o + l + 1;
        });
    }

    public int checkSum;

    enum DecodeState {KEY_PARSING, VALUE_PARSING}

    public final BitSet parsed = new BitSet(5);
    public final List<String> parseErrors = new ArrayList<>();


    public void encode(ByteBuffer buf) {
        if (checkSum != 0) {
            buf.put(TAG_CHECKSUM);
            buf.put(EQ);
            if (checkSum < 100) {
                buf.put((byte) '0');
            }
            if (checkSum < 10) {
                buf.put((byte) '0');
            }
            CodeUtils.put(buf, checkSum);
            buf.put(SEP);
        }
    }

    public int decode(ByteBuffer bb, int offset) {

        DecodeState state = DecodeState.KEY_PARSING;
        RadixTree.Node<FieldDecoder<MessageTrailer>> search = TAGS_TREE.root;

        int startPos = offset;
        int eqPos = startPos;
        int curr = startPos;

        for (; ; ) {
            if (curr >= bb.limit()) break;
            byte b = bb.get(curr++);

            if (b == SEP) {
                if (search.get() == null) {
                    parseErrors.add("Unknown tag: " + CodeUtils.toString(bb, startPos, curr - startPos));
                    return -1;
                }

                int res = search.get().decode(bb, eqPos, curr - eqPos - 1, this);
                if (res < 0) return startPos;

                search = TAGS_TREE.root;
                state = DecodeState.KEY_PARSING;
                startPos = res;
                curr = startPos;
                continue;

            } else if (b == EQ) {
                state = DecodeState.VALUE_PARSING;
                eqPos = curr;
                continue;
            }

            if (state == DecodeState.KEY_PARSING) {
                search = search.find(b);
                if (search == null) {
                    return startPos;
                }
            }
        }

        return curr;
    }


}
