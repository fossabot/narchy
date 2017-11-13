package nars.derive.mutate;

import nars.$;
import nars.term.container.ShuffledSubterms;
import nars.term.container.TermContainer;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 12/22/15.
 */
public final class CommutivePermutations extends Termutator.AbstractTermutator {

    @NotNull
    private final TermContainer y;
    private final TermContainer x;

    /**
     * important note: using raw Set<Term> here to avoid the clobbering of PatternCompound subterms if interned with current impl
     * x and y must have same size
     */
    public CommutivePermutations(TermContainer x, TermContainer y) {
        super($.p(x), $.p(y));
        assert(x.subs()==y.subs());
        this.y = y;
        this.x = x;
    }

    @Override
    public int getEstimatedPermutations() {
        throw new UnsupportedOperationException();
        //return perm.total();
    }

    @Override
    public void mutate(Unify f, Termutator[] chain, int current) {
        int start = f.now();

        ShuffledSubterms p = new ShuffledSubterms(x, f.random);


        while (p.hasNextThenNext()) {

            if (p.unifyLinear(y, f)) {
                f.tryMutate(chain, current);
            }

            if (!f.revertLive(start))
                break;
        }


    }


}
