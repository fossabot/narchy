package nars.util.term.builder;

import nars.Op;
import nars.The;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.util.term.InternedCompound;
import nars.util.term.InternedSubterms;
import nars.util.term.SubtermsCache;
import nars.util.term.TermCache;

import static nars.util.time.Tense.DTERNAL;
import static nars.util.time.Tense.XTERNAL;

/** can intern subterms, compounds, and temporal compounds.
 * the requirements for intern cache admission are configurable.
 **/
public class InterningTermBuilder extends HeapTermBuilder {

    //TODO Atom Cache

    public final TermCache termTemporalCache = new TermCache(128 * 1024, 3, false);
    public final TermCache termCache = new TermCache(128 * 1024, 3, false);
    public final SubtermsCache subtermCache = new SubtermsCache(128 * 1024, 3, false);

    @Override public final Term newCompound(Op op, int dt, Term[] u) {
        return internable(op, dt, u) ?
                (dt == DTERNAL ? termCache : termTemporalCache).apply(new InternedCompound(op, dt, u)) :
                super.newCompound(op, dt, u);
    }

    @Override public Subterms newSubterms(Term... s) {
        //return The.Subterms.the.apply(s);
        //return compound(PROD, s).subterms();
        if (internable(s))
            return subtermCache.apply(new InternedSubterms(s));
        else
            return super.newSubterms(s);
    }

    protected boolean internable(Op op, int dt, Term[] u) {
        return //op!=NEG &&
                internable(u);
    }

    protected boolean internable(Term[] subterms) {
        if (subterms.length == 0)
            return false;

        for (Term x : subterms) {
            if (!(x instanceof The)) {
                //HACK caching these interferes with unification.  instead fix unification then allow caching of these
                return false;
            }
            switch (x.dt()) {
                case DTERNAL:
                case 0:
                case XTERNAL:
                    break; //OK
                default:
                    return false; //specific dt: exclude temporal terms polluting the cache
            }
        }
        return true;
    }


    public String summary() {
        return  "subterm cache: " + subtermCache.summary() + "\n" +
                "compound cache: " + termCache.summary() + "\n" +
                "termporal cache: " + termTemporalCache.summary() + "\n"
                ;
    }
}
