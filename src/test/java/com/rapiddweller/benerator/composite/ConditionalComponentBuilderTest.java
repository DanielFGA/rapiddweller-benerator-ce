/*
 * (c) Copyright 2021 by rapiddweller GmbH & Volker Bergmann. All rights reserved.
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

package com.rapiddweller.benerator.composite;

import com.rapiddweller.benerator.engine.DefaultBeneratorContext;
import com.rapiddweller.benerator.sample.ConstantGenerator;
import com.rapiddweller.benerator.wrapper.ProductWrapper;
import com.rapiddweller.model.data.DataModel;
import com.rapiddweller.model.data.DefaultDescriptorProvider;
import com.rapiddweller.model.data.DescriptorProvider;
import com.rapiddweller.model.data.Entity;
import com.rapiddweller.script.expression.ConstantExpression;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests the {@link ConditionalComponentBuilder}.<br/><br/>
 * Created: 02.10.2021 00:00:50
 * @author Volker Bergmann
 * @since 2.1.0
 */
public class ConditionalComponentBuilderTest {

  @Test
  public void testConditionTrue() {
    check(true);
  }

  @Test
  public void testConditionFalse() {
    check(false);
  }

  public void check(boolean condition) {
    ConstantGenerator<String> gen = new ConstantGenerator<>("XXXXX");
    ComponentBuilder<Entity> source = new PlainEntityComponentBuilder("name", gen, null);
    ConditionalComponentBuilder<Entity> builder = new ConditionalComponentBuilder<>(source, new ConstantExpression<>(condition));
    DefaultBeneratorContext context = new DefaultBeneratorContext();
    builder.init(context);
    assertEquals(source.isThreadSafe(), builder.isThreadSafe());
    assertEquals(source.isParallelizable(), builder.isParallelizable());
    DataModel dm = new DataModel();
    DescriptorProvider dp = new DefaultDescriptorProvider("default", dm);
    Entity entity = new Entity("x", dp);
    entity.set("name", "Alice");
    context.setCurrentProduct(new ProductWrapper<>(entity));
    builder.execute(context);
    if (condition) {
      assertEquals("XXXXX", entity.get("name"));
    } else {
      assertEquals("Alice", entity.get("name"));
    }
    assertNull(builder.getMessage());
    builder.close();
  }

}
