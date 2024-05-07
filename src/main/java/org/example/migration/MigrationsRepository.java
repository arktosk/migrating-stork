package org.example.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MigrationsRepository {
    private final static String TABLE_NAME = "__migrations__";

    public Statement createMigrationsTableIfNotExists(Connection connection) throws SQLException {
        String sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s (version INTEGER PRIMARY KEY, description TEXT NOT NULL, migrated_at INTEGER)",
                TABLE_NAME);
        Statement statement = connection.createStatement();
        statement.executeUpdate(sql);
        return statement;
    }

    public List<Integer> selectMigratedMigrationVersions(Connection connection) throws SQLException {
        String sql = String.format("SELECT version FROM %s WHERE migrated_at IS NOT NULL", TABLE_NAME);
        Statement statement = connection.createStatement();

        ResultSet rs = statement.executeQuery(sql);
        List<Integer> list = new ArrayList<>();

        while (rs.next()) {
            int version = rs.getInt("version");

            list.add(version);
        }

        return list;
    }

    public Statement deleteNotMigratedMigrations(Connection connection) throws SQLException {
        String sql = String.format("DELETE FROM %s WHERE migrated_at IS NULL", TABLE_NAME);
        Statement statement = connection.createStatement();
        statement.executeUpdate(sql);
        return statement;
    }

    public PreparedStatement insertMigrations(Connection connection, List<MigrationData> migrations)
            throws SQLException {
        String sql = String.format("INSERT INTO %s (version, description, migrated_at) values(?, ?, ?)", TABLE_NAME);
        PreparedStatement ps = connection.prepareStatement(sql);

        for (MigrationData migration : migrations) {
            ps.setInt(1, migration.getVersion());
            ps.setString(2, migration.getDescription());
            ps.setNull(3, Types.INTEGER);
            ps.executeUpdate();
        }

        return ps;
    }

    public List<Integer> selectNotMigratedMigrationVersions(Connection connection) throws SQLException {
        String sql = String.format("SELECT version FROM %s WHERE migrated_at IS NULL", TABLE_NAME);
        Statement statement = connection.createStatement();

        ResultSet rs = statement.executeQuery(sql);
        List<Integer> list = new ArrayList<>();

        while (rs.next()) {
            int version = rs.getInt("version");

            list.add(version);
        }

        return list;
    }

    public PreparedStatement markMigrationAsDone(Connection connection, Integer version, Date migratedAt) throws SQLException {
        String sql = String.format("UPDATE %s SET migrated_at = ? WHERE version = ?", TABLE_NAME);
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setLong(1, migratedAt.getTime());
        ps.setInt(2, version);
        ps.executeUpdate();
        return ps;
    }
}
