package com.mcscm.fixtools;

import com.mcscm.fixtools.utils.CodeUtils;
import com.mcscm.fixtools.utils.FieldDecoder;
import com.mcscm.fixtools.utils.RadixTree;

import java.nio.ByteBuffer;
import java.util.*;

public class MessageWrapper {

    public static final byte SEP = 1;
    public static final byte EQ = 61;

    public static final byte[] TAG_BEGINSTRING = {56}; //8
    public static final byte[] TAG_BODYLENGTH = {57}; //9
    public static final byte[] TAG_MSGTYPE = {51, 53}; //35
    public static final byte[] TAG_CHECKSUM = {49, 48}; //10
    public static final byte[] TAG_MSGSEQNUM = {51, 52}; //34
    public static final byte[] TAG_SENDERCOMPID = {52, 57}; //49
    public static final byte[] TAG_SENDINGTIME = {53, 50}; //52
    public static final byte[] TAG_TARGETCOMPID = {53, 54}; //56


    public static void main(String[] args) {
        System.out.println(Arrays.toString("56".getBytes()));
    }

    public static final RadixTree<FieldDecoder<MessageWrapper>> TAGS_TREE = new RadixTree<>();

    static {
        TAGS_TREE.add(TAG_BEGINSTRING, (bb, o, l, mes) -> {
            mes.beginString = CodeUtils.getString(bb, o, l);
            return o + l + 1;
        });
        TAGS_TREE.add(TAG_BODYLENGTH, (bb, o, l, mes) -> {
            mes.bodyLength = CodeUtils.getInt(bb, o, l);
            return o + l + 1;
        });
        TAGS_TREE.add(TAG_MSGTYPE, (bb, o, l, mes) -> {
            mes.msgType = CodeUtils.getString(bb, o, l);
            return o + l + 1;
        });
        TAGS_TREE.add(TAG_MSGSEQNUM, (bb, o, l, mes) -> {
            mes.mesSeqNum = CodeUtils.getInt(bb, o, l);
            return o + l + 1;
        });
        TAGS_TREE.add(TAG_SENDERCOMPID, (bb, o, l, mes) -> {
            mes.senderCompID = CodeUtils.getString(bb, o, l);
            return o + l + 1;
        });
        TAGS_TREE.add(TAG_SENDINGTIME, (bb, o, l, mes) -> {
            mes.sendingTime = DateFormatter.parseDateTimeMilis(CodeUtils.getString(bb, o, l));
            return o + l + 1;
        });
        TAGS_TREE.add(TAG_TARGETCOMPID, (bb, o, l, mes) -> {
            mes.targetCompID = CodeUtils.getString(bb, o, l);
            return o + l + 1;
        });

        TAGS_TREE.add(TAG_CHECKSUM, (bb, o, l, mes) -> {
            mes.checkSum = CodeUtils.getInt(bb, o, l);
            return o + l + 1;
        });
    }

    public String beginString;
    public int bodyLength;
    public String msgType;
    public int checkSum;
    int mesSeqNum;
    String senderCompID;
    Date sendingTime;
    String targetCompID;

    enum DecodeState {KEY_PARSING, VALUE_PARSING}

    public final BitSet parsed = new BitSet(5);
    public final List<String> parseErrors = new ArrayList<>();


    public int decode(ByteBuffer bb, int offset) {

        DecodeState state = DecodeState.KEY_PARSING;
        RadixTree.Node<FieldDecoder<MessageWrapper>> search = TAGS_TREE.root;

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
