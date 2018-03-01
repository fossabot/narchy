package nars.control;

import jcog.Texts;
import jcog.util.AtomicFloat;

/** concurrent traffic accumulator;
 *  concurrent updates (ie. add) but expects a synchronous commit
 *  to sample after each cycle.
 *
 *  the AtomicFloat which this subclasses holds the accumulating value
 *  that safely supports multiple concurrent accumulators */
public class Traffic extends AtomicFloat {

//    /** previous value */
//    public float prev;

    /** current value */
    public volatile float current;

    public volatile double total;

    public final void commit() {
        zero((cur)->{
//          this.prev = this.current;
            this.total += (this.current = cur);
        });
    }

    @Override
    public String toString() {
        return Texts.n4(current) + "/" + Texts.n4(total);
    }
}
