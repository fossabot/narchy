package nars.concept.table;

import nars.NAR;
import nars.bag.impl.SortedListTable;
import nars.task.Revision;
import nars.task.RevisionTask;
import nars.task.Task;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static nars.nal.Tense.ETERNAL;

/**
 * Created by me on 5/7/16.
 */
public class EternalTable extends SortedListTable<Task, Task> {

    public EternalTable(Map<Task, Task> index, int initialCapacity) {
        super(Task[]::new, index);
        setCapacity(initialCapacity);
    }

    @Override
    protected final void removeWeakest(Object reason) {
        remove(weakest()).delete(reason);
    }

    @Nullable
    @Override
    public final Task key(Task task) {
        return task;
    }

    @Override
    public final int compare(@NotNull Task o1, @NotNull Task o2) {
        float f1 = BeliefTable.rankEternalByOriginality(o2); //reversed
        float f2 = BeliefTable.rankEternalByOriginality(o1);
        if (f1 < f2)
            return -1;
        if (f1 > f2)
            return 1;
        return 0;
    }

    @Nullable
    public /*Revision*/Task tryRevision(@NotNull Task newBelief, @NotNull NAR nar) {

        int bsize = size();
        if (bsize == 0)
            return null; //nothing to revise with

        //Try to select a best revision partner from existing beliefs:
        Task oldBelief = null;
        float bestRank = 0f, bestConf = 0f;
        Truth conclusion = null;
        final float newBeliefConf = newBelief.conf();
        Truth newBeliefTruth = newBelief.truth();

        for (int i = 0; i < bsize; i++) {
            Task x = get(i);

            if (!Revision.isRevisible(newBelief, x))
                continue;


//
//            float factor = tRel * freqMatch;
//            if (factor < best) {
//                //even with conf=1.0 it wouldnt be enough to exceed existing best match
//                continue;
//            }

            //            float minValidConf = Math.min(newBeliefConf, x.conf());
//            if (minValidConf < bestConf) continue;
//            float minValidRank = BeliefTable.rankEternalByOriginality(minValidConf, totalEvidence);
//            if (minValidRank < bestRank) continue;

            Truth oldBeliefTruth = x.truth();

            Truth c = Revision.revisionEternal(newBeliefTruth, oldBeliefTruth, 1f, bestConf);
            if (c == null)
                continue;

            //avoid a duplicate truth
            if (c.equals(oldBeliefTruth) || c.equals(newBeliefTruth))
                continue;

            float cconf = c.conf();
            final int totalEvidence = 1; //newBelief.evidence().length + x.evidence().length; //newBelief.evidence().length + x.evidence().length;
            float rank = BeliefTable.rankEternalByOriginality(cconf, totalEvidence);

            if (rank > bestRank) {
                bestRank = rank;
                bestConf = cconf;
                oldBelief = x;
                conclusion = c;
            }
        }

        return oldBelief != null ?
                new RevisionTask(
                    Revision.intermpolate(
                        newBelief, oldBelief,
                        newBeliefConf, oldBelief.conf()
                    ),
                    newBelief, oldBelief,
                    conclusion,
                    nar.time(), ETERNAL).
                        budget(oldBelief, newBelief).
                        log("Insertion Revision")
                :
                null;
    }

    public final Truth truth() {
        Task s = top();
        return s!=null ? s.truth() : null;
    }
}
