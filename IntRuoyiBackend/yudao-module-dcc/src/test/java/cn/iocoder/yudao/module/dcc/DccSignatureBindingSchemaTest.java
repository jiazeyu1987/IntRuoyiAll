package cn.iocoder.yudao.module.dcc;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DccSignatureBindingSchemaTest {

    @Test
    void schemaDefinesOneImmutableBindingEventPerTenantSignature() throws Exception {
        Path projectDir = findProjectDir();
        String migration = Files.readString(projectDir.resolve(
                "sql/mysql/20260811_dcc_signature_copy_binding.sql")).toLowerCase(Locale.ROOT);
        String testSchema = Files.readString(projectDir.resolve(
                "yudao-module-dcc/src/test/resources/sql/create_tables.sql")).toLowerCase(Locale.ROOT);

        assertBindingSchema(migration);
        assertBindingSchema(testSchema);
    }

    private void assertBindingSchema(String schema) {
        assertTrue(schema.contains("`dcc_controlled_file_signature_binding`"));
        assertTrue(schema.contains("`original_evidence_hash`"));
        assertTrue(schema.contains("`controlled_copy_file_id`"));
        assertTrue(schema.contains("`controlled_copy_sha256`"));
        assertTrue(schema.contains("`binding_event_key`"));
        assertTrue(schema.contains("`binding_hash`"));
        assertTrue(schema.contains("`uk_dcc_signature_binding_signature` (`tenant_id`, `signature_id`, `deleted`)"));
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
