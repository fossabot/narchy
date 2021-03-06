package jcog.exe.realtime;

import java.util.concurrent.Callable;

import static jcog.exe.realtime.TimedFuture.Status.CANCELLED;

public class FixedRateTimedFuture<T> extends AbstractTimedCallable<T> {

    /** adjustable while running */
    private volatile long period;

    public FixedRateTimedFuture(int rounds,
                                Callable<T> callable,
                                long recurringTimeout, long resolution, int wheelSize) {
        super(rounds, callable);
        this.period = recurringTimeout;
        reset(resolution, wheelSize);
    }

    @Override
    public void execute(HashedWheelTimer t) {
        if (status != CANCELLED) {
            super.execute(t);
            reset(t.resolution, t.wheels);
            t._schedule(this);
        }
    }

    @Override
    public boolean isPeriodic() {
        return true;
    }

    public void setPeriodMS(long periodMS) {
        setPeriodNS(periodMS * 1_000_000);
    }

    public void setPeriodNS(long periodNS) {
        this.period = periodNS;
    }

    public int getOffset(long resolution) {
        return (int) Math.round(((double)period)/resolution);
    }

    public void reset(long resolution, int wheels) {
        this.rounds = (int)
            Math.min(Integer.MAX_VALUE-1,
                Math.round((((double)period)/resolution) / wheels)
            );
    }


}

//public class FixedRateTimedFuture<T> extends FixedDelayTimedFuture<T> {
//
//    public FixedRateTimedFuture(int rounds,
//                                Callable<T> callable,
//                                long periodNS, long resolution, int wheels, Consumer<TimedFuture<?>> resched) {
//        super(rounds, callable, periodNS, resolution, wheels, resched);
//    }
//
//
//    private long lastDutyNS;
//
////    @Override public int getOffset(long resolution) {
////        return (int) Math.min(Integer.MAX_VALUE-1,
////                Math.round(((double)Math.max(resolution, (periodNS-lastDutyNS))) / resolution)
////        );
////    }
//
//
//    public int getOffset(long resolution) {
//        return (int) (periodNS / resolution);
//    }
//
//    @Override
//    public void run() {
//        long start = System.nanoTime();
//        super.run();
//        long end = System.nanoTime();
//        this.lastDutyNS = end - start;
//
//        reset();
//        rescheduleCallback.accept(this);
//    }
//
//}
