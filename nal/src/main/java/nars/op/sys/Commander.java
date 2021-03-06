//package nars.op.sys;
//
//import com.google.common.collect.Iterators;
//import nars.NAR;
//import nars.bag.BLink;
//import nars.concept.Concept;
//import nars.nal.Tense;
//import nars.task.Task;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.Iterator;
//import java.util.LinkedHashSet;
//import java.util.function.Consumer;
//import java.util.function.Supplier;
//
//
///**
// * Captures input goals and questions into a buffer
// * (ie. input by user) and re-processes them in a
// * controllable pattern, frequency,
// * and priority -- in addition to ordinary system activity.
// *
// * This guides inference according to the explicit
// * inputs that were input, focusing it towards those
// * outcomes.
// *
// * Analogous to a continuous echo/delay effect,
// * or a sustain effecct.
// */
//public class Commander implements Consumer<NAR>, Supplier<Concept> {
//
//    public final ItemAccumulator<Task> commands;
//    @NotNull
//    public final Iterator<BLink<Task>> commandIterator;
//    public final LinkedHashSet<Concept> concepts = new LinkedHashSet();
//    final Iterator<Concept> conceptsIterator = Iterators.cycle(concepts);
//
//
////    private final On cycleEnd;
////    private final NAR nar;
//
//    /** how far away from the occurence time of a temporal belief before it is Deleted */
//    private final int maxTemporalBeliefAge;
//    private final int maxTemporalBeliefDurations = 16 /* should be tuned */;
//    @NotNull
//    private final NAR nar;
//
//    int inputsPerFrame = 2;
//    int cycleDivisor = 3;
//
////    float priorityPerCycle = 1,
////            priorityRemaining = 0; //change left over from last cycle
//
//
//
//    public Commander(@NotNull NAR nar, ItemAccumulator<Task> buffer) {
//
//        this.nar = nar;
//
//        //TODO reset event
//        //this.cycleEnd = active ?
//                nar.eventFrameStart.on(this);
//                //: null;
//
//        commands = buffer;
//        commandIterator = Iterators.cycle(commands.bag());
//
//
//        maxTemporalBeliefAge = maxTemporalBeliefDurations;
//
//
//        nar.eventInput.on((tp) -> {
//            Task t = tp.task();
//            if (t.isInput() && !commands.bag().contains(t))
//                input(t);
//        });
//    }
//
//
////    @Override
////    public void setActive(boolean b) {
////        super.setActive(b);
////        if (!b) {
////            commands.clear();
////        }
////    }
//
//
//    protected void input(@NotNull Task t) {
//        if (/*(t.isGoal() || t.isQuestOrQuestion()) && */ t.isInput()) {
//            commands.bag().put(t);
//        }
//    }
//
//
//    @Override
//    public void accept(@NotNull NAR nar) {
//
//        //TODO iterate tasks until allotted priority has been reached,
//        //  TaskProcess each
//
//        int cs = commands.bag().size();
//        if (cs == 0) return;
//
//
//        long now = nar.time();
//        if (now%cycleDivisor!= 0) return;
//
//        Iterator<BLink<Task>> commandIterator = this.commandIterator;
//        for (int i = 0; i < inputsPerFrame; i++) {
//            if (commandIterator.hasNext()) {
//                Task next = commandIterator.next().get();
//                if (valid(now, next)) {
//                    /*Concept c = */nar.input(next);
//                    Concept c = nar.concept( next );
//                    if (c!=null) {
//                        concepts.add(c);
//                        //TODO add recursive components?
//                    }
//                }
//                else
//                    commandIterator.remove();
//            }
//        }
//
//    }
//
//    public final boolean valid(long now, @NotNull Task t) {
//
//        if (t.budget().isDeleted())
//            return false;
//
//        if (!Tense.isEternal(t.occurrence())) {
//            long age = Math.abs( now - t.occurrence() );
//            if (age > maxTemporalBeliefAge)
//                return false;
//        }
//
//        return true;
//    }
//
//    @Override
//    public Concept get() {
//        return conceptsIterator.next();
//    }
//
//    public boolean isEmpty() {
//        return this.commands.bag().isEmpty();
//    }
//
//    public int size() {
//        return this.commands.bag().size();
//    }
//
//    //TODO getBufferPrioritySum
//    //TODO setPriorityPerCycle
//    //TODO max tasks limit
//    //TODO rebudgeting Function<Budget,Budget> for manipulating values
//    //add with TTL?
//}
