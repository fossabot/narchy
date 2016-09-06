package nars.bag.impl.experimental;

import nars.Param;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.budget.Forget;
import nars.budget.merge.BudgetMerge;
import nars.link.ArrayBLink;
import nars.link.BLink;
import nars.util.data.map.nbhm.HijacKache;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.util.Util.clamp;
import static nars.util.data.map.nbhm.HijacKache.*;

/**
 * Created by me on 9/4/16.
 */
public class HijackBag<X> implements Bag<X> {

    public final HijacKache<X, float[]> map;

    /**
     * max # of times allowed to scan through until either the next item is
     * accepted with final tolerance or gives up.
     * for safety, should be >= 1.0
     */
    private static final float SCAN_ITERATIONS = 1.1f;

    private float pressure;
    float priMin, priMax;
    int count;


    /**
     * the fraction of capacity which must contain entries to exceed in order to apply forgetting.
     * this is somewhat analogous to hashmap load factor
     */
    private final float FORGET_CAPACITY_THRESHOLD = 0.75f;


    public HijackBag(int capacity, int reprobes, Random random) {
        map = new HijacKache<>(capacity, reprobes, random) {
            @Override
            protected void reincarnateInto(Object[] k) {
                HijackBag.this.forEach((x,v)->{
                    float[] f = putBag(k, x, v[0], map.reprobes, map.rng);
                    if (f!=null) {
                        f[1] = v[1];
                        f[2] = v[2];
                    } else {
                        //lost
                    }
                });
            }
        };
    }


    @Override
    public void clear() {
        map.clear();
    }

    @Nullable
    @Override
    public BLink<X> remove(X x) {
        throw new UnsupportedOperationException();
        //return map.remove(x);
    }

    /**
     * returns the target array if insertion was successful, null otherwise
     */
    @Nullable
    private static final float[] putBag(final HijacKache map, final Object key, float newPri) {
        return putBag(map.data, key, newPri, map.reprobes, map.rng);
    }

    private static float[] putBag(Object[] kvs, Object key, float newPri, int reprobes, Random rng) {
        final int fullhash = HijacKache.hash(key); // throws NullPointerException if key null
        final int len = HijacKache.len(kvs); // Count of key/value pairs, reads kvs.length
        //final CHM chm = chm(kvs); // Reads kvs[0]
        final int[] hashes = HijacKache.hashes(kvs); // Reads kvs[1], read before kvs[0]
        int idx = fullhash & (len - 1);
        int startIdx = idx;

        int maxReprobes = reprobes;

        // Key-Claim stanza: spin till we can claim a Key (or force a resizing).
        int reprobe = 0;
        Object K = null;
        Object V = null;


        while (true) {             // Spin till we get a Key slot


            V = val(kvs, idx);         // Get old value (before volatile read below!)
            K = key(kvs, idx);         // Get current key

                // Found an empty Key slot - which means this Key has never been in
                // this table.  No need to put a Tombstone - the Key is not here!

            if (K == null) {
                // Claim the null key-slot
                if (CAS_key(kvs, idx, null, key)) { // Claim slot for Key
                    //chm._slots.add(1);      // Raise key-slots-used count
                    hashes[idx] = fullhash; // Memoize fullhash
                    break;                  // Got a null entry
                } else {
                    K = key(kvs, idx); //recalculte
                }
            }


            if (keyeq(K, key, hashes, idx, fullhash)) {
                break;                  // Got its existing entry
            }

            if (key == TOMBSTONE) { // found a TOMBSTONE key
                break;
            }

            //URGENT HIJACK
            if (reprobe++ > maxReprobes) {
                //probe expired on a non-empty index,
                // attempt hijack of probed index at random,
                // erasing the old value of another key

                idx = (startIdx + rng.nextInt(maxReprobes)) & (len - 1);

                V = val(kvs, idx);
                float[] f = (float[])V;
                float oldPri = f[0];
                boolean hijack;
                if (oldPri!=oldPri) {
                    hijack = true; //yes take this pre-deleted slot
                } else {
                    boolean newPriThresh = newPri > Param.BUDGET_EPSILON;
                    boolean oldPriThresh = oldPri > Param.BUDGET_EPSILON;
                    if (newPriThresh && oldPriThresh) {
                        hijack = (rng.nextFloat() > (newPri / (newPri + oldPri)));
                    } else if (newPriThresh) {
                        hijack = true;
                    } else {
                        hijack = false;
                    }
                }

                if (hijack) {
                    set_key(kvs, idx, key); // Got it!
                    hashes[idx] = fullhash; // Memoize fullhash
                    f[0] = Float.NaN;
//
//                    } else {
//                        return null; //the hijack got hijacked, just fail
//                    }
                } else {
                    return null;
                }
                break;
            }

            idx = (idx + 1) & (len - 1); // Reprobe!
        }
        // End of spinning till we get a Key slot


        if (V == null) {
            float[] ff = new float[] { Float.NaN, 0, 0 };
            if (CAS_val(kvs, idx, null, ff)) {
                return ff;
            } else {
                return (float[]) val(kvs, idx);
            }
        } else {
            return (float[]) V;
        }
    }

    @Override
    public void put(X x, Budgeted b, float scale, MutableFloat overflowing) {

        float nP = b.pri() * scale;
        float[] f = putBag(map, x, nP);
        if (f == null) {
            //rejected insert
            pressure += range(nP);
        } else {

            float pBefore = f[0];
            if (pBefore == pBefore) {
                //existing to merge with
                float overflow = BudgetMerge.plusBlend.merge(new ArrayBLink(x, f), b, scale);
                if (overflowing != null)
                    overflowing.add(overflow);

                pressure += range(f[0]) - pBefore;
            } else {
                //hijacked an empty entry
                f[0] = range(nP);
                f[1] = b.dur();
                f[2] = b.qua();
                pressure += nP;
            }

        }

    }

    /**
     * considers if this priority value stretches the current min/max range
     */
    private final float range(float p) {
        if (p > priMax) priMax = p;
        if (p < priMin) priMin = p;
        return p;
    }

    @Override
    public float priMin() {
        return priMin;
    }

    @Override
    public float priMax() {
        return priMax;
    }

    @Override
    public boolean setCapacity(int c) {
        return map.setCapacity(c);
    }

    @Override
    public @Nullable BLink<X> get(Object key) {
        float[] f = map.get(key);
        return f == null ? null : link((X) key, f);
    }

    private BLink<X> link(X key, float[] f) {
        if (key instanceof Budgeted) {
            return new ArrayBLink.ArrayBLinkToBudgeted((Budgeted)key, f);
        } else {
            return new ArrayBLink<>(key, f);
        }
    }

    @Override
    public int capacity() {
        return map.capacity();
    }

    @Override
    public void topWhile(Predicate<? super BLink<X>> each, int n) {
        throw new UnsupportedOperationException("yet");
    }

    @NotNull
    @Override
    public Bag<X> sample(int n, Predicate<? super BLink<X>> target) {
        Object[] data = this.map.data;
        int c = (data.length - 2) / 2;
        int jLimit = (int) Math.ceil(c * SCAN_ITERATIONS);

        int start = map.rng.nextInt(c); //starting index
        int i = start, j = 0;

        float r = curve(); //randomized threshold

        //TODO detect when the array is completely empty after 1 iteration through it in case the scan limit > 1.0

        //randomly choose traversal direction
        int di = map.rng.nextBoolean() ? +1 : -1;

        ArrayBLink<X> a = new ArrayBLink<>();

        while ((n > 0) && (j < jLimit) /* prevent infinite looping */) {
            int m = ((i += di) & (c - 1)) * 2 + 2;

            //slight chance these values may be inconsistently paired. TODO use CAS double-checked access
            Object k = data[m];
            Object v = data[m + 1];

            if (k != null && v != null) {

                float[] f = (float[]) v;


                float p = f[0];
                if (p==p && p >= 0) {

                    if ((r < p) || (r < p + tolerance((((float) j) / jLimit)))) {
                        if (target.test(a.set((X)k, f))) {
                            n--;
                            r = curve();
                        }
                    }
                }
            }
            j++;
        }
        return this;
    }


    @Override public int size() {
        return count;
    }

    @Override
    public void forEach(Consumer<? super BLink<X>> action) {
        Object[] data = map.data;
        int c = (data.length - 2) / 2;

        int j = 0;
        ArrayBLink<X> a = new ArrayBLink();

        while (j < c) {
            int m = ((j++) & (c - 1)) * 2 + 2;

            //slight chance these values may be inconsistently paired. TODO use CAS double-checked access
            Object k = data[m];
            Object v = data[m + 1];

            if (k != null && v != null) {
                action.accept(a.set((X) k, (float[]) v));
            }
        }
    }

    public void forEach(BiConsumer<X, float[]> e) {
        Object[] data = map.data;
        int c = (data.length - 2) / 2;

        int j = 0;

        while (j < c) {
            int m = ((j++) & (c - 1)) * 2 + 2;

            //slight chance these values may be inconsistently paired. TODO use CAS double-checked access
            Object k = data[m];
            Object v = data[m + 1];

            if (k != null && v != null) {
                float[] f = (float[]) v;
                float p = f[0];
                if (p == p) /* NaN? */ {
                    e.accept((X) k, f);
                } else {
                    data[m] = null; //nullify
                }
            }

        }
    }

    /**
     * yields the next threshold value to sample against
     */
    public float curve() {
        float c = map.rng.nextFloat();
        c *= c; //c^2 curve

        //float min = this.priMin;
        return (c); // * (priMax - min);
    }

    /**
     * beam width (tolerance range)
     * searchProgress in range 0..1.0
     */
    private float tolerance(float searchProgress) {
        /* raised polynomially to sharpen the selection curve, growing more slowly at the beginning */
        float exp = 6;
        return (float) Math.pow(searchProgress, exp);
    }

    @NotNull
    @Override
    public Iterator<BLink<X>> iterator() {
        return map.entrySet().stream().map(x -> (BLink<X>)new ArrayBLink<X>(x.getKey(), x.getValue())).iterator();
    }

    @Override
    public boolean contains(X it) {
        return map.contains(it);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @NotNull
    @Override
    public Bag<X> commit() {

        final float[] mass = {0};

        final int[] count = {0};
        int cap = capacity();
        final float[] min = {Float.MAX_VALUE};
        final float[] max = {Float.MIN_VALUE};

        forEach((X x, float[] f) -> {
            float p = f[0];
            if (p > max[0]) max[0] = p;
            if (p < min[0]) min[0] = p;
            mass[0] += p;
            count[0]++;
        });

        this.priMin = min[0];
        this.priMax = max[0];
        this.count = count[0];

        Forget f;
        if (mass[0] > 0 && (count[0] >= cap * FORGET_CAPACITY_THRESHOLD)) {
            float p = this.pressure;
            this.pressure = 0;

            float forgetRate = clamp(p / (p + mass[0]));

            if (forgetRate > Param.BUDGET_EPSILON) {
                f = new Forget(forgetRate);
            } else {
                f = null;
            }
        } else {
            f = null;
            this.pressure = 0;
        }

        return commit(f);
    }

    @NotNull
    @Override
    public Bag<X> commit(Consumer<BLink> each) {
        if (each != null && !isEmpty())
            forEach(each);

        return this;
    }


    @Override
    public X boost(Object key, float boost) {
        BLink<X> b = get(key);
        if (b != null && !b.isDeleted()) {
            float before = b.pri();
            b.priMult(boost);
            float after = b.pri();
            pressure += (after - before);
            return b.get();
        }
        return null;
    }
}
