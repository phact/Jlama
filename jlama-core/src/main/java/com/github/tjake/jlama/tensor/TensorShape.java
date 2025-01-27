package com.github.tjake.jlama.tensor;

import com.github.tjake.jlama.util.Pair;
import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 *
 */
public class TensorShape {
    public static TensorShape of(int... shape) {
        return new TensorShape(shape, Optional.empty());
    }

    public static TensorShape sparse(int[] shape, Pair<Integer, Integer> sparseOffset) {
        return new TensorShape(shape, Optional.of(sparseOffset));
    }

    private final int[] tshape;
    private final long capacity;
    private final Optional<Pair<Integer, Integer>> sparseRange;
    private final boolean isSparse;
    private final int sparseOffset;
    private final int sparseLength;

    private TensorShape(int[] shape, Optional<Pair<Integer, Integer>> sparseRange) {
        this.tshape = shape;
        this.sparseRange = sparseRange;
        this.isSparse = sparseRange.isPresent();
        this.sparseOffset = sparseRange.map(Pair::left).orElse(0);
        this.sparseLength = sparseRange.map(Pair::right).orElse(shape[shape.length - 1]);

        long c = 1;
        for (int i = 0; i < shape.length - 1; i++)
            c *= shape[i];

        c *= sparseLength;
        this.capacity = c;
    }

    final public boolean isSparse() {
        return isSparse;
    }

    public int dims() {
        return tshape.length;
    }

    public int dim(int i) {
        Preconditions.checkArgument(i < tshape.length);
        return tshape[i];
    }

    public int sparseLength() {
        return sparseLength;
    }

    public int sparseAdjustment(int offset) {
        Preconditions.checkArgument(sparseOffset <= offset, "Offset is outside of sparse range");
        return offset - sparseOffset;
    }

    public TensorShape scaleLastDim(float scale) {
        int[] copy = Arrays.copyOf(tshape, tshape.length);
        copy[copy.length - 1] *= scale;
        return isSparse ? sparse(copy, Pair.create((int)(sparseOffset * scale), (int)(sparseLength * scale))) : of(copy);
    }

    public TensorShape setDimValue(int dim, int value) {
        Preconditions.checkArgument(dim < tshape.length);
        int[] copy = Arrays.copyOf(tshape, tshape.length);
        copy[dim] = value;
        int newSparseLength = copy[copy.length - 1];
        return isSparse ? sparse(copy, Pair.create(sparseOffset, newSparseLength)) : of(copy);
    }

    public int first() {
        return tshape[0];
    }

    public int last() {
        return tshape[tshape.length - 1];
    }

    public long size() {
        return capacity;
    }

    public TensorShape sparsify(int offset, int length) {
        Preconditions.checkArgument(!isSparse, "Cannot sparsify a sparse tensor");
        return new TensorShape(tshape, Optional.of(Pair.create(offset, length)));
    }

    public TensorShape slice(int numDims) {
        Preconditions.checkArgument(numDims < tshape.length, "Too many dimensions specified for tensor");
        return new TensorShape(Arrays.copyOfRange(tshape, numDims, tshape.length), sparseRange);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TensorShape that = (TensorShape) o;
        return Arrays.equals(tshape, that.tshape) && Objects.equals(sparseRange, that.sparseRange);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(sparseRange);
        result = 31 * result + Arrays.hashCode(tshape);
        return result;
    }

    @Override
    public String toString() {
        return "TensorShape{" +
                "tshape=" + Arrays.toString(tshape) +
                ", capacity=" + capacity +
                ", sparseRange=" + sparseRange +
                '}';
    }
}
