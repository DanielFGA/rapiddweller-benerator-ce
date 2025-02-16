/*
 * (c) Copyright 2006-2020 by rapiddweller GmbH & Volker Bergmann. All rights reserved.
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

package com.rapiddweller.benerator.test;

import com.rapiddweller.benerator.Generator;
import com.rapiddweller.benerator.NonNullGenerator;
import com.rapiddweller.benerator.engine.DefaultBeneratorContext;
import com.rapiddweller.benerator.factory.BeneratorExceptionFactory;
import com.rapiddweller.benerator.primitive.number.AbstractNonNullNumberGenerator;
import com.rapiddweller.benerator.wrapper.ProductWrapper;
import com.rapiddweller.common.BeanUtil;
import com.rapiddweller.common.CollectionUtil;
import com.rapiddweller.common.Converter;
import com.rapiddweller.common.Filter;
import com.rapiddweller.common.IOUtil;
import com.rapiddweller.common.Resettable;
import com.rapiddweller.common.Validator;
import com.rapiddweller.common.collection.ObjectCounter;
import com.rapiddweller.common.converter.ToStringConverter;
import com.rapiddweller.common.ui.ConsolePrinter;
import com.rapiddweller.common.ui.TextPrinter;
import com.rapiddweller.common.validator.UniqueValidator;
import com.rapiddweller.model.data.Entity;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Provides methods for testing generators.<br/><br/>
 * Created: 15.11.2007 14:46:31
 * @author Volker Bergmann
 */
public abstract class GeneratorTest extends ModelTest {

  private final Converter<Object, String> formatter = new ToStringConverter();

  // helper methods for this and child classes -----------------------------------------------------------------------

  public <T extends Generator<U>, U> T initialize(T generator) {
    generator.init(context);
    return generator;
  }

  public void close(Generator<?> generator) {
    IOUtil.close(generator);
  }

  public void setCurrentProduct(Object product, String productName) {
    ((DefaultBeneratorContext) context).setCurrentProduct(new ProductWrapper<>(product), productName);
  }

  public void printProducts(Generator<?> generator, int n) {
    printProducts(generator, n, new ConsolePrinter());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public void printProducts(Generator<?> generator, int n, TextPrinter printer) {
    ProductWrapper wrapper = new ProductWrapper();
    for (int i = 0; i < n; i++) {
      ProductWrapper<?> tmp = generator.generate(wrapper);
      if (tmp == null) {
        printer.printStd("<>");
        return;
      } else {
        printer.printStd(formatter.convert(tmp.unwrap()));
      }
    }
  }

  /** Collects the products generated by the generator.
   *  @param maxCount the maximum number of generations to check. If maxCount == 0 then no maximum is imposed
   *  @return a {@link List} with all the generated products
   */
  public <T> List<T> generate(Generator<T> generator, int maxCount) {
    List<T> result = new ArrayList<>();
    ProductWrapper<T> wrapper = new ProductWrapper<>();
    for (int i = 0; i < maxCount || maxCount <= 0; i++) {
      wrapper = generator.generate(wrapper);
      if (wrapper != null) {
        result.add(wrapper.unwrap());
      } else {
        return result;
      }
    }
    return result;
  }

  public static <T> Map<T, AtomicInteger> countProducts(Generator<T> generator, int n) {
    ObjectCounter<T> counter = new ObjectCounter<>(Math.min(n, 1000));
    ProductWrapper<T> wrapper = new ProductWrapper<>();
    for (int i = 0; i < n; i++) {
      wrapper = generator.generate(wrapper);
      if (wrapper == null) {
        fail("Generator unavailable after " + i + " of " + n + " invocations");
      } else {
        counter.count(wrapper.unwrap());
      }
    }
    return counter.getCounts();
  }

  public static void assertTrueQuota(Collection<Entity> entities, String componentName, double expectedQuota, double tolerance) {
    assertMatchQuota(entities, componentName, expectedQuota, tolerance, Boolean.TRUE::equals);
  }

  public static void assertNullQuota(Collection<Entity> entities, String componentName, double expectedQuota, double tolerance) {
    assertMatchQuota(entities, componentName, expectedQuota, tolerance, Objects::isNull);
  }

  public static void assertAllMatch(
      Collection<Entity> entities, String componentName, double tolerance, Filter<Object> matcher) {
    assertMatchQuota(entities, componentName, 1., tolerance, matcher);
  }

  public static void assertMatchQuota(Collection<Entity> entities, String componentName, double expectedQuota,
                                      double tolerance, Filter<Object> matcher) {
    int matchCount = 0;
    for (Entity entity : entities) {
      Object component = entity.get(componentName);
      if (matcher.accept(component)) {
        matchCount++;
      }
    }
    double actualQuota = (double) matchCount / entities.size();
    assertEquals(expectedQuota, actualQuota, tolerance);
  }

  public static void assertEqualCardinalityDistribution(
      Collection<Entity> entities, String partName, double tolerance, Integer... expectedCounts) {
    ObjectCounter<Integer> counter = countPartCounts(entities, partName);
    checkDistribution(counter, true, tolerance, CollectionUtil.toSet(expectedCounts));
  }

  public static void assertEqualLengthDistribution(
      Collection<Entity> entities, String partName, double tolerance, Integer... expectedLengths) {
    ObjectCounter<Integer> counter = countPartLengths(entities, partName);
    checkDistribution(counter, true, tolerance, CollectionUtil.toSet(expectedLengths));
  }

  public static void assertEqualDistribution(
      Collection<Entity> entities, String partName, double tolerance, Object... expectedValues) {
    ObjectCounter<Object> counter = countPartValues(entities, partName);
    assertEqualDistribution(counter, tolerance, expectedValues);
  }

  @SafeVarargs
  protected static <T> Helper expectGeneratedSequence(Generator<T> generator, T... products) {
    expectGeneratedSequenceOnce(generator, products);
    generator.reset();
    expectGeneratedSequenceOnce(generator, products);
    return new Helper(generator);
  }

  @SafeVarargs
  protected final <T> Helper expectGeneratedSet(Generator<T> generator, int invocations, T... products) {
    expectGeneratedSetOnce(generator, invocations, products);
    generator.reset();
    expectGeneratedSetOnce(generator, invocations, products);
    return new Helper(generator);
  }

  @SafeVarargs
  protected final <T> Helper expectUniquelyGeneratedSet(Generator<T> generator, T... products) {
    expectUniquelyGeneratedSetOnce(generator, products);
    generator.reset();
    expectUniquelyGeneratedSetOnce(generator, products);
    return new Helper(generator);
  }

  protected <T> Helper expectUniqueProducts(Generator<T> generator, int n) {
    expectUniqueProductsOnce(generator, n);
    generator.reset();
    expectUniqueProductsOnce(generator, n);
    return new Helper(generator);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected <T> Helper expectGenerations(Generator<T> generator, int n, Validator... validators) {
    expectGenerationsOnce(generator, n, validators);
    generator.reset();
    for (Validator<?> validator : validators) {
      if (validator instanceof Resettable) {
        ((Resettable) validator).reset();
      }
    }
    expectGenerationsOnce(generator, n, validators);
    return new Helper(generator);
  }

  protected <T> Helper expectUniqueGenerations(Generator<T> generator, int n) {
    expectUniqueGenerationsOnce(generator, n);
    generator.reset();
    expectUniqueGenerationsOnce(generator, n);
    return new Helper(generator);
  }

  protected <T extends Comparable<T>> void expectRange(Generator<T> generator, int n, T min, T max) {
    expectRangeOnce(generator, n, min, max);
    generator.reset();
    expectRangeOnce(generator, n, min, max);
    new Helper(generator);
  }

  protected <T> String format(T product) {
    return ToStringConverter.convert(product, "[null]");
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static void assertUnavailable(Generator<?> generator) {
    assertNull("Generator " + generator + " is expected to be unavailable", generator.generate(new ProductWrapper()));
  }

  public static void assertAvailable(Generator<?> generator) {
    assertAvailable("Generator " + generator + " is expected to be available", generator);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static void assertAvailable(String message, Generator<?> generator) {
    assertNotNull(message, generator.generate(new ProductWrapper()));
  }

  // Number generator tests ------------------------------------------------------------------------------------------

  @SafeVarargs
  public static <T extends Number> void checkEqualDistribution(
      Class<? extends AbstractNonNullNumberGenerator<T>> generatorClass, T min, T max, T granularity,
      int iterations, double tolerance, T... expectedValues) {
    Set<T> expectedSet = CollectionUtil.toSet(expectedValues);
    checkDistribution(generatorClass, min, max, granularity, iterations, true, tolerance, expectedSet);
  }

  public static <T extends Number> void checkEqualDistribution(
      Class<? extends AbstractNonNullNumberGenerator<T>> generatorClass, T min, T max, T granularity,
      int iterations, double tolerance, Set<T> expectedSet) {
    checkDistribution(generatorClass, min, max, granularity, iterations, true, tolerance, expectedSet);
  }

  public static <T extends Number> void checkDistribution(
      Class<? extends AbstractNonNullNumberGenerator<T>> generatorClass, T min, T max, T granularity,
      int iterations, boolean equalDistribution, double tolerance, Set<T> expectedSet) {
    try (AbstractNonNullNumberGenerator<T> generator = BeanUtil.newInstance(generatorClass)) {
      generator.setMin(min);
      generator.setMax(max);
      generator.setGranularity(granularity);
      ObjectCounter<T> counter = new ObjectCounter<>(expectedSet != null ? expectedSet.size() : 10);
      ProductWrapper<T> wrapper = new ProductWrapper<>();
      for (int i = 0; i < iterations; i++) {
        ProductWrapper<T> tmp = generator.generate(wrapper);
        if (tmp != null) {
          counter.count(tmp.unwrap());
        } else {
          fail(iterations + " iterations requested, but generator is depleted after " + i + " iterations");
        }
      }
      checkDistribution(counter, equalDistribution, tolerance, expectedSet);
    }
  }

  // unspecific generator tests --------------------------------------------------------------------------------------

  public static <T> void checkEqualDistribution(
      Generator<T> generator, int iterations, double tolerance, Set<T> expectedSet) {
    checkDistribution(generator, iterations, true, tolerance, expectedSet);
  }

  public static <T> void checkProducts(Generator<T> generator, int iterations, T... expectedProducts) {
    checkDistribution(generator, iterations, false, 0, CollectionUtil.toSet(expectedProducts));
  }

  public static <T> void checkProductSet(Generator<T> generator, int iterations, Set<T> expectedSet) {
    checkDistribution(generator, iterations, false, 0, expectedSet);
  }

  private static <T> void checkDistribution(Generator<T> generator,
                                            int iterations, boolean equalDistribution, double tolerance, Set<T> expectedSet) {
    ObjectCounter<T> counter = new ObjectCounter<>(expectedSet != null ? expectedSet.size() : 10);
    ProductWrapper<T> wrapper = new ProductWrapper<>();
    for (int i = 0; i < iterations; i++) {
      counter.count(generator.generate(wrapper).unwrap());
    }
    checkDistribution(counter, equalDistribution, tolerance, expectedSet);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  protected static void expectRelativeWeights(Generator<?> generator, int iterations, Object... expectedValueWeightPairs) {
    ObjectCounter<Object> counter = new ObjectCounter<>(expectedValueWeightPairs.length / 2);
    ProductWrapper wrapper = new ProductWrapper();
    for (int i = 0; i < iterations; i++) {
      wrapper = generator.generate(wrapper);
      if (wrapper == null) {
        fail("Generator unavailable after " + i + " of " + iterations + " invocations");
      } else {
        counter.count(wrapper.unwrap());
      }
    }
    expectRelativeWeights(counter, expectedValueWeightPairs);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  protected static void expectRelativeWeights(ObjectCounter counter, Object... expectedValueWeightPairs) {
    Set<?> productSet = counter.objectSet();
    double totalExpectedWeight = 0;
    for (int i = 1; i < expectedValueWeightPairs.length; i += 2) {
      totalExpectedWeight += ((Number) expectedValueWeightPairs[i]).doubleValue();
    }

    for (int i = 0; i < expectedValueWeightPairs.length; i += 2) {
      Object value = expectedValueWeightPairs[i];
      if (totalExpectedWeight == 0) {
        totalExpectedWeight = 0.00000001;
      }
      double expectedWeight = ((Number) expectedValueWeightPairs[i + 1]).doubleValue() / totalExpectedWeight;
      if (expectedWeight > 0) {
        assertTrue("Generated set does not contain value " + value, productSet.contains(value));
        double measuredWeight = counter.getRelativeCount(value);
        assertTrue("For value '" + value + "', weight " + expectedWeight + " is expected, but it is " + measuredWeight,
            Math.abs(measuredWeight - expectedWeight) / expectedWeight < 0.15);
      } else {
        assertFalse("Generated contains value " + value + " though it has zero weight", productSet.contains(value));
      }
    }
  }

  // collection checks -----------------------------------------------------------------------------------------------

  public static <T> void checkEqualDistribution(Collection<T> collection, double tolerance, Set<T> expectedSet) {
    checkDistribution(collection, true, tolerance, expectedSet);
  }

  public static void assertEqualLengthDistribution(Collection<String> collection, double tolerance,
                                                   Integer... expectedValues) {
    ObjectCounter<Integer> counter = new ObjectCounter<>();
    for (String object : collection) {
      counter.count(object.length());
    }
    Set<Integer> expectedSet = (expectedValues != null ? CollectionUtil.toSet(expectedValues) : null);
    checkDistribution(counter, true, tolerance, expectedSet);
  }

  public static <T> void checkDistribution(
      Collection<T> collection, boolean equalDistribution, double tolerance, Set<T> expectedSet) {
    ObjectCounter<T> counter = new ObjectCounter<>(expectedSet != null ? expectedSet.size() : 10);
    for (T object : collection) {
      counter.count(object);
    }
    checkDistribution(counter, equalDistribution, tolerance, expectedSet);
  }

  // counter checks --------------------------------------------------------------------------------------------------

  public static <T> void checkEqualDistribution(
      ObjectCounter<T> counter, double tolerance, Set<T> expectedSet) {
    checkDistribution(counter, true, tolerance, expectedSet);
  }

  public static <T> void assertEqualDistribution(ObjectCounter<T> counter, double tolerance, T... expectedValues) {
    checkDistribution(counter, true, tolerance, CollectionUtil.toSet(expectedValues));
  }

  private static <T> void checkDistribution(
      ObjectCounter<T> counter, boolean equalDistribution, double tolerance, Set<T> expectedSet) {
    if (equalDistribution) {
      assertEquals(expectedSet, counter.objectSet());
      assertTrue("Distribution is not equal: " + counter, counter.equalDistribution(tolerance));
    }
    if (expectedSet != null) {
      assertEquals(expectedSet, counter.objectSet());
    }
  }

  public static class Helper {
    private final Generator<?> generator;

    public Helper(Generator<?> generator) {
      this.generator = generator;
    }

    public void withCeasedAvailability() {
      try {
        assertUnavailable(generator);
      } finally {
        generator.close();
      }
    }

    public void withContinuedAvailability() {
      assertAvailable(generator);
    }
  }

  // private helpers -------------------------------------------------------------------------------------------------

  private static ObjectCounter<Integer> countPartCounts(Collection<Entity> entities, String partName) {
    ObjectCounter<Integer> counter = new ObjectCounter<>();
    for (Entity entity : entities) {
      Object partValue = entity.get(partName);
      int partCount;
      if (partValue == null) {
        partCount = 0;
      } else if (partValue instanceof Collection) {
        partCount = ((Collection) partValue).size();
      } else if (partValue.getClass().isArray()) {
        partCount = Array.getLength(partValue);
      } else {
        partCount = 1;
      }
      counter.count(partCount);
    }
    return counter;
  }

  private static ObjectCounter<Integer> countPartLengths(Collection<Entity> entities, String partName) {
    ObjectCounter<Integer> counter = new ObjectCounter<>();
    for (Entity entity : entities) {
      Object partValue = entity.get(partName);
      if (partValue != null) {
        if (partValue instanceof String) {
          counter.count(((String) partValue).length());
        } else {
          throw BeneratorExceptionFactory.getInstance().illegalArgument("Expected String, found " + partValue.getClass());
        }
      }
    }
    return counter;
  }

  private static ObjectCounter<Object> countPartValues(Collection<Entity> entities, String partName) {
    ObjectCounter<Object> counter = new ObjectCounter<>();
    for (Entity entity : entities) {
      counter.count(entity.get(partName));
    }
    return counter;
  }

  @SafeVarargs
  protected static <T> void expectGeneratedSequenceOnce(Generator<T> generator, T... products) {
    int count = 0;
    ProductWrapper<T> wrapper = new ProductWrapper<>();
    for (T expectedProduct : products) {
      wrapper = generator.generate(wrapper);
      assertNotNull("Generator is unavailable after generating " + count + " of " + products.length + " products: " + generator, wrapper);
      T generatedProduct = wrapper.unwrap();
      if (generatedProduct.getClass().isArray()) {
        assertEqualArrays(expectedProduct, generatedProduct);
      } else {
        assertEquals(expectedProduct, generatedProduct);
      }
      count++;
    }
  }

  @SafeVarargs
  private <T> void expectGeneratedSetOnce(Generator<T> generator, int invocations, T... expectedProducts) {
    Set<T> expectedSet = CollectionUtil.toSet(expectedProducts);
    Set<T> observedSet = new HashSet<>(expectedProducts.length);
    ProductWrapper<T> wrapper = new ProductWrapper<>();
    for (int i = 0; i < invocations; i++) {
      wrapper = generator.generate(wrapper);
      assertNotNull("Generator has gone unavailable. " +
              "Generated only " + i + " of " + expectedProducts.length + " expected values: " + observedSet,
          wrapper);
      T generation = wrapper.unwrap();
      logProduct(generation);
      assertTrue("The generated value '" + format(generation) + "' was not in the expected set: " + expectedSet,
          expectedSet.contains(generation));
      observedSet.add(generation);
    }
    assertEquals(expectedSet, observedSet);
  }

  private <T> void logProduct(T product) {
    if (logger.isDebugEnabled()) {
      logger.debug("created {}", format(product));
    }
  }

  @SafeVarargs
  private <T> void expectUniquelyGeneratedSetOnce(Generator<T> generator, T... expectedProducts) {
    Set<T> expectedSet = CollectionUtil.toSet(expectedProducts);
    UniqueValidator<Object> validator = new UniqueValidator<>();
    ProductWrapper<T> wrapper = new ProductWrapper<>();
    for (int i = 0; i < expectedProducts.length; i++) {
      wrapper = generator.generate(wrapper);
      assertNotNull("Generator has gone unavailable after " + i + " products, " +
          "expected " + expectedProducts.length + " products. ", wrapper);
      T product = wrapper.unwrap();
      logProduct(format(product));
      assertTrue("Product is not unique: " + product, validator.valid(product));
      assertTrue("'" + format(product) + "' was not in the expected set: "
          + format(expectedSet), expectedSet.contains(product));
    }
  }

  private <T> void expectUniqueProductsOnce(Generator<T> generator, int n) {
    UniqueValidator<T> validator = new UniqueValidator<>();
    ProductWrapper<T> wrapper = new ProductWrapper<>();
    for (int i = 0; i < n; i++) {
      wrapper = generator.generate(wrapper);
      assertNotNull("Generator is not available: " + generator, wrapper);
      T product = wrapper.unwrap();
      logProduct(product);
      assertTrue("Product is not unique: " + product, validator.valid(product));
    }
  }

  @SafeVarargs
  private <T> void expectGenerationsOnce(Generator<T> generator, int n, Validator<T>... validators) {
    ProductWrapper<T> wrapper = new ProductWrapper<>();
    for (int i = 0; i < n; i++) {
      wrapper = generator.generate(wrapper);
      assertNotNull("Generator has gone unavailable before creating the required number of products, " +
              "required " + n + " but was " + i,
          wrapper);
      T product = wrapper.unwrap();
      logProduct(format(product));
      for (Validator<T> validator : validators) {
        assertTrue("'" + format(product) + "' is not valid according to " + validator +
                ", failed after " + i + " generations",
            validator.valid(product));
      }
    }
  }

  private <T> void expectUniqueGenerationsOnce(Generator<T> generator, int n) {
    UniqueValidator<T> validator = new UniqueValidator<>();
    ProductWrapper<T> wrapper = new ProductWrapper<>();
    for (int i = 0; i < n; i++) {
      wrapper = generator.generate(wrapper);
      assertNotNull("Generator has gone unavailable before creating the required number of products ",
          wrapper);
      T product = wrapper.unwrap();
      logProduct(product);
      assertTrue("'" + format(product) + "' is not unique. Generator is " + generator,
          validator.valid(product));
    }
  }

  protected <T extends Comparable<T>> void expectRangeOnce(Generator<T> generator, int n, T min, T max) {
    ProductWrapper<T> wrapper = new ProductWrapper<>();
    for (int i = 0; i < n; i++) {
      wrapper = generator.generate(wrapper);
      assertNotNull(wrapper);
      T product = wrapper.unwrap();
      assertTrue("Generated value (" + product + ") is less than the configured minimum (" + min + ")", min.compareTo(product) <= 0);
      assertTrue("Generated value (" + product + ") is greater than the configured maximum (" + max + ")", max.compareTo(product) >= 0);
    }
  }

  public static void assertComponents(Entity entity, Object... nameValuePairs) {
    for (int i = 0; i < nameValuePairs.length; i += 2) {
      String name = (String) nameValuePairs[i];
      Object expected = nameValuePairs[i + 1];
      Object actual = entity.getComponent(name);
      assertEquals("Unexpected value for component '" + name + "':", expected, actual);
    }
  }

  protected ObjectCounter<String> checkDiversity(NonNullGenerator<String> generator, int genCount, int expMinDiversity) {
    generator.init(context);
    ObjectCounter<String> counter = new ObjectCounter<>(expMinDiversity);
    for (int i = 0; i < genCount; i++) {
      counter.count(generator.generate());
    }
    int diversity = counter.objectSet().size();
    assertTrue("Expected at least " + expMinDiversity + " different products, but had only " + diversity,
        diversity >= expMinDiversity);
    return counter;
  }

  protected static <T> void assertContains(ObjectCounter<T> samples, T... values) {
    for (T value : values) {
      assertTrue(samples.getCount(value) > 0);
    }
  }

}
