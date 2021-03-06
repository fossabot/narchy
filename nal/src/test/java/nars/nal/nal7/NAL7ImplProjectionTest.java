package nars.nal.nal7;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.term.Term;
import nars.truth.Truth;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.util.Precision;
import org.eclipse.collections.api.block.function.primitive.DoubleToDoubleFunction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class NAL7ImplProjectionTest {

    @Test public void test1() {

        int implDT = 5;
        int dur = 1;

        /* eventTime, relative to impl belief */
        for (int implTime = 0; implTime < 5; implTime+=2) {
            for (int eventTime = 0; eventTime < 5; eventTime+=2) {

                Term y = $.the("y");


                NAR n = NARS.tmp();
                //n.log();
                n.time.dur(dur);
                n.inputAt(eventTime, "x. :|:");
                n.inputAt(implTime, "(x ==>+" + implDT + " y). :|:");
                int end = Math.max(eventTime + implDT, implTime);
                n.run(end + 1);

                double[] max = new MyBrentOptimizer(0.001f, 0.01, 0, end, (t) -> {
                    Truth u = n.beliefTruth(y, Math.round(t));
                    if (u == null)
                        return -1f;
                    return u.conf();
                }).max(0, end, end / 2.0);


                long yTimeEstimate = Math.round(max[0]);
                long yTimeActual = eventTime + implDT;
                assertTrue(Math.abs(yTimeEstimate - yTimeActual) <= 1);

                double yConfMax = max[1];
                long eventBeliefDelta = Math.abs(eventTime - implTime);
                System.out.println("+-" + eventBeliefDelta + " -> " + max[0] + "=" + max[1]);
            }
        }

//        double zero = UnivariateSolverUtils.solve(t->{
//            return 0;
//        }, 0, end, 0.01f);

        //n.concept(x).print();
        //n.concept(y).print();
    }

    public static class MyBrentOptimizer /*extends UnivariateOptimizer*/ {
    /**
     * Golden section.
     */
    private static final double GOLDEN_SECTION = 0.5 * (3 - Math.sqrt(5));
    /**
     * Minimum relative tolerance.
     */
    private static final double MIN_RELATIVE_TOLERANCE = 2 * Math.ulp(1d);
    /**
     * Relative threshold.
     */
    private final double relativeThreshold;
    /**
     * Absolute threshold.
     */
    private final double absoluteThreshold;

    final double min, max;
    final private DoubleToDoubleFunction func;

//    /**
//     * The arguments are used implement the original stopping criterion
//     * of Brent's algorithm.
//     * {@code abs} and {@code rel} define a tolerance
//     * {@code tol = rel |x| + abs}. {@code rel} should be no smaller than
//     * <em>2 macheps</em> and preferably not much less than <em>sqrt(macheps)</em>,
//     * where <em>macheps</em> is the relative machine precision. {@code abs} must
//     * be positive.
//     *
//     * @param rel Relative threshold.
//     * @param abs Absolute threshold.
//     * @param checker Additional, user-defined, convergence checking
//     * procedure.
//     * @throws NotStrictlyPositiveException if {@code abs <= 0}.
//     * @throws NumberIsTooSmallException if {@code rel < 2 * Math.ulp(1d)}.
//     */
//    public MyBrentOptimizer(double rel,
//                            double abs,
//                            ConvergenceChecker<UnivariatePointValuePair> checker) {
//
//    }


    /**
     * The arguments are used for implementing the original stopping criterion
     * of Brent's algorithm.
     * {@code abs} and {@code rel} define a tolerance
     * {@code tol = rel |x| + abs}. {@code rel} should be no smaller than
     * <em>2 macheps</em> and preferably not much less than <em>sqrt(macheps)</em>,
     * where <em>macheps</em> is the relative machine precision. {@code abs} must
     * be positive.
     *
     * @param rel Relative threshold.
     * @param abs Absolute threshold.
     * @param min
     * @param func
     * @throws NotStrictlyPositiveException if {@code abs <= 0}.
     * @throws NumberIsTooSmallException if {@code rel < 2 * Math.ulp(1d)}.
     */
    public MyBrentOptimizer(double rel,
                            double abs, double min, double max, DoubleToDoubleFunction func) {


        this.min = min;
        this.max = max;
        this.func = func;
        if (rel < MIN_RELATIVE_TOLERANCE) {
            throw new NumberIsTooSmallException(rel, MIN_RELATIVE_TOLERANCE, true);
        }
        if (abs <= 0) {
            throw new NotStrictlyPositiveException(abs);
        }

        relativeThreshold = rel;
        absoluteThreshold = abs;
    }


    protected double[] max(double... xStart) {
        double[] previous = new double [] { Double.NaN, Double.NEGATIVE_INFINITY };
        for(double x : xStart) {
            previous = max(x, previous);
        }
        return previous;
    }

    protected double[] max(double xStart, double[] previous) {
        final boolean isMinim = false; //MAX   //getGoalType() == GoalType.MINIMIZE;
        final double lo = min;
        //final double mid = (min+max)/2.0;
        final double hi = max;

        // Optional additional convergence criteria.
//        final ConvergenceChecker<UnivariatePointValuePair> checker
//            = getConvergenceChecker();

        double a;
        double b;
        if (lo < hi) {
            a = lo;
            b = hi;
        } else {
            a = hi;
            b = lo;
        }

        double X = xStart;
        double v = X;
        double w = X;
        double d = 0;
        double e = 0;
        double Y = func.valueOf(X);
        if (!isMinim) {
            Y = -Y;
        }
        double fv = Y;
        double fw = Y;

        double[] current
            = new double[] { X, isMinim ? Y : -Y };
        // Best point encountered so far (which is the initial guess).
        double[] best = current;

        while (true) {
            //System.out.println(X + " " + Y);

            final double m = 0.5 * (a + b);
            final double tol1 = relativeThreshold * Math.abs(X) + absoluteThreshold;
            final double tol2 = 2 * tol1;

            // Default stopping criterion.
            final boolean stop = Math.abs(X - m) <= tol2 - 0.5 * (b - a);
            if (!stop) {
                double p = 0;
                double q = 0;
                double r = 0;
                double u = 0;

                if (Math.abs(e) > tol1) { // Fit parabola.
                    r = (X - w) * (Y - fv);
                    q = (X - v) * (Y - fw);
                    p = (X - v) * q - (X - w) * r;
                    q = 2 * (q - r);

                    if (q > 0) {
                        p = -p;
                    } else {
                        q = -q;
                    }

                    r = e;
                    e = d;

                    if (p > q * (a - X) &&
                        p < q * (b - X) &&
                        Math.abs(p) < Math.abs(0.5 * q * r)) {
                        // Parabolic interpolation step.
                        d = p / q;
                        u = X + d;

                        // f must not be evaluated too close to a or b.
                        if (u - a < tol2 || b - u < tol2) {
                            if (X <= m) {
                                d = tol1;
                            } else {
                                d = -tol1;
                            }
                        }
                    } else {
                        // Golden section step.
                        if (X < m) {
                            e = b - X;
                        } else {
                            e = a - X;
                        }
                        d = GOLDEN_SECTION * e;
                    }
                } else {
                    // Golden section step.
                    e = (X < m) ? (b - X) : (a - X);
                    d = GOLDEN_SECTION * e;
                }

                // Update by at least "tol1".
                u = (Math.abs(d) < tol1) ? ((d >= 0) ? (X + tol1) : (X - tol1)) : (X + d);

                double fu = func.valueOf(u);
                if (!isMinim) {
                    fu = -fu;
                }

                // User-defined convergence checker.
                previous = best(previous, current, isMinim);
                current = new double[] {u, isMinim ? fu : -fu };
                best = best(best,
                            previous,
                            isMinim);

//                if (checker != null && checker.converged(getIterations(), previous, current)) {
//                    return best;
//                }

                // Update a, b, v, w and x.
                if (fu <= Y) {
                    if (u < X) {
                        b = X;
                    } else {
                        a = X;
                    }
                    v = w;
                    fv = fw;
                    w = X;
                    fw = Y;
                    X = u;
                    Y = fu;
                } else {
                    if (u < X) {
                        a = u;
                    } else {
                        b = u;
                    }
                    if (fu <= fw ||
                        Precision.equals(w, X)) {
                        v = w;
                        fv = fw;
                        w = u;
                        fw = fu;
                    } else if (fu <= fv ||
                               Precision.equals(v, X) ||
                               Precision.equals(v, w)) {
                        v = u;
                        fv = fu;
                    }
                }
            } else { // Default termination (Brent's criterion).
                return best(best,
                        best(previous,
                                current,
                                isMinim),
                        isMinim);
            }

            //incrementIterationCount();
        }
    }

    /**
     * Selects the best of two points.
     *
     * @param a Point and value.
     * @param b Point and value.
     * @param isMinim {@code true} if the selected point must be the one with
     * the lowest value.
     * @return the best point, or {@code null} if {@code a} and {@code b} are
     * both {@code null}. When {@code a} and {@code b} have the same function
     * value, {@code a} is returned.
     */
    private double[] best(double[] a, double[] b, boolean isMinim) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }

        if (isMinim) {
            return a[1] <= b[1] ? a : b;
        } else {
            return a[1] >= b[1] ? a : b;
        }
    }
}

}
