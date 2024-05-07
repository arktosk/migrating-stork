package db.migration;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import javax.sql.DataSource;

import org.example.migration.BaseMigration;
import org.example.migration.MigrationException;
import org.springframework.beans.factory.annotation.Autowired;

import db.util.SQLTransaction;

import java.sql.PreparedStatement;

public class V3__create_table_with_json extends BaseMigration {

    @Autowired
    DataSource dataSource;

    @Override
    public String getDescription() {
        return "Creates table utilizing JSON field";
    }

    public void migrate() throws MigrationException {
        try {
            SQLTransaction.runTransaction(dataSource, (transaction) -> {
                Statement statement = transaction.createStatement();
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS json_pointer (id INTEGER PRIMARY KEY, data TEXT)");

                PreparedStatement ps = transaction.prepareStatement("INSERT INTO json_pointer (id, data) VALUES(?, ?)");

                for (int i = 1; i <= 100; i++) {
                    ps.setInt(1, i);
                    ps.setObject(2, "{\"test\": {\"timestamp\": " + new Date().getTime() + "}}");

                    ps.executeUpdate();
                }
            });
        } catch (SQLException e) {
            throw new MigrationException(e);
        }
    }
}
