//package nars.concept;
//
//import nars.bag.Bag;
//import nars.concept.table.BeliefTable;
//import nars.concept.table.QuestionTable;
//import nars.task.Task;
//import nars.term.Compound;
//import nars.term.Termed;
//import org.jetbrains.annotations.NotNull;
//
///**
// * will be removed soon
// */
//@Deprecated public final class NegationConcept extends CompoundConcept {
//
//    public NegationConcept(@NotNull Compound term, Bag<Termed> termLinks, Bag<Task> taskLinks) {
//        super(term, termLinks, taskLinks);
//        throw new RuntimeException("REDUNDANT");
//    }
//
//    @Override
//    protected @NotNull BeliefTable newBeliefTable() {
//        return BeliefTable.EMPTY;
//    }
//
//    @Override
//    protected @NotNull BeliefTable newGoalTable() {
//        return BeliefTable.EMPTY;
//    }
//
//    @Override
//    protected @NotNull QuestionTable newQuestionTable() {
//        return QuestionTable.EMPTY;
//    }
//
//}