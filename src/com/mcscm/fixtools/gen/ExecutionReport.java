package com.mcscm.fixtools.gen;

import com.mcscm.fixtools.FIXMessage;

import java.lang.String;
import java.util.Date;

public class ExecutionReport implements FIXMessage {

    public String orderID;
    public String execID;
    public char execType;
    public char ordStatus;
    public int ordRejReason;
    public String account;
    public String symbol;
    public String securityID;
    public char side;
    public double price;
    public char timeInForce;
    public long leavesQty;
    public long cumQty;
    public Date transactTime;

    public String getType() {
        return "8";
    }


    public String encode() {
        final StringBuilder sb = new StringBuilder();

        //todo: append headers

        if (orderID != null) {
            sb.append("37=").append(orderID).append("\001");
        }
        if (execID != null) {
            sb.append("17=").append(execID).append("\001");
        }
        if (execType != 0) {
            sb.append("150=").append(execType).append("\001");
        }
        if (ordStatus != 0) {
            sb.append("39=").append(ordStatus).append("\001");
        }
        if (ordRejReason != 0) {
            sb.append("103=").append(ordRejReason).append("\001");
        }
        if (account != null) {
            sb.append("1=").append(account).append("\001");
        }
        if (symbol != null) {
            sb.append("55=").append(symbol).append("\001");
        }
        if (securityID != null) {
            sb.append("48=").append(securityID).append("\001");
        }
        if (side != 0) {
            sb.append("54=").append(side).append("\001");
        }
        if (price != 0.0) {
            sb.append("44=").append(price).append("\001");
        }
        if (timeInForce != 0) {
            sb.append("59=").append(timeInForce).append("\001");
        }
        if (leavesQty != 0) {
            sb.append("151=").append(leavesQty).append("\001");
        }
        if (cumQty != 0) {
            sb.append("14=").append(cumQty).append("\001");
        }
        if (transactTime != null) {
            sb.append("60=").append(DateFormatter.format(transactTime)).append("\001");
        }
        //todo: append tails

        return sb.toString();
    }


    public void decode(String fixmes) {
        int end, middle, start = 0;
        for (; ; ) {
            end = fixmes.indexOf('\001', start);
            middle = fixmes.indexOf('=', start);
            if (end < 0) break;
            int tag = Integer.valueOf(fixmes.substring(start, middle));
            String value = fixmes.substring(middle + 1, end);

            if (tag == 37) {
                orderID = value;
            } else if (tag == 17) {
                execID = value;
            } else if (tag == 150) {
                execType = value.charAt(0);
            } else if (tag == 39) {
                ordStatus = value.charAt(0);
            } else if (tag == 103) {
                ordRejReason = Integer.parseInt(value);
            } else if (tag == 1) {
                account = value;
            } else if (tag == 55) {
                symbol = value;
            } else if (tag == 48) {
                securityID = value;
            } else if (tag == 54) {
                side = value.charAt(0);
            } else if (tag == 44) {
                price = Double.parseDouble(value);
            } else if (tag == 59) {
                timeInForce = value.charAt(0);
            } else if (tag == 151) {
                leavesQty = Long.parseLong(value);
            } else if (tag == 14) {
                cumQty = Long.parseLong(value);
            } else if (tag == 60) {
                transactTime = DateFormatter.parse(value);
            }
            start = end + 1;
        }
    }


    @Override
    public String toString() {
        return "ExecutionReport{" +
                "\n\torderID='" + orderID + '\'' +
                "\n\t, execID='" + execID + '\'' +
                "\n\t, execType=" + execType +
                "\n\t, ordStatus=" + ordStatus +
                "\n\t, ordRejReason=" + ordRejReason +
                "\n\t, account='" + account + '\'' +
                "\n\t, symbol='" + symbol + '\'' +
                "\n\t, securityID='" + securityID + '\'' +
                "\n\t, side=" + side +
                "\n\t, price=" + price +
                "\n\t, timeInForce=" + timeInForce +
                "\n\t, leavesQty=" + leavesQty +
                "\n\t, cumQty=" + cumQty +
                "\n\t, transactTime=" + transactTime +
                '}';
    }

    public static void main(String[] args) {
        ExecutionReport rep = new ExecutionReport();
        rep.orderID = "131012";
        rep.symbol = "I0001";
        rep.account = "A0031";
        rep.price = 134.1;
        rep.cumQty = 50000;
        rep.side = 5;
        rep.transactTime = new Date();

        final String encode = rep.encode();
        System.out.println(encode);

        ExecutionReport rep2 = new ExecutionReport();
        rep2.decode(encode);
        System.out.println(rep2);

    }

}