package com.eignex.util.bench;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

/** One dispatch per batch; report scores per batch and normalize explicitly in the adapter. */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class CollectionsBenchmark {
    @Param({"int-int-map.lookup-hit"})
    public String workload;
    @Param({"1024"})
    public int size;
    @Param({"42"})
    public long seed;
    private Workloads.Fixture fixture;

    @Setup(Level.Trial)
    public void setup() {
        fixture = Workloads.create(workload, size, seed);
        fixture.verify();
    }

    @Benchmark
    public Object batch() {
        return fixture.batch().get();
    }

    @TearDown(Level.Trial)
    public void verifyFinalState() {
        fixture.verify();
    }
}
