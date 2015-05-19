package com.mcscm.fixtools;

public interface MessageFactory {

    FIXMessage create(String tag);
}
