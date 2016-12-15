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
package org.apache.lucene.codecs;


import org.apache.lucene.index.Fields;
import org.apache.lucene.util.Accountable;

import java.io.Closeable;
import java.io.IOException;

/** Abstract API that produces terms, doc, freq, prox, offset and
 *  payloads postings.  
 *
 * @lucene.experimental
 */

public abstract class FieldsProducer extends Fields implements Closeable, Accountable {
  /** Sole constructor. (For invocation by subclass 
   *  constructors, typically implicit.) */
  protected FieldsProducer() {
  }

  @Override
  public abstract void close() throws IOException;
  
  /** 
   * Checks consistency of this reader.
   * <p>
   * Note that this may be costly in terms of I/O, e.g. 
   * may involve computing a checksum value against large data files.
   * @lucene.internal
   */
  public abstract void checkIntegrity() throws IOException;
  
  /** 
   * Returns an instance optimized for merging.
   * <p>
   * The default implementation returns {@code this} */
  public FieldsProducer getMergeInstance() throws IOException {
    return this;
  }
}
