package db.migration;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import javax.sql.DataSource;

import java.sql.ResultSet;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import db.util.SQLTransaction;

import org.example.migration.BaseMigration;
import org.example.migration.MigrationException;
import org.springframework.beans.factory.annotation.Autowired;

public class V4__migrate_json_data extends BaseMigration {

    @Autowired
    DataSource dataSource;

    @Override
    public String getDescription() {
        return "Migrate JSON field";
    }

    public void migrate() throws MigrationException {

        try {
            SQLTransaction.runTransaction(dataSource, (transaction) -> {
                Statement statement = transaction.createStatement();
                ResultSet rs = statement.executeQuery("SELECT * FROM json_pointer");
                PreparedStatement ps = transaction.prepareStatement("UPDATE json_pointer SET data = ? WHERE id = ?");

                ObjectMapper mapper = new ObjectMapper();

                while (rs.next()) {
                    String data = rs.getString("data");
                    var maybeNode = parseNode(mapper, data);
                    if (maybeNode.isEmpty()) {
                        continue;
                    }
                    JsonNode node = maybeNode.get();

                    if (node.isObject()) {
                        ObjectNode objectNode = (ObjectNode) node;
                        if (objectNode.has("test") && objectNode.get("test").isObject()) {
                            ObjectNode testNode = (ObjectNode) objectNode.get("test");

                            ObjectNode innerNode = mapper.createObjectNode();
                            innerNode.put("r1", 1);
                            innerNode.put("r2", "two");
                            innerNode.put("r3", false);

                            testNode.set("inner", innerNode);
                        }
                    }

                    ps.setString(1, node.toString());
                    ps.setInt(2, rs.getInt("id"));

                    ps.executeUpdate();
                }
            });
        } catch (SQLException e) {
            throw new MigrationException(e);
        }
    }

    private Optional<JsonNode> parseNode(ObjectMapper mapper, String content) {
        JsonFactory factory = mapper.getFactory();

        try {
            JsonParser parser = factory.createParser(content);
            JsonNode node = mapper.readTree(parser);

            return Optional.of(node);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
