package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MesPqcCorrectionHistoryConstraintTest {

    @Test
    void mysqlMigrationIsGuardedAndReplacesOnlyConstraintWithoutDeletingEvidence() throws Exception {
        String sql = Files.readString(Path.of("../sql/mysql/20260928_mes_pqc_piece_correction_history.sql"));
        assertTrue(sql.contains("dependsOn=20260802_mes_pqc_inspection_task,20260811_mes_process_pool_pqc_repeat_review_constraint"));
        assertTrue(sql.contains("information_schema.columns"));
        assertTrue(sql.contains("information_schema.statistics"));
        assertTrue(sql.contains("GENERATED ALWAYS AS"));
        assertTrue(sql.contains("CASE WHEN deleted = 0 THEN sample_no ELSE NULL END"));
        assertTrue(sql.contains("generation_expression"));
        assertTrue(sql.contains("SIGNAL SQLSTATE '45000'"));
        assertTrue(sql.indexOf("ADD UNIQUE KEY uk_mes_pqc_piece_current") < sql.indexOf("DROP INDEX uk_mes_pqc_piece_item"));
        assertFalse(sql.matches("(?is).*\\b(DELETE\\s+FROM|UPDATE\\s+mes_pqc|TRUNCATE|DROP\\s+TABLE)\\b.*"));
        assertFalse(sql.contains("ADD COLUMN IF NOT EXISTS"));
    }

    @Test
    void twoCorrectionsRetainEveryHistoricalPieceAndOneCurrentPiece() throws Exception {
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            insert(statement, 1L, "original");
            statement.executeUpdate("UPDATE mes_pqc_inspection_piece_detail SET deleted=TRUE WHERE deleted=FALSE");
            insert(statement, 1L, "corrected-once");
            statement.executeUpdate("UPDATE mes_pqc_inspection_piece_detail SET deleted=TRUE WHERE deleted=FALSE");
            insert(statement, 1L, "corrected-twice");

            try (var rows = statement.executeQuery("SELECT COUNT(*) FROM mes_pqc_inspection_piece_detail")) {
                rows.next();
                assertEquals(3, rows.getInt(1));
            }
            try (var rows = statement.executeQuery("SELECT measured_value FROM mes_pqc_inspection_piece_detail WHERE deleted=FALSE")) {
                rows.next();
                assertEquals("corrected-twice", rows.getString(1));
            }
        }
    }

    @Test
    void duplicateCurrentPieceIsStillRejectedAndTenantsRemainIndependent() throws Exception {
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            insert(statement, 1L, "first");
            assertThrows(SQLException.class, () -> insert(statement, 1L, "duplicate"));
            insert(statement, 2L, "other-tenant");
        }
    }

    private static Connection open() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:pqc_history_"
                + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        try (Statement statement = connection.createStatement()) {
            statement.execute(Files.readString(Path.of("src/test/resources/sql/pqc-correction-history-schema.sql")));
        }
        return connection;
    }

    private static void insert(Statement statement, Long tenantId, String value) throws SQLException {
        statement.executeUpdate("INSERT INTO mes_pqc_inspection_piece_detail"
                + " (tenant_id,task_id,sample_no,item_code,measured_value,deleted) VALUES ("
                + tenantId + ",5101,1,'QA-001','" + value + "',FALSE)");
    }
}
