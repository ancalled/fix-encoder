package com.mcscm.fixtools;

import java.nio.ByteBuffer;

public class MessageCoder {


    public static FIXMessage decodeMessage(ByteBuffer bb, int offset, MessageWrapper wrapper, MessageFactory factory) {
        if (wrapper == null || factory == null) return null;

        offset = wrapper.decode(bb, offset);
        FIXMessage message = factory.create(wrapper.msgType);
        message.decode(bb, offset);
        //todo parse and check checksum
        return message;
    }

    public static void encodeMessage(ByteBuffer bb, int offset, FIXMessage mes) {

    }
}
