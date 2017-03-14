package nars.premise;

import jcog.version.Versioned;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.derive.meta.BoolCondition;
import nars.derive.meta.constraint.MatchConstraint;
import nars.task.DerivedTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.Unify;
import nars.term.transform.substitute;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static nars.Op.NEG;
import static nars.Op.VAR_PATTERN;
import static nars.term.transform.substituteIfUnifies.*;
import static nars.time.Tense.DTERNAL;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 */
public class Derivation extends Unify {


    /**
     * the current premise being evaluated in this context TODO make private again
     */
    @NotNull
    public final Premise premise;

    // cached for fast access during derivation:
    public final float truthResolution;
    public final float quaMin;
    public final float premiseEvidence;
    public final float confMin;


    @NotNull
    public final Versioned<TruthPuncEvidence> punct;


    /**
     * current MatchTerm to receive matches at the end of the Termute chain; set prior to a complete match by the matchee
     */
    @NotNull
    private BoolCondition forEachMatch;


    /**
     * only continues termuting while matchesRemain > 0
     */
    int matchesRemain;

    final int matchesMax;


    /**
     * cached values
     */


    /** op ordinals: 0=task, 1=belief */
    public final byte termSub0op;
    public final byte termSub1op;

    /** op bits: 0=task, 1=belief */
    public final int termSub0opBit;
    public final int termSub1opBit;

    /** structs, 0=task, 1=belief */
    public final int termSub0Struct;
    public final int termSub1Struct;

    public final boolean overlap;

    @Nullable
    public final Truth taskTruth;
    @Nullable
    public final Truth beliefTruth;

    @NotNull
    public final Compound taskTerm;
    @NotNull
    public final Term beliefTerm;
    @NotNull
    public final NAR nar;

    @NotNull
    public final Task task;
    @Nullable
    public final Task belief;
    public final byte taskPunct;

    /**
     * whether the premise involves temporality that must be calculated upon derivation
     */
    public final boolean temporal;

    @Nullable
    private long[] evidenceDouble, evidenceSingle;

    @Nullable
    public final Consumer<DerivedTask> target;
    public final boolean cyclic;


    public Derivation(@NotNull NAR nar, @NotNull Premise p, @NotNull Consumer<DerivedTask> c,
                      int matchMax, int stack) {
        super(nar.concepts, VAR_PATTERN, nar.random, stack);

        this.matchesMax = matchMax;

        this.nar = nar;
        this.truthResolution = nar.truthResolution.floatValue();
        this.confMin = Math.max(truthResolution, nar.confMin.floatValue());
        this.quaMin = nar.quaMin.floatValue();


        //occDelta = new Versioned(this);
        //tDelta = new Versioned(this);
        this.punct = new Versioned<>(versioning, 2);

        set(new substitute(this));
        set(new substituteIfUnifiesAny(this));
        //set(new substituteIfUnifiesForward(this));
        set(new substituteIfUnifiesDep(this));
        //set(new substituteIfUnifiesIndep(this));

        this.premise = p;

        Task task;
        this.task = task = p.task;
        Compound tt = task.term();
        Term taskTerm = this.taskTerm = tt;

        Task belief;
        this.belief = belief = p.belief;
        this.beliefTerm = p.beliefTerm();
        if (beliefTerm.op()==NEG) {
            throw new RuntimeException("negated belief term");
        }

        this.taskTruth = task.truth();
        this.taskPunct = task.punc();
        this.beliefTruth = belief != null ? belief.truth() : null;


//        //normalize to positive truth
//        if (taskTruth != null && Global.INVERT_NEGATIVE_PREMISE_TASK && taskTruth.isNegative()) {
//            this.taskInverted = true;
//            this.taskTruth = this.taskTruth.negated();
//        } else {
//            this.taskInverted = false;
//        }
//
//        //normalize to positive truth
//        if (beliefTruth!=null && Global.INVERT_NEGATIVE_PREMISE_TASK && beliefTruth.isNegative()) {
//            this.beliefInverted = true;
//            this.beliefTruth = this.beliefTruth.negated();
//        } else {
//            this.beliefInverted = false;
//        }

        this.cyclic = task.cyclic(); //belief cyclic should not be considered because in single derivation its evidence will not be used any way
        //NOT: this.cyclic = task.cyclic() || (belief != null && belief.cyclic());

        this.overlap = belief != null && Stamp.overlapping(task, belief);

        this.termSub0Struct = taskTerm.structure();

        Op tOp = taskTerm.op();
        this.termSub0op = (byte) tOp.ordinal();
        this.termSub0opBit = tOp.bit;

        this.termSub1Struct = beliefTerm.structure();

        Op bOp = beliefTerm.op();
        this.termSub1op = (byte) bOp.ordinal();
        this.termSub1opBit = bOp.bit;

        this.temporal = temporal(task, belief);

        this.target = c;

        float premiseEvidence = task.isBeliefOrGoal() ? task.evi() : 0;
        if (belief!=null)
            premiseEvidence += belief.evi();
        this.premiseEvidence = premiseEvidence;

    }


    protected final void set(@NotNull Term t) {
        setXY(t, t);
    }

    /**
     * only one thread should be in here at a time
     */
    public final void matchAll(@NotNull Term x, @NotNull Term y, @Nullable BoolCondition eachMatch, @Nullable MatchConstraint constraints) {

        int t = now();

        if (constraints != null) {
            if (this.constraints.set(constraints)==null)
                throw new RuntimeException("constraints not set");
        }

        boolean finish = eachMatch!=null;
        this.forEachMatch = eachMatch; //to notify of matches
        matchesRemain = matchesMax;

        unify(x, y, !finish, finish);

        this.forEachMatch = null;

        if (finish) {
            versioning.revert(t);
        } //else: allows the set constraints to continue

    }

    @Override public final boolean onMatch() {
        return  (--matchesRemain > 0) && forEachMatch.run(this);
    }


    private static boolean temporal(@NotNull Task task, @Nullable Task belief) {
        if (!task.isEternal() || task.dt() != DTERNAL)
            return true;

        return belief != null && (!belief.isEternal() || belief.dt() != DTERNAL);
    }







    /**
     * gets the op of the (top-level) pattern being compared
     *
     * @param subterm 0 or 1, indicating task or belief
     */
    /*public final boolean subTermIs(int subterm, int op) {
        return (subterm==0 ? termSub0op : termSub1op) == op;
    }*/
    public final int subOp(int i /* 0 or 1 */) {
        return (i == 0 ? termSub0op : termSub1op);
    }


    public void replaceAllXY(@NotNull Unify m) {
        m.xy.forEachVersioned(this::replaceXY);
    }


    @Nullable
    public long[] evidenceSingle() {
        if (evidenceSingle == null) {
            evidenceSingle = Stamp.cyclic(task.stamp());
        }
        return evidenceSingle;
    }

    @Nullable
    public long[] evidenceDouble() {
        if (evidenceDouble == null) {
            evidenceDouble = Stamp.zip(task.stamp(), belief.stamp());
//            if ((task.cyclic() || belief.cyclic()) && Stamp.isCyclic(evidenceDouble))
//                throw new RuntimeException("cyclic should not be propagated");
            //System.out.println(Arrays.toString(task.evidence()) + " " + Arrays.toString(belief.evidence()) + " -> " + Arrays.toString(evidenceDouble));
        }

        return evidenceDouble;
    }

    public boolean overlap(boolean single) {
        return single ? cyclic : overlap;
    }
}


