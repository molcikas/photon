package com.github.molcikas.photon.datasource;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * A JDBC database connection that ignores commands that change its state, such as commit() or close(). This is useful
 * if Photon is being used alongside another ORM.
 */
@SuppressWarnings("SQL_INJECTION_JDBC")
public class ReadOnlyConnection implements Connection
{
    private final Connection connection;

    public ReadOnlyConnection(Connection connection)
    {
        this.connection = connection;
    }

    @Override
    public Statement createStatement() throws SQLException
    {
        return connection.createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException
    {
        return connection.prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException
    {
        return connection.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException
    {
        return connection.nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException
    {
        // Do nothing.
    }

    @Override
    public boolean getAutoCommit() throws SQLException
    {
        return connection.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException
    {
        // Do nothing.
    }

    @Override
    public void rollback() throws SQLException
    {
        // Do nothing.
    }

    @Override
    public void close() throws SQLException
    {
        // Do nothing.
    }

    @Override
    public boolean isClosed() throws SQLException
    {
        return connection.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException
    {
        return connection.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException
    {
        // Do nothing.
    }

    @Override
    public boolean isReadOnly() throws SQLException
    {
        return connection.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException
    {
        // Do nothing.
    }

    @Override
    public String getCatalog() throws SQLException
    {
        return connection.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException
    {
        // Do nothing.
    }

    @Override
    public int getTransactionIsolation() throws SQLException
    {
        return connection.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException
    {
        return connection.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException
    {
        // Do nothing.
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return connection.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql,
                                              int resultSetType,
                                              int resultSetConcurrency) throws SQLException
    {
        return connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
    {
        return connection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException
    {
        return connection.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException
    {
        // Do nothing.
    }

    @Override
    public void setHoldability(int holdability) throws SQLException
    {
        // Do nothing.
    }

    @Override
    public int getHoldability() throws SQLException
    {
        return connection.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException
    {
        throw new IllegalStateException("Cannot call setSavepoint() on a read only connection.");
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException
    {
        throw new IllegalStateException("Cannot call setSavepoint() on a read only connection.");
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException
    {
        // Do nothing.
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException
    {
        // Do nothing.
    }

    @Override
    public Statement createStatement(int resultSetType,
                                     int resultSetConcurrency,
                                     int resultSetHoldability) throws SQLException
    {
        return connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql,
                                              int resultSetType,
                                              int resultSetConcurrency,
                                              int resultSetHoldability) throws SQLException
    {
        return connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public CallableStatement prepareCall(String sql,
                                         int resultSetType,
                                         int resultSetConcurrency,
                                         int resultSetHoldability) throws SQLException
    {
        return connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
    {
        return connection.prepareStatement(sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
    {
        return connection.prepareStatement(sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
    {
        return connection.prepareStatement(sql, columnNames);
    }

    @Override
    public Clob createClob() throws SQLException
    {
        return connection.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException
    {
        return connection.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException
    {
        return connection.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException
    {
        return connection.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException
    {
        return connection.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException
    {
        // Do nothing.
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException
    {
        // Do nothing.
    }

    @Override
    public String getClientInfo(String name) throws SQLException
    {
        return connection.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException
    {
        return connection.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException
    {
        return connection.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException
    {
        return connection.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException
    {
        // Do nothing.
    }

    @Override
    public String getSchema() throws SQLException
    {
        return connection.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException
    {
        // Do nothing.
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException
    {
        // Do nothing.
    }

    @Override
    public int getNetworkTimeout() throws SQLException
    {
        return connection.getNetworkTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        return connection.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return connection.isWrapperFor(iface);
    }
}
