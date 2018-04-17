package jcog.decide;

import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

import java.util.Arrays;
import java.util.Random;
import java.util.function.IntPredicate;

import static java.lang.Float.MIN_NORMAL;

/** efficient embeddable roulette decision executor.
 * takes a weight-update function applied to the current
 * choice after it has been selected in order to influence
 * its probability of being selected again.
 *
 * if the update function returns zero, then the choice is
 * deactivated and can not be selected again.
 *
 * when no choices remain, it exits automatically.
 */
public class MutableRoulette {

    /** weights of each choice */
    private final float[] w;

    private final Random rng;

    final static float EPSILON = MIN_NORMAL;

    /** weight update function applied between selections to the last selected index's weight */
    private final FloatToFloatFunction weightUpdate;

    /** current weight sum */
    private float weightSum;

    /** current # entries remaining above epsilon threshold */
    private int remaining;

    /** current index (roulette ball position) */
    int i;

    public static void run(float[] weights, FloatToFloatFunction weightUpdate, IntPredicate select, Random rng) {
        switch (weights.length) {
            case 0: throw new UnsupportedOperationException();
            case 1: select.test(0); return;
            //TODO simple 2 case, with % probability of the ordering then try the order in sequence
            default: {
                MutableRoulette r = new MutableRoulette(weights, weightUpdate, rng);
                while (r.next(select)) { }
            }
            break;
        }
    }


    MutableRoulette(float[] w, FloatToFloatFunction weightUpdate, Random rng) {
        this.w = w;
        int n = w.length;
        this.remaining = n;
        this.weightUpdate = weightUpdate;

        float s = 0;

        for (int i = 0; i < n; i++) {
            float wi = w[i];
            assert(wi >= 0);
            if (wi < EPSILON) {
                w[i] = 0; //hard set to zero
                remaining--;
            } else {
                s += wi;
            }
        }

        if (remaining == 0 || s < (n+1)* EPSILON) {
            //flat
            Arrays.fill(w, 1);
            s = remaining = n;
        }

        this.weightSum = s;
        this.i = (this.rng = rng).nextInt(n); //random start position
    }

    public boolean next(IntPredicate select) {
        return select.test(next()) && --remaining > 0;
    }

    private int next() {

        assert(remaining > 0);

        float[] w = this.w;

        int count = w.length;

        if (remaining == 1) {
            //special case:
            //there is only one with non-zero weight remaining

            for (int x = 0; x < count; x++)
                if (w[x] > 0)
                    return x;

            throw new RuntimeException();
        } else {

            float distance = rng.nextFloat() * weightSum;
            //boolean dir = rng.nextBoolean(); //randomize the direction

            int i = this.i;

            float wi;
            while ((distance = distance - (wi = w[i])) > EPSILON) {
                //if (dir) {
                if (++i == count) i = 0;
                //} else {
                //  if (--i == -1) i = count - 1;
                //}
            }

            float nextWeight = weightUpdate.valueOf(wi);
            if (nextWeight!=wi) {
                float delta = nextWeight - wi;
                w[i] = nextWeight;
                weightSum += delta;
                if (nextWeight < EPSILON)
                    remaining--;
            }

            this.i = i;

            return i;
        }

    }

}
