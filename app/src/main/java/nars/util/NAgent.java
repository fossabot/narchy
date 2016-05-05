package nars.util;

import com.gs.collections.api.block.function.primitive.FloatToObjectFunction;
import nars.Global;
import nars.NAR;
import nars.concept.table.BeliefTable;
import nars.nal.Tense;
import nars.task.Task;
import nars.truth.DefaultTruth;
import nars.util.data.Util;
import nars.util.signal.MotorConcept;
import nars.util.signal.SensorConcept;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Agent interface wrapping a NAR
 */
public class NAgent implements Agent {

    private final NAR nar;

    float motivation[];
    float input[];

    private List<MotorConcept> actions;
    private List<SensorConcept> inputs;
    private SensorConcept reward;
    private int lastAction = -1;
    private float prevReward = 0, dReward = 0;

    /** learning rate */
    float alpha = 0.51f;

    /** exploration rate - confidence of initial goal for each action */
    float epsilon = 0.6f;

    private int discretization = 3;
    private SensorConcept dRewardPos, dRewardNeg;

    public NAgent(NAR n) {
        this.nar = n;
    }

    @Override
    public void start(int inputs, int actions) {
        nar.reset();

        motivation = new float[actions];
        input = new float[inputs];

        this.actions = IntStream.range(0, actions).mapToObj(i -> {

            MotorConcept.MotorFunction motorFunc = (b,d) -> {

                motivation[i] =
                        //d;
                        //Math.max(0, d-b);
                        d-b;
                        //d  / (1f + b);

                /*if (d < 0.5) return 0; //Float.NaN;
                if (d < b) return 0; //Float.NaN;
                return d-b;*/

                return Float.NaN;
            };

            return new MotorConcept(actionConceptName(i), nar, motorFunc);
        }).collect( toList());

        FloatToObjectFunction sensorTruth = (v) -> {
            return new DefaultTruth(v,
                    alpha);
                    //0.5f + alpha /2f /* learning rate */);
        };

        this.inputs = IntStream.range(0, inputs).mapToObj(i -> {
            return getSensorConcepts(sensorTruth, i, discretization);
        }).flatMap(x -> x).collect( toList());

        this.reward = new SensorConcept("(R)", nar,
                new RangeNormalizedFloat(() -> prevReward, -1, 1), sensorTruth)
                .resolution(0.01f).timing(-1, -1);

        FloatSupplier linearPositive = () -> Util.clamp(dReward > 0 ? dReward/2f + 0.5f : 0);
        FloatSupplier linearNegative = () -> Util.clamp(-dReward > 0 ? (-dReward)/2f + 0.5f : 0);
        this.dRewardPos = new SensorConcept("(dRp)", nar,
                linearPositive, sensorTruth)
                .resolution(0.01f).timing(-1, -1);
        this.dRewardNeg = new SensorConcept("(dRn)", nar,
                linearNegative, sensorTruth)
                .resolution(0.01f).timing(-1, -1);

        init();
    }

    public
    @NotNull
    Stream<SensorConcept> getSensorConcepts(FloatToObjectFunction sensorTruth, int i, int bits) {


        return IntStream.range(0, bits).mapToObj(bit -> {


//
//            00 0
//            01 1
//            10 2
//            11 3
//
//            return new SensorConcept(inputConceptName(i,bit), nar,  () -> {
//
//                int v = (( Math.round(input[i] * (1 << bits)) >> (bit)) % 2);
//                //System.out.println(i + ": " + input[i] + " " + bit + " = " + v);
//                return v;
//
//            }, sensorTruth).resolution(0.01f).timing(-1, -1);

            float min = bit / bits, max = min + (1f/bits);

            return new SensorConcept(inputConceptName(i,bit), nar,  () -> {

                float v = input[i];
                if (v >= 1f) v = 1f - Global.TRUTH_EPSILON; //clamp below 1.0 for this discretization

                //System.out.println(i + ": " + input[i] + " " + bit + " = " + v);
                return (v >= min) && (v < max) ? 1f : 0f;

            }, sensorTruth).resolution(0.01f).timing(-1, -1);

        });
    }

    protected void init() {

        seekReward();

        //nar.input("(--,(r))! %0.00;1.00%");
        actions.forEach(m -> init(m));


    }

    private void seekReward() {
        //TODO get this from the sensor/digitizers
        nar.goal("(R)", Tense.Eternal, 0.95f, 1f); //goal reward
        nar.goal("(dRp)", Tense.Eternal, 0.95f, 1f); //prefer increase
        nar.goal("(dRn)", Tense.Eternal, 0.05f, 1f); //avoid decrease
    }

    private void init(MotorConcept m) {
        //nar.ask($.$("(?x &&+0 " + m + ")"), '@');
        nar.goal(m, Tense.Present, 1f, epsilon);
        //nar.goal(m, Tense.Present, 0f, epsilon);


    }

    @Override
    public int act(float reward, float[] nextObservation) {

        if (lastAction!=-1) {
            learn(input, lastAction, reward);
        }

        observe(nextObservation);

        int nextAction = decide(this.lastAction);

        return this.lastAction = nextAction;
    }

    public void observe(float[] nextObservation) {
        System.arraycopy(nextObservation, 0, input, 0, nextObservation.length);

        //nar.conceptualize(reward, UnitBudget.One);
        nar.step();
    }

    private void learn(float[] input, int action, float reward) {

        this.dReward = reward - prevReward;

        this.prevReward = reward;

    }

    private int decide(int lastAction) {
        int nextAction = -1;
        float best = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < motivation.length; i++) {
            float m = motivation[i];
            if (m > best) {
                best = m;
                nextAction = i;
            }
        }

        if (lastAction!=nextAction) {
            if (lastAction != -1) {
                nar.believe(actions.get(lastAction), Tense.Present, 0f, alpha);
                nar.goal(actions.get(lastAction), Tense.Present, 0f, alpha);
            }
        }
        nar.goal(actions.get(nextAction), Tense.Present, 1f, alpha);
        nar.believe(actions.get(nextAction), Tense.Future, 1f, alpha);


        /*for (int a = 0; a < actions.size(); a++)
            nar.believe(actions.get(a), Tense.Present,
                    (nextAction == a ? 1f : 0f), 0.9f);*/

        return nextAction;
    }

    private String actionConceptName(int i) {
        return "(a" + i + ")";
    }

    private String inputConceptName(int i, int component) {
        return "(i" + i +
                    (component != -1 ? ("_" + component) :"") +
                ")";

        //return "{i" + i + "}";
        //return "(input, i" + i + ")";
        //return "input:i" + i;
        //return "input:{i" + i + '}';

    }

    public static void printTasks(NAR n, boolean beliefsOrGoals) {
        TreeSet<Task> bt = new TreeSet<>((a, b) -> { return a.term().toString().compareTo(b.term().toString()); });
        n.forEachConcept(c -> {
            BeliefTable table = beliefsOrGoals ? c.beliefs() : c.goals();

            if (!table.isEmpty()) {
                bt.add(table.top(n.time()));
                //System.out.println("\t" + c.beliefs().top(n.time()));
            }
        });
        bt.forEach(xt -> {
            System.out.println(xt);
        });
    }


}
