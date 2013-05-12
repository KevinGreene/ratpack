/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ratpackframework.context.internal;

import org.ratpackframework.context.Context;
import org.ratpackframework.context.HierarchicalContextSupport;

public class ObjectHoldingHierarchicalContext extends HierarchicalContextSupport {

  private final Object value;

  public ObjectHoldingHierarchicalContext(Context parent, Object value) {
    super(parent);
    this.value = value;
  }

  @Override
  protected <T> T doMaybeGet(Class<T> type) {
    if (type.isInstance(value)) {
      return type.cast(value);
    } else {
      return null;
    }
  }

}