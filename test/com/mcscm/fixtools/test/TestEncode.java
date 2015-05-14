package com.mcscm.fixtools.test;

import org.junit.Test;
import org.sample.ExecutionReport;

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
        expected.side = 5;
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
}
