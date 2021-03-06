package nars.unify.mutate;

import jcog.math.Combinations;
import nars.$;
import nars.subterm.ShuffledSubterms;
import nars.subterm.Subterms;
import nars.subterm.util.TermList;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.unify.Unify;
import nars.unify.match.Ellipsis;
import nars.unify.match.EllipsisMatch;
import org.apache.commons.lang3.ArrayUtils;

import java.util.SortedSet;

/**
 * Created by me on 12/22/15.
 */
public class Choose2 extends Termutator.AbstractTermutator {

    /*@NotNull*/
    final Combinations comb;
    //private final Term[] yFree;
    /*@NotNull*/
    private final Term[] x;
    /*@NotNull*/
    private final Ellipsis xEllipsis;
    /*@NotNull*/
    private final Unify f;
    /*@NotNull*/
    private final ShuffledSubterms yy;

    final static Atom CHOOSE_2 = $.the(Choose2.class);

    public Choose2(Ellipsis xEllipsis, Unify f, SortedSet<Term> x, SortedSet<Term> yFree) {
        this(xEllipsis, f,
                x.toArray(new Term[x.size()]),
                new TermList(yFree.toArray(new Term[yFree.size()])));
    }

    Choose2(Ellipsis xEllipsis, Unify f, Term[] x, TermList yFree) {
        super(CHOOSE_2, $.pFast(x), xEllipsis, $.pFast((Subterms)yFree));

        this.f = f;

        this.xEllipsis = xEllipsis;

        this.x = x;

        this.yy = new ShuffledSubterms(yFree, f.random  /*new ArrayTermVector(yFree)*/);

        this.comb = new Combinations(yFree.subs(), 2);
    }

    @Override
    public int getEstimatedPermutations() {
        return comb.getTotal()*2;
    }

    @Override
    public void mutate(Unify versioneds, Termutator[] chain, int current) {

        Combinations ccc = this.comb;
        ccc.reset();

        boolean phase = true;

        int start = f.now();
        ShuffledSubterms yy = this.yy;



        Ellipsis xEllipsis = this.xEllipsis;
        Unify f = this.f;
        Term[] x = this.x;

        int[] c = null;
        while (ccc.hasNext() || !phase) {

            c = phase ? ccc.next() : c;
            phase = !phase;

            byte c0 = (byte) c[0];
            byte c1 = (byte) c[1];
            ArrayUtils.reverse(c); //swap to try the reverse next iteration

            Term y1 = yy.sub(c0);

            if (x[0].unify(y1, f)) {

                Term y2 = yy.sub(c1);

                if (x[1].unify(y2, f) &&
                        xEllipsis.unify(EllipsisMatch.matchExcept(yy, c0, c1), f)) {

                    if (!f.tryMutate(chain, current))
                        break;
                }

            }

            if (!f.revertLive(start))
                break;
        }

    }

}
