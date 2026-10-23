package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationImpactAuditDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationFollowupBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationImpactAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationImpactTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationRelationSnapshotMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;

class DccImpactAssessmentTransactionIntegrationTest extends BaseDbUnitTest {

    @Resource private DataSource dataSource;
    @Resource private PlatformTransactionManager transactionManager;
    @Resource private DccPublicationFollowupBatchMapper batchMapper;
    @Resource private DccPublicationRelationSnapshotMapper relationMapper;
    @Resource private DccPublicationImpactTaskMapper taskMapper;
    @Resource private DccPublicationImpactAuditMapper auditMapper;
    @Resource private DccControlledFileMapper controlledFileMapper;

    private JdbcTemplate jdbcTemplate;
    private DccRelatedFileImpactAssessmentServiceImpl service;

    @BeforeEach
    void setUpService() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        DccPublicationImpactAuditMapper failingAuditMapper = mock(DccPublicationImpactAuditMapper.class,
                Mockito.withSettings().defaultAnswer(delegatesTo(auditMapper)));
        doThrow(new IllegalStateException("injected impact audit failure"))
                .when(failingAuditMapper).insert(any(DccPublicationImpactAuditDO.class));
        service = new DccRelatedFileImpactAssessmentServiceImpl();
        set("batchMapper", batchMapper);
        set("relationMapper", relationMapper);
        set("taskMapper", taskMapper);
        set("auditMapper", failingAuditMapper);
        set("adminUserApi", mock(AdminUserApi.class));
        set("permissionApi", mock(PermissionApi.class));
        set("controlledFileMapper", controlledFileMapper);
        set("followupStatusService", mock(DccPublicationFollowupStatusService.class));
    }

    @Test
    void startTask_auditFailureRollsBackStateAndVersionUpdate() {
        jdbcTemplate.update("""
                INSERT INTO dcc_publication_impact_task
                  (id, batch_id, publication_relation_snapshot_id, published_controlled_file_id,
                   related_master_id, related_active_controlled_file_id, assignee_user_id,
                   task_status, revision_tracking_status, row_version, creation_token, tenant_id, deleted)
                VALUES (10, 77, 701, 100, 20, 200, 99, 'PENDING', 'NOT_APPLICABLE', 0,
                        'start-audit-failure', 1, 0)
                """);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new TransactionTemplate(transactionManager).executeWithoutResult(
                        ignored -> service.startTask(99L, 10L, 0)));

        assertEquals("injected impact audit failure", error.getMessage());
        assertEquals("PENDING", jdbcTemplate.queryForObject(
                "SELECT task_status FROM dcc_publication_impact_task WHERE id = 10", String.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT row_version FROM dcc_publication_impact_task WHERE id = 10", Integer.class));
        assertEquals(0L, count("dcc_publication_impact_audit"));
    }

    @Test
    void materializeTask_auditFailureRollsBackUniqueTaskInsert() {
        jdbcTemplate.update("""
                INSERT INTO dcc_publication_followup_batch
                  (id, published_controlled_file_id, published_master_id, category_id,
                   file_number_snapshot, file_name_snapshot, version_no_snapshot, status,
                   published_at, creation_token, tenant_id, deleted)
                VALUES (77, 100, 10, 20, 'PUB-100', '发布文件', 'B/1', 'PENDING',
                        TIMESTAMP '2026-09-07 13:00:00', 'batch-token', 1, 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO dcc_publication_relation_snapshot
                  (id, batch_id, related_master_id, related_active_controlled_file_id,
                   related_file_number_snapshot, related_file_name_snapshot, related_version_no_snapshot,
                   responsible_user_id_snapshot, responsible_user_name_snapshot,
                   responsible_user_status_snapshot, resolution_status, frozen_at, tenant_id, deleted)
                VALUES (701, 77, 20, 200, 'REL-20', '关联文件', 'A/1', 99, '负责人', 0,
                        'RESOLVED', TIMESTAMP '2026-09-07 13:00:00', 1, 0)
                """);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new TransactionTemplate(transactionManager).executeWithoutResult(
                        ignored -> service.materializeForPublicationBatch(77L)));

        assertEquals("injected impact audit failure", error.getMessage());
        assertEquals(0L, count("dcc_publication_impact_task"));
        assertEquals(0L, count("dcc_publication_impact_audit"));
    }

    private long count(String table) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return count == null ? 0L : count;
    }

    private void set(String field, Object value) {
        ReflectionTestUtils.setField(service, field, value);
    }
}
