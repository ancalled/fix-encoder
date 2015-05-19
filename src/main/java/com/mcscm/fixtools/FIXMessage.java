package com.mcscm.fixtools;


import java.nio.ByteBuffer;

public interface FIXMessage {

//    String getType();

//    byte[] getTypeBytes();

    String encode();

    void encode(ByteBuffer buf);

    int decode(String fixmes, int fromIdx);

    int decode(ByteBuffer bb, int offset);


}
