package com.mcscm.fixtools;


import java.nio.ByteBuffer;

public interface FIXMessage {

    String getType();

    byte getSeparator();

    String encode();

    void encode(ByteBuffer buf);

    int decode(String fixmes, int fromIdx);

    int decode(ByteBuffer bb, int offset);

    int decode(ByteBuffer bb, int offset, int length);

    String printParsed(String indent);

    void printWarnings();

    void clearWarnings();


}
