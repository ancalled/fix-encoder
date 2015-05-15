package com.mcscm.fixtools.test;

import org.openjdk.jmh.annotations.*;
import org.sample.MarketDataIncrementalRefresh;
import org.sample.enums.MDEntryType;
import org.sample.enums.MDUpdateAction;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(java.util.concurrent.TimeUnit.NANOSECONDS)
@Fork(value = 1)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
@State(Scope.Benchmark)
public class PerfTestJMH {

    @State(Scope.Benchmark)
    public static class MyState {
        final MarketDataIncrementalRefresh marketDataEncode = new MarketDataIncrementalRefresh();
        final MarketDataIncrementalRefresh marketDataDecode = new MarketDataIncrementalRefresh();
        final String mesToDecode;

        public MyState() {
            mesToDecode = PerfTestJMH.encode(marketDataEncode);
        }
    }

    @Benchmark
    public String testEncode(final MyState state) {
        final MarketDataIncrementalRefresh marketData = state.marketDataEncode;
        return encode(marketData);
    }

    @Benchmark
    public MarketDataIncrementalRefresh testDecode(final MyState state) {
        final String mesToDecode = state.mesToDecode;
        System.out.println("mesToDecode = " + mesToDecode);
        return decode(mesToDecode, state.marketDataDecode);
    }


    public static String encode(MarketDataIncrementalRefresh marketData) {
        marketData.mDReqID = "1234";

        MarketDataIncrementalRefresh.NoMDEntries mdIncGroup = new MarketDataIncrementalRefresh.NoMDEntries();
        mdIncGroup.securityID = "56789";
        mdIncGroup.mDEntryPx = 50;
        mdIncGroup.mDEntrySize = 50;
        mdIncGroup.numberOfOrders = 1;
        mdIncGroup.mDUpdateAction = MDUpdateAction.NEW;
        mdIncGroup.mDEntryType = MDEntryType.BID;
//        if (marketData.noMDEntries.size() < 2) {
        marketData.addNoMDEntries(mdIncGroup);
//        } else {
//            marketData.noMDEntries.set(0, mdIncGroup);
//        }

        mdIncGroup = new MarketDataIncrementalRefresh.NoMDEntries();
        mdIncGroup.securityID = "56789";
        mdIncGroup.mDEntryPx = 50;
        mdIncGroup.mDEntrySize = 50;
        mdIncGroup.numberOfOrders = 1;
        mdIncGroup.mDUpdateAction = MDUpdateAction.NEW;
        mdIncGroup.mDEntryType = MDEntryType.OFFER;
//        if (marketData.noMDEntries.size() < 2) {
        marketData.addNoMDEntries(mdIncGroup);
//        } else {
//            marketData.noMDEntries.set(0, mdIncGroup);
//        }

        return marketData.encode();
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
        MyState state = new MyState();

        bench.testEncode(state);
        bench.testDecode(state);
    }
}
