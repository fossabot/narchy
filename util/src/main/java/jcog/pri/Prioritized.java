package jcog.pri;


import jcog.Util;
import org.jetbrains.annotations.NotNull;

/**
 * something which has a priority floating point value
 *      stores priority with 32-bit float precision
 *      restricted to 0..1.0 range
 *      NaN means it is 'deleted' which is a valid and testable state
 */
public interface Prioritized extends Deleteable {

    /**
     * a value in range 0..1.0 inclusive.
     * if the value is NaN, then it means this has been deleted
     */
    /*default float pri() {
        return priority().pri();
    }*/
    float pri();

    default float priSafe(float valueIfDeleted) {
        float p = pri();
        return p == p ? p : valueIfDeleted;
    }

    default float priElseZero() {
        float p = pri();
        return p == p ? p : 0;
        //return priSafe(0);
    }
    default float priElseNeg1() {
        float p = pri();
        return p == p ? p : -1;
        //return priSafe(-1);
    }


    /** the result of this should be that pri() is not finite (ex: NaN)
     * returns false if already deleted (allowing overriding subclasses to know if they shold also delete) */
    boolean delete();

    @Override
    boolean isDeleted();
//    default boolean isDeleted() {
//        float p = pri();
//        return p!=p; //fast NaN check
//    }

    @NotNull
    Priority priority();

    static float priSum(@NotNull Iterable<? extends Prioritized> c) {
        float totalPriority = 0;
        for (Prioritized i : c)
            totalPriority += i.priElseZero();
        return totalPriority;
    }

    default boolean equalsBudget(@NotNull Prioritized t, float epsilon) {
        return Util.equals(priElseNeg1(), t.priElseNeg1(), epsilon);
    }


//    static void normalizePriSum(@NotNull Iterable<? extends Prioritized> l, float total) {
//
//        float priSum = Prioritized.priSum(l);
//        float mult = total / priSum;
//        for (Prioritized b : l) {
//            b.priMult(mult);
//        }
//
//    }
//
//    /**
//     * randomly selects an item from a collection, weighted by priority
//     */
//    static <P extends Prioritized> P selectRandomByPriority(@NotNull NAR memory, @NotNull Iterable<P> c) {
//        float totalPriority = priSum(c);
//
//        if (totalPriority == 0) return null;
//
//        float r = memory.random.nextFloat() * totalPriority;
//
//        P s = null;
//        for (P i : c) {
//            s = i;
//            r -= s.priSafe(0);
//            if (r < 0)
//                return s;
//        }
//
//        return s;
//
//    }

}
