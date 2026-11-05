package bench;

import com.eignex.util.collections.*;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class CollectionsBench {
    static final int COUNT = 1024;
    final int[] keys = new int[COUNT];
    final long[] longKeys = new long[COUNT];
    final Object value = new Object();
    MutableIntIntMap ints;
    MutableLongIntMap longs;
    MutableIntObjectMap<Object> objects;
    IntHashSet intSet;
    LongHashSet longSet;

    @Setup public void setup() {
        var random = new SplittableRandom(42);
        ints = new MutableIntIntMap(COUNT);
        longs = new MutableLongIntMap(COUNT);
        objects = new MutableIntObjectMap<>(COUNT);
        intSet = new IntHashSet(COUNT);
        longSet = new LongHashSet(COUNT);
        for (int i = 0; i < COUNT; i++) {
            keys[i] = random.nextInt();
            longKeys[i] = random.nextLong();
            ints.put(keys[i], i);
            longs.put(longKeys[i], i);
            objects.put(keys[i], value);
            intSet.add(keys[i]);
            longSet.add(longKeys[i]);
        }
    }

    @Benchmark @OperationsPerInvocation(COUNT)
    public int intLookup() {
        int sum = 0;
        for (int key : keys) sum += ints.getOrDefault(key, -1);
        return sum;
    }
    @Benchmark @OperationsPerInvocation(COUNT)
    public int longLookup() {
        int sum = 0;
        for (long key : longKeys) sum += longs.getOrDefault(key, -1);
        return sum;
    }
    @Benchmark @OperationsPerInvocation(COUNT)
    public int intInsert() {
        ints.clear();
        for (int i = 0; i < COUNT; i++) ints.put(keys[i], i);
        return ints.getSize();
    }
    @Benchmark @OperationsPerInvocation(COUNT)
    public int longInsert() {
        longs.clear();
        for (int i = 0; i < COUNT; i++) longs.put(longKeys[i], i);
        return longs.getSize();
    }
    @Benchmark @OperationsPerInvocation(COUNT)
    public MutableIntIntMap intGrowth() {
        var map = new MutableIntIntMap(8);
        for (int i = 0; i < COUNT; i++) map.put(keys[i], i);
        return map;
    }
    @Benchmark @OperationsPerInvocation(COUNT)
    public MutableLongIntMap longGrowth() {
        var map = new MutableLongIntMap(8);
        for (int i = 0; i < COUNT; i++) map.put(longKeys[i], i);
        return map;
    }
    @Benchmark @OperationsPerInvocation(COUNT)
    public int intRemoveRestore() {
        int removed = 0;
        for (int key : keys) {
            if (ints.remove(key)) removed++;
            ints.put(key, key);
        }
        return removed;
    }
    @Benchmark @OperationsPerInvocation(COUNT)
    public int longRemoveRestore() {
        int removed = 0;
        for (long key : longKeys) {
            if (longs.remove(key)) removed++;
            longs.put(key, 7);
        }
        return removed;
    }
    @Benchmark @OperationsPerInvocation(COUNT)
    public int objectRemoveRestore() {
        int removed = 0;
        for (int key : keys) {
            if (objects.remove(key)) removed++;
            objects.put(key, value);
        }
        return removed;
    }
    @Benchmark @OperationsPerInvocation(COUNT)
    public int intSetContains() {
        int found = 0;
        for (int key : keys) if (intSet.contains(key)) found++;
        return found;
    }
    @Benchmark @OperationsPerInvocation(COUNT)
    public int longSetContains() {
        int found = 0;
        for (long key : longKeys) if (longSet.contains(key)) found++;
        return found;
    }
}
