package nars.subterm;

import com.google.common.io.ByteArrayDataOutput;
import nars.Op;
import nars.The;
import nars.term.Term;
import nars.term.compound.UnitCompound;

import static nars.Op.NEG;

public final class Neg extends UnitCompound implements The {

    private final Term sub;

    public static Term the(Term x) {
        switch (x.op()) {
            case BOOL:
                return x.neg();
            case NEG:
                return x.unneg();
            default:
                return new Neg(x);
        }
    }

    /** condensed NEG compound byte serialization - elides length byte */
    @Override public final void append(ByteArrayDataOutput out) {
        out.writeByte(Op.NEG.id);
        sub.append(out);
    }

    @Override
    public Term neg() {
        return sub;
    }

    @Override
    public Term unneg() {
        return sub;
    }

    private Neg(Term negated) {
        this.sub = negated;
    }

    @Override
    public Op op() {
        return NEG;
    }

    @Override
    public Term sub() {
        return sub;
    }

    @Override
    public final boolean equalsNeg(Term t) {
        return sub.equals(t);
    }
}
