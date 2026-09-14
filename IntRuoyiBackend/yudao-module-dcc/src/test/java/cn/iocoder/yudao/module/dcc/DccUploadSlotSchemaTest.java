package cn.iocoder.yudao.module.dcc;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DccUploadSlotSchemaTest {

    @Test
    void schemaDefinesOneActiveTicketPerTenantUserSessionAndPurpose() throws Exception {
        Path projectDir = findProjectDir();
        String migration = Files.readString(projectDir.resolve(
                "sql/mysql/20260811_dcc_upload_slot_idempotency.sql")).toLowerCase(Locale.ROOT);
        String testSchema = Files.readString(projectDir.resolve(
                "yudao-module-dcc/src/test/resources/sql/create_tables.sql")).toLowerCase(Locale.ROOT);

        assertActiveSlotConstraint(migration);
        assertActiveSlotConstraint(testSchema);
        assertTrue(migration.contains("signal sqlstate '45000'"),
                "migration must fail fast when historical active-slot duplicates exist");
    }

    private void assertActiveSlotConstraint(String schema) {
        assertTrue(schema.contains("`active_slot_unique_flag`"),
                "schema must define the active-slot generated flag");
        assertTrue(schema.contains("generated always as"),
                "active-slot flag must be generated from row state");
        assertTrue(schema.contains("`uk_dcc_temp_active_slot`"),
                "schema must define the active-slot unique key");
        assertTrue(schema.contains("`tenant_id`, `uploader_id`, `session_id`, `purpose`, `active_slot_unique_flag`"),
                "active-slot unique key must cover tenant, user, session and purpose");
    }

    private Path findProjectDir() {
        Path current = Path.of("").toAbsolutePath().normalize();
        if (Files.isDirectory(current.resolve("sql/mysql"))) {
            return current;
        }
        Path parent = current.getParent();
        if (parent != null && Files.isDirectory(parent.resolve("sql/mysql"))) {
            return parent;
        }
        throw new IllegalStateException("Unable to locate IntRuoyiBackend project directory from " + current);
    }
}
