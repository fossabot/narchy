package nars.op;

import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

import static nars.Op.Null;


/**
 * if STRICT is 4th argument, then there will only be a valid result
 * if the input has changed (not if nothing changed, and not if the attempted change had no effect)
 */
public class Subst extends Functor {

    //TODO use special symbol encoding to avoid collision with equivalent normal input
    final static Term STRICT = Atomic.the("strict");
    final static Term FORCE = Atomic.the("force");

    public static final Subst replace = new Subst("replace");

    protected Subst(String id) {
        this((Atom)Atomic.the(id));
    }
    protected Subst(Atom id) {
        super(id);
    }

    @Nullable @Override public Term apply( Subterms xx) {

        final Term input = xx.sub(0); //term to possibly transform

        final Term x = xx.sub(1); //original term (x)

        final Term y = xx.sub(2); //replacement term (y)

        return apply(xx, input, x, y);
    }

    public @Nullable Term apply(Subterms xx, Term input, Term x, Term y) {
        Term result = input.replace(x, y);
        if (xx.subEquals(3, STRICT) && input.equals(result))
            return Null;

//        if (!(result instanceof Bool && !result.equals(input))) {
////            //add mapping in parent
////            if (!onChange(input, x, y, result))
////                return Null;
//        }

        return result;
    }

//    /** called if substitution was successful */
//    protected boolean onChange(Term from, Term x, Term y, Term to) {
//        return true;
//    }

}
