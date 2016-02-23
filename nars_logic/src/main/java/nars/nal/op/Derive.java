package nars.nal.op;

import com.google.common.base.Joiner;
import nars.$;
import nars.Global;
import nars.Memory;
import nars.Op;
import nars.budget.Budget;
import nars.concept.ConceptProcess;
import nars.concept.Temporalize;
import nars.nal.meta.*;
import nars.nal.meta.match.EllipsisMatch;
import nars.nal.nal8.AtomicStringConstant;
import nars.nal.nal8.Operator;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.ATOM;
import static nars.nal.Tense.ETERNAL;

/**
 * Handles matched derivation results
 * < (&&, postMatch1, postMatch2) ==> derive(term) >
 */
public class Derive extends AtomicStringConstant implements ProcTerm {

    @NotNull
    public final String id;

    public final boolean anticipate;
    public final boolean eternalize;

    @NotNull
    public final PremiseRule rule;
    private final Temporalize temporalizer;

    /** result pattern */
    @NotNull
    public final Term conclusionPattern;

    @NotNull private final BooleanCondition<PremiseEval> postMatch; //TODO use AND condition

    /** whether this a single or double premise derivation; necessary in case premise
     * does have a belief but it was not involved in determining Truth */
    public final boolean beliefSingle, desireSingle;


    public Derive(@NotNull PremiseRule rule, @NotNull Term term, @NotNull BooleanCondition[] postMatch,
                  boolean beliefSingle, boolean desireSingle, boolean anticipate, boolean eternalize, Temporalize temporalizer) {
        this.rule = rule;
        this.temporalizer = temporalizer;
        this.postMatch = (postMatch.length > 0) ? new AndCondition(postMatch) : BooleanCondition.TRUE;
        this.conclusionPattern = term;
        this.beliefSingle = beliefSingle;
        this.desireSingle = desireSingle;
        this.anticipate = anticipate;
        this.eternalize = eternalize;

        String i = "Derive:(" + term;
        if (eternalize && anticipate) {
            i += ", {eternalize,anticipate}";
        } else if (eternalize) {
            i += ", {eternalize}";
        } else if (anticipate) {
            i += ", {anticipate}";
        }


        if (postMatch.length > 0) {
            i += ", {" + Joiner.on(',').join(postMatch) + '}';
        }

        i += ")";
        this.id = i;
    }


    @Override
    public@Nullable
    Op op() {
        return ATOM; //product?
    }

    @NotNull
    @Override
    public String toString() {
        return id;
    }


    /** main entry point for derivation result handler.
     * @return true to allow the matcher to continue matching,
     * false to stop it */
    @Override public final void accept(@NotNull PremiseEval m) {

        Term derivedTerm = m.resolve(conclusionPattern);

        if (derivedTerm == null)
            return;

        if ((derivedTerm instanceof EllipsisMatch)) {
            //TODO hack prevent this
            //throw new RuntimeException("invalid ellipsis match: " + derivedTerm);
            EllipsisMatch em = ((EllipsisMatch)derivedTerm);
            switch (em.size()) {
                case 1:
                    derivedTerm = em.term(0); //unwrap the item
                    break;
                case 0:
                    return;
                default:
                    //throw new RuntimeException("invalid ellipsis match: " + em);
                    return;
            }
        }

        if (ensureValidVolume(derivedTerm)) {
            if (postMatch.booleanValueOf(m))
                derive(m, derivedTerm);
        } else {

                if (Global.DEBUG) {
                    //$.logger.error("Term volume overflow");
                /*c.forEach(x -> {
                    Terms.printRecursive(x, (String line) ->$.logger.error(line) );
                });*/

                    String message = "Term volume overflow: " + derivedTerm;
                    $.logger.error(message + "\n" + rule);
                    //System.exit(1);
                    //throw new RuntimeException(message);
                }

        }


    }

    private static boolean ensureValidVolume(@NotNull Term derivedTerm) {

        //HARD VOLUME LIMIT
        boolean tooLarge = derivedTerm.volume() > Global.COMPOUND_VOLUME_MAX;

        return !tooLarge;

    }


    /** part 1 */
    private void derive(@NotNull PremiseEval p, @Nullable Term t) {

        if (t.varPattern()!=0) {
            return;
        }

        ConceptProcess premise = p.currentPremise;
        Memory mem = premise.memory();

        //get the normalized term to determine the budget (via it's complexity)
        //this way we can determine if the budget is insufficient
        //before conceptualizating in mem.taskConcept
        Termed tNorm = mem.index.normalized(t);

        //HACK why?
        if ((tNorm == null) || !tNorm.term().isCompound())
            return;

        Truth truth = p.truth.get();

        Budget budget = p.getBudget(truth, tNorm);
        if (budget == null)
            return;

        boolean p7 = mem.nal() >= 7;

        long now = mem.time();
        long occ;

        Compound ct = (Compound) tNorm.term();

        if (p7 && (!premise.isEternal() || premise.hasTemporality())) {
            Term cp = this.conclusionPattern;

            if (Op.isOperation(cp) && p.transforms.containsKey( Operator.operator((Compound) cp) ) ) {
                //unwrap operation from conclusion pattern; the pattern we want is its first argument
                cp = Operator.argArray((Compound) cp)[0];
            }

            long[] occReturn = new long[] { ETERNAL };

            ct = this.temporalizer.compute(ct,
                    p, this, occReturn
            );

            occ = occReturn[0];

        } else {
            occ = ETERNAL;
        }

        premise.derive(ct, truth, budget, now, occ, p, this);

    }






}
