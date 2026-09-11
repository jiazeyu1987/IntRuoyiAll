package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationFollowupBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationCandidateReasonDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationDeliveryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationCandidateDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationVisibilityRuleSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationVisibilityUserSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationImpactTaskDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationRelationDirectionSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationFollowupBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationCandidateReasonMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationDeliveryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationImpactTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationVisibilityRuleSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationVisibilityUserSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationCandidateMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationRelationDirectionSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationImpactAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationAuditDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationImpactAuditDO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationFollowupPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationImpactTaskPageReqVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_NOTIFICATION_MANAGE_DENIED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class DccPublicationFollowupQueryServiceTest extends BaseMockitoUnitTest {

    @Mock private DccControlledFileQueryService controlledFileQueryService;
    @Mock private DccPublicationFollowupBatchMapper batchMapper;
    @Mock private DccPublicationNotificationDeliveryMapper deliveryMapper;
    @Mock private DccPublicationNotificationCandidateReasonMapper reasonMapper;
    @Mock private DccPublicationVisibilityRuleSnapshotMapper visibilityRuleMapper;
    @Mock private DccPublicationImpactTaskMapper impactTaskMapper;
    @Mock private DccPublicationVisibilityUserSnapshotMapper visibilityUserMapper;
    @Mock private DccPublicationNotificationCandidateMapper candidateMapper;
    @Mock private DccPublicationRelationDirectionSnapshotMapper directionMapper;
    @Mock private DccPublicationNotificationAuditMapper notificationAuditMapper;
    @Mock private DccPublicationImpactAuditMapper impactAuditMapper;
    @Mock private DccControlledFileMapper controlledFileMapper;
    @Mock private PermissionApi permissionApi;
    @InjectMocks private DccPublicationFollowupQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void getFileFollowup_reusesCurrentDetailAuthorizationAndAggregatesReasonsOnBackend() {
        when(batchMapper.selectLatestByPublishedControlledFileId(1L, 70L)).thenReturn(
                DccPublicationFollowupBatchDO.builder().id(7L).publishedControlledFileId(70L)
                        .fileNumberSnapshot("DOC-70").versionNoSnapshot("B/1")
                        .publishedAt(LocalDateTime.of(2026, 9, 7, 10, 0)).build());
        when(deliveryMapper.selectListByBatchIds(1L, List.of(7L))).thenReturn(List.of(
                DccPublicationNotificationDeliveryDO.builder().id(10L).batchId(7L).candidateId(11L)
                        .userId(21L).status("SENT").rowVersion(2).build()));
        when(candidateMapper.selectListByIds(1L, List.of(11L))).thenReturn(List.of(
                DccPublicationNotificationCandidateDO.builder().id(11L).userId(21L)
                        .userNameSnapshot("收件人").deptNameSnapshot("质量部").resolutionStatus("ACTIVE").build()));
        when(reasonMapper.selectListByCandidateIds(1L, List.of(11L))).thenReturn(List.of(
                DccPublicationNotificationCandidateReasonDO.builder().candidateId(11L)
                        .reasonType("FILE_OWNER").reasonSummary("发布文件责任人").build(),
                DccPublicationNotificationCandidateReasonDO.builder().candidateId(11L)
                        .reasonType("FORMAL_DISTRIBUTION").reasonSummary("正式分发对象").build()));
        when(visibilityRuleMapper.selectListByBatchIds(1L, List.of(7L))).thenReturn(List.of(
                DccPublicationVisibilityRuleSnapshotDO.builder().id(31L).batchId(7L)
                        .sourceType("CURRENT_VIEW_MATRIX").sourceRuleId(301L).sourceSummary("研发角色")
                        .resolutionStatus("RESOLVED").build()));
        when(visibilityUserMapper.selectListByRuleIds(1L, List.of(31L))).thenReturn(List.of(
                DccPublicationVisibilityUserSnapshotDO.builder().ruleSnapshotId(31L).userId(21L)
                        .userNameSnapshot("收件人").deptNameSnapshot("质量部").userStatusSnapshot(0)
                        .assignmentScopeResult("ALLOWED").build()));
        when(impactTaskMapper.selectListByBatchIds(1L, List.of(7L))).thenReturn(List.of(
                DccPublicationImpactTaskDO.builder().id(41L).batchId(7L).publicationRelationSnapshotId(51L)
                        .publishedControlledFileId(70L).relatedMasterId(71L).taskStatus("PENDING")
                        .revisionTrackingStatus("NOT_APPLICABLE").rowVersion(0).build()));
        when(directionMapper.selectListByRelationSnapshotIds(1L, List.of(51L))).thenReturn(List.of(
                DccPublicationRelationDirectionSnapshotDO.builder().relationSnapshotId(51L).direction("FORWARD").build(),
                DccPublicationRelationDirectionSnapshotDO.builder().relationSnapshotId(51L).direction("REVERSE").build()));
        when(notificationAuditMapper.selectListByBatchIds(1L, List.of(7L))).thenReturn(List.of(
                DccPublicationNotificationAuditDO.builder().id(58L).deliveryId(10L).batchId(7L)
                        .actionType("ATTEMPT").statusBefore("PENDING").statusAfter("PENDING")
                        .attemptCount(1).occurredAt(LocalDateTime.of(2026, 9, 7, 10, 0, 30)).build(),
                DccPublicationNotificationAuditDO.builder().id(59L).deliveryId(10L).batchId(7L)
                        .actionType("RETRY").statusBefore("FAILED").statusAfter("FAILED")
                        .attemptCount(2).occurredAt(LocalDateTime.of(2026, 9, 7, 10, 1)).build(),
                DccPublicationNotificationAuditDO.builder().id(60L).deliveryId(10L).batchId(7L)
                        .actionType("FAILED").statusBefore("PENDING").statusAfter("FAILED")
                        .attemptCount(2).occurredAt(LocalDateTime.of(2026, 9, 7, 10, 1, 30)).build(),
                DccPublicationNotificationAuditDO.builder().id(61L).deliveryId(10L).batchId(7L)
                        .actionType("SENT").statusBefore("PENDING").statusAfter("SENT")
                        .attemptCount(2).systemMessageId(9007199254740993L)
                        .occurredAt(LocalDateTime.of(2026, 9, 7, 10, 3)).build()));
        when(impactAuditMapper.selectListByBatchIds(1L, List.of(7L))).thenReturn(List.of(
                DccPublicationImpactAuditDO.builder().id(62L).taskId(41L).batchId(7L)
                        .actionType("START").statusBefore("PENDING").statusAfter("IN_REVIEW")
                        .occurredAt(LocalDateTime.of(2026, 9, 7, 10, 2)).build()));

        var result = service.getFileFollowup(99L, 70L);

        verify(controlledFileQueryService).getControlledFile(99L, 70L);
        assertEquals(List.of("发布文件责任人", "正式分发对象"),
                result.getNotificationDeliveries().get(0).getReasonSummaries());
        assertEquals("收件人", result.getNotificationDeliveries().get(0).getUserName());
        assertEquals("质量部", result.getNotificationDeliveries().get(0).getDeptName());
        assertEquals("收件人", result.getVisibilityRules().get(0).getUsers().get(0).getUserName());
        assertEquals(List.of("FORWARD", "REVERSE"), result.getImpactTasks().get(0).getRelationDirections());
        assertEquals(List.of("BATCH", "NOTIFICATION", "NOTIFICATION", "NOTIFICATION", "IMPACT", "NOTIFICATION"),
                result.getTimeline().stream()
                .map(cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationTimelineEventRespVO::getSourceType)
                .toList());
        assertEquals(List.of(1, 2, 2, 2), List.of(
                result.getTimeline().get(1).getAttemptCount(), result.getTimeline().get(2).getAttemptCount(),
                result.getTimeline().get(3).getAttemptCount(), result.getTimeline().get(5).getAttemptCount()));
        assertEquals("开始影响评估", result.getTimeline().get(4).getActionLabel());
        assertEquals("评估中", result.getTimeline().get(4).getStatusAfterLabel());
        assertNull(result.getTimeline().get(4).getAttemptCount());
        assertNull(result.getTimeline().get(4).getAssigneeBefore());
        assertNull(result.getTimeline().get(4).getAssigneeAfter());
        assertNull(result.getTimeline().get(4).getLinkedRevisionControlledFileId());
        assertNull(result.getTimeline().get(4).getLinkedRevisionVersion());
        assertEquals("9007199254740993", result.getTimeline().get(5).getSystemMessageId());
        assertNull(result.getTimeline().get(5).getAssigneeBefore());
        assertNull(result.getTimeline().get(5).getAssigneeAfter());
        assertNull(result.getTimeline().get(5).getLinkedRevisionControlledFileId());
        assertNull(result.getTimeline().get(5).getLinkedRevisionVersion());
    }

    @Test
    void getManagementPage_serviceDoubleChecksRoleAndPermission() {
        when(permissionApi.hasAnyRoles(9L, "doc_control")).thenReturn(true);
        when(permissionApi.hasAnyPermissions(9L, "dcc:controlled-file:approve")).thenReturn(false);

        assertServiceException(() -> service.getManagementPage(9L, null),
                PUBLICATION_NOTIFICATION_MANAGE_DENIED);
    }

    @Test
    void getFileFollowup_deniedCurrentViewDoesNotReadTimelineAudits() {
        when(controlledFileQueryService.getControlledFile(98L, 70L)).thenThrow(
                new cn.iocoder.yudao.framework.common.exception.ServiceException(403, "当前用户无权查看"));

        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.getFileFollowup(98L, 70L));

        verifyNoInteractions(notificationAuditMapper, impactAuditMapper);
    }

    @Test
    void getFileFollowup_linkRevisionTimelineUsesAuditLinkedVersionAfterTaskReopened() {
        when(batchMapper.selectLatestByPublishedControlledFileId(1L, 70L)).thenReturn(
                DccPublicationFollowupBatchDO.builder().id(7L).publishedControlledFileId(70L)
                        .fileNumberSnapshot("DOC-70").versionNoSnapshot("B/1")
                        .publishedAt(LocalDateTime.of(2026, 9, 7, 10, 0)).build());
        when(deliveryMapper.selectListByBatchIds(1L, List.of(7L))).thenReturn(List.of());
        when(visibilityRuleMapper.selectListByBatchIds(1L, List.of(7L))).thenReturn(List.of());
        DccPublicationImpactTaskDO reopenedTask = DccPublicationImpactTaskDO.builder()
                .id(41L).batchId(7L).publicationRelationSnapshotId(51L)
                .publishedControlledFileId(70L).relatedMasterId(71L)
                .relatedFileNumberSnapshot("REL-71").relatedFileNameSnapshot("关联文件")
                .taskStatus("IN_REVIEW").revisionTrackingStatus("TRACKING_NOT_STARTED")
                .linkedRevisionControlledFileId(null).linkedRevisionVersionSnapshot(null).rowVersion(3).build();
        when(impactTaskMapper.selectListByBatchIds(1L, List.of(7L))).thenReturn(List.of(reopenedTask));
        when(directionMapper.selectListByRelationSnapshotIds(1L, List.of(51L))).thenReturn(List.of());
        when(notificationAuditMapper.selectListByBatchIds(1L, List.of(7L))).thenReturn(List.of());
        when(impactAuditMapper.selectListByBatchIds(1L, List.of(7L))).thenReturn(List.of(
                DccPublicationImpactAuditDO.builder().id(62L).taskId(41L).batchId(7L)
                        .actionType("LINK_REVISION").actorId(99L)
                        .statusBefore("COMPLETED").statusAfter("COMPLETED")
                        .linkedRevisionControlledFileId(500L)
                        .occurredAt(LocalDateTime.of(2026, 9, 7, 10, 2)).build()));
        when(controlledFileMapper.selectBatchIds(List.of(500L))).thenReturn(List.of(
                DccControlledFileDO.builder().id(500L).versionNo("C/1").build()));

        var result = service.getFileFollowup(99L, 70L);

        assertEquals("500", result.getTimeline().get(1).getLinkedRevisionControlledFileId());
        assertEquals("C/1", result.getTimeline().get(1).getLinkedRevisionVersion());
    }

    @Test
    void getManagementPage_usesDatabasePageAndAllApprovedFilters() {
        when(permissionApi.hasAnyRoles(9L, "doc_control")).thenReturn(true);
        when(permissionApi.hasAnyPermissions(9L, "dcc:controlled-file:approve")).thenReturn(true);
        when(permissionApi.hasAnyPermissions(9L, "dcc:controlled-file:publication-followup:manage")).thenReturn(true);
        DccPublicationFollowupPageReqVO req = new DccPublicationFollowupPageReqVO();
        req.setPageNo(2); req.setPageSize(20); req.setFileNumber("DOC"); req.setVersionNo("B/1");
        req.setBatchStatus("PARTIAL_FAILED"); req.setTaskStatus("PENDING");
        req.setNotificationStatus("FAILED"); req.setAssigneeUserId("9007199254740993");
        Page<DccPublicationFollowupBatchDO> page = new Page<>(2, 20, 1);
        page.setRecords(List.of());
        when(batchMapper.selectManagementPage(any(), eq(1L), eq("DOC"), eq("B/1"),
                eq("PARTIAL_FAILED"), eq("PENDING"), eq("FAILED"), eq(9007199254740993L)))
                .thenReturn(page);

        service.getManagementPage(9L, req);

        verify(batchMapper).selectManagementPage(any(), eq(1L), eq("DOC"), eq("B/1"),
                eq("PARTIAL_FAILED"), eq("PENDING"), eq("FAILED"), eq(9007199254740993L));
        verify(batchMapper, never()).selectList();
    }

    @Test
    void getManagementPage_rejectsOverflowAssigneeBeforeMapper() {
        when(permissionApi.hasAnyRoles(9L, "doc_control")).thenReturn(true);
        when(permissionApi.hasAnyPermissions(9L, "dcc:controlled-file:approve")).thenReturn(true);
        when(permissionApi.hasAnyPermissions(9L,
                "dcc:controlled-file:publication-followup:manage")).thenReturn(true);
        DccPublicationFollowupPageReqVO req = new DccPublicationFollowupPageReqVO();
        req.setAssigneeUserId("9999999999999999999");

        assertServiceException(() -> service.getManagementPage(9L, req),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_FOLLOWUP_FILTER_INVALID);

        verify(batchMapper, never()).selectManagementPage(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getManagementPage_acceptsLongMaximumAndRejectsZeroNegativeAndNonNumeric() {
        when(permissionApi.hasAnyRoles(9L, "doc_control")).thenReturn(true);
        when(permissionApi.hasAnyPermissions(9L, "dcc:controlled-file:approve")).thenReturn(true);
        when(permissionApi.hasAnyPermissions(9L,
                "dcc:controlled-file:publication-followup:manage")).thenReturn(true);
        DccPublicationFollowupPageReqVO request = new DccPublicationFollowupPageReqVO();
        for (String invalid : new String[]{"0", "-1", "not-a-number"}) {
            request.setAssigneeUserId(invalid);
            assertServiceException(() -> service.getManagementPage(9L, request),
                    cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_FOLLOWUP_FILTER_INVALID);
        }
        request.setAssigneeUserId(String.valueOf(Long.MAX_VALUE));
        Page<DccPublicationFollowupBatchDO> page = new Page<>(1, 10, 0);
        page.setRecords(List.of());
        when(batchMapper.selectManagementPage(any(), eq(1L), any(), any(), any(), any(), any(),
                eq(Long.MAX_VALUE))).thenReturn(page);

        service.getManagementPage(9L, request);

        verify(batchMapper).selectManagementPage(any(), eq(1L), any(), any(), any(), any(), any(),
                eq(Long.MAX_VALUE));
    }

    @Test
    void getMyImpactTasks_usesDatabasePageAndStringSafeFilters() {
        DccPublicationImpactTaskPageReqVO req = new DccPublicationImpactTaskPageReqVO();
        req.setPageNo(3); req.setPageSize(10); req.setTaskStatus("PENDING");
        req.setRevisionTrackingStatus("NOT_STARTED");
        Page<DccPublicationImpactTaskDO> page = new Page<>(3, 10, 0);
        page.setRecords(List.of());
        when(impactTaskMapper.selectAssigneePage(any(), eq(1L), eq(9L), eq("PENDING"), eq("NOT_STARTED")))
                .thenReturn(page);

        service.getMyImpactTasks(9L, req);

        verify(impactTaskMapper).selectAssigneePage(any(), eq(1L), eq(9L), eq("PENDING"), eq("NOT_STARTED"));
        verify(impactTaskMapper, never()).selectListByAssignee(any(), any());
    }
}
