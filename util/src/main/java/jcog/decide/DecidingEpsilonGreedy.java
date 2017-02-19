package jcog.decide;

import jcog.Util;
import jcog.data.array.Arrays;

import java.util.Random;

/**
 * Created by me on 6/9/16.
 */
public class DecidingEpsilonGreedy implements Deciding {

    private final Random random;
    float epsilonRandom; //0.01f;

    /*
    TODO - decaying epsilon:
            epsilonRandom *= epsilonRandomDecay;
            epsilonRandom = Math.max(epsilonRandom, epsilonRandomMin);
     */

    public DecidingEpsilonGreedy(float epsilonRandom, Random random) {

        this.epsilonRandom = epsilonRandom;
        this.random = random;
    }

    int motivationOrder[];

    @Override
    public int decide(float[] motivation, int lastAction) {
        int actions = motivation.length;

        if (motivationOrder == null) {
            motivationOrder = new int[actions];
            for (int i = 0; i < actions; i++)
                motivationOrder[i] = i;

        }
        if (random.nextFloat() < epsilonRandom) {
            return random.nextInt(actions);
        }

        int nextAction = -1;
        boolean equalToPreviousAction = true;
        float nextMotivation = Float.NEGATIVE_INFINITY;

        Arrays.shuffle(motivationOrder, random);

        for (int j = 0; j < motivation.length; j++) {
            int i = motivationOrder[j];
            float m = motivation[i];

            if (m > nextMotivation) {
                nextAction = i;
                nextMotivation = m;
            }
            if (equalToPreviousAction && j > 0 && !Util.equals(m, motivation[motivationOrder[j - 1]])) {
                equalToPreviousAction = false; //there is some variation
            }

        }
        if (equalToPreviousAction) //all equal?
            return lastAction;


        return nextAction;
    }
}