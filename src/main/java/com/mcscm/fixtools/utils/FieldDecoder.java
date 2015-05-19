package com.mcscm.fixtools.utils;

import java.nio.ByteBuffer;

@FunctionalInterface
public interface FieldDecoder<K> {

    int decode(ByteBuffer bb, int offset, int length, K k);

}
