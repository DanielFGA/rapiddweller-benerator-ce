/*
 * (c) Copyright 2006-2021 by rapiddweller GmbH & Volker Bergmann. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, is permitted under the terms of the
 * GNU General Public License.
 *
 * For redistributing this software or a derivative work under a license other
 * than the GPL-compatible Free Software License as defined by the Free
 * Software Foundation or approved by OSI, you must first obtain a commercial
 * license to this software product from rapiddweller GmbH & Volker Bergmann.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * WITHOUT A WARRANTY OF ANY KIND. ALL EXPRESS OR IMPLIED CONDITIONS,
 * REPRESENTATIONS AND WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE
 * HEREBY EXCLUDED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.rapiddweller.benerator.util;

import com.rapiddweller.benerator.Generator;
import com.rapiddweller.benerator.wrapper.GeneratorProxy;
import com.rapiddweller.benerator.wrapper.ProductWrapper;
import com.rapiddweller.common.exception.ExceptionFactory;
import com.rapiddweller.common.Expression;

/**
 * Generator proxy which takes the input of another Generator and only
 * passes it if a boolean expression evaluates to true.<br/><br/>
 * Created: 11.03.2010 14:23:53
 * @param <E> the type parameter
 * @author Volker Bergmann
 * @since 0.6.0
 */
public class FilteringGenerator<E> extends GeneratorProxy<E> {

  private final Expression<Boolean> filter;

  public FilteringGenerator(Generator<E> source, Expression<Boolean> filter) {
    super(source);
    this.filter = filter;
  }

  @Override
  public ProductWrapper<E> generate(ProductWrapper<E> wrapper) {
    ProductWrapper<E> feed;
    while ((feed = super.generate(wrapper)) != null) {
      E candidate = feed.unwrap();
      context.set("_candidate", candidate);
      Boolean evaluation = filter.evaluate(context);
      if (evaluation == null) {
        throw ExceptionFactory.getInstance().missingInfo("filter expression is null");
      }
      if (evaluation) {
        return wrapper.wrap(candidate);
      }
    }
    return null;
  }

}
