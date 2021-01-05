package pro.taskana.common.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import javax.sql.DataSource;
import org.assertj.core.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pro.taskana.SpringTaskanaEngineConfiguration;
import pro.taskana.common.internal.configuration.DB;
import pro.taskana.common.test.rest.TaskanaSpringBootTest;
import pro.taskana.sampledata.SampleDataGenerator;

/** Test that the schema name can be customized. */
@TaskanaSpringBootTest
class TestSchemaNameCustomizable {

  String schemaName = "CUSTOMSCHEMANAME";
  boolean isPostgres = false;

  @Autowired private DataSource dataSource;

  void resetDb() throws SQLException {
    SampleDataGenerator sampleDataGenerator;
    try (Connection connection = dataSource.getConnection()) {
      String databaseProductId =
          DB.getDatabaseProductId(connection.getMetaData().getDatabaseProductName());

      if (DB.isPostgres(databaseProductId)) {
        schemaName = schemaName.toLowerCase(Locale.ENGLISH);
      }
    }
    new SpringTaskanaEngineConfiguration(dataSource, true, true, schemaName);
    sampleDataGenerator = new SampleDataGenerator(dataSource, schemaName);
    sampleDataGenerator.generateSampleData();
  }

  @Test
  void checkCustomSchemaNameIsDefined_Postgres() throws SQLException {
    resetDb();
    Assumptions.assumeThat(isPostgres).isTrue();
    ;
    try (Connection connection = dataSource.getConnection()) {

      try (PreparedStatement preparedStatement =
          connection.prepareStatement(
              "SELECT tablename FROM pg_catalog.pg_tables where schemaname = ?")) {
        preparedStatement.setString(1, schemaName);
        ResultSet rs = preparedStatement.executeQuery();

        boolean tablefound = false;
        while (rs.next() && !tablefound) {
          String tableName = rs.getString("tablename");
          tablefound = tableName.equals("workbasket");
        }
        assertThat(tablefound).as("Table workbasket should be there ...").isTrue();
      }
    }
  }

  @Test
  void checkCustomSchemaNameIsDefined_OtherDb() throws SQLException {
    resetDb();
    Assumptions.assumeThat(isPostgres).isTrue();
    try (Connection connection = dataSource.getConnection()) {

      try (PreparedStatement preparedStatement =
          connection.prepareStatement(
              "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?")) {
        preparedStatement.setString(1, schemaName);
        ResultSet rs = preparedStatement.executeQuery();
        boolean tablefound = false;
        while (rs.next() && !tablefound) {
          String tableName = rs.getString("TABLE_NAME");
          tablefound = tableName.equals("WORKBASKET");
        }
        assertThat(tablefound).as("Table WORKBASKET should be there ...").isTrue();
      }
    }
  }
}
