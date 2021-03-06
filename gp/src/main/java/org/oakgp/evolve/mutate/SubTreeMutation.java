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
package org.oakgp.evolve.mutate;

import org.oakgp.evolve.GeneticOperator;
import org.oakgp.generate.TreeGenerator;
import org.oakgp.node.Node;
import org.oakgp.node.walk.NodeWalk;
import org.oakgp.select.NodeSelector;
import org.oakgp.util.GPRandom;
import org.oakgp.util.Utils;

/**
 * Randomly replaces a subtree of the parent with a newly generated subtree.
 */
public final class SubTreeMutation implements GeneticOperator {
    private final GPRandom random;
    private final TreeGenerator treeGenerator;

    /**
     * Creates a {@code SubTreeMutation} that uses the given {@code TreeGenerator} to generate new subtrees to replace existing subtrees.
     *
     * @param random        used to randomly select nodes to mutate
     * @param treeGenerator used to generate new subtrees to replace nodes selected for mutation
     */
    public SubTreeMutation(GPRandom random, TreeGenerator treeGenerator) {
        this.random = random;
        this.treeGenerator = treeGenerator;
    }

    @Override
    public Node evolve(NodeSelector selector) {
        Node root = selector.next();
        int mutationPoint = Utils.selectSubNodeIndex(random, root);
        return NodeWalk.replaceAt(root, mutationPoint, node -> treeGenerator.generate(node.returnType(), node.depth()));
    }
}
