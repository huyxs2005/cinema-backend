package com.cinema.hub.backend.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures the Bookings table contains all columns required by the Booking entity.
 * Automatically adds missing columns so runtime mismatches do not crash the app.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingSchemaMigrationRunner implements ApplicationRunner {

    private static final List<ColumnSpec> REQUIRED_COLUMNS = List.of(
            new ColumnSpec("CustomerEmail", "NVARCHAR(255) NULL"),
            new ColumnSpec("CustomerPhone", "NVARCHAR(20) NULL")
    );

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        REQUIRED_COLUMNS.forEach(this::ensureColumnExists);
    }

    private void ensureColumnExists(ColumnSpec column) {
        try {
            boolean exists = Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                            SELECT CASE WHEN EXISTS (
                                SELECT 1
                                FROM INFORMATION_SCHEMA.COLUMNS
                                WHERE TABLE_SCHEMA = 'dbo'
                                  AND TABLE_NAME = 'Bookings'
                                  AND COLUMN_NAME = ?
                            ) THEN CAST(1 AS BIT) ELSE CAST(0 AS BIT) END
                            """,
                    Boolean.class,
                    column.name()));
            if (!exists) {
                String sql = "ALTER TABLE dbo.Bookings ADD " + column.name() + " " + column.definition();
                jdbcTemplate.execute(sql);
                log.info("Added missing column {} to dbo.Bookings", column.name());
            }
        } catch (Exception ex) {
            log.error("Failed to verify/add column {} on dbo.Bookings", column.name(), ex);
            throw ex;
        }
    }

    private record ColumnSpec(String name, String definition) {
    }
}
