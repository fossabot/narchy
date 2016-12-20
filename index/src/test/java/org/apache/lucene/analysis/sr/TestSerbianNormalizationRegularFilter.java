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
package org.apache.lucene.analysis.sr;


import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.KeywordTokenizer;

import java.io.IOException;

/**
 * Tests {@link SerbianNormalizationFilter}
 */
public class TestSerbianNormalizationRegularFilter extends BaseTokenStreamTestCase {
  private Analyzer analyzer;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    analyzer = new Analyzer() {
      @Override
      protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer tokenizer = new MockTokenizer(MockTokenizer.WHITESPACE, false);
        final TokenStream stream = new SerbianNormalizationRegularFilter(tokenizer);
        return new TokenStreamComponents(tokenizer, stream);
      }
    };
  }
  
  @Override
  public void tearDown() throws Exception {
    analyzer.close();
    super.tearDown();
  }
  
  /**
   * Tests Cyrillic text.
   */
  public void testCyrillic() throws IOException {
    checkOneTerm(analyzer, "абвгдђежзијклљмнњопрстћуфхцчџш", "abvgdđežzijklljmnnjoprstćufhcčdžš");
  }

  /**
   * Tests Latin text.
   */
  public void testLatin() throws IOException {
    checkOneTerm(analyzer, "abcčćddžđefghijklljmnnjoprsštuvzž", "abcčćddžđefghijklljmnnjoprsštuvzž");
  }

  /** blast some random strings through the analyzer */
  public void testRandomStrings() throws Exception {
    checkRandomData(random(), analyzer, 1000*RANDOM_MULTIPLIER);
  }
  
  public void testEmptyTerm() throws IOException {
    Analyzer a = new Analyzer() {
      @Override
      protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new KeywordTokenizer();
        return new TokenStreamComponents(tokenizer, new SerbianNormalizationRegularFilter(tokenizer));
      }
    };
    checkOneTerm(a, "", "");
    a.close();
  }
}
