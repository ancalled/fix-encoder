package com.mcscm.fixtools.test;

import com.mcscm.fixtools.perf.PerfTestJMH;
import com.mcscm.fixtools.utils.EncodeUtils;
import com.mcscm.fixtools.utils.RadixTree;
import org.sample.MarketDataIncrementalRefresh;
import org.sample.enums.ApplQueueResolution;
import org.sample.enums.MDBookType;
import org.sample.enums.MDEntryType;
import org.sample.enums.MDUpdateAction;

import java.nio.ByteBuffer;

import static com.mcscm.fixtools.test.DecodeTest.DecodeState.KEY;
import static com.mcscm.fixtools.test.DecodeTest.DecodeState.VALUE;
import static com.mcscm.fixtools.utils.EncodeUtils.getInt;
import static com.mcscm.fixtools.utils.EncodeUtils.getLong;
import static com.mcscm.fixtools.utils.EncodeUtils.getString;
import static org.sample.MarketDataIncrementalRefresh.*;

public class DecodeTest {

    public static final RadixTree<FieldDecoder<MarketDataIncrementalRefresh>> MD_TAGS_TREE = new RadixTree<>();
    static {
        MD_TAGS_TREE.add(TAG_APPLQUEUEDEPTH, (bb, o, l, md) -> {
            md.applQueueDepth = getInt(bb, o, l);
            return o + l + 1;
        });
        MD_TAGS_TREE.add(TAG_APPLQUEUERESOLUTION, (bb, o, l, md) -> {
            md.applQueueResolution = ApplQueueResolution.getByValue(getInt(bb, o, l));
            return o + l + 1;
        });
        MD_TAGS_TREE.add(TAG_MDREQID, (bb, o, l, md) -> {
            md.mDReqID = getString(bb, o, l);
            return o + l + 1;
        });
        MD_TAGS_TREE.add(TAG_NOMDENTRIES, (bb, o, l, md) -> {
            int size = getInt(bb, o, l);
            NoMDEntries item = new NoMDEntries();
            md.addNoMDEntries(item);
            int enfOffset = o + l + 1;
            for (int i = 0; i < size; i++) {
                enfOffset = decode(bb, enfOffset, item);
            }

            return enfOffset;
        });
        MD_TAGS_TREE.add(TAG_MDBOOKTYPE, (bb, o, l, md) -> {
            md.mDBookType = MDBookType.getByValue(getInt(bb, o, l));
            return o + l + 1;
        });
    }


    public static final RadixTree<FieldDecoder<NoMDEntries>> NOMD_TAGS_TREE = new RadixTree<>();
    static {
        NOMD_TAGS_TREE.add(NoMDEntries.TAG_MDUPDATEACTION, (bb, o, l, md) -> {
            md.mDUpdateAction = MDUpdateAction.getByValue(getString(bb, o, l).charAt(0));
            return o + l + 1;
        });
        NOMD_TAGS_TREE.add(NoMDEntries.TAG_MDENTRYTYPE, (bb, o, l, md) -> {
            md.mDEntryType = MDEntryType.getByValue(getString(bb, o, l).charAt(0));
            return o + l + 1;
        });
        NOMD_TAGS_TREE.add(NoMDEntries.TAG_SYMBOL, (bb, o, l, md) -> {
            md.symbol = getString(bb, o, l);
            return o + l + 1;
        });
        NOMD_TAGS_TREE.add(NoMDEntries.TAG_SECURITYID, (bb, o, l, md) -> {
            md.securityID = getString(bb, o, l);
            return o + l + 1;
        });
        NOMD_TAGS_TREE.add(NoMDEntries.TAG_MDENTRYPX, (bb, o, l, md) -> {
            md.mDEntryPx = getLong(bb, o, l);
            return o + l + 1;
        });
        NOMD_TAGS_TREE.add(NoMDEntries.TAG_MDENTRYSIZE, (bb, o, l, md) -> {
            md.mDEntrySize = getLong(bb, o, l);
            return o + l + 1;
        });
        NOMD_TAGS_TREE.add(NoMDEntries.TAG_NUMBEROFORDERS, (bb, o, l, md) -> {
            md.numberOfOrders = getInt(bb, o, l);
            return o + l + 1;
        });
    }

    @FunctionalInterface
    public interface FieldDecoder<K> {

        int decode(ByteBuffer bb, int offset, int length, K k);

    }

    enum DecodeState {KEY, VALUE, SKIP}

    public static int decode(ByteBuffer bb, int offset, MarketDataIncrementalRefresh marketData) {

        DecodeState st = KEY;
        RadixTree.Node<FieldDecoder<MarketDataIncrementalRefresh>> search = MD_TAGS_TREE.root;

        int pieceStart = offset;
        int pos = offset;

        for (; ; ) {
            if (bb.position() >= bb.capacity()) break;
            byte b = bb.get(pos++);

            if (b == SEP) {
                if (search.value != null) {
                    pos = search.value.decode(bb, pieceStart, pos - pieceStart - 1, marketData);
                }

                search = MD_TAGS_TREE.root;
                st = KEY;
                pieceStart = pos;
                continue;

            } else if (b == EQ) {
                st = VALUE;
                pieceStart = pos;
                continue;
            }

            if (st == KEY) {
                search = search.find(b);
                if (search == null) {
                    return pieceStart;
                }
            }
        }

        return pos;
    }


    public static int decode(ByteBuffer bb, int offset, NoMDEntries noMDEntries) {

        DecodeState st = KEY;
        RadixTree.Node<FieldDecoder<NoMDEntries>> search = NOMD_TAGS_TREE.root;

        int pieceStart = offset;
        int pos = offset;

        for (; ; ) {
            if (bb.position() >= bb.capacity()) break;
            byte b = bb.get(pos++);

            if (b == SEP) {
                if (search.value != null) {
                    pos = search.value.decode(bb, pieceStart, pos - pieceStart - 1, noMDEntries);
                }

                search = NOMD_TAGS_TREE.root;
                st = KEY;
                pieceStart = pos;
                continue;

            } else if (b == EQ) {
                st = VALUE;
                pieceStart = pos;
                continue;
            }

            if (st == KEY) {
                search = search.find(b);
                if (search == null) {
                    return pieceStart;
                }
            }
        }

        return pos;
    }

    public static void main(String[] args) {

        ByteBuffer bb = ByteBuffer.allocate(2048);
        PerfTestJMH.encode_ByteBuffer(new MarketDataIncrementalRefresh(), bb);
        System.out.println(EncodeUtils.toString(bb));
//        bb.position(0);
        bb.flip();

        MarketDataIncrementalRefresh decode = new MarketDataIncrementalRefresh();
        decode(bb, 0, decode);
    }


}
