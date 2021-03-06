package nars.concept.dynamic;

import nars.*;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.link.TaskLink;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Int;
import nars.truth.Truth;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static nars.$.$;
import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.util.time.Tense.ETERNAL;
import static nars.util.time.Tense.XTERNAL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 10/27/16.
 */
public class DynamicTruthBeliefTableTest {

    @Test
    public void testDynamicConjunction2() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("b:x", 0f, 0.9f);
        n.run(1);
        long now = n.time();

        assertEquals($.t(1f, 0.81f), n.beliefTruth($("(a:x && a:y)"), now)); //truth only
        //assertEquals($.t(0f, 0.90f), n.beliefTruth("(a:x && (--,a:y))", now));
        assertEquals($.t(0f, 0.81f), n.beliefTruth("(a:x && (--,a:y))", now));

        assertEquals($.t(1f, 0.81f), n.belief($("(a:x && a:y)"), now).truth()); //matched task

        assertEquals($.t(0f, 0.81f), n.beliefTruth("(b:x && a:y)", now));
        assertEquals($.t(1f, 0.81f), n.beliefTruth("((--,b:x) && a:y)", now));
        assertEquals($.t(0f, 0.81f), n.beliefTruth("((--,b:x) && (--,a:y))", now));
    }

    @Test
    public void testDynamicConjunctionEternalOverride() throws Narsese.NarseseException {
        NAR n = NARS.shell()
                .believe($$("a:x"), 0)
                .believe($$("a:y"), 0);

        long now = n.time();
        assertEquals($.t(1f, 0.81f), n.beliefTruth($("(a:x && a:y)"), now)); //truth only
        assertEquals($.t(1f, 0.81f), n.belief($("(a:x && a:y)"), now).truth()); //truth only

        n.believe($$("--(a:x && a:y)"), 0); //contradict

        n.concept("(a:x && a:y)").beliefs().print();

        Truth tNow = n.beliefTruth($("(a:x && a:y)"), now);
        assertTrue($.t(0.32f, 0.93f).equalsIn(tNow, n));

        Truth tAfter = n.beliefTruth($("(a:x && a:y)"), now + 2);
        assertTrue($.t(0.32f, 0.82f).equalsIn(tAfter, n), ()->tAfter.toString());

        Truth tLater = n.beliefTruth($("(a:x && a:y)"), now + 5);
        assertTrue($.t(0.32f, 0.69f).equalsIn(tLater, n),()->tLater.toString()); //more certainly negative because the eternal will override
    }

    @Test
    public void testDynamicConjunctionTemporalOverride() throws Narsese.NarseseException {
        NAR n = NARS.shell()
                .believe("a:x", 1f, 0.9f)
                .believe("a:y", 1f, 0.9f);

        n.run(1);
        long now = n.time();
        assertEquals($.t(1f, 0.81f), n.beliefTruth($("(a:x && a:y)"), now));

        n.believe($$("--(a:x && a:y)"), now); //contradict

        //n.concept("(a:x && a:y)").beliefs().print();
        Truth tt = n.belief($("(a:x && a:y)"), now).truth();
        assertTrue($.t(0.32f, 0.93f).equalsIn(tt, n), () -> tt.toString());
    }

    @Test
    public void testDynamicIntersection() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("a:(--,y)", 0f, 0.9f);
        n.believe("a:z", 0f, 0.9f);
        n.believe("a:(--,z)", 1f, 0.9f);
        n.believe("x:b", 1f, 0.9f);
        n.believe("y:b", 1f, 0.9f);
        n.believe("z:b", 0f, 0.9f);
        n.run(2);
        for (long now : new long[]{0, n.time() /* 2 */, ETERNAL}) {
            assertTrue(n.conceptualize($("((x|y)-->a)")).beliefs() instanceof DynamicTruthBeliefTable);
            assertEquals($.t(1f, 0.81f), n.beliefTruth("((x|y)-->a)", now));
            assertEquals($.t(0f, 0.81f), n.beliefTruth(n.conceptualize($("((x|z)-->a)")), now));
            assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("((x&z)-->a)")), now));
            assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("(b --> (x|y))")), now));
            assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("(b --> (x|z))")), now));
            assertEquals($.t(0f, 0.81f), n.beliefTruth(n.conceptualize($("(b --> (x&z))")), now));

            Concept xIntNegY = n.conceptualize($("((x|--y)-->a)"));
            assertTrue(xIntNegY.beliefs() instanceof DynamicTruthBeliefTable);
            assertTrue(xIntNegY.goals() instanceof DynamicTruthBeliefTable);
            assertEquals($.t(0f, 0.81f), n.beliefTruth(xIntNegY, now), now + " " + xIntNegY);
            assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("((x|--z)-->a)")), now));
        }
    }

    @Test
    public void testDynamicIntRange() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe("x:1", 1f, 0.9f);
        n.believe("x:2", 0.5f, 0.9f);
        n.believe("x:3", 0f, 0.9f);
        n.run(1);

        Concept x12 = n.conceptualize($.inh(Int.range(1, 2), $.the("x")));
        Concept x23 = n.conceptualize($.inh(Int.range(2, 3), $.the("x")));
        Concept x123 = n.conceptualize($.inh(Int.range(1, 3), $.the("x")));
        assertEquals("%.50;.81%", n.beliefTruth(x12, ETERNAL).toString());
        assertEquals("%0.0;.90%", n.beliefTruth(x23, ETERNAL).toString());
        assertEquals("%0.0;.90%", n.beliefTruth(x123, ETERNAL).toString());
    }

    @Test
    public void testDynamicIntVectorRange() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe("x(1,1)", 1f, 0.9f);
        n.believe("x(1,2)", 0.5f, 0.9f);
        n.believe("x(1,3)", 0f, 0.9f);
        n.run(1);

        Term t12 = $.inh($.p(Int.the(1), Int.range(1, 2)), $.the("x"));
        assertEquals("x(1,1..2)", t12.toString());
        Concept x12 = n.conceptualize(t12);
        assertTrue(x12.beliefs() instanceof DynamicTruthBeliefTable);

        Concept x23 = n.conceptualize($.inh($.p(Int.the(1), Int.range(2, 3)), $.the("x")));
        Concept x123 = n.conceptualize($.inh($.p(Int.the(1), Int.range(1, 3)), $.the("x")));
        assertEquals("%.50;.81%", n.beliefTruth(x12, ETERNAL).toString());
        assertEquals("%0.0;.90%", n.beliefTruth(x23, ETERNAL).toString());
        assertEquals("%0.0;.90%", n.beliefTruth(x123, ETERNAL).toString());
    }


    @Test
    public void testDynamicConjunction3() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("a:z", 1f, 0.9f);
        n.run(1);

        TaskConcept cc = (TaskConcept) n.conceptualize($("(&&, a:x, a:y, a:z)"));
        Truth now = n.beliefTruth(cc, n.time());
        assertNotNull(now);
        assertTrue($.t(1f, 0.73f).equalsIn(now, 0.1f), now + " truth at " + n.time());
        //the truth values were provided despite the belief tables being empty:

        //test unknown:
        {
            TaskConcept ccn = (TaskConcept) n.conceptualize($("(&&, a:x, a:w)"));
            Truth nown = n.beliefTruth(ccn, n.time());
            assertNull(nown);
        }

        //test negation:
        Concept ccn = n.conceptualize($("(&&, a:x, (--, a:y), a:z)"));

        {
            Task t = n.belief(ccn.term());
            assertNotNull(t);
            assertEquals(0f, t.freq());
        }

        assertTrue(ccn instanceof TaskConcept);
        Truth nown = n.beliefTruth(ccn, n.time());
        assertEquals("%0.0;.73%", nown.toString());

        n.clear();

        //test change after a component's revision to negate the input freq
        n.believe("a:y", 0, 0.95f);
        n.run(1);
        n.concept("a:y").print();
        Task ay = n.belief($$("a:y"));
        assertTrue(ay.freq() < 0.5f);

        Task bb = n.belief(n.conceptualize($("(&&, a:x, a:y, a:z)")), n.time());
        Truth now2 = bb.truth();
        assertTrue(now2.freq() < 0.4f);

    }

    @Test
    public void testDynamicConjunctionEternal() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe($("x"));
        n.believe($("y"));
        n.believe($("--z"));
        //n.run(1);
//        TaskConcept cc = (TaskConcept) n.conceptualize($("(x && y)"));

//        DynamicBeliefTable xtable = (DynamicBeliefTable) (cc.beliefs());

        for (long w : new long[]{ETERNAL, 0, 1}) { //since it's eternal, it will be equal to any time calculated
            assertEquals($.t(1, 0.81f), n.truth($("(x && y)"), BELIEF, w));
            assertEquals($.t(0, 0.81f), n.truth($("(x && --y)"), BELIEF, w));
            assertEquals($.t(1, 0.81f), n.truth($("(x && --z)"), BELIEF, w));
        }
    }

    @Test
    public void testDynamicConjunction2Temporal() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe($("(x)"), (long) 0);
        n.believe($("(y)"), (long) 4);
        n.time.dur(8);
        TaskConcept cc = (TaskConcept) n.conceptualize($("((x) && (y))"));

        DynamicTruthBeliefTable xtable = (DynamicTruthBeliefTable) cc.beliefs();
//        Compound template = $("((x) &&+4 (y))");

//        DynTruth xt = xtable.truthDynamic(0, 0, template, n);
//        assertNotNull(xt);
        //assertTrue($.t(1f, 0.81f).equals(xt.truth(n), 0.1f), xt.truth(n).toString());

        assertEquals(0.81f, xtable.taskDynamic(0, 0, $("((x) &&+4 (y))"), n).conf(), 0.05f); //best match to the input
        assertEquals(0.74f, xtable.taskDynamic(0, 0, $("((x) &&+6 (y))"), n).conf(), 0.07f);
        assertEquals(0.75f, xtable.taskDynamic(0, 0, $("((x) &&+2 (y))"), n).conf(), 0.07f);
        assertEquals(0.75f, xtable.taskDynamic(0, 0, $("((x) &&+0 (y))"), n).conf(), 0.07f);
        assertEquals(0.62f, xtable.taskDynamic(0, 0, $("((x) &&-32 (y))"), n).conf(), 0.2f);


    }

    @Test
    public void testDynamicConceptValid1() throws Narsese.NarseseException {
        Term c =
                //$.$("( &&+- ,(--,($1 ==>+- (((joy-->fz)&&fwd) &&+- $1))),(joy-->fz),fwd)");
                Op.CONJ.compound(XTERNAL, new Term[]{$.$("(--,($1 ==>+- (((joy-->fz)&&fwd) &&+- $1)))"), $.$("(joy-->fz)"), $.$("fwd")}).normalize();

        assertTrue(c instanceof Compound, () -> c.toString());
        assertTrue(Task.validTaskTerm(c), () -> c + " should be a valid task term");
    }

    @Test
    public void testDynamicConceptValid2() throws Narsese.NarseseException {
        Term c =
                //( &&+- ,(--,((--,#1)&&#2)),(--,#2),#1)
                Op.CONJ.compound(XTERNAL, new Term[]{$.$("(--,((--,#1)&&#2))"), $.$("(--,#2)"), $.varDep(1)}).normalize();

        assertTrue(c instanceof Compound, () -> c.toString());
        assertTrue(Task.validTaskTerm(c), () -> c + " should be a valid task term");
    }

    @Test
    public void testDynamicConjunctionXYZ() throws Narsese.NarseseException {
        //only needs to consider the evidence which effectively short-circuits the result
        //ie. once Intersection hits zero frequency it can not go any higher.

        NAR n = NARS.shell();
        n.believe("x", 1f, 0.50f);
        n.believe("y", 1f, 0.50f);
        n.believe("z", 0f, 0.81f);
        n.run(1);
        assertEquals(
                "%0.0;.20%", n.beliefTruth(
                        n.conceptualize($("(&&,x,y,z)")
                        ), n.time()).toString()
        );
        {
            //reduced
            Task bXYZ = n.belief($("(&&,x,y,z)"), n.time());
            assertEquals("(&&,x,y,z)", bXYZ.term().toString()); //only z term mattered
            assertEquals(3, bXYZ.stamp().length);
        }
        {
            //normal
            Task bXY = n.belief($("(x && y)"), n.time());
            assertEquals("(x&&y)", bXY.term().toString());
            assertEquals(2, bXY.stamp().length);
        }
        {
            //normal eternal
            Task bXY = n.belief($("(x && y)"), ETERNAL);
            assertEquals("(x&&y)", bXY.term().toString());
            assertEquals(2, bXY.stamp().length);
        }

        assertEquals(
                "%0.0;.41%", n.beliefTruth(
                        n.conceptualize($("(&&,y,z)")
                        ), n.time()).toString()
        );
        assertEquals(
                "%1.0;.25%", n.beliefTruth(
                        n.conceptualize($("(&&,x,y)")
                        ), n.time()).toString()
        );
    }

//    @Test
//    public void testDynamicConjunctionShortCircuitEvenWithMissing() throws Narsese.NarseseException {
//
//        NAR n = NARS.shell();
//        n.believe("x", 1f, 0.50f);
//        //n.believe("y", ...); //Y unknown
//        n.believe("z", 0f, 0.81f);
//        n.run(1);
//        {
//            Task belief = n.belief(n.conceptualize($("(&&,x,y,z)")), n.time());
//            assertEquals("%0.0;.81%", belief.truth().toString());
//            assertEquals(1, belief.stamp().length);
//        }
//    }
//
//    @Test
//    public void testDynamicUnionShortCircuit() throws Narsese.NarseseException {
//        NAR n = NARS.shell();
//        n.believe("a:x", 0f, 0.50f);
//        n.believe("a:y", 0f, 0.50f);
//        n.believe("a:z", 1f, 0.81f);
//        n.run(1);
//        Term xyz = $("((&,x,y,z)-->a)");
//        assertEquals(
//                "%1.0;.81%", n.beliefTruth(xyz, n.time()).toString()
//        );
//        Task nbt = n.belief(xyz, n.time());
//        assertNotNull(nbt);
//        assertEquals(
//                1, nbt.stamp().length
//        );
//    }

    @Test
    public void testDynamicConjConceptWithNegations() throws Narsese.NarseseException {

        NAR n = NARS.shell();
        for (String s : new String[]{
                "((y-->t) &&+1 (t-->happy))",
                "(--(y-->t) &&+1 (t-->happy))",
                "((y-->t) &&+1 --(t-->happy))",
                "(--(y-->t) &&+1 --(t-->happy))",
        }) {
            Concept c = n.conceptualize($.$(s));
            assertTrue(c.beliefs() instanceof DynamicTruthBeliefTable);
            assertTrue(c.goals() instanceof DynamicTruthBeliefTable);
        }

    }

    @Test
    public void testRawDifference() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe("x", 0.75f, 0.50f);
        n.believe("y", 0.25f, 0.50f);
        n.run(1);
        Term xMinY = $("(x ~ y)");
        Term yMinX = $("(y ~ x)");
        assertEquals(DynamicTruthBeliefTable.class, n.conceptualize(xMinY).beliefs().getClass());
        assertEquals(DynamicTruthBeliefTable.class, n.conceptualize(yMinX).beliefs().getClass());
        assertEquals(
                "%.56;.25%", n.beliefTruth(xMinY, n.time()).toString()
        );
        assertEquals(
                "%.06;.25%", n.beliefTruth(yMinX, n.time()).toString()
        );

    }

    @Test
    public void testDynamicBeliefTableSampling() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.believe("x", 0f, 0.50f);
        n.believe("y", 0f, 0.50f);
        n.run(1);
        TaskLink.GeneralTaskLink tl = new TaskLink.GeneralTaskLink($$("(x && y)"), BELIEF, ETERNAL, 1f);
        Set<Task> tasks = new HashSet();
        for (int i = 0; i < 10; i++)
            tasks.add(tl.get(n));
        assertEquals("[$.50 (x&&y). %0.0;.25%]", tasks.toString());
        tasks.forEach(System.out::println);
    }

    @Test
    public void testDynamicBeliefTableSamplingTemporalFlexible() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.input("x. +1");
        n.input("y. +2");
        n.run(1);
        TaskLink.GeneralTaskLink tl = new TaskLink.GeneralTaskLink($$("(x && y)"), BELIEF, 1, 1f);
        Set<Task> tasks = new HashSet();
        for (int i = 0; i < 100; i++)
            tasks.add(tl.get(n));
        assertEquals("[$.50 (x &&+1 y). 1 %1.0;.81%]", tasks.toString());

    }

}