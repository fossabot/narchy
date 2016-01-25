package nars.nal.nal8;

import nars.Narsese;
import nars.task.Task;
import nars.term.Term;
import nars.term.transform.subst.Subst;
import nars.util.data.random.XorShift128PlusRandom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

/** responds to questions by inserting beliefs as answers */
public abstract class PatternAnswer implements Function<Task, List<Task>> {

    final Random rng = new XorShift128PlusRandom(1);
    @Nullable
    public final Term pattern;

    protected PatternAnswer(String pattern) {
        this.pattern = Narsese.the().termRaw(pattern);
    }

    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + pattern.toString() + ']';
    }

    @Nullable
    @Override
    public List<Task> apply(Task questionTask) {
// TODO reimplement
//        FindSubst s = new FindSubst(Op.VAR_PATTERN, rng);
//        if (s.matchAll(pattern, questionTask.get(), Global.UNIFICATION_POWER)) {
//            List<Task> answers = run(questionTask, s);
//            if (answers!=null)
//                return process(questionTask, answers);
//        }
        return null;
    }

    @NotNull
    private List<Task> process(Task question, @NotNull List<Task> answers) {
        answers.forEach(a -> {
//            a.setParentTask(question);
        });
        return answers;
    }

    @Nullable
    public abstract List<Task> run(Task operationTask, Subst map1);
}
