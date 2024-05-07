package db.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.sql.DataSource;

public class SQLTransaction {

    private final int QUERY_TIMEOUT = 30;

    private final Connection connection;

    private final List<Statement> statements = new ArrayList<>();

    public static void runTransaction(DataSource dataSource, SQLTransactionFunction callable) throws SQLException {
        SQLTransaction transaction = null;
        SQLException exception = null;

        try {
            transaction = SQLTransaction.of(dataSource);

            callable.apply(transaction);

            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            try {
                if (transaction != null) {
                    transaction.rollback();
                    exception = new SQLException("Transaction failed, but rollback succeed", e);
                }
                exception = new SQLException("The transaction cannot be established", e);
            } catch (SQLException e2) {
                e2.printStackTrace();
                System.err.println(e2.getMessage());
                exception = new SQLException("Transaction failed and rollback failed", e2);
            }
        } finally {
            try {
                if (transaction != null) {
                    transaction.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println(e.getMessage());
                exception = new SQLException("Transaction was unable to close connection", e);
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    private static SQLTransaction of(DataSource dataSource) throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        return new SQLTransaction(connection);
    }

    private SQLTransaction(Connection connection) {
        this.connection = connection;
    }

    public Statement createStatement() throws SQLException {
        Statement statement = connection.createStatement();
        statement.setQueryTimeout(QUERY_TIMEOUT);
        statements.add(statement);
        return statement;
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setQueryTimeout(QUERY_TIMEOUT);
        statements.add(statement);
        return statement;
    }

    public void commit() throws SQLException {
        connection.commit();
    }

    public void close() throws SQLException {
        ListIterator<Statement> listIterator = statements.listIterator(statements.size());
        while (listIterator.hasPrevious()) {
            listIterator.previous().close();
        }
        if (connection != null) {
            connection.close();
        }
    }

    public void rollback() throws SQLException {
        connection.rollback();
    }

    @FunctionalInterface
    public interface SQLTransactionFunction {
        void apply(SQLTransaction t) throws SQLException;
    }
}
