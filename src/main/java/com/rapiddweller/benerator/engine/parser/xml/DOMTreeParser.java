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

package com.rapiddweller.benerator.engine.parser.xml;

import com.rapiddweller.benerator.BeneratorErrorIds;
import com.rapiddweller.benerator.engine.BeneratorRootStatement;
import com.rapiddweller.benerator.engine.Statement;
import com.rapiddweller.benerator.engine.statement.DefineDOMTreeStatement;
import com.rapiddweller.benerator.engine.statement.IfStatement;
import com.rapiddweller.benerator.factory.BeneratorExceptionFactory;
import com.rapiddweller.common.ConversionException;
import com.rapiddweller.format.xml.AttrInfoSupport;
import com.rapiddweller.common.Expression;
import org.w3c.dom.Element;

import static com.rapiddweller.benerator.engine.DescriptorConstants.ATT_ID;
import static com.rapiddweller.benerator.engine.DescriptorConstants.ATT_INPUT_URI;
import static com.rapiddweller.benerator.engine.DescriptorConstants.ATT_NAMESPACE_AWARE;
import static com.rapiddweller.benerator.engine.DescriptorConstants.ATT_OUTPUT_URI;
import static com.rapiddweller.benerator.engine.DescriptorConstants.EL_DOMTREE;
import static com.rapiddweller.benerator.engine.parser.xml.DescriptorParserUtil.getConstantStringAttributeAsExpression;
import static com.rapiddweller.benerator.engine.parser.xml.DescriptorParserUtil.parseBooleanExpressionAttribute;
import static com.rapiddweller.benerator.engine.parser.xml.DescriptorParserUtil.parseScriptableStringAttribute;

/**
 * Parses &lt;domtree&gt; elements in a Benerator descriptor file.<br/><br/>
 * Created: 16.01.2014 15:59:48
 * @author Volker Bergmann
 * @since 0.9.0
 */
public class DOMTreeParser extends AbstractBeneratorDescriptorParser {

  private static final AttrInfoSupport ATTR_INFO;
  static {
    ATTR_INFO = new AttrInfoSupport(BeneratorErrorIds.SYN_BEAN_ILLEGAL_ATTR);
    ATTR_INFO.add(ATT_ID, true, BeneratorErrorIds.SYN_BEAN_ID);
    ATTR_INFO.add(ATT_INPUT_URI, true, BeneratorErrorIds.SYN_BEAN_ID);
    ATTR_INFO.add(ATT_OUTPUT_URI, false, BeneratorErrorIds.SYN_BEAN_CLASS);
    ATTR_INFO.add(ATT_NAMESPACE_AWARE, false, BeneratorErrorIds.SYN_BEAN_SPEC);
  }

  public DOMTreeParser() {
    super(EL_DOMTREE, ATTR_INFO, BeneratorRootStatement.class, IfStatement.class);
  }

  @Override
  public DefineDOMTreeStatement doParse(
      Element element, Element[] parentXmlPath, Statement[] parentComponentPath, BeneratorParseContext context) {
    try {
      Expression<String> id = DescriptorParserUtil.getConstantStringAttributeAsExpression(ATT_ID, element);
      Expression<String> inputUri = parseScriptableStringAttribute(ATT_INPUT_URI, element);
      Expression<String> outputUri = parseScriptableStringAttribute(ATT_OUTPUT_URI, element);
      Expression<Boolean> namespaceAware = parseBooleanExpressionAttribute(ATT_NAMESPACE_AWARE, element);
      return new DefineDOMTreeStatement(id, inputUri, outputUri, namespaceAware, context.getResourceManager());
    } catch (ConversionException e) {
      throw BeneratorExceptionFactory.getInstance().configurationError("Error parsing element", e);
    }
  }

}
