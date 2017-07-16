package com.github.molcikas.photon.datasource;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

/**
 * A DataSource that always returns an existing provided connection. This is useful if Photon is being used alongside
 * another ORM. If photon is strictly used for queries and not updates, wrap the connection with ReadOnlyConnection so
 * that Photon does not ever modify the state of the connection.
 */
public class ExistingConnectionDataSource implements DataSource
{
    private Connection connection;

    public ExistingConnectionDataSource()
    {
    }

    public ExistingConnectionDataSource(Connection connection)
    {
        this.connection = connection;
    }

    public Connection getConnection() throws SQLException
    {
        return connection;
    }

    public Connection getConnection(String username, String password) throws SQLException
    {
        return connection;
    }

    public void setConnection()
    {
        this.connection = connection;
    }

    public PrintWriter getLogWriter() throws SQLException
    {
        return DriverManager.getLogWriter();
    }

    public void setLogWriter(PrintWriter printWriter) throws SQLException
    {
        DriverManager.setLogWriter(printWriter);
    }

    public void setLoginTimeout(int i) throws SQLException
    {
        DriverManager.setLoginTimeout(i);
    }

    public int getLoginTimeout() throws SQLException
    {
        return DriverManager.getLoginTimeout();
    }

    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        throw new SQLFeatureNotSupportedException();
    }

    public <T> T unwrap(Class<T> tClass) throws SQLException
    {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean isWrapperFor(Class<?> aClass) throws SQLException
    {
        return false;
    }
}
