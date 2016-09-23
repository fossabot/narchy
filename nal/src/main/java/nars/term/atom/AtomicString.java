package nars.term.atom;

import nars.Op;
import nars.term.Term;
import nars.term.subst.FindSubst;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 1/1/16.
 */
public abstract class AtomicString implements Atomic {

    /** Assumes that the op()
     *  is encoded within its string such that additional op()
     *  comparison would be redundant. */
    @Override public boolean equals(Object u) {

        return  (u instanceof Atomic) &&
                (
                    (this == u)
                        ||
                    (toString().equals(u.toString()) && (u instanceof AtomicString ? true : op() == ((Atomic) u).op()))
                );

    }


    @Override
    public int complexity() {
        return 1;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }


}
