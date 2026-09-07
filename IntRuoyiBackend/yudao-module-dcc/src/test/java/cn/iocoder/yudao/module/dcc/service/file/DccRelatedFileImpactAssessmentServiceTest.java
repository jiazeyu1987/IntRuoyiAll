package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationFollowupBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationImpactAuditDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationImpactTaskDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationRelationSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationFollowupBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationImpactAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationImpactTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationRelationSnapshotMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileChangeTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_IMPACT_ASSIGNEE_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_IMPACT_DECISION_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_IMPACT_DOC_CONTROL_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_IMPACT_REASON_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_IMPACT_REVISION_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_IMPACT_VERSION_CONFLICT;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DccRelatedFileImpactAssessmentServiceTest extends BaseMockitoUnitTest {

    @Mock private DccPublicationFollowupBatchMapper batchMapper;
    @Mock private DccPublicationRelationSnapshotMapper relationMapper;
    @Mock private DccPublicationImpactTaskMapper taskMapper;
    @Mock private DccPublicationImpactAuditMapper auditMapper;
    @Mock private AdminUserApi adminUserApi;
    @Mock private PermissionApi permissionApi;
    @Mock private DccControlledFileMapper controlledFileMapper;

    @InjectMocks
    private DccRelatedFileImpactAssessmentServiceImpl service;

    private final Map<Long, DccPublicationImpactTaskDO> tasksByMaster = new LinkedHashMap<>();
    private final AtomicLong taskIds = new AtomicLong(1000L);

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        lenient().doAnswer(invocation -> {
            DccPublicationImpactTaskDO proposed = invocation.getArgument(0);
            tasksByMaster.computeIfAbsent(proposed.getRelatedMasterId(), ignored -> {
                proposed.setId(taskIds.getAndIncrement());
                return proposed;
            });
            return 1;
        }).when(taskMapper).insertOrKeepExisting(any(DccPublicationImpactTaskDO.class));
        lenient().when(taskMapper.selectByBatchIdAndRelatedMasterId(any(), any(), any()))
                .thenAnswer(invocation -> tasksByMaster.get(invocation.getArgument(2)));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void materializeForPublicationBatch_createsOneTaskPerRelatedMasterAndAssignsOnlyActiveRequester() {
        when(batchMapper.selectById(77L)).thenReturn(batch(77L, 100L));
        when(relationMapper.selectListByBatchId(77L)).thenReturn(List.of(
                relation(701L, 20L, 200L, 3L, 0),
                relation(702L, 30L, null, null, null),
                relation(703L, 40L, 400L, 4L, 1)));

        service.materializeForPublicationBatch(77L);

        ArgumentCaptor<DccPublicationImpactTaskDO> captor =
                ArgumentCaptor.forClass(DccPublicationImpactTaskDO.class);
        verify(taskMapper, times(3)).insertOrKeepExisting(captor.capture());
        assertEquals(List.of("PENDING", "UNASSIGNED", "UNASSIGNED"), captor.getAllValues().stream()
                .map(DccPublicationImpactTaskDO::getTaskStatus).toList());
        assertEquals(3L, captor.getAllValues().get(0).getAssigneeUserId());
        assertEquals(null, captor.getAllValues().get(1).getAssigneeUserId());
        assertEquals(null, captor.getAllValues().get(2).getAssigneeUserId());
        assertEquals(List.of(20L, 30L, 40L), captor.getAllValues().stream()
                .map(DccPublicationImpactTaskDO::getRelatedMasterId).toList());
        verify(auditMapper, times(3)).insert(any(DccPublicationImpactAuditDO.class));
        verifyNoInteractions(adminUserApi);
    }

    @Test
    void materializeForPublicationBatch_duplicateCallDoesNotDuplicateAudit() {
        when(batchMapper.selectById(77L)).thenReturn(batch(77L, 100L));
        when(relationMapper.selectListByBatchId(77L)).thenReturn(List.of(
                relation(701L, 20L, 200L, 3L, 0)));

        service.materializeForPublicationBatch(77L);
        service.materializeForPublicationBatch(77L);

        verify(taskMapper, times(1)).insertOrKeepExisting(any(DccPublicationImpactTaskDO.class));
        verify(auditMapper, times(1)).insert(any(DccPublicationImpactAuditDO.class));
    }

    @Test
    void materializeForPublicationBatch_lostUniqueInsertRaceDoesNotDuplicateMaterializeAudit() {
        when(batchMapper.selectById(77L)).thenReturn(batch(77L, 100L));
        DccPublicationRelationSnapshotDO relation = relation(701L, 20L, 200L, 3L, 0);
        when(relationMapper.selectListByBatchId(77L)).thenReturn(List.of(relation));
        DccPublicationImpactTaskDO winner = task(1001L, "PENDING", 3L, 0);
        winner.setCreationToken("winner-token");
        when(taskMapper.selectByBatchIdAndRelatedMasterId(1L, 77L, 20L))
                .thenReturn(null, winner);

        service.materializeForPublicationBatch(77L);

        verify(taskMapper).insertOrKeepExisting(any(DccPublicationImpactTaskDO.class));
        verifyNoInteractions(auditMapper, adminUserApi);
    }

    @Test
    void startTask_assigneeWithMatchingVersionMovesToInReviewAndAudits() {
        DccPublicationImpactTaskDO task = task(10L, "PENDING", 99L, 0);
        when(taskMapper.selectByIdAndTenant(1L, 10L)).thenReturn(task);
        when(taskMapper.startTask(1L, 10L, 0, 99L)).thenReturn(1);

        service.startTask(99L, 10L, 0);

        verify(taskMapper).startTask(1L, 10L, 0, 99L);
        verify(auditMapper).insert(any(DccPublicationImpactAuditDO.class));
    }

    @Test
    void startTask_nonAssigneeHasZeroSideEffects() {
        when(taskMapper.selectByIdAndTenant(1L, 10L)).thenReturn(task(10L, "PENDING", 99L, 0));

        assertServiceException(() -> service.startTask(98L, 10L, 0), PUBLICATION_IMPACT_ASSIGNEE_DENIED);

        verify(taskMapper, never()).startTask(any(), any(), any(), any());
        verifyNoInteractions(auditMapper);
    }

    @Test
    void submitDecision_noRevisionCompletesWithoutChangingControlledFiles() {
        when(taskMapper.selectByIdAndTenant(1L, 10L)).thenReturn(task(10L, "IN_REVIEW", 99L, 1));
        when(taskMapper.completeDecision(1L, 10L, 1, 99L, "NO_REVISION_REQUIRED",
                "接口未受影响", "NOT_APPLICABLE")).thenReturn(1);

        service.submitDecision(99L, 10L, 1, "NO_REVISION_REQUIRED", "接口未受影响");

        verify(taskMapper).completeDecision(1L, 10L, 1, 99L, "NO_REVISION_REQUIRED",
                "接口未受影响", "NOT_APPLICABLE");
        verifyNoInteractions(controlledFileMapper);
        verify(auditMapper).insert(any(DccPublicationImpactAuditDO.class));
    }

    @Test
    void submitDecision_revisionRequiredOnlyMarksNotStartedAndDoesNotCreateRevision() {
        when(taskMapper.selectByIdAndTenant(1L, 10L)).thenReturn(task(10L, "IN_REVIEW", 99L, 1));
        when(taskMapper.completeDecision(1L, 10L, 1, 99L, "REVISION_REQUIRED",
                "需要同步接口", "NOT_STARTED")).thenReturn(1);

        service.submitDecision(99L, 10L, 1, "REVISION_REQUIRED", "需要同步接口");

        verify(taskMapper).completeDecision(1L, 10L, 1, 99L, "REVISION_REQUIRED",
                "需要同步接口", "NOT_STARTED");
        verifyNoInteractions(controlledFileMapper);
    }

    @Test
    void submitDecision_invalidDecisionBlankReasonAndStaleVersionHaveZeroSideEffects() {
        DccPublicationImpactTaskDO task = task(10L, "IN_REVIEW", 99L, 1);
        when(taskMapper.selectByIdAndTenant(1L, 10L)).thenReturn(task);

        assertServiceException(() -> service.submitDecision(99L, 10L, 1, "MAYBE", "原因"),
                PUBLICATION_IMPACT_DECISION_INVALID);
        assertServiceException(() -> service.submitDecision(99L, 10L, 1, "NO_REVISION_REQUIRED", " "),
                PUBLICATION_IMPACT_REASON_REQUIRED);
        when(taskMapper.completeDecision(1L, 10L, 1, 99L, "NO_REVISION_REQUIRED",
                "无影响", "NOT_APPLICABLE")).thenReturn(0);
        assertServiceException(() -> service.submitDecision(99L, 10L, 1,
                "NO_REVISION_REQUIRED", "无影响"), PUBLICATION_IMPACT_VERSION_CONFLICT);

        verifyNoInteractions(auditMapper, controlledFileMapper);
    }

    @Test
    void reassignTask_requiresDocControlRoleAndApprovePermissionAndActiveTarget() {
        DccPublicationImpactTaskDO task = task(10L, "UNASSIGNED", null, 0);
        when(taskMapper.selectByIdAndTenant(1L, 10L)).thenReturn(task);
        when(permissionApi.hasAnyRoles(9L, "doc_control")).thenReturn(true);
        when(permissionApi.hasAnyPermissions(9L, "dcc:controlled-file:approve")).thenReturn(true);
        when(adminUserApi.getUser(88L)).thenReturn(new AdminUserRespDTO().setId(88L).setNickname("新负责人").setStatus(0));
        when(taskMapper.reassignTask(1L, 10L, 0, 88L, "新负责人", "职责调整")).thenReturn(1);

        service.reassignTask(9L, 10L, 0, 88L, "职责调整");

        verify(taskMapper).reassignTask(1L, 10L, 0, 88L, "新负责人", "职责调整");
        verify(auditMapper).insert(any(DccPublicationImpactAuditDO.class));
    }

    @Test
    void reassignTask_missingDocControlPermissionFailsBeforeUserLookup() {
        when(permissionApi.hasAnyRoles(9L, "doc_control")).thenReturn(true);
        when(permissionApi.hasAnyPermissions(9L, "dcc:controlled-file:approve")).thenReturn(false);

        assertServiceException(() -> service.reassignTask(9L, 10L, 0, 88L, "职责调整"),
                PUBLICATION_IMPACT_DOC_CONTROL_DENIED);

        verifyNoInteractions(adminUserApi, auditMapper);
        verify(taskMapper, never()).reassignTask(any(), any(), any(), any(), any(), any());
    }

    @Test
    void reassignTask_disabledTargetHasZeroSideEffects() {
        when(permissionApi.hasAnyRoles(9L, "doc_control")).thenReturn(true);
        when(permissionApi.hasAnyPermissions(9L, "dcc:controlled-file:approve")).thenReturn(true);
        when(taskMapper.selectByIdAndTenant(1L, 10L)).thenReturn(task(10L, "UNASSIGNED", null, 0));
        when(adminUserApi.getUser(88L)).thenReturn(new AdminUserRespDTO().setId(88L).setStatus(1));

        assertServiceException(() -> service.reassignTask(9L, 10L, 0, 88L, "职责调整"),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_IMPACT_ASSIGNEE_INVALID);

        verify(taskMapper, never()).reassignTask(any(), any(), any(), any(), any(), any());
        verifyNoInteractions(auditMapper);
    }

    @Test
    void reopenTask_docControlClearsCurrentDecisionButKeepsImmutableAudit() {
        DccPublicationImpactTaskDO task = task(10L, "COMPLETED", 99L, 2);
        task.setDecision("NO_REVISION_REQUIRED");
        when(taskMapper.selectByIdAndTenant(1L, 10L)).thenReturn(task);
        when(permissionApi.hasAnyRoles(9L, "doc_control")).thenReturn(true);
        when(permissionApi.hasAnyPermissions(9L, "dcc:controlled-file:approve")).thenReturn(true);
        when(taskMapper.reopenTask(1L, 10L, 2, "结论录入错误")).thenReturn(1);

        service.reopenTask(9L, 10L, 2, "结论录入错误");

        verify(taskMapper).reopenTask(1L, 10L, 2, "结论录入错误");
        ArgumentCaptor<DccPublicationImpactAuditDO> auditCaptor =
                ArgumentCaptor.forClass(DccPublicationImpactAuditDO.class);
        verify(auditMapper).insert(auditCaptor.capture());
        assertEquals("NO_REVISION_REQUIRED", auditCaptor.getValue().getDecisionSnapshot());
    }

    @Test
    void linkExistingMajorRevision_requiresSameMasterOwnerAndUniqueOpenRevision() {
        DccPublicationImpactTaskDO task = revisionRequiredTask(10L, 99L, 2);
        when(taskMapper.selectByIdAndTenant(1L, 10L)).thenReturn(task);
        DccControlledFileDO revision = openRevision(500L, 20L, 99L);
        when(controlledFileMapper.selectById(500L)).thenReturn(revision);
        when(controlledFileMapper.selectListByMasterId(20L)).thenReturn(List.of(revision));
        when(taskMapper.linkRevision(1L, 10L, 2, 500L, "B/1", "关联现有修订")).thenReturn(1);

        service.linkExistingMajorRevision(99L, 10L, 2, 500L, "关联现有修订");

        verify(taskMapper).linkRevision(1L, 10L, 2, 500L, "B/1", "关联现有修订");
        verify(auditMapper).insert(any(DccPublicationImpactAuditDO.class));
    }

    @Test
    void linkExistingMajorRevision_crossMasterFailsWithoutUpdate() {
        when(taskMapper.selectByIdAndTenant(1L, 10L)).thenReturn(revisionRequiredTask(10L, 99L, 2));
        when(controlledFileMapper.selectById(500L)).thenReturn(openRevision(500L, 30L, 99L));

        assertServiceException(() -> service.linkExistingMajorRevision(99L, 10L, 2, 500L, "关联"),
                PUBLICATION_IMPACT_REVISION_INVALID);

        verify(taskMapper, never()).linkRevision(any(), any(), any(), any(), any(), any());
        verifyNoInteractions(auditMapper);
    }

    @Test
    void linkExistingMajorRevision_activeOrFailedVersionIsNotAnOpenRevision() {
        when(taskMapper.selectByIdAndTenant(1L, 10L)).thenReturn(revisionRequiredTask(10L, 99L, 2));
        DccControlledFileDO active = openRevision(500L, 20L, 99L);
        active.setStatus(DccControlledFileStatusEnum.ACTIVE.getStatus());
        DccControlledFileDO failed = openRevision(501L, 20L, 99L);
        failed.setStatus(DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus());
        when(controlledFileMapper.selectById(500L)).thenReturn(active);
        when(controlledFileMapper.selectById(501L)).thenReturn(failed);

        assertServiceException(() -> service.linkExistingMajorRevision(99L, 10L, 2, 500L, "关联"),
                PUBLICATION_IMPACT_REVISION_INVALID);
        assertServiceException(() -> service.linkExistingMajorRevision(99L, 10L, 2, 501L, "关联"),
                PUBLICATION_IMPACT_REVISION_INVALID);

        verify(taskMapper, never()).linkRevision(any(), any(), any(), any(), any(), any());
        verifyNoInteractions(auditMapper);
    }

    @Test
    void assertRevisionCreationAllowed_requiresMatchingTaskVersionMasterAndRequester() {
        DccPublicationImpactTaskDO task = revisionRequiredTask(10L, 99L, 2);
        when(taskMapper.selectByIdAndTenant(1L, 10L)).thenReturn(task);
        when(controlledFileMapper.selectById(200L)).thenReturn(DccControlledFileDO.builder()
                .id(200L).masterId(20L).requesterId(99L).build());

        service.assertRevisionCreationAllowed(99L, 10L, 2, 200L, "同步关联文件");

        assertServiceException(() -> service.assertRevisionCreationAllowed(
                99L, 10L, 1, 200L, "同步关联文件"), PUBLICATION_IMPACT_VERSION_CONFLICT);
        verifyNoInteractions(auditMapper);
    }

    @Test
    void resolveLinkedRevisionAfterPublication_onlyResolvesConcreteActiveRevision() {
        DccPublicationImpactTaskDO task = revisionRequiredTask(10L, 99L, 3);
        task.setLinkedRevisionControlledFileId(500L);
        task.setRevisionTrackingStatus("REVISION_LINKED");
        when(taskMapper.selectListByLinkedRevisionId(1L, 500L)).thenReturn(List.of(task));
        when(taskMapper.resolveRevision(1L, 10L, 3, 500L)).thenReturn(1);

        service.resolveLinkedRevisionAfterPublication(DccControlledFileDO.builder()
                .id(500L).masterId(20L).status(DccControlledFileStatusEnum.ACTIVE.getStatus()).build());

        verify(taskMapper).resolveRevision(1L, 10L, 3, 500L);
        verify(auditMapper).insert(any(DccPublicationImpactAuditDO.class));
    }

    @Test
    void resolveLinkedRevisionAfterPublication_concurrentResolverWinnerDoesNotThrowOrDuplicateAudit() {
        DccPublicationImpactTaskDO selected = revisionRequiredTask(10L, 99L, 3);
        selected.setLinkedRevisionControlledFileId(500L);
        selected.setRevisionTrackingStatus("REVISION_LINKED");
        DccPublicationImpactTaskDO current = revisionRequiredTask(10L, 99L, 4);
        current.setLinkedRevisionControlledFileId(500L);
        current.setRevisionTrackingStatus("RESOLVED");
        when(taskMapper.selectListByLinkedRevisionId(1L, 500L)).thenReturn(List.of(selected));
        when(taskMapper.resolveRevision(1L, 10L, 3, 500L)).thenReturn(0);
        when(taskMapper.selectByIdAndTenantForUpdate(1L, 10L)).thenReturn(current);

        assertDoesNotThrow(() -> service.resolveLinkedRevisionAfterPublication(activeRevision(500L, 20L)));

        verifyNoInteractions(auditMapper);
    }

    @Test
    void resolveLinkedRevisionAfterPublication_concurrentReopenDoesNotThrowOrWriteAudit() {
        DccPublicationImpactTaskDO selected = revisionRequiredTask(10L, 99L, 3);
        selected.setLinkedRevisionControlledFileId(500L);
        selected.setRevisionTrackingStatus("REVISION_LINKED");
        DccPublicationImpactTaskDO reopened = task(10L, "PENDING", 99L, 4);
        reopened.setLinkedRevisionControlledFileId(null);
        reopened.setDecision(null);
        when(taskMapper.selectListByLinkedRevisionId(1L, 500L)).thenReturn(List.of(selected));
        when(taskMapper.resolveRevision(1L, 10L, 3, 500L)).thenReturn(0);
        when(taskMapper.selectByIdAndTenantForUpdate(1L, 10L)).thenReturn(reopened);

        assertDoesNotThrow(() -> service.resolveLinkedRevisionAfterPublication(activeRevision(500L, 20L)));

        verifyNoInteractions(auditMapper);
    }

    @Test
    void resolveLinkedRevisionAfterPublication_unexplainedCasMissStillFailsFast() {
        DccPublicationImpactTaskDO selected = revisionRequiredTask(10L, 99L, 3);
        selected.setLinkedRevisionControlledFileId(500L);
        selected.setRevisionTrackingStatus("REVISION_LINKED");
        DccPublicationImpactTaskDO stillLinked = revisionRequiredTask(10L, 99L, 4);
        stillLinked.setLinkedRevisionControlledFileId(500L);
        stillLinked.setRevisionTrackingStatus("REVISION_LINKED");
        when(taskMapper.selectListByLinkedRevisionId(1L, 500L)).thenReturn(List.of(selected));
        when(taskMapper.resolveRevision(1L, 10L, 3, 500L)).thenReturn(0);
        when(taskMapper.selectByIdAndTenantForUpdate(1L, 10L)).thenReturn(stillLinked);

        assertServiceException(() -> service.resolveLinkedRevisionAfterPublication(activeRevision(500L, 20L)),
                PUBLICATION_IMPACT_VERSION_CONFLICT);

        verifyNoInteractions(auditMapper);
    }

    @Test
    void resolveLinkedRevisionAfterPublication_failedOrRejectedRevisionDoesNothing() {
        service.resolveLinkedRevisionAfterPublication(DccControlledFileDO.builder()
                .id(500L).masterId(20L).status(DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus()).build());

        verify(taskMapper, never()).selectListByLinkedRevisionId(any(), any());
        verifyNoInteractions(auditMapper);
    }

    private DccPublicationFollowupBatchDO batch(Long id, Long publishedFileId) {
        DccPublicationFollowupBatchDO batch = DccPublicationFollowupBatchDO.builder()
                .id(id).publishedControlledFileId(publishedFileId).publishedMasterId(10L).build();
        batch.setTenantId(1L);
        return batch;
    }

    private DccPublicationRelationSnapshotDO relation(Long id, Long masterId, Long activeFileId,
                                                       Long requesterId, Integer requesterStatus) {
        DccPublicationRelationSnapshotDO relation = DccPublicationRelationSnapshotDO.builder()
                .id(id).batchId(77L).relatedMasterId(masterId).relatedActiveControlledFileId(activeFileId)
                .relatedFileNumberSnapshot("REL-" + masterId).relatedFileNameSnapshot("关联文件" + masterId)
                .relatedVersionNoSnapshot(activeFileId == null ? null : "A/1")
                .responsibleUserIdSnapshot(requesterId).responsibleUserNameSnapshot("负责人" + requesterId)
                .responsibleUserStatusSnapshot(requesterStatus).build();
        relation.setTenantId(1L);
        return relation;
    }

    private DccPublicationImpactTaskDO task(Long id, String status, Long assignee, int version) {
        DccPublicationImpactTaskDO task = DccPublicationImpactTaskDO.builder()
                .id(id).batchId(77L).publicationRelationSnapshotId(701L).publishedControlledFileId(100L)
                .relatedMasterId(20L).relatedActiveControlledFileId(200L).taskStatus(status)
                .assigneeUserId(assignee).revisionTrackingStatus("NOT_APPLICABLE").rowVersion(version).build();
        task.setTenantId(1L);
        return task;
    }

    private DccPublicationImpactTaskDO revisionRequiredTask(Long id, Long assignee, int version) {
        DccPublicationImpactTaskDO task = task(id, "COMPLETED", assignee, version);
        task.setDecision("REVISION_REQUIRED");
        task.setDecisionReason("需要同步");
        task.setRevisionTrackingStatus("NOT_STARTED");
        return task;
    }

    private DccControlledFileDO openRevision(Long id, Long masterId, Long requesterId) {
        return DccControlledFileDO.builder().id(id).masterId(masterId).requesterId(requesterId)
                .versionNo("B/1").changeType(DccControlledFileChangeTypeEnum.REVISION.getCode())
                .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus()).build();
    }

    private DccControlledFileDO activeRevision(Long id, Long masterId) {
        return DccControlledFileDO.builder().id(id).masterId(masterId)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus()).build();
    }
}
