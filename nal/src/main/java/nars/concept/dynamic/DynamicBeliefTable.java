package nars.concept.dynamic;

import jcog.list.FasterList;
import jcog.pri.Priority;
import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.control.Activate;
import nars.table.DefaultBeliefTable;
import nars.table.TemporalBeliefTable;
import nars.task.NALTask;
import nars.term.Compound;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class DynamicBeliefTable extends DefaultBeliefTable {

    final DynamicTruthModel model;
    private final boolean beliefOrGoal;
    private final Compound term;

    //static final boolean rejectDerivations = false;

    @Override
    public void add(@NotNull Task input, @NotNull TaskConcept concept, @NotNull NAR nar) {
        if (!input.isInput()) {
            Activate.activate(input,
                    input.priElseZero() * 0.5f /* since it isnt being stored, we simulate the effect of the task already being present which would otherwise reduce the activation fired */, concept, nar); //activate only; dont process
            return;
        }
//        if (input instanceof AnswerTask) {
//            return; //dont insert its own dynamic belief task, causing a feedback loop
//        }

        super.add(input, concept, nar);
    }

    public DynamicBeliefTable(Compound c, TemporalBeliefTable t, DynamicTruthModel model, boolean beliefOrGoal) {
        super(t);
        this.term = c;
        this.model = model;
        this.beliefOrGoal = beliefOrGoal;
    }


    public NALTask generate( @NotNull Compound template, long when, NAR nar) {
        return generate(template, when, null, nar);
    }


    @Nullable
    public NALTask generate(@NotNull Compound template, long when, @Nullable Priority b, NAR nar) {

        DynTruth yy = truth(when,  template, true, nar);
        if (yy == null)
            return null;

        //compute the optimistic temporal union of the component's occurrences
        long start = Long.MAX_VALUE;
        long end = Long.MIN_VALUE;
        @Nullable FasterList<Task> ee = yy.e;
        if (ee == null || ee.isEmpty())
            return null;

        for (int i = 0, e1Size = ee.size(); i < e1Size; i++) {
            Task x = ee.get(i);
            long s = x.start();
            long e = x.end();
            if (s < start) start = s;
            if (e > end) end = e;
        }

        return yy.task(template, beliefOrGoal, nar.time(), start, end, b, nar);
    }

    @Override
    public Truth truth(long when, NAR nar) {
        DynTruth d = truth(when, term, false, nar);
        return Truth.maxConf(d != null ? d.truth() : null,
                super.truth(when, nar) /* includes only non-dynamic beliefs */);
    }




    @Nullable
    public DynTruth truth(long when, @NotNull Compound template, boolean evidence, NAR nar) {
        return model.eval(template, beliefOrGoal, when, evidence, nar); //newDyn(evidence);
    }

    @Override
    public Task match(long when, @Nullable Task target, Compound template, boolean noOverlap, NAR nar) {
        if (isEmpty())
            return null;

        if (template == null) {
            if (target!=null) {
                template = target.term();

            }
            if (template == null) {
                //HACK use the first held task
                try {
                    Task first = iterator().next();
                    template = first.term();
                } catch (NullPointerException e) {
                    return null;
                }
            }
        }

        template = nar.terms.retemporalize(template,
                target==null || target.isEternal() ?
                nar.terms.retemporalizationDTERNAL : nar.terms.retemporalizationZero); //TODO move this somewhere else where it can use the NAR's index
        if (template == null)
            return null; //??

        Task y = generate(template, when, nar);

        Task x = super.match(when, target, template, noOverlap, nar);

        if (x == null) return y;
        if (y == null || x.equals(y)) return x;

//        //choose the non-overlapping one
//        if (noOverlap && target != null) {
//            if (Stamp.overlapping(y, target))
//                return x;
//            if (Stamp.overlapping(x, target))
//                return y;
//        }

        //choose higher confidence
        int dur = nar.dur();
        float xc = x.evi(when, dur);
        float yc = y.evi(when, dur);
        //if (!Util.equals(xc, yc, TRUTH_EPSILON)) {
        return xc >= yc ? x : y;
        //}
//
//        //choose based on originality (includes cyclic), but by default prefer the existing task not the dynamic one
//        return (x.originality() >= y.originality()) ? x : y;
    }
}
