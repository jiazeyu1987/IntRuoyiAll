package cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** Static proposal contract only: this test neither connects to MySQL nor executes migration SQL. */
class MesReleaseTaskNotificationMigrationContractTest {

    @Test
    void proposalPersistsPerFrozenRecipientAndDoesNotResetFormalHistory() throws Exception {
        Path backend = findBackendRoot();
        Path proposal = backend.resolve("sql/mysql/20261003_mes_release_task_notification.sql");
        assertTrue(Files.isRegularFile(proposal), "the durable notification schema proposal is required");
        String sql = Files.readString(proposal, StandardCharsets.UTF_8);
        String normalized = sql.replace("`", "").toLowerCase(Locale.ROOT);
        assertTrue(normalized.contains("create table if not exists mes_pro_edhr_release_task_notify_delivery"));
        for (String column : new String[]{"tenant_id", "work_task_id", "event_type", "user_id",
                "business_key", "template_code", "template_params_json", "initiated_by", "status",
                "attempt_count", "row_version", "last_attempt_at", "sent_at", "system_message_id",
                "last_error_summary"}) {
            assertTrue(normalized.contains(column), "missing delivery fact: " + column);
        }
        assertTrue(Pattern.compile("unique\\s+key\\s+\\w+\\s*\\(\\s*tenant_id\\s*,\\s*work_task_id\\s*,\\s*event_type\\s*,\\s*user_id\\s*\\)")
                .matcher(normalized).find(), "soft deletion must not reset assigned-event identity");
        assertTrue(Pattern.compile("unique\\s+key\\s+\\w+\\s*\\(\\s*tenant_id\\s*,\\s*business_key\\s*\\)")
                .matcher(normalized).find(), "domain business key must remain unique across soft deletion");
        assertTrue(sql.contains("MES_EDHR_PQC_PRODUCTION_RELEASE_TASK_ASSIGNED"));
        assertTrue(sql.contains("dependsOn=20260815_system_notify_message_business_key"));
        assertFalse(Pattern.compile("(?is)\\b(?:delete\\s+from|truncate\\s+table|update)\\s+`?mes_(?:process_pool|pro_edhr)_")
                .matcher(sql).find(), "notification migration must not backfill or rewrite existing business facts");
        assertFalse(Pattern.compile("(?is)update\\s+`?system_notify_template`?\\s+set")
                .matcher(sql).find(), "preserve the existing manager template and user-owned content");
    }

    @Test
    void platformPreflightRequiresFullBusinessKeyUniqueInsteadOfPrefixUnique() throws Exception {
        String sql = Files.readString(findBackendRoot().resolve("sql/mysql/20261003_mes_release_task_notification.sql"),
                StandardCharsets.UTF_8);
        var preflight = Pattern.compile("(?is)SELECT\\s+COUNT\\(\\*\\)\\s+INTO\\s+v_unique_count\\s+FROM\\s*\\((.*?)\\)\\s+AS\\s+platform_identity\\s*;")
                .matcher(sql);
        assertTrue(preflight.find(), "actual migration platform preflight SELECT must be inspectable");
        String query = "SELECT COUNT(*) FROM (" + preflight.group(1) + ") AS platform_identity";
        query = query.replace("information_schema.STATISTICS", "migration_index_statistics")
                .replace("DATABASE()", "'notify_contract'");
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:notify_migration_"
                + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", "sa", ""));
        // Metadata input double only. Execute the actual proposed SELECT, never migration DDL or real MySQL.
        jdbc.execute("""
                CREATE TABLE migration_index_statistics (
                  TABLE_SCHEMA VARCHAR(64),TABLE_NAME VARCHAR(64),NON_UNIQUE INT,
                  INDEX_NAME VARCHAR(64),COLUMN_NAME VARCHAR(64),SEQ_IN_INDEX INT,SUB_PART BIGINT)
                """);
        jdbc.update("INSERT INTO migration_index_statistics VALUES ('notify_contract','system_notify_message',0,'uk_tenant_business','tenant_id',1,NULL)");
        jdbc.update("INSERT INTO migration_index_statistics VALUES ('notify_contract','system_notify_message',0,'uk_tenant_business','business_key',2,NULL)");
        assertEquals(1, jdbc.queryForObject(query, Integer.class), "exact full-column platform identity is accepted");
        jdbc.update("UPDATE migration_index_statistics SET SUB_PART=1 WHERE COLUMN_NAME='business_key'");
        assertEquals(0, jdbc.queryForObject(query, Integer.class),
                "business_key(1) prefix uniqueness does not prove full platform business-key idempotency");
    }

    private Path findBackendRoot() {
        Path location = Path.of("").toAbsolutePath();
        while (location != null) {
            if (Files.isDirectory(location.resolve("sql/mysql"))
                    && Files.isDirectory(location.resolve("yudao-module-mes"))) {
                return location;
            }
            if (Files.isDirectory(location.resolve("IntRuoyiBackend/sql/mysql"))) {
                return location.resolve("IntRuoyiBackend");
            }
            location = location.getParent();
        }
        throw new IllegalStateException("backend migration root is required for the static contract");
    }
}
