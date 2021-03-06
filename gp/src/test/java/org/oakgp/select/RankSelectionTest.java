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
package org.oakgp.select;

import org.junit.jupiter.api.Test;
import org.oakgp.rank.Candidates;
import org.oakgp.rank.RankedCandidate;
import org.oakgp.util.DummyRandom;
import org.oakgp.util.GPRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.oakgp.TestUtils.integerConstant;

public class RankSelectionTest {
    @Test
    public void test() {
        RankedCandidate c1 = new RankedCandidate(integerConstant(1), 1);
        RankedCandidate c2 = new RankedCandidate(integerConstant(2), 2);
        RankedCandidate c3 = new RankedCandidate(integerConstant(3), 3);
        Candidates candidates = new Candidates(new RankedCandidate[]{c1, c2, c3});

        DummyRandom r = new DummyRandom(.0, .49, .5, .83, .84, .999, .25, .65, .93);
        NodeSelector s = createRankSelection(r, candidates);

        assertEquals(c1.node, s.next());
        assertEquals(c1.node, s.next());
        assertEquals(c2.node, s.next());
        assertEquals(c2.node, s.next());
        assertEquals(c3.node, s.next());
        assertEquals(c3.node, s.next());
        assertEquals(c1.node, s.next());
        assertEquals(c2.node, s.next());
        assertEquals(c3.node, s.next());

        r.assertEmpty();
    }

    private RankSelection createRankSelection(GPRandom random, Candidates candidates) {
        RankSelectionFactory f = new RankSelectionFactory(random);
        return f.getSelector(candidates);
    }
}
