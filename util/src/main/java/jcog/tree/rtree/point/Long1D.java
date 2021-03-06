package jcog.tree.rtree.point;

import jcog.tree.rtree.HyperPoint;

/**
 * Created by me on 12/2/16.
 */
public class Long1D implements HyperPoint {

    public final long x;

    public Long1D(long X) {
        this.x = X;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(x);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || ((Long1D) obj).x == x;
    }

    @Override
    public int dim() {
        return 1;
    }

    @Override
    public Long coord(int d) {
        return x;
    }

    @Override
    public double distance(HyperPoint p) {
        return Math.abs(x - ((Long1D) p).x);
    }

    @Override
    public double distance(HyperPoint p, int d) {
        return distance(p);
    }
}
