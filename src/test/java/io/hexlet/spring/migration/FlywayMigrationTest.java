package io.hexlet.spring.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Checks that Flyway applied all migrations from db/migration at context start (LR4):
 * flyway_schema_history has successful rows for versions 1, 1.1, 2, 3,
 * PRODUCT has 3 rows, authors/books contain the seed data.
 */
@SpringBootTest
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Test
    void schemaHistoryContainsAllMigrations() {
        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = TRUE AND version IS NOT NULL ORDER BY installed_rank",
                String.class);

        assertThat(versions).containsExactly("1", "1.1", "2", "3");
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void productTableContainsThreeRows() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM PRODUCT", Integer.class);
        List<String> names = jdbcTemplate.queryForList("SELECT name FROM PRODUCT ORDER BY id", String.class);
        Integer withDates = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PRODUCT WHERE created_at IS NOT NULL AND modified_at IS NOT NULL",
                Integer.class);

        assertThat(count).isEqualTo(3);
        assertThat(names).containsExactly("Product 1", "Product 2", "Product 3");
        assertThat(withDates).isEqualTo(3);
    }

    @Test
    void seedDataForBooksAndAuthorsIsPresent() {
        String email = jdbcTemplate.queryForObject("SELECT email FROM authors WHERE id = 2", String.class);
        String slug = jdbcTemplate.queryForObject("SELECT slug FROM books WHERE id = 1", String.class);
        Long authorId = jdbcTemplate.queryForObject("SELECT author_id FROM books WHERE id = 1", Long.class);

        assertThat(jdbcTemplate.queryForObject("SELECT email FROM authors WHERE id = 1", String.class))
                .isEqualTo("leo.tolstoy@example.com");
        assertThat(email).isEqualTo("fyodor.dostoevsky@example.com");
        assertThat(slug).isEqualTo("crime-and-punishment");
        assertThat(authorId).isEqualTo(2L);
    }
}
