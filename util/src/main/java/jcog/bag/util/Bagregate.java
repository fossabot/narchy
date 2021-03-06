package jcog.bag.util;

import jcog.bag.Bag;
import jcog.bag.impl.PLinkArrayBag;
import jcog.math.FloatRange;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import jcog.pri.op.PriMerge;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * a bag which wraps another bag, accepts its value as input but at a throttled rate
 * resulting in containing effectively the integrated / moving average values of the input bag
 * TODO make a PLink version of ArrayBag since quality is not used here
 */
public class Bagregate<X extends Prioritized> implements Iterable<PriReference<X>> {

    final Bag<X, PriReference<X>> bag;
    private final Iterable<X> src;
    private final MutableFloat scale;
    final AtomicBoolean busy = new AtomicBoolean();

    public Bagregate(Stream<X> src, int capacity, float scale) {
        this(src::iterator, capacity, scale);
    }

    public Bagregate(Iterable<X> src, int capacity, float scale) {
        this.bag = new PLinkArrayBag(/*PriMerge.plus*/ PriMerge.replace, capacity) {
            @Override
            public void onRemove(Object value) {
                Bagregate.this.onRemove((PriReference<X>) value);
            }
        };
        this.src = src;
        this.scale = new FloatRange(scale, 0f, 1f);

    }

    protected void onRemove(PriReference<X> value) {

    }

    public boolean update() {
        if (src==null || !busy.compareAndSet(false, true))
            return false;

        try {

            bag.commit();

            float scale = this.scale.floatValue();

            src.forEach(x -> {
                if (include(x)) {
                    float pri = x.pri();
                    if (pri==pri)
                        bag.putAsync(new PLink<>(x, pri * scale));
                }
            });


        } finally {
            busy.set(false);
        }
        return true;
    }

    /**
     * can be overridden to filter entry
     */
    protected boolean include(X x) {
        return true;
    }

    @NotNull
    @Override
    public Iterator<PriReference<X>> iterator() {
        return bag.iterator();
    }

    @Override
    public void forEach(Consumer<? super PriReference<X>> action) {
        bag.forEach(action);
    }

    public void clear() {
        bag.clear();
    }


    //    @Nullable
//    @Override
//    public X key(PriReference<X> x) {
//        return x.get();
//    }
}
