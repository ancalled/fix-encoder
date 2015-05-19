package com.mcscm.fixtools.test;

import com.mcscm.fixtools.perf.PerfTestJMH;
import com.mcscm.fixtools.utils.CodeUtils;
import com.mcscm.fixtools.utils.FieldDecoder;
import com.mcscm.fixtools.utils.RadixTree;
import org.sample.MarketDataIncrementalRefresh;
import org.sample.enums.ApplQueueResolution;
import org.sample.enums.MDBookType;
import org.sample.enums.MDEntryType;
import org.sample.enums.MDUpdateAction;

import java.nio.ByteBuffer;

import static com.mcscm.fixtools.test.DecodeTest.DecodeState.ERROR_ACCURED;
import static com.mcscm.fixtools.test.DecodeTest.DecodeState.KEY_PARSING;
import static com.mcscm.fixtools.test.DecodeTest.DecodeState.VALUE_PARSING;
import static com.mcscm.fixtools.utils.CodeUtils.*;
import static org.sample.MarketDataIncrementalRefresh.*;

public class DecodeTest {

    public static final RadixTree<FieldDecoder<MarketDataIncrementalRefresh>> TAGS_TREE = new RadixTree<>();

    static {
        TAGS_TREE.add(TAG_MDREQID, (bb, o, l, mes) -> {
            if (mes.parsed.get(0)) return -1;
            mes.mDReqID = getString(bb, o, l);
            mes.parsed.set(0);
            return o + l + 1;
        });
        TAGS_TREE.add(TAG_NOMDENTRIES, (bb, o, l, mes) -> {
            if (mes.parsed.get(1)) return -1;

            int size = getInt(bb, o, l);
            int offset = o + l + 1;

            for (int i = 0; i < size; i++) {
                NoMDEntries item = new NoMDEntries();
                mes.addNoMDEntries(item);
                offset = decode(bb, offset, item);
            }

            mes.parsed.set(1);

            return offset;
        });
        TAGS_TREE.add(TAG_APPLQUEUEDEPTH, (bb, o, l, mes) -> {
            if (mes.parsed.get(2)) return -1;

            mes.applQueueDepth = getInt(bb, o, l);
            mes.parsed.set(2);

            return o + l + 1;
        });
        TAGS_TREE.add(TAG_APPLQUEUERESOLUTION, (bb, o, l, mes) -> {
            if (mes.parsed.get(3)) return -1;

            mes.applQueueResolution = ApplQueueResolution.getByValue(getInt(bb, o, l));
            mes.parsed.set(3);

            return o + l + 1;
        });

        TAGS_TREE.add(TAG_MDBOOKTYPE, (bb, o, l, mes) -> {
            if (mes.parsed.get(4)) return -1;

            mes.mDBookType = MDBookType.getByValue(getInt(bb, o, l));
            mes.parsed.set(4);

            return o + l + 1;
        });
    }


    public static final RadixTree<FieldDecoder<NoMDEntries>> NOMD_TAGS_TREE = new RadixTree<>();

    static {
        NOMD_TAGS_TREE.add(NoMDEntries.TAG_MDUPDATEACTION, (bb, o, l, mes) -> {
            if (mes.parsed.get(0)) return -1;

            mes.mDUpdateAction = MDUpdateAction.getByValue(getString(bb, o, l).charAt(0));
            mes.parsed.set(0);

            return o + l + 1;
        });
        NOMD_TAGS_TREE.add(NoMDEntries.TAG_MDENTRYTYPE, (bb, o, l, mes) -> {
            if (mes.parsed.get(1)) return -1;

            mes.mDEntryType = MDEntryType.getByValue(getString(bb, o, l).charAt(0));
            mes.parsed.set(1);

            return o + l + 1;
        });
        NOMD_TAGS_TREE.add(NoMDEntries.TAG_SYMBOL, (bb, o, l, mes) -> {
            if (mes.parsed.get(2)) return -1;

            mes.symbol = getString(bb, o, l);
            mes.parsed.set(2);

            return o + l + 1;
        });
        NOMD_TAGS_TREE.add(NoMDEntries.TAG_SECURITYID, (bb, o, l, mes) -> {
            if (mes.parsed.get(3)) return -1;

            mes.securityID = getString(bb, o, l);
            mes.parsed.set(3);

            return o + l + 1;
        });
        NOMD_TAGS_TREE.add(NoMDEntries.TAG_MDENTRYPX, (bb, o, l, mes) -> {
            if (mes.parsed.get(4)) return -1;

            mes.mDEntryPx = getLong(bb, o, l);
            mes.parsed.set(4);

            return o + l + 1;
        });
        NOMD_TAGS_TREE.add(NoMDEntries.TAG_MDENTRYSIZE, (bb, o, l, mes) -> {
            if (mes.parsed.get(5)) return -1;

            mes.mDEntrySize = getLong(bb, o, l);
            mes.parsed.set(5);

            return o + l + 1;
        });
        NOMD_TAGS_TREE.add(NoMDEntries.TAG_NUMBEROFORDERS, (bb, o, l, mes) -> {
            if (mes.parsed.get(6)) return -1;

            mes.numberOfOrders = getInt(bb, o, l);
            mes.parsed.set(6);

            return o + l + 1;
        });
    }

    enum DecodeState {KEY_PARSING, VALUE_PARSING, ERROR_ACCURED}

    public static int decode(ByteBuffer bb, int offset, MarketDataIncrementalRefresh message) {

        DecodeState state = KEY_PARSING;
        RadixTree.Node<FieldDecoder<MarketDataIncrementalRefresh>> search = TAGS_TREE.root;

        int startPos = offset;
        int eqPos = startPos;
        int curr = startPos;

        for (; ; ) {
            if (bb.position() >= bb.capacity()) break;
            byte b = bb.get(curr++);

            if (b == SEP) {
                if (search.get() == null) {
                    state = ERROR_ACCURED;
                    return -1;
                }

                int res = search.get().decode(bb, eqPos, curr - eqPos - 1, message);
                if (res < 0) return startPos;

                search = TAGS_TREE.root;
                state = KEY_PARSING;
                startPos = res;
                curr = startPos;
                continue;

            } else if (b == EQ) {
                state = VALUE_PARSING;
                eqPos = curr;
                continue;
            }

            if (state == KEY_PARSING) {
                search = search.find(b);
                if (search == null) {
                    return startPos;
                }
            }
        }

        return curr;
    }


    public static int decode(ByteBuffer bb, int offset, NoMDEntries message) {

        DecodeState state = KEY_PARSING;
        RadixTree.Node<FieldDecoder<NoMDEntries>> search = NOMD_TAGS_TREE.root;

        int startPos = offset;
        int eqPos = startPos;
        int curr = startPos;

        for (; ; ) {
            if (bb.position() >= bb.capacity()) break;
            byte b = bb.get(curr++);

            if (b == SEP) {
                if (search.get() == null) {
                    state = ERROR_ACCURED;
                    return -1;
                }
                int res = search.get().decode(bb, eqPos, curr - eqPos - 1, message);
                if (res < 0) return startPos;

                search = NOMD_TAGS_TREE.root;
                state = KEY_PARSING;
                startPos = res;
                curr = startPos;
                continue;

            } else if (b == EQ) {
                state = VALUE_PARSING;
                eqPos = curr;
                continue;
            }

            if (state == KEY_PARSING) {
                search = search.find(b);
                if (search == null) {
                    return startPos;
                }
            }
        }

        return curr;
    }

    public static void main(String[] args) {

        ByteBuffer bb = ByteBuffer.allocate(2048);
        PerfTestJMH.encode_ByteBuffer(new MarketDataIncrementalRefresh(), bb);
        System.out.println(CodeUtils.toString(bb));
//        bb.position(0);
        bb.flip();

        MarketDataIncrementalRefresh decode = new MarketDataIncrementalRefresh();
        decode(bb, 0, decode);

        System.out.println(decode);
    }


}
