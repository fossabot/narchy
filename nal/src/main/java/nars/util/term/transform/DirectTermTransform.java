package nars.util.term.transform;

import jcog.memoize.QuickMemoize;
import nars.Op;
import nars.subterm.util.TermList;
import nars.term.Term;
import nars.util.term.TermBuilder;
import nars.util.term.builder.HeapTermBuilder;
import org.eclipse.collections.api.tuple.Pair;

import static nars.util.time.Tense.DTERNAL;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/** bypasses interning and */
public interface DirectTermTransform extends TermTransform {

    @Override
    default Term the(Op op, int dt, TermList t) {
        return Op.terms.compoundInstance(op, dt, t.arrayShared()); //bypass
    }

    class CachedDirectTermTransform implements DirectTermTransform {
        /** stores constructed Anon's locally, thread-local */
        final QuickMemoize<Pair<Op,TermList>,Term> localIntern;
        final TermBuilder localBuilder = new HeapTermBuilder(); //ensure no global interning

        public CachedDirectTermTransform(int capacity) {
            this.localIntern = new QuickMemoize<>(capacity, (ot) ->
                localBuilder.compound(ot.getOne(), DTERNAL,
                        ot.getTwo().arraySharedKeep()) //keep the items in the TermList with arraySharedKeep
            );
        }


        @Override
        public Term the(Op op, int dt, TermList t) {
            if (dt == DTERNAL)
                return localIntern.apply(pair(op, t)); //interned locally
            else
                return localBuilder.compound(op, dt, t.arrayShared()); //t is disposable, allow to remove the TermList items
        }

        public void resize(int s) {
            localIntern.resize(s);
        }
    }

}
