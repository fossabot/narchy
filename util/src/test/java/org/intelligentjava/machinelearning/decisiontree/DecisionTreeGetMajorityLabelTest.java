package org.intelligentjava.machinelearning.decisiontree;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.intelligentjava.machinelearning.decisiontree.label.BooleanLabel.FALSE_LABEL;
import static org.intelligentjava.machinelearning.decisiontree.label.BooleanLabel.TRUE_LABEL;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DecisionTreeGetMajorityLabelTest {
    
    // TODO fix handling of empty lists
//    @Test
//    public void testGetLabelOnEmptyList() {
//        DecisionTree tree = new DecisionTree();
//        List<DataSample> data = Lists.newArrayList();
//        assertNull(tree.getMajorityLabel(data));
//    }
    
    @Test
    public void testGetMajorityLabel() {
        DecisionTree<Object, Object> tree = new DecisionTree();
        List<Function<Object,Object>> data = Lists.newArrayList();
        data.add(new TestValue(TRUE_LABEL));
        data.add(new TestValue(FALSE_LABEL));
        data.add(new TestValue(TRUE_LABEL));
        data.add(new TestValue(FALSE_LABEL));
        data.add(new TestValue(FALSE_LABEL));
        assertEquals("false", DecisionTree.majority(null, data).toString());
    }

    @Test
    public void testGetMajorityLabelWhenEqualCounts() {
        DecisionTree<Object, Object> tree = new DecisionTree();
        List<Function<Object,Object>> data = Lists.newArrayList();
        data.add(new TestValue(TRUE_LABEL));
        data.add(new TestValue(FALSE_LABEL));
        data.add(new TestValue(TRUE_LABEL));
        data.add(new TestValue(FALSE_LABEL));
        assertEquals("false", DecisionTree.majority(null, data).toString());
    }
}
