package com.brainbucket.config;

import db.migration.V2__complete_existing_schema;
import db.migration.V2__integrate_post_management;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.zip.CRC32;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Resolve either already-published migration identity without rewriting successful DB history. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class MigrationCompatibilityConfig {
  @Bean
  FlywayConfigurationCustomizer migrationCompatibility() {
    return MigrationCompatibilityConfig::configure;
  }

  public static void configure(FluentConfiguration configuration) {
    boolean legacySql = false;
    boolean legacyJava = false;
    try (var connection = configuration.getDataSource().getConnection()) {
      String table = null;
      try (var tables = connection.getMetaData().getTables(connection.getCatalog(), null, "%", null)) {
        while (tables.next()) {
          if ("flyway_schema_history".equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
            table = tables.getString("TABLE_NAME");
            break;
          }
        }
      }
      if (table != null) {
        String quote = connection.getMetaData().getIdentifierQuoteString().trim();
        // The table name is metadata, and column names are fixed Flyway identifiers.
        String sql = "SELECT " + quote + "version" + quote + "," + quote + "description" + quote
            + "," + quote + "checksum" + quote + "," + quote + "type" + quote
            + " FROM " + quote + table.replace(quote, quote + quote) + quote
            + " WHERE " + quote + "success" + quote + "=TRUE";
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
          while (rows.next()) {
            if ("1".equals(rows.getString(1)) && "SQL".equals(rows.getString(4))) {
              int checksum = rows.getInt(3);
              legacySql = !rows.wasNull() && checksum == resourceChecksum("/db/legacy/V1__initial_schema.sql");
            }
            if ("2".equals(rows.getString(1))) {
              legacyJava = "integrate post management".equals(rows.getString(2));
            }
          }
        }
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Cannot inspect migration history; no history was changed.", e);
    }
    configuration.locations(legacySql ? "classpath:db/legacy" : "classpath:db/current");
    configuration.javaMigrations(legacyJava
        ? new V2__integrate_post_management() : new V2__complete_existing_schema());
  }

  // Flyway SQL checksums exclude line separators and a leading UTF-8 BOM.
  private static int resourceChecksum(String path) {
    CRC32 checksum = new CRC32();
    try (var input = MigrationCompatibilityConfig.class.getResourceAsStream(path)) {
      if (input == null) throw new IllegalStateException("Missing migration: " + path);
      try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
        boolean first = true;
        String line;
        while ((line = reader.readLine()) != null) {
          if (first && line.startsWith("\uFEFF")) line = line.substring(1);
          first = false;
          checksum.update(line.getBytes(StandardCharsets.UTF_8));
        }
      }
      return (int) checksum.getValue();
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Cannot read migration: " + path, e);
    }
  }
}
