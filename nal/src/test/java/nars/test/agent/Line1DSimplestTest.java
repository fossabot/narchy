package nars.test.agent;

import jcog.list.FasterList;
import jcog.math.RecycledSummaryStatistics;
import nars.NAR;
import nars.Narsese;
import nars.Param;
import nars.nar.Default;
import nars.task.DerivedTask;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static jcog.io.SparkLine.renderFloats;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 3/4/17.
 */
public class Line1DSimplestTest {

    @Test
    public void testSimple1() throws Narsese.NarseseException {

        NAR n = new Default();


        Line1DSimplest a = new Line1DSimplest(n);

        a.init();

        //n.log();
        a.trace = true;

        System.out.println("START initializing at target..\n");
        a.current = 0; a.target = 0;

        n.run(1);

        assertEquals(1f, a.rewardValue, 0.01f);

        n.run(1);

        assertEquals( 0.81f, n.emotion.happy(), 0.01f);
        assertEquals( 0.0, n.emotion.sad(), 0.01f);

        System.out.println("moving target away from reward..\n");
        a.target = 1;
        n.run(1);

        assertEquals(-1f, a.rewardValue, 0.01f);
        assertEquals( 0.0f, n.emotion.happy(), 0.1f);
        assertEquals( 0.81f, n.emotion.sad(), 0.4f); //this will be weakened by what caused the happiness in the previous cycle due to evidence decay's lingering effect

        assertEquals(0f, a.rewardSum(), 0.01f);

        System.out.println("AVG SCORE=" + a.rewardSum() / n.time());

    }

    @Test
    public void testSimplePerformance() throws Narsese.NarseseException {

        Param.ANSWER_REPORTING = false;

        Default n = new Default() {
//            @Override
//            public Deriver newDeriver() {
//                return Deriver.get("induction.nal");
//            }
        };
        n.core.conceptsFiredPerCycle.setValue(16);

        n.truthResolution.setValue(0.01f);
        n.termVolumeMax.setValue(16);

        Line1DSimplest a = new Line1DSimplest(n);

        a.init();

        List<Float> rewards = new ArrayList(8*1024);
        List<Float> motv = new ArrayList(8*1024);

        n.onCycle(()->{
            rewards.add(a.rewardValue);
            motv.add(a.dexterity());
        });

        //n.log();
        //a.trace = true;

        int trainTime = 8;

        a.current = 0; a.target = 0; n.run(trainTime);
        a.current = 0; a.target = 1; n.run(trainTime);
        a.current = 1; a.target = 0; n.run(trainTime);
        a.current = 1; a.target = 1; n.run(trainTime);


        final int changePeriod = trainTime;

        int time = 100;

        //n.log();
        for (int i = 0; i < time; i++) {
            if (i % changePeriod == 0)
                a.target = n.random.nextBoolean() ?  1f : 0f;
            n.run(1);
        }

        System.out.println( "rwrd: " +  renderFloats(downSample(rewards, 4)) );
        System.out.println( "motv: " + renderFloats(downSample(motv, 4)) );
        float avgReward = a.rewardSum() / n.time();
        System.out.println("avg reward = " + avgReward);

        assertTrue(avgReward > 0.5f); //75% accuracy

    }

    private static List<Float> downSample(List<Float> f, int divisor) {
        if (divisor == 1)
            return f;

        List<Float> l = new FasterList<>((int)Math.ceil(((float)f.size())/divisor));
        for (int i = 0; i < f.size(); ) {
            float total = 0;
            int j;
            for (j = 0; j < divisor && i < f.size(); j++ ) {
                total += f.get(i++);
            }
            l.add(total/j);
        }
        return l;
    }


    /** tests with an explicit rule provided that will help it succeed */
    @Ignore
    @Test public void testSimpleCheat() throws Narsese.NarseseException {


        NAR n = new Default(1024, 32, 1, 3);

        final int changePeriod = 32;

        //n.time.dur(changePeriod/4f);

        n.termVolumeMax.setValue(24);

        Line1DSimplest a = new Line1DSimplest(n);

        Param.DEBUG = true;

        //n.derivedEvidenceGain.setValue(0f);

        a.trace = true;
        a.init();
        a.target = 0;
        a.curiosity.setValue(0.05f);

        List<Float> hapy = new ArrayList(1*1024);
        List<Float> motv = new ArrayList(1*1024);
        List<Float> in = new ArrayList(1*1024);
        List<Float> out = new ArrayList(1*1024);

        //n.log();
        n.onTask(t -> {
            if (t instanceof DerivedTask) {
                if (t.isGoal() && t.term().toString().equals("L(out)"))
                    System.out.println(t.proof());
            }

            //System.out.println(t);
        });

//        n.input("((in) <=> (out)). %1.0;0.99%");
        //n.input("((in) ==> (out)). %1.0;0.99%");
        //n.input("(--(in) ==> --(out)). %1.0;0.99%");





        int time = 1024;

        int j = 0;
        for (int i = 0; i < time; i++) {
            n.input("(happy)! :|:");

            if ((i+1) % changePeriod == 0) {
                System.out.println("SWITCH");
                a.target = (j++) % 2 == 0  ? 1f : 0f;
            }
            if (i > time/4f)
                a.curiosity.setValue(0f);

            n.run(1);

            in.add(a.in.asFloat());
            out.add((float)a.out.feedback.getAsDouble());
            hapy.add(a.rewardValue);
            motv.add(a.dexterity());
        }

        int ds = 4;
        System.out.println( "  in:\t" + renderFloats(downSample(in, ds)) );
        System.out.println( " out:\t" + renderFloats(downSample(out, ds)) );
        System.out.println( "hapy:\t" + renderFloats(downSample(hapy, ds)) );
        System.out.println( "motv:\t" + renderFloats(downSample(motv, ds)) );

        System.out.println("AVG SCORE=" + a.rewardSum() / n.time());

        RecycledSummaryStatistics motvStat = new RecycledSummaryStatistics();
        for (Float x : motv)
            motvStat.accept(x);
        System.out.println(motvStat);
    }


}