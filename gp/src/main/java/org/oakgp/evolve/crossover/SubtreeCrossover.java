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
package org.oakgp.evolve.crossover;

import org.oakgp.Type;
import org.oakgp.evolve.GeneticOperator;
import org.oakgp.node.Node;
import org.oakgp.node.walk.DepthWalk;
import org.oakgp.node.walk.StrategyWalk;
import org.oakgp.select.NodeSelector;
import org.oakgp.util.GPRandom;
import org.oakgp.util.Utils;

import java.util.function.Predicate;

/**
 * Replaces a randomly selected subtree of one parent with a randomly selected subtree of another parent.
 * <p>
 * A subtree is selected at random from each of the parents. The subtree of the first is replaced by the subtree of the second. The structure of the resulting
 * tree can differ significantly from either of the trees that were combined to produce it.
 * </p>
 */
public final class SubtreeCrossover implements GeneticOperator {
    private final GPRandom random;
    private final int maxDepth;

    /**
     * Creates a {@code SubtreeCrossover} that uses the given {@code Random} to select subtrees from parents.
     *
     * @param random   used to randomly select subtrees to replace and the subtrees to replace them with
     * @param maxDepth used to enforce a maximum depth of any offspring
     */
    public SubtreeCrossover(GPRandom random, int maxDepth) {
        this.random = random;
        this.maxDepth = maxDepth;
    }

    @Override
    public Node evolve(NodeSelector selector) {
        Node parent1 = selector.next();
        Node parent2 = selector.next();
        if (parent1.equals(parent2))
            return parent1;
        int to = Utils.selectSubNodeIndex(random, parent1);
        return DepthWalk.replaceAt(parent1, to, (t, d) -> {
            int maxHeightParent2 = maxDepth - d;
            Type toType = t.returnType();
            Predicate<Node> treeWalkerStrategy = n -> n.returnType() == toType && n.depth() <= maxHeightParent2 + 1;
            int nodeCount = StrategyWalk.getNodeCount(parent2, treeWalkerStrategy);
            if (nodeCount == 0) {
                return t;
            } else {
                int from = random.nextInt(nodeCount);
                return StrategyWalk.getAt(parent2, from, treeWalkerStrategy);
            }
        });
    }
}
