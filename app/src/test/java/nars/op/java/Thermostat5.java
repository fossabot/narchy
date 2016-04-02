package nars.op.java;

import com.gs.collections.api.block.function.primitive.FloatToFloatFunction;
import com.gs.collections.api.block.predicate.primitive.FloatPredicate;
import nars.$;
import nars.Global;
import nars.NAR;
import nars.Narsese;
import nars.nal.Tense;
import nars.nar.Default;
import nars.task.Task;
import nars.util.Optimization;
import nars.util.data.MutableInteger;
import nars.util.data.Util;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static java.lang.System.out;

/**
 *  this is trying to guess how to react to a hidden variable, its only given clues when its above or below
  and its goal is to avoid both those states
  anything below a score of 0.5 should be better than random
  it gets these above/below hints but it has to process these among all the other processing its thinking about
  then to really guess right it has to learn the timing of the sine wave
  and imagine at what rate it will travel and when it will change direction etc
 */
public class Thermostat5 {

    public static final float basePeriod = 8;
    public static final float tolerance = 0.05f;
    public static float targetPeriod = 2f;
    public static final float speed = 0.05f;
    static boolean print = true, debugError = false;

    public static void main(String[] args) {
        Default d = new Default(1024, 1, 1, 1);
        d.duration.set(Math.round(1f * basePeriod));
        d.activationRate.setValue(0.5f);
        //d.premiser.confMin.setValue(0.02f);

        float score = eval(d, 100);
        System.out.println("score=" + score);
    }
    public static void main2(String[] args) {
        int cycles = 2000;

        new Optimization<Default>(() -> {
            Default d = new Default(1024, 5, 2, 4);
            //d.perfection.setValue(0.1);
            d.shortTermMemoryHistory.setValue(4);
            d.premiser.confMin.setValue(0.1f);
            d.duration.set(Math.round(1.5f * basePeriod));
            d.core.conceptsFiredPerCycle.set(5);
            return d;
        })
                .with("activationRate", 0.1f, 0.3f, 0.1f, (a, x) -> {
                    x.activationRate.setValue(a);
                })
                .with("conceptDurations", 0.1f, 5f, 0.1f, (a, x) -> {
                    x.conceptForgetDurations.setValue(a);
                })
                .with("termLinkDurations", 0.1f, 20f, 0.1f, (a, x) -> {
                    x.termLinkForgetDurations.setValue(a);
                })
                .with("taskLinkDurations", 0.1f, 20f, 0.1f, (a, x) -> {
                    x.taskLinkForgetDurations.setValue(a);
                })
                /*.with("confMin", 0.05f, 0.3f, 0.01f, (a, x) -> {
                    x.premiser.confMin.setValue(a);
                })*/
                /*.with("durationFactor", 0.25f, 3.5f, 0.1f, (dFactor, x) -> {
                    x.duration.set(Math.round(basePeriod * dFactor));
                })*/
                /*.with("conceptsPercycle", 2, 5, 1, (c, x) -> {
                    x.core.conceptsFiredPerCycle.set((int)c);
                })*/
                .run(x -> eval(x, cycles)).print();


        //n.cyclesPerFrame.set(10);
        //n.derivationDurabilityThreshold.setValue(0.02f);
        //n.premiser.confMin.setValue(0.05f);

        //System.out.println(eval(n, 1000));
    }



    public static float eval(NAR n, int cycles) {

        final MutableInteger t = new MutableInteger();

        MutableBoolean training = new MutableBoolean();

        Global.DEBUG = true;

        //MutableFloat x0 = new MutableFloat();
        //MutableFloat x1 = new MutableFloat();
        MutableFloat yEst = new MutableFloat(0.5f); //NAR estimate of Y
        MutableFloat yHidden = new MutableFloat(0.5f); //actual best Y used by loss function

        MutableFloat loss = new MutableFloat(0);


        SensorConcept aboveness, belowness;


        //n.on(new SensorConcept((Compound)$.$("a:x0"), n, ()-> x0.floatValue())
        //        .resolution(0.01f)/*.pri(0.2f)*/
        //);
        /*n.on(new SensorConcept((Compound)$.$("a:x1"), n, ()-> x1.floatValue())
                .resolution(0.01f).pri(0.2f)
        );*/


        n.on(aboveness = new SensorConcept("<diff-->[above]>", n, () -> {
            float diff = yHidden.floatValue() - yEst.floatValue();
            if (diff > tolerance) return 0.5f + 0.5f * Util.clamp(diff);
            return 0;
        }).resolution(0.01f));

        n.on(belowness = new SensorConcept("<diff-->[below]>", n, () -> {
            float diff = -(yHidden.floatValue() - yEst.floatValue());
            if (diff > tolerance) return 0.5f + 0.5f * Util.clamp(diff);
            return 0;
        }).resolution(0.01f));


        n.onFrame(nn -> {

            //float switchPeriod = 20;
            //float highPeriod = 5f;

            float estimated = yEst.floatValue();

            int tt = t.intValue();
            float actual;
            if (tt > 0) {

                double y = 0.5f + 0.45f * Math.sin(tt / (targetPeriod * basePeriod));
                //y = y > 0.5f ? 0.95f : 0.05f;

                //x0.setValue(y); //high frequency phase
                //x1.setValue( 0.5f + 0.3f * Math.sin(n.time()/(highPeriod * period)) ); //low frequency phase

                //yHidden.setValue((n.time() / (switchPeriod * period)) % 2 == 0 ? x0.floatValue() : x1.floatValue());
                yHidden.setValue(y);

                actual = yHidden.floatValue();
                //out.println( actual + "," + estimated );

                loss.add(Math.abs(actual - estimated));
            } else {
                actual = 0.5f;
            }

            if (print) {

                int cols = 50;
                int colActual = (int) Math.round(cols * actual);
                int colEst = (int) Math.round(cols * estimated);
                for (int i = 0; i <= cols; i++) {

                    char c;
                    if (i == colActual)
                        c = '#';
                    else if (i == colEst)
                        c = '|';
                    else
                        c = '.';

                    out.print(c);
                }

                out.print(" <:" + belowness.get() + " >:" + aboveness.get());
                out.println();
            }
        });

        /** difference in order to diagnose an error */
        final float errorThresh = 0.15f;

        n.on(new DebugMotorConcept(n, "t(up)", yEst, yHidden,
                (v) -> {
                    if (v > 0) {
                        yEst.setValue(Util.clamp(+speed * v + yEst.floatValue()));
                        return v;
                    }
                    return -1;
                },
                (v) -> {
                    if (t.intValue()==0) return false; //training
                    //if already above the target value
                    return yHidden.floatValue() - yEst.floatValue() > errorThresh;
                }
        ));
        n.on(new DebugMotorConcept(n, "t(down)", yEst, yHidden,
                (v) -> {
                    if (v > 0) {
                        yEst.setValue(Util.clamp(-speed * v + yEst.floatValue()));
                        return v;
                    }
                    return -1;
                },
                (v) -> {
                    if (t.intValue()==0) return false; //training
                    //if already above the target value
                    return -(yHidden.floatValue() - yEst.floatValue()) > errorThresh;
                }
        ));



        //n.logSummaryGT(System.out, 0.0f);

        int trainMotionCycles = (int)(basePeriod * 8f);
        float str = 0.5f;

        System.out.println("training up");
        yEst.setValue(0.3f);
        n.goal($.$("t(up)"), Tense.Present, 1f, str);
        n.goal($.$("t(down)"), Tense.Present, 0f, str);
        for (int i = 0; i < trainMotionCycles; i++) {
            n.step();
        }

        System.out.println("training down");
        yEst.setValue(0.7f);
        n.goal($.$("t(up)"), Tense.Present, 0f, str);
        n.goal($.$("t(down)"), Tense.Present, 1f, str);
        for (int i = 0; i < trainMotionCycles; i++) {
            n.step();
        }

        System.out.println("training balance oscillation");
        yEst.setValue(0.5f);
        n.goal($.$("t(up)"), Tense.Present, 0.5f, str);
        n.goal($.$("t(down)"), Tense.Present, 0.5f, str);
        for (int i = 0; i < trainMotionCycles; i++) {
            n.step();
        }

        System.out.println("training finished");

        n.goal($.$("<diff-->[above]>"), 0f, 0.99f); //not above
        n.goal($.$("<diff-->[below]>"), 0f, 0.99f); //not below
        yEst.setValue(0.5f);

        for (int i = 0; i < cycles; i++) {
            n.step();
            t.add(1);
        }

        return loss.floatValue()/t.intValue();

    }

    private static class DebugMotorConcept extends MotorConcept {



        long lastTime;

        /** tasks collected from last cycle in which goals were received */
        final List<Task> current = Global.newArrayList();

        public DebugMotorConcept(NAR n, String term, MutableFloat yEst, MutableFloat yHidden, FloatToFloatFunction motor, FloatPredicate errorful) throws Narsese.NarseseException {
            super(term, n);
            setMotor( (v) -> {
                float next = motor.valueOf(v);
                if (debugError) {
                    if (errorful.accept(v)) {
                        for (Task t : current) {
                            if (!t.isInput())
                                System.err.println(t.explanation());
                        }
                    }
                }
                return next;
            });
            lastTime = -1;
        }

        @Nullable
        @Override
        public Task processGoal(@NotNull Task goal, @NotNull NAR nar) {
            long now = nar.time();
            if (now !=lastTime) {
                current.clear();
            }
            Task g = super.processGoal(goal, nar);
            if (g!=null)
                current.add(g);
            return g;
        }
    }
}
