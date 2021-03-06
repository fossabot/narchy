package nars.task.signal;

import nars.Param;

/**
 * an impulse function which provides contrasting truth wave
 * before, during, and after the specified duration (pulse).
 */
public class ImpulseTruthlet extends ProxyTruthlet {

    float freqInactive;
    float eviInactive;

    public ImpulseTruthlet(Truthlet active, float freqInactive, float eviInactive) {
        super(active);
        this.freqInactive = freqInactive;
        this.eviInactive = eviInactive;
    }

    @Override
    public void truth(long when, float[] freqEvi) {
        long s = start();
        long e = end();
        if (when >= s && when <= e) {
            //internal
            super.truth(when, freqEvi);
        } else {
            //external
            freqEvi[0] = freqInactive;
            long dist = Math.min(Math.abs(s - when), Math.abs(e - when));
            freqEvi[1] = (float) Param.evi(eviInactive, dist, Math.max(1, (e - s) / 2));
        }
    }

}
