package com.brainbucket.database;

import static org.junit.jupiter.api.Assertions.*;

import com.brainbucket.BrainBucketBackendApplication;
import java.nio.file.Files;
import java.sql.*;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.SpringApplication;

/** Opt-in: a disposable MySQL instance whose account can create/drop test databases. */
@EnabledIfEnvironmentVariable(named = "BB_MIGRATION_TEST_URL", matches = ".+")
class MySqlMigrationTests {
  @Test
  void emptyLegacyAndAlreadyUpdatedDatabasesPreserveDataAndBoot() throws Exception {
    String base = System.getenv("BB_MIGRATION_TEST_URL");
    String user = System.getenv().getOrDefault("BB_MIGRATION_TEST_USER", "root");
    String password = System.getenv().getOrDefault("BB_MIGRATION_TEST_PASSWORD", "");
    try (Connection admin = DriverManager.getConnection(base, user, password)) {
      for (String scenario : new String[] {"empty", "legacy", "manual"}) {
        String db = "bb_migration_" + UUID.randomUUID().toString().replace("-", "");
        try (Statement s = admin.createStatement()) { s.execute("CREATE DATABASE `" + db + "`"); }
        String url = base.replaceFirst("/[^/]*(\\?.*)?$", "/" + db);
        try {
          Flyway flyway = Flyway.configure().dataSource(url, user, password)
              .baselineOnMigrate(true).baselineVersion("1").cleanDisabled(true).load();
          if (!scenario.equals("empty")) {
            Flyway.configure().dataSource(url, user, password).target("1").load().migrate();
            try (Connection c = DriverManager.getConnection(url, user, password);
                Statement s = c.createStatement()) {
              s.execute("DROP TABLE flyway_schema_history");
              s.execute("INSERT INTO users (id,email,password_hash,display_name) VALUES (1,'preserve@example.com','hash','Preserve')");
              s.execute("INSERT INTO posts (author_id,title) VALUES (1,'Preserve post')");
              if (scenario.equals("legacy")) {
                for (String col : new String[] {"display_until", "display_from", "version"}) {
                  s.execute("ALTER TABLE posts DROP COLUMN " + col);
                }
                for (String col : new String[] {"snapshot", "poster_key", "evidence_key", "original_key", "poster_type", "evidence_type", "original_type"}) {
                  s.execute("ALTER TABLE approval_logs DROP COLUMN " + col);
                }
              }
            }
          }
          assertTrue(flyway.migrate().success);
          assertEquals(0, flyway.migrate().migrationsExecuted);
          assertEquals("2", flyway.info().current().getVersion().getVersion());
          try (Connection c = DriverManager.getConnection(url, user, password);
              Statement s = c.createStatement()) {
            try (ResultSet rows = s.executeQuery("SELECT display_from, display_until, version FROM posts")) {
              if (!scenario.equals("empty")) {
                assertTrue(rows.next()); assertNull(rows.getDate(1)); assertEquals(0, rows.getLong(3));
              }
            }
            try (ResultSet rows = s.executeQuery("SELECT title FROM posts")) {
              if (!scenario.equals("empty")) { assertTrue(rows.next()); assertEquals("Preserve post", rows.getString(1)); }
            }
            s.executeQuery("SELECT snapshot,poster_key,evidence_key,original_key,poster_type,evidence_type,original_type FROM approval_logs").close();
          }
          // Confirm Boot discovers Java migrations and completes JPA initialization.
          try (var app = SpringApplication.run(BrainBucketBackendApplication.class,
              "--spring.datasource.url=" + url, "--spring.datasource.username=" + user,
              "--spring.datasource.password=" + password, "--server.port=0",
              "--app.upload-dir=" + Files.createTempDirectory("bb-migration-uploads"))) {
            assertTrue(app.isActive());
          }
        } finally {
          try (Statement s = admin.createStatement()) { s.execute("DROP DATABASE `" + db + "`"); }
        }
      }
    }
  }
}
