package com.mcscm.fixtools.test;

import com.mcscm.fixtools.FIXMessage;
import com.mcscm.fixtools.MessageHeader;
import com.mcscm.fixtools.MessageTrailer;
import com.mcscm.fixtools.utils.CodeUtils;
import junit.framework.TestCase;
import org.junit.Test;
import org.sample.FIXMessageFactory;
import org.sample.MarketDataIncrementalRefresh;
import org.sample.enums.MDBookType;
import org.sample.enums.MDEntryType;
import org.sample.enums.MDUpdateAction;

import java.nio.ByteBuffer;
import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class DecodeTest2 {

    public static final FIXMessageFactory FACTORY = new FIXMessageFactory();
    public static final MessageHeader HEADER = new MessageHeader();
    public static final MessageTrailer TRAILER = new MessageTrailer();
    public static final double DELTA = 0.000001;

    @Test
    public void testDecode() {
        String text = "8=FIXT.1.1\u00019=164\u000135=X\u000134=0\u000149=FixClient\u000152=20150519-08:06:05.155\u000156=FixServer\u00011021=2\u0001262=1000\u0001268=2\u0001279=1\u0001269=0\u000155=KZTK\u0001270=341\u0001271=100004\u0001279=1\u0001269=1\u000155=KZTK\u0001270=342.19\u0001271=200100\u000110=010\u0001";
//        String text = "9=108\u000135=X\u00011021=2\u0001262=1000\u0001268=2\u0001279=1\u0001269=0\u000155=KZTK\u0001270=341\u0001271=100004\u0001279=1\u0001269=1\u000155=KZTK\u0001270=342.19\u0001271=200100\u000110=115\u0001";
        ByteBuffer bb = ByteBuffer.wrap(text.getBytes());

        final int offset = 0;

        MarketDataIncrementalRefresh mdInc =
                (MarketDataIncrementalRefresh) CodeUtils.decodeMessage(bb, offset, HEADER, TRAILER, FACTORY);

        System.out.println("beginString: " + HEADER.beginString);
        System.out.println("bodyLength: " + HEADER.bodyLength);
        System.out.println("msgType: " + HEADER.subHeader.msgType);
        System.out.println();

        System.out.println("mDReqID: " + mdInc.mDReqID);
        System.out.println("mDBookType: " + mdInc.mDBookType);

        for (int i = 0; i < mdInc.noMDEntries.size(); i++) {
            final MarketDataIncrementalRefresh.NoMDEntries grp = mdInc.noMDEntries.get(i);
            System.out.printf("noMd[%d].mDUpdateAction: %s\n", i, grp.mDUpdateAction);
            System.out.printf("noMd[%d].mDEntryType: %s\n", i, grp.mDEntryType);
            System.out.printf("noMd[%d].securityID: %s\n", i, grp.securityID);
            System.out.printf("noMd[%d].symbol: %s\n", i, grp.symbol);
            System.out.printf("noMd[%d].mDEntryPx: %s\n", i, grp.mDEntryPx);
            System.out.printf("noMd[%d].mDEntrySize: %s\n", i, grp.mDEntrySize);
            System.out.printf("noMd[%d].numberOfOrders: %s\n", i, grp.numberOfOrders);
        }

        ByteBuffer encode = ByteBuffer.allocate(1024);
        CodeUtils.encodeMessage(encode, 0, mdInc, HEADER, TRAILER);
        encode.flip();

        System.out.println();
        System.out.println(CodeUtils.toString(encode));
    }

    @Test
    public void testEncode() {
        MarketDataIncrementalRefresh mes = new MarketDataIncrementalRefresh();
        mes.mDReqID = "1234";
        mes.mDBookType = MDBookType.TOP_OF_BOOK;
        mes.applQueueDepth = 5;

        MarketDataIncrementalRefresh.NoMDEntries noMd1 = new MarketDataIncrementalRefresh.NoMDEntries();
        noMd1.mDUpdateAction = MDUpdateAction.CHANGE;
        noMd1.mDEntryType = MDEntryType.BID;
        noMd1.securityID = "4125112";
        noMd1.mDEntryPx = 1401.1;
        noMd1.mDEntrySize = 100000;
        noMd1.numberOfOrders = 1;
        mes.addNoMDEntries(noMd1);

        MarketDataIncrementalRefresh.NoMDEntries noMd2 = new MarketDataIncrementalRefresh.NoMDEntries();
        noMd2.mDUpdateAction = MDUpdateAction.CHANGE;
        noMd2.mDEntryType = MDEntryType.OFFER;
        noMd2.mDEntryPx = 4045.3;
        noMd1.securityID = "ca3Csfc";
        noMd1.mDEntrySize = 314511;
        mes.addNoMDEntries(noMd2);

        ByteBuffer bb = ByteBuffer.allocateDirect(1024);
        final MessageHeader HEADER = new MessageHeader();
        HEADER.beginString = "FIXT.1.1";
        HEADER.subHeader.mesSeqNum = 10;
        HEADER.subHeader.senderCompID = "FIX-Server";
        HEADER.subHeader.targetCompID = "FIX-Client";
        HEADER.subHeader.sendingTime = new Date();

        final MessageTrailer TRAILER = new MessageTrailer();

        CodeUtils.encodeMessage(bb, 0, mes, HEADER, TRAILER);

        System.out.println(CodeUtils.toString(bb));

        final MessageHeader DECODE_HEADER = new MessageHeader();
        final MessageTrailer DECODE_TRAILER = new MessageTrailer();
        final FIXMessageFactory FACTORY = new FIXMessageFactory();

        FIXMessage decodedMes = CodeUtils.decodeMessage(bb, 0, DECODE_HEADER, DECODE_TRAILER, FACTORY);

        TestCase.assertEquals(HEADER.beginString, DECODE_HEADER.beginString);
        TestCase.assertEquals(HEADER.subHeader.mesSeqNum, DECODE_HEADER.subHeader.mesSeqNum);
        TestCase.assertEquals(HEADER.subHeader.senderCompID, DECODE_HEADER.subHeader.senderCompID);
        TestCase.assertEquals(HEADER.subHeader.targetCompID, DECODE_HEADER.subHeader.targetCompID);
        TestCase.assertEquals(HEADER.subHeader.sendingTime, DECODE_HEADER.subHeader.sendingTime);

        assertTrue(decodedMes instanceof MarketDataIncrementalRefresh);
        MarketDataIncrementalRefresh decoded = (MarketDataIncrementalRefresh) decodedMes;

        assertEquals(mes.mDReqID, decoded.mDReqID);
        assertEquals(mes.mDBookType, decoded.mDBookType);
        assertEquals(mes.noMDEntries.size(), decoded.noMDEntries.size());

        for (int i = 0; i < mes.noMDEntries.size(); i++) {
            MarketDataIncrementalRefresh.NoMDEntries exp = mes.noMDEntries.get(0);
            MarketDataIncrementalRefresh.NoMDEntries act = decoded.noMDEntries.get(0);

            TestCase.assertEquals(exp.mDUpdateAction, act.mDUpdateAction);
            TestCase.assertEquals(exp.mDEntryType, act.mDEntryType);
            TestCase.assertEquals(exp.mDEntryPx, act.mDEntryPx, DELTA);
            TestCase.assertEquals(exp.securityID, act.securityID);
            TestCase.assertEquals(exp.mDEntrySize, act.mDEntrySize);
        }

    }
}
