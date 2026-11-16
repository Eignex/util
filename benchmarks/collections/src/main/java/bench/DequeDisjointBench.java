package bench;

import com.eignex.util.collections.IntArrayDeque;
import com.eignex.util.collections.IntDisjointSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

/** Focused allocation and indexed-removal benchmarks; scores are per invocation. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class DequeDisjointBench {
    @State(Scope.Thread)
    public static class Groups {
        @Param({"1024", "65536"}) public int size;
        @Param({"1", "32", "65536"}) public int groupSize;
        private IntDisjointSet sets;

        @Setup(Level.Trial)
        public void setup() {
            sets = new IntDisjointSet(size);
            for (int i = 1; i < size; i++) {
                if (i % groupSize != 0) sets.union(i - 1, i);
            }
            verify(); // Compress paths before timing repeated partition snapshots.
        }

        @TearDown(Level.Trial)
        public void verify() {
            var groups = sets.groups();
            if (groups.size() != (size + groupSize - 1) / groupSize) throw new AssertionError("group count");
            int next = 0;
            for (int[] group : groups) {
                if (group.length != Math.min(groupSize, size - next)) throw new AssertionError("group size");
                for (int member : group) if (member != next++) throw new AssertionError("group order");
            }
            if (next != size) throw new AssertionError("members");
        }
    }

    @State(Scope.Thread)
    public static class Deque {
        @Param({"1024", "65536"}) public int size;
        @Param({"front", "middle", "back"}) public String position;
        private IntArrayDeque deque;
        private int index;

        @Setup(Level.Trial)
        public void setup() {
            deque = new IntArrayDeque(size);
            for (int i = 0; i < size; i++) deque.addLast(i);
            // Start with a wrapped full ring. Removal/restoration keeps every element live.
            for (int i = 0; i < size / 2; i++) deque.addLast(deque.removeFirst());
            index = switch (position) {
                case "front" -> 0;
                case "middle" -> size / 2;
                case "back" -> size - 1;
                default -> throw new IllegalArgumentException(position);
            };
        }

        @TearDown(Level.Trial)
        public void verify() {
            if (deque.getSize() != size) throw new AssertionError("deque size");
            boolean[] seen = new boolean[size];
            for (int i = 0; i < size; i++) {
                int value = deque.get(i);
                if (value < 0 || value >= size || seen[value]) throw new AssertionError("deque contents");
                seen[value] = true;
            }
        }
    }

    @State(Scope.Thread)
    public static class Growth {
        @Param({"1024", "65536"}) public int size;

        IntArrayDeque build() {
            var deque = new IntArrayDeque(size);
            for (int i = 0; i < size; i++) deque.addLast(i);
            for (int i = 0; i < size / 2; i++) deque.addLast(deque.removeFirst());
            deque.addLast(size); // Grow a wrapped full ring.
            return deque;
        }

        @Setup(Level.Trial)
        public void verify() {
            var deque = build();
            if (deque.getSize() != size + 1) throw new AssertionError("growth size");
            for (int i = 0; i < size; i++) {
                if (deque.get(i) != (i + size / 2) % size) throw new AssertionError("growth order");
            }
            if (deque.get(size) != size) throw new AssertionError("growth append");
        }
    }

    @Benchmark public List<int[]> groups(Groups state) { return state.sets.groups(); }

    @Benchmark public int removeAt(Deque state) {
        int value = state.deque.removeAt(state.index);
        state.deque.addLast(value);
        return value;
    }

    @Benchmark public IntArrayDeque grow(Growth state) { return state.build(); }
}
