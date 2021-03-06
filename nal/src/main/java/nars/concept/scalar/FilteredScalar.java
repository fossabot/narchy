package nars.concept.scalar;

import jcog.Util;
import jcog.math.FloatSupplier;
import jcog.util.ArrayIterator;
import nars.NAR;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.tuple.Pair;

import java.util.Iterator;

import static nars.Op.CONJ;

/** calculates a set of derived scalars from an input scalar */
public class FilteredScalar extends DemultiplexedScalar {

    public final Filter[] filter;

    public FilteredScalar(FloatSupplier input, NAR nar, Pair<Term,FloatToFloatFunction>... filters) {
        super(input,
                //$.disj
                CONJ.the
                    (Util.map(Pair::getOne, Term[]::new, filters)), nar);

        this.filter = new Filter[filters.length];

        int j = 0;
        for (Pair<Term,FloatToFloatFunction> p : filters) {
            filter[j++] = new Filter(p.getOne(), input, p.getTwo(), nar);
        }

        for (Scalar s : filter)
            nar.on(s);

        nar.on(this);
    }

//    public static FilteredScalar filter(@Nullable Term id,
//                                       FloatSupplier input,
//                                       NAR nar,
//
//                                       IntFunction<Term> filterTerm,
//                                       FloatToFloatFunction... filters) {
//        return new FilteredScalar(id, input, filters.length,
//                (f) -> new Filter(f == 0 ? id : filterTerm.applyAsInt(f),
//                input, filters[f], nar), nar);
//    }

    @Override
    public Iterator<Scalar> iterator() {
        return ArrayIterator.get(filter);
    }

    public static class Filter extends Scalar {

        //TODO
        //public float belief; //relative priority of generated beliefs
        //public float goal; //relative priority of generated goals

        Filter(Term id, FloatSupplier input, FloatToFloatFunction f, NAR nar) {
            super(id,
                () -> f.valueOf(input.asFloat()),
                nar);
        }
    }
}
