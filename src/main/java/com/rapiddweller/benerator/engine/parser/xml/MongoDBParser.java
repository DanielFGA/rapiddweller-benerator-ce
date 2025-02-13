package com.rapiddweller.benerator.engine.parser.xml;

import com.rapiddweller.benerator.BeneratorErrorIds;
import com.rapiddweller.benerator.engine.BeneratorRootStatement;
import com.rapiddweller.benerator.engine.Statement;
import com.rapiddweller.benerator.engine.parser.attr.CommonAttrs;
import com.rapiddweller.benerator.engine.parser.string.IdParser;
import com.rapiddweller.benerator.engine.parser.string.ScriptableParser;
import com.rapiddweller.benerator.engine.statement.MongoDBStatement;
import com.rapiddweller.benerator.engine.statement.IfStatement;
import com.rapiddweller.common.Expression;
import com.rapiddweller.common.exception.ExceptionFactory;
import com.rapiddweller.common.exception.ParseException;
import com.rapiddweller.common.parser.BooleanParser;
import com.rapiddweller.common.parser.PositiveIntegerParser;
import com.rapiddweller.common.parser.StringParser;
import com.rapiddweller.format.xml.AttrInfo;
import com.rapiddweller.format.xml.AttrInfoSupport;
import com.rapiddweller.platform.nosql.CustomStorageSystem;
import com.rapiddweller.script.expression.ExpressionUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Objects;

import static com.rapiddweller.benerator.BeneratorErrorIds.*;
import static com.rapiddweller.benerator.engine.DescriptorConstants.*;
import static com.rapiddweller.benerator.engine.parser.xml.DescriptorParserUtil.*;
import static java.lang.String.format;

public class MongoDBParser extends AbstractBeneratorDescriptorParser {

    private static final AttrInfo<String> ID_ATT_INFO = new AttrInfo<>(
            ATT_ID, true, SYN_SYSTEM_STORAGE_ID, new IdParser(), null);

    private static final AttrInfo<Expression<String>> ENVIRONMENT_ATT_INFO =
            CommonAttrs.environment(BeneratorErrorIds.SYN_DB_ENVIRONMENT, false);

    private static final AttrInfo<Expression<String>> SYSTEM_ATT_INFO =
            CommonAttrs.system(BeneratorErrorIds.SYN_DB_SYSTEM, false);

    private static final AttrInfo<Expression<String>> HOST_ATT_INFO = new AttrInfo<>(
            ATT_HOST, false, SYN_SYSTEM_STORAGE_ATTR,
            new ScriptableParser<>(new StringParser("mongodb host")), null);

    private static final AttrInfo<Expression<Integer>> PORT_ATT_INFO = new AttrInfo<>(
            ATT_PORT, false, SYN_SYSTEM_STORAGE_ATTR,
            new ScriptableParser<>(new PositiveIntegerParser()), null);

    private static final AttrInfo<Expression<String>> DATABASE_ATT_INFO = new AttrInfo<>(
            ATT_DATABASE, false, SYN_SYSTEM_STORAGE_ATTR,
            new ScriptableParser<>(new StringParser("mongodb database name")), null);

    private static final AttrInfo<Expression<String>> USER_ATT_INFO = new AttrInfo<>(
            ATT_USER, false, SYN_SYSTEM_STORAGE_ATTR,
            new ScriptableParser<>(new StringParser("mongodb user")), null);

    private static final AttrInfo<Expression<String>> PASSWORD_ATT_INFO = new AttrInfo<>(
            ATT_PASSWORD, false, SYN_SYSTEM_STORAGE_ATTR,
            new ScriptableParser<>(new StringParser("mongodb password")), null);

    private static final AttrInfo<Expression<Boolean>> CLEAN_ATT_INFO = new AttrInfo<>(
            ATT_CLEAN, false, SYN_SYSTEM_STORAGE_ATTR,
            new ScriptableParser<>(new BooleanParser()), "true");

    private static final AttrInfoSupport ATTR_INFO_SUPPORT =
            new AttrInfoSupport(SYN_SYSTEM_STORAGE_ATTR, ID_ATT_INFO, ENVIRONMENT_ATT_INFO, SYSTEM_ATT_INFO,
                    HOST_ATT_INFO, PORT_ATT_INFO, DATABASE_ATT_INFO, USER_ATT_INFO, PASSWORD_ATT_INFO, CLEAN_ATT_INFO);

    protected MongoDBParser() {
        super(EL_MONGO_DB, ATTR_INFO_SUPPORT, BeneratorRootStatement.class, IfStatement.class);
    }

    @Override
    public Statement doParse(Element element, Element[] parentXmlPath, Statement[] parentComponentPath, BeneratorParseContext context) {
        Expression<String> id = parseScriptableStringAttribute(ATT_ID, element);
        Expression<String> environment = ENVIRONMENT_ATT_INFO.parse(element);
        Expression<String> system = SYSTEM_ATT_INFO.parse(element);
        Expression<String> host = parseScriptableStringAttribute(ATT_HOST, element);
        Expression<Integer> port = parseIntAttribute(ATT_PORT, element);
        Expression<String> database = parseScriptableStringAttribute(ATT_DATABASE, element);
        Expression<String> user = parseScriptableStringAttribute(ATT_USER, element);
        Expression<String> password = parseScriptableStringAttribute(ATT_PASSWORD, element);
        Expression<Boolean> clean = parseBooleanExpressionAttribute(ATT_CLEAN, element);
        return new MongoDBStatement(id, environment, system, host, port, database, user, password, clean,
                context.getResourceManager());
    }

}
