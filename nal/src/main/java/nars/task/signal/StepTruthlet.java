package nars.task.signal;

import nars.Param;

public class StepTruthlet extends ProxyTruthlet {
    public float freqBefore, freqAfter;
    public float eviInactive;

    public StepTruthlet(float freqBefore, Truthlet active, float freqAfter, float eviInactive) {
        super(active);
        this.freqBefore = freqBefore;
        this.freqAfter = freqAfter;
        this.eviInactive = eviInactive;
    }

    @Override
    public void truth(long when, float[] freqEvi) {
        long s = start();
        long e = end();

        if (s <= when && when <= e) {
            super.truth(when, freqEvi); //during the duty cycle
        } else {

            long sd = Math.abs(s - when);
            long ed = Math.abs(e - when);
            long dist; float f;
            if (sd <= ed) {
                dist = sd;
                f = freqBefore;
            } else {
                dist = ed;
                f = freqAfter;
            }
            freqEvi[0] = f;
            freqEvi[1] = (float) Param.evi(eviInactive, dist, Math.max(1, (e - s) / 2));
        }
    }

}
