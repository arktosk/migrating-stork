package org.example.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MigrationRunner {
    private static final String MIGRATION_CLASSPATH = "db.migration";

    Logger logger = LoggerFactory.getLogger(MigrationRunner.class);

    MigrationLoader migrationLoader;

    MigrationsRepository migrationsRepository;

    DataSource dataSource;

    private MigrationRunner(MigrationLoader migrationLoader, MigrationsRepository migrationsRepository,
            DataSource dataSource) {
        this.migrationLoader = migrationLoader;
        this.migrationsRepository = migrationsRepository;
        this.dataSource = dataSource;
    }

    public static void runInContext(ApplicationContext applicationContext) {
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        MigrationLoader migrationLoader = new MigrationLoader(applicationContext);
        MigrationsRepository migrationsRepository = new MigrationsRepository();
        MigrationRunner runner = new MigrationRunner(migrationLoader, migrationsRepository, dataSource);

        runner.runMigrations();
    }

    private void runMigrations() {
        List<BaseMigration> migrations = migrationLoader.findAllMigrationsInPackage(MIGRATION_CLASSPATH);
        Map<Integer, BaseMigration> migrationsMap = migrations.stream()
                .collect(Collectors.toMap(m -> m.getVersion(), m -> m));

        logger.info("Loaded {} migrations classes", migrations.size());

        List<MigrationData> listOfNotMigratedMigrations = prepareListOfNotMigratedMigrations(migrations);

        if (listOfNotMigratedMigrations.size() == 0) {
            logger.error("Stopping migration process because there is no new migrations");
            return;
        }

        Optional<List<Integer>> migrationQueue = prepareMigrationQueue(listOfNotMigratedMigrations);

        if (migrationQueue.isEmpty()) {
            logger.error("Cannot prepare migration queue");
            return;
        }

        for (Integer nextMigration : migrationQueue.get()) {
            try {
                processMigration(migrationsMap, nextMigration);
            } catch (MigrationException e) {
                logger.error("Stopping migration process because of error");
                e.printStackTrace();
                break;
            }
        }
    }

    private List<MigrationData> prepareListOfNotMigratedMigrations(List<BaseMigration> migrations) {
        try {
            Connection connection = dataSource.getConnection();
            logger.info("Preparing migrations database table");
            migrationsRepository.createMigrationsTableIfNotExists(connection);

            List<Integer> migrated = migrationsRepository.selectMigratedMigrationVersions(connection);

            List<MigrationData> listOfNotMigratedMigrations = migrations.stream()
                    .filter(m -> !migrated.contains(m.getVersion()))
                    .map(MigrationData::of)
                    .toList();

            int countedMigrations = listOfNotMigratedMigrations.size();

            logger.info("Detected {} new migration{}", countedMigrations, countedMigrations == 1 ? "" : "s");

            connection.close();

            return listOfNotMigratedMigrations;

        } catch (Exception e) {
            return null;
        }
    }

    private Optional<List<Integer>> prepareMigrationQueue(List<MigrationData> notMigratedMigrations) {
        Connection connection = null;
        Statement statement = null;
        PreparedStatement preparedStatement = null;
        List<Integer> migrationQueue = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);

            logger.info("Deleting not migrated migrations in database");
            statement = migrationsRepository.deleteNotMigratedMigrations(connection);
            logger.info("Appending new migrations in database");
            preparedStatement = migrationsRepository.insertMigrations(connection, notMigratedMigrations);
            connection.commit();

            connection.setAutoCommit(true);

            logger.info("Retrieving list of not migrated migrations");
            migrationQueue = migrationsRepository.selectNotMigratedMigrationVersions(connection);
        } catch (Exception e) {
            e.printStackTrace();
            // Trying rollback
            try {
                if (connection != null) {
                    connection.rollback();
                    // Rolback success
                }
            } catch (SQLException e2) {
                // Rolback error
                e2.printStackTrace();
                System.err.println(e2.getMessage());
            }
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println(e.getMessage());
            }
        }
        return migrationQueue == null ? Optional.empty() : Optional.of(migrationQueue);
    }

    private void processMigration(Map<Integer, BaseMigration> migrationMap, Integer nextMigrationVersion)
            throws MigrationException {
        if (!migrationMap.containsKey(nextMigrationVersion)) {
            String msg = String.format("Couldn't find migration V%d reference", nextMigrationVersion);
            logger.error(msg);
            throw new MigrationException(msg);
        }

        BaseMigration nextMigration = migrationMap.get(nextMigrationVersion);

        logger.info("Starting migration V{}: {}", nextMigration.getVersion(),
                nextMigration.getDescription());

        try {
            nextMigration.migrate();

            logger.info("Migrated V{}: {}", nextMigration.getVersion(), nextMigration.getDescription());

            Date date = new Date();

            migrationsRepository.markMigrationAsDone(dataSource.getConnection(), nextMigration.getVersion(), date);
        } catch (SQLException e) {
            e.printStackTrace();
            String msg = String.format("Couldn't mark migration V%d as done", nextMigrationVersion);
            throw new MigrationException(msg);
        }

    }
}
