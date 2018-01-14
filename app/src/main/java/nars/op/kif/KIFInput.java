/*
 * Copyright (C) 2014 me
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.op.kif;

import jcog.Util;
import nars.*;
import nars.control.MetaGoal;
import nars.op.prolog.PrologCore;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.var.Variable;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static nars.Op.*;
import static nars.op.rdfowl.NQuadsRDF.disjoint;
import static nars.op.rdfowl.NQuadsRDF.equi;
import static nars.time.Tense.ETERNAL;

/**
 * http://sigmakee.cvs.sourceforge.net/viewvc/sigmakee/sigma/suo-kif.pdf
 * http://sigma-01.cim3.net:8080/sigma/Browse.jsp?kb=SUMO&lang=EnglishLanguage&flang=SUO-KIF&term=subclass
 * https://raw.githubusercontent.com/ontologyportal/sumo/master/Merge.kif
 * https://raw.githubusercontent.com/ontologyportal/sumo/master/Mid-level-ontology.kif
 **/
public class KIFInput implements Runnable {

    private final KIF kif;



    private final NAR nar;

    private final PrintStream output;

    private final boolean includeSubclass = true;
    private final boolean includeInstance = true;
    private final boolean includeRelatedInternalConcept = true;
    private final boolean includeDisjoint = true;
    private final boolean includeDoc = false;

    public KIFInput(NAR nar, String kifPath) throws Exception {
        this.nar = nar;
        this.kif = new KIF(kifPath);
        this.output = System.out;
    }

    public void start() {
        new Thread(this).start();
    }


    final Map<Term, FnDef> fn = new HashMap();

    static class FnDef {
        final IntObjectHashMap<Term> domain = new IntObjectHashMap();
        Term range;
    }

    @Override
    public void run() {
        Set<Term> beliefs = new TreeSet();
        kif.formulas().forEach(x->{

            try {
                Term y = formulaToTerm(x);
                if (y != null) {
                    beliefs.add(y);
                }
            } catch (Exception e) {
                logger.error("{} {}", x, e.getMessage());
            }

            //  => Implies
            //  <=> Equivalance
            /*Unknown operators: {=>=466, rangeSubclass=5, inverse=1, relatedInternalConcept=7, documentation=128, range=29, exhaustiveAttribute=1, trichotomizingOn=4, subrelation=22, not=2, partition=12, contraryAttribute=1, subAttribute=2, disjoint=5, domain=102, disjointDecomposition=2, domainSubclass=9, <=>=70}*/
        });

        fn.forEach((f, s) -> {
            int ds = s.domain.isEmpty() ? 0 : s.domain.keySet().max();
            Term[] vt =  Util.map(0, ds, i -> $.varIndep(1 + i), Term[]::new);
            Term v = null;
            if (s.range!=null) {
                v = $.varIndep("R");
                vt = ArrayUtils.add(vt, v);
            }
            Term[] typeConds = Util.map(0, ds, i ->
                    $.inh($.varIndep(1 + i),
                            s.domain.getIfAbsent(1 + i, () -> True)), Term[]::new);
            if (s.range!=null) {
                typeConds = ArrayUtils.add(typeConds, $.inh(v, s.range));
            }
            Term types = CONJ.the(
                    typeConds
            );
            Term fxy = impl($.inh($.p( vt ), f), types, true);
            if (fxy instanceof Bool) {
                logger.error("bad function {} {} {}", f, s.domain, s.range);
            } else {
                beliefs.add(fxy);
            }

        });

        //nar.input( beliefs.stream().map(x -> task(x)) );

//        long[] stamp = { new Random().nextLong() };
        for (Term x : beliefs) {
            output.println(x + ".");
            try {
                nar.believe(x);
            } catch (Exception e) {
                logger.error("{} {}", e.getMessage(), x);
            }
//            try {
//                nar.input("$0.01$ " + x + ".");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }

        //nar.believe(y);
    }

    public Term formulaToTerm(String sx) {
        sx = sx.replace("?", "#"); //query var to indepvar HACK

        Formula f = Formula.the(sx);
        if (f != null)
            return formulaToTerm(f);
        else
            try {
                return $.$(sx);
            } catch (Narsese.NarseseException e) {
                return $.quote(sx);
            }
    }

    static final Logger logger = LoggerFactory.getLogger(KIFInput.class);

    public Term formulaToTerm(final Formula x) {
        if (x.theFormula.contains("@ROW"))
            return null; //ignore @ROW stuff

        String root = x.car(); //root operate

        int l = x.listLength();
        if (l == 1)
            return formulaToTerm(root);

        List<String> sargs = IntStream.range(1, l).mapToObj(x::getArgument).collect(Collectors.toList());
        List<Term> args = sargs != null ? sargs.stream().map(this::formulaToTerm).collect(Collectors.toList()) : Collections.emptyList();

        assert(!args.isEmpty()); //should have been handled first

        /**
         *
         *
         * https://github.com/opencog/opencog/blob/04db8e557a2d67da9025fe455095d2cda0261ea7/opencog/python/sumo/sumo.py
         * def special_link_type(predicate):
         mapping = {
         '=>':types.ImplicationLink,
         '<=>':types.EquivalenceLink,
         'and':types.AndLink,
         'or':types.OrLink,
         'not':types.NotLink,
         'instance':types.MemberLink,
         # This might break some of the formal precision of SUMO, but who cares
         'attribute':types.InheritanceLink,
         'member':types.MemberLink,
         'subclass':types.InheritanceLink,
         'exists':types.ExistsLink,
         'forall':types.ForAllLink,
         'causes':types.PredictiveImplicationLink
         *
         */

        Term y = null;
        switch (root) {
            case "subrelation":
            case "subclass":
            case "subAttribute":
                if (includeSubclass) {
                    if (args.size() != 2) {
                        System.err.println("subclass expects 2 arguments");
                    } else {
                        y = INH.the(args.get(0), args.get(1));
                    }
                }
                break;
            case "instance":
                if (includeInstance) {
                    if (args.size() != 2) {
                        System.err.println("instance expects 2 arguments");
                    } else {
                        y = //$.inst
                            $.inh
                                (args.get(0), args.get(1));
                    }
                }
                break;
            case "relatedInternalConcept":
                /*(documentation relatedInternalConcept EnglishLanguage "Means that the two arguments are related concepts within the SUMO, i.e. there is a significant similarity of meaning between them. To indicate a meaning relation between a SUMO concept and a concept from another source, use the Predicate relatedExternalConcept.")            */
                if (includeRelatedInternalConcept) {
                    if (args.size() != 2) {
                        throw new UnsupportedOperationException("relatedInternalConcept expects 2 arguments");
                    } else {
                        y = $.sim(args.get(0), args.get(1));
                    }
                }
                break;

            case "equal":
                y = $.func("equal", args.get(0), args.get(1));
                //y = $.sim(args.get(0), args.get(1));
                break;
            case "disjointRelation":
            case "disjoint":

                //TODO represent disjoint with a pair of implications, not this
                if (includeDisjoint) {
                    y = disjoint(args.get(0), args.get(1));
                }
                break;

            case "forall":
                y = impl(args.get(0), args.get(1), true);
                break;
            case "exists":
                y = args.get(1); //skip over the first parameter, since depvar is inherently existential
                break;
            case "=>":
                y = impl(args.get(0), args.get(1), true);
                break;
            case "<=>":
                y = impl(args.get(0), args.get(1), false);
                break;

            case "termFormat":
                String language = args.get(0).toString();
                language = language.replace("Language", "");

                Term term = args.get(1);
                Term string = args.get(2);
                y = $.inh($.p($.the(language), string), term);
                break;

            case "domain":
                //TODO use the same format as Range, converting quantity > 1 to repeats in an argument list
                if (args.size() >= 3) {
                    Term subj = (args.get(0));
                    Term arg = (args.get(1));
                    Term type = (args.get(2));
                    FnDef d = fn.computeIfAbsent(subj, (s) -> new FnDef());
                    Term existing = d.domain.put(((Int) arg).id, type);
                    assert (existing == null || existing.equals(type));
                } else {
                    throw new UnsupportedOperationException("unrecognized domain spec");
                }
                return null;

            case "range":
                if (args.size() == 2) {
                    Term subj = args.get(0);
                    Term range = args.get(1);
                    FnDef d = fn.computeIfAbsent(subj, (s) -> new FnDef());
                    d.range = range;
                } else {
                    throw new UnsupportedOperationException("unrecognized range spec");
                }
                return null;
            case "contraryAttribute":
                //like n-ary disjoint
                if (args.size() >= 2) {
                    Term a = args.get(0);
                    Term b = args.get(1);
                    Variable v0 = nextVar(VAR_DEP);
                    y = disjoint($.inh(v0, a), $.inh(v0, b.neg()));
                }
                break;
            case "documentation":
                if (includeDoc) {
                    if (args.size() >= 2) {
                        Term subj = args.get(0);
                        Term lang = args.get(1);
                        Term desc = $.quote(args.get(2));
                        try {
                            y = $.inh($.p(subj, desc), lang);
                        } catch (Exception e) {
                            //e.printStackTrace();
                            y = null;
                        }
                    }
                }
                break;
            default:
                //System.out.println("unknown: " + x);
                break;
        }

        if (y == null) {

            if (x.car().equals("documentation") && !includeDoc)
                return null;

            Term z = formulaToTerm(x.car());

            if (z != null) {
                switch (z.toString()) {
                    case "and":
                        y = $.conj(args.toArray(new Term[args.size()]));
                        break;
                    case "or":
                        y = $.disj(args.toArray(new Term[args.size()]));
                        break;
                    case "not":
                        y = args.get(0).neg();
                        break;
                    default:
                        y = $.inh($.p(args), z); //HACK
                        break;
                }

            }


        }

        if (y instanceof Bool)
            throw new UnsupportedOperationException("Bool singularity: args=" + args);

        return y;
    }

    private Variable nextVar(Op v) {
        return $.v(v, nextVar());
    }

    private final AtomicInteger serial = new AtomicInteger(0);

    private String nextVar() {
        return Integer.toString(Math.abs(serial.incrementAndGet()), 36);
    }

    //public final Set<Twin<Term>> impl = new HashSet();

    public Term impl(Term a, Term b, boolean implOrEquiv) {

        MutableSet<Term> aVars = new VarOnlySet();
        if (a instanceof Compound)
            ((Compound) a).recurseTermsToSet(Op.VariableBits, aVars, true);
        else if (a.op().var)
            aVars.add(a);
        MutableSet<Term> bVars = new VarOnlySet();
        if (b instanceof Compound)
            ((Compound) b).recurseTermsToSet(Op.VariableBits, bVars, true);
        else if (b.op().var)
            bVars.add(b);

        MutableSet<Term> common = aVars.intersect(bVars);
        if (!common.isEmpty()) {
            Map<Term, Term> remap = new HashMap();
            common.forEach(t -> {
                Variable u = $.v(
                        Op.VAR_INDEP,
                        //Op.VAR_QUERY,
                        //Op.VAR_PATTERN,
                        t.toString().substring(1));
                if (!t.equals(u))
                    remap.put(t, u);
            });

            a = a.replace(remap);
            if (a == null)
                return null;

            b = b.replace(remap);
            if (b == null)
                return null;
        }

        try {
//            impl.add(Tuples.twin(conditionTerm, actionTerm));
//            if (!implOrEquiv) {
//                impl.add(Tuples.twin(actionTerm, conditionTerm)); //reverse
//            }

            return
                    implOrEquiv ?
                            $.impl(a, b) :
                            equi(a, b)
                    ;
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
        return null;
    }


    public static void main(String[] args) throws Exception {
        Param.DEBUG = true;

        NAR e = NARS.tmp();
        MetaGoal.Perceive.set(e.want, -0.1f);

        new PrologCore(e);

        KIFInput k = new KIFInput(e,
                //"/home/me/sumo/Biography.kif"
                //"/home/me/sumo/Military.kif"
                //"/home/me/sumo/ComputerInput.kif"
                //"/home/me/sumo/FinancialOntology.kif"
                "/home/me/sumo/Merge.kif"
                //"/home/me/sumo/emotion.kif"
                //"/home/me/sumo/Weather.kif"
        );
        k.run();


//https://github.com/ontologyportal/sumo/blob/master/tests/TQG1.kif.tq
//(time 240)
//(instance Org1-1 Organization)
//(query (exists (?MEMBER) (member ?MEMBER Org1-1)))
//(answer yes)
        //e.clear();
        e.log();
        e.believe("Organization:org1");
//        e.question("member(?1, org1)?", ETERNAL, (q,a)->{
//            System.out.println(a);
//        });
        e.ask($.$safe("(org1<->?1)"), ETERNAL, QUESTION, (t)->{
           System.out.println(t);
        });
        //e.believe("accountHolder(xyz,1)");
//        e.ask($.$safe("(EmotionalState<->?1)"), ETERNAL, QUESTION, (t)->{
//           System.out.println(t);
//        });
        //e.believe("attribute(xyz,Philosopher)");
        //e.input("(xyz<->?1)?");

        e.run(1000);
//        Thread.sleep(1000);
//        e.run(1000);
        //e.conceptsActive().forEach(s -> System.out.println(s));

        //(($_#AGENT,#OBJECT)-->needs)==>($_#AGENT,#OBJECT)-->wants)).
        //String rules = "((%AGENT,%OBJECT)-->needs), %X |- ((%AGENT,%OBJECT)-->wants), (Belief:Identity)\n";


//        TrieDeriver miniDeriver =
//                //new TrieDeriver(PremiseRuleSet.rules(false, "nal6.nal"));
//                TrieDeriver.get(new PremiseRuleSet(
//                        k.impl.parallelStream().map(tt -> {
//                            try {
//                                return PremiseRuleSet.parse(tt.getOne() + ", () |- " + tt.getTwo() + ", (Belief:Identity)\n");
//                            } catch (Exception e1) {
//                                //e1.printStackTrace();
//                                return null;
//                            }
//                        }).filter(Objects::nonNull).toArray(PremiseRule[]::new)
//                ) );


//        miniDeriver.print(System.out);

        //d.clear();


//        e.onTask(t -> {
//           if (t.isInput()) {
//               //d.forEachTask(b -> {
//                   miniDeriver.test(new Derivation(
//                           e,
//                           budgeting,
//                           Param.UnificationStackMax
//                   ) {
//                       @Override
//                       public void derive(Task x) {
//                           e.input(x);
//                       }
//                   }.restartC(new Premise( t, Terms.ZeroProduct, null, 1f), Param.UnificationTTLMax));
//               //});
//           }
//        });
//        e.input("[Physical]:X.");
//        e.input("[Atom]:Y.");
//        e.input("[Electron]:E.");
//        e.input("[Proton]:P.");
//        e.input("contains(X,Y).");
//        e.input("([Integer]:1 && [Integer]:3).");
//        e.input("starts(A,B).");
//        e.input("[GovernmentFn]:A.");
//        e.input("[WealthFn]:B.");
        e.run(2500);
//        d.conceptsActive().forEach(System.out::println);
        //d.concept("[Phrase]").print();

    }

    /** HACK because recurseTermsToSet isnt designed to check only Op */
    private static class VarOnlySet extends UnifiedSet {
        @Override public boolean add(Object key) { //HACK
            if (!((Term)key).op().var)
                return true;
            return super.add(key);
        }
    }
}
