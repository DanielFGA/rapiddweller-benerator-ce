/* (c) Copyright 2021 by Volker Bergmann. All rights reserved. */

package com.rapiddweller.benerator.engine.parser.xml;

import com.rapiddweller.benerator.BeneratorErrorIds;
import com.rapiddweller.benerator.engine.parser.attr.ErrorHandlerAttribute;
import com.rapiddweller.benerator.engine.parser.attr.NameAttribute;
import com.rapiddweller.benerator.engine.parser.attr.PageSizeAttribute;
import com.rapiddweller.benerator.engine.parser.string.IdParser;
import com.rapiddweller.benerator.engine.parser.string.ScriptableParser;
import com.rapiddweller.common.parser.BooleanParser;
import com.rapiddweller.common.parser.DoubleParser;
import com.rapiddweller.common.parser.NonNegativeLongParser;
import com.rapiddweller.format.xml.AttrInfo;
import com.rapiddweller.format.xml.AttrInfoSupport;
import com.rapiddweller.common.Expression;

import static com.rapiddweller.benerator.BeneratorErrorIds.SYN_GENERATE_NAME;
import static com.rapiddweller.benerator.BeneratorErrorIds.SYN_GENERATE_ON_ERROR;
import static com.rapiddweller.benerator.BeneratorErrorIds.SYN_GENERATE_PAGE_SIZE;
import static com.rapiddweller.benerator.engine.DescriptorConstants.*;

/**
 * Parses a &lt;generate&gt; element in a Benerator XML file.<br/><br/>
 * Created: 29.11.2021 15:41:49
 * @author Volker Bergmann
 * @since 2.1.0
 */
public class GenerateParser extends AbstractGenIterParser {

  private static final AttrInfo<String> NAME = new NameAttribute(SYN_GENERATE_NAME, false, false);

  private static final AttrInfo<Expression<Long>> COUNT = new AttrInfo<>(
      ATT_COUNT, false, BeneratorErrorIds.SYN_GENERATE_COUNT, new ScriptableParser<>(new NonNegativeLongParser()), null
  );

  private static final AttrInfo<Expression<Long>> MIN_COUNT = new AttrInfo<>(
      ATT_MIN_COUNT, false, BeneratorErrorIds.SYN_GENERATE_MIN_COUNT, new ScriptableParser<>(new NonNegativeLongParser()), null);

  private static final AttrInfo<Expression<Long>> MAX_COUNT = new AttrInfo<>(
      ATT_MAX_COUNT, false, BeneratorErrorIds.SYN_GENERATE_MAX_COUNT, new ScriptableParser<>(new NonNegativeLongParser()), null);

  private static final AttrInfo<String> COUNT_DISTRIBUTION = new AttrInfo<>(
      ATT_COUNT_DISTRIBUTION, false, BeneratorErrorIds.SYN_GENERATE_COUNT_DISTRIBUTION, null, null); // TODO

  private static final PageSizeAttribute PAGESIZE = new PageSizeAttribute(SYN_GENERATE_PAGE_SIZE);

  private static final ErrorHandlerAttribute ON_ERROR = new ErrorHandlerAttribute(SYN_GENERATE_ON_ERROR);

  private static final AttrInfo<String> TEMPLATE = new AttrInfo<>(
      ATT_TEMPLATE, false, BeneratorErrorIds.SYN_GENERATE_TEMPLATE, null, null);

  private static final AttrInfo<String> CONSUMER = new AttrInfo<>(
      ATT_CONSUMER, false, BeneratorErrorIds.SYN_GENERATE_CONSUMER, null, null);

  private static final AttrInfo<String> TYPE = new AttrInfo<>(
      ATT_TYPE, false, BeneratorErrorIds.SYN_GENERATE_TYPE, new IdParser(), null);

  private static final AttrInfo<String> SCOPE = new AttrInfo<>(
      ATT_SCOPE, false, BeneratorErrorIds.SYN_GENERATE_SCOPE, null, null);

  private static final AttrInfo<String> GENERATOR = new AttrInfo<>(
      ATT_GENERATOR, false, BeneratorErrorIds.SYN_GENERATE_GENERATOR, null, null);

  private static final AttrInfo<String> VALIDATOR = new AttrInfo<>(
      ATT_VALIDATOR, false, BeneratorErrorIds.SYN_GENERATE_VALIDATOR, null, null);

  private static final AttrInfo<String> CONVERTER = new AttrInfo<>(
      ATT_CONVERTER, false, BeneratorErrorIds.SYN_GENERATE_CONVERTER, null, null);

  private static final AttrInfo<Expression<Double>> NULL_QUOTA = new AttrInfo<>(
      ATT_NULL_QUOTA, false, BeneratorErrorIds.SYN_GENERATE_NULL_QUOTA,
      new ScriptableParser<>(new DoubleParser()), null);

  private static final AttrInfo<Boolean> UNIQUE = new AttrInfo<>(
      ATT_UNIQUE, false, BeneratorErrorIds.SYN_GENERATE_UNIQUE, new BooleanParser(), "false");

  private static final AttrInfo<String> DISTRIBUTION = new AttrInfo<>(
      ATT_DISTRIBUTION, false, BeneratorErrorIds.SYN_GENERATE_DISTRIBUTION, null, null);

  private static final AttrInfo<Boolean> CYCLIC = new AttrInfo<>(
      ATT_CYCLIC, false, BeneratorErrorIds.SYN_GENERATE_CYCLIC, new BooleanParser(), "false");

  private static final AttrInfo<Long> OFFSET = new AttrInfo<>(
      ATT_OFFSET, false, BeneratorErrorIds.SYN_GENERATE_OFFSET, new NonNegativeLongParser(), "0");

  private static final AttrInfoSupport ATTR_INFO = new AttrInfoSupport(BeneratorErrorIds.SYN_GENERATE_ILLEGAL_ATTR,
        COUNT, MIN_COUNT, MAX_COUNT, COUNT_DISTRIBUTION, THREADS, PAGESIZE, STATS, ON_ERROR, TEMPLATE,
        CONSUMER, NAME, TYPE, SCOPE, GENERATOR, VALIDATOR, CONVERTER, NULL_QUOTA, UNIQUE, DISTRIBUTION,
        CYCLIC, OFFSET, SENSOR);

  public GenerateParser() {
    super(EL_GENERATE, ATTR_INFO);
  }

}
