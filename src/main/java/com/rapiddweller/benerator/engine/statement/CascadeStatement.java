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

package com.rapiddweller.benerator.engine.statement;

import com.rapiddweller.benerator.composite.GenerationStepSupport;
import com.rapiddweller.benerator.composite.GenerationStep;
import com.rapiddweller.benerator.engine.BeneratorContext;
import com.rapiddweller.benerator.factory.BeneratorExceptionFactory;
import com.rapiddweller.benerator.factory.GenerationStepFactory;
import com.rapiddweller.common.ArrayBuilder;
import com.rapiddweller.common.ArrayFormat;
import com.rapiddweller.common.Context;
import com.rapiddweller.format.DataContainer;
import com.rapiddweller.format.DataIterator;
import com.rapiddweller.jdbacl.identity.IdentityModel;
import com.rapiddweller.jdbacl.identity.IdentityProvider;
import com.rapiddweller.jdbacl.identity.KeyMapper;
import com.rapiddweller.jdbacl.identity.NoIdentity;
import com.rapiddweller.jdbacl.model.DBForeignKeyConstraint;
import com.rapiddweller.jdbacl.model.DBTable;
import com.rapiddweller.jdbacl.model.Database;
import com.rapiddweller.model.data.ComplexTypeDescriptor;
import com.rapiddweller.model.data.Entity;
import com.rapiddweller.model.data.InstanceDescriptor;
import com.rapiddweller.model.data.ReferenceDescriptor;
import com.rapiddweller.model.data.Uniqueness;
import com.rapiddweller.platform.db.AbstractDBSystem;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.List;

import static java.io.StreamTokenizer.TT_WORD;

/**
 * Cascades the 'transcode' operation to all entities configured to be related
 * to the currently transcoded entity.<br/><br/>
 * Created: 18.04.2011 07:14:34
 * @author Volker Bergmann
 * @since 0.6.6
 */
public class CascadeStatement extends SequentialStatement implements CascadeParent {

  private static final Logger logger = LoggerFactory.getLogger(CascadeStatement.class);

  private static final String REF_SYNTAX_MESSAGE = "Expected Syntax: table(column1, column2, ...)";

  private final CascadeParent parent;
  private final Reference ref;
  private Entity currentEntity;
  final MutatingTypeExpression typeExpression;
  ComplexTypeDescriptor type;

  public CascadeStatement(String ref, MutatingTypeExpression typeExpression, CascadeParent parent) {
    this.typeExpression = typeExpression;
    this.ref = Reference.parse(ref);
    this.parent = parent;
    this.currentEntity = null;
  }

  @Override
  public boolean execute(BeneratorContext context) {
    AbstractDBSystem source = getSource(context);
    getType(source, context);
    IdentityModel identity = parent.getIdentityProvider().getIdentity(type.getName(), false);
    String tableName = type.getName();
    if (logger.isDebugEnabled()) {
      logger.debug("Cascading transcode from {} to {}", parent.currentEntity().type(), tableName);
    }

    // iterate rows
    List<GenerationStep<Entity>> generationSteps =
        GenerationStepFactory.createMutatingGenerationSteps(type, true, Uniqueness.NONE, context);
    try (GenerationStepSupport<Entity> support = new GenerationStepSupport<>(tableName, generationSteps)) {
      support.init(context);
      try (DataIterator<Entity> iterator = ref.resolveReferences(parent.currentEntity(), source, context)) {
        DataContainer<Entity> container = new DataContainer<>();
        while ((container = iterator.next(container)) != null) {
          mutateAndTranscodeEntity(container.getData(), identity, support, context);
        }
      }
      return true;
    }
  }

  @Override
  public AbstractDBSystem getSource(BeneratorContext context) {
    return parent.getSource(context);
  }

  @Override
  public Entity currentEntity() {
    return currentEntity;
  }

  @Override
  public KeyMapper getKeyMapper() {
    return parent.getKeyMapper();
  }

  @Override
  public IdentityProvider getIdentityProvider() {
    return parent.getIdentityProvider();
  }

  @Override
  public boolean needsNkMapping(String type) {
    return parent.needsNkMapping(type);
  }

  @Override
  public AbstractDBSystem getTarget(BeneratorContext context) {
    return parent.getTarget(context);
  }

  @Override
  public ComplexTypeDescriptor getType(AbstractDBSystem db, BeneratorContext context) {
    if (type == null) {
      String parentType = parent.getType(db, context).getName();
      typeExpression.setTypeName(ref.getTargetTableName(parentType, db));
      type = typeExpression.evaluate(context);
    }
    return type;
  }

  // implementation --------------------------------------------------------------------------------------------------

  private void mutateAndTranscodeEntity(
      Entity sourceEntity, IdentityModel identity, GenerationStepSupport<Entity> support, BeneratorContext context) {
    Object sourcePK = sourceEntity.idComponentValues();
    boolean mapNk = parent.needsNkMapping(sourceEntity.type());
    String nk = null;
    KeyMapper mapper = getKeyMapper();
    AbstractDBSystem source = getSource(context);
    if (mapNk) {
      nk = mapper.getNaturalKey(source.getId(), identity, sourcePK);
    }
    Entity targetEntity = new Entity(sourceEntity);
    support.apply(targetEntity, context);
    Object targetPK = targetEntity.idComponentValues();
    transcodeForeignKeys(targetEntity, source, context);
    mapper.store(source.getId(), identity, nk, sourcePK, targetPK);
    getTarget(context).store(targetEntity);
    logger.debug("transcoded {} to {}", sourceEntity, targetEntity);
    cascade(sourceEntity, context);
  }

  private void transcodeForeignKeys(Entity entity, AbstractDBSystem source, Context context) {
    ComplexTypeDescriptor tableDescriptor = entity.descriptor();
    for (InstanceDescriptor component : tableDescriptor.getParts()) {
      if (component instanceof ReferenceDescriptor) {
        ReferenceDescriptor fk = (ReferenceDescriptor) component;
        String refereeTableName = fk.getTargetType();
        Object sourceRef = entity.get(fk.getName());
        if (sourceRef != null) {
          IdentityProvider identityProvider = parent.getIdentityProvider();
          IdentityModel sourceIdentity = identityProvider.getIdentity(refereeTableName, false);
          if (sourceIdentity == null) {
            DBTable refereeTable = source.getDbMetaData().getTable(refereeTableName);
            sourceIdentity = new NoIdentity(refereeTable.getName());
            identityProvider.registerIdentity(sourceIdentity, refereeTableName);
          }

          boolean needsNkMapping = parent.needsNkMapping(refereeTableName);
          if (sourceIdentity instanceof NoIdentity && needsNkMapping) {
            throw BeneratorExceptionFactory.getInstance().configurationError("No identity defined for table " + refereeTableName);
          }
          KeyMapper mapper = parent.getKeyMapper();
          Object targetRef;
          if (needsNkMapping) {
            String sourceRefNK = mapper.getNaturalKey(source.getId(), sourceIdentity, sourceRef);
            targetRef = mapper.getTargetPK(sourceIdentity, sourceRefNK);
          } else {
            targetRef = mapper.getTargetPK(source.getId(), sourceIdentity, sourceRef);
          }
          if (targetRef == null) {
            String message = "No mapping found for " + source.getId() + '.' + refereeTableName + "#" + sourceRef +
                " referred in " + entity.type() + "(" + fk.getName() + "). " +
                "Probably has not been in the result set of the former '" + refereeTableName + "' nk query.";
            getErrorHandler(context).handleError(message);
          }
          entity.setComponent(fk.getName(), targetRef);
        }
      }
    }
  }

  private void cascade(Entity sourceEntity, BeneratorContext context) {
    this.currentEntity = sourceEntity;
    executeSubStatements(context);
    this.currentEntity = null;
  }

  public static class Reference {

    private final String refererTableName;
    private final String[] columnNames;

    private DBForeignKeyConstraint fk;
    private Database database;
    private DBTable refererTable;
    private DBTable refereeTable;
    private DBTable targetTable;

    public Reference(String refererTableName, String[] columnNames) {
      this.refererTableName = refererTableName;
      this.columnNames = columnNames;
    }

    public String getTargetTableName(String parentTable, AbstractDBSystem db) {
      if (!parentTable.equals(refererTableName)) {
        return refererTableName;
      } else {
        initIfNecessary(parentTable, db);
        return targetTable.getName();
      }
    }

    public DataIterator<Entity> resolveReferences(Entity currentEntity, AbstractDBSystem db, BeneratorContext context) {
      initIfNecessary(currentEntity.type(), db);
      DBTable parentTable = database.getTable(currentEntity.type());
      if (parentTable.equals(refereeTable)) {
        return resolveToManyReference(currentEntity, fk, db, context); // including self-recursion
      } else if (parentTable.equals(refererTable)) {
        return resolveToOneReference(currentEntity, fk, db, context);
      } else {
        throw BeneratorExceptionFactory.getInstance().configurationError("Table '" + parentTable + "' does not relate to the foreign key " +
            refererTableName + '(' + ArrayFormat.format(columnNames) + ')');
      }
    }

    private void initIfNecessary(String parentTable, AbstractDBSystem db) {
      if (this.database != null) {
        return;
      }
      this.database = db.getDbMetaData();
      this.refererTable = this.database.getTable(refererTableName);
      this.fk = refererTable.getForeignKeyConstraint(columnNames);
      this.refereeTable = fk.getRefereeTable();
      this.targetTable = (parentTable.equalsIgnoreCase(refereeTable.getName()) ? refererTable : refereeTable);
    }

    DataIterator<Entity> resolveToManyReference(
        Entity fromEntity, DBForeignKeyConstraint fk, AbstractDBSystem db, BeneratorContext context) {
      StringBuilder selector = new StringBuilder();
      String[] refererColumnNames = fk.getColumnNames();
      String[] refereeColumnNames = fk.getRefereeColumnNames();
      for (int i = 0; i < refererColumnNames.length; i++) {
        if (selector.length() > 0) {
          selector.append(" and ");
        }
        Object refereeColumnValue = fromEntity.get(refereeColumnNames[i]);
        selector.append(refererColumnNames[i]).append('=').append(db.getDialect().formatValue(refereeColumnValue));
      }
      return db.queryEntities(fk.getTable().getName(), selector.toString(), context).iterator();
    }

    DataIterator<Entity> resolveToOneReference(
        Entity fromEntity, DBForeignKeyConstraint fk, AbstractDBSystem db, BeneratorContext context) {
      StringBuilder selector = new StringBuilder();
      String[] refererColumnNames = fk.getColumnNames();
      String[] refereeColumnNames = fk.getRefereeColumnNames();
      for (int i = 0; i < refererColumnNames.length; i++) {
        if (selector.length() > 0) {
          selector.append(" and ");
        }
        Object refererColumnValue = fromEntity.get(refererColumnNames[i]);
        selector.append(refereeColumnNames[i]).append('=').append(db.getDialect().formatValue(refererColumnValue));
      }
      return db.queryEntities(fk.getRefereeTable().getName(), selector.toString(), context).iterator();
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    static Reference parse(String refSpec) {
      StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(refSpec));
      tokenizer.wordChars('_', '_');
      try {
        // parse table name
        int token = tokenizer.nextToken();
        if (token != TT_WORD) {
          throw BeneratorExceptionFactory.getInstance().syntaxErrorForText(refSpec, REF_SYNTAX_MESSAGE);
        }
        // this must be at this position!
        String tableName = tokenizer.sval;

        // parse column names
        if ((tokenizer.nextToken()) != '(') {
          throw BeneratorExceptionFactory.getInstance().syntaxErrorForText(refSpec, REF_SYNTAX_MESSAGE);
        }
        ArrayBuilder<String> columnNames = new ArrayBuilder<>(String.class);
        do {
          if ((tokenizer.nextToken()) != TT_WORD) {
            throw BeneratorExceptionFactory.getInstance().syntaxErrorForText(refSpec, REF_SYNTAX_MESSAGE);
          }
          columnNames.add(tokenizer.sval);
          token = tokenizer.nextToken();
          if (token != ',' && token != ')') {
            throw BeneratorExceptionFactory.getInstance().syntaxErrorForText(refSpec, REF_SYNTAX_MESSAGE);
          }
        } while (token == ',');
        if (token != ')') {
          throw BeneratorExceptionFactory.getInstance().syntaxErrorForText(refSpec, "reference definition must end with ')'");
        }
        return new Reference(tableName, columnNames.toArray());
      } catch (IOException e) {
        throw BeneratorExceptionFactory.getInstance().syntaxErrorForText(refSpec, REF_SYNTAX_MESSAGE);
      }
    }

  }

}
