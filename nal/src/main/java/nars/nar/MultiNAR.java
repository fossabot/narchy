package nars.nar;


import jcog.Loop;
import jcog.Util;
import nars.$;
import nars.NAR;
import nars.NARLoop;
import nars.conceptualize.DefaultConceptBuilder;
import nars.index.term.map.CaffeineIndex;
import nars.task.ITask;
import nars.time.Time;
import nars.util.exe.Executioner;
import nars.util.exe.TaskExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.util.concurrent.ForkJoinPool.defaultForkJoinWorkerThreadFactory;

/**
 * multithreaded recursive cluster of NAR's
 * <sseehh> any hierarchy can be defined including nars within nars within nars
 * <sseehh> each nar runs in its own thread
 * <sseehh> they share concepts
 * <sseehh> but not the importance of concepts
 * <sseehh> each one has its own concept attention
 * <sseehh> link attention is currently shared but ill consider if this needs changing
 */
public class MultiNAR extends NAR {


    public final List<SubExecutor> sub = $.newArrayList();

    //private AffinityExecutor pool;
    private List<Loop> loops;


    /** foreground: the independent, preallocated, high frequency worker threads ; a fixed threadpool */
    private ExecutorService working;


    /** background: misc tasks to finish before starting next cycle */
    final static int passiveThreads = 2;
    final ForkJoinPool passive;

    MultiNAR(@NotNull Time time, @NotNull Random rng, Executioner e) {
        this(time, rng, new ForkJoinPool(passiveThreads, defaultForkJoinWorkerThreadFactory,
                    null, true /* async */), e);
    }

    MultiNAR(@NotNull Time time, @NotNull Random rng, ForkJoinPool passive, Executioner e) {
        super(new CaffeineIndex(new DefaultConceptBuilder(), 256*1024, passive) {

//                  @Override
//                  protected void onBeforeRemove(Concept c) {
//
//                      //victimize neighbors
//                      PriReference<Term> mostComplex = c.termlinks().maxBy((x -> x.get().volume()));
//                      if (mostComplex!=null) shrink(mostComplex.get());
//
//                      PriReference<Task> mostComplexTa = c.tasklinks().maxBy((x -> x.get().volume()));
//                      if (mostComplexTa!=null) shrink(mostComplexTa.get());
//
//                  }
//
//                  private void shrink(Term term) {
//                      Concept n = nar.concept(term);
//                      if (n != null) {
//                          shrink(n);
//                      }
//                  }
//
//                  private void shrink(Task task) {
//                      Concept n = task.concept(nar);
//                      if (n != null) {
//                          shrink(n);
//                      }
//                  }
//
//                  private void shrink(Concept n) {
//                      int ntl = n.termlinks().capacity();
//                      if (ntl > 0) {
//                          n.termlinks().setCapacity(ntl - 1);
//                      }
//                  }
//
//

              }, e, time,
            //new HijackTermIndex(new DefaultConceptBuilder(), 128 * 1024, 4),
                rng);

        this.passive = passive;


    }

//    @Override
//    protected PSinks newInputMixer() {
//        MixContRL<ITask> r = new MixContRL<>(20f,
//                null,
//
//                FloatAveraged.averaged(emotion.happy.sumIntegrator()::sumThenClear, 1),
//
//                8,
//
//                new EnumClassifier("type", new String[]{
//                        "Belief", "Goal", "Question", "Quest",
//                        "ConceptFire"
//                }, (x) -> {
//
//                    if (x instanceof NALTask) {
//                        //NAL
//                        switch (((Task) x).punc()) {
//                            case BELIEF:
//                                return 0;
//                            case GOAL:
//                                return 1;
//                            case QUESTION:
//                                return 2;
//                            case QUEST:
//                                return 3;
//                        }
//                    } else if (x instanceof ConceptFire) {
//                        return 4;
//                    }
//
//                    return -1;
//                }),
//
//                new EnumClassifier("complexity", 3, (t) -> {
//                    if (t instanceof NALTask) {
//                        int c = ((NALTask) t).
//                                volume();
//                                //complexity();
////                        int m = termVolumeMax.intValue();
////                        assert(m > 5);
//                        if (c < 5) return 0;
//                        if (c < 10) return 1;
//                        return 2;
//                    }
//                    return -1;
//                }),
//
//                new EnumClassifier("when", new String[]{"Present", "Future", "Past"}, (t) -> {
//                    if (t instanceof NALTask) {
//                        long now = time();
//                        int radius = 2;
//                        long h = ((NALTask) t).nearestStartOrEnd(now);
//                        if (Math.abs(h - now) <= dur() * radius) {
//                            return 0; //present
//                        } else if (h > now) {
//                            return 1; //future
//                        } else {
//                            return 2; //past
//                        }
//                    }
//                    return -1;
//                }, true)
//
////            new MixRouter.Classifier<>("original",
////                    (x) -> x.stamp().length <= 2),
////            new MixRouter.Classifier<>("unoriginal",
////                    (x) -> x.stamp().length > 2),
//        );
//
//        r.setAgent(
//                new NARMixAgent<>(new NARBuilder()
//                        .index(
//                                new HijackTermIndex(new DefaultConceptBuilder(), 8*1024, 3)
//                                //new CaffeineIndex(new DefaultConceptBuilder(), -1, MoreExecutors.newDirectExecutorService())
//                        ).get(), r, this)
//
//                //new HaiQMixAgent()
//
//                //new MultiHaiQMixAgent()
//        );
//
//        return r;
//    }


//    @Override
//    public void input(@NotNull ITask partiallyClassified) {
//        ((MixContRL) in).test(partiallyClassified);
//        super.input(partiallyClassified);
//    }

    /**
     * default implementation convenience method
     */
    public void addNAR(int conceptCapacity, int taskCapacity, float conceptRate) {
        synchronized (sub) {
            sub.add( new SubExecutor(conceptCapacity, taskCapacity, conceptRate) );
        }

    }


    private static class RootExecutioner extends Executioner implements Runnable {


        public ForkJoinTask lastCycle;
        private ForkJoinPool passive;



        public RootExecutioner() {

        }

        @Override
        public void start(NAR nar) {
            this.passive = ((MultiNAR)nar).passive;
            super.start(nar);

        }

        @Override
        public void runLater(Runnable cmd) {
            ((MultiNAR)nar).passive.execute(cmd);
        }

        @Override
        public boolean run(@NotNull ITask x) {

            List<SubExecutor> workers = ((MultiNAR) nar).sub;
            int num = workers.size();
            if (num == 0) {
                //HACK do nothing because the workers havent started yet?
                return false;
            }


            int sub =
                    //random.nextInt(num);
                    Math.abs(Util.hashWangJenkins(x.hashCode())) % num;
            //apply(x);
            return workers.get(sub).run(x);
        }


//        public void apply(CLink<? extends ITask> x) {
//            if (x!=null && !x.isDeleted()) {
//                x.priMult(((MixContRL) (((NARS) nar).in)).gain(x));
//            }
//        }

        @Override
        public void stop() {
            lastCycle = null;
            super.stop();
        }

        final AtomicBoolean busy = new AtomicBoolean(false);

        @Override
        public void cycle(@NotNull NAR nar) {


//            int waitCycles = 0;
//            while (!passive.isQuiescent()) {
//                Util.pauseNext(waitCycles++);
//            }

            if (!busy.compareAndSet(false, true))
                return; //already in the cycle

            ((MultiNAR) nar).nextCycle();
            try {

                if (lastCycle != null) {
                    //System.out.println(lastCycle + " " + lastCycle.isDone());
                    if (!lastCycle.isDone()) {
                        long start = System.currentTimeMillis();
                        lastCycle.join(); //wait for lastCycle's to finish
                        logger.info("cycle lag {}", (System.currentTimeMillis() - start) + "ms");
                    }

                    lastCycle.reinitialize();
                    passive.execute(lastCycle);

                    ((MultiNAR) nar).nextCycle();

                } else {
                    lastCycle = passive.submit(this);
                }
            } finally {
                busy.set(false);
            }
        }

        /**
         * dont call directly
         */
        @Override
        public void run() {
            nar.eventCycleStart.emitAsync(nar, passive); //TODO make a variation of this for ForkJoin specifically
        }

        @Override
        public int concurrency() {
            return 2; //TODO calculate based on # of sub-NAR's but definitely is concurrent so we add 1 here in case passive=1
        }

        @Override
        public boolean concurrent() {
            return true;
        }

        @Override
        public void forEach(Consumer<ITask> each) {
            ((MultiNAR) nar).sub.forEach(s -> s.forEach(each));
        }

    }

    protected void nextCycle() {
//        if (!((HijackMemoize)truthCache).isEmpty()) {
//            System.out.println("Truth Cache: " + truthCache.summary());
//        } else {
//            truthCache.summary(); //HACK to call stat reset
//        }
//
//        truthCache.clear();
    }

//    /** temporary 1-cycle old cache of truth calculations */
//    final Memoize<Pair<Termed, ByteLongPair>, Truth> truthCache =
//            new HijackMemoize<>(2048, 3,
//                    k -> {
//                        Truth x = super.truth(k.getOne(), k.getTwo().getOne(), k.getTwo().getTwo());
//                        if (x == null)
//                            return Truth.Null;
//                        return x;
//                    }
//            );
//
//    @Override
//    public @Nullable Truth truth(@Nullable Termed concept, byte punc, long when) {
//        Pair<Termed, ByteLongPair> key = Tuples.pair(concept, PrimitiveTuples.pair(punc, when));
//        Truth t = truthCache.apply(key);
//        if (t == Truth.Null) {
//            return null;
//        }
//        return t;
//        //return truthCache.computeIfAbsent(key, k -> super.truth(k.getOne(), k.getTwo().getOne(), k.getTwo().getTwo()));
//        //return super.truth(concept, punc, when);
//    }


    class SubExecutor extends TaskExecutor {
        public SubExecutor(int conceptCapacity, int inputTaskCapacity, float exePct) {
            super(conceptCapacity, inputTaskCapacity, exePct);
        }

//        @Override
//        protected void actuallyRun(CLink<? extends ITask> x) {
//
//            super.actuallyRun(x);
//
//            ((RootExecutioner) exe).apply(x); //apply gain after running
//
//        }

        @Override
        protected void actuallyFeedback(ITask x, ITask[] next) {
            if (next != null)
                MultiNAR.this.input(next); //through post mix
        }

        @Override
        public void runLater(@NotNull Runnable r) {
            passive.execute(r); //use the common threadpool
        }

        public Loop start() {
            start(MultiNAR.this);
            Loop l = new Loop(0) {
                @Override
                public boolean next() {
                    flush();
                    return true;
                }
            };
            return l;
        }
    }


    public MultiNAR(@NotNull Time time, @NotNull Random rng) {
        this(time, rng, new RootExecutioner());
    }


    @Override
    public NARLoop startPeriodMS(int ms) {
        assert (!this.loop.isRunning());

        synchronized (terms) {

            exe.start(this);

            int num = sub.size();

            this.working = Executors.newFixedThreadPool(num);

            //((ThreadPoolExecutor)pool).getThreadFactory().
            //self().toString();

            this.loops = $.newArrayList(num);
            sub.forEach(s -> loops.add(s.start()));

            loops.forEach(working::execute);
        }

        return super.startPeriodMS(ms);
    }

    @Override
    public void stop() {
        synchronized (terms) {
            if (!this.loop.isRunning())
                return;

            super.stop();

            loops.forEach(Loop::stop);
            this.loops = null;

            this.working.shutdownNow();
            this.working = null;

        }
    }

//    public static void main(String[] args) {
//
//        NARS n = new NARS(
//                new RealTime.DSHalf(true),
//                new XorShift128PlusRandom(1), 2);
//
//
//        n.addNAR(2048);
//        n.addNAR(2048);
//
//        //n.log();
//
//        new DeductiveMeshTest(n, 5, 5);
//
//        n.start();
//
//        for (int i = 0; i < 10; i++) {
//            System.out.println(n.stats());
//            Util.sleep(500);
//        }
//
//        n.stop();
//    }
//

}