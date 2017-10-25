package nars.control;

import jcog.Util;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.derive.AbstractPred;
import nars.derive.PrediTerm;
import nars.derive.PrediTrie;
import nars.derive.TrieDeriver;
import nars.derive.instrument.DebugDerivationPredicate;
import nars.derive.rule.PremiseRuleSet;
import nars.index.term.PatternIndex;
import nars.task.ITask;

import java.io.PrintStream;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * an individual deriver process: executes a particular Deriver model
 * specified by a set of premise rules.
 * <p>
 * runtime intensity is metered and throttled by causal feedback
 */
public class Deriver extends NARService {

    public static Function<NAR, Deriver> deriver(Function<NAR, PremiseRuleSet> rules) {
        return (nar) ->
                new Deriver(PrediTrie.the(rules.apply(nar),
                        Param.TRACE ? DebugDerivationPredicate::new : null
                ), nar);
    }

    public static Function<NAR, Deriver> deriver(int nal, String... additional) {
        assert (nal > 0 || additional.length > 0);

        return deriver(nar ->
                PremiseRuleSet.rules(nar, new PatternIndex(),
                        Derivers.defaultRules(nal, additional)
                ));
    }

    public final PrediTerm<Derivation> deriver;
    private final NAR nar;
    private final Causable can;
    private final CauseChannel<ITask> cause;

    private float minPremisesPerConcept = 2;
    private float maxPremisesPerConcept = 5;

    protected Deriver(NAR nar, String... rulesets) {
        this(PrediTrie.the(
                new PremiseRuleSet(
                        new PatternIndex(), nar, rulesets
                )), nar);
    }

    protected Deriver(PrediTerm<Derivation> deriver, NAR nar) {
        super(nar);
        this.deriver = deriver;
        this.nar = nar;

        this.cause = nar.newCauseChannel(this);
        this.can = new Causable(nar) {
            @Override
            protected int next(NAR n, int iterations) {
                return Deriver.this.run(iterations);
            }

            @Override
            public float value() {
                return cause.value();
            }
        };
    }


    protected int run(int work) {
        NAR nar = this.nar;
        Derivation d = derivation.get().cycle(nar, deriver);

        int matchTTL = Param.TTL_PREMISE_MIN * 3;
        int ttlMin = nar.matchTTLmin.intValue();
        int ttlMax = nar.matchTTLmax.intValue();
        final int conceptBatch = 8;

        BatchActivation activator = BatchActivation.get();

        final int[] derivations = {0};

        int derivationsBefore;
        while ((derivationsBefore = derivations[0]) < work) {

            nar.exe.fire(Math.min(work - derivations[0], conceptBatch), a -> {

                int hh = Math.min(work - derivations[0], premises(a));
                if (hh == 0)
                    return false;
                Iterable<Premise> h = a.hypothesize(nar, activator, hh);

                if (h != null) {

                    for (Premise p : h) {

                        if (p.match(d, matchTTL) != null) {

                            int deriveTTL = Util.lerp(Util.unitize(
                                    p.task.priElseZero() / nar.priDefault(p.task.punc())),
                                    ttlMin, ttlMax);

                            d.derive(deriveTTL);
                            derivations[0]++;
                        }
                    }
                } else {
                    //premise miss
                }

                return derivations[0] > 0;
            });
            if (derivations[0] == derivationsBefore)
                break; //nothing happened
        }

        int derived = d.commit(cause::input);
        activator.commit(nar);

        return derivations[0];
    }


    private int premises(Activate a) {
        return Math.round(Util.lerp(a.priElseZero(), minPremisesPerConcept, maxPremisesPerConcept));
    }


    public static final Function<NAR, PrediTerm<Derivation>> NullDeriver = (n) -> new AbstractPred<Derivation>(Op.Null) {
        @Override
        public boolean test(Derivation derivation) {
            return true;
        }
    };


    public static Stream<Deriver> derivers(NAR n) {
        return n.services().filter(Deriver.class::isInstance).map(Deriver.class::cast);
    }

    public static void print(NAR n, PrintStream p) {
        derivers(n).forEach(d -> {
            p.println(d.toString());
            TrieDeriver.print(d.deriver, p);
            p.println();
        });
    }

    //    public final IterableThreadLocal<Derivation> derivation =
//            new IterableThreadLocal<>(() -> new Derivation(this));
    public static final ThreadLocal<Derivation> derivation =
            ThreadLocal.withInitial(Derivation::new);

}


//    /**
//     * for now it seems there is a leak so its better if each NAR gets its own copy. adds some overhead but we'll fix this later
//     * not working yet probably due to unsupported ellipsis IO codec. will fix soon
//     */
//    static PremiseRuleSet DEFAULT_RULES_cached() {
//
//
//        return new PremiseRuleSet(
//                Stream.of(
//                        "nal1.nal",
//                        //"nal4.nal",
//                        "nal6.nal",
//                        "misc.nal",
//                        "induction.nal",
//                        "nal2.nal",
//                        "nal3.nal"
//                ).flatMap(x -> {
//                    try {
//                        return rulesParsed(x);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    return Stream.empty();
//                }), new PatternTermIndex(), true);
//    }


//    PremiseRuleSet DEFAULT_RULES = PremiseRuleSet.rules(true,
//                "nal1.nal",
//                //"nal4.nal",
//                "nal6.nal",
//                "misc.nal",
//                "induction.nal",
//                "nal2.nal",
//                "nal3.nal"
//        );


//    Cache<String, Deriver> derivers = Caffeine.newBuilder().builder();
//    Function<String,Deriver> loader = (s) -> new TrieDeriver(PremiseRuleSet.rules(s));

//    @NotNull
//    static Deriver get(String... path) {
//        PremiseRuleSet rules = PremiseRuleSet.rules(true, path);
//        return TrieDeriver.get(rules);
//    }


//    Logger logger = LoggerFactory.getLogger(Deriver.class);
//
//    BiConsumer<Stream<Compound>, DataOutput> encoder = (x, o) -> {
//        try {
//            IO.writeTerm(x, o);
//            //o.writeUTF(x.getTwo());
//        } catch (IOException e) {
//            throw new RuntimeException(e); //e.printStackTrace();
//        }
//    };
//
//
//    @NotNull
//    static Stream<Pair<PremiseRule, String>> rulesParsed(String ruleSet) throws IOException, URISyntaxException {
//
//        PatternTermIndex p = new PatternTermIndex();
//
//        Function<DataInput, PremiseRule> decoder = (i) -> {
//            try {
//                return //Tuples.pair(
//                        (PremiseRule) readTerm(i, p);
//                //,i.readUTF()
//                //);
//            } catch (IOException e) {
//                throw new RuntimeException(e); //e.printStackTrace();
//                //return null;
//            }
//        };
//
//
//        URL path = NAR.class.getResource("nal/" + ruleSet);
//
//        Stream<PremiseRule> parsed =
//                FileCache.fileCache(path, PremiseRuleSet.class.getSimpleName(),
//                        () -> load(ruleSet),
//                        encoder,
//                        decoder,
//                        logger
//                );
//
//        return parsed.map(x -> Tuples.pair(x, "."));
//    }
//
//    static Stream<PremiseRule> load(String ruleFile) {
//        return parsedRules(new PatternTermIndex(), ruleFile).map(Pair::getOne /* HACK */);
//    }
