package db.migration;

import java.sql.Connection;
import java.sql.SQLException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Adds only absent columns, including on databases already updated manually. */
public class V2__integrate_post_management extends BaseJavaMigration {
  @Override
  public boolean canExecuteInTransaction() { return false; }

  @Override
  public Integer getChecksum() { return 20261010; }

  @Override
  public void migrate(Context context) throws Exception {
    Connection c = context.getConnection();
    add(c, "posts", "display_until", "DATE NULL");
    add(c, "posts", "display_from", "DATE NULL");
    add(c, "posts", "version", "BIGINT NOT NULL DEFAULT 0");
    add(c, "approval_logs", "snapshot", "LONGTEXT NULL");
    for (String kind : new String[] {"poster", "evidence", "original"}) {
      add(c, "approval_logs", kind + "_key", "VARCHAR(512) NULL");
      add(c, "approval_logs", kind + "_type", "VARCHAR(255) NULL");
    }
  }

  private void add(Connection c, String table, String column, String definition)
      throws SQLException {
    try (var query = c.prepareStatement(
        "SELECT COUNT(*) FROM information_schema.columns "
            + "WHERE table_schema=DATABASE() AND table_name=? AND column_name=?")) {
      query.setString(1, table);
      query.setString(2, column);
      try (var rows = query.executeQuery()) {
        rows.next();
        if (rows.getInt(1) > 0) return;
      }
    }
    // Identifiers/definitions above are fixed source constants, never request input.
    try (var statement = c.createStatement()) {
      statement.executeUpdate(
          "ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition);
    }
  }
}
