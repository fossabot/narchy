package nars.bag;

import nars.Global;
import nars.budget.Budget;
import nars.budget.Budgeted;
import nars.budget.UnitBudget;
import org.jetbrains.annotations.NotNull;

import static nars.util.data.Util.clamp;

/**
 * Budgeted Link (an entry in a bag)
 * equalsTo/hashCode proxies to the wrapped element, X id
 *
 * Acts as a "budget vector" containing an accumulating delta
 * that can be commit()'d on the next udpate
 */
public final class BLink<X> extends Budget implements Link<X> {

    /** the referred item */
    public final X id;


    /** changed status bit */
    boolean changed;

    /** priority */
    final static int PRI = 0;
    /** delta pri */
    final static int DPRI = 1;

    /** durability */
    final static int DUR = 2;
    /** delta dur */
    final static int DDUR = 3;

    /** quality */
    final static int QUA = 4;
    /** delta qua */
    final static int DQUA = 5;

    /** overflow/backpressure buffer variable */
    //final static int BUFFERED = 6;

    /** time of last forget */
    final static int LASTFORGET = 6;

    private final float[] b = new float[7];



    private BLink(X id) {
        this.id = id;
    }

    public BLink(X id, float p, float d, float q) {
        this(id);
        init(p, d, q);
    }

    public BLink(X id, @NotNull Budgeted b) {
        this(id);
        init(b, 1f);
    }

    public BLink(X id, @NotNull Budgeted b, float scale) {
        this(id);
        init(b, scale);
    }

    @NotNull @Override
    public X get() {
        return id;
    }

    private void init(@NotNull Budgeted c, float scale) {
        init(c.pri() * scale, c.dur(), c.qua());
    }

    public void init(float p, float d, float q) {
        float[] b = this.b;
        b[PRI] = clamp(p);
        b[DUR] = clamp(d);
        b[QUA] = clamp(q);
        b[LASTFORGET] = Float.NaN;
    }

    @Override
    public final void delete() {
        b[PRI] = Float.NaN;
        changed = true;
    }

    /** TODO return false to signal to the bag to remove this item */
    public final boolean commit() {
        if (changed) {
            float[] b = this.b;
            b[PRI] = clamp(b[PRI] + b[DPRI]); b[DPRI] = 0;
            b[DUR] = clamp(b[DUR] + b[DDUR]); b[DDUR] = 0;
            b[QUA] = clamp(b[QUA] + b[DQUA]); b[DQUA] = 0;
            changed = false;
            return true;
        }
        return false;
    }

    @Override
    public final float pri() {
        return b[PRI];
    }

    @Override
    public final boolean isDeleted() {
        float p = b[PRI];
        return (p!=p); //fast NaN test
    }

    protected final void setValue(int x, float v) {
        float[] b = this.b;
        int twoX = 2 * x;
        float delta = v - b[twoX];
        b[twoX + 1] += delta;
        changed = true;
    }

    @Override
    public final void _setPriority(float p) {
        setValue(0, p);
    }

    @Override
    public final float dur() {
        return b[DUR];
    }

    @Override
    public final void _setDurability(float d) {
        setValue(1, d);
    }

    @Override
    public final float qua() {
        return b[QUA];
    }

    @Override
    public void _setQuality(float q) {
        setValue(2, q);
    }

    @Override
    public final float setLastForgetTime(float currentTime) {
        float[] b = this.b;
        float lastForget = b[LASTFORGET];
        float diff = (lastForget != lastForget /* NaN test */) ? Global.SUBFRAME_EPSILON : (currentTime - lastForget);
        b[LASTFORGET] = currentTime;
        return diff;
    }

    @Override
    public float getLastForgetTime() {
        return b[LASTFORGET];
    }


    @NotNull
    @Override
    public UnitBudget clone() {
        return new UnitBudget(this);
    }


    @Override public boolean equals(Object obj) {
//        /*if (obj instanceof Budget)*/ {
//            return equalsBudget((Budget) obj);
//        }
//        return id.equals(((BagBudget)obj).id);
        return obj == this;
    }

    @Override public int hashCode() {
        return id.hashCode();
    }

    @NotNull
    @Override
    public String toString() {
        return id + "=" + getBudgetString();
    }



//    public void charge(float overflow) {
//        assert(overflow > 0);
//        b[6] += overflow;
//    }
//    public float drain() {
//        float[] b = this.b;
//        float o = b[6];
//        if (o > 0) {
//            b[6] = 0; //clear
//        }
//        return o;
//    }

//    static boolean nonZero(float x) {
//        //return (Math.abs(x) > Global.BUDGET_EPSILON);
//        return x!=0f;
//    }
}
