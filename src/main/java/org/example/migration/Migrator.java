package org.example.migration;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class Migrator {
    private static String URL = "jdbc:sqlite:/src/main/resources/db/sample.sqlite";

    public void runMigrations() {
        List<MigrationWrapper> migrations = loadMigrationClasses();

        // List should be validated if containing duplicated version identifiers!

        prepareMigrationTable(migrations);

        ResultSet rs = null;
        Connection connection = null;
        try {
            for (MigrationWrapper migration : migrations) {
                connection = getConnection();
                Statement statement = getStatement(connection);
                connection.setAutoCommit(false);

                rs = statement
                        .executeQuery("SELECT * FROM __migrations__ WHERE version = " + migration.getVersion() + "");

                long migrated = rs.getLong("migrated");

                if (migrated != 0) {
                    System.out.println(
                            "Migration V" + migration.getVersion() + " was executed at "
                                    + new Date(migrated).toString());
                    continue;
                }

                System.out.println("Running migration V" + migration.getVersion() + " " + migration.getDescription());

                migration.getInstance().migrate();

                Date date = new Date();

                statement.executeUpdate("UPDATE __migrations__ SET migrated = " + date.getTime() + " WHERE version = "
                        + migration.getVersion() + "");

                connection.commit();

                System.out.println(
                        "Migration V" + migration.getVersion() + " executed at " + date.toString() + " successfully");

                System.out.println("Updated database entry for migration V" + migration.getVersion());
            }
        } catch (Exception e) {
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException e2) {
                System.out.println(e2.getMessage());
            }
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }

        logPersons();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    private Statement getStatement(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();

        statement.setQueryTimeout(30); // set timeout to 30 sec.

        return statement;
    }

    private List<MigrationWrapper> loadMigrationClasses() {
        ClassesLoader loader = new ClassesLoader();

        Set<Class<BaseMigration>> classes = loader.findAllClassesMatchingSuper("db.migration", BaseMigration.class);

        List<MigrationWrapper> orderedList = classes.stream()
                .map(clazz -> {
                    try {
                        Constructor<BaseMigration> ctor = clazz.getConstructor();
                        BaseMigration instance = ctor.newInstance();
                        Integer version = instance.getVersion();
                        String description = instance.getDescription();

                        return new MigrationWrapper(version, description, instance);
                    } catch (Exception e) {
                        // handle the exception
                    }
                    return null;
                })
                .filter(instance -> instance != null)
                .sorted((m1, m2) -> m1.getVersion() - m2.getVersion())
                .toList();

        return orderedList;
    }

    private void prepareMigrationTable(List<MigrationWrapper> migrations) {
        try (Connection connection = getConnection(); Statement statement = getStatement(connection)) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS __migrations__ (version TEXT PRIMARY KEY, description TEXT NOT NULL, migrated INTEGER)");

            System.out.println("Discovered " + migrations.size() + " migration" + (migrations.size() > 1 ? "s" : ""));

            for (MigrationWrapper migration : migrations) {
                statement.executeUpdate(
                        "INSERT OR IGNORE INTO __migrations__ values(" + migration.getVersion().toString()
                                + ", '" + migration.getDescription() + "', NULL)");
            }
        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            e.printStackTrace(System.err);
        }
    }

    private void logPersons() {
        try (Connection connection = getConnection(); Statement statement = getStatement(connection)) {
            ResultSet rs = statement.executeQuery("SELECT * FROM person");

            System.out.println("---");
            while (rs.next()) {
                // read the result set
                System.out.println("name = " + rs.getString("name"));
                System.out.println("id = " + rs.getInt("id"));
                System.out.println("---");
            }
        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            e.printStackTrace(System.err);
        }
    }

    private class MigrationWrapper {
        private Integer version;
        private String description;
        private BaseMigration instance;

        MigrationWrapper(Integer version, String description, BaseMigration instance) {
            this.version = version;
            this.description = description;
            this.instance = instance;
        }

        public Integer getVersion() {
            return version;
        }

        public String getDescription() {
            return description;
        }

        public BaseMigration getInstance() {
            return instance;
        }
    }
}
