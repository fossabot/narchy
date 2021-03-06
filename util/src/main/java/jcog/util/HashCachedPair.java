package jcog.util;

import jcog.TODO;
import jcog.Util;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.ImmutableEntry;

import java.util.Map;

public class HashCachedPair<T1, T2> implements Pair<T1, T2> {


    private final T1 one;
    private final T2 two;
    private final int hash;

    public HashCachedPair(T1 newOne, T2 newTwo) {
        this.one = newOne;
        this.two = newTwo;
        this.hash = Util.hashCombine(one, two);
    }


    @Override
    public T1 getOne() {
        return this.one;
    }

    @Override
    public T2 getTwo() {
        return this.two;
    }

    @Override
    public void put(Map<T1, T2> map) {
        map.put(this.one, this.two);
    }

    @Override
    public Pair<T2, T1> swap() {
        throw new TODO();
        //return new org.eclipse.collections.impl.tuple.PairImpl<>(this.two, this.one);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        Pair<?, ?> that = (Pair<?, ?>) o;

        return one.equals(that.getOne()) && two.equals(that.getTwo());
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return this.one + ":" + this.two;
    }

    @Override
    public Map.Entry<T1, T2> toEntry() {
        return ImmutableEntry.of(this.one, this.two);
    }

    @Override
    public int compareTo(Pair<T1, T2> other) {
        int i = ((Comparable<T1>) this.one).compareTo(other.getOne());
        if (i != 0) {
            return i;
        }
        return ((Comparable<T2>) this.two).compareTo(other.getTwo());
    }
}
