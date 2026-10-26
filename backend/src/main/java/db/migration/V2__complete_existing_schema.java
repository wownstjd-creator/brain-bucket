package db.migration;

import java.sql.Connection;
import java.sql.SQLException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Additive upgrade for the project's pre-Flyway MySQL databases. */
public class V2__complete_existing_schema extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    // Check the expected legacy schema before making any changes.
    for (String table : new String[]{"users", "posts", "post_files", "approval_logs", "bookmarks"}) {
      try (var statement = connection.prepareStatement(
          "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?")) {
        statement.setString(1, table);
        try (var rows = statement.executeQuery()) {
          rows.next();
          if (rows.getInt(1) != 1) throw new SQLException(
              "Expected BrainBucket table is missing: " + table + ". Check DATABASE_URL and restore the complete legacy schema.");
        }
      }
    }
    add(connection, "posts", "display_until", "DATE NULL");
    add(connection, "posts", "display_from", "DATE NULL");
    add(connection, "posts", "version", "BIGINT NOT NULL DEFAULT 0");
    add(connection, "approval_logs", "snapshot", "LONGTEXT NULL");
    for (String kind : new String[]{"poster", "evidence", "original"}) {
      add(connection, "approval_logs", kind + "_key", "VARCHAR(512) NULL");
      add(connection, "approval_logs", kind + "_type", "VARCHAR(255) NULL");
    }
  }

  private void add(Connection connection, String table, String column, String definition)
      throws SQLException {
    try (var statement = connection.prepareStatement(
        "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=? AND column_name=?")) {
      statement.setString(1, table);
      statement.setString(2, column);
      try (var rows = statement.executeQuery()) {
        rows.next();
        if (rows.getInt(1) > 0) return;
      }
    }
    // Identifiers and definitions above are code constants, never request input.
    try (var statement = connection.createStatement()) {
      statement.executeUpdate("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition);
    }
  }
}
