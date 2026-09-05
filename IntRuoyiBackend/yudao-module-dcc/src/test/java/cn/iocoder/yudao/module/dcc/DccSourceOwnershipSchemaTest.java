package cn.iocoder.yudao.module.dcc;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceBatchDO;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DccSourceOwnershipSchemaTest {

    @Test
    void migrationDefinesExclusiveOwnershipAndRestartableMigrationEvidence() throws Exception {
        String sql = Files.readString(Path.of("..", "sql", "mysql", "20260811_dcc_source_ownership.sql"),
                StandardCharsets.UTF_8).toLowerCase();

        assertTrue(sql.contains("dcc_controlled_file_source_ownership"));
        assertTrue(sql.contains("controlled_file_id"));
        assertTrue(sql.contains("source_file_id"));
        assertTrue(sql.contains("origin_source_file_id"));
        assertTrue(sql.contains("source_sha256"));
        assertTrue(sql.contains("unique key `uk_dcc_source_owner_file` (`tenant_id`, `controlled_file_id`)"));
        assertTrue(sql.contains("unique key `uk_dcc_source_owner_source` (`tenant_id`, `source_file_id`)"));
        assertTrue(sql.contains("dcc_controlled_file_source_migration"));
        assertTrue(sql.contains("legacy_source_file_id"));
        assertTrue(sql.contains("isolated_source_file_id"));
        assertTrue(sql.contains("migration_status"));
        assertTrue(sql.contains("unique key `uk_dcc_source_migration_file` (`tenant_id`, `controlled_file_id`)"));
    }

    @Test
    void migrationDefinesGovernanceBatchAndEvidenceItems() throws Exception {
        String sql = Files.readString(Path.of("..", "sql", "mysql", "20260905_dcc_source_governance.sql"),
                StandardCharsets.UTF_8).toLowerCase();

        assertTrue(sql.contains("dcc_controlled_file_source_governance_batch"));
        assertTrue(sql.contains("tenant_scope_json"));
        assertTrue(sql.contains("snapshot_max_controlled_file_id"));
        assertTrue(sql.contains("rule_version"));
        assertTrue(sql.contains("manifest_sha256"));
        assertTrue(sql.contains("task_key"));
        assertTrue(sql.contains("request_sha256"));
        assertTrue(sql.contains("unique key `uk_dcc_source_governance_batch_task` (`task_key`)"));
        assertTrue(sql.contains("dcc_controlled_file_source_governance_item"));
        assertTrue(sql.contains("batch_id"));
        assertTrue(sql.contains("snapshot_source_file_id"));
        assertTrue(sql.contains("snapshot_source_sha256"));
        assertTrue(sql.contains("snapshot_location_hash"));
        assertTrue(sql.contains("snapshot_history_evidence_hash"));
        assertTrue(sql.contains("shared_group_key"));
        assertTrue(sql.contains("blocker_reason_code"));
        assertTrue(sql.contains("unique key `uk_dcc_source_governance_item_file` (`batch_id`, `tenant_id`, `controlled_file_id`)"));
        String normalizedSql = sql.replace("`", "");
        assertTrue(normalizedSql.contains("blocker_detail varchar(1000)"));
        assertTrue(normalizedSql.contains("processed_by bigint"));
        assertTrue(normalizedSql.contains("processed_time datetime"));
        assertTrue(normalizedSql.contains("completed_count bigint not null default 0"));
        assertTrue(normalizedSql.contains("blocked_count bigint not null default 0"));
        assertTrue(normalizedSql.contains("failed_count bigint not null default 0"));
        assertTrue(sql.contains("dcc_controlled_file_source_global_claim"));
        assertTrue(sql.contains("unique key `uk_dcc_source_global_claim_source` (`source_file_id`)"));
        assertTrue(sql.contains("unique key `uk_dcc_source_global_claim_controlled` (`tenant_id`, `controlled_file_id`)"));
    }

    @Test
    void governanceBatchIsGlobalAndItemQueriesAreTenantScoped() throws Exception {
        assertTrue(DccControlledFileSourceGovernanceBatchDO.class.isAnnotationPresent(TenantIgnore.class));

        String mapper = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao", "module",
                        "dcc", "dal", "mysql", "file", "DccControlledFileSourceGovernanceItemMapper.java"),
                StandardCharsets.UTF_8);
        assertTrue(mapper.contains("selectByBatchAndTenant"));
        assertTrue(mapper.contains("AND tenant_id = #{tenantId}"));
        assertTrue(mapper.contains("@Param(\"tenantId\") Long tenantId"));
    }

    @Test
    void governanceBatchTaskKeyLookupIsGlobalAndRetainsRequestDigestForConflictCheck() throws Exception {
        String mapper = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao", "module",
                        "dcc", "dal", "mysql", "file", "DccControlledFileSourceGovernanceBatchMapper.java"),
                StandardCharsets.UTF_8);
        assertTrue(mapper.contains("@TenantIgnore"));
        assertTrue(mapper.contains("WHERE task_key = #{taskKey}"));

        String source = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao", "module",
                        "dcc", "dal", "dataobject", "file", "DccControlledFileSourceGovernanceBatchDO.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("private String requestSha256;"));
    }
}
