package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

class MesReleaseReworkBatchConstraintTest {
    @Test
    void reworkKeepsBothRoundsAndBindsSecondRoundWithRealUniqueConstraint() throws Exception {
        String schema = Files.readString(Path.of("src/test/resources/sql/create_tables.sql"));
        String table = "mes_pro_process_pool_active_order_release_application";
        int start = schema.indexOf("CREATE TABLE IF NOT EXISTS \"" + table + "\"");
        try (Connection db = DriverManager.getConnection("jdbc:h2:mem:release_round;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
             Statement sql = db.createStatement()) {
            sql.execute(schema.substring(start, schema.indexOf(';', start) + 1));
            String insert = "INSERT INTO " + table + " (id,active_order_id,work_order_id,route_id,route_version_id,"
                    + "application_status,source_snapshot_hash,request_idempotency_key,business_idempotency_key,tenant_id) "
                    + "VALUES (%d,10,20,30,31,'PQC_RELEASE_PENDING','hash','request-%d','round-%d',1)";
            sql.execute(insert.formatted(1, 1, 1));
            bind(sql, 1);
            close(sql, 1, "NONCONFORMANCE_REWORK");
            sql.execute(insert.formatted(2, 2, 2));
            bind(sql, 2);
            try (ResultSet history = sql.executeQuery("SELECT count(*) FROM " + table + " WHERE batch_execution_id=77")) {
                assertTrue(history.next()); assertEquals(2, history.getInt(1));
            }
            String currentSql = String.join(" ", MesProcessPoolActiveOrderReleaseApplicationMapper.class
                    .getMethod("selectByBatchExecutionIdForUpdate", Long.class).getAnnotation(Select.class).value()).replace("b'0'", "FALSE")
                    .replace("#{batchExecutionId}", "77");
            try (ResultSet current = sql.executeQuery(currentSql)) {
                assertTrue(current.next()); assertEquals(2L, current.getLong("id")); assertFalse(current.next());
            }
            sql.execute(insert.formatted(3, 3, 3));
            assertThrows(SQLException.class, () -> bind(sql, 3), "two current applications must remain forbidden");
            close(sql, 2, "NONCONFORMANCE_VOID");
            assertThrows(SQLException.class, () -> bind(sql, 3), "void must not reopen the batch");
        }
    }

    private void close(Statement sql, int id, String decision) throws Exception {
        String statement = String.join(" ", MesProcessPoolActiveOrderReleaseApplicationMapper.class
                .getMethod("closeFromNonconformance", Long.class, Integer.class, String.class, Long.class,
                        java.time.LocalDateTime.class, String.class, String.class).getAnnotation(Update.class).value())
                .replace("b'0'", "FALSE").replace("#{id}", String.valueOf(id))
                .replace("#{expectedVersion}", "2").replace("#{pqcDecision}", "'" + decision + "'")
                .replace("#{decidedBy}", "9").replace("#{decidedAt}", "CURRENT_TIMESTAMP")
                .replace("#{rejectReason}", "'review opinion'").replace("#{dossierSummaryJson}", "'{}'");
        assertEquals(1, sql.executeUpdate(statement));
    }

    private void bind(Statement sql, int id) throws Exception {
        String statement = String.join(" ", MesProcessPoolActiveOrderReleaseApplicationMapper.class
                .getMethod("bindP3BatchExecution", Long.class, Integer.class, Long.class).getAnnotation(Update.class).value()).replace("b'0'", "FALSE")
                .replace("#{id}", String.valueOf(id)).replace("#{expectedVersion}", "1")
                .replace("#{batchExecutionId}", "77");
        assertEquals(1, sql.executeUpdate(statement));
    }
}
