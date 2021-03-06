package jcog.data.map;

import jcog.TODO;
import jcog.util.ArrayIterator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class ConcurrentFastIteratingHashMap<X, Y> extends AbstractMap<X, Y> {

    final Y[] emptyArray;
    final Map<X, Y> map = new ConcurrentHashMap<>();
    volatile Y[] list = null;


    public ConcurrentFastIteratingHashMap(Y[] emptyArray) {
        this.emptyArray = emptyArray;
    }

    /**
     * without synchronizing this entire method, the best this can do is
     * a near atomic invalidation of the list after the hashmap method returns
     */
    @Override
    public Y putIfAbsent(X key, Y value) {
        Y r;
        if ((r = map.putIfAbsent(key, value)) == null) {
            invalidate();
            return null;
        }
        return r;
    }

    /**
     * without synchronizing this entire method, the best this can do is
     * a near atomic invalidation of the list after the hashmap method returns
     */
    @Override
    public Y remove(Object key) {
        Y r = map.remove(key);
        if (r != null)
            invalidate();
        return r;
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (map.remove(key, value)) {
            invalidate();
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        map.clear();
        invalidate();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * this is the fast value iterating method
     */
    public void forEachValue(Consumer<? super Y> action) {
        Y[] x = valueArray();
        for (Y t : x)
            action.accept(t);
    }

    @Override
    public Y compute(X key, BiFunction<? super X, ? super Y, ? extends Y> remappingFunction) {
        throw new TODO();
    }

    @Override
    public Y computeIfAbsent(X key, Function<? super X, ? extends Y> mappingFunction) {
        final boolean[] changed = {false};
        Y prev = map.computeIfAbsent(key, (p) -> {
            Y next = mappingFunction.apply(p);
            if (next != p)
                changed[0] = true;
            return next;
        });

        if (changed[0]) {
            invalidate();
        }

        return prev;
    }

    public void invalidate() {
        list = null;
    }

    public boolean whileEachValue(Predicate<? super Y> action) {
        Y[] x = valueArray();
        for (int i = 0, xLength = x.length; i < xLength; i++) {
            if (!action.test(x[i]))
                return false;
        }
        return true;
    }

    public boolean whileEachValueReverse(Predicate<? super Y> action) {
        Y[] x = valueArray();
        for (int i = x.length - 1; i >= 0; i--) {
            if (!action.test(x[i]))
                return false;
        }
        return true;
    }

    @Override
    public Set<Entry<X, Y>> entrySet() {
        return map.entrySet();
    }

    @Override
    public Set<X> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<Y> values() {
        return map.values();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }


    public Iterator<Y> valueIterator() {
        return ArrayIterator.get(valueArray());
    }


    public Y[] valueArray() {
        Y[] x = list;
        if (x == null) {
            return this.list = values().toArray(emptyArray);
        } else {
            return x;
        }
    }


    @Override
    public Y put(X key, Y value) {
        return map.put(key, value);
    }

}
