package com.example.retailstore.alerts;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class LowStockAlertMigrationTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    @Test
    void backfillsOnlyExistingLowStockUsingStockIdsAndRequiredFields() throws Exception {
        migrate("001-initial-schema.xml");
        migrate("002-stock-tracking.xml");
        try (Connection connection = connection();
                var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO products (id, created_at, updated_at, version, sku, name, category,
                                          unit_cost, reorder_level, initial_stock)
                    SELECT n, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'SKU-' || n, 'Product', 'Tools', 1, 5, 10
                    FROM generate_series(1, 4) n
                    """);
            statement.executeUpdate("""
                    INSERT INTO stocks (id, created_at, updated_at, version, product_id, quantity_on_hand, last_updated)
                    VALUES (101, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 1, 4, CURRENT_TIMESTAMP),
                           (102, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 2, 5, CURRENT_TIMESTAMP),
                           (103, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 3, 6, CURRENT_TIMESTAMP)
                    """);
        }

        migrate("004-low-stock-alerts.xml");
        migrate("004-low-stock-alerts.xml");

        try (Connection connection = connection();
                var statement = connection.createStatement();
                var rows = statement.executeQuery("SELECT * FROM low_stock_alerts ORDER BY product_id")) {
            for (long productId : new long[] {1, 2}) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getLong("id")).isEqualTo(100 + productId);
                assertThat(rows.getLong("product_id")).isEqualTo(productId);
                assertThat(rows.getLong("version")).isZero();
                assertThat(rows.getBoolean("acknowledged")).isFalse();
                assertThat(rows.getTimestamp("created_at")).isNotNull();
                assertThat(rows.getTimestamp("updated_at")).isNotNull();
                assertThat(rows.getTimestamp("alerted_at")).isNotNull();
                assertThat(rows.getTimestamp("resolved_at")).isNull();
            }
            assertThat(rows.next()).isFalse();
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private void migrate(String file) throws Exception {
        var database =
                DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection()));
        try (var resources = new ClassLoaderResourceAccessor();
                var liquibase = new Liquibase("db/changelog/" + file, resources, database)) {
            liquibase.update(new Contexts(), new LabelExpression());
        }
    }
}
