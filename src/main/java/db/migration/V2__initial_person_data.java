package db.migration;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.example.migration.BaseMigration;
import org.example.migration.MigrationException;
import org.springframework.beans.factory.annotation.Autowired;

import db.util.SQLTransaction;

public class V2__initial_person_data extends BaseMigration {

    @Autowired
    DataSource dataSource;

    private static final String[] PERSONS = { "Leo", "Yui", "May" };

    @Override
    public String getDescription() {
        String names = String.join(", ", PERSONS);
        return "Seeds persons table by " + names;
    }

    public void migrate() throws MigrationException {
        try {
            SQLTransaction.runTransaction(dataSource, (transaction) -> {
                PreparedStatement preparedStatement = transaction
                        .prepareStatement("INSERT INTO persons (id, name) VALUES(?, ?)");

                var counter = new AtomicInteger();
                for (String name : PERSONS) {
                    preparedStatement.setInt(1, counter.incrementAndGet());
                    preparedStatement.setString(2, name);
                    preparedStatement.executeUpdate();
                }
            });
        } catch (SQLException e) {
            throw new MigrationException(e);
        }
    }
}
