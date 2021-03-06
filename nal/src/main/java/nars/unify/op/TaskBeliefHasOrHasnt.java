package nars.unify.op;

import nars.$;
import nars.Op;
import nars.derive.premise.PreDerivation;
import nars.term.control.AbstractPred;


/**
 * Created by me on 5/19/17.
 */
public final class TaskBeliefHasOrHasnt extends AbstractPred<PreDerivation> {
    private final int structure;
    private final boolean task;
    private final boolean belief;
    private final boolean includeOrExclude;

    public TaskBeliefHasOrHasnt(Op o, boolean testTask, boolean testBelief, boolean includeExclude) {
        this(o.bit, testTask, testBelief, includeExclude);
    }
    public TaskBeliefHasOrHasnt(int structure, boolean testTask, boolean testBelief, boolean includeExclude) {
        super($.func((includeExclude ? "OpHas" : "OpHasNot"), $.the(structure), $.the(testTask ? 1 : 0), $.the(testBelief ? 1 : 0)));
        assert(testTask || testBelief);
        this.structure = structure;
        this.task = testTask;
        this.belief = testBelief;
        this.includeOrExclude = includeExclude;
    }

    @Override
    public boolean test(PreDerivation derivation) {
        if (includeOrExclude) {
               return
                   (!task || ((derivation._taskStruct & structure) > 0))
                        &&
                   (!belief || ((derivation._beliefStruct & structure) > 0));
        } else {
                return
                    (!task || ((derivation._taskStruct & structure) == 0))
                        &&
                    (!belief || ((derivation._beliefStruct & structure) == 0));
        }
    }

    @Override
    public float cost() {
        return 0.1f;
    }
}

