package cn.iocoder.yudao.module.mdm;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MdmProductSchemaContractTest {

    @Test
    void migrationContainsProductMasterTablesAndReferences() throws Exception {
        String sql = Files.readString(resolveMigration(), StandardCharsets.UTF_8);

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mdm_product`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mdm_product_import_batch`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `mdm_product_import_row`"));
        assertTrue(sql.contains("`product_master_id`"));
        assertTrue(sql.contains("mdm:product:query"));
        assertTrue(sql.contains("mdm:product:import"));
        assertFalse(sql.contains("mdm:product:map-showroom"));
    }

    private Path resolveMigration() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path migration = current.resolve("sql/mysql/20260607_product_master_data.sql");
            if (Files.exists(migration)) {
                return migration;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("MDM_PRODUCT_MIGRATION_MISSING: sql/mysql/20260607_product_master_data.sql");
    }

}
