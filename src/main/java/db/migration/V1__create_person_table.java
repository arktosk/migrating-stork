package db.migration;

import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.example.migration.BaseMigration;
import org.example.migration.MigrationException;
import org.springframework.beans.factory.annotation.Autowired;

import db.util.SQLTransaction;

public class V1__create_person_table extends BaseMigration {

    @Autowired
    DataSource dataSource;

    @Override
    public String getDescription() {
        return "Creates persons table";
    }

    public void migrate() throws MigrationException {
        try {
            SQLTransaction.runTransaction(dataSource, (transaction) -> {
                Statement statement = transaction.createStatement();
                
                statement.executeUpdate("DROP TABLE IF EXISTS persons");
                statement.executeUpdate("CREATE TABLE persons (id INTEGER PRIMARY KEY, name TEXT NOT NULL, data TEXT)");
            });
        } catch (SQLException e) {
            throw new MigrationException(e);
        }
    }

}
