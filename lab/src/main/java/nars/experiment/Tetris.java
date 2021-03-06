package nars.experiment;

import jcog.math.FloatRange;
import jcog.signal.Bitmap2D;
import nars.*;
import nars.op.java.Opjects;
import nars.term.Term;
import nars.util.TimeAware;
import nars.util.signal.Bitmap2DSensor;

import java.util.Arrays;
import java.util.Random;
import java.util.Vector;

import static nars.experiment.Tetris.TetrisState.*;
import static nars.util.signal.Bitmap2DSensor.XY;

/**
 * Created by me on 7/28/16.
 */
public class Tetris extends NAgentX implements Bitmap2D {

    public final FloatRange timePerFall = new FloatRange(2f, 1f, 32f);

    public static final int tetris_width = 8;
    public static final int tetris_height = 16;

    //private static SensorConcept[][] concept;
    //private int afterlife = TIME_PER_FALL * tetris_height * tetris_width;
    static boolean easy;

    private TetrisState state;

    private final Bitmap2DSensor<Bitmap2D> pixels;

    public Tetris(NAR nar) throws Narsese.NarseseException {
        this(nar, Tetris.tetris_width, Tetris.tetris_height);
    }

    public Tetris(NAR nar, int width, int height) throws Narsese.NarseseException {
        this(nar, width, height, 1);
    }

    /**
     * @param width
     * @param height
     * @param timePerFall larger is slower gravity
     */
    public Tetris(NAR nar, int width, int height, int timePerFall) {
        super("tetris", nar);

        state = new TetrisState(width, height, timePerFall) {
            @Override
            protected int nextBlock() {


                if (easy) {
                    //EASY MODE
                    return 1; //square blocks
                    //return 0; //long blocks
                } else {
                    return super.nextBlock(); //all blocks
                }
            }

//            @Override
//            protected void die() {
//                //nar.time.tick(afterlife);
//                super.die();
//            }
        };

//        view.children().add(new TetrisVisualizer(state, 2, false) {
//            @Override
//            public boolean onKey(v2 hitPoint, char charCode, boolean pressed) {
////
////                switch (charCode) {
////                    case 'a':
////                        if (motorRotate!=null)
////                            nar.goal(motorRotate, Tense.Present, pressed ? 0f : 0.5f, gamma);
////                        break;
////                    case 's':
////                        if (motorRotate!=null)
////                            nar.goal(motorRotate, Tense.Present, pressed ? 1f : 0.5f, gamma);
////                        break;
////                    case 'z':
////                        nar.goal(motorLeftRight, Tense.Present, pressed ? 0f : 0.5f, gamma);
////                        break;
////                    case 'x':
////                        nar.goal(motorLeftRight, Tense.Present, pressed ? 1f : 0.5f, gamma);
////                        break;
////                }
//
//                return true;
//            }
//        });


        addCamera(
                pixels = new Bitmap2DSensor<>(
                    //(x, y)->$.inh($.p(x,y), id)
                    XY(id, 4, width, height)
                    //InhRecurse(id, width, height, 2)
                    //RadixRecurse(id, width, height, 2)
                    //RadixProduct(id, width,height,2)
                    //(x, y)->$.func("cam", id, $.the(x), $.the(y))
                    //(x, y)->$.p(id, $.the(x), $.the(y))
                    //(x, y)->$.p(x,y)
                , this, nar)
                //.resolution(0.1f)
        );
        //pixels.resolution(0.1f);


        actionsReflect();
        //actionsTriState();
        actionsToggle();

        state.reset();


//        view = new Grid(
////                    new MatrixView(tetris_width, tetris_height, (x, y, gl) -> {
////                        float r = nar.pri(concept[x][y], Float.NaN);
////                        gl.glColor3f(r, 0, 0);
////                        return 0f;
////                    }),
//                new MatrixView(tetris_width, tetris_height, (x, y, gl) -> {
//                    SensorConcept cxy = pixels.concept(x, y);
//                    long now = this.now;
//                    int dur = nar.dur();
//                    float b = cxy.beliefFreq(now, dur);
//                    Truth gg = cxy.goal(now, dur);
//                    float gp, gn;
//                    float g = gg!=null ? gg.freq() : Float.NaN;
//                    if (g == g) {
//                        g -= 0.5f;
//                        g *= 2f;
//                        float c = gg.conf();
//                        if (g < 0) {
//                            gp = 0;
//                            gn = -g * c;
//                        } else {
//                            gp = g * c;
//                            gn = 0;
//                        }
//                    } else {
//                        gp = gn = 0;
//                    }
//                    gl.glColor3f(gp, b, gn);
//                    return 0f;
//                })
////                    new MatrixView(tetris_width, tetris_height, (x, y, gl) -> {
////                        long then = (long) (now + DUR * 8);
////                        Truth f = concept[x][y].belief(then, DUR);
////                        float fr, co;
////                        if (f == null) {
////                            fr = 0.5f;
////                            co = 0;
////                        } else {
////                            fr = f.freq();
////                            co = f.conf();
////                        }
////                        gl.glColor4f(0, fr, 0, 0.25f + 0.75f * co);
//////                        Draw.colorPolarized(gl,
//////                                concept[x][y].beliefFreq(then, 0.5f) -
//////                                        concept[x][y].beliefFreq(now, 0.5f));
////                        return 0f;
////                    })
//
//        );
//        view.layout();
    }

    private void actionsReflect() {

        Opjects oo = new Opjects(nar);
        oo.exeThresh.set(0.6f);

        Opjects.methodExclusions.add("toVector");

        //state = new TetrisState(tetris_width, tetris_height, 2);
        state =
                //oo.the("tetris", this.state);
                oo.a("tetris", TetrisState.class, tetris_width, tetris_height, 2);

    }


    void actionsToggle() {
        final Term LEFT = $.the("left"); //$.inh("left", id);
        final Term RIGHT = $.the("right"); //$.inh("right", id);
        final Term ROT = $.the("rotate"); //$.inh("rotCW", id);

        actionPushButton(LEFT, () -> state.act(TetrisState.LEFT));
        actionPushButton(RIGHT, () -> state.act(TetrisState.RIGHT));
        actionPushButton(ROT, () -> state.act(CW));
        //actionToggle($.p("rotCCW"), ()-> state.take_action(CCW));
    }

    void actionsTriState() {


        actionTriState($.func("X", id), (i) -> {
            switch (i) {
                case -1:
                    return state.act(LEFT);
                case +1:
                    return state.act(RIGHT);
                default:
                case 0:
                    return true;
            }
        });



        actionPushButton($.func("R", id), () -> state.act(CW));

//        actionTriState($("R"), (i) -> {
//            switch (i) {
//                case -1:
//                    state.take_action(CCW);
//                    break;
//                case 0:
//                    break;
//                case +1:
//                    state.take_action(CW);
//                    break;
//            }
//        });

//        actions.add(new ActionConcept($("x(tetris)"), nar, (b, d) -> {
//            //float alpha = nar.confidenceDefault(BELIEF);
//
//            if (d != null) {
//                float x = d.freq();
//                float alpha = d.conf();
//                //System.out.println(d + " " + x);
//                if (x > actionThresholdHigh) {
//                    if (state.take_action(RIGHT))
//                        //return d; //legal move
//                        //return d.withConf(gamma);
//                        return $.t(1, alpha);
//                } else if (x < actionThresholdLow) {
//                    if (state.take_action(LEFT))
//                        //return d; //legal move
//                        //return d.withConf(gamma);
//                        return $.t(0, alpha);
//                } else {
//                    //return $.t(0.5f, alpha); //no action taken or move ineffective
//                }
//            }
//            //return $.t(0.5f, alpha); //no action taken or move ineffective
//            return null;
//        }));

        //if (rotate) {
//        actions.add(new ActionConcept($("rotate(tetris)"), nar, (b, d) -> {
//
//            if (d != null) {
//                float r = d.freq();
//                float alpha = d.conf();
//                if (r > actionThresholdHigh) {
//                    if (state.take_action(CW))
//                        //return d; //legal move
//                        //return d.withConf(gamma);
//                        return $.t(1, alpha);
//                } else if (r < actionThresholdLow) {
//                    if (state.take_action(CCW))
//                        //return d; //legal move
//                        //return d.withConf(gamma);
//                        return $.t(0, alpha);
//                } else {
//                    //return $.t(0.5f, alpha); //no action taken or move ineffective
//                }
//            }
//            //return $.t(0.5f, alpha); //no action taken or move ineffective
//            return null;
//        }));
//        } else {
//            motorRotate = null;
//        }

        //actions.add(motorDown = new MotorConcept("(down)", nar));
//        if (downMotivation > actionThresholdHigh) {
//            state.take_action(FALL);
//        }

    }

    @Override
    public int width() {
        return state.width;
    }

    @Override
    public int height() {
        return state.height;
    }

    @Override
    public float brightness(int xx, int yy) {
        int index = yy * state.width + xx;
        return state.seen[index] > 0 ? 1f : 0f;
    }

//    public void sensors(NAR nar, TetrisState state, List<SensorConcept> sensors) {
//
//
//
//        concept = new SensorConcept[state.width][state.height];
//
//        Atomic tetris = $.the("tetris");
//        for (int y = 0; y < state.height; y++) {
//            int yy = y;
//            for (int x = 0; x < state.width; x++) {
//                int xx = x;
//                Compound squareTerm =
//                        //$.p(x, y);
//
//                        $.inh(
//                                //$.func(
//
//                                 /*   $.p(
//                                            $.pRadix(x, PIXEL_RADIX, state.width),
//                                            $.pRadix(y, PIXEL_RADIX, state.height))*/
//                                $.p( $.pRecurse( $.radixArray(x, PIXEL_RADIX, state.width) ),
//                                     $.pRecurse( $.radixArray(y, PIXEL_RADIX, state.height) ) ),
//
////                                $.secte( $.pRecurseIntersect( 'x', $.radixArray(x, PIXEL_RADIX, state.width) ),
////                                        $.pRecurseIntersect( 'y', $.radixArray(y, PIXEL_RADIX, state.height) ) ),
//
//                                tetris
//                                //        x, y
//
//                        )
//                        //$.p(
//                        //$.the("tetris"))
//                        //, $.the(state.time)))
//                        ;
//
//                //System.out.println(x + " " + y + " "  + squareTerm);
//
//                //$.p($.pRadix(x, 4, state.width), $.pRadix(y, 4, state.height));
//                int index = yy * state.width + xx;
//                @NotNull SensorConcept s = new SensorConcept(squareTerm, nar,
//                        () -> state.seen[index] > 0 ? 1f : 0f,
//
//                        //null //disable input
//
//                        (v) -> $.t(v, nar.confidenceDefault(BELIEF))
//                )
//                        //timing(0, visionSyncPeriod)
//                        ;
//
////                FloatSupplier defaultPri = s.sensor.pri;
////                s.pri( () -> defaultPri.asFloat() * 0.25f );
//
//                concept[x][y] = s;
//
//                s.pri(pixelPri);
//
//                sensors.add(s);
//
//            }
//        }
//    }

//    //TODO
//    public static class NARCam {
//        public int width;/*how wide our board is*/
//        public int height;/*how tall our board is*/
//
//
//    }

//    /**
//     * RLE/scanline input method: groups similar pixels (monochrome) into a runline using a integer range
//     */
//    protected void input() {
//
////        float thresh = 0.5f;
////
////        inputAxis(thresh, true);
////        inputAxis(thresh, false);
//    }

//    private void inputAxis(float thresh, boolean horizontal) {
//        int hh = horizontal ? state.height : state.width;
//        for (int y = 0; y < hh; ) {
//
//            int start = 0, end = 0;
//            int sign = 0;
//
//            int ww = horizontal ? state.width : state.height;
//            for (int x = 0; x < ww; ) {
//
//                int i;
//                if (horizontal)
//                    i = y * ww + x;
//                else
//                    i = x * hh + y;
//
//                float s = state.seen[i];
//
//                if (x == 0) {
//                    //beginning of span
//                    sign = (int) Math.signum(s);
//                } else {
//
//                    if (sign > 0) {
//                        if (s < (thresh)) {
//                            sign = -1;
//                        } else {
//                            end = x;  //continue span
//                        }
//                    }
//                    if (sign < 0) {
//                        if (s > (1f - thresh)) {
//                            sign = +1;
//                        } else {
//                            end = x; //continue span
//                        }
//                    }
//                }
//
//                //if it switched or reach the end of the line
//                if (end != x || (x >= ww - 1)) {
//                    //end of span
//                    if (end - start == 1) {
//                        inputBlock(start, start + 1, sign, horizontal);
//                    } else {
//                        inputSpan(start, end, y, sign, horizontal);
//                    }
//                }
//
//                x++;
//            }
//
//            y++;
//        }
//    }
//
//    private void inputSpan(int start, int end, int axis, int sign, boolean horizontal) {
//
//        Truth t = $.t(sign > 0 ? 1f : 0f,
//                //(float)Math.pow(alpha, end-start)
//                alpha
//        );
//        if (t == null)
//            return; //too low confidence
//
//        Term range = new Termject.IntInterval(start, end);
//        Term fixed = new IntTerm(axis);
//
//        //TODO collect evidence stamp
//        nar.believe(
//                horizontal ? $.p(range, fixed) : $.p(fixed, range),
//                Tense.Present,
//                t.freq(), t.conf() //HACK this parameters sux
//        );
//    }
//
//    private void inputBlock(int x, int y, float v, boolean horizontal) {
//
//        Truth t = $.t(v > 0 ? 1f : 0f,
//                //(float)Math.pow(alpha, end-start)
//                alpha
//        );
//        if (t == null)
//            return; //too low confidence
//
//        //TODO collect evidence stamp
//        nar.believe(
//                horizontal ? $.p(x, y) : $.p(y, x),
//                Tense.Present,
//                t.freq(), t.conf() //HACK this parameters sux
//        );
//    }


    @Override
    public float act() {

        this.pixels.input();

        state.timePerFall = Math.round(timePerFall.floatValue());
        return state.next();

    }


//        public static void newTimeWindow(NAR n) {
//            LogIndex li = new LogIndex();
//
//            final int cap = 90;
//            long start = System.currentTimeMillis();
//            long end = start + 5000;
//            float width = 2f;
//            SpaceGraph s = new SpaceGraph(
//                    new NARSpace(n) {
//                        @Override
//                        protected void get(Collection displayNext) {
//                            li.containing(new Rect1D.DefaultRect1D(start, end), x -> {
//                                if (displayNext.size() > cap) {
//                                    return false;
//                                }
//                                Spatial w = space.getOrAdd(x, t -> new ConceptWidget(n, $.the(t.toString()), 1));
//
//                                ((SimpleSpatial) w).moveX((-0.5f + (float) (x.from() - start) / (end - start)) * width, 0.8f);
//                                //((SimpleSpatial)w).moveY(0, 0.4f);
//                                //((SimpleSpatial)w).moveZ(0, 0.4f);
//                                displayNext.add(w);
//                                return true;
//                            });
//                        }
//                    }.with(
//                            new Flatten()
//                            //new Spiral()
//                            //new FastOrganicLayout()
//                    )
//            );
////        ForceDirected forceDirect = new ForceDirected();
////        //forceDirect.repelSpeed = 0.5f;
////        s.dyn.addBroadConstraint(forceDirect);
//
//
//            s.show(1300, 900);
//        }

    public static void main(String[] args) {
        //


        TimeAware nn = NAgentX.runRT((n) -> {
            Tetris a = null;
            try {

                //n.confResolution.set(0.01f);



                a = new Tetris(n, Tetris.tetris_width, Tetris.tetris_height);
//                a.nar.log();


                n.freqResolution.set(0.02f);
                //n.time.dur(a.nar.dur()*2); //16x slower perception

                //a.durations.setValue(2f);
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }

//            try {
//                InterNAR i = new InterNAR(n, 8, 0);
//                i.runFPS(5);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }


            Tetris aa = a;
//            n.onTask((t) -> {
//                if (t.isEternal() && !t.isInput()) {
//                    System.err.println(t.proof());
//                }
//                if (t.isGoal() && (t.term().equals(aa.sad.term()) || t.term().equals(aa.happy.term()))) {
//                    System.err.println(t.proof());
//                }
//
//            });

            return a;
        }, 10f);


//
//        for (int i = 0; i < 10000; i++) {
//            System.out.println(((NARS)n).stats());
//            Util.sleep(500);
//        }
//
//        n.stop();
//        a.stop();

//        //nar.index.print(System.out);
//        n.forEachTask(System.out::println);
//
//        //NAR.printActiveTasks(nar, true);
//        //NAR.printActiveTasks(nar, false);
//        n.printConceptStatistics();
//        new TaskStatistics().add(n).print();


        //NARBuilder.newALANN(clock, 4, 64, 5, 4, 1);

//            n.onCycle(new Runnable() {
//
//                long forgetPeriod = 200;
//                long lastForget = n.time();
//
//                @Override
//                public void run() {
//                    long now = n.time();
//                    if (now - lastForget > forgetPeriod) {
//                        System.err.println("BREATHE");
//                        ((Default)n).core.active.clear();
//                        lastForget = now;
//                    }
//                }
//            });


        //nar.derivedEvidenceGain.setValue(2f);


        //nar.truthResolution.setValue(0.05f);

        //NAR nar = new TaskNAR(32 * 1024, new MultiThreadExecutioner(4, 4096), clock);
//            MySTMClustered stm = new MySTMClustered(nar, 64, '.', 4, false, 2);
//            MySTMClustered stmGoal = new MySTMClustered(nar, 16, '!', 2, false, 1);


        //nar.linkFeedbackRate.setValue(0.05f);

        //newTimeWindow(nar);


//        Random rng = new XorShift128PlusRandom(1);
//        //Multi nar = new Multi(3,512,
//        int maxVol = 32;
//
//        Executioner e = Tetris.exe;
//        ((MultiThreadExecutioner)exe).sync(false);
//
//        Default nar = new Default(1024,
//                256, 2, 3, rng,
//                //new CaffeineIndex(new DefaultConceptBuilder(rng), 1024*128, maxVol/2, false, e),
//                //new MapDBIndex(new DefaultConceptBuilder(rng), 200000, Executors.newSingleThreadScheduledExecutor()),
//                new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(), 200000, 8192, 2),
//                new FrameClock(), e
//        );
//
//
//        nar.beliefConfidence(0.9f);
//        nar.goalConfidence(0.9f);
//
//        Param.DEBUG_ANSWERS = Param.DEBUG;
//
////        nar.onTask(t -> {
////            long now = nar.time();
////            if (t.isBeliefOrGoal() && t.occurrence() > 1 + now) {
////                System.err.println("\tFUTURE: " + t + "\t vs. PRESENT: " +
////                        ((BeliefTable)(t.concept(nar).tableFor(t.punc()))).truth(now)
////                        //+ "\n" + t.proof() + "\n"
////                );
////            }
////        });
//

        /*
        nar.onCycle((n)->{
            FloatSummaryReusableStatistics inputPri = new FloatSummaryReusableStatistics();
            FloatSummaryReusableStatistics derivPri = new FloatSummaryReusableStatistics();
            FloatSummaryReusableStatistics otherPri = new FloatSummaryReusableStatistics();
            n.tasks.forEach(t -> {
                float tp = t.pri();
                if (tp != tp)
                    return;
                if (t.isInput()) {
                    inputPri.accept(tp);
                } else if (t instanceof DerivedTask) {
                    derivPri.accept(tp);
                } else {
                    otherPri.accept(tp);
                }
            });

            System.out.println("input=" + inputPri);
            System.out.println("deriv=" + derivPri);
            System.out.println("other=" + otherPri);
            System.out.println();
        });
        */

//
//
//        nar.confMin.setValue(0.02f);
//
//
//        nar.compoundVolumeMax.setValue(maxVol);
//        nar.linkFeedbackRate.setValue(0.15f);
//

//
////        nar.on(new TransformConcept("seq", (c) -> {
////            if (c.size() != 3)
////                return null;
////            Term X = c.term(0);
////            Term Y = c.term(1);
////
////            Integer x = intOrNull(X);
////            Integer y = intOrNull(Y);
////            Term Z = (x!=null && y!=null)? ((Math.abs(x-y) <= 1) ? $.the("TRUE") : $.the("FALSE")) : c.term(2);
////
////
////            return $.inh($.p(X, Y, Z), $.oper("seq"));
////        }));
////        nar.believe("seq(#1,#2,TRUE)");
////        nar.believe("seq(#1,#2,FALSE)");
//
        //n.log();
//        //nar.logSummaryGT(System.out, 0.1f);
//
////		nar.log(System.err, v -> {
////			if (v instanceof Task) {
////				Task t = (Task)v;
////				if (t instanceof DerivedTask && t.punc() == '!')
////					return true;
////			}
////			return false;
////		});
//
//
////        Abbreviation abbr = new Abbreviation(nar, "the", 3, 12,
////                0.01f, 64);
//
//        new Inperience(nar);
//

        //new VariableCompressor(nar);


//
//            Default m = new Default(512, 16, 2, n.random,
//                    new CaffeineIndex(new DefaultConceptBuilder(), 4096, false, n.exe),
//                    new RealTime.DSHalf().durSeconds(0.2f));

//            float metaLearningRate = 0.75f;
//            m.confMin.setValue(0.01f);
//            m.goalConfidence(metaLearningRate);
//            m.termVolumeMax.setValue(24);


//        MetaAgent metaT = new MetaAgent(a
//                //,m
//
//        );
//        metaT.init();
//        //metaT.trace = true;
//        a.onFrame((z)->metaT.cycle());

        //n.onCycle(metaT.nar::cycle);


        //a.trace = true;

        //metaT.nar.log();
//            m.onTask(t -> {
//                if (t instanceof DerivedTask)
//                    System.out.println("meta: " + t);
//            });
        //window(conceptsTreeChart(m, 64), 800, 600);
        //NAgentX.chart( metaT );

//
//            SoNAR s = null;
//            try {
//                s = new SoNAR(nar);
//                s.samples("/tmp/wav");
//                s.listen(t.happy.term());
//                s.listen($("tetris"));
//                s.listen($("0"));
//                s.listen($("1"));
//
////        s.listen($("a:b"));
////        s.listen($("b:c"));
//                //s.join();
//            } catch (LineUnavailableException e) {
//                e.printStackTrace();
//            }


//        NARController meta = new NARController(nar, loop, t);
//
//        newControlWindow(Lists.newArrayList(
//                newCPanel(nar, 256, () -> meta.rewardValue),
//                newBeliefChart(meta, 200)
//
//        ));


        //NAR.printTasks(meta.nar, true);
        //NAR.printTasks(meta.nar, false);
//        nar.forEachActiveConcept(c -> {
//            if (c.volume() < 12)
//                c.print();
//        });
    }

    public static class OfflineTetris {
        public static void main(String[] args) throws Narsese.NarseseException {


            NAR n = NARS.tmp();
            n.time.dur(4);
            n.freqResolution.set(0.02f);
            n.confResolution.set(0.02f);
            //new Abbreviation(n, "z", 3, 8, 0.1f, 32);

            new Tetris(n, Tetris.tetris_width, Tetris.tetris_height, 2);
            n.run(200);

            //n.concepts().map(x -> x.toString()).sorted().forEach(c -> {
            n.concepts().forEach(c -> {
                System.out.println(c);
                c.tasks().forEach(t -> {
                    System.out.println("\t" + t.toString(true));
                });
                System.out.println();
            });


            n.stats(System.out);
        }
    }

    //    public static class NARController extends NAgent {
//
//        private final NARLoop loop;
//        private final NAR worker;
//        private final NAgent env;
//        private final FloatSupplier learn;
//        private final RangeNormalizedFloat busy;
//        public float score;
//
//
//        @Override
//        protected float act() {
//            //float avgFramePeriodMS = (float) loop.frameTime.getMean();
//
//            //float mUsage = memory();
//            //float targetMemUsage = 0.75f;
//
//            return this.score = (
//                    (1f + learn.asFloat()) *       //learn
////                    (1f + (1f- busy.asFloat())) *  //avoid busywork
//                    (1f + happysad.asFloat())      //boost for motivation change
//
//                    //env.rewardNormalized.asFloat() +
//
////                    1 / (1f + Math.abs(targetMemUsage - mUsage) / (targetMemUsage)) //maintain % memory utilization TODO cache 'memory()' result
//            );
//        }
//
//
//        public NARController( NAR worker, NARLoop loop, NAgent env) {
//
//            super( new Default(384, 4, 3, 2, new XORShiftRandom(2),
//                    //new CaffeineIndex(new DefaultConceptBuilder(new XORShiftRandom(3)), 5*100000, false, exe),
//                    new TreeIndex.L1TreeIndex(new DefaultConceptBuilder(new XORShiftRandom(3)), 100000, 16384, 4),
//
//                    new FrameClock()) {
//                       @Override
//                       protected void initHigherNAL() {
//                           super.initHigherNAL();
//                           cyclesPerFrame.setValue(16);
//                           //ctl.confMin.setValue(0.01f);
//                           //ctl.truthResolution.setValue(0.01f);
//                           beliefConfidence(0.5f);
//                           goalConfidence(0.5f);
//                       }
//                   });
//
//            this.worker = worker;
//            this.loop = loop;
//            this.env = env;
//
//            busy = new RangeNormalizedFloat(()->(float)worker.emotion.busy.getSum());
//            happysad = new RangeNormalizedFloat(()->(float)worker.emotion.happysad());
//            learn = ()->(float)worker.emotion.learning();
//
//            //nar.log();
//            worker.onFrame(nn -> next());
//
//            init(nar);
//            mission();
//        }
//
//        @Override public void next() {
//            super.next();
//            nar.next();
//        }
//
//
//
//        @Override
//        protected void init(NAR n) {
//
//            float sensorResolution = 0.05f;
//
//            float sensorConf = alpha;
//
//            FloatToObjectFunction<Truth> truther = (v) -> $.t(v, sensorConf);
//
//            sensors.addAll(Lists.newArrayList(
//                    new SensorConcept("(motive)", n,
//                            happysad,
//                            truther
//                    ).resolution(sensorResolution),
//                    new SensorConcept("(busy)", n,
//                            new RangeNormalizedFloat(() -> (float) worker.emotion.busy.getSum()),
//                            truther
//                    ).resolution(sensorResolution),
//                    new SensorConcept("(learn)", n,
//                            learn,
//                            truther
//                    ).resolution(sensorResolution),
//                    new SensorConcept("(memory)", n,
//                            () -> memory(),
//                            truther
//                    ).resolution(sensorResolution)
//            ));
//
//            //final int BASE_PERIOD_MS = 100;
//            final int MAX_CONCEPTS_FIRE_PER_CYCLE = 32;
//            final int MAX_LINKS_PER_CONCEPT = 24;
//
//            actions.addAll(Lists.newArrayList(
//                    //cpu throttle
//                    /*new MotorConcept("(cpu)", nar, (b, d) -> {
//                        int newPeriod = Math.round(((1f - (d.expectation())) * BASE_PERIOD_MS));
//                        loop.setPeriodMS(newPeriod);
//                        //System.err.println("  loop period ms: " + newPeriod);
//                        return d;
//                    }),*/
//
//                    //memory throttle
////                    new MotorConcept("(memoryWeight)", nar, (b, d) -> {
////                        ((CaffeineIndex) worker.index).compounds.policy().eviction().ifPresent(e -> {
////                            float sweep = 0.1f; //% sweep , 0<sweep
////                            e.setMaximum((long) (DEFAULT_INDEX_WEIGHT * (1f + sweep * 2f * (d.freq() - 0.5f))));
////                        });
////                        //System.err.println("  loop period ms: " + newPeriod);
////                        return d;
////                    }),
//
//                    new MotorConcept("(confMin)", nar, (b, d) -> {
//                        float MAX_CONFMIN = 0.1f;
//                        float newConfMin = Math.max(Param.TRUTH_EPSILON, MAX_CONFMIN * d.freq());
//                        worker.confMin.setValue(newConfMin);
//                        return d;
//                    }),
////
////                    new MotorConcept("(inputActivation)", nar, (b, d) -> {
////                        worker.inputActivation.setValue(d.freq());
////                        return d;
////                    }),
////
////                    new MotorConcept("(derivedActivation)", nar, (b, d) -> {
////                        worker.derivedActivation.setValue(d.freq());
////                        return d;
////                    }),
//
//                    new MotorConcept("(conceptsPerFrame)", nar, (b, d) -> {
//                        ((Default) worker).core.conceptsFiredPerCycle.setValue((int) (d.freq() * MAX_CONCEPTS_FIRE_PER_CYCLE));
//                        return d;
//                    }),
//
//                    new MotorConcept("(linksPerConcept)", nar, (b, d) -> {
//                        float l = d.freq() * MAX_LINKS_PER_CONCEPT;
//                        l = Math.max(l, 1f);
//                        int vv = (int) Math.floor((float)Math.sqrt(l));
//
//                        ((Default) worker).core.tasklinksFiredPerFiredConcept.setValue(vv);
//                        ((Default) worker).core.termlinksFiredPerFiredConcept.setValue((int)Math.ceil(l / vv));
//                        return d;
//                    }),
//
//                    new MotorConcept("(envCuriosity)", nar, (b, d) -> {
//                        float exp = d.freq();
//                        env.epsilonProbability = exp;
//                        env.gammaEpsilonFactor = exp*exp;
//                        return d;
//                    })
//            ));
//        }
//
//        public final float memory() {
//            Runtime runtime = Runtime.getRuntime();
//            long total = runtime.totalMemory(); // current heap allocated to the VM process
//            long free = runtime.freeMemory(); // out of the current heap, how much is free
//            long max = runtime.maxMemory(); // Max heap VM can use e.g. Xmx setting
//            long usedMemory = total - free; // how much of the current heap the VM is using
//            long availableMemory = max - usedMemory; // available memory i.e. Maximum heap size minus the current amount used
//            float ratio = 1f - ((float)availableMemory) / max;
//            //logger.warn("max={}k, used={}k {}%, free={}k", max/1024, total/1024, Texts.n2(100f * ratio), free/1024);
//            return ratio;
//        }
//
//        final RangeNormalizedFloat happysad;
//
//    }
//
//
//    //    static void addCamera(Tetris t, NAR n, int w, int h) {
////        //n.framesBeforeDecision = GAME_DIVISOR;
////        SwingCamera s = new SwingCamera(t.vis);
////
////        NARCamera nc = new NARCamera("t", n, s, (x, y) -> $.p($.the(x), $.the(y)));
////
////        NARCamera.newWindow(s);
////
////        s.input(0, 0, t.vis.getWidth(),t.vis.getHeight());
////        s.output(w, h);
////
////        n.onFrame(nn -> {
////            s.update();
////        });
////    }

    public static class TetrisPiece {

        int[][][] thePiece = new int[4][5][5];
        int currentOrientation;

        public void setShape(int Direction, int[] row0, int[] row1, int[] row2, int[] row3, int[] row4) {
            thePiece[Direction][0] = row0;
            thePiece[Direction][1] = row1;
            thePiece[Direction][2] = row2;
            thePiece[Direction][3] = row3;
            thePiece[Direction][4] = row4;
        }

        public int[][] getShape(int whichOrientation) {
            return thePiece[whichOrientation];
        }

        public static TetrisPiece makeSquare() {
            TetrisPiece newPiece = new TetrisPiece();

            //Orientation 0,1,2,3
            int[] row0 = {0, 0, 0, 0, 0};
            int[] row1 = {0, 0, 1, 1, 0};
            int[] row2 = {0, 0, 1, 1, 0};
            int[] row3 = {0, 0, 0, 0, 0};
            int[] row4 = {0, 0, 0, 0, 0};
            newPiece.setShape(0, row0, row1, row2, row3, row4);
            newPiece.setShape(1, row0, row1, row2, row3, row4);
            newPiece.setShape(2, row0, row1, row2, row3, row4);
            newPiece.setShape(3, row0, row1, row2, row3, row4);

            return newPiece;
        }

        public static TetrisPiece makeTri() {
            TetrisPiece newPiece = new TetrisPiece();

            {
                //Orientation 0
                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 0, 1, 0, 0};
                int[] row2 = {0, 1, 1, 1, 0};
                int[] row3 = {0, 0, 0, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(0, row0, row1, row2, row3, row4);
            }
            {
                //Orientation 1
                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 0, 1, 0, 0};
                int[] row2 = {0, 0, 1, 1, 0};
                int[] row3 = {0, 0, 1, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(1, row0, row1, row2, row3, row4);
            }

            {
                //Orientation 2
                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 0, 0, 0, 0};
                int[] row2 = {0, 1, 1, 1, 0};
                int[] row3 = {0, 0, 1, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(2, row0, row1, row2, row3, row4);
            }
            //Orientation 3
            int[] row0 = {0, 0, 0, 0, 0};
            int[] row1 = {0, 0, 1, 0, 0};
            int[] row2 = {0, 1, 1, 0, 0};
            int[] row3 = {0, 0, 1, 0, 0};
            int[] row4 = {0, 0, 0, 0, 0};
            newPiece.setShape(3, row0, row1, row2, row3, row4);

            return newPiece;
        }

        public static TetrisPiece makeLine() {
            TetrisPiece newPiece = new TetrisPiece();

            {
                //Orientation 0+2
                int[] row0 = {0, 0, 1, 0, 0};
                int[] row1 = {0, 0, 1, 0, 0};
                int[] row2 = {0, 0, 1, 0, 0};
                int[] row3 = {0, 0, 1, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(0, row0, row1, row2, row3, row4);
                newPiece.setShape(2, row0, row1, row2, row3, row4);
            }

            //Orientation 1+3
            int[] row0 = {0, 0, 0, 0, 0};
            int[] row1 = {0, 0, 0, 0, 0};
            int[] row2 = {0, 1, 1, 1, 1};
            int[] row3 = {0, 0, 0, 0, 0};
            int[] row4 = {0, 0, 0, 0, 0};
            newPiece.setShape(1, row0, row1, row2, row3, row4);
            newPiece.setShape(3, row0, row1, row2, row3, row4);
            return newPiece;

        }

        public static TetrisPiece makeSShape() {
            TetrisPiece newPiece = new TetrisPiece();

            {
                //Orientation 0+2
                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 1, 0, 0, 0};
                int[] row2 = {0, 1, 1, 0, 0};
                int[] row3 = {0, 0, 1, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(0, row0, row1, row2, row3, row4);
                newPiece.setShape(2, row0, row1, row2, row3, row4);
            }

            //Orientation 1+3
            int[] row0 = {0, 0, 0, 0, 0};
            int[] row1 = {0, 0, 1, 1, 0};
            int[] row2 = {0, 1, 1, 0, 0};
            int[] row3 = {0, 0, 0, 0, 0};
            int[] row4 = {0, 0, 0, 0, 0};
            newPiece.setShape(1, row0, row1, row2, row3, row4);
            newPiece.setShape(3, row0, row1, row2, row3, row4);
            return newPiece;

        }

        public static TetrisPiece makeZShape() {
            TetrisPiece newPiece = new TetrisPiece();

            {
                //Orientation 0+2
                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 0, 1, 0, 0};
                int[] row2 = {0, 1, 1, 0, 0};
                int[] row3 = {0, 1, 0, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(0, row0, row1, row2, row3, row4);
                newPiece.setShape(2, row0, row1, row2, row3, row4);
            }

            //Orientation 1+3
            int[] row0 = {0, 0, 0, 0, 0};
            int[] row1 = {0, 1, 1, 0, 0};
            int[] row2 = {0, 0, 1, 1, 0};
            int[] row3 = {0, 0, 0, 0, 0};
            int[] row4 = {0, 0, 0, 0, 0};
            newPiece.setShape(1, row0, row1, row2, row3, row4);
            newPiece.setShape(3, row0, row1, row2, row3, row4);
            return newPiece;

        }

        public static TetrisPiece makeLShape() {
            TetrisPiece newPiece = new TetrisPiece();

            {
                //Orientation 0
                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 0, 1, 0, 0};
                int[] row2 = {0, 0, 1, 0, 0};
                int[] row3 = {0, 0, 1, 1, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(0, row0, row1, row2, row3, row4);
            }
            {
                //Orientation 1
                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 0, 0, 0, 0};
                int[] row2 = {0, 1, 1, 1, 0};
                int[] row3 = {0, 1, 0, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(1, row0, row1, row2, row3, row4);
            }

            {
                //Orientation 2
                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 1, 1, 0, 0};
                int[] row2 = {0, 0, 1, 0, 0};
                int[] row3 = {0, 0, 1, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(2, row0, row1, row2, row3, row4);
            }
            //Orientation 3
            int[] row0 = {0, 0, 0, 0, 0};
            int[] row1 = {0, 0, 0, 1, 0};
            int[] row2 = {0, 1, 1, 1, 0};
            int[] row3 = {0, 0, 0, 0, 0};
            int[] row4 = {0, 0, 0, 0, 0};
            newPiece.setShape(3, row0, row1, row2, row3, row4);

            return newPiece;
        }

        public static TetrisPiece makeJShape() {
            TetrisPiece newPiece = new TetrisPiece();

            {
                //Orientation 0
                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 0, 1, 0, 0};
                int[] row2 = {0, 0, 1, 0, 0};
                int[] row3 = {0, 1, 1, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(0, row0, row1, row2, row3, row4);
            }
            {
                //Orientation 1
                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 1, 0, 0, 0};
                int[] row2 = {0, 1, 1, 1, 0};
                int[] row3 = {0, 0, 0, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(1, row0, row1, row2, row3, row4);
            }

            {
                //Orientation 2
                int[] row0 = {0, 0, 0, 0, 0};
                int[] row1 = {0, 0, 1, 1, 0};
                int[] row2 = {0, 0, 1, 0, 0};
                int[] row3 = {0, 0, 1, 0, 0};
                int[] row4 = {0, 0, 0, 0, 0};
                newPiece.setShape(2, row0, row1, row2, row3, row4);
            }
            //Orientation 3
            int[] row0 = {0, 0, 0, 0, 0};
            int[] row1 = {0, 0, 0, 0, 0};
            int[] row2 = {0, 1, 1, 1, 0};
            int[] row3 = {0, 0, 0, 1, 0};
            int[] row4 = {0, 0, 0, 0, 0};
            newPiece.setShape(3, row0, row1, row2, row3, row4);

            return newPiece;
        }

        @Override
        public String toString() {
            StringBuilder shapeBuffer = new StringBuilder();
            for (int i = 0; i < thePiece[currentOrientation].length; i++) {
                for (int j = 0; j < thePiece[currentOrientation][i].length; j++) {
                    shapeBuffer.append(' ').append(thePiece[currentOrientation][i][j]);
                }
                shapeBuffer.append('\n');
            }
            return shapeBuffer.toString();

        }
    }

    public static class TetrisState {
        /*Action values*/
        public static final int LEFT = 0; /*Action value for a move left*/
        public static final int RIGHT = 1; /*Action value for a move right*/
        public static final int CW = 2; /*Action value for a clockwise rotation*/
        public static final int CCW = 3; /*Action value for a counter clockwise rotation*/
        public static final int NONE = 4; /*The no-action Action*/
        public static final int FALL = 5; /* fall down */


        public int width;
        public int height;
        public float[] seen;

        private final Random randomGenerator = new Random();

        public boolean running = true;
        public int currentBlockId;/*which block we're using in the block table*/

        public int currentRotation;
        public int currentX;/* where the falling block is currently*/

        public int currentY;
        public float score;/* what is the current_score*/

        public boolean is_game_over;/*have we reached the end state yet*/


        public float[] worldState;/*what the world looks like without the current block*/

        //	/*Hold all the possible bricks that can fall*/
        Vector<TetrisPiece> possibleBlocks = new Vector<>();
        public int time;

        public int timePerFall;
        private int rowsFilled;


        //private double[] worldObservation;



        public TetrisState(int width, int height, int timePerFall) {
            this.width = width;
            this.height = height;
            this.timePerFall = timePerFall;
            possibleBlocks.add(TetrisPiece.makeLine());
            possibleBlocks.add(TetrisPiece.makeSquare());
            possibleBlocks.add(TetrisPiece.makeTri());
            possibleBlocks.add(TetrisPiece.makeSShape());
            possibleBlocks.add(TetrisPiece.makeZShape());
            possibleBlocks.add(TetrisPiece.makeLShape());
            possibleBlocks.add(TetrisPiece.makeJShape());

            worldState=new float[this.height * this.width];
            seen = new float[width * height];
            reset();
        }

        public void reset() {
            currentX = width / 2 - 1;
            currentY = 0;
            score = 0;
            for (int i = 0; i < worldState.length; i++) {
                worldState[i] = 0;
            }
            currentRotation = 0;
            is_game_over = false;

            spawnBlock();
            running = true;

        }

        private void toVector(boolean monochrome, float[] target) {
            //eget observation with only the state space

            Arrays.fill(target, -1);

            int x = 0;
            for (double i : worldState) {
                if (monochrome)
                    target[x] = i > 0 ? 1.0f : -1.0f;
                else
                    target[x] = i > 0 ? (float)i : - 1.0f;
                x++;
            }

            writeCurrentBlock(target, 0.5f);


    //            //Set the bit vector value for which block is currently following
    //            target[worldState.length + currentBlockId] = 1;

        }


        private void writeCurrentBlock(float[] f, float color) {
            int[][] thisPiece = possibleBlocks.get(currentBlockId).getShape(currentRotation);

            if (color == -1)
                color = currentBlockId + 1;
            for (int y = 0; y < thisPiece[0].length; ++y) {
                for (int x = 0; x < thisPiece.length; ++x) {
                    if (thisPiece[x][y] != 0) {
                        //Writing currentBlockId +1 because blocks are 0 indexed, and we want spots to be
                        //0 if they are clear, and >0 if they are not.
                        int linearIndex = i(currentX + x, currentY + y);
                        /*if(linearIndex<0){
                            System.err.printf("Bogus linear index %d for %d + %d, %d + %d\n",linearIndex,currentX,x,currentY,y);
                            Thread.dumpStack();
                            System.exit(1);
                        }*/
                        f[linearIndex] = color;
                    }
                }
            }

        }

        public boolean gameOver() {
            return is_game_over;
        }

        /* This code applies the action, but doesn't do the default fall of 1 square */
        public synchronized boolean act(int theAction) {


            int nextRotation = currentRotation;
            int nextX = currentX;
            int nextY = currentY;

            switch (theAction) {
                case CW:
                    nextRotation = (currentRotation + 1) % 4;
                    break;
                case CCW:
                    nextRotation = (currentRotation - 1);
                    if (nextRotation < 0) {
                        nextRotation = 3;
                    }
                    break;
                case LEFT:
                    nextX = currentX - 1;
                    break;
                case RIGHT:
                    nextX = currentX + 1;
                    break;
                case FALL:
                    nextY = currentY;

                    boolean isInBounds = true;
                    boolean isColliding = false;

                    //Fall until you hit something then back up once
                    while (isInBounds && !isColliding) {
                        nextY++;
                        isInBounds = inBounds(nextX, nextY, nextRotation);
                        if (isInBounds) {
                            isColliding = colliding(nextX, nextY, nextRotation);
                        }
                    }
                    nextY--;
                    break;
                default:
                    throw new RuntimeException("unknown action");
            }


            return act(nextRotation, nextX, nextY);
        }

        protected boolean act() {
            return act(currentRotation, currentX, currentY);
        }

        protected boolean act(int nextRotation, int nextX, int nextY) {

            //Check if the resulting position is legal. If so, accept it.
            //Otherwise, don't change anything
            if (inBounds(nextX, nextY, nextRotation)) {
                if (!colliding(nextX, nextY, nextRotation)) {
                    currentRotation = nextRotation;
                    currentX = nextX;
                    currentY = nextY;
                    return true;
                }
            }

            return false;
        }

        /**
         * Calculate the learn array position from (x,y) components based on
         * worldWidth.
         * Package level access so we can use it in tests.
         * @param x
         * @param y
         * @return
         */
        private final int i(int x, int y) {
            return y * width + x;
            //assert returnValue >= 0 : " "+y+" * "+worldWidth+" + "+x+" was less than 0.";
            //return returnValue;
        }
    //    final int x(int i) {
    //        return i % width;
    //    }
    //    final int y(int i) {
    //        return i / width;
    //    }

        /**
         * Check if any filled part of the 5x5 block array is either out of bounds
         * or overlapping with something in wordState
         * @param checkX X location of the left side of the 5x5 block array
         * @param checkY Y location of the top of the 5x5 block array
         * @param checkOrientation Orientation of the block to check
         * @return
         */
        private boolean colliding(int checkX, int checkY, int checkOrientation) {
            int[][] thePiece = possibleBlocks.get(currentBlockId).getShape(checkOrientation);
            int ll = thePiece.length;
            try {

                for (int y = 0; y < thePiece[0].length; ++y) {
                    for (int x = 0; x < ll; ++x) {
                        if (thePiece[x][y] != 0) {
                            //First check if a filled in piece of the block is out of bounds!
                            //if the height of this square is negative or the X of
                            //this square is negative, then we're "colliding" with the wall
                            if (checkY + y < 0 || checkX + x < 0) {
                                return true;
                            }

                            //if the height of this square is more than the board size or the X of
                            //this square is more than the board size, then we're "colliding" with the wall
                            if (checkY + y >= height || checkX + x >= width) {
                                return true;
                            }

                            //Otherwise check if it hits another piece
                            int linearArrayIndex = i(checkX + x, checkY + y);
                            if (worldState[linearArrayIndex] != 0) {
                                return true;
                            }
                        }
                    }
                }
                return false;

            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Error: ArrayIndexOutOfBoundsException in GameState::colliding called with params: " + checkX + " , " + checkY + ", " + checkOrientation);
                System.err.println("Error: The Exception was: " + e);
                Thread.dumpStack();
                System.err.println("Returning true from colliding to help save from error");
                System.err.println("Setting is_game_over to true to hopefully help us to recover from this problem");
                is_game_over = true;
                return true;
            }
        }

        private boolean collidingCheckOnlySpotsInBounds(int checkX, int checkY, int checkOrientation) {
            int[][] thePiece = possibleBlocks.get(currentBlockId).getShape(checkOrientation);
            int ll = thePiece.length;
            try {

                for (int y = 0; y < thePiece[0].length; ++y) {
                    for (int x = 0; x < ll; ++x) {
                        if (thePiece[x][y] != 0) {

                            //This checks to see if x and y are in bounds
                            if ((checkX + x >= 0 && checkX + x < width && checkY + y >= 0 && checkY + y < height)) {
                                //This array location is in bounds
                                //Check if it hits another piece
                                int linearArrayIndex = i(checkX + x, checkY + y);
                                if (worldState[linearArrayIndex] != 0) {
                                    return true;
                                }
                            }
                        }
                    }
                }
                return false;

            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Error: ArrayIndexOutOfBoundsException in GameState::collidingCheckOnlySpotsInBounds called with params: " + checkX + " , " + checkY + ", " + checkOrientation);
                System.err.println("Error: The Exception was: " + e);
                Thread.dumpStack();
                System.err.println("Returning true from colliding to help save from error");
                System.err.println("Setting is_game_over to true to hopefully help us to recover from this problem");
                is_game_over = true;
                return true;
            }
        }

        /**
         * This function checks every filled part of the 5x5 block array and sees if
         * that piece is in bounds if the entire block is sitting at (checkX,checkY)
         * on the board.
         * @param checkX X location of the left side of the 5x5 block array
         * @param checkY Y location of the top of the 5x5 block array
         * @param checkOrientation Orientation of the block to check
         * @return
         */
        private boolean inBounds(int checkX, int checkY, int checkOrientation) {
            try {
                int[][] thePiece = possibleBlocks.get(currentBlockId).getShape(checkOrientation);

                for (int y = 0; y < thePiece[0].length; ++y) {
                    for (int x = 0; x < thePiece.length; ++x) {
                        if (thePiece[x][y] != 0) {
                            //if ! (thisX is non-negative AND thisX is less than width
                            // AND thisY is non-negative AND thisY is less than height)
                            //Through demorgan's law is
                            //if thisX is negative OR thisX is too big or
                            //thisY is negative OR this Y is too big
                            if (!(checkX + x >= 0 && checkX + x < width && checkY + y >= 0 && checkY + y < height)) {
                                return false;
                            }
                        }
                    }
                }

                return true;
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Error: ArrayIndexOutOfBoundsException in GameState::inBounds called with params: " + checkX + " , " + checkY + ", " + checkOrientation);
                System.err.println("Error: The Exception was: " + e);
                Thread.dumpStack();
                System.err.println("Returning false from inBounds to help save from error.  Not sure if that's wise.");
                System.err.println("Setting is_game_over to true to hopefully help us to recover from this problem");
                is_game_over = true;
                return false;
            }

        }

        public boolean nextInBounds() {
            return inBounds(currentX, currentY + 1, currentRotation);
        }

        public boolean nextColliding() {
            return colliding(currentX, currentY + 1, currentRotation);
        }

        /*Ok, at this point, they've just taken their action.  We now need to make them fall 1 spot, and check if the game is over, etc */
        private void update() {
            act();
            time++;

            // Sanity check.  The game piece should always be in bounds.
            if (!inBounds(currentX, currentY, currentRotation)) {
                System.err.println("In GameState.Java the Current Position of the board is Out Of Bounds... Consistency Check Failed");
            }

            //Need to be careful here because can't check nextColliding if not in bounds

            //onSomething means we're basically done with this piece
            boolean onSomething = false;
            if (!nextInBounds()) {
                onSomething = true;
            }
            if (!onSomething) {
                if (nextColliding()) {
                    onSomething = true;
                }
            }

            if (onSomething) {
                running = false;
                writeCurrentBlock(worldState, -1);
            } else {
                //fall
                if (time % timePerFall == 0)
                    currentY += 1;
            }

        }

        public int spawnBlock() {
            running = true;

            currentBlockId = nextBlock();

            currentRotation = 0;
            currentX = (width / 2) - 2;
            currentY = -4;

            //score += getWidth() / 2;

    //Colliding checks both bounds and piece/piece collisions.  We really only want the piece to be falling
    //If the filled parts of the 5x5 piece are out of bounds.. IE... we want to stop falling when its all on the screen
            boolean hitOnWayIn = false;
            while (!inBounds(currentX, currentY, currentRotation)) {
                //We know its not in bounds, and we're bringing it in.  Let's see if it would have hit anything...
                hitOnWayIn = collidingCheckOnlySpotsInBounds(currentX, currentY, currentRotation);
                currentY++;
            }
            is_game_over = colliding(currentX, currentY, currentRotation) || hitOnWayIn;
            if (is_game_over) {
                running = false;
            }

            return currentBlockId;
        }

        protected int nextBlock() {
            return randomGenerator.nextInt(possibleBlocks.size());
        }

        public void checkScore() {
            int numRowsCleared = 0;
            int rowsFilled = 0;


            //Start at the bottom, work way up
            for (int y = height - 1; y >= 0; --y) {
                if (isRow(y, true)) {
                    removeRow(y);
                    numRowsCleared += 1;
                    y += 1;
                } else {
                    if (!isRow(y,false))
                        rowsFilled++;
                }
            }

            int prevRows = this.rowsFilled;
            this.rowsFilled = rowsFilled;


            if (numRowsCleared > 0) {
                //1 line == 1
                //2 lines == 2
                //3 lines == 4
                //4 lines == 8
                //score = numRowsCleared;
            } else {
                //score = 0;
            }
            //score -= (((float)rowsFilled) / height) * 0.5f; //penalty for height


            int diff = prevRows - rowsFilled;

            if (diff >= height-1) {
                //die
                score = Float.NaN;
                        //-prevRows;
            } else {
//                if (diff == 0) score = Float.NaN; //no score
//                else
                    score = diff;
            }
        }

        public float height() {
            return (((float)rowsFilled) / height);
        }

        /**
         * Check if a row has been completed at height y.
         * Short circuits, returns false whenever we hit an unfilled spot.
         * @param y
         * @return
         */
        public boolean isRow(int y, boolean filledOrClear) {
            for (int x = 0; x < width; ++x) {
                float s = worldState[i(x, y)];
                if (filledOrClear ? (s==0) : (s != 0)) {
                    return false;
                }
            }
            return true;
        }



        /**
         * Dec 13/07.  Radkie + Tanner found 2 bugs here.
         * Bug 1: Top row never gets updated when removing lower rows. So, if there are
         * pieces in the top row, and we clear something, they will float there.
         * @param y
         */
        void removeRow(int y) {
            if (!isRow(y, true)) {
                System.err.println("In GameState.java remove_row you have tried to remove a row which is not complete. Failed to remove row");
                return;
            }

            for (int x = 0; x < width; ++x) {
                int linearIndex = i(x, y);
                worldState[linearIndex] = 0;
            }

            //Copy each row down one (except the top)
            for (int ty = y; ty > 0; --ty) {
                for (int x = 0; x < width; ++x) {
                    int linearIndexTarget = i(x, ty);
                    int linearIndexSource = i(x, ty - 1);
                    worldState[linearIndexTarget] = worldState[linearIndexSource];
                }
            }

            //Clear the top row
            for (int x = 0; x < width; ++x) {
                int linearIndex = i(x, 0);
                worldState[linearIndex] = 0;
            }

        }

    //    public int numEmptyRows() {
    //        int t = 0;
    //        for (int y = 0; y < getHeight(); y++)
    //            if (isRow(y,false))
    //                t++;
    //        return t;
    //    }


        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

    //    public int[] getNumberedStateSnapShot() {
    //        int[] numberedStateCopy = new int[worldState.length];
    //        for (int i = 0; i < worldState.length; i++) {
    //            numberedStateCopy[i] = worldState[i];
    //        }
    //        writeCurrentBlock(numberedStateCopy);
    //        return numberedStateCopy;
    //
    //    }

        public int getCurrentPiece() {
            return currentBlockId;
        }

        /**
         * Utility methd for debuggin
         *
         */
        public void printState() {
            int index = 0;
            for (int i = 0; i < height - 1; i++) {
                for (int j = 0; j < width; j++) {
                    System.out.print(worldState[i * width + j]);
                }
                System.out.print("\n");
            }
            System.out.println("-------------");


        }

    //    private Random getRandom() {
    //        return randomGenerator;
    //    }

        protected float next() {
            if (running) {
                update();
            } else {
                spawnBlock();
            }

            checkScore();

            toVector(false, seen);

            if (gameOver()) {
                die();
            }

            return score();

        }

        private float score() {
            return score;
        }

        protected void die() {
            reset();
        }

    //    /*End of Tetris Helper Functions*/
    //
    //    public TetrisState(TetrisState stateToCopy) {
    //        blockMobile = stateToCopy.blockMobile;
    //        currentBlockId = stateToCopy.currentBlockId;
    //        currentRotation = stateToCopy.currentRotation;
    //        currentX = stateToCopy.currentX;
    //        currentY = stateToCopy.currentY;
    //        score = stateToCopy.score;
    //        is_game_over = stateToCopy.is_game_over;
    //        width = stateToCopy.width;
    //        height = stateToCopy.height;
    //
    //        worldState = new float[stateToCopy.worldState.length];
    //        System.arraycopy(stateToCopy.worldState, 0, worldState, 0, worldState.length);
    //
    //        possibleBlocks = new Vector<>();
    //        //hopefully nobody modifies the pieces as they go
    //        possibleBlocks.addAll(stateToCopy.possibleBlocks.stream().collect(Collectors.toList()));
    //
    //    }
    }
}
//                AutoClassifier ac = new AutoClassifier($.the("row"), nar, sensors,
//                        tetris_width/2, 7 /* states */,
//                        0.05f);
//                view.autoenc = new MatrixView(ac.W.length, ac.W[0].length, arrayRenderer(ac.W));

//                int totalSize = tetris_width*tetris_height;
//                AutoClassifier bc = new AutoClassifier($.the("row4"), nar, sensors,
//                        tetris_width*4, 16,
//                        0.1f);

//                newControlWindow(
//                        new GridSurface(VERTICAL,
//                                charted.stream().map(c -> new BeliefTableChart(nar, c)).collect(toList())
//                        )
//                );


//window(Vis.concepts(nar, 1024), 500, 500);

//STMView.show(stm, 800, 600);
//
//        window(
//                col(
//
//                        //Vis.concepts(nar, 32),
//                        //Vis.agentActions(t, 1200),
//
//                        Vis.budgetHistogram(nar, 24)
//                        /*Vis.conceptLinePlot(nar,
//                                Iterables.concat(t.actions, Lists.newArrayList(t.happy, t.joy)),
//                                600)*/
//                ), 1200, 900);



            /*view.plot2 = new GridSurface(HORIZONTAL,
                //conceptLinePlot(nar, Lists.newArrayList( t.happy, t.joy ), (c) -> nar.conceptPriority(c), 256),

                conceptLinePlot(nar, t.actions, (c) -> {
                    try {
                        return nar.concept(c).goals().truth(nar.time()).freq();
                    } catch (NullPointerException npe) {
                        return 0.5f;
                    }
                }, 256)
            );*/

//                {
//                    List<FloatSupplier> li = new ArrayList();
//                    for (int i = 0; i < sensors.size(); i++) {
//                        li.add(sensors.get(i).getInput());
//                    }
//
//                    List<FloatSupplier> lo = new ArrayList();
//                    RangeNormalizedFloat normReward = new RangeNormalizedFloat(() -> rewardValue);
//                    lo.add(normReward);
////
////
//                    LSTMPredictor lp = new LSTMPredictor(li, lo, 1);
////
//
//                    double[] lpp = new double[2];
//                    nar.onFrame(nn -> {
//                        double[] p = lp.next();
//                        System.arraycopy(p, 0, lpp, 0, p.length);
//                        //System.out.println("LSTM: " + Texts.n4(p) + " , " + normReward.asFloat());
//                    });
//
//                    view.lstm = new Plot2D(plotHistory, Plot2D.Line)
//                            .add("Reward (actual)", () -> normReward.asFloat())
//                            .add("Predicted", () -> lpp[0]);
//
//
//                }


//        nar.onFrame(f -> {
//            //view.lstm.update();
//            try {
//                view.term.term.putLinePre(t.summary());
//            } catch (IOException e1) {
//            }
//        });


//                int window = 32;
//                GridSurface camHistory = new GridSurface(HORIZONTAL,
//                        new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, -window * 2)),
//                        new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, -window)),
//                        new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, 0)),
//                        new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, +window)),
//                        new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, +window * 2))
//                );
//                newControlWindow(12f,4f, new Object[] { camHistory } );

//newControlWindow(view);


//newControlWindow(2f,4f, new Object[] { new MatrixView(tetris_width, tetris_height, sensorMatrixView(nar, 0)) } );

//Vis.newBeliefChartWindow(t, 200);

//window(Vis.budgetHistogram(nar, 30), 500, 300);

//Arkancide.newBeliefChartWindow(nar, 200, nar.inputTask("(&&, ((happy) ==>+0 (joy)), ((joy) ==>+0 (happy)), ((happy) <=>+0 (joy))). :|:").term());

//                BeliefTableChart.newBeliefChart(nar, Lists.newArrayList(
//                        sensors.get(0),
//                        sensors.get(1),
//                        sensors.get(2),
//                        sensors.get(3),
//                        sensors.get(4),
//                        sensors.get(5)
//                ), 200);

//NARSpace.newConceptWindow((Default) nar, 32, 8);


//        Iterable<Termed> cheats = Iterables.concat(
//                numericSensor(() -> t.currentX, nar, 0.3f,
//                        "(cursor_x)")
//                        //"(active,a)","(active,b)","(active,c)","(active,d)","(active,e)","(active,f)","(active,g)","(active,h)")
//                        //"I(a)","I(b)","I(c)","I(d)","I(e)","I(f)","I(g)","I(h)")
//                        //"(active,x)")
//                        .resolution(0.5f / t.width),
//                numericSensor(() -> t.currentY, nar, 0.3f,
//                        "(cursor_y)")
//                        //"active:(y,t)", "active:(y,b)")
//                        //"(active,y)")
//                        .resolution(0.5f / t.height)
//        );

//        NAgent n = new NAgent(nar) {
//            @Override
//            public void start(int inputs, int actions) {
//                super.start(inputs, actions);
//
//                List<Termed> charted = new ArrayList(super.actions);
//
//                charted.add(sad);
//                charted.add(happy);
//                Iterables.addAll(charted, cheats);
//
//                if (nar instanceof Default) {
//
//                    new BeliefTableChart(nar, charted).show(600, 900);
//
//                    //BagChart.show((Default) nar, 128);
//
//                    //STMView.show(stm, 800, 600);
//
//
//                    NARSpace.newConceptWindow((Default) nar, 128, 8);
//                }
//
//
//            }
//
//            @Override
//            protected Collection<Task> perceive(Set<Task> inputs) {
//                return super.perceive(inputs);
//            }
//        };


//addCamera(t, nar, 8, 8);


//t.run(runFrames);
//t.run(20000);
