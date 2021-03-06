package nars.task.signal;

import jcog.Paper;
import jcog.Skill;
import jcog.Util;
import nars.truth.Truth;

import static nars.truth.TruthFunctions.w2cSafe;
import static nars.util.time.Tense.ETERNAL;

/** wavelet-like freq/evidence function */
@Paper
@Skill("Wavelet")
abstract public class Truthlet implements Truth {

    abstract public long start();
    abstract public long end();

    public long mid() { return (start() + end())/2L; }
    public long range() {
        long e = end();
        long s = start();
        return e - s;
    }

    public Truthlet stretch(long newStart, long newEnd) {
        throw new UnsupportedOperationException();
    }

    @Deprecated @Override public final float freq() {

        float fs = freq(start());
        if (fs == fs) {
            float fe = freq(end());
            if (fe == fe) {
                return Util.mean(fs, fe);
            }
        }

        //fallback:
        return freq(mid());
    }
    @Deprecated @Override public final float evi() {
        float es = evi(start());
        if (es > 0) {
            float ee = evi(end());
            if (ee > 0) {
                return Util.mean(es, ee);
            }
        }

        //fallback:
        return evi(mid());
    }

    /** computes the average frequency over the interval */
    public float freq(long start, long end) {
        float fStart = freq(start);
        if (fStart!=fStart)
            return Float.NaN;
        if (start == end) {
            return fStart;
        } else {
            //TODO use the N-point approximation, and weight by the evidence because it may not be constant

            float fEnd = freq(end);
            if (fEnd != fEnd)
                return Float.NaN;


            if (end - start > 1) {
                //interpolate using 3 points:
                float fMid = freq((start+end)/2L);
                if (fMid==fMid) {
                    return Util.mean(fStart, fMid, fEnd);
                }
            }

            //interpolate using 2 points:
            return Util.mean(fStart, fEnd);

        }
    }

    public float freq(long when) {
        return truth(when)[0];
    }
    public float evi(long when) {
        return truth(when)[1];
    }

    public final float conf(long when) {
        return w2cSafe(evi(when));
    }

    public boolean intersects(long start, long end) {
        return start <= end() && end >= start();
    }

    public final boolean during(long when) {
        if (when == ETERNAL)
            return true;
        long s = start();
        if (when == s) return true;
        long e = end();
        return (when > s && when <= e);
    }

    public final float[] truth(long when) {
        float[] fe = new float[2];
        truth(when, fe);
        return fe;
    }

    abstract public void truth(long when, float[] freqEvi);

    /** sets a FreqEvi vector as unknown */
    protected static void unknown(float[] freqEvi) {
        freqEvi[0] = Float.NaN;
        freqEvi[1] = 0;
    }


    public static Truthlet impulse(long start, long end, float freqOn, float freqOff, float evi) {
        return new ImpulseTruthlet(
                flat(start, end, freqOn, evi), freqOff, evi
        );
    }
    public static Truthlet step(float freqBefore, long start, float freqOn, long end, float freqAfter, float evi) {
        return new StepTruthlet(
                freqBefore, flat(start, end, freqOn, evi), freqAfter, evi
        );
    }
    public static Truthlet step(float freqBefore, long start, float freqOn, float eviOn, long end, float freqAfter, float eviOff) {
        return new StepTruthlet(
                freqBefore, flat(start, end, freqOn, eviOn), freqAfter, eviOff
        );
    }

    public static Truthlet flat(long start, long end, float freq, float evi) {
        return new FlatTruthlet(start, end, freq, evi);
    }

    public static LinearTruthlet slope(long start, float startFreq, long end, float endFreq, float evi) {
        return new LinearTruthlet(start, startFreq, end, endFreq, evi);
    }


}
