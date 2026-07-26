package io.github.opencubicchunks.cubicchunks.api.util;

import java.util.Objects;

/**
 * Immutable inclusive integer range.
 */
public final class IntRange {
    private final int min;
    private final int max;

    public IntRange(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public boolean contains(int value) {
        return value >= min && value <= max;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IntRange)) return false;
        IntRange other = (IntRange) o;
        return this.min == other.min && this.max == other.max;
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max);
    }

    @Override
    public String toString() {
        return "IntRange[" + min + ", " + max + "]";
    }
}
