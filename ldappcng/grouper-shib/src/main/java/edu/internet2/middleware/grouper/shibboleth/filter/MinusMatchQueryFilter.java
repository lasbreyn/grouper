/*
 * Copyright (C) 2004-2007 University Corporation for Advanced Internet Development, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package edu.internet2.middleware.grouper.shibboleth.filter;

import edu.internet2.middleware.grouper.filter.ComplementFilter;

/**
 * Selects objects that match the complement of two {@link MatchQueryFilter}s, e.g. the result of the first filter minus
 * the result of the second filter. An object matches this filter if it matches the first filter and not the second.
 */
public class MinusMatchQueryFilter<ValueType> extends ConditionalMatchQueryFilter<ValueType> {

  /**
   * Constructor. Creates an {@link ComplementFilter} of the given {@link MatchQueryFilter}s.
   * 
   * @param filter0 MatchQueryFilter
   * @param filter1 MatchQueryFilter
   */
  public MinusMatchQueryFilter(MatchQueryFilter filter0, MatchQueryFilter filter1) {
    this.setFilter0(filter0);
    this.setFilter1(filter1);
    this.setQueryFilter(new ComplementFilter(filter0, filter1));
  }

  /** Returns true if the object matches the first filter and not the second filter. {@inheritDoc} */
  public boolean matches(ValueType valueType) {
    return this.getFilter0().matches(valueType) && !this.getFilter1().matches(valueType);
  }
}
