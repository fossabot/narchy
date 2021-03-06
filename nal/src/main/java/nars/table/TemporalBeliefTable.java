package nars.table;

import jcog.Skill;
import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.task.Revision;
import nars.task.signal.SignalTask;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.set.primitive.ImmutableLongSet;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;


/** https://en.wikipedia.org/wiki/Compressed_sensing */
@Skill("Compressed_sensing")
public interface TemporalBeliefTable extends TaskTable {


    /**
     * range will be between 0 and 1
     * should return non-negative values only
     */
    static float value(Task t, long start, long end, long dur) {

//        float focusedEvi = Revision.eviInteg(t, start, end, dur);
        float absDistance =
                t.midDistanceTo((start + end) / 2L);
                //t.minDistanceTo(start, end) / ((float)dur);
                //t.maxDistanceTo(start, end);
        float ownEvi =
                Revision.eviInteg(t, t.start(), t.end(),
                        //1);
                        dur);
                //0;

        //the ownEvi * 0.5 might be somewhat of an analog of the HORIZON parameter, reducing the strength of old evidence in relation to newer
//        return (1 + focusedEvi) * (1 + ownEvi * 0.5f) / (1 + absDistance);
        return  ( ownEvi / (1 + (/*Math.log(1+*/absDistance/((float)dur))));
//        return focusedEvi;

//        if (t.isDeleted())
//            return Float.NEGATIVE_INFINITY;

        //return ((1+t.conf()) * (1+t.priElseZero())) / (1f + Math.abs((start+end)/2 - t.mid())/((float)dur));
        //return t.conf() / (1f + t.distanceTo(start, end)/((float)dur));
        //return (float) (t.conf() / (1f + Math.log(1f+t.distanceTo(start, end)/((float)dur))));
        //return t.conf() / (1f + t.distanceTo(start, end)/dur);

//
//        //float fdur = dur;
//        //float range = t.range();

//        if (focusedEvi > Float.MIN_NORMAL) {
//            return focusedEvi;
//        } else {
//            //rank out-of-range by distance divided by its evidence integral
//
//            return
//                    //-t.minDistanceTo(start, end)
//                    //-t.midDistanceTo(start, end) //TODO
//                    absDistance
//                    /
//                    +t.eviInteg(dur, t.start(), t.end());
//        }

//        if (dt == 0)
//            return ee; //full integral
//        else {
//            //reduce the full integration by a factor relating to the distance (ex: isoceles triangle below)
//            /*
//            -----------+
//                       |   .      .
//                       |                 .     .
//                       ^  - - - -  - - - - - - -
//                      end                      ? <- time point being sampled
//             */
//            return (float) Param.evi(ee, dt, dur);
//        }

                //t.evi() * (1/(1+t.minDistanceTo(start, end)/dur))
                //t.evi() * (1+Interval.intersectLength(start, end, t.start(), t.end()))

////                //t.conf(now, dur) *
////                //t.evi(now, dur) *
////                //* range == 0 ? 1f : (float) (1f + Math.sqrt(t.range()) / dur); ///(1+t.distanceTo(start, end)))); ///fdur

//        float fdur = dur;
//        return
//                //(1f + t.evi()) *
//                //(t.evi(start,end,dur))
//                (t.conf(start,end,dur))
//                * (float)Math.sqrt(1f + t.range()/fdur) //boost for duration
//                ;
//
//                //(1f + t.evi()) * //raw because time is considered below. this covers cases where the task eternalizes
//                //(1f / (1 + t.distanceTo(start, end)/fdur));
//
//                //(1f + t.conf()) * //raw because time is considered below. this covers cases where the task eternalizes
//                //t.evi(start,end,dur) *
//                //t.conf(now, dur) *
//                //t.evi(now, dur) *
//                /* ((float)Math.sqrt(1+t.range()/fdur)) */
//                 //1 / ((1 + t.distanceTo(start, end)/fdur));
    }

    static FloatFunction<TaskRegion> mergabilityWith(Task x, float tableDur) {
        ImmutableLongSet xStamp = Stamp.toSet(x);

        long xStart = x.start();
        long xEnd = x.end();

        //how important the weak confidence is
        float weakConfFactor = 0.25f;

        return (TaskRegion _y) -> {
            Task y = (Task) _y;

            if (Stamp.overlapsAny(xStamp, y.stamp()))
                return Float.NaN;

            //float freqCoherence = 1 - Math.abs(x.freq() - y.freq());
            return
                    (1f / (1f +
                            (Math.abs(y.start() - xStart) + Math.abs(y.end() - xEnd))/tableDur))
                    //* (1f + weakConfFactor * (1f - y.conf())) //prefer weak
            ;
        };
    }

    static float costDtDiff(Term template, Task x, int dur) {
        return 1f + Revision.dtDiff(template, x.term()) / (dur * dur);
    }

    /** finds or generates the strongest match to the specified parameters.
     * Task against is an optional argument which can be used to compare internal temporal dt structure for similarity */
    Task match(long start, long end, @Nullable Term template, EternalTable eternals, NAR nar, Predicate<Task> filter);

    /** estimates the truth value for the provided time.
     * the eternal table's top value, if existent, contributes a 'background'
     * level in interpolation.
     * */
    Truth truth(long start, long end, EternalTable eternal, Term template, int dur);


    void setCapacity(int temporals);


    void update(SignalTask x, Runnable change);

    TemporalBeliefTable Empty = new TemporalBeliefTable() {

        @Override
        public boolean add(Task t, TaskConcept c, NAR n) {
            return false;
        }

        @Override
        public void update(SignalTask x, Runnable change) {

        }

        @Override
        public void setCapacity(int c) {

        }

        @Override
        public int capacity() {
            //throw new UnsupportedOperationException();
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean removeTask(Task x) {
            return false;
        }

        @Override
        public int size() {
            return 0;
        }


        @Override
        public Stream<Task> streamTasks() {
            return Stream.empty();
        }

           @Override
        public void forEachTask(Consumer<? super Task> x) {

        }

        @Override
        public void whileEach(Predicate<? super Task> each) {

        }

        @Override
        public void whileEach(long minT, long maxT, Predicate<? super Task> x) {

        }

        @Override
        public Task match(long start, long end, @Nullable Term template, EternalTable ete, NAR nar, Predicate<Task> filter) {
            return null;
        }

        @Override
        public Truth truth(long start, long end, EternalTable eternal, Term template, int dur) {
            return null;
        }

        @Override
        public void clear() {

        }
    };


    void whileEach(Predicate<? super Task> each);

    /** finds all temporally intersectnig tasks.  minT and maxT inclusive.  while the predicate remains true, it will continue scanning */
    default void whileEach(long minT, long maxT, Predicate<? super Task> each) {
        whileEach(x -> {
            if (!x.isDeleted() && x.intersects(minT, maxT))
                return each.test(x);
            else
                return true; //continue
        });
    }
}
