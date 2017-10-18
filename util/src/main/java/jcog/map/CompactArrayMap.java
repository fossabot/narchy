package jcog.map;

import jcog.TODO;
import jcog.list.FasterList;

import java.util.Arrays;
import java.util.function.Function;

/**
 * TODO dont extend FasterList because 'size' field isnt used. removals will involve CoW
 */
public class CompactArrayMap<K, V> extends FasterList {

    final Object lock = new Object();

    public CompactArrayMap() {
        this(0);
    }

    public CompactArrayMap(int initialCapacity) {
        super(initialCapacity);
    }


    public boolean containsValue(Object aValue) {
        throw new TODO();
    }

    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    public V get(Object key) {
        Object[] a = items;
        if (a!=null) {
            int s = a.length;
            for (int i = 0; i < s; ) {
                Object k = a[i];
                if (k!=null) {
                    if (keyEquals(k, key))
                        return (V) a[i + 1];
                }
                i += 2;
            }
        }
        return null;
    }

    public void put(K key, V value) {
        synchronized (lock) {
            Object[] a = items;
            if (a == null) {
                this.items = new Object[] { key, value };
            } else {
                int s = size;
                for (int i = 0; i < s; ) {
                    if (keyEquals(a[i], key)) {
                        a[i + 1] = value; //directly modify
                        return;
                    }
                    i += 2;
                }
                a = Arrays.copyOf(a, s+2);
                a[s++] = key;
                a[s] = value;
                this.items = a;
            }
        }
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V e = get(key);
        if (e!=null)
            return e;

        V v = mappingFunction.apply(key);
        synchronized (lock) {
            put(key, v);
            return v;
        }
    }


    @Override
    public boolean remove(Object object) {
        throw new UnsupportedOperationException("use removeKey");
    }

    public V removeKey(Object key) {
        throw new TODO();
//        int i = indexOf(key);
//        if (i != -1) {
//
//        }
//        return null;
    }

    /** override for alternate equality test */
    public boolean keyEquals(Object a, Object b) {
        return a.equals(b);
    }

    @Override
    public void clear() {
        synchronized(lock) {
            clearFast();
        }
    }

    public void clearExcept(K key) {
        synchronized(lock) {
            V exist = get(key);
            clearFast();
            if (exist!=null)
                put(key, exist);
        }
    }

    public void clearPut(K key, V value) {
        synchronized(lock) {
            clearFast();
            if (value!=null)
                put(key, value);
        }
    }
}
