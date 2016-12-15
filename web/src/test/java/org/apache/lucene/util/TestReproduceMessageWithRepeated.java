/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.util;

import com.carrotsearch.randomizedtesting.annotations.Repeat;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.JUnitCore;

/**
 * Test reproduce message is right with {@link Repeat} annotation.
 */
public class TestReproduceMessageWithRepeated extends WithNestedTests {
  public static class Nested extends AbstractNestedTest {
    @Test
    @Repeat(iterations = 10)
    public void testMe() {
      throw new RuntimeException("bad");
    }
  }

  public TestReproduceMessageWithRepeated() {
    super(true);
  }

  @Test
  public void testRepeatedMessage() throws Exception { 
    String syserr = runAndReturnSyserr();
    Assert.assertTrue(syserr.contains(" -Dtests.method=testMe "));
  }

  private String runAndReturnSyserr() {
    JUnitCore.runClasses(Nested.class);
    String err = getSysErr();
    return err;
  }
}
