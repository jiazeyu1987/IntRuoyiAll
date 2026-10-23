package cn.iocoder.yudao.module.dcc;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class DccWorkflowMigrationContractTest {
    @Test void schemaIsGuardedAndDoesNotBackfillHistoricalBusinessFacts() throws Exception {
        String schema=Files.readString(Path.of("../sql/mysql/20260930_dcc_a_lifecycle.sql"));
        assertTrue(schema.startsWith("-- release-migration:"));
        for(String column:new String[]{"latest_controlled_file_id","controlled_time","activated_time","distributed_time",
                "distribution_payload_hash","leader_user_id","process_instance_id","assignment_signature_id","assignment_payload_hash","assigned_time"}) {
            assertTrue(schema.contains("COLUMN_NAME='"+column+"'"),"MySQL column guard: "+column);
        }
        assertFalse(schema.contains("ADD COLUMN IF NOT EXISTS"));
        assertFalse(schema.matches("(?is).*UPDATE\\s+dcc_controlled_file(?:_master)?\\s.*"));
        assertFalse(schema.matches("(?is).*INSERT\\s+INTO\\s+dcc_controlled_file(?:_master)?\\s.*"));
        assertTrue(schema.contains("UNIQUE KEY uk_dcc_a_lifecycle_event(tenant_id,event_key)"));
    }
    @Test void bpmnVersionFourOnlyAddsNewDefinitionsAndGuardsConflictAndDependencies() throws Exception {
        String bpmn=Files.readString(Path.of("../sql/mysql/20260930_dcc_a_workflow_bpmn_v4.sql"));
        assertTrue(bpmn.contains("dependsOn=20260930_dcc_a_lifecycle,20260926_dcc_three_workflow_matrix_multi_instance_fix"));
        assertTrue(bpmn.contains("SIGNAL SQLSTATE '45000'"));
        assertTrue(bpmn.contains("conflicting BPMN"));
        assertFalse(bpmn.matches("(?is).*UPDATE\\s+act_ge_bytearray.*"));
        assertFalse(bpmn.matches("(?is).*UPDATE\\s+act_ru_.*"));
        assertTrue(bpmn.contains("-bpmn-v4-tenant-"));
        assertTrue(bpmn.contains("NOT EXISTS"));
        assertTrue(bpmn.contains("PROCESS_DCC_TASK_OBLIGATION_IDS"));
    }
    @Test void signatureWorkflowRoundMigrationOnlyAddsNewEvidenceColumns() throws Exception {
        String migration=Files.readString(Path.of("../sql/mysql/20260930_dcc_a_signature_workflow_round.sql"));
        assertTrue(migration.startsWith("-- release-migration:"));
        assertTrue(migration.contains("dependsOn=20260930_dcc_a_lifecycle"));
        assertTrue(migration.contains("COLUMN_NAME='process_instance_id'"));
        assertTrue(migration.contains("COLUMN_NAME='file_number_snapshot'"));
        assertFalse(migration.matches("(?is).*UPDATE\\s+dcc_controlled_file_signature\\s.*"));
        assertFalse(migration.contains("ADD COLUMN IF NOT EXISTS"));
    }
    @Test void futureObsoleteUsesAnExplicitPolicyWithoutChangingOriginalActivePolicy() throws Exception {
        String migration=Files.readString(Path.of("../sql/mysql/20260930_dcc_a_future_obsolete_policy.sql"));
        assertTrue(migration.contains("dependsOn=20260930_dcc_a_workflow_bpmn_v4,20260930_dcc_a_signature_workflow_round"));
        assertTrue(migration.contains("'CONTROLLED_PENDING_EFFECTIVE'"));
        assertTrue(migration.contains("NOT EXISTS"));
        assertTrue(migration.contains("SIGNAL SQLSTATE '45000'"));
        assertFalse(migration.matches("(?is).*UPDATE\\s+bpm_business_approval_policy\\s.*"));
        assertFalse(migration.matches("(?is).*UPDATE\\s+dcc_controlled_file\\s.*"));
    }
}
