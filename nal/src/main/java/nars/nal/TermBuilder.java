package nars.nal;

import com.gs.collections.api.set.MutableSet;
import nars.$;
import nars.Op;
import nars.Param;
import nars.index.TermIndex;
import nars.nal.meta.match.Ellipsislike;
import nars.nal.op.TermTransform;
import nars.op.data.differ;
import nars.term.Compound;
import nars.term.InvalidTermException;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.term.compound.Statement;
import nars.term.container.TermContainer;
import nars.term.container.TermSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.util.Arrays.copyOfRange;
import static nars.Op.*;
import static nars.nal.Tense.DTERNAL;
import static nars.term.Terms.empty;
import static nars.term.compound.Statement.pred;
import static nars.term.compound.Statement.subj;

/**
 * Created by me on 1/2/16.
 */
public abstract class TermBuilder {


    /** truth singularity subterms */
    private static final Atom False = $.the("ø");
    private static final Atom True = $.the("¿");
    private static final Term[] TrueArray = new Term[] { True };

    @NotNull
    public final Term build(@NotNull Op op, int dt, @NotNull Term[] u) throws InvalidTermException {

        /* special handling */
        switch (op) {
            case NEG:
                if (u.length != 1)
                    throw new RuntimeException("invalid negation subterms: " + Arrays.toString(u));

                return negation(u[0]);


            case INSTANCE:
                if (u.length != 2 || dt != DTERNAL) throw new InvalidTermException(INSTANCE, dt, u, "needs 2 arg");
                return inst(u[0], u[1]);
            case PROPERTY:
                if (u.length != 2 || dt != DTERNAL) throw new InvalidTermException(PROPERTY, dt, u, "needs 2 arg");
                return prop(u[0], u[1]);
            case INSTANCE_PROPERTY:
                if (u.length != 2 || dt != DTERNAL) throw new InvalidTermException(INSTANCE_PROPERTY, dt, u, "needs 2 arg");
                return instprop(u[0], u[1]);


            case DISJ:
                if (dt!=DTERNAL)
                    throw new InvalidTermException(op,dt,u, "Disjunction must be DTERNAL");
                return disjunction(u);
            case CONJ:
                return junction(op, dt, filterTrueFalseImplicits(op, u));

            case IMGi:
            case IMGe:
                //if no relation was specified and it's an Image,
                //it must contain a _ placeholder
                if (hasImdex(u)) {
                    //TODO use result of hasImdex in image construction to avoid repeat iteration to find it
                    return image(op, u);
                } else if ((dt < 0) || (dt > u.length)) {
                    throw new InvalidTermException(op,dt,u,"Invalid Image");
                }
                break;


            case DIFFe:
            case DIFFi:
                return newDiff(op, u);
            case SECTe:
                return newIntersectEXT(u);
            case SECTi:
                return newIntersectINT(u);

            case INH:
            case SIM:
            case EQUI:
            case IMPL:
                if (u.length != 2) {//throw new RuntimeException("invalid statement: args=" + Arrays.toString(u));
                    throw new InvalidTermException(op, dt, u, "Statement without exactly 2 arguments");
                }
                return statement(op, dt, u[0], u[1]);

            case PROD:
                if (u.length == 0)
                    return Terms.ZeroProduct;
                break;

        }

        return finish(op, dt, u);
    }

    private static Term[] filterTrueFalseImplicits(@NotNull Op o, @NotNull Term[] u) {
        int imdices = 0;
        for (Term x : u) {
            if (x == True) {
                imdices++;
            } else if (x == False)  {
                if (o != DISJ) {
                    //false subterm in conjunction makes the entire condition false
                    //this will eventually reduce diectly to false in this method's only callee HACK
                    return new Term[] { False };
                } else { //DISJ
                    imdices++; //false subterm in disjunction (or) has no effect
                }
            }
        }

        if (imdices == 0)
            return u;

        int ul = u.length;
        if (ul == imdices)
            return TrueArray; //reduces to an Imdex itself

        Term[] y = new Term[ul - imdices];
        for (int i = 0, j = 0; i < ul; i++) {
            Term uu = u[i];
            if ((uu!=True) && (uu!=False))
                y[j++] = uu;
        }

        return y;
    }


    static boolean validEquivalenceTerm(@NotNull Term t) {
        return !t.isAny(TermIndex.InvalidEquivalenceTerm);
//        if ( instanceof Implication) || (subject instanceof Equivalence)
//                || (predicate instanceof Implication) || (predicate instanceof Equivalence) ||
//                (subject instanceof CyclesInterval) || (predicate instanceof CyclesInterval)) {
//            return null;
//        }
    }

    static boolean hasImdex(@NotNull Term[] r) {
        for (Term x : r) {
            //        if (t instanceof Compound) return false;
//        byte[] n = t.bytes();
//        if (n.length != 1) return false;
            if (x.equals(Imdex)) return true;
        }
        return false;
    }


    @NotNull
    public abstract Term newCompound(@NotNull Op op, int dt, @NotNull TermContainer subterms);


    @NotNull
    public Term build(@NotNull Op op, @NotNull Term... tt) {
        return build(op, DTERNAL, tt);
    }


    @Nullable
    public Term newDiff(@NotNull Op op, @NotNull Term[] t) {

        //corresponding set type for reduction:
        Op set = op == DIFFe ? SETe : SETi;

        switch (t.length) {
            case 1:
                Term t0 = t[0];
                if (t0 instanceof Ellipsislike)
                    return finish(op, t0);
                else
                    return t0;
            case 2:
                Term et0 = t[0], et1 = t[1];
                if ((et0.op() == set && et1.op() == set))
                    return differ.difference(this, set, (Compound) et0, (Compound) et1);
                else
                    return et0.equals(et1) ?
                            empty(set) :
                            finish(op, t);
            default:
                return null;
        }
    }



    @Nullable
    public final Term finish(@NotNull Op op, @NotNull Term... args) {
        return finish(op, DTERNAL, args);
    }

    @NotNull
    public final Term finish(@NotNull Op op, @NotNull TermContainer args) {
        return finish(op, DTERNAL, args);
    }

    @NotNull
    public final Term finish(@NotNull Op op, int dt, @NotNull Term... args) {
        for (Term x : args) {
            if ((x == True) || (x == False)) {
                if (!Param.ALLOW_SINGULARITY_LEAK)
                    throw new InvalidTermException(op, dt, args, "singularity leak");
            }
        }
        return finish(op, dt, TermContainer.the(op, args));
    }


    /**
     * step before calling Make, do not call manually from outside
     */
    @NotNull
    protected final Term finish(@NotNull Op op, int dt, @NotNull TermContainer args) {

        int s = args.size();

        if (s == 1 && op.minSize > 1) {
            //special case: allow for ellipsis to occupy one item even if minArity>1
            Term a0 = args.term(0);
            if (!(a0 instanceof Ellipsislike)) {
                //return null;
                //throw new RuntimeException("invalid size " + s + " for " + op);
                return a0; //reduction
            }
        }

        if (Param.DEBUG ) {
            //check for any imdex terms that may have not been removed
            for (Term x : args.terms()) {
                if ((x == True) || (x == False)) {
                    //return null;
                    throw new RuntimeException(op + " term with imdex in subterms: " + args);
                }
            }
        }

        return newCompound(op, dt, args);
    }


    @Nullable
    public Compound inst(Term subj, Term pred) {
        return (Compound) build(INH, build(SETe, subj), pred);
    }

    @Nullable
    public Compound prop(Term subj, Term pred) {
        return (Compound) build(INH, subj, build(SETi, pred));
    }

    @Nullable
    public Compound instprop(@NotNull Term subj, @NotNull Term pred) {
        return (Compound) build(INH, build(SETe, subj), build(SETi, pred));
    }

    @Nullable
    public final Term[] negation(@NotNull Term[] t) {
        int l = t.length;
        Term[] u = new Term[l];
        for (int i = 0; i < l; i++) {
            u[i] = negation(t[i]);
        }
        return u;
    }

    @NotNull
    public final Term negation(@NotNull Term t) {

        //HACK testing for equality like this is not a complete solution. for that we need a new special term type

        if (t.equals(True)) return False;
        else if (t.equals(False)) return True;

        if (t.op() == NEG) {
            // (--,(--,P)) = P
            t = ((TermContainer) t).term(0);

            if (t.equals(True)) return False;
            else if (t.equals(False)) return True;

            return t;

        } else {
            if ((t instanceof Compound) || (t.op() == VAR_PATTERN))
                return finish(NEG, t);


            throw new InvalidTermException(NEG, new Term[] { t }, "Non-compound negation content");
        }
    }


    @Nullable
    final Term image(@NotNull Op o, @NotNull Term[] res) {

        int index = DTERNAL, j = 0;
        for (Term x : res) {
            if (x.equals(Imdex)) {
                index = j;
            }
            j++;
        }

        if (index == DTERNAL)
            throw new RuntimeException("invalid image subterms: " + Arrays.toString(res));

        int serN = res.length - 1;
        Term[] ser = new Term[serN];
        System.arraycopy(res, 0, ser, 0, index);
        System.arraycopy(res, index + 1, ser, index, (serN - index));

        return finish( o, index, ser);
    }

    @NotNull
    public Term junction(@NotNull Op op, int dt, final @NotNull Term... u) {

        int ul = u.length;
        if (ul == 0)
            return True;

        if (ul == 1) {
            Term only = u[0];
            //preserve unitary ellipsis for patterns etc
            return (only instanceof Ellipsislike) ?
                    finish(op, dt, only) :
                    only;

        }

        //simple equality and negequal test
        if (ul == 2) {
            Term a = u[0];
            Term b = u[1];
            if (a.equals(b))
                return a;
            if (((dt == DTERNAL) || (dt == 0)) &&
                    (((a.op() == NEG)   && (negation(a).equals(b))) ||
                    ((b.op() == NEG)    && (negation(b).equals(a))))) {
                return True;
            }
        }

        if (dt == DTERNAL) {
            return junctionFlat(op, DTERNAL, u);
        } else {

            if (dt == 0) {
                Compound x = (Compound) junctionFlat(op, 0, u);
                if (x.size() == 0)
                    return True;

                if (x.size() == 1) {
                    return x.term(0);
                } else {
                    return x.op().temporal ? finish(x.op(), 0, TermSet.the(x.subterms().terms())) : x;
                }
            } else {

                if (ul != 2) {
                    //if (Global.DEBUG)
                    //throw new InvalidTerm(op, DTERNAL, t, u);
                    //else
                    throw new InvalidTermException(op, dt, u, "temporal conjunction requires exactly 2 arguments");
                } else {

                    return finish(op,
                            (u[0].compareTo(u[1]) > 0) ? -dt : dt, //it will be reversed in commutative sorting, so invert dt if sort order swapped
                            u);
                }
            }
        }
    }


    /**
     * flattening junction builder, for multi-arg conjunction and disjunction (dt == 0 ar DTERNAL)
     */
    @NotNull
    public Term junctionFlat(@NotNull Op op, int dt, @NotNull Term[] u) {


        assert(dt ==0 || dt == DTERNAL); //throw new RuntimeException("should only have been called with dt==0 or dt==DTERNAL");


        TreeSet<Term> s = new TreeSet();
        int negations = /*UnifiedSet<Term> unwrappedNegs = */flatten(op, u, dt, s);

        //boolean negate = false;
        int n = s.size();
        if (n == 1) {
            return s.iterator().next();
        } else if (n == negations) {
//            //DEMORGAN's LAWS when all negated https://en.wikipedia.org/wiki/De_Morgan%27s_laws
//            if (op == DISJ) {
//                Term[] y = new Term[n];
//                int i = 0;
//                for (Term xi : s) {
//                    y[i++] = negation(xi);
//                }
//                return negation(build(CONJ, dt, y));
//
//            } /* else if (op = CONJ... */
        }




        //Co-Negated Subterms - any commutive terms with both a subterm and its negative are invalid
        //if (unwrappedNegs!=null) {
//            if (op == DISJ && unwrappedNegs.anySatisfy(s::contains))
//                return null; //throw new InvalidTerm(op, u);
//            //for conjunction, this is handled by the Task normalization process to allow the co-negations for naming concepts
//            if (s.removeAll(unwrappedNegs)) {
//                //remove their negative counterparts
//                s.removeIf(x -> {
//                    return (x.op()==NEG) && unwrappedNegs.contains(((Compound)x).term(0));
//                });
//
//                n = s.size();
//                if (n == 0)
//                    return null;
//                if (n == 1)
//                    return s.iterator().next();
//
//            } else {
//                //if all subterms negated; apply DeMorgan's Law
//                if ((dt == DTERNAL) && (unwrappedNegs.size() == n)) {
//                    op = (op == CONJ) ? DISJ : CONJ;
//                    negate = true;
//                }
//            }
        //}

//        if (negate) {
//            return negation( finish(op, dt, unwrappedNegs.toArray(new Term[n])) );
//        } else {
        if (dt == 0) {
            s = junctionGroupNonDTSubterms(s, 0);
        }
        return finish(op, dt, TermSet.the(s));
        //}
    }

    /** this is necessary to keep term structure consistent for intermpolation.
     *  by grouping all non-sequence subterms into its own subterm, future
     *  flattening and intermpolation is prevented from destroying temporal
     *  measurements.
     */
    protected TreeSet<Term> junctionGroupNonDTSubterms(TreeSet<Term> s, int innerDT) {
        TreeSet<Term> outer = new TreeSet();
        Iterator<Term> ss = s.iterator();
        while (ss.hasNext()) {
            Term x = ss.next();
            if (x.op() == CONJ /* dt will be something other than 'innerDT' having just been flattened */ ) {
                outer.add(x);
                ss.remove();
            }
        }
        if (outer.isEmpty()) {
            return s; //no change
        }

        Term groupedInner = finish(CONJ, innerDT, TermSet.the(s));
        outer.add(groupedInner);

        return outer;
    }

    /** returns # of terms negated */
    static /*UnifiedSet<Term>*/int flatten(@NotNull Op op, @NotNull Term[] u, int dt, @NotNull Collection<Term> s) {
        int negations = 0;
        for (Term x : u) {

            if ((x.op() == op) && (((Compound) x).dt()==dt) /* 0 or DTERNAL */) {
                negations += /*unwrappedNegations = */flatten(op, ((Compound) x).terms(), dt, s); //recurse
            } else {
                if (s.add(x)) { //ordinary term, add
                    if (x.op() == NEG) {
                        negations++;
                    }
//                        if (unwrappedNegations == null)
//                            unwrappedNegations = new UnifiedSet<>(1);
//                        unwrappedNegations.add(((Compound) x).term(0));
//                    }
                }
            }
        }
        return negations; //return unwrappedNegations;
    }


    @NotNull
    public Term statement(@NotNull Op op, int dt, @NotNull Term subject, @NotNull Term predicate) {
        while (true) {

            //special statement filters
            switch (op) {

                case INH:
                    if (predicate instanceof TermTransform && transformImmediates() && subject.op() == PROD) {
                        return ((TermTransform) predicate).function((Compound<?>) subject);
                    }
                    break;


                case EQUI:
                    if (!Param.ALLOW_RECURSIVE_IMPLICATIONS && !validEquivalenceTerm(subject))
                        throw new InvalidTermException(op, dt, new Term[] { subject, predicate }, "Invalid equivalence subject");
                    if (!Param.ALLOW_RECURSIVE_IMPLICATIONS &&!validEquivalenceTerm(predicate))
                        throw new InvalidTermException(op, dt, new Term[] { subject, predicate }, "Invalid equivalence predicate");
                    break;

                case IMPL:
                    if (!Param.ALLOW_RECURSIVE_IMPLICATIONS) {
                        if (subject.isAny(TermIndex.InvalidEquivalenceTerm))
                            throw new InvalidTermException(op, dt, new Term[]{subject, predicate}, "Invalid implication subject");
                        if (predicate.isAny(TermIndex.InvalidEquivalenceTerm))
                            throw new InvalidTermException(op, dt, new Term[]{subject, predicate}, "Invalid implication predicate");
                    }


                    if (subject == True) {
                        return predicate;
                    } else if (subject == False) {
                        return False;
                        //throw new InvalidTermException(op, dt, new Term[] { subject, predicate }, "Implication predicate is singular FALSE");
                        //return negation(predicate); /??
                    }

                    if (predicate.op() == IMPL) {
                        Term oldCondition = subj(predicate);
                        if (!Param.ALLOW_RECURSIVE_IMPLICATIONS && (oldCondition.op() == CONJ && oldCondition.containsTerm(subject)))
                            throw new InvalidTermException(op, dt, new Term[] { subject, predicate }, "Implication circularity");
                        else
                            return impl2Conj(dt, subject, predicate, oldCondition);
                    }


                    //filter (factor out) any common subterms iff equal 'dt'
                    if ((subject.op() == CONJ) && (predicate.op() == CONJ)) {
                        Compound csub = (Compound) subject;
                        Compound cpred = (Compound) predicate;
                        if (csub.dt() == cpred.dt()) {

                            TermContainer subjs = csub.subterms();
                            TermContainer preds = cpred.subterms();

                            MutableSet<Term> common = TermContainer.intersect(subjs, preds);
                            if (!common.isEmpty()) {
                                Term newSubject = build(csub, TermContainer.except(subjs, common));
                                if (newSubject == null)
                                    throw new InvalidTermException(op, dt, new Term[] { subject, predicate }, "Failed flattening implication conjunction subject");
                                Term newPredicate = build(cpred, TermContainer.except(preds, common));
                                if (newPredicate == null)
                                    throw new InvalidTermException(op, dt, new Term[] { subject, predicate }, "Failed flattening implication conjunction predicate");

                                subject = newSubject;
                                predicate = newPredicate;
                                continue;
                            }
                        }
                    }

                    break;

            }

            int validity = Statement.validStatement(op, subject, predicate);
            switch (validity) {

                case -1:
                    throw new InvalidTermException(op, dt, new Term[] { subject, predicate }, "Statement invalid (TODO be more specific)");

                case 0:
                    return True;

                case 1:
                    if (op.commutative && (dt != DTERNAL && dt != 0) && subject.compareTo(predicate) > 0) //equivalence
                        dt = -dt;
                    return finish(op, dt, subject, predicate);

                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    /** whether this builder applies immediate transforms */
    protected abstract boolean transformImmediates();


    @Nullable
    public Term subtractSet(@NotNull Op setType, @NotNull Compound A, @NotNull Compound B) {
        return differ.difference(this, setType, A, B);
    }

    @NotNull
    public Term impl2Conj(int t, Term subject, @NotNull Term predicate, Term oldCondition) {
        Term s = junction(CONJ, t, subject, oldCondition);
        return s != null ? build(IMPL, s, pred(predicate)) : null;
    }

    @Nullable
    public Term newIntersectINT(@NotNull Term[] t) {
        return newIntersection(t,
                SECTi,
                SETi,
                SETe);
    }

    @Nullable
    public Term newIntersectEXT(@NotNull Term[] t) {
        return newIntersection(t,
                SECTe,
                SETe,
                SETi);
    }

    @Nullable
    public Term newIntersection(@NotNull Term[] t, @NotNull Op intersection, @NotNull Op setUnion, @NotNull Op setIntersection) {
        switch (t.length) {

            case 1:

                Term single = t[0];
                if (single instanceof Ellipsislike) {
                    //allow
                    return finish(intersection, single);
                } else {
                    return single;
                }

            case 2:
                return newIntersection2(t[0], t[1], intersection, setUnion, setIntersection);
            default:
                //HACK use more efficient way
                Term a = newIntersection2(t[0], t[1], intersection, setUnion, setIntersection);
                if (a == null) return null;
                Term b = newIntersection(copyOfRange(t, 2, t.length), intersection, setUnion, setIntersection);
                if (b == null) return null;
                return newIntersection2(a, b,
                        intersection, setUnion, setIntersection
                );
        }

    }

    @Nullable
    @Deprecated
    public Term newIntersection2(@NotNull Term term1, @NotNull Term term2, @NotNull Op intersection, @NotNull Op setUnion, @NotNull Op setIntersection) {

        if (term1.equals(term2))
            return term1;

        Op o1 = term1.op();
        Op o2 = term2.op();

        if ((o1 == setUnion) && (o2 == setUnion)) {
            //the set type that is united
            return union(setUnion, (Compound) term1, (Compound) term2);
        }


        if ((o1 == setIntersection) && (o2 == setIntersection)) {
            //the set type which is intersected
            return intersect(setIntersection, (Compound) term1, (Compound) term2);
        }

        if (o2 == intersection && o1 != intersection) {
            //put them in the right order so everything fits in the switch:
            Term x = term1;
            term1 = term2;
            term2 = x;
            o2 = o1;
            o1 = intersection;
        }

        //reduction between one or both of the intersection type

        if (o1 == intersection) {
            return finish(intersection, TermSet.concat(
                        ((TermContainer) term1).terms(),
                        o2 == intersection ? ((TermContainer) term2).terms() : new Term[]{term2}
                    )
            );
        }

        return finish(intersection, term1, term2);
    }



    @NotNull
    public Term intersect(@NotNull Op o, @NotNull Compound a, @NotNull Compound b) {
        if (a.equals(b))
            return a;

        MutableSet<Term> s = TermContainer.intersect(
                /*(TermContainer)*/ a, /*(TermContainer)*/ b
        );
        return s.isEmpty() ? emptySet(o) : (Compound) finish(o, TermContainer.the(o, s));
    }

    public static Term emptySet(Op o) {
        return True; //use the True for empty set
    }

    @Nullable
    public Compound union(@NotNull Op o, @NotNull Compound term1, @NotNull Compound term2) {
        TermContainer u = TermContainer.union(term1, term2);
        if (u == term1)
            return term1;
        else if (u == term2)
            return term2;
        else
            return (Compound)finish(o, u);
    }

    @Nullable
    public final Term build(@NotNull Compound csrc, @NotNull Term[] newSubs) {
        return build(csrc.op(), csrc.dt(), newSubs);
    }

    @Nullable
    public final Term build(@NotNull Compound csrc, @NotNull TermContainer newSubs) {
        if (csrc.subterms().equals(newSubs))
            return csrc;
        else
            return build(csrc.op(), csrc.dt(), newSubs.terms());
    }

    public final Term disjunction(Term[] u) {
        return negation(build(CONJ, DTERNAL, negation(u)));
    }

}
