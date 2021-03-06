package jcog.bag.impl;

import jcog.Util;
import jcog.pri.Prioritized;
import jcog.pri.Priority;
import jcog.pri.op.PriMerge;

import java.util.Map;
import java.util.Random;

/**
 * ArrayBag with a randomized sampling range
 */
public class CurveBag<X extends Priority> extends PriArrayBag<X> {

//    public static final int SAMPLE_WINDOW_SIZE = 4;

    public CurveBag(PriMerge mergeFunction, Map<X, X> map, int cap) {
        this(mergeFunction, map);
        setCapacity(cap);
    }


    public CurveBag(PriMerge mergeFunction, Map<X, X> map) {
        super(mergeFunction, map);
    }

    @Override
    protected int sampleStart(Random random, int size) {
        assert(size > 0);
        if (size == 1 || random==null)
            return 0;
        else {
            float min = this.priMin();
            float max = this.priMax();
            float diff = max - min;
            if (diff > Prioritized.EPSILON * size) {
                float i = random.nextFloat(); //uniform
                //normalize to the lack of dynamic range
                i = Util.lerp(diff, i /* flat */, (i*i) /* curved */);
                int j = (int) /*Math.floor*/(i * (size-0.5f));
                if (j >= size) j = size-1;
                else if (j < 0) j = 0;
                return j;
            } else {
                return random.nextInt(size);
            }
        }
    }

    @Override
    protected int sampleNext(Random rng, int size, int i) {
        return sampleStart(rng, size);
    }

    //    @Override
//    public Bag<X, X> sample(Random rng, BagCursor<? super X> each) {
//        return sample(rng, each, this::pri);
//    }
//
//    private Bag<X, X> sample(Random random, BagCursor<? super X> each, FloatFunction<X> pri) {
//
//
//        restart:
//        while (true) {
//            final Object[] ii = this.items.list;
//            final int s = Math.min(size(), ii.length);
//            if (s <= 0)
//                return this;
//
//
//
//
//            int windowCap = Math.min(s, SAMPLE_WINDOW_SIZE); //ESTIMATE HUERISTIC
//            float[] wPri = new float[windowCap];
//            Object[] wVal = new Object[windowCap];
//
//            /** emergency null counter, in case map becomes totally null avoids infinite loop*/
//            int nulls = 0;
//
//            //0. seek to some non-null item
//            int prefilled = 0;
//            int i =
//                //random.nextInt(s);
//                sampleStart(random, s);
//
//            boolean direction = random.nextBoolean();
//            while ((nulls+prefilled) < s && size() > 0) {
//                X v = (X) ii[i];
//
//                //move ahead now in case it terminates on the first try, it wont remain on the same value when the next phase starts
//                if (direction) {
//                    if (++i == s) i = 0;
//                } else {
//                    if (--i == -1) i = s - 1;
//                }
//
//                if (v != null) {
//                    wVal[windowCap - 1 - prefilled] = v;
//                    wPri[windowCap - 1 - prefilled] = pri.floatValueOf(v);
//                    if (++prefilled >= windowCap)
//                        break;
//                } else {
//                    nulls++;
//                }
//
//            }
//
//
//            //2. slide window, roulette sampling from it as it changes
//
//            nulls = 0;
//            while (nulls < s && size() > 0) {
//                X v0 = (X) ii[i];
//                float p;
//                if (v0 == null) {
//                    nulls++;
//                } else  if ((p = pri.floatValueOf(v0)) == p /* not deleted*/) {
//                    nulls=0; //reset contiguous null counter
//
//                    //shift window down, erasing value (if any) in position 0
//                    System.arraycopy(wVal, 1, wVal, 0, windowCap - 1);
//                    wVal[windowCap - 1] = v0;
//                    System.arraycopy(wPri, 1, wPri, 0, windowCap - 1);
//                    wPri[windowCap - 1] = Util.max(p, Pri.EPSILON); //to differentiate from absolute zero
//
//                    int which = Roulette.decideRoulette(windowCap, (r) -> wPri[r], random);
//                    X v = (X) wVal[which];
//                    if (v == null)
//                        continue; //shouldnt happen but just in case
//
//                    BagSample next = each.next(v);
//                    if (next.remove) {
//                        remove(key(v));
//                    }
//
//                    if (next.stop) {
//                        break;
//                    } else if (next.remove) {
//
//                        //prevent the removed item from further selection
//                        if (which==windowCap-1) {
//                            //if it's in the last place, it will be replaced in next cycle anyway
//                            wVal[which] = null;
//                            wPri[which] = 0;
//                        } else if (wVal[0] != null) {
//                            //otherwise swap a non-null value in the 0th place with it, because it will be removed in the next shift
//                            ArrayUtils.swap(wVal, 0, which);
//                            ArrayUtils.swap(wPri, 0, which);
//                        }
//                    }
//                } else {
//                    //deleted
//                     remove(key(v0));
//                }
//
//
//                if (ii != this.items.list)
//                    continue restart;
//
//                if (direction) {
//                    if (++i == s) i = 0;
//                } else {
//                    if (--i == -1) i = s - 1;
//                }
//            }
//
//            return this;
//        }
//
//    }



    //    /** optimized point sample impl */
//    @Nullable
//    @Override public PriReference<X> sample() {
//        Object[] ii = items.array();
//        if (ii.length == 0)
//            return null;
//
//        int size = Math.min(ii.length, this.size());
//        if (size == 0)
//            return null;
//
//        if (size == 1)
//            return (PriReference<X>) ii[0];
//
//        for (int i = 0; i < size /* max # of trials */; i++) {
//            Object n = ii[ThreadLocalRandom.current().nextInt(size)];
//            if (n != null)
//                return (PriReference<X>) n;
//        }
//        return null;
//    }


}