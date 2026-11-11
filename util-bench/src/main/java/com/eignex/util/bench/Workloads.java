package com.eignex.util.bench;

import com.eignex.util.collections.*;
import com.eignex.util.collections.BitSet;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Typed batches; selection and reference checks happen outside the measured loop. */
public final class Workloads {
    private Workloads() {
    }

    public record Fixture(Supplier<Object> batch, Consumer<Object> check) {
        public void verify() {
            // Repeat to catch destructive workloads that only work on their initial state.
            for (int i = 0; i < 3; i++) check.accept(batch.get());
        }
    }

    private interface Factory {
        Fixture create(Data data, String operation);
    }

    private record Entry(Factory factory, Set<String> operations) {
    }

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    private static void register(String name, Factory factory, String... operations) {
        ENTRIES.put(name, new Entry(factory, new LinkedHashSet<>(List.of(operations))));
    }

    static {
        register("int-int-map", Workloads::intIntMap, "lookup-hit", "lookup-miss", "insert", "grow", "remove-restore");
        register("int-long-map", Workloads::intLongMap, "lookup-hit", "lookup-miss", "insert", "grow", "remove-restore");
        register("int-double-map", Workloads::intDoubleMap, "lookup-hit", "lookup-miss", "insert", "grow", "remove-restore");
        register("int-object-map", Workloads::intObjectMap, "lookup-hit", "lookup-miss", "insert", "grow", "remove-restore");
        register("long-int-map", Workloads::longIntMap, "lookup-hit", "lookup-miss", "insert", "grow", "remove-restore");
        register("long-object-map", Workloads::longObjectMap, "lookup-hit", "lookup-miss", "insert", "grow", "remove-restore");
        register("int-set", Workloads::intSet, "contains-hit", "contains-miss", "insert", "grow", "remove-restore");
        register("long-set", Workloads::longSet, "contains-hit", "contains-miss", "insert", "grow", "remove-restore");
        register("int-int-dense", Workloads::immutableIntDense, "lookup-hit", "lookup-miss", "build");
        register("int-int-sparse", Workloads::immutableIntSparse, "lookup-hit", "lookup-miss", "build");
        register("int-long-dense", Workloads::immutableLongDense, "lookup-hit", "lookup-miss", "build");
        register("int-long-sparse", Workloads::immutableLongSparse, "lookup-hit", "lookup-miss", "build");
        register("int-list", Workloads::intList, "read", "append", "grow", "remove-restore");
        register("long-list", Workloads::longList, "read", "append", "grow", "remove-restore");
        register("int-deque", Workloads::deque, "cycle", "grow");
        register("int-swap-set", Workloads::swapSet, "contains", "remove-restore");
        register("bits", Workloads::fixedBits, "get", "cardinality", "range", "traverse");
        register("dynamic-bits", Workloads::dynamicBits, "get", "cardinality", "range", "traverse", "grow");
        register("indexed-heap", Workloads::heap, "update", "extract-restore");
        register("disjoint-set", Workloads::disjoint, "union-find", "find-compressed");
    }

    public static List<String> names() {
        List<String> result = new ArrayList<>();
        ENTRIES.forEach((name, entry) -> entry.operations.forEach(op -> result.add(name + "." + op)));
        return List.copyOf(result);
    }

    public static Fixture create(String name, int size, long seed) {
        if (size < 1 || size > 65536) throw new IllegalArgumentException("size must be 1..65536");
        int dot = name.lastIndexOf('.');
        if (dot < 0) throw new IllegalArgumentException("Unknown workload: " + name);
        Entry entry = ENTRIES.get(name.substring(0, dot));
        String op = name.substring(dot + 1);
        if (entry == null || !entry.operations.contains(op)) throw new IllegalArgumentException("Unknown workload: " + name);
        return entry.factory.create(new Data(size, seed), op);
    }

    private static Fixture scalar(Supplier<Object> batch, Number expected, Runnable stateCheck) {
        return new Fixture(batch, actual -> {
            require(actual instanceof Number && (expected instanceof Double
                ? Double.compare(((Number) actual).doubleValue(), expected.doubleValue()) == 0
                : ((Number) actual).longValue() == expected.longValue()),
                "Checksum: expected " + expected + ", got " + actual);
            stateCheck.run();
        });
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }

    private static final class Data {
        final int n;
        final int[] intKeys, intMisses, denseKeys, denseMisses, intValues, order;
        final long[] longKeys, longMisses, longValues;
        final double[] doubleValues;
        final Integer[] objects;
        final long intSum, longSum;
        final double doubleSum;
        Data(int n, long seed) {
            this.n = n;
            intKeys = new int[n];
            intMisses = new int[n];
            denseKeys = new int[n];
            denseMisses = new int[n];
            intValues = new int[n];
            order = new int[n];
            longKeys = new long[n];
            longMisses = new long[n];
            longValues = new long[n];
            doubleValues = new double[n];
            objects = new Integer[n];
            var random = new SplittableRandom(seed);
            var ints = new HashSet<Integer>();
            var longs = new HashSet<Long>();
            long si = 0, sl = 0;
            double sd = 0;
            for (int i = 0; i < n; i++) {
                int k = i == 0 ? 0 : i == 1 ? Integer.MIN_VALUE : i == 2 ? Integer.MAX_VALUE : random.nextInt();
                while (!ints.add(k)) k = random.nextInt();
                intKeys[i] = k;
                long l = i == 0 ? 0 : i == 1 ? Long.MIN_VALUE : i == 2 ? Long.MAX_VALUE : random.nextLong();
                while (!longs.add(l)) l = random.nextLong();
                longKeys[i] = l;
                intValues[i] = i + 1;
                longValues[i] = (i + 1L) * 0x100000001L;
                doubleValues[i] = i + 0.25;
                objects[i] = i + 1;
                denseKeys[i] = 2 * i;
                denseMisses[i] = 2 * i + 1;
                order[i] = i;
                si += intValues[i];
                sl += longValues[i];
                sd += doubleValues[i];
            }
            for (int i = 0; i < n; i++) {
                int k;
                do {
                    k = random.nextInt();
                } while (!ints.add(k));
                intMisses[i] = k;
                long l;
                do {
                    l = random.nextLong();
                } while (!longs.add(l));
                longMisses[i] = l;
            }
            for (int i = n - 1; i > 0; i--) {
                int j = random.nextInt(i + 1), t = order[i];
                order[i] = order[j];
                order[j] = t;
            }
            intSum = si;
            longSum = sl;
            doubleSum = sd;
        }
    }

    private static Fixture intIntMap(Data d, String op) {
        var map = new MutableIntIntMap(d.n);
        for (int i = 0; i < d.n; i++) map.put(d.intKeys[i], i + 1);
        Consumer<Object> check = actual -> {
            var m = (MutableIntIntMap) actual;
            require(m.getSize() == d.n, "int-int-map size");
            for (int i = 0; i < d.n; i++) require(Objects.equals(m.getOrDefault(d.intKeys[i], -1), d.intValues[i]), "int-int-map entry " + i);
        };
        return switch (op) {
            case "lookup-hit" -> scalar(() -> {
                long sum = 0;
                for (int key : d.intKeys) sum += map.getOrDefault(key, -1);
                return sum;
            }, d.intSum, () -> check.accept(map));
            case "lookup-miss" -> scalar(() -> {
                long misses = 0;
                for (int key : d.intMisses) if (map.getOrDefault(key, -1) == -1) misses++;
                return misses;
            }, d.n, () -> check.accept(map));
            case "insert" -> new Fixture(() -> {
                map.clear();
                for (int i = 0; i < d.n; i++) map.put(d.intKeys[i], i + 1);
                return map;
            }, check);
            case "grow" -> new Fixture(() -> {
                var m = new MutableIntIntMap(1);
                for (int i = 0; i < d.n; i++) m.put(d.intKeys[i], i + 1);
                return m;
            }, check);
            case "remove-restore" -> scalar(() -> {
                long removed = 0;
                for (int i = 0; i < d.n; i++) {
                    if (map.remove(d.intKeys[i])) removed++;
                    map.put(d.intKeys[i], i + 1);
                }
                return removed;
            }, d.n, () -> check.accept(map));
            default -> throw new AssertionError(op);
        };
    }

    private static Fixture intLongMap(Data d, String op) {
        var map = new MutableIntLongMap(d.n);
        for (int i = 0; i < d.n; i++) map.put(d.intKeys[i], d.longValues[i]);
        Consumer<Object> check = actual -> {
            var m = (MutableIntLongMap) actual;
            require(m.getSize() == d.n, "int-long-map size");
            for (int i = 0; i < d.n; i++) require(Objects.equals(m.getOrDefault(d.intKeys[i], -1L), d.longValues[i]), "int-long-map entry " + i);
        };
        return switch (op) {
            case "lookup-hit" -> scalar(() -> {
                long sum = 0;
                for (int key : d.intKeys) sum += map.getOrDefault(key, -1L);
                return sum;
            }, d.longSum, () -> check.accept(map));
            case "lookup-miss" -> scalar(() -> {
                long misses = 0;
                for (int key : d.intMisses) if (map.getOrDefault(key, -1L) == -1) misses++;
                return misses;
            }, d.n, () -> check.accept(map));
            case "insert" -> new Fixture(() -> {
                map.clear();
                for (int i = 0; i < d.n; i++) map.put(d.intKeys[i], d.longValues[i]);
                return map;
            }, check);
            case "grow" -> new Fixture(() -> {
                var m = new MutableIntLongMap(1);
                for (int i = 0; i < d.n; i++) m.put(d.intKeys[i], d.longValues[i]);
                return m;
            }, check);
            case "remove-restore" -> scalar(() -> {
                long removed = 0;
                for (int i = 0; i < d.n; i++) {
                    if (map.remove(d.intKeys[i])) removed++;
                    map.put(d.intKeys[i], d.longValues[i]);
                }
                return removed;
            }, d.n, () -> check.accept(map));
            default -> throw new AssertionError(op);
        };
    }

    private static Fixture intDoubleMap(Data d, String op) {
        var map = new MutableIntDoubleMap(d.n);
        for (int i = 0; i < d.n; i++) map.put(d.intKeys[i], d.doubleValues[i]);
        Consumer<Object> check = actual -> {
            var m = (MutableIntDoubleMap) actual;
            require(m.getSize() == d.n, "int-double-map size");
            for (int i = 0; i < d.n; i++) require(Objects.equals(m.getOrDefault(d.intKeys[i], -1.0), d.doubleValues[i]), "int-double-map entry " + i);
        };
        return switch (op) {
            case "lookup-hit" -> scalar(() -> {
                double sum = 0;
                for (int key : d.intKeys) sum += map.getOrDefault(key, -1.0);
                return sum;
            }, d.doubleSum, () -> check.accept(map));
            case "lookup-miss" -> scalar(() -> {
                long misses = 0;
                for (int key : d.intMisses) if (map.getOrDefault(key, -1.0) == -1) misses++;
                return misses;
            }, d.n, () -> check.accept(map));
            case "insert" -> new Fixture(() -> {
                map.clear();
                for (int i = 0; i < d.n; i++) map.put(d.intKeys[i], d.doubleValues[i]);
                return map;
            }, check);
            case "grow" -> new Fixture(() -> {
                var m = new MutableIntDoubleMap(1);
                for (int i = 0; i < d.n; i++) m.put(d.intKeys[i], d.doubleValues[i]);
                return m;
            }, check);
            case "remove-restore" -> scalar(() -> {
                long removed = 0;
                for (int i = 0; i < d.n; i++) {
                    if (map.remove(d.intKeys[i])) removed++;
                    map.put(d.intKeys[i], d.doubleValues[i]);
                }
                return removed;
            }, d.n, () -> check.accept(map));
            default -> throw new AssertionError(op);
        };
    }

    @SuppressWarnings("unchecked")
    private static Fixture intObjectMap(Data d, String op) {
        var map = new MutableIntObjectMap<Integer>(d.n);
        for (int i = 0; i < d.n; i++) map.put(d.intKeys[i], d.objects[i]);
        Consumer<Object> check = actual -> {
            var m = (MutableIntObjectMap<Integer>) actual;
            require(m.getSize() == d.n, "int-object-map size");
            for (int i = 0; i < d.n; i++) require(Objects.equals(m.get(d.intKeys[i]), d.objects[i]), "int-object-map entry " + i);
        };
        return switch (op) {
            case "lookup-hit" -> scalar(() -> {
                long sum = 0;
                for (int key : d.intKeys) sum += map.get(key);
                return sum;
            }, d.intSum, () -> check.accept(map));
            case "lookup-miss" -> scalar(() -> {
                long misses = 0;
                for (int key : d.intMisses) if (map.get(key) == null) misses++;
                return misses;
            }, d.n, () -> check.accept(map));
            case "insert" -> new Fixture(() -> {
                map.clear();
                for (int i = 0; i < d.n; i++) map.put(d.intKeys[i], d.objects[i]);
                return map;
            }, check);
            case "grow" -> new Fixture(() -> {
                var m = new MutableIntObjectMap<Integer>(1);
                for (int i = 0; i < d.n; i++) m.put(d.intKeys[i], d.objects[i]);
                return m;
            }, check);
            case "remove-restore" -> scalar(() -> {
                long removed = 0;
                for (int i = 0; i < d.n; i++) {
                    if (map.remove(d.intKeys[i])) removed++;
                    map.put(d.intKeys[i], d.objects[i]);
                }
                return removed;
            }, d.n, () -> check.accept(map));
            default -> throw new AssertionError(op);
        };
    }

    private static Fixture longIntMap(Data d, String op) {
        var map = new MutableLongIntMap(d.n);
        for (int i = 0; i < d.n; i++) map.put(d.longKeys[i], i + 1);
        Consumer<Object> check = actual -> {
            var m = (MutableLongIntMap) actual;
            require(m.getSize() == d.n, "long-int-map size");
            for (int i = 0; i < d.n; i++) require(Objects.equals(m.getOrDefault(d.longKeys[i], -1), d.intValues[i]), "long-int-map entry " + i);
        };
        return switch (op) {
            case "lookup-hit" -> scalar(() -> {
                long sum = 0;
                for (long key : d.longKeys) sum += map.getOrDefault(key, -1);
                return sum;
            }, d.intSum, () -> check.accept(map));
            case "lookup-miss" -> scalar(() -> {
                long misses = 0;
                for (long key : d.longMisses) if (map.getOrDefault(key, -1) == -1) misses++;
                return misses;
            }, d.n, () -> check.accept(map));
            case "insert" -> new Fixture(() -> {
                map.clear();
                for (int i = 0; i < d.n; i++) map.put(d.longKeys[i], i + 1);
                return map;
            }, check);
            case "grow" -> new Fixture(() -> {
                var m = new MutableLongIntMap(1);
                for (int i = 0; i < d.n; i++) m.put(d.longKeys[i], i + 1);
                return m;
            }, check);
            case "remove-restore" -> scalar(() -> {
                long removed = 0;
                for (int i = 0; i < d.n; i++) {
                    if (map.remove(d.longKeys[i])) removed++;
                    map.put(d.longKeys[i], i + 1);
                }
                return removed;
            }, d.n, () -> check.accept(map));
            default -> throw new AssertionError(op);
        };
    }

    @SuppressWarnings("unchecked")
    private static Fixture longObjectMap(Data d, String op) {
        var map = new MutableLongObjectMap<Integer>(d.n);
        for (int i = 0; i < d.n; i++) map.put(d.longKeys[i], d.objects[i]);
        Consumer<Object> check = actual -> {
            var m = (MutableLongObjectMap<Integer>) actual;
            require(m.getSize() == d.n, "long-object-map size");
            for (int i = 0; i < d.n; i++) require(Objects.equals(m.get(d.longKeys[i]), d.objects[i]), "long-object-map entry " + i);
        };
        return switch (op) {
            case "lookup-hit" -> scalar(() -> {
                long sum = 0;
                for (long key : d.longKeys) sum += map.get(key);
                return sum;
            }, d.intSum, () -> check.accept(map));
            case "lookup-miss" -> scalar(() -> {
                long misses = 0;
                for (long key : d.longMisses) if (map.get(key) == null) misses++;
                return misses;
            }, d.n, () -> check.accept(map));
            case "insert" -> new Fixture(() -> {
                map.clear();
                for (int i = 0; i < d.n; i++) map.put(d.longKeys[i], d.objects[i]);
                return map;
            }, check);
            case "grow" -> new Fixture(() -> {
                var m = new MutableLongObjectMap<Integer>(1);
                for (int i = 0; i < d.n; i++) m.put(d.longKeys[i], d.objects[i]);
                return m;
            }, check);
            case "remove-restore" -> scalar(() -> {
                long removed = 0;
                for (int i = 0; i < d.n; i++) {
                    if (map.remove(d.longKeys[i])) removed++;
                    map.put(d.longKeys[i], d.objects[i]);
                }
                return removed;
            }, d.n, () -> check.accept(map));
            default -> throw new AssertionError(op);
        };
    }

    private static Fixture intSet(Data d, String op) {
        var set = new IntHashSet(d.n);
        for (int key : d.intKeys) set.add(key);
        Consumer<Object> check = actual -> {
            var value = (IntHashSet) actual;
            require(value.getSize() == d.n, "intSet size");
            for (int key : d.intKeys) require(value.contains(key), "intSet entry");
        };
        return switch (op) {
            case "contains-hit", "contains-miss" -> scalar(() -> {
                long hits = 0;
                for (int key : (op.equals("contains-hit") ? d.intKeys : d.intMisses)) if (set.contains(key)) hits++;
                return hits;
            }, op.equals("contains-hit") ? d.n : 0, () -> check.accept(set));
            case "insert" -> new Fixture(() -> {
                set.clear();
                for (int key : d.intKeys) set.add(key);
                return set;
            }, check);
            case "grow" -> new Fixture(() -> {
                var value = new IntHashSet(1);
                for (int key : d.intKeys) value.add(key);
                return value;
            }, check);
            case "remove-restore" -> scalar(() -> {
                long removed = 0;
                for (int key : d.intKeys) {
                    if (set.remove(key)) removed++;
                    set.add(key);
                }
                return removed;
            }, d.n, () -> check.accept(set));
            default -> throw new AssertionError(op);
        };
    }

    private static Fixture longSet(Data d, String op) {
        var set = new LongHashSet(d.n);
        for (long key : d.longKeys) set.add(key);
        Consumer<Object> check = actual -> {
            var value = (LongHashSet) actual;
            require(value.getSize() == d.n, "longSet size");
            for (long key : d.longKeys) require(value.contains(key), "longSet entry");
        };
        return switch (op) {
            case "contains-hit", "contains-miss" -> scalar(() -> {
                long hits = 0;
                for (long key : (op.equals("contains-hit") ? d.longKeys : d.longMisses)) if (set.contains(key)) hits++;
                return hits;
            }, op.equals("contains-hit") ? d.n : 0, () -> check.accept(set));
            case "insert" -> new Fixture(() -> {
                set.clear();
                for (long key : d.longKeys) set.add(key);
                return set;
            }, check);
            case "grow" -> new Fixture(() -> {
                var value = new LongHashSet(1);
                for (long key : d.longKeys) value.add(key);
                return value;
            }, check);
            case "remove-restore" -> scalar(() -> {
                long removed = 0;
                for (long key : d.longKeys) {
                    if (set.remove(key)) removed++;
                    set.add(key);
                }
                return removed;
            }, d.n, () -> check.accept(set));
            default -> throw new AssertionError(op);
        };
    }

    private static Fixture immutableIntDense(Data d, String op) {
        var keys = d.denseKeys;
        var map = IntIntMap.Companion.build(keys, d.intValues, -1, 4);
        Consumer<Object> check = actual -> {
            var value = (IntIntMap) actual;
            require(value.getSize() == d.n, "immutable size");
            for (int i = 0; i < d.n; i++) require(value.getOrDefault(keys[i], -1) == d.intValues[i], "immutable entry");
        };
        return switch (op) {
            case "lookup-hit" -> scalar(() -> {
                long sum = 0;
                for (int key : keys) sum += map.getOrDefault(key, -1);
                return sum;
            }, d.intSum, () -> check.accept(map));
            case "lookup-miss" -> scalar(() -> {
                long misses = 0;
                for (int key : d.denseMisses) if (map.getOrDefault(key, -1) == -1) misses++;
                return misses;
            }, d.n, () -> check.accept(map));
            case "build" -> new Fixture(() -> IntIntMap.Companion.build(keys, d.intValues, -1, 4), check);
            default -> throw new AssertionError(op);
        };
    }

    private static Fixture immutableIntSparse(Data d, String op) {
        var keys = d.intKeys;
        var map = IntIntMap.Companion.build(keys, d.intValues, -1, 0);
        Consumer<Object> check = actual -> {
            var value = (IntIntMap) actual;
            require(value.getSize() == d.n, "immutable size");
            for (int i = 0; i < d.n; i++) require(value.getOrDefault(keys[i], -1) == d.intValues[i], "immutable entry");
        };
        return switch (op) {
            case "lookup-hit" -> scalar(() -> {
                long sum = 0;
                for (int key : keys) sum += map.getOrDefault(key, -1);
                return sum;
            }, d.intSum, () -> check.accept(map));
            case "lookup-miss" -> scalar(() -> {
                long misses = 0;
                for (int key : d.intMisses) if (map.getOrDefault(key, -1) == -1) misses++;
                return misses;
            }, d.n, () -> check.accept(map));
            case "build" -> new Fixture(() -> IntIntMap.Companion.build(keys, d.intValues, -1, 0), check);
            default -> throw new AssertionError(op);
        };
    }

    private static Fixture immutableLongDense(Data d, String op) {
        var keys = d.denseKeys;
        var map = IntLongMap.Companion.build(keys, d.longValues, -1, 4);
        Consumer<Object> check = actual -> {
            var value = (IntLongMap) actual;
            require(value.getSize() == d.n, "immutable size");
            for (int i = 0; i < d.n; i++) require(value.getOrDefault(keys[i], -1) == d.longValues[i], "immutable entry");
        };
        return switch (op) {
            case "lookup-hit" -> scalar(() -> {
                long sum = 0;
                for (int key : keys) sum += map.getOrDefault(key, -1);
                return sum;
            }, d.longSum, () -> check.accept(map));
            case "lookup-miss" -> scalar(() -> {
                long misses = 0;
                for (int key : d.denseMisses) if (map.getOrDefault(key, -1) == -1) misses++;
                return misses;
            }, d.n, () -> check.accept(map));
            case "build" -> new Fixture(() -> IntLongMap.Companion.build(keys, d.longValues, -1, 4), check);
            default -> throw new AssertionError(op);
        };
    }

    private static Fixture immutableLongSparse(Data d, String op) {
        var keys = d.intKeys;
        var map = IntLongMap.Companion.build(keys, d.longValues, -1, 0);
        Consumer<Object> check = actual -> {
            var value = (IntLongMap) actual;
            require(value.getSize() == d.n, "immutable size");
            for (int i = 0; i < d.n; i++) require(value.getOrDefault(keys[i], -1) == d.longValues[i], "immutable entry");
        };
        return switch (op) {
            case "lookup-hit" -> scalar(() -> {
                long sum = 0;
                for (int key : keys) sum += map.getOrDefault(key, -1);
                return sum;
            }, d.longSum, () -> check.accept(map));
            case "lookup-miss" -> scalar(() -> {
                long misses = 0;
                for (int key : d.intMisses) if (map.getOrDefault(key, -1) == -1) misses++;
                return misses;
            }, d.n, () -> check.accept(map));
            case "build" -> new Fixture(() -> IntLongMap.Companion.build(keys, d.longValues, -1, 0), check);
            default -> throw new AssertionError(op);
        };
    }

    private static Fixture intList(Data d, String op) {
        var list = new IntArrayList(d.n);
        list.addAll(d.intValues);
        Consumer<Object> check = actual -> {
            var value = (IntArrayList) actual;
            require(value.getSize() == d.n, "list size");
            for (int i = 0; i < d.n; i++) require(value.get(i) == d.intValues[i], "list entry");
        };
        return switch (op) {
            case "read" -> scalar(() -> {
                long sum = 0;
                for (int i = 0; i < d.n; i++) sum += list.get(i);
                return sum;
            }, d.intSum, () -> check.accept(list));
            case "append" -> new Fixture(() -> {
                list.clear();
                for (var v : d.intValues) list.add(v);
                return list;
            }, check);
            case "grow" -> new Fixture(() -> {
                var value = new IntArrayList(1);
                for (var v : d.intValues) value.add(v);
                return value;
            }, check);
            case "remove-restore" -> scalar(() -> {
                long sum = 0;
                for (int i : d.order) {
                    int old = list.get(i), last = list.last();
                    sum += old;
                    list.removeAt(i);
                    if (i < list.getSize()) list.set(i, old);
                    list.add(last);
                }
                return sum;
            }, d.intSum, () -> check.accept(list));
            default -> throw new AssertionError(op);
        };
    }

    private static Fixture longList(Data d, String op) {
        var list = new LongArrayList(d.n);
        for (long value : d.longValues) list.add(value);
        Consumer<Object> check = actual -> {
            var value = (LongArrayList) actual;
            require(value.getSize() == d.n, "list size");
            for (int i = 0; i < d.n; i++) require(value.get(i) == d.longValues[i], "list entry");
        };
        return switch (op) {
            case "read" -> scalar(() -> {
                long sum = 0;
                for (int i = 0; i < d.n; i++) sum += list.get(i);
                return sum;
            }, d.longSum, () -> check.accept(list));
            case "append" -> new Fixture(() -> {
                list.clear();
                for (var v : d.longValues) list.add(v);
                return list;
            }, check);
            case "grow" -> new Fixture(() -> {
                var value = new LongArrayList(1);
                for (var v : d.longValues) value.add(v);
                return value;
            }, check);
            case "remove-restore" -> scalar(() -> {
                long sum = 0;
                for (int i : d.order) {
                    long old = list.get(i), last = list.last();
                    sum += old;
                    list.removeAt(i);
                    if (i < list.getSize()) list.set(i, old);
                    list.add(last);
                }
                return sum;
            }, d.longSum, () -> check.accept(list));
            default -> throw new AssertionError(op);
        };
    }

    private static Fixture deque(Data d, String op) {
        var deque = new IntArrayDeque(d.n);
        for (int v : d.intValues) deque.addLast(v);
        Consumer<Object> check = actual -> {
            var value = (IntArrayDeque) actual;
            require(value.getSize() == d.n, "deque size");
            for (int i = 0; i < d.n; i++) require(value.get(i) == d.intValues[i], "deque order");
        };
        if (op.equals("grow")) return new Fixture(() -> {
            var value = new IntArrayDeque(1);
            for (int v : d.intValues) value.addLast(v);
            return value;
        }, check);
        return scalar(() -> {
            long sum = 0;
            for (int i = 0; i < d.n; i++) {
                int v = deque.removeFirst();
                sum += v;
                deque.addLast(v);
            }
            return sum;
        }, d.intSum, () -> check.accept(deque));
    }

    private static Fixture swapSet(Data d, String op) {
        var set = new IntSwapSet(d.n);
        for (int id : d.order) set.add(id);
        Runnable check = () -> {
            require(set.getSize() == d.n, "swap set size");
            for (int id : d.order) require(set.contains(id), "swap set entry");
        };
        if (op.equals("contains")) return scalar(() -> {
            long count = 0;
            for (int id : d.order) if (set.contains(id)) count++;
            return count;
        }, d.n, check);
        return scalar(() -> {
            long count = 0;
            for (int id : d.order) {
                if (set.remove(id)) count++;
                set.add(id);
            }
            return count;
        }, d.n, check);
    }

    private static Fixture fixedBits(Data d, String op) {
        return bits(d, op, new Bits(d.n));
    }

    private static Fixture dynamicBits(Data d, String op) {
        if (op.equals("grow")) return new Fixture(() -> {
            var result = new DynamicBits(0);
            for (int i = 0; i < d.n; i++) result.add(i % 3 == 0);
            return result;
        }, actual -> checkBits(d, (BitSet) actual));
        var value = new DynamicBits(d.n);
        value.resize(d.n);
        return bits(d, op, value);
    }

    private static void checkBits(Data d, BitSet bits) {
        require(bits.getSize() == d.n, "bits size");
        for (int i = 0; i < d.n; i++) require(bits.get(i) == (i % 3 == 0), "bit " + i);
    }

    private static Fixture bits(Data d, String op, BitSet bits) {
        long count = (d.n + 2L) / 3;
        long indexSum = 3 * count * (count - 1) / 2;
        for (int i = 0; i < d.n; i += 3) bits.set(i);
        Runnable check = () -> checkBits(d, bits);
        return switch (op) {
            case "get" -> scalar(() -> {
                long hits = 0;
                for (int id : d.order) if (bits.get(id)) hits++;
                return hits;
            }, count, check);
            case "cardinality" -> scalar(() -> bits.cardinality(), count, check);
            case "traverse" -> scalar(() -> {
                long[] sum = {
                    0
                };
                bits.forEachSet(id -> sum[0] += id);
                return sum[0];
            }, indexSum, check);
            case "range" -> scalar(() -> {
                bits.clear();
                bits.setRange(0, d.n);
                bits.clearRange(1, d.n);
                return bits.cardinality();
            }, 1, () -> {
                require(bits.getSize() == d.n && bits.get(0) && bits.cardinality() == 1, "range result");
                for (int i = 1; i < d.n; i++) require(!bits.get(i), "range tail");
            });
            default -> throw new AssertionError(op);
        };
    }

    private static Fixture heap(Data d, String op) {
        var heap = new IndexedMaxHeap(d.n);
        for (int id : d.order) heap.insert(id, id);
        Runnable check = () -> {
            require(heap.getSize() == d.n && heap.peekMax() == d.n - 1, "heap root/size");
            for (int id : d.order) require(heap.contains(id) && heap.keyOf(id) == id, "heap entry");
            for (int id = d.n - 1; id >= 0; id--) require(heap.extractMax() == id, "heap priority order");
            require(heap.extractMax() == -1, "heap empty");
            for (int id : d.order) heap.restore(id);
        };
        if (op.equals("update")) return scalar(() -> {
            for (int id : d.order) {
                heap.updateKey(id, id + d.n);
                heap.updateKey(id, id);
            }
            return heap.peekMax();
        }, d.n - 1, check);
        return scalar(() -> {
            long sum = 0;
            for (int i = 0; i < d.n; i++) sum += heap.extractMax();
            for (int id : d.order) heap.restore(id);
            return sum;
        }, (long) d.n * (d.n - 1) / 2, check);
    }

    private static Fixture disjoint(Data d, String op) {
        if (op.equals("union-find")) return new Fixture(() -> {
            var set = new IntDisjointSet(d.n);
            for (int i = 1; i < d.n; i++) set.union(d.order[i - 1], d.order[i]);
            for (int id : d.order) set.find(id);
            return set;
        }, actual -> {
            var set = (IntDisjointSet) actual;
            int root = set.find(0);
            for (int i = 0; i < d.n; i++) require(set.find(i) == root, "union-find connectivity");
        });
        var set = new IntDisjointSet(d.n);
        for (int i = 1; i < d.n; i++) set.union(d.order[i - 1], d.order[i]);
        for (int id : d.order) set.find(id);
        return scalar(() -> {
            long sum = 0;
            for (int id : d.order) sum += set.find(id);
            return sum;
        },
        (long) d.n * d.order[d.n - 1], () -> {
        });
    }
}
