package com.brainbucket;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import javax.sql.DataSource;

/** Opt in only against a disposable MySQL server with CREATE DATABASE privileges. */
@EnabledIfEnvironmentVariable(named="BB_MIGRATION_TEST_URL", matches=".+")
class MySqlMigrationTests {
  @Test
  void emptyLegacyAndManuallyUpdatedDatabasesPreserveDataAndRestart() throws Exception {
    String base = System.getenv("BB_MIGRATION_TEST_URL");
    String user = System.getenv().getOrDefault("BB_MIGRATION_TEST_USER", "root");
    String password = System.getenv().getOrDefault("BB_MIGRATION_TEST_PASSWORD", "");
    // A server-only URL prevents accidentally targeting the application's database.
    if (!base.matches("jdbc:mysql://[^/?]+/?")) {
      throw new IllegalArgumentException("BB_MIGRATION_TEST_URL must be a server-only URL, e.g. jdbc:mysql://127.0.0.1:13316/");
    }
    if (!base.endsWith("/")) base += "/";
    try (Connection server = DriverManager.getConnection(base, user, password)) {
      for (String scenario : new String[]{"empty", "legacy", "manual"}) {
        String database = "bb_migration_" + UUID.randomUUID().toString().replace("-", "");
        execute(server, "CREATE DATABASE `" + database + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        try {
          String url = base + database;
          if (!scenario.equals("empty")) {
            try (Connection connection = DriverManager.getConnection(url, user, password)) {
              String schema;
              try (var stream = getClass().getResourceAsStream("/db/current/V1__initial_schema.sql")) {
                schema = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
              }
              for (String sql : schema.split(";")) if (!sql.isBlank()) execute(connection, sql);
              execute(connection, "INSERT INTO users(email,password_hash,display_name) VALUES ('preserved@example.com','hash','보존 계정')");
              execute(connection, "INSERT INTO posts(author_id,title,status) VALUES (1,'보존 게시물','DRAFT')");
              execute(connection, "INSERT INTO approval_logs(post_id,reviewer_id,decision) VALUES (1,1,'REJECTED')");
              execute(connection, "INSERT INTO bookmarks(user_id,post_id) VALUES (1,1)");
              execute(connection, "INSERT INTO post_files(post_id,file_kind,storage_key,original_name,content_type,size_bytes) VALUES (1,'POSTER','keep.png','원본.png','image/png',10)");
              if (scenario.equals("legacy")) {
                for (String column : new String[]{"display_from","display_until","version"})
                  execute(connection, "ALTER TABLE posts DROP COLUMN " + column);
                for (String column : new String[]{"snapshot","poster_key","evidence_key","original_key","poster_type","evidence_type","original_type"})
                  execute(connection, "ALTER TABLE approval_logs DROP COLUMN " + column);
              } else {
                execute(connection, "UPDATE posts SET version=7,display_until='2030-12-31'");
                execute(connection, "UPDATE approval_logs SET snapshot='보존 이력',poster_key='keep.png'");
              }
            }
          }
          // Use the real Boot configuration: proves migrations are included and run before JPA.
          try (var app = new SpringApplicationBuilder(BrainBucketBackendApplication.class).run(
              "--spring.datasource.url=" + url, "--spring.datasource.username=" + user,
              "--spring.datasource.password=" + password, "--server.port=0",
              "--spring.jpa.hibernate.ddl-auto=validate", "--spring.main.banner-mode=off")) {
            try (Connection connection = app.getBean(DataSource.class).getConnection()) {
              assertEquals(2, scalar(connection, "SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success=1"));
              if (!scenario.equals("empty")) {
                assertEquals("보존 게시물", value(connection, "SELECT title FROM posts WHERE id=1"));
                assertEquals("보존 계정", value(connection, "SELECT display_name FROM users WHERE id=1"));
                assertEquals("keep.png", value(connection, "SELECT storage_key FROM post_files WHERE id=1"));
                assertEquals(1, scalar(connection, "SELECT COUNT(*) FROM bookmarks"));
                assertEquals(scenario.equals("manual") ? 7 : 0, scalar(connection, "SELECT version FROM posts WHERE id=1"));
                if (scenario.equals("manual")) assertEquals("보존 이력", value(connection, "SELECT snapshot FROM approval_logs WHERE id=1"));
              }
            }
          }
          var configuration = Flyway.configure().dataSource(url,user,password).baselineOnMigrate(true)
              .baselineVersion("1").cleanDisabled(true);
          com.brainbucket.config.MigrationCompatibilityConfig.configure(configuration);
          var flyway = configuration.load();
          assertEquals(0, flyway.migrate().migrationsExecuted, scenario + " restart must be a no-op");
        } finally {
          execute(server, "DROP DATABASE `" + database + "`");
        }
      }
    }
  }

  @Test
  void bothPublishedMigrationHistoriesStartWithoutRewritingHistory() throws Exception {
    String base=System.getenv("BB_MIGRATION_TEST_URL");
    if (!base.matches("jdbc:mysql://[^/?]+/?")) throw new IllegalArgumentException("Use a server-only disposable MySQL URL");
    if (!base.endsWith("/")) base+="/";
    String user=System.getenv().getOrDefault("BB_MIGRATION_TEST_USER","root");
    String password=System.getenv().getOrDefault("BB_MIGRATION_TEST_PASSWORD","");
    try (var server=DriverManager.getConnection(base,user,password)) {
      for (boolean legacy : new boolean[]{false,true}) {
        String database="bb_migration_"+UUID.randomUUID().toString().replace("-","");
        execute(server,"CREATE DATABASE `"+database+"` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        try {
          String url=base+database;
          Flyway.configure().dataSource(url,user,password)
              .locations(legacy ? "classpath:db/legacy" : "classpath:db/current")
              .javaMigrations(legacy ? new db.migration.V2__integrate_post_management() : new db.migration.V2__complete_existing_schema())
              .load().migrate();
          try (var c=DriverManager.getConnection(url,user,password)) {
            execute(c,"INSERT INTO users(email,password_hash,display_name) VALUES ('keep@example.com','hash','Keep')");
          }
          try (var app=new SpringApplicationBuilder(BrainBucketBackendApplication.class).run(
              "--spring.datasource.url="+url,"--spring.datasource.username="+user,
              "--spring.datasource.password="+password,"--server.port=0","--spring.jpa.hibernate.ddl-auto=validate")) {
            assertTrue(app.getBean(Flyway.class).validateWithResult().validationSuccessful);
            try (var c=app.getBean(DataSource.class).getConnection()) {
              assertEquals(1,scalar(c,"SELECT COUNT(*) FROM users WHERE email='keep@example.com'"));
              assertEquals(legacy ? "integrate post management" : "complete existing schema",
                  value(c,"SELECT description FROM flyway_schema_history WHERE version='2'"));
            }
          }
        } finally { execute(server,"DROP DATABASE `"+database+"`"); }
      }
    }
  }

  private static void execute(Connection connection, String sql) throws SQLException {
    try (var statement = connection.createStatement()) { statement.execute(sql); }
  }
  private static int scalar(Connection connection, String sql) throws SQLException {
    try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
      rows.next(); return rows.getInt(1);
    }
  }
  private static String value(Connection connection, String sql) throws SQLException {
    try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
      rows.next(); return rows.getString(1);
    }
  }
}
