package bench;

import com.eignex.util.collections.MutableLongIntMap;
import java.util.HashSet;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

/** Whole-map batches with random, sequential and high-bit key controls. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class LongHashBench {
    @State(Scope.Thread)
    public static class Data {
        @Param({"1024", "8192"}) public int size;
        @Param({"random", "sequential", "upper32", "upper48"}) public String shape;
        long[] keys;
        MutableLongIntMap map;
        long expected;

        @Setup(Level.Trial)
        public void setup() {
            keys = new long[size];
            var random = new SplittableRandom(42);
            var seen = new HashSet<Long>();
            for (int i = 0; i < size; i++) {
                long key = switch (shape) {
                    case "random" -> random.nextLong();
                    case "sequential" -> i;
                    case "upper32" -> (long) i << 32;
                    case "upper48" -> (long) i << 48;
                    default -> throw new IllegalArgumentException(shape);
                };
                while (!seen.add(key)) {
                    if (!shape.equals("random")) throw new IllegalArgumentException("repeated strided key");
                    key = random.nextLong();
                }
                keys[i] = key;
            }
            expected = (long) size * (size + 1) / 2;
            var bench = new LongHashBench();
            map = bench.build(this);
            verifyMap(map);
            if (bench.lookup(this) != expected) throw new AssertionError("lookup checksum");
            if (bench.removeRestore(this) != expected) throw new AssertionError("remove checksum");
            verify();
        }

        void verifyMap(MutableLongIntMap value) {
            if (value.getSize() != size) throw new AssertionError("size");
            for (int i = 0; i < size; i++) {
                if (value.getOrDefault(keys[i], -1) != i + 1) throw new AssertionError("entry " + i);
            }
        }

        @TearDown(Level.Trial)
        public void verify() {
            verifyMap(map);
            verifyMap(new LongHashBench().build(this));
            if (new LongHashBench().lookup(this) != expected) throw new AssertionError("lookup checksum");
        }
    }

    @Benchmark
    public MutableLongIntMap build(Data state) {
        var map = new MutableLongIntMap(state.size);
        for (int i = 0; i < state.size; i++) map.put(state.keys[i], i + 1);
        return map;
    }

    @Benchmark
    public long lookup(Data state) {
        long sum = 0;
        for (long key : state.keys) sum += state.map.getOrDefault(key, -1);
        return sum;
    }

    @Benchmark
    public long removeRestore(Data state) {
        long sum = 0;
        for (int i = 0; i < state.size; i++) {
            sum += state.map.removeAndGet(state.keys[i], -1);
            state.map.put(state.keys[i], i + 1);
        }
        return sum;
    }
}
