/*
 * Copyright 2015 S. Webber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.oakgp.node;

import jcog.Util;
import org.oakgp.Arguments;
import org.oakgp.Assignments;
import org.oakgp.Type;
import org.oakgp.function.Function;

/**
 * Contains a function (operator) and the arguments (operands) to apply to it.
 */
public final class FunctionNode implements Node {

    private final Function function;
    private final Arguments arguments;
    private final int nodeCount;
    private final int hash;

    /**
     * Constructs a new {@code FunctionNode} with the specified function function and arguments.
     *
     * @param function  the function to associate with this {@code FunctionNode}
     * @param arguments the arguments (i.e. operands) to apply to {@code function} when evaluating this {@code FunctionNode}
     */
    public FunctionNode(Function function, Node... arguments) {
        this(function, Arguments.get(function, arguments));
    }

    /**
     * Constructs a new {@code FunctionNode} with the specified function function and arguments.
     *
     * @param function  the function to associate with this {@code FunctionNode}
     * @param arguments the arguments (i.e. operands) to apply to {@code function} when evaluating this {@code FunctionNode}
     */
    public FunctionNode(Function function, Arguments arguments) {
        if (function.argsSorted()) {
//            if (arguments.length() > 1 && !(arguments instanceof Arguments.SortedArguments)) {
//                System.err.println("could have been pre-sorted");
//            }
            arguments = arguments.sorted();
        }

        this.function = function;
        this.arguments = arguments;
        this.nodeCount = calculateNodeCount(arguments);
        //this.hashCode = (function.getClass().getName().hashCode() * 31) * createHashCode(arguments, nodeCount);
        this.hash = Util.hashCombine(function.getClass().hashCode(), arguments.hashCode());
    }

    private static int calculateNodeCount(Arguments arguments) {
        int total = 1;
        int n = arguments.length();
        for (int i = 0; i < n; i++) {
            total += arguments.get(i).size();
        }
        return total;
    }

    public Function func() {
        return function;
    }

    public Arguments args() {
        return arguments;
    }


    @SuppressWarnings("unchecked")
    @Override
    public Object eval(Assignments assignments) {
        return function.evaluate(arguments, assignments);
    }

    @Override
    public int size() {
        return nodeCount;
    }

    @Override
    public int depth() {
        return arguments.depth;
    }

    @Override
    public Type returnType() {
        return function.sig().returnType();
    }

    @Override
    public NodeType nodeType() {
        return NodeType.FUNCTION;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || this.hash != o.hashCode()) {
            return false;
        } else if (o instanceof FunctionNode) {
            FunctionNode fn = (FunctionNode) o;
            // NOTE if we often return false here then that indicates hashCode() could be improved
            return this.function == fn.function && this.arguments.equals(fn.arguments);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append('(').append(function.name());
        int n = arguments.length();
        for (int i = 0; i < n; i++) {
            sb.append(' ').append(arguments.get(i));
        }
        return sb.append(')').toString();
    }
}
