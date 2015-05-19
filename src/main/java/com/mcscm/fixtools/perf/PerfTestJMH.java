package com.mcscm.fixtools.perf;

import com.mcscm.fixtools.utils.CodeUtils;
import org.openjdk.jmh.annotations.*;
import org.sample.MarketDataIncrementalRefresh;
import org.sample.enums.MDBookType;
import org.sample.enums.MDEntryType;
import org.sample.enums.MDUpdateAction;

import java.nio.ByteBuffer;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(java.util.concurrent.TimeUnit.NANOSECONDS)
@Fork(value = 1)
@Warmup(iterations = 10)
@Measurement(iterations = 20)
@State(Scope.Benchmark)
public class PerfTestJMH {

//    @State(Scope.Benchmark)
    public static class MyState {
        final MarketDataIncrementalRefresh marketDataEncode = new MarketDataIncrementalRefresh();
        final MarketDataIncrementalRefresh marketDataDecode = new MarketDataIncrementalRefresh();
        final String decodeMessage;
        final ByteBuffer decodeBuffer;

        public MyState() {
            decodeMessage = PerfTestJMH.encode_StringBuffer(marketDataEncode);
            decodeBuffer = ByteBuffer.allocate(2048);
            PerfTestJMH.encode_ByteBuffer(marketDataEncode, decodeBuffer);
            decodeBuffer.flip();
        }
    }

    private MyState state;
//    private final ByteBuffer bb = ByteBuffer.allocate(2048);
    private final ByteBuffer bb = ByteBuffer.allocateDirect(2048);

    @Setup(Level.Iteration)
    public void init() {
        state = new MyState();
    }


    @Benchmark
    public void testFill() {
        final MarketDataIncrementalRefresh marketData = state.marketDataEncode;
        fill(marketData);
    }

    @Benchmark
    public String testEncode_StringBuffer() {
        final MarketDataIncrementalRefresh marketData = state.marketDataEncode;
        return encode_StringBuffer(marketData);
    }

    @Benchmark
    public void testEncode_ByteBuffer() {
        final MarketDataIncrementalRefresh marketData = state.marketDataEncode;
        final ByteBuffer bb = this.bb;
        bb.clear();

        encode_ByteBuffer(marketData, bb);
    }

    @Benchmark
    public MarketDataIncrementalRefresh testDecode_StringBuilder() {
        final String mesToDecode = state.decodeMessage;
        return decode(mesToDecode, state.marketDataDecode);
    }

    @Benchmark
    public MarketDataIncrementalRefresh testDecode_ByteBuffer() {
        final ByteBuffer decodeBuffer = state.decodeBuffer;
        return decode(decodeBuffer, state.marketDataDecode);
    }


    public static String encode_StringBuffer(MarketDataIncrementalRefresh marketData) {
        fill(marketData);

        return marketData.encode();
    }

    public static void encode_ByteBuffer(MarketDataIncrementalRefresh marketData, ByteBuffer bb) {
        fill(marketData);
        marketData.encode(bb);
    }

    private static void fill(MarketDataIncrementalRefresh marketData) {
        marketData.mDReqID = "1234";
        marketData.mDBookType = MDBookType.TOP_OF_BOOK;
        marketData.applQueueDepth = 5;

        MarketDataIncrementalRefresh.NoMDEntries mdIncGroup = new MarketDataIncrementalRefresh.NoMDEntries();
        mdIncGroup.securityID = "56789";
//        mdIncGroup.mDEntryPx = 50;
        mdIncGroup.mDEntrySize = 50;
        mdIncGroup.numberOfOrders = 1;
        mdIncGroup.mDUpdateAction = MDUpdateAction.NEW;
        mdIncGroup.mDEntryType = MDEntryType.BID;
        if (marketData.noMDEntries == null || marketData.noMDEntries.size() < 2) {
            marketData.addNoMDEntries(mdIncGroup);
        } else {
            marketData.noMDEntries.set(0, mdIncGroup);
        }

        mdIncGroup = new MarketDataIncrementalRefresh.NoMDEntries();
        mdIncGroup.securityID = "56789";
//        mdIncGroup.mDEntryPx = 50;
        mdIncGroup.mDEntrySize = 50;
        mdIncGroup.numberOfOrders = 1;
        mdIncGroup.mDUpdateAction = MDUpdateAction.NEW;
        mdIncGroup.mDEntryType = MDEntryType.OFFER;
        if (marketData.noMDEntries == null || marketData.noMDEntries.size() < 2) {
            marketData.addNoMDEntries(mdIncGroup);
        } else {
            marketData.noMDEntries.set(0, mdIncGroup);
        }

    }


    public MarketDataIncrementalRefresh decode(String text, MarketDataIncrementalRefresh mdIncRef) {

        mdIncRef.decode(text);

        MarketDataIncrementalRefresh.NoMDEntries grp1 = mdIncRef.noMDEntries.get(0);
        MDEntryType type = grp1.mDEntryType;
        MDUpdateAction updAct = grp1.mDUpdateAction;
        MarketDataIncrementalRefresh.NoMDEntries grp2 = mdIncRef.noMDEntries.get(1);
        MDEntryType type2 = grp2.mDEntryType;
        MDUpdateAction updAct2 = grp2.mDUpdateAction;

        return mdIncRef;
    }

    public MarketDataIncrementalRefresh decode(ByteBuffer bb, MarketDataIncrementalRefresh mdIncRef) {

        mdIncRef.decode(bb, 0);

        MarketDataIncrementalRefresh.NoMDEntries grp1 = mdIncRef.noMDEntries.get(0);
        MDEntryType type = grp1.mDEntryType;
        MDUpdateAction updAct = grp1.mDUpdateAction;
        MarketDataIncrementalRefresh.NoMDEntries grp2 = mdIncRef.noMDEntries.get(1);
        MDEntryType type2 = grp2.mDEntryType;
        MDUpdateAction updAct2 = grp2.mDUpdateAction;

        return mdIncRef;
    }

    public static void main(String[] args) {
        PerfTestJMH bench = new PerfTestJMH();
        bench.init();

        System.out.println(CodeUtils.toString(bench.state.decodeBuffer));
        bench.state.decodeBuffer.flip();

        MarketDataIncrementalRefresh mdInc = new MarketDataIncrementalRefresh();
        bench.decode(bench.state.decodeBuffer, mdInc);
        System.out.println();

        System.out.println("mDReqID: " + mdInc.mDReqID);
        System.out.println("mDBookType: " + mdInc.mDBookType);
        System.out.println("noMd[0].mDUpdateAction: " + mdInc.noMDEntries.get(0).mDUpdateAction);
        System.out.println("noMd[0].mDEntryType: " + mdInc.noMDEntries.get(0).mDEntryType);
        System.out.println("noMd[0].securityID: " + mdInc.noMDEntries.get(0).securityID);
        System.out.println("noMd[0].symbol: " + mdInc.noMDEntries.get(0).symbol);
        System.out.println("noMd[0].mDEntryPx: " + mdInc.noMDEntries.get(0).mDEntryPx);
        System.out.println("noMd[0].mDEntrySize: " + mdInc.noMDEntries.get(0).mDEntrySize);
        System.out.println("noMd[0].numberOfOrders: " + mdInc.noMDEntries.get(0).numberOfOrders);

        System.out.println("noMd[1].mDUpdateAction: " + mdInc.noMDEntries.get(1).mDUpdateAction);
        System.out.println("noMd[1].mDEntryType: " + mdInc.noMDEntries.get(1).mDEntryType);
        System.out.println("noMd[1].securityID: " + mdInc.noMDEntries.get(1).securityID);
        System.out.println("noMd[1].symbol: " + mdInc.noMDEntries.get(1).symbol);
        System.out.println("noMd[1].mDEntryPx: " + mdInc.noMDEntries.get(1).mDEntryPx);
        System.out.println("noMd[1].mDEntrySize: " + mdInc.noMDEntries.get(1).mDEntrySize);
        System.out.println("noMd[1].numberOfOrders: " + mdInc.noMDEntries.get(1).numberOfOrders);

        long tst = 0;

        for (int i = 0; i < 100000; i++) {
            mdInc = bench.testDecode_ByteBuffer();
            tst += mdInc.noMDEntries.get(0).mDEntrySize;
            tst += mdInc.noMDEntries.get(1).mDEntrySize;
        }

        tst = 0;
        long start = System.nanoTime();
        final int iterations = 1000000;
        for (int i = 0; i < iterations; i++) {
            mdInc = bench.testDecode_ByteBuffer();
            tst += mdInc.noMDEntries.get(0).mDEntrySize;
            tst += mdInc.noMDEntries.get(1).mDEntrySize;
        }
        long proc = System.nanoTime() - start;
        long op = proc / iterations;
        System.out.println("Process nanos " + op);
        System.out.println("tst = " + tst);

    }


}
