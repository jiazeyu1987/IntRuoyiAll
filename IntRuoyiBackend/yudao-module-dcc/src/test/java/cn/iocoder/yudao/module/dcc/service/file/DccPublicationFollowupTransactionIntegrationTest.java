package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationVisibilityUserSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryViewMatrixRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryDistributionRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryTrainingRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMessageJobMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileObsoleteAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingProgressMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationFollowupBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationCandidateMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationCandidateReasonMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationRelationDirectionSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationRelationSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationVisibilityRuleSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationVisibilityUserSnapshotMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.springframework.transaction.TransactionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccPublicationFollowupTransactionIntegrationTest extends BaseDbUnitTest {

    @Resource private PlatformTransactionManager transactionManager;
    @Resource private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    @Resource private DccControlledFileMapper controlledFileMapper;
    @Resource private DccControlledFileMasterMapper masterMapper;
    @Resource private DccFileCategoryMapper categoryMapper;
    @Resource private DccControlledFileDistributionMapper distributionMapper;
    @Resource private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Resource private DccCategoryViewMatrixRuleMapper viewMatrixRuleMapper;
    @Resource private DccPublicationFollowupBatchMapper batchMapper;
    @Resource private DccPublicationVisibilityRuleSnapshotMapper visibilityRuleMapper;
    @Resource private DccPublicationVisibilityUserSnapshotMapper visibilityUserMapper;
    @Resource private DccPublicationNotificationCandidateMapper candidateMapper;
    @Resource private DccPublicationNotificationCandidateReasonMapper candidateReasonMapper;
    @Resource private DccPublicationRelationSnapshotMapper relationSnapshotMapper;
    @Resource private DccPublicationRelationDirectionSnapshotMapper relationDirectionMapper;

    private DccControlledContentAdapter platformAdapter;
    private DccControlledFileFinalizationServiceImpl finalizationService;
    private final AtomicReference<RuntimeException> capturedTransactionFailure = new AtomicReference<>();

    @BeforeEach
    void setUpServicesAndData() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        seedPublicationRows();
        platformAdapter = mock(DccControlledContentAdapter.class);

        DccPublicationVisibilityUserSnapshotMapper failingVisibilityUserMapper = mock(
                DccPublicationVisibilityUserSnapshotMapper.class,
                Mockito.withSettings().defaultAnswer(delegatesTo(visibilityUserMapper)));
        doThrow(new IllegalStateException("injected child snapshot insert failure"))
                .when(failingVisibilityUserMapper).insert(any(DccPublicationVisibilityUserSnapshotDO.class));

        DccControlledFileAssignmentScopeService assignmentScopeService =
                mock(DccControlledFileAssignmentScopeService.class);
        when(assignmentScopeService.filterBusinessVisibleUserIds(Set.of(1L), 100L)).thenReturn(Set.of(1L));
        AdminUserApi adminUserApi = mock(AdminUserApi.class);
        when(adminUserApi.getUserList(Set.of(1L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(1L).setNickname("发布责任人").setStatus(0)));
        DccControlledFileRelatedFileService relatedFileService = mock(DccControlledFileRelatedFileService.class);
        when(relatedFileService.listForwardRelations(100L)).thenReturn(List.of());
        when(relatedFileService.listReverseCurrentActiveRelations(1L, 10L)).thenReturn(List.of());

        DccPublicationFollowupServiceImpl followupService = new DccPublicationFollowupServiceImpl();
        set(followupService, "batchMapper", batchMapper);
        set(followupService, "visibilityRuleMapper", visibilityRuleMapper);
        set(followupService, "visibilityUserMapper", failingVisibilityUserMapper);
        set(followupService, "candidateMapper", candidateMapper);
        set(followupService, "candidateReasonMapper", candidateReasonMapper);
        set(followupService, "relationSnapshotMapper", relationSnapshotMapper);
        set(followupService, "relationDirectionMapper", relationDirectionMapper);
        set(followupService, "viewMatrixRuleMapper", viewMatrixRuleMapper);
        set(followupService, "viewMatrixAccessService", mock(DccControlledFileViewMatrixAccessService.class));
        set(followupService, "assignmentScopeService", assignmentScopeService);
        set(followupService, "distributionMapper", distributionMapper);
        set(followupService, "distributionRecipientMapper", distributionRecipientMapper);
        set(followupService, "relatedFileService", relatedFileService);
        set(followupService, "masterMapper", masterMapper);
        set(followupService, "controlledFileMapper", controlledFileMapper);
        set(followupService, "adminUserApi", adminUserApi);
        set(followupService, "deptApi", mock(DeptApi.class));

        PermissionApi permissionApi = mock(PermissionApi.class);
        when(permissionApi.hasAnyPermissions(9L, "dcc:controlled-file:approve")).thenReturn(true);
        finalizationService = new DccControlledFileFinalizationServiceImpl();
        set(finalizationService, "transactionTemplate", new CapturingTransactionTemplate(
                transactionManager, capturedTransactionFailure));
        set(finalizationService, "controlledFileMapper", controlledFileMapper);
        set(finalizationService, "controlledFileMasterMapper", masterMapper);
        set(finalizationService, "distributionMapper", distributionMapper);
        set(finalizationService, "distributionRecipientMapper", distributionRecipientMapper);
        set(finalizationService, "trainingMapper", mock(DccControlledFileTrainingMapper.class));
        set(finalizationService, "trainingAssignmentMapper", mock(DccControlledFileTrainingAssignmentMapper.class));
        set(finalizationService, "trainingProgressMapper", mock(DccControlledFileTrainingProgressMapper.class));
        set(finalizationService, "messageJobMapper", mock(DccControlledFileMessageJobMapper.class));
        set(finalizationService, "obsoleteAuditMapper", mock(DccControlledFileObsoleteAuditMapper.class));
        set(finalizationService, "categoryMapper", categoryMapper);
        set(finalizationService, "distributionRuleMapper", mock(DccFileCategoryDistributionRuleMapper.class));
        set(finalizationService, "trainingRuleMapper", mock(DccFileCategoryTrainingRuleMapper.class));
        set(finalizationService, "fileMapper", mock(FileMapper.class));
        set(finalizationService, "fileService", mock(FileService.class));
        set(finalizationService, "pdfStampService", mock(DccPdfStampService.class));
        set(finalizationService, "pdfConversionService", mock(DccDocumentPdfConversionService.class));
        set(finalizationService, "adminUserApi", adminUserApi);
        set(finalizationService, "permissionApi", permissionApi);
        set(finalizationService, "queryService", mock(DccControlledFileQueryService.class));
        set(finalizationService, "permissionSupport", mock(DccControlledFileCategoryPermissionSupport.class));
        set(finalizationService, "messageDeliveryService", mock(DccControlledFileMessageDeliveryService.class));
        set(finalizationService, "obsoleteFileStorageService", mock(DccObsoleteFileStorageService.class));
        set(finalizationService, "platformAdapter", platformAdapter);
        set(finalizationService, "pendingActionGuard", mock(DccControlledFilePendingActionGuard.class));
        set(finalizationService, "signatureBindingService", mock(DccControlledFileSignatureBindingService.class));
        set(finalizationService, "publicationFollowupService", followupService);
    }

    @Test
    void applyApprovedPublishControlledFile_childSnapshotFailureRollsBackWholePublicationTransaction() {
        assertFalse(Mockito.mockingDetails(transactionManager).isMock());
        assertFalse(Mockito.mockingDetails(controlledFileMapper).isMock());
        assertFalse(Mockito.mockingDetails(masterMapper).isMock());

        ServiceException error = assertThrows(ServiceException.class,
                () -> finalizationService.applyApprovedPublishControlledFile(9L, 100L, "tx-followup-failure"));

        assertEquals("injected child snapshot insert failure",
                capturedTransactionFailure.get() == null ? null : capturedTransactionFailure.get().getMessage(),
                capturedTransactionFailure.get() == null ? "no transaction failure captured"
                        : java.util.Arrays.toString(capturedTransactionFailure.get().getStackTrace()));
        assertTrue(error.getMessage().contains("injected child snapshot insert failure"), error.getMessage());
        assertEquals(DccControlledFileStatusEnum.ACTIVE.getStatus(), stringValue(
                "SELECT status FROM dcc_controlled_file WHERE id = 99"));
        assertEquals(DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus(), stringValue(
                "SELECT status FROM dcc_controlled_file WHERE id = 100"));
        assertEquals(99L, longValue(
                "SELECT current_active_controlled_file_id FROM dcc_controlled_file_master WHERE id = 10"));
        assertEquals(0L, longValue("SELECT COUNT(*) FROM dcc_publication_followup_batch"));
        assertEquals(0L, longValue("SELECT COUNT(*) FROM dcc_publication_visibility_rule_snapshot"));
        assertEquals(0L, longValue("SELECT COUNT(*) FROM dcc_publication_visibility_user_snapshot"));
        assertEquals(0L, longValue("SELECT COUNT(*) FROM dcc_publication_notification_candidate"));
        assertEquals(0L, longValue("SELECT COUNT(*) FROM dcc_publication_notification_candidate_reason"));
        assertEquals(0L, longValue("SELECT COUNT(*) FROM dcc_publication_relation_snapshot"));
        assertEquals(0L, longValue("SELECT COUNT(*) FROM dcc_publication_relation_direction_snapshot"));
        verify(platformAdapter, never()).recordFinalized(any(), any(), any(), any());
    }

    private void seedPublicationRows() {
        jdbcTemplate.update("""
                INSERT INTO dcc_file_category
                  (id, code, name, active, sort, source, lifecycle_stage, distribution_required,
                   training_required, tenant_id, deleted)
                VALUES (20, 'TX-SOP', '事务规范', 1, 0, 'MANUAL', 'ACTIVE', 0, 0, 1, 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO dcc_controlled_file_master
                  (id, category_id, directory_id, file_name, file_number, dcc_project_code_id,
                   file_type_taxonomy_leaf_id, normalized_file_number, current_active_controlled_file_id,
                   status, tenant_id, deleted)
                VALUES (10, 20, 30, '事务文件', 'TX-100', 40, 50, 'TX-100', 99, 'ACTIVE_CHAIN', 1, 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO dcc_controlled_file
                  (id, master_id, category_id, directory_id, source_file_id, original_file_id,
                   published_file_id, stamped_file_id, file_name, title, file_number, dcc_project_code_id,
                   file_type_taxonomy_id, need_training, process_type, change_type, version_no, revision_code,
                   iteration_no, status, submitter_id, requester_id, published_time, tenant_id, deleted)
                VALUES
                  (99, 10, 20, 30, 1000, 1000, 1000, 1000, '事务文件', '事务文件', 'TX-100', 40,
                   50, 0, 'CONTROLLED_FILE', 'NEW', 'A/1', 'A', 1, 'ACTIVE', 1, 1,
                   TIMESTAMP '2026-09-07 10:00:00', 1, 0),
                  (100, 10, 20, 30, 1001, 1001, 1001, 1001, '事务文件', '事务文件', 'TX-100', 40,
                   50, 0, 'CONTROLLED_FILE', 'REVISION', 'B/1', 'B', 1, 'READY_TO_PUBLISH', 1, 1,
                   NULL, 1, 0)
                """);
    }

    private String stringValue(String sql) {
        return jdbcTemplate.queryForObject(sql, String.class);
    }

    private long longValue(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }

    private void set(Object target, String field, Object value) {
        ReflectionTestUtils.setField(target, field, value);
    }

    private static final class CapturingTransactionTemplate extends TransactionTemplate {

        private final AtomicReference<RuntimeException> capturedFailure;

        private CapturingTransactionTemplate(PlatformTransactionManager transactionManager,
                                             AtomicReference<RuntimeException> capturedFailure) {
            super(transactionManager);
            this.capturedFailure = capturedFailure;
        }

        @Override
        public void executeWithoutResult(Consumer<TransactionStatus> action) {
            try {
                super.executeWithoutResult(action);
            } catch (RuntimeException ex) {
                capturedFailure.compareAndSet(null, ex);
                throw ex;
            }
        }
    }
}
