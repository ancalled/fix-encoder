package com.mcscm.fixtools.perf;

import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(java.util.concurrent.TimeUnit.NANOSECONDS)
@Fork(value = 1)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
@State(Scope.Benchmark)
public class BaselineTest1 {

    private StringBuilder buf;
    private final String testString = "test-string";
    private final int testInt = 21541;
    private final long testLong = 2154113113L;
    private final double testDouble = Math.PI;


    @Setup(Level.Iteration)
    public void init() {
        buf = new StringBuilder();
    }

    @Benchmark
    public void sbAppendString() {
        buf.append(testString);
    }

    @Benchmark
    public void sbAppendInt() {
        buf.append(testInt);
    }

    @Benchmark
    public void sbAppendLong() {
        buf.append(testLong);
    }

//    @Benchmark
//    public void sbAppendDouble() {
//        buf.append(testDouble);
//    }
}
