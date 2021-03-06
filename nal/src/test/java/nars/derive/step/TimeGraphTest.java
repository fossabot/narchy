package nars.derive.step;

import com.google.common.collect.Sets;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.Narsese;
import nars.term.Term;
import nars.util.time.TimeGraph;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static nars.$.$$;
import static nars.util.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimeGraphTest {

    /**
     * example time graphs
     */
    final TimeGraph A; {
        A = newTimeGraph(1);
        A.know($$("((one &&+1 two) ==>+1 (three &&+1 four))"), ETERNAL);
        A.know($$("one"), 1);
        A.know($$("two"), 20);

    }

    final TimeGraph B; {
        //               .believe("(y ==>+3 x)")
        //                .believe("(y ==>+2 z)")
        //                .mustBelieve(cycles, "(z ==>+1 x)", 1.00f, 0.45f)
        //                .mustNotOutput(cycles, "(z ==>-1 x)", BELIEF, Tense.ETERNAL)
        //                .mustBelieve(cycles, "(x ==>-1 z)", 1.00f, 0.45f)
        //                .mustNotOutput(cycles, "(x ==>+1 z)", BELIEF, Tense.ETERNAL)
        B = newTimeGraph(1);
        B.know($$("(y ==>+3 x)"), ETERNAL);
        B.know($$("(y ==>+2 z)"), ETERNAL);
    }

//    @BeforeEach
//    void init() {
//    }

    @Test
    public void testAtomEvent() {
        A.print();
        assertSolved("one", A, "one@1", "one@19");
    }

    @Test
    public void testSimpleConjWithOneKnownAbsoluteSubEvent1() {

        assertSolved("(one &&+1 two)", A,
                "(one &&+1 two)@1", "(one &&+1 two)@19");

    }

    @Test
    public void testSimpleConjWithOneKnownAbsoluteSubEvent2() {
        assertSolved("(one &&+- two)", A,
                "(one &&+1 two)","(one &&+1 two)@1", "(one &&+19 two)@1", "(one &&+1 two)@19");

    }

    @Test
    public void testSimpleConjOfTermsAcrossImpl1() {

        assertSolved("(two &&+1 three)", A,
                "(two &&+1 three)@2", "(two &&+1 three)@20");
    }
    @Test
    public void testSimpleConjOfTermsAcrossImpl2() {

        assertSolved("(two &&+- three)", A,

                "(two &&+17 three)@3", "(two &&+1 three)@2", "(two &&+1 three)@2", "(two &&+1 three)@20");

    }

    @Test
    public void testSimpleImplWithOneKnownAbsoluteSubEvent() {
        A.print();
        assertSolved("(one ==>+- three)", A,
                "(one ==>+2 three)", "(one ==>+20 three)", "(one ==>-16 three)");
    }

    @Test public void testImplChain1() {
        B.print();
        assertSolved("(z ==>+- x)", B, "(z ==>+1 x)");
    }

    @Test public void testImplChain2() {
        assertSolved("(x ==>+- z)", B, "(x ==>-1 z)");
    }



    @Test public void testConjChain1() throws Narsese.NarseseException {
        /** c is the same event, @6 */
        TimeGraph cc1 = newTimeGraph(1);
        cc1.know($.$("(a &&+5 b)"), 1);
        cc1.know($.$("(b &&+5 c)"), 6);
        cc1.print();
        assertSolved("(a &&+- c)", cc1,
                "(a &&+10 c)@1");
    }

    @Test public void testExact() throws Narsese.NarseseException {

        TimeGraph cc1 = newTimeGraph(1);
        cc1.know($.$("(a &&+5 b)"), 1);
        cc1.print();
        assertSolved("(a &&+- b)", cc1,
                "(a &&+5 b)@1");
    }

    @Test public void testLinkedTemporalConj() throws Narsese.NarseseException {

        TimeGraph cc1 = newTimeGraph(1);
        cc1.know($.$("(a &&+5 b)"), 1);
        cc1.know($.$("(b &&+5 c)"), 6);
        cc1.print();
        assertSolved("((b &&+5 c) &&+- (a &&+5 b))", cc1,
                "((a &&+5 b) &&+5 c)@1");
    }

    @Test
    public void testImplWithConjPredicate1() {
        assertSolved("(one ==>+- (two &&+1 three))", A,
                "(one ==>+1 (two &&+1 three))");
        A.print();
    }


    @Test public void testDecomposeImplConj() throws Narsese.NarseseException {
        /*
          $.02 ((b &&+10 d)==>a). 6 %1.0;.38% {320: 1;2;3;;} (((%1==>%2),(%1==>%2),is(%1,"&&")),((dropAnyEvent(%1) ==>+- %2),((StructuralDeduction-->Belief))))
            $.06 (((b &&+5 c) &&+5 d) ==>-15 a). 6 %1.0;.42% {94: 1;2;3} ((%1,%2,time(raw),belief(positive),task("."),notImpl(%2),notImpl(%1)),((%2 ==>+- %1),((Induction-->Belief))))
        */
        TimeGraph C = newTimeGraph(1);
        C.know($.$("(((b &&+5 c) &&+5 d) ==>-15 a)"), 6);
        assertSolved("((b &&+10 d) ==>+- a)", C, "((b &&+10 d) ==>-15 a)");
    }

    @Test
    public void testImplWithConjPredicate2() {
        assertSolved("(one ==>+- (two &&+- three))", A, //using one@1

                "((one &&+1 two) ==>+1 three)", "(one ==>+- (two &&+1 three))" //@ETE ?
                //using two@20
                //"(one ==>+1 (two &&+1 three))@19"
        );
    }
    @Test
    public void testImplWithTwoConjPredicates() throws Narsese.NarseseException {

        //wrong:
        //    $.05 ((a &&+5 b) ==>+3 (b &&+5 c)). -2 %1.0;.41% {4: 1;2} ((%1,%2,belief(positive),notImpl(%2),notImpl(%1)),((%2 ==>+- %1),((Induction-->Belief))))
        //    $.50 (b &&+5 c). 3 %1.0;.90% {3: 2} Narsese
        //    $.50 (a &&+5 b). 1 %1.0;.90% {1: 1} Narsese
        TimeGraph C = newTimeGraph(1);
        C.know($.$("(a &&+5 b)"), 1);
        C.know($.$("(b &&+5 c)"), 3);
        C.print();
        System.out.println();
        assertSolved("((a &&+5 b) ==>+- (b &&+5 c))", C,
                "((a &&+5 b) ==>-3 (b &&+5 c))@1");
        C.print();
    }
    @Test
    public void testImplWithConjSubjDecomposeProperly() throws Narsese.NarseseException {

        //wrong:
//        $.13 (a &&+1 a2)! 6 %1.0;.62% {7: 1;2} ((%1,(%2==>%3),notImpl(%1)),(subIfUnifiesAny(%2,%3,%1,"$"),((Abduction-->Belief),(Deduction-->Goal))))
//        $.50 b! 6 %1.0;.90% {6: 2} Narsese
//        $.50 ((a &&+1 a2)=|>b). 1 %1.0;.90% {1: 1} Narsese
        TimeGraph C = newTimeGraph(1);
        C.know($.$("b"), 6);
        C.know($.$("((a &&+1 a2)=|>b)"), 1);
        C.print();
        System.out.println();
        assertSolved("(a &&+1 a2)", C,
                "(a &&+1 a2)@5");
        C.print();
    }

    @Test public void testNobrainerNegation() throws Narsese.NarseseException {

        TimeGraph C = newTimeGraph(1);
        C.know($.$("x"), 1);
        C.know($.$("y"), 2);
        C.print();
        System.out.println();
        assertSolved("(--x ==>+- y)", C,
                "((--,x) ==>+1 y)@1");
        C.print();
    }
    @Test
    public void testConjSimpleOccurrences() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1);
        C.know($.$("(x &&+5 y)"), 1);
        C.know($.$("(y &&+5 z)"), 6);
        C.know($.$("(w &&+5 x)"), -4);
        C.print();
        System.out.println();
        assertSolved("x", C, "x@1");
        assertSolved("y", C, "y@6");
        assertSolved("z", C, "z@11");
        assertSolved("w", C, "w@-4");
        C.print();
    }
    @Test
    public void testConjTrickyOccurrences() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1);
        C.know($.$("(x &&+5 y)"), 1);
        C.know($.$("(y &&+5 z)"), 3);
        assertSolved("x", C, "x@1", "x@-2");
        assertSolved("y", C, "y@6", "y@3");
        assertSolved("z", C, "z@11", "z@8");
    }

    @Test
    public void testImplCrossDternalInternalConj() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1);

        C.know($.$("((a&&x) ==>+1 b)"), 1);
        //C.print(); System.out.println();
        assertSolved("(a ==>+- b)", C, "(a ==>+1 b)");
        //C.print(); System.out.println();
    }
    @Test
    public void testImplCrossParallelInternalConj() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1);
        C.know($.$("((a&|x) ==>+1 b)"), 1);
        assertSolved("(a ==>+- b)", C, "(a ==>+1 b)");
    }

    @Test
    public void testImplMultipleSolutions() throws Narsese.NarseseException {
        Occurrify C = new Occurrify(null) {
            XoRoShiRo128PlusRandom rng = new XoRoShiRo128PlusRandom(1);
            @Override
            protected Random random() {
                return rng;
            }

            @Override
            protected void onNewTerm(Term t) {
                link(shadow(t), 0, shadow(t.replace($$("_2"), $.varDep(1))));
                link(shadow(t), 0, shadow(t.neg()));
            }

            @Override
            protected Term dt(Term x, int dt) {
                return x.dt(dt);
            }
        };
        C.know((Term)$.$("(_2,_1)"), 1590);
        C.know((Term)$.$("(_2,_1)"), 2540);
        C.know((Term)$.$("(_2,_1)"), 2567);
//        C.link(C.shadow($.$("_2")), 0, C.shadow($.varDep(1)));
        C.print();

        //several slutions
        assertSolved("(((--,(#1,_1)) &&+220 (_2,_1)) ==>+- (_2,_1))", C,
                "(((--,(#1,_1)) &&+220 (_2,_1)) ==>+" + (2540-1590) + " (_2,_1))",
                "(((--,(#1,_1)) &&+220 (_2,_1)) ==>+" + (2567-1590) + " (_2,_1))"
                //..
        );
    }



    final List<Runnable> afterEach = $.newArrayList();

    @AfterEach
    void test() {
        afterEach.forEach(Runnable::run);
    }

    void assertSolved(String inputTerm, TimeGraph t, String... solutions) {

        int nodesBefore = A.nodes().size();
        String nodes = A.nodes().toString();
        long edgesBefore = A.edges().count();

        {
            ExpectSolutions ee = new ExpectSolutions(solutions);

            t.solve($$(inputTerm), false, ee);
        }

        int nodesAfter = A.nodes().size();
        long edgesAfter = A.edges().count();
        //assertEquals(edgesBefore, edgesAfter, "# of edges changed as a result of solving");
//        assertEquals(nodesBefore, nodesAfter, ()->"# of nodes changed as a result of solving:\n\t" + nodes + "\n\t" + A.nodes());

    }

    private class ExpectSolutions extends TreeSet<String> implements Predicate<TimeGraph.Event> {

        final Supplier<String> errorMsg;
        private final String[] solutions;

        public ExpectSolutions(String... solutions) {
            this.solutions = solutions;
            errorMsg = () ->
                    "expect: " + Arrays.toString(solutions) + "\n   got: " + this;

            afterEach.add(() -> {
                assertEquals(Sets.newTreeSet(List.of(solutions)), this, errorMsg);
                System.out.print(errorMsg.get());
            });
        }

        @Override
        public boolean test(TimeGraph.Event termEvent) {
            add(termEvent.toString());
            return true;
        }

    }

    static TimeGraph newTimeGraph(long seed) {
        return new TimeGraph() {
            XoRoShiRo128PlusRandom rng = new XoRoShiRo128PlusRandom(seed);
            @Override
            protected Random random() {
                return rng;
            }
        };
    }


}

//    public static void main(String[] args) throws Narsese.NarseseException {
//
//        TimeGraph t = new TimeGraph();
////        t.know($.$("(a ==>+1 b)"), ETERNAL);
////        t.know($.$("(b ==>+1 (c &&+1 d))"), 0);
////        t.know($.$("(a &&+1 b)"), 4);
//
//        t.know($.$("((one &&+1 two) ==>+1 (three &&+1 four))"), ETERNAL);
//        t.know($.$("one"), 1);
//        t.know($.$("two"), 20);
//
//        t.print();
//
//        System.out.println();
//
//        for (String s : List.of(
//                "one",
//                "(one &&+1 two)", "(one &&+- two)",
//                "(two &&+1 three)", "(two &&+- three)",
//                "(one ==>+- three)",
//                "(one ==>+- (two &&+1 three))",
//                "(one ==>+- (two &&+- three))"
//        )) {
//            Term x = $.$(s);
//            System.out.println("SOLVE: " + x);
//            t.solve(x, (y) -> {
//                System.out.println("\t" + y);
//                return true;
//            });
//        }
//
//
//    }