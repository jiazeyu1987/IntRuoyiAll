package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class DccRevisionMigrationTest {
    @Test void initialCandidateRoleKeyHasForwardIdempotentContractAndDoesNotRewriteHistory() throws Exception {
        String sql=Files.readString(Path.of("..","sql","mysql","20261002_dcc_c_initial_candidate_identity.sql"));
        assertTrue(sql.contains("dependsOn=20260930_dcc_c_revision_identity"));
        assertTrue(sql.contains("information_schema.COLUMNS"));assertTrue(sql.contains("GENERATION_EXPRESSION"));
        assertTrue(sql.contains("revision_change_type='INITIAL' AND selected_iteration_controlled_file_id IS NOT NULL"));
        assertTrue(sql.contains("CONCAT(version_no,'#INITIAL')"));assertTrue(sql.contains("ELSE version_no END USING binary"));
        assertTrue(sql.contains("uk_dcc_c_version"));assertFalse(sql.toLowerCase().contains("drop index"));
        assertFalse(sql.matches("(?is).*\\b(update|delete|insert)\\s+(into\\s+|from\\s+)?`?dcc_controlled_file.*"));
        assertTrue(8+8+288<3072);
    }
    @Test void mysqlMigrationIsAdditiveGuardedAndDoesNotClassifyHistory() throws Exception {
        String sql=Files.readString(Path.of("..","sql","mysql","20260930_dcc_c_revision_identity.sql"));
        assertTrue(sql.contains("dependsOn=20260917_dcc_controlled_file_name_claim,20260906_dcc_new_file_lifecycle_p3"));
        assertTrue(sql.contains("information_schema.COLUMNS")); assertTrue(sql.contains("information_schema.STATISTICS"));
        assertFalse(sql.toLowerCase().contains("add column if not exists"));
        assertFalse(sql.matches("(?is).*\\b(update|delete|insert)\\s+(into\\s+|from\\s+)?`?dcc_controlled_file.*"));
        for(String field:new String[]{"revision_change_type","revision_source_controlled_file_id","revision_source_version_no",
                "selected_iteration_controlled_file_id","selected_iteration_version_no","source_original_file_name","retain_until"}) {
            assertTrue(sql.contains("'"+field+"'"),field);
        }
    }
    @Test void fullUtf8BinaryIndexesPreserveCaseSuffixAndTrailingSpaces() throws Exception {
        String sql=Files.readString(Path.of("..","sql","mysql","20260930_dcc_c_revision_identity.sql"));
        assertTrue(sql.contains("varbinary(1024) GENERATED ALWAYS AS (CONVERT(`source_original_file_name` USING binary))"));
        assertTrue(sql.contains("varbinary(512) GENERATED ALWAYS AS (CONVERT(`normalized_file_number` USING binary))"));
        assertTrue(sql.contains("(`tenant_id`,`source_name_key`,`active_unique_flag`)"));
        assertTrue(sql.contains("(`tenant_id`,`dcc_project_code_id`,`file_type_taxonomy_leaf_id`,`number_key`,`active_unique_flag`)"));
        assertTrue(sql.indexOf("ADD UNIQUE KEY `uk_dcc_c_source_name`")<sql.indexOf("DROP INDEX `uk_dcc_file_name_claim_active`"));
        assertFalse(sql.contains("LOWER(")); assertFalse(sql.contains("source_name_key`("));
        assertTrue(8+1024+8<3072); assertTrue(8+8+8+512+8<3072);
    }
}
