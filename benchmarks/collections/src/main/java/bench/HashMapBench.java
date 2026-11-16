package bench;

import com.eignex.util.collections.*;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

/** Required nullable lookups and clear paths; timing includes one dispatch per invocation. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class HashMapBench {
    @State(Scope.Thread)
    public static class Lookup {
        @Param({"1024", "65536"}) public int size;
        @Param({"int", "long"}) public String kind;
        @Param({"null", "value"}) public String payload;
        int[] keys;
        long[] wideKeys;
        Object[] values;
        int missing;
        long wideMissing;
        MutableIntObjectMap<Object> ints;
        MutableLongObjectMap<Object> longs;

        @Setup(Level.Trial)
        public void setup() {
            keys = new int[size];
            wideKeys = new long[size];
            values = new Object[size];
            var random = new SplittableRandom(42);
            var seen = new HashSet<Integer>();
            var wideSeen = new HashSet<Long>();
            if (kind.equals("int")) ints = new MutableIntObjectMap<>(size);
            else longs = new MutableLongObjectMap<>(size);
            for (int i = 0; i < size; i++) {
                int key = i == 0 ? 0 : i == 1 ? Integer.MIN_VALUE : i == 2 ? Integer.MAX_VALUE : random.nextInt();
                while (!seen.add(key)) key = random.nextInt();
                long wideKey = i == 0 ? 0 : i == 1 ? Long.MIN_VALUE : i == 2 ? Long.MAX_VALUE : random.nextLong();
                while (!wideSeen.add(wideKey)) wideKey = random.nextLong();
                keys[i] = key;
                wideKeys[i] = wideKey;
                values[i] = payload.equals("null") ? null : new Object();
                if (ints != null) ints.put(key, values[i]);
                else longs.put(wideKey, values[i]);
            }
            do { missing = random.nextInt(); } while (seen.contains(missing));
            do { wideMissing = random.nextLong(); } while (wideSeen.contains(wideMissing));
            verify();
        }

        @TearDown(Level.Trial)
        public void verify() {
            if ((ints != null ? ints.getSize() : longs.getSize()) != size) throw new AssertionError("size");
            for (int i = 0; i < size; i++) {
                Object value = ints != null ? ints.getValue(keys[i]) : longs.getValue(wideKeys[i]);
                if (value != values[i]) throw new AssertionError("value");
            }
            long nulls = new HashMapBench().requiredLookup(this);
            if (nulls != (payload.equals("null") ? size : 0)) throw new AssertionError("lookup checksum");
            try {
                if (ints != null) ints.getValue(missing);
                else longs.getValue(wideMissing);
                throw new AssertionError("missing key returned a value");
            } catch (NoSuchElementException expected) {
                // A stored null must remain distinguishable from an absent key.
            }
        }
    }

    @State(Scope.Thread)
    public static class Clear {
        @Param({"1024", "65536"}) public int size;
        @Param({"int-int", "int-long", "int-double", "long-int", "int-object", "long-object"}) public String kind;
        @Param({"false", "true"}) public boolean refill;
        Object map;
        final Object payload = new Object();

        @Setup(Level.Trial)
        public void setup() {
            map = switch (kind) {
                case "int-int" -> new MutableIntIntMap(size);
                case "int-long" -> new MutableIntLongMap(size);
                case "int-double" -> new MutableIntDoubleMap(size);
                case "long-int" -> new MutableLongIntMap(size);
                case "int-object" -> new MutableIntObjectMap<Object>(size);
                case "long-object" -> new MutableLongObjectMap<Object>(size);
                default -> throw new IllegalArgumentException(kind);
            };
            verify();
        }

        Object run() {
            switch (kind) {
                case "int-int" -> {
                    var value = (MutableIntIntMap) map;
                    if (refill) value.put(0, 0);
                    value.clear();
                }
                case "int-long" -> {
                    var value = (MutableIntLongMap) map;
                    if (refill) value.put(0, 0L);
                    value.clear();
                }
                case "int-double" -> {
                    var value = (MutableIntDoubleMap) map;
                    if (refill) value.put(0, 0.0);
                    value.clear();
                }
                case "long-int" -> {
                    var value = (MutableLongIntMap) map;
                    if (refill) value.put(0L, 0);
                    value.clear();
                }
                case "int-object" -> {
                    @SuppressWarnings("unchecked") var value = (MutableIntObjectMap<Object>) map;
                    if (refill) value.put(0, payload);
                    value.clear();
                }
                case "long-object" -> {
                    @SuppressWarnings("unchecked") var value = (MutableLongObjectMap<Object>) map;
                    if (refill) value.put(0L, payload);
                    value.clear();
                }
                default -> throw new AssertionError(kind);
            }
            return map;
        }

        @TearDown(Level.Trial)
        public void verify() {
            if (map instanceof IntKeyed value) {
                if (value.getSize() != 0 || value.containsKey(0)) throw new AssertionError("not empty");
            } else {
                var value = (LongKeyed) map;
                if (value.getSize() != 0 || value.containsKey(0L)) throw new AssertionError("not empty");
            }
        }
    }

    @Benchmark
    public long requiredLookup(Lookup state) {
        long nulls = 0;
        if (state.ints != null) {
            for (int key : state.keys) if (state.ints.getValue(key) == null) nulls++;
        } else {
            for (long key : state.wideKeys) if (state.longs.getValue(key) == null) nulls++;
        }
        return nulls;
    }

    @Benchmark public Object clear(Clear state) { return state.run(); }
}
