package com.mcscm.fixtools.test;

import com.mcscm.fixtools.FIXUtils;
import org.junit.Test;
import org.sample.*;
import org.sample.enums.MDBookType;
import org.sample.enums.MDEntryType;
import org.sample.enums.MDUpdateAction;
import org.sample.enums.Side;

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
        expected.side = Side.BUY;
        expected.transactTime = new Date();

        final String encode = expected.encode();
        FIXUtils.initHeader("FIX.5.1", "Ping", "", "", "", "Pong", "", 1);

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
        exp.mDReqID = "1234";
        exp.mDBookType = MDBookType.TOP_OF_BOOK;
        exp.applQueueDepth = 5;

        MarketDataIncrementalRefresh.NoMDEntries noMd1 = new MarketDataIncrementalRefresh.NoMDEntries();
        noMd1.mDUpdateAction = MDUpdateAction.CHANGE;
        noMd1.mDEntryType = MDEntryType.BID;
//        noMd1.securityID = "4125112";
        noMd1.mDEntryPx = 1401.1;
//        noMd1.mDEntrySize = 100000;
//        noMd1.numberOfOrders = 1;
        exp.addNoMDEntries(noMd1);

        MarketDataIncrementalRefresh.NoMDEntries noMd2 = new MarketDataIncrementalRefresh.NoMDEntries();
        noMd2.mDUpdateAction = MDUpdateAction.CHANGE;
        noMd2.mDEntryType = MDEntryType.OFFER;
        noMd2.mDEntryPx = 4045.3;
//        noMd1.securityID = "ca3Csfc";
//        noMd1.mDEntrySize = 314511;
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

    @Test
    public void testDecode3() {
        String text = "262=1234\u0001268=2\u0001279=0\u0001269=0\u000148=56789\u0001270=50.0\u0001271=50\u0001346=1\u0001279=0\u0001269=1\u000148=56789\u0001270=50.0\u0001271=50\u0001346=1\u0001";
        MarketDataIncrementalRefresh md = new MarketDataIncrementalRefresh();
        md.decode(text);

        System.out.println(md.mDReqID);

    }

}
