package nars.concept.dynamic;

import nars.$;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.term.Compound;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static nars.Op.CONJ;
import static nars.time.Tense.ETERNAL;

/**
 * Created by me on 12/4/16.
 */
public final class DynTruth implements Truthed {

    @Nullable public final List<Task> e;
    public Truthed truth;

    public float freq;
    public float conf; //running product

    public DynTruth(List<Task> e) {
        //this.t = t;
        this.e = e;
        this.truth = null;
    }

    public void setTruth(Truthed truth) {
        this.truth = truth;
    }

    @Nullable
    public Budget budget() {
        //RawBudget b = new RawBudget();
        int s = e.size();
        assert (s > 0);

        if (s > 1) {
            float f = 1f / s;
            //            for (Task x : e) {
            //                BudgetMerge.plusBlend.apply(b, x.budget(), f);
            //            }
            //            return b;
            return BudgetFunctions.fund(e, f);
        } else {
            return e.get(0).budget().clone();
        }
    }

    @Nullable
    public long[] evidence() {

        //return e == null ? null :
        return Stamp.zip(e);
    }

    @Override
    @Nullable
    public Truth truth() {
        return conf <= 0 ? null : $.t(freq, conf);
    }


    @Override
    public String toString() {
        return truth().toString();
    }

    @Nullable public DynamicBeliefTask task(@NotNull Compound template, boolean beliefOrGoal, long cre, long start, @Nullable Budget b) {

        Budget budget = b != null ? b : budget();
        if (budget == null || budget.isDeleted())
            return null;

        Truth tr = truth();
        if (tr == null)
            return null;

        long dur = (start!=ETERNAL && template.op() == CONJ) ? template.dtRange() : 0;

        DynamicBeliefTask dyn = new DynamicBeliefTask(template, beliefOrGoal ? Op.BELIEF : Op.GOAL,
                tr, cre, start, start + dur, evidence());
        dyn.setBudget( budget );
        if (Param.DEBUG)
            dyn.log("Dynamic");

        return dyn;
    }
}
