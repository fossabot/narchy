package nars.budget.policy;

import nars.concept.AbstractConcept;
import nars.concept.CompoundConcept;
import nars.util.data.MutableInteger;

/**
 * Created by me on 5/11/16.
 */
public class DefaultConceptPolicy implements ConceptPolicy {

    public final MutableInteger beliefsMaxEteOrTemp, goalsMaxEteOrTemp;
    public final MutableInteger questionsMax;
    public final MutableInteger termlinksCapacity, taskLinksCapacity;

    public DefaultConceptPolicy(int beliefsCapTotal, int goalsCapTotal, int questionsMax, int termlinksCapacity, int taskLinksCapacity) {
        this(new MutableInteger(Math.max(1, beliefsCapTotal / 2)),
                new MutableInteger(Math.max(1, goalsCapTotal / 2)),
                new MutableInteger(questionsMax),
                new MutableInteger(termlinksCapacity),
                new MutableInteger(taskLinksCapacity)
        );
    }

    public DefaultConceptPolicy(MutableInteger beliefsMaxEteOrTemp, MutableInteger goalsMaxEteOrTemp, MutableInteger questionsMax, MutableInteger termlinksCapacity, MutableInteger taskLinksCapacity) {
        this.beliefsMaxEteOrTemp = beliefsMaxEteOrTemp;
        this.goalsMaxEteOrTemp = goalsMaxEteOrTemp;
        this.questionsMax = questionsMax;
        this.termlinksCapacity = termlinksCapacity;
        this.taskLinksCapacity = taskLinksCapacity;
    }

    /** no eternal; use allocated eternal capacity added to temporals */
    public static void beliefCapacityNonEternal(CompoundConcept c, ConceptPolicy p) {
        c.beliefs().capacity(0, p.beliefCap(c, true, true) + p.beliefCap(c, true, false));
        c.goals().capacity(0, p.beliefCap(c, false, true) + p.beliefCap(c, false, false));
    }

    @Override
    public int beliefCap(CompoundConcept compoundConcept, boolean beliefOrGoal, boolean eternalOrTemporal) {
        if (beliefOrGoal) {
            return beliefsMaxEteOrTemp.intValue();
        } else {
            return goalsMaxEteOrTemp.intValue();
        }
    }

    @Override
    public int linkCap(AbstractConcept compoundConcept, boolean termOrTask) {
        return termOrTask ? termlinksCapacity.intValue() : taskLinksCapacity.intValue();
    }


//        public int getBeliefsCapacity(Termed t);
//        public int getGoalsCapabity(Termed t);
//        public int getTermLinksCapacity(Termed t);
//        public int getTaskLinksCapacity(Termed t);
}
