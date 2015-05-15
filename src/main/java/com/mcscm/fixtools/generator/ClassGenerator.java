package com.mcscm.fixtools.generator;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ClassGenerator {

    public static String tab(int level) {
        return IntStream.range(0, level).mapToObj(i -> "\t").collect(Collectors.joining());
    }
}
