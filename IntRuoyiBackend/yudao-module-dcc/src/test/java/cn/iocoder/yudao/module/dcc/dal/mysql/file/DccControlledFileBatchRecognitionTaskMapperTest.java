package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DccControlledFileBatchRecognitionTaskMapperTest extends BaseDbUnitTest {

    @Resource
    private DataSource dataSource;

    @Test
    void activeTaskUniqueGuardIsScopedByTenantAndRecognitionTypeAndReleasedByTerminalStatus() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                insertTask(connection, 1L, 0L, "BASIC_INFO", "WAITING");

                assertThrows(SQLException.class,
                        () -> insertTask(connection, 2L, 0L, "BASIC_INFO", "RUNNING"));
                assertDoesNotThrow(() -> insertTask(connection, 3L, 0L, "FILE_CATEGORY", "WAITING"));
                assertDoesNotThrow(() -> insertTask(connection, 4L, 1L, "BASIC_INFO", "WAITING"));

                updateTaskStatus(connection, 1L, "COMPLETED");
                assertDoesNotThrow(() -> insertTask(connection, 5L, 0L, "BASIC_INFO", "RUNNING"));
            } finally {
                connection.rollback();
            }
        }
    }

    private void insertTask(Connection connection, Long id, Long tenantId,
                            String recognitionType, String status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO dcc_controlled_file_batch_recognition_task
                    (id, operator_user_id, recognition_type, scope_type, recognition_version_snapshot,
                     candidate_ids_json, status, tenant_id)
                VALUES (?, ?, ?, 'GLOBAL', 'test-v1', '[]', ?, ?)
                """)) {
            statement.setLong(1, id);
            statement.setLong(2, 99L);
            statement.setString(3, recognitionType);
            statement.setString(4, status);
            statement.setLong(5, tenantId);
            statement.executeUpdate();
        }
    }

    private void updateTaskStatus(Connection connection, Long id, String status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE dcc_controlled_file_batch_recognition_task
                   SET status = ?
                 WHERE id = ?
                """)) {
            statement.setString(1, status);
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }
}
