package nars.unify.constraint;

import nars.Op;
import nars.term.Term;
import nars.term.var.Variable;

import java.util.function.Predicate;

/**
 * containment test of x to y's subterms and y to x's subterms
 */
public final class NoCommonSubtermConstraint extends RelationConstraint {

    public final boolean recurse;

    /**
     * @param recurse true: recursive
     *                false: only cross-compares the first layer of subterms.
     */
    public NoCommonSubtermConstraint(Term target, Term x, boolean recurse) {
        super(target, x, recurse ? "neqRCom" : "neqCom");
        this.recurse = recurse;
    }


    @Override
    public float cost() {
        return recurse ? 1.5f : 1f;
    }

    @Override
    public boolean invalid(Term x, Term y) {

        return isSubtermOfTheOther(x, y, recurse, true);
    }


    final static Predicate<Term> limit =
            Op.recursiveCommonalityDelimeterWeak;


    static boolean isSubtermOfTheOther(Term a, Term b, boolean recurse, boolean excludeVariables) {

        if ((excludeVariables) && (a instanceof Variable || b instanceof Variable))
            return false;

        int av = a.volume();
        int bv = b.volume();
        if (av == bv) {
//            boolean invalid = recurse ?
//                    a.containsRecursively(b, true, limit) ||
//                            b.containsRecursively(a, true, limit) :
//
//                    a.containsRoot(b) || b.containsRoot(a);
//            if (invalid)
//                throw new RuntimeException("wtf");
//            return invalid;
            return false; //impossible for either to contain the other
        } else {
            //if one volume is smaller than the other we only need to test containment unidirectionally

            if (av < bv) {
                //swap
                Term c = a;
                a = b;
                b = c;
            }

            return recurse ?
                    a.containsRecursively(b, true, limit) :
                    a.containsRoot(b);
        }
    }
    //commonSubtermsRecurse((Compound) B, C, true, new HashSet())
    //commonSubterms((Compound) B, C, true, scratch.get())


}
