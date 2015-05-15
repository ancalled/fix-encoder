package com.mcscm.fixtools.test;

import org.junit.Test;
import org.sample.ExecutionReport;
import org.sample.MarketDataIncrementalRefresh;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class TestEncode {

    public static final double DELTA = 0.000001;

    @Test
    public void encodeDecode() {
        ExecutionReport expected = new ExecutionReport();
        expected.orderID = "131012";
        expected.symbol = "I0001";
        expected.account = "A0031";
        expected.price = 134.1;
        expected.cumQty = 50000;
        expected.side = '1';
        expected.transactTime = new Date();

        final String encode = expected.encode();
        System.out.println(encode);

        ExecutionReport restored = new ExecutionReport();
        restored.decode(encode);

        assertEquals(expected.orderID, restored.orderID);
        assertEquals(expected.symbol, restored.symbol);
        assertEquals(expected.account, restored.account);
        assertEquals(expected.price, restored.price, DELTA);
        assertEquals(expected.cumQty, restored.cumQty);
        assertEquals(expected.side, restored.side);
//        assertEquals(expected.transactTime, restored.transactTime);
    }

    @Test
    public void encodeDecode2() {
        MarketDataIncrementalRefresh exp = new MarketDataIncrementalRefresh();
        exp.mDBookType = '1';
        exp.applQueueDepth = 5;

        MarketDataIncrementalRefresh.NoMDEntries noMd1 = new MarketDataIncrementalRefresh.NoMDEntries();
        noMd1.mDUpdateAction='1';
        noMd1.mDEntryType = '0';
        noMd1.mDEntryPx = 1401.1;
        exp.addNoMDEntries(noMd1);

        MarketDataIncrementalRefresh.NoMDEntries noMd2 = new MarketDataIncrementalRefresh.NoMDEntries();
        noMd2.mDUpdateAction='1';
        noMd2.mDEntryType = '1';
        noMd2.mDEntryPx = 4045.3;
        exp.addNoMDEntries(noMd2);

        String encode = exp.encode();
        System.out.println(encode);

        MarketDataIncrementalRefresh restored = new MarketDataIncrementalRefresh();
        restored.decode(encode);

        assertEquals(exp.noMDEntries.size(), restored.noMDEntries.size());
        for (int i = 0; i < exp.noMDEntries.size(); i++) {
            MarketDataIncrementalRefresh.NoMDEntries noMdExp = exp.noMDEntries.get(i);
            MarketDataIncrementalRefresh.NoMDEntries noMdRest = restored.noMDEntries.get(i);

            assertEquals(noMdExp.mDUpdateAction, noMdRest.mDUpdateAction);
            assertEquals(noMdExp.mDEntryType, noMdRest.mDEntryType);
            assertEquals(noMdExp.mDEntryPx, noMdRest.mDEntryPx, DELTA);
        }
        assertEquals(exp.mDBookType, restored.mDBookType);
        assertEquals(exp.applQueueDepth, restored.applQueueDepth);


    }
}
