package nars.nal.nal8;

import jcog.list.CircularArrayList;
import nars.*;
import nars.op.Operator;
import nars.task.DerivedTask;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.var.Variable;
import nars.time.Tense;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.SortedSet;
import java.util.TreeSet;

import static jcog.Texts.n4;
import static nars.Op.BELIEF;

public class SanityTest {

    @Test public void testTape1() throws Narsese.NarseseException {
        Param.DEBUG = true;
        NAR n = NARS.tmp();
        n.log();


        int cap = 4;
        TermBuffer t = new TermBuffer(cap);
        for (int i = 0; i < cap; i++)
            t.tape.add($.the(i));

        n.onOp("get", new Operator.AtomicExec((xt, nn)->{
            @Nullable TermContainer aa = Operator.args(xt);
            if (aa.subs()==2) {
                Term tapeID = aa.sub(0);
                if (tapeID.op().conceptualizable && aa.sub(1) instanceof Variable) {

                    Term y = t.next();

                    long now = nn.time();
                    NALTask u = new NALTask(
                            $.func("get", tapeID, y), BELIEF, $.t(1f, nn.confDefault(BELIEF)),
                            now, now, now, nn.time.nextInputStamp());
                    u.pri(nn.priDefault(BELIEF));
                    n.input(u);
                }
            }
        }, 0.75f));

        for (int i = 0; i < 4; i++) {
            n.input("get(a, #x)! :|:");
            n.run(5);
            n.input("get(a, #x)! :|:");
            n.run(5);
            n.input("get(a, #x)! :|:");
            n.run(5);
            n.input("get(a, #x)! :|:");
            n.run(5);
            n.run(55);
        }
        n.clear();
        System.out.println("clear");
        n.input("get(a,3)! :|:");
        n.run(100);

    }

    @Test public void testSanity0() {
        Param.DEBUG = true;

        Term happy = $.the("happy");
        Term up = $.the("up");
        Term down = $.the("down");

        NAR n = NARS.tmp();

        n.truthResolution.setValue(0.1f);
        n.termVolumeMax.setValue(10);

        n.log();

        n.goal(happy);

        n.onCycle(()->{
            long now = n.time();

            Truth u = n.goalTruth(up, now);
            float uu = u!=null ? Math.max(0.5f, u.expectation()) : 0f;
            Truth d = n.goalTruth(down, now);
            float dd = d!=null ? Math.max(0.5f, d.expectation()) : 0f;

            n.believe(up, Tense.Present, uu);
            n.believe(down, Tense.Present, dd);
            n.believe(happy, Tense.Present, (uu-dd));
        });

        int STEP = 5;

        n.run(STEP);

        n.goal(up, Tense.Present, 1f);  n.run(STEP);
        n.goal(up, Tense.Present, 0f);  n.run(STEP);

        n.goal(down, Tense.Present, 1f); n.run(STEP);
        n.goal(down, Tense.Present, 0f); n.run(STEP);

        n.run(STEP*10);

    }

    @Test
    public void testSanity1() {

        Param.DEBUG = true;
        NAR n = NARS.tmp();

        final int[] togglesPos = {0};
        final int[] togglesNeg = {0};
        boolean[] target = new boolean[1];

        NAgent a = new NAgent(n) {


            float score = 0;

            {
                actionToggle($.the("mustBeOn"), (b) -> {
                    boolean good = b == target[0];
                    System.out.println(good ? "=== GOOD ===" : "=== BADD ===");
                    score += good ? +1 : -1;
                });

            }

            @Override
            protected float act() {
                float r = score;
                score = 0;

                return r;
            }
        };

        n.termVolumeMax.setValue(10);
        //a.durations.setValue(1);



        SortedSet<DerivedTask> derived = new TreeSet<>((y,x) -> {
             int f1 = Float.compare(x.conf(), y.conf());
             if (f1 == 0) {
                 int f2 = x.term().compareTo(y.term());
                 if (f2 == 0) {
                     return Integer.compare(x.hashCode(), y.hashCode());
                 }
                 return f2;
             }
             return f1;
        });
        n.onTask(t -> {
            if (t instanceof DerivedTask &&
                    t.isGoal()) {
                    //t.isBeliefOrGoal()) {
                //derived.add((DerivedTask)t);
                System.err.println(t.proof());
            }
        });

        //n.log();
        int timeBatch = 50;

        int batches = 2;
        for (int i = 0; i < batches; i++) {
            batchCycle(n, togglesPos, togglesNeg, target, a, derived, timeBatch);
            a.curiosity.setValue(1+(1f/i)); //decrease
        }
    }

    public void batchCycle(NAR n, int[] togglesPos, int[] togglesNeg, boolean[] target, NAgent a, SortedSet<DerivedTask> derived, int timeBatch) {
        //System.out.println("ON");
        target[0] = true;
        togglesPos[0] = togglesNeg[0] = 0;
        n.believe($.$safe("target:on"), Tense.Present);
        for (int i = 0; i < 1; i++) {
            batch(n, togglesPos[0], togglesNeg[0], a, derived, timeBatch);
        }

        //System.out.println("OFF");
        target[0] = false;
        togglesPos[0] = togglesNeg[0] = 0;
        n.believe($.$safe("(--,target:on)"), Tense.Present);
        for (int i = 0; i < 1; i++) {
            batch(n, togglesPos[0], togglesNeg[0], a, derived, timeBatch);
        }
    }

    public void batch(NAR n, int togglesPo, int i, NAgent a, SortedSet<DerivedTask> derived, int timeBatch) {
        n.run(timeBatch);


        float averageReward = a.rewardSum / timeBatch;
        //System.out.println("time=" + n.time() + "\tavg reward=" + n4(averageReward) );
        System.out.println(n.time() + ", "  + n4(averageReward) );
//                "\ttoggles +" + togglesPo + "/-" + i);
        a.rewardSum = 0;


            if (averageReward < 0) {
                derived.forEach(t -> {
                    if (t.isGoal())
                        System.out.println(t.proof());
                });
            }
        derived.clear();
    }

    static class TermBuffer {

        CircularArrayList<Term> tape;
        int pos = 0;

        public TermBuffer(int capacity) {
             tape = new CircularArrayList<>(capacity);
        }
        public synchronized void rewind() {
            pos = 0;
        }
        public synchronized Term next() {
            Term x = tape.get(pos);
            if (++pos >= tape.size())
                pos = 0;
            return x;
        }
    }
}