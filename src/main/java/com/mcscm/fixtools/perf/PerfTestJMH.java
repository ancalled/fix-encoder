package com.mcscm.fixtools.perf;

import org.openjdk.jmh.annotations.*;
import org.sample.MarketDataIncrementalRefresh;
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
        final String mesToDecode;

        public MyState() {
            mesToDecode = PerfTestJMH.encode(marketDataEncode);
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
    public String testEncode() {
        final MarketDataIncrementalRefresh marketData = state.marketDataEncode;
        return encode(marketData);
    }

    @Benchmark
    public void testEncode2() {
        final MarketDataIncrementalRefresh marketData = state.marketDataEncode;
        final ByteBuffer bb = this.bb;
        bb.clear();

        encode2(marketData, bb);
    }

//    @Benchmark
    public MarketDataIncrementalRefresh testDecode(/*final MyState state*/) {
        final String mesToDecode = state.mesToDecode;
        return decode(mesToDecode, state.marketDataDecode);
    }

////    @Benchmark
//    public MarketDataIncrementalRefresh testCreate() {
//        final MarketDataIncrementalRefresh res = new MarketDataIncrementalRefresh();
//        return res;
//    }
//
////    @Benchmark
//    public MarketDataIncrementalRefresh.NoMDEntries testCreateNoMDEntry() {
//        final MarketDataIncrementalRefresh.NoMDEntries mdIncGroup = new MarketDataIncrementalRefresh.NoMDEntries();
//        return mdIncGroup;
//
//    }

    public static String encode(MarketDataIncrementalRefresh marketData) {
        marketData.mDReqID = "1234";

        MarketDataIncrementalRefresh.NoMDEntries mdIncGroup = new MarketDataIncrementalRefresh.NoMDEntries();
//        mdIncGroup.securityID = "56789";
        mdIncGroup.mDEntryPx = 50;
//        mdIncGroup.mDEntrySize = 50;
//        mdIncGroup.numberOfOrders = 1;
        mdIncGroup.mDUpdateAction = MDUpdateAction.NEW;
        mdIncGroup.mDEntryType = MDEntryType.BID;
        if (marketData.noMDEntries == null || marketData.noMDEntries.size() < 2) {
            marketData.addNoMDEntries(mdIncGroup);
        } else {
            marketData.noMDEntries.set(0, mdIncGroup);
        }

        mdIncGroup = new MarketDataIncrementalRefresh.NoMDEntries();
//        mdIncGroup.securityID = "56789";
        mdIncGroup.mDEntryPx = 50;
//        mdIncGroup.mDEntrySize = 50;
//        mdIncGroup.numberOfOrders = 1;
        mdIncGroup.mDUpdateAction = MDUpdateAction.NEW;
        mdIncGroup.mDEntryType = MDEntryType.OFFER;
        if (marketData.noMDEntries == null || marketData.noMDEntries.size() < 2) {
            marketData.addNoMDEntries(mdIncGroup);
        } else {
            marketData.noMDEntries.set(0, mdIncGroup);
        }

        return marketData.encode();
    }

    public static void encode2(MarketDataIncrementalRefresh marketData, ByteBuffer bb) {
        marketData.mDReqID = "1234";

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

        marketData.encode2(bb);
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

    public static void main(String[] args) {
        PerfTestJMH bench = new PerfTestJMH();
        bench.init();

        for (int i = 0; i < 100000; i++) {
            bench.testEncode2();
        }

        long start = System.nanoTime();
        final int iterations = 1000000;
        for (int i = 0; i < iterations; i++) {
            bench.testEncode2();
        }
        long proc = System.nanoTime() - start;
        long op = proc / iterations;
        System.out.println("Encode nanos " + op);
//
////        System.out.println("entries: " + state.marketDataEncode.noMDEntries.size());
//
//        for (int i = 0; i < 100000; i++) {
//            bench.testDecode();
//        }
//
//        start = System.nanoTime();
//        for (int i = 0; i < iterations; i++) {
//            bench.testEncode();
//        }
//        proc = System.nanoTime() - start;
//        op = proc / iterations;
//        System.out.println("Decode nanos " + op);
    }


}
