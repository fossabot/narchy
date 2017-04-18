package jcog.bag.util;

import jcog.bag.Bag;
import jcog.pri.PLink;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * proxies to a delegate bag
 */
public class ProxyBag<X> implements Bag<X,PLink<X>> {

    @NotNull Bag<X,PLink<X>> bag;

    public ProxyBag(Bag<X,PLink<X>> delegate) {
        set(delegate);
    }

    @Override
    public float pri(@NotNull PLink<X> key) {
        return key.pri();
    }

    @NotNull
    @Override
    public X key(PLink<X> value) {
        return value.get();
    }

    public void set(Bag<X,PLink<X>> delegate) {
        bag = delegate;
    }

    @Override
    public @Nullable PLink<X> get(@NotNull Object key) {
        return bag.get(key);
    }

    @Override
    public int capacity() {
        return bag.capacity();
    }

    @Override
    public void forEachWhile(@NotNull Predicate<? super PLink<X>> each, int n) {
        bag.forEachWhile(each, n);
    }

    @Override
    public void forEach(Consumer<? super PLink<X>> action) {
        bag.forEach(action);
    }

    @Override
    public void forEachKey(Consumer<? super X> each) {
        bag.forEachKey(each);
    }

    @Override
    public void forEachWhile(@NotNull Predicate<? super PLink<X>> action) {
        bag.forEachWhile(action);
    }

    @Override
    public void forEach(int max, @NotNull Consumer<? super PLink<X>> action) {
        throw new UnsupportedOperationException(); //typing issue, TODO
    }

    @Override
    public void clear() {
        bag.clear();
    }

    @Nullable
    @Override
    public PLink<X> remove(@NotNull X x) {
        return bag.remove(x);
    }

    @Override
    public PLink<X> put(@NotNull PLink<X> b, float scale, @Nullable MutableFloat overflowing) {
        return bag.put(b, scale, overflowing);
    }

    @Override
    public Bag<X, PLink<X>> sample(int n, @NotNull IntObjectToIntFunction<? super PLink<X>> target) {
        bag.sample(n, target);
        return this;
    }

    @NotNull
    @Override
    public Bag<X,PLink<X>> sample(int n, @NotNull Predicate<? super PLink<X>> target) {
        bag.sample(n, target);
        return this;
    }

    @Override
    public int size() {
        return bag.size();
    }

    @NotNull
    @Override
    public Iterator<PLink<X>> iterator() {
        return bag.iterator();
    }

    @Override
    public boolean contains(@NotNull X it) {
        return bag.contains(it);
    }

    @Override
    public boolean isEmpty() {
        return bag.isEmpty();
    }

    @Override
    public boolean setCapacity(int c) {
        return bag.setCapacity(c);
    }

    @Override
    public float priMax() {
        return bag.priMax();
    }

    @Override
    public float priMin() {
        return bag.priMin();
    }


    @NotNull
    @Override
    public Bag<X,PLink<X>> commit(Consumer<PLink<X>> update) {
        bag.commit(update);
        return this;
    }



    @Override
    public void onAdded(PLink<X> v) {
        bag.onAdded(v);
    }

    @Override
    public void onRemoved(@NotNull PLink<X> v) {
        bag.onRemoved(v);
    }

    @Override
    public Bag<X,PLink<X>> commit() {
        bag.commit();
        return this;
    }
}