/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 	* Redistributions of source code must retain the above copyright notice,
 * 	  this list of conditions and the following disclaimer.
 * 	* Redistributions in binary form must reproduce the above copyright notice,
 * 	  this list of conditions and the following disclaimer in the documentation
 * 	  and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package spacegraph.space2d.phys.pooling;

import spacegraph.space2d.phys.common.Vec2;
import spacegraph.util.math.Tuple2f;

import java.util.HashMap;

/**
 * not thread safe Vec2[] pool
 *
 * @author dmurph
 */
public class Vec2Array {

    private final HashMap<Integer, Tuple2f[]> map = new HashMap<>();

    public Tuple2f[] get(int argLength) {
        assert (argLength > 0);

        if (!map.containsKey(argLength)) {
            map.put(argLength, getInitializedArray(argLength));
        }

        assert (map.get(argLength).length == argLength) : "Array not built of correct length";
        return map.get(argLength);
    }

    protected static Tuple2f[] getInitializedArray(int argLength) {
        final Tuple2f[] ray = new Tuple2f[argLength];
        for (int i = 0; i < ray.length; i++) {
            ray[i] = new Vec2();
        }
        return ray;
    }
}
