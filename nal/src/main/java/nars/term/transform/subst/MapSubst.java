package nars.term.transform.subst;

import nars.term.Term;
import nars.util.version.Versioned;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Created by me on 12/3/15.
 */
public final class MapSubst implements Subst {

    public final Map<Term, Term> xy;

//    /**
//     * creates a substitution of one variable; more efficient than supplying a Map
//     */
//    public MapSubst(Term termFrom, Term termTo) {
//        this(UnifiedMap.newWithKeysValues(termFrom, termTo));
//    }


    public MapSubst(Map<Term, Term> xy) {
        this.xy = xy;
    }

    @Override
    public void clear() {
        xy.clear();
    }

    @Override
    public boolean isEmpty() {
        return xy.isEmpty();
    }

    /**
     * gets the substitute
     * @param t
     */
    @Override
    public final Term term(Term t) {
        return xy.get(t);
    }

    @Override public void forEach(@NotNull BiConsumer<? super Term, ? super Term> each) {
        if (xy.isEmpty()) return;
        xy.forEach(each);
    }


    @NotNull
    @Override
    public String toString() {
        return "Substitution{" +
                "subs=" + xy +
                '}';
    }


}
