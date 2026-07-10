package com.ikms.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PostgresIntegrationTestSmokeTest extends PostgresIntegrationTest {

  @Autowired
  private DataSource dataSource;

  @Test
  void startsPostgresWithPgvectorAvailable() throws Exception {
    try (Connection connection = dataSource.getConnection();
        ResultSet resultSet = connection.createStatement()
            .executeQuery("select extname from pg_extension where extname = 'vector'")) {
      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getString(1)).isEqualTo("vector");
    }
  }
}
