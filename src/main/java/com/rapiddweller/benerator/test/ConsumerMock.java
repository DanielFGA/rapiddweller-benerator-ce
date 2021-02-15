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

import com.rapiddweller.benerator.Consumer;
import com.rapiddweller.benerator.consumer.AbstractConsumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mock implementation of the {@link Consumer} interface to be used for testing.<br/><br/>
 * Created: 11.03.2010 12:51:40
 *
 * @author Volker Bergmann
 * @since 0.6.0
 */
public class ConsumerMock extends AbstractConsumer {

  /**
   * The constant START_CONSUMING.
   */
  public static final String START_CONSUMING = "sc";
  /**
   * The constant FINISH_CONSUMING.
   */
  public static final String FINISH_CONSUMING = "fc";
  /**
   * The constant FLUSH.
   */
  public static final String FLUSH = "fl";
  /**
   * The constant CLOSE.
   */
  public static final String CLOSE = "cl";

  /**
   * The constant instances.
   */
  public static final Map<Integer, ConsumerMock> instances = new HashMap<>();

  private final int id;
  private final int minDelay;
  private final int delayDelta;

  private final boolean storeProducts;
  /**
   * The Products.
   */
  public List<Object> products;
  /**
   * The Invocations.
   */
  public final List<String> invocations;

  /**
   * The Start consuming count.
   */
  public final AtomicInteger startConsumingCount = new AtomicInteger();
  /**
   * The Finish consuming count.
   */
  public final AtomicInteger finishConsumingCount = new AtomicInteger();
  /**
   * The Flush count.
   */
  public final AtomicInteger flushCount = new AtomicInteger();
  /**
   * The Close count.
   */
  public final AtomicInteger closeCount = new AtomicInteger();

  private Random random;
  private final Set<String> threadNames;

  /**
   * Instantiates a new Consumer mock.
   *
   * @param storeProducts the store products
   */
  public ConsumerMock(boolean storeProducts) {
    this(storeProducts, 0, 0, 0);
  }

  /**
   * Instantiates a new Consumer mock.
   *
   * @param storeProducts the store products
   * @param id            the id
   */
  public ConsumerMock(boolean storeProducts, int id) {
    this(storeProducts, id, 0, 0);
  }

  /**
   * Instantiates a new Consumer mock.
   *
   * @param storeProducts the store products
   * @param id            the id
   * @param minDelay      the min delay
   * @param maxDelay      the max delay
   */
  public ConsumerMock(boolean storeProducts, int id, int minDelay, int maxDelay) {
    this.storeProducts = storeProducts;
    this.id = id;
    this.minDelay = minDelay;
    if (maxDelay > 0) {
      this.delayDelta = maxDelay - minDelay;
      random = new Random();
    } else {
      this.delayDelta = 0;
    }
    if (storeProducts) {
      products = new ArrayList<>();
    }
    this.invocations = new ArrayList<>();
    instances.put(id, this);
    threadNames = new HashSet<>();
  }

  /**
   * Gets products.
   *
   * @return the products
   */
  public List<?> getProducts() {
    return products;
  }

  /**
   * Gets thread count.
   *
   * @return the thread count
   */
  public int getThreadCount() {
    return threadNames.size();
  }

  @Override
  public synchronized void startProductConsumption(Object product) {
    threadNames.add(Thread.currentThread().getName());
    invocations.add(START_CONSUMING);
    startConsumingCount.incrementAndGet();
    if (storeProducts) {
      synchronized (products) {
        products.add(product);
      }
    }
    if (random != null) {
      try {
        Thread.sleep(minDelay + random.nextInt(delayDelta));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public void finishProductConsumption(Object product) {
    invocations.add(FINISH_CONSUMING);
    finishConsumingCount.incrementAndGet();
  }

  @Override
  public void flush() {
    invocations.add(FLUSH);
    flushCount.incrementAndGet();
  }

  @Override
  public void close() {
    invocations.add(CLOSE);
    closeCount.incrementAndGet();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + '[' + id + ']';
  }

}
