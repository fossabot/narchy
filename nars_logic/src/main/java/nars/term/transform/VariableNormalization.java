package nars.term.transform;

import nars.$;
import nars.Global;
import nars.term.Compound;
import nars.term.variable.Variable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

/**
 * Variable normalization
 *
 * Destructive mode modifies the input Compound instance, which is
 * fine if the concept has been created and unreferenced.
 *
 * The term 'destructive' is used because it effectively destroys some
 * information - the particular labels the input has attached.
 *
 */
public class VariableNormalization extends VariableTransform implements Function<Variable,Variable> {

//    final static Comparator<Map.Entry<Variable, Variable>> comp = new Comparator<Map.Entry<Variable, Variable>>() {
//        @Override
//        public int compare(Map.Entry<Variable, Variable> c1, Map.Entry<Variable, Variable> c2) {
//            return c1.getKey().compareTo(c2.getKey());
//        }
//    };

//    /**
//     * overridden keyEquals necessary to implement alternate variable hash/equality test for use in normalization's variable transform hashmap
//     */
//    static final class VariableMap extends FastPutsArrayMap<Pair<Variable,Term>, Variable> {
//
//
//
//        public VariableMap(int initialCapacity) {
//            super(initialCapacity);
//        }
//
//        @Override
//        public final boolean keyEquals(final Variable a, final Object ob) {
//            if (a == ob) return true;
//            Variable b = ((Variable) ob);
//            return Byted.equals(a, b);
//        }
//
////        @Override
////        public Variable put(Variable key, Variable value) {
////            Variable removed = super.put(key, value);
////            /*if (size() > 1)
////                Collections.sort(entries, comp);*/
////            return removed;
////        }
//    }

    /** for use with compounds that have exactly one variable */
    public static final VariableTransform singleVariableNormalization = new VariableTransform() {

        @Override
        public Variable apply(Compound containing, @NotNull Variable current, int depth) {
            //      (containing, current, depth) ->
            return $.v(current.op(), 1);
        }
    };

    final Map<Variable, Variable> rename = Global.newHashMap(8);

    boolean renamed = false;


    @NotNull
    @Override
    public final Variable apply(@NotNull Variable v) {
        Variable rvv = newVariable(v, rename.size()+1);
        if (!renamed) {
            //test for any rename to know if modification occurred
            renamed = !rvv.equals(v);
        }
        return rvv;
    }

    @Override
    public final Variable apply(Compound ct, Variable v, int depth) {
        return rename.computeIfAbsent(v, this);
    }

    /** if already normalized, alreadyNormalized will be non-null with the value */
    @NotNull
    protected Variable newVariable(@NotNull Variable v, int serial) {
        return v.normalize(serial);
    }

}
