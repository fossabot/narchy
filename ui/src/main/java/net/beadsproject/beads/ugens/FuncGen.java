/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.ugens;

import net.beadsproject.beads.core.UGen;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

/**
 * Function is an abstract class which can be quickly subclassed to make a custom {@link UGen} on the fly. Subclasses of Function must implement {@link #asFloat()}, getting data from the array {@link #x}, and returning the result. {@link #x} provides access to the array of {@link UGen}s that are passed to Function's constructor.
 * <p>
 * apply(float[]) calculates what to do.
 *
 * @author ollie
 * @return the result of the calculation.
 * @beads.category utilities
 */
public abstract class FuncGen extends UGen implements FloatFunction<float[]> {

    // TODO curious challenge:
    //can we get Function to not bother updating if its inputs are not updating?
    //could UGen have a method 'hasChanged()'?

    /**
     * An array representing the current values from the array of input UGens.
     * TODO use ArrayTensor
     */
    protected final float[] x;

    /**
     * The inputs.
     */
    private final UGen[] inputs;


    /**
     * Instantiates a new function.
     *
     * @param inputs the set of input UGens.
     */
    public FuncGen(UGen... inputs) {
        super(inputs[0].getContext(), 1);
        this.inputs = inputs;
        this.x = new float[inputs.length];
    }

    /* (non-Javadoc)
     * @see net.beadsproject.beads.core.UGen#calculateBuffer()
     */
    @Override
    public final void gen() {
        for (int i = 0; i < inputs.length; i++) {
            inputs[i].update();
        }
        for (int i = 0; i < bufferSize; i++) {
            for (int j = 0; j < inputs.length; j++) {
                x[j] = inputs[j].getValue(0, i);
            }
            bufOut[0][i] = floatValueOf(x);
        }
    }


}
