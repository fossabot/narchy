package jcog.bag.impl.hijack;

import jcog.bag.impl.HijackBag;
import jcog.pri.PForget;
import jcog.pri.PLink;
import jcog.pri.PriMerge;

import java.util.Random;
import java.util.function.Consumer;

/**
 * unsorted priority queue with stochastic replacement policy
 * <p>
 * it uses a AtomicReferenceArray<> to hold the data but Unsafe CAS operations might perform better (i couldnt get them to work like NBHM does).  this is necessary when an index is chosen for replacement that it makes certain it was replacing the element it thought it was (that it hadnt been inter-hijacked by another thread etc).  on an insert i issue a ticket to the thread and store this in a small ConcurrentHashMap<X,Integer>.  this spins in a busy putIfAbsent loop until it can claim the ticket for the object being inserted. this is to prevent the case where two threads try to insert the same object and end-up puttnig two copies in adjacent hash indices.  this should be rare so the putIfAbsent should usually work on the first try.  when it exits the update critical section it removes the key,value ticket freeing it for another thread.  any onAdded and onRemoved subclass event handling happen outside of this critical section, and all cases seem to be covered.
 */
public class DefaultHijackBag<K> extends PriorityHijackBag<K,PLink<K>> {

    public DefaultHijackBag(int capacity, int reprobes, PriMerge merge, Random random) {
        this(reprobes, merge, random);
        setCapacity(capacity);
    }

    public DefaultHijackBag(int reprobes, PriMerge merge, Random random) {
        super(random, merge, reprobes);
        this.map.set(HijackBag.EMPTY_ARRAY);
    }

    @Override
    protected Consumer<PLink<K>> forget(float avgToBeRemoved) {
        return new PForget(avgToBeRemoved);
    }



    @Override
    public K key(PLink<K> value) {
        return value.get();
    }

    @Override
    protected float priEpsilon() {
        return PLink.EPSILON_DEFAULT;
    }




}

//    public static void flatForget(BudgetHijackBag<?,? extends Budgeted> b) {
//        double p = b.pressure.get() /* MULTIPLIER TO ANTICIPATE NEXT period */;
//        int s = b.size();
//
//
//            //float ideal = s * b.temperature();
//            if (p > Param.BUDGET_EPSILON * s) {
//                if (b.pressure.compareAndSet(p, 0)) {
//
//                    b.commit(null); //precommit to get accurate mass
//                    float mass = b.mass;
//
//                    float over = //(float) ((p + mass) - ideal);
//                            ((float) p / ((float)p + mass) / s);
//                    if (over >= Param.BUDGET_EPSILON) {
//                        b.commit(x -> x.budget().priSub(over * (1f - x.qua())));
//                    }
//                }
//
//            }
//
//    }