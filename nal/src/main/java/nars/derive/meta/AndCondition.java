package nars.derive.meta;

import com.google.common.collect.Lists;
import nars.Op;
import nars.premise.Derivation;
import nars.term.compound.GenericCompound;
import nars.term.container.TermVector;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Created by me on 12/31/15.
 */
public final class AndCondition extends GenericCompound implements BoolCondition {

    @NotNull
    public final BoolCondition[] termCache;

    /*public AndCondition(@NotNull BooleanCondition<C>[] p) {
        this(TermVector.the((Term[])p));
    }*/
    public AndCondition(@NotNull Collection<BoolCondition> p) {
        super(Op.PROD, TermVector.the(p));
        this.termCache = p.toArray(new BoolCondition[p.size()]);
        if (termCache.length < 2)
            throw new RuntimeException("unnecessary use of AndCondition");
    }


//    /** just attempts to evaluate the condition, causing any desired side effects as a result */
//    @Override public final void accept(@NotNull PremiseEval m, int now) {
//        booleanValueOf(m, now);
////        m.revert(now);
//    }

    @Override
    public final boolean run(@NotNull Derivation m) {
        boolean result = true;
        int start = m.now();
        for (BoolCondition x : termCache) {
            boolean b = x.run(m);
            if (!b) {
                result = false;
                break;
            }
        }

        m.revert(start);
        return result;
    }

    public static @Nullable BoolCondition the(@NotNull List<BoolCondition> cond) {

        //remove suffix 'TRUE'
        int s = cond.size();
        if (s == 0) return null;


//        if (cond.get(s - 1) == BoolCondition.TRUE) {
//            cond = cond.subList(0, s - 1);
//            s--;
//            if (s == 0) return null;
//        }


        if (s == 1) return cond.get(0);
        return new AndCondition(cond);
    }

    public @Nullable BoolCondition without(BoolCondition condition) {
        //TODO returns a new AndCondition with condition removed, or null if it was the only item
        BoolCondition[] x = ArrayUtils.removeElement(termCache, condition);
        if (x.length == termCache.length)
            throw new RuntimeException("element missing for removal");

        return AndCondition.the(Lists.newArrayList(x));
    }
}