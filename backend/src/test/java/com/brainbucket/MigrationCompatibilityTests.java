package com.brainbucket;

import static org.junit.jupiter.api.Assertions.*;

import com.brainbucket.config.MigrationCompatibilityConfig;
import java.sql.*;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

/** H2 tests resolution/validation only, not MySQL DDL execution. */
class MigrationCompatibilityTests {
  private String database() { return "jdbc:h2:mem:compat_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1"; }

  private void history(String url, boolean legacySql, boolean sqlV1, boolean legacyJava) throws Exception {
    // Obtain the real resolver's checksum without executing MySQL-specific SQL on H2.
    String location = legacySql ? "classpath:db/legacy" : "classpath:db/current";
    var initial = Flyway.configure().dataSource(url, "sa", "").locations(location).load();
    Integer checksum = initial.info().pending()[0].getChecksum();
    Flyway.configure().dataSource(url, "sa", "").locations(location).baselineVersion("1").load().baseline();
    try (var c = DriverManager.getConnection(url,"sa", "")) {
      if (sqlV1) {
        try (var s = c.prepareStatement("UPDATE \"flyway_schema_history\" SET \"type\"='SQL', \"description\"='initial schema', \"script\"='V1__initial_schema.sql', \"checksum\"=? WHERE \"version\"='1'")) {
          s.setInt(1, checksum); s.executeUpdate();
        }
      }
      try (var s = c.prepareStatement("INSERT INTO \"flyway_schema_history\" (\"installed_rank\",\"version\",\"description\",\"type\",\"script\",\"checksum\",\"installed_by\",\"execution_time\",\"success\") VALUES (2,'2',?,'JDBC',?,?,'SA',1,TRUE)")) {
        s.setString(1,legacyJava ? "integrate post management" : "complete existing schema");
        s.setString(2,legacyJava ? "db.migration.V2__integrate_post_management" : "db.migration.V2__complete_existing_schema");
        if (legacyJava) s.setInt(3,20261010); else s.setNull(3,Types.INTEGER);
        s.executeUpdate();
      }
    }
  }

  @Test void bothPublishedVersionsValidateAndRestartWithoutRepair() throws Exception {
    for (boolean sqlV1 : new boolean[]{false,true}) {
      for (boolean legacySql : new boolean[]{false,true}) {
        for (boolean legacyJava : new boolean[]{false,true}) {
          String url=database(); history(url,legacySql,sqlV1,legacyJava);
          var config=Flyway.configure().dataSource(url,"sa","");
          MigrationCompatibilityConfig.configure(config);
          var flyway=config.load();
          assertTrue(flyway.validateWithResult().validationSuccessful,
              "sqlV1="+sqlV1+", legacySql="+legacySql+", legacyJava="+legacyJava);
          assertEquals(0,flyway.migrate().migrationsExecuted);
          try (var c=DriverManager.getConnection(url,"sa",""); var s=c.createStatement();
              var rows=s.executeQuery("SELECT \"description\" FROM \"flyway_schema_history\" WHERE \"version\"='2'")) {
            assertTrue(rows.next());
            assertEquals(legacyJava ? "integrate post management" : "complete existing schema",rows.getString(1));
          }
        }
      }
    }
  }

  @Test void unknownChecksumStillFailsValidation() throws Exception {
    String url=database(); history(url,false,true,false);
    try (var c=DriverManager.getConnection(url,"sa","");var s=c.createStatement()) {
      s.executeUpdate("UPDATE \"flyway_schema_history\" SET \"checksum\"=123 WHERE \"version\"='1'");
    }
    var config=Flyway.configure().dataSource(url,"sa","");
    MigrationCompatibilityConfig.configure(config);
    assertFalse(config.load().validateWithResult().validationSuccessful);
  }

  @Test void emptyDatabaseSelectsCurrentMigrations() {
    var config=Flyway.configure().dataSource(database(),"sa","");
    MigrationCompatibilityConfig.configure(config);
    var pending=config.load().info().pending();
    assertEquals(2,pending.length);
    assertEquals("initial schema",pending[0].getDescription());
    assertEquals("complete existing schema",pending[1].getDescription());
  }

  @Test void springBootUsesCompatibilitySelectionBeforeJpaStarts() throws Exception {
    for (boolean legacy : new boolean[]{false,true}) {
      String url=database(); history(url,legacy,true,legacy);
      try (var app=new org.springframework.boot.builder.SpringApplicationBuilder(BrainBucketBackendApplication.class).run(
          "--spring.datasource.url="+url,"--spring.datasource.username=sa","--spring.datasource.password=",
          "--spring.jpa.hibernate.ddl-auto=update","--spring.flyway.enabled=true","--spring.sql.init.mode=never",
          "--server.port=0","--app.upload-dir="+java.nio.file.Files.createTempDirectory("bb-compat-files"))) {
        var flyway=app.getBean(Flyway.class);
        assertTrue(flyway.validateWithResult().validationSuccessful);
        assertEquals("2",flyway.info().current().getVersion().getVersion());
      }
    }
  }
}
