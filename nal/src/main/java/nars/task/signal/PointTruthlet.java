package nars.task.signal;

public abstract class PointTruthlet extends Truthlet {

    long when;

    @Override
    public final long start() {
        return when;
    }

    @Override
    public final long end() {
        return when;
    }

    @Override
    public final void truth(long when, float[] freqEvi) {
        if (this.when == when) {
            eval(freqEvi);
        } else {
            unknown(freqEvi);
        }
    }

    /** for the specific time point this represents */
    protected abstract void eval(float[] freqEvi);
}
