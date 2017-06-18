package jcog.tree.rtree.rect;


import jcog.tree.rtree.HyperRect;
import jcog.tree.rtree.point.LongND;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.function.Function;


/**
 * Created by jcovert on 6/15/15.
 */

public class RectLongND implements HyperRect<LongND>, Serializable, Comparable<RectLongND> {

    public static final HyperRect ALL_1 = RectLongND.all(1);
    public static final HyperRect ALL_2 = RectLongND.all(2);
    public static final HyperRect ALL_3 = RectLongND.all(3);
    public static final HyperRect ALL_4 = RectLongND.all(4);
    public static final LongND unbounded = new LongND() {
        @Override
        public String toString() {
            return "*";
        }
    };
    public final LongND min;
    public final LongND max;

    public RectLongND() {
        min = unbounded;
        max = unbounded;
    }

    public RectLongND(final LongND p) {
        min = p;
        max = p;
    }

    public RectLongND(long[] a, long[] b) {
        this(new LongND(a), new LongND(b));
    }


    public RectLongND(final LongND a, final LongND b) {
        int dim = a.dim();

        long[] min = new long[dim];
        long[] max = new long[dim];

        long[] ad = a.coord;
        long[] bd = b.coord;
        for (int i = 0; i < dim; i++) {
            long ai = ad[i];
            long bi = bd[i];
            min[i] = Math.min(ai, bi);
            max[i] = Math.max(ai, bi);
        }
        this.min = new LongND(min);
        this.max = new LongND(max);
    }

    public static HyperRect all(int i) {
        return new RectLongND(LongND.fill(i, Long.MIN_VALUE), LongND.fill(i, Long.MAX_VALUE));
    }

    @Override
    public boolean contains(final HyperRect _inner) {
        final RectLongND inner = (RectLongND) _inner;

        int dim = dim();
        for (int i = 0; i < dim; i++) {
            if (!(min.coord[i] <= inner.min.coord[i] && max.coord[i] >= inner.max.coord[i]))
                //if (min.coord[i] > inner.min.coord[i] || max.coord[i] < inner.max.coord[i])
                return false;
        }
        return true;
    }

    @Override
    public boolean intersects(final HyperRect r) {
        final RectLongND x = (RectLongND) r;

        int dim = dim();
        for (int i = 0; i < dim; i++) {
            /*return !((min.x > r2.max.x) || (r2.min.x > max.x) ||
                    (min.y > r2.max.y) || (r2.min.y > max.y));*/

            if (min.coord[i] > x.max.coord[i] || x.min.coord[i] > max.coord[i])
                return false;
        }
        return true;
    }

    @Override
    public double cost() {
        float sigma = 1f;
        int dim = dim();
        for (int i = 0; i < dim; i++) {
            sigma *= getRangeFinite(i, 1 /* an infinite dimension can not be compared, so just ignore it */);
        }
        return sigma;
    }

    @Override
    public HyperRect mbr(final HyperRect r) {
        final RectLongND x = (RectLongND) r;

        int dim = dim();
        long[] newMin = new long[dim];
        long[] newMax = new long[dim];
        for (int i = 0; i < dim; i++) {
            newMin[i] = Math.min(min.coord[i], x.min.coord[i]);
            newMax[i] = Math.max(max.coord[i], x.max.coord[i]);
        }
        return new RectLongND(newMin, newMax);
    }


    @Override
    public double center(int dim) {
        return centerF(dim);
    }

    public double centerF(int dim) {
        long min = this.min.coord[dim];
        long max = this.max.coord[dim];
        if ((min == Long.MIN_VALUE) && (max == Long.MAX_VALUE))
            return 0;
        if (min == Long.MIN_VALUE)
            return max;
        if (max == Long.MAX_VALUE)
            return min;

        return (max + min) / 2.0f;
    }

    @Override
    public LongND center() {
        int dim = dim();
        long[] c = new long[dim];
        for (int i = 0; i < dim; i++) {
            c[i] = (min.coord(i) + max.coord(i))/2;
        }
        return new LongND(c);
    }


    @Override
    public int dim() {
        return min.dim();
    }

    @Override
    public LongND min() {
        return min;
    }

    @Override
    public LongND max() {
        return max;
    }

    @Override
    public double getRange(final int i) {
        float min = this.min.coord[i];
        float max = this.max.coord[i];
        if (min == max)
            return 0;
        if ((min == Long.MIN_VALUE) || (max == Long.MAX_VALUE))
            return Long.MAX_VALUE;
        return (max - min);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null /*|| getClass() != o.getClass()*/) return false;

        RectLongND r = (RectLongND) o;
        return min.equals(r.min) && max.equals(r.max);
    }

    @Override
    public int compareTo(@NotNull RectLongND o) {
        int a = min.compareTo(o.min);
        if (a != 0) return a;
        int b = max.compareTo(o.max);
        return b;
    }

    @Override
    public int hashCode() {
        int result = min.hashCode();
        result = 31 * result + max.hashCode();
        return result;
    }

    public String toString() {
        if (min.equals(max)) {
            return min.toString();
        } else {
            final StringBuilder sb = new StringBuilder();
            sb.append('(');
            sb.append(min);
            sb.append(',');
            sb.append(max);
            sb.append(')');
            return sb.toString();
        }
    }


    public final static class Builder<X extends RectLongND> implements Function<X, HyperRect> {

        @Override
        public X apply(final X rect2D) {
            return rect2D;
        }

    }


}