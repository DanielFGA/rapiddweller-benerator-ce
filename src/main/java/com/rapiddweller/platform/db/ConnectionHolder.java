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

package com.rapiddweller.platform.db;

import com.rapiddweller.benerator.factory.BeneratorExceptionFactory;
import com.rapiddweller.common.LogCategoriesConstants;
import com.rapiddweller.common.OrderedMap;
import com.rapiddweller.jdbacl.ColumnInfo;
import com.rapiddweller.jdbacl.DBUtil;
import com.rapiddweller.jdbacl.model.DBTable;
import com.rapiddweller.model.data.ComplexTypeDescriptor;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Wraps a database connection and provides access functionality.<br/><br/>
 * Created: 07.01.2013 08:28:36
 * @author Volker Bergmann
 * @since 0.8.0
 */
public class ConnectionHolder implements Closeable {

  private static final Logger jdbcLogger = LoggerFactory.getLogger(LogCategoriesConstants.JDBC);

  public final Map<ComplexTypeDescriptor, PreparedStatement> insertStatements;
  public final Map<ComplexTypeDescriptor, PreparedStatement> updateStatements;
  public final Map<ComplexTypeDescriptor, PreparedStatement> selectByPKStatements;
  private final AbstractDBSystem db;
  private Connection connection;

  public ConnectionHolder(AbstractDBSystem db) {
    this.insertStatements = new OrderedMap<>();
    this.updateStatements = new OrderedMap<>();
    this.selectByPKStatements = new OrderedMap<>();
    this.db = db;
    this.connection = null; // lazily initialized
  }

  public Connection getConnection() {
    if (connection == null) {
      this.connection = db.createConnection();
    }
    return connection;
  }

  public void commit() {
    try {
      flushStatements(insertStatements);
      flushStatements(updateStatements);
      jdbcLogger.debug("Committing connection: {}", connection);
      getConnection().commit();
    } catch (SQLException e) {
      throw BeneratorExceptionFactory.getInstance().operationFailed("Commit failed", e);
    }
  }

  private void flushStatements(Map<ComplexTypeDescriptor, PreparedStatement> statements) throws SQLException {
    for (Map.Entry<ComplexTypeDescriptor, PreparedStatement> entry : statements.entrySet()) {
      PreparedStatement statement = entry.getValue();
      if (statement != null) {
        // need to finish old statement
        if (db.isBatch()) {
          statement.executeBatch();
        }
        jdbcLogger.debug("Closing statement: {}", statement);
        DBUtil.close(statement);
      }
      entry.setValue(null);
    }
  }

  public PreparedStatement getSelectByPKStatement(ComplexTypeDescriptor descriptor) {
    try {
      PreparedStatement statement = selectByPKStatements.get(descriptor);
      if (statement == null) {
        statement = createSelectByPKStatement(descriptor);
      } else {
        statement.clearParameters();
      }
      return statement;
    } catch (SQLException e) {
      throw BeneratorExceptionFactory.getInstance().operationFailed("Error creating statement", e);
    }
  }

  private PreparedStatement createSelectByPKStatement(ComplexTypeDescriptor descriptor) throws SQLException {
    PreparedStatement statement;
    String tableName = descriptor.getName();
    DBTable table = db.getTable(tableName.toUpperCase());
    if (table == null) {
      throw BeneratorExceptionFactory.getInstance().illegalArgument("Table not found: " + tableName);
    }
    StringBuilder builder = new StringBuilder("select * from ").append(tableName).append(" where");
    for (String idColumnName : descriptor.getIdComponentNames()) {
      builder.append(' ').append(idColumnName).append("=?");
    }
    statement = DBUtil.prepareStatement(getConnection(), builder.toString(), db.isReadOnly());
    selectByPKStatements.put(descriptor, statement);
    return statement;
  }

  public PreparedStatement getStatement(
      ComplexTypeDescriptor descriptor, boolean insert, List<ColumnInfo> columnInfos) {
    try {
      PreparedStatement statement =
          (insert ? insertStatements.get(descriptor) : updateStatements.get(descriptor));
      if (statement == null) {
        statement = createStatement(descriptor, insert, columnInfos);
      } else {
        statement.clearParameters();
      }
      return statement;
    } catch (SQLException e) {
      throw BeneratorExceptionFactory.getInstance().operationFailed("Failed to create statement", e);
    }
  }

  private PreparedStatement createStatement(
      ComplexTypeDescriptor descriptor, boolean insert, List<ColumnInfo> columnInfos) throws SQLException {
    PreparedStatement statement;
    String tableName = descriptor.getName();
    DBTable table = db.getTable(tableName.toUpperCase());
    if (table == null) {
      throw BeneratorExceptionFactory.getInstance().illegalArgument("Table not found: " + tableName);
    }
    String sql = (insert ?
        db.getDialect().insert(table, columnInfos) :
        db.getDialect().update(table,
            db.getTable(tableName).getPKColumnNames(),
            columnInfos));
    jdbcLogger.debug("Creating prepared statement: {}", sql);
    statement = DBUtil.prepareStatement(getConnection(), sql, db.isReadOnly());
    if (insert) {
      insertStatements.put(descriptor, statement);
    } else {
      updateStatements.put(descriptor, statement);
    }
    return statement;
  }

  @Override
  public void close() {
    commit();
    DBUtil.close(connection);
  }

}
