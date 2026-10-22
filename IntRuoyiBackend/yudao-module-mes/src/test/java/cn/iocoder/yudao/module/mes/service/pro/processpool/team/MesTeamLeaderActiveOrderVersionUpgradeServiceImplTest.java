package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalOrchestrator;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderVersionUpgradeRequestDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderVersionUpgradeRequestMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import org.mockito.InOrder;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderVersionUpgradeServiceImplTest {

    private static final Long LEADER_USER_ID = 3001L;
    private static final Long ACTIVE_ORDER_ID = 8101L;
    private static final Long WORK_ORDER_ID = 9001L;
    private static final Long ROUTE_ID = 922119L;
    private static final Long CURRENT_ROUTE_VERSION_ID = 447L;
    private static final Long TARGET_ROUTE_VERSION_ID = 448L;
    private static final Long REGULATION_ID = 9901L;
    private static final Long CURRENT_QA_VERSION_ID = 9902L;
    private static final Long TARGET_QA_VERSION_ID = 9903L;

    @Mock private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock private MesProcessPoolActiveOrderVersionUpgradeRequestMapper versionUpgradeRequestMapper;
    @Mock private MesProcessPoolActiveOrderPickListBindingMapper pickListBindingMapper;
    @Mock private MesProcessPoolActiveOrderPickListBindingItemMapper pickListBindingItemMapper;
    @Mock private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    @Mock private MesProcessPoolTeamMaintenanceAuditMapper auditMapper;
    @Mock private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock private MesProEdhrWorkTaskService workTaskService;
    @Mock private MesReportAllocationOrderChangeService reportAllocationOrderChangeService;
    @Mock private MesTeamLeaderActiveOrderService activeOrderService;
    @Mock private MesProWorkOrderMapper workOrderMapper;
    @Mock private MesProRouteMapper routeMapper;
    @Mock private MesProRouteVersionMapper routeVersionMapper;
    @Mock private MesQaInspectionRegulationMapper regulationMapper;
    @Mock private MesQaInspectionRegulationVersionMapper regulationVersionMapper;
    @Mock private ObjectProvider<BusinessApprovalOrchestrator> approvalOrchestratorProvider;
    @Mock private BusinessApprovalOrchestrator approvalOrchestrator;
    @Mock private MesProEdhrNonconformanceReviewService nonconformanceReviewService;

    private MesTeamLeaderActiveOrderVersionUpgradeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderActiveOrderVersionUpgradeServiceImpl(
                activeOrderMapper, versionUpgradeRequestMapper, pickListBindingMapper,
                pickListBindingItemMapper, releaseApplicationMapper, auditMapper, batchExecutionMapper,
                workTaskService, reportAllocationOrderChangeService, activeOrderService, workOrderMapper,
                routeMapper, routeVersionMapper, regulationMapper, regulationVersionMapper,
                approvalOrchestratorProvider, nonconformanceReviewService);
    }

    @Test
    void previewBlocksActiveOrderAlreadyInReleaseFlow() {
        stubPreviewSources();
        when(releaseApplicationMapper.selectListByActiveOrderIds(List.of(ACTIVE_ORDER_ID)))
                .thenReturn(List.of(releaseApplication()));

        MesTeamLeaderActiveOrderVersionUpgradePreview preview =
                service.preview(LEADER_USER_ID, ACTIVE_ORDER_ID);

        assertFalse(preview.getSubmittable());
        assertTrue(preview.getBlockers().contains("活跃订单已进入生产放行链路，禁止版本升级重启"));
    }

    @Test
    void submitRejectsReleaseApplicationBeforeFreezingActiveOrder() {
        stubPreviewSources();
        when(releaseApplicationMapper.selectListByActiveOrderIds(List.of(ACTIVE_ORDER_ID)))
                .thenReturn(List.of());
        when(versionUpgradeRequestMapper.selectByIdempotencyKey(ACTIVE_ORDER_ID, "upgrade-locked"))
                .thenReturn(null);
        when(versionUpgradeRequestMapper.selectOngoingBySourceActiveOrderId(ACTIVE_ORDER_ID))
                .thenReturn(null);
        when(activeOrderMapper.selectByIdForUpdate(ACTIVE_ORDER_ID)).thenReturn(activeOrder());
        when(releaseApplicationMapper.selectListByActiveOrderIdsForUpdate(List.of(ACTIVE_ORDER_ID)))
                .thenReturn(List.of(releaseApplication()));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.submit(
                LEADER_USER_ID,
                new MesTeamLeaderActiveOrderVersionUpgradeSubmitCommand()
                        .setActiveOrderId(ACTIVE_ORDER_ID)
                        .setIdempotencyKey("upgrade-locked")
                        .setUpgradeReason("正式版本升级")
                        .setConfirmRestartFromBeginning(true)));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_RELEASE_APPLICATION_LOCKED.getCode(),
                ex.getCode());
        verify(activeOrderMapper, never()).freezeForVersionUpgrade(any(), any(), any(), any());
        verify(versionUpgradeRequestMapper, never()).insert(
                any(MesProcessPoolActiveOrderVersionUpgradeRequestDO.class));
    }

    @Test
    void submitAllowsUnfrozenChangedVersionThroughExistingPath() {
        stubPreviewSources();
        when(releaseApplicationMapper.selectListByActiveOrderIds(List.of(ACTIVE_ORDER_ID)))
                .thenReturn(List.of());
        when(versionUpgradeRequestMapper.selectByIdempotencyKey(ACTIVE_ORDER_ID, "upgrade-normal"))
                .thenReturn(null);
        when(versionUpgradeRequestMapper.selectOngoingBySourceActiveOrderId(ACTIVE_ORDER_ID))
                .thenReturn(null);
        when(activeOrderMapper.selectByIdForUpdate(ACTIVE_ORDER_ID)).thenReturn(activeOrder());
        when(releaseApplicationMapper.selectListByActiveOrderIdsForUpdate(List.of(ACTIVE_ORDER_ID)))
                .thenReturn(List.of());
        when(activeOrderMapper.freezeForVersionUpgrade(any(), any(), any(), any())).thenReturn(1);
        when(approvalOrchestratorProvider.getObject()).thenReturn(approvalOrchestrator);

        MesTeamLeaderActiveOrderVersionUpgradeSubmitResult result = service.submit(
                LEADER_USER_ID,
                new MesTeamLeaderActiveOrderVersionUpgradeSubmitCommand()
                        .setActiveOrderId(ACTIVE_ORDER_ID)
                        .setIdempotencyKey("upgrade-normal")
                        .setUpgradeReason("正式版本升级")
                        .setConfirmRestartFromBeginning(true));

        assertEquals("PENDING", result.getApprovalStatus());
        assertEquals("OLD_ORDER_FROZEN", result.getFreezeStatus());
        verify(activeOrderMapper).freezeForVersionUpgrade(any(), any(), any(), any());
        verify(versionUpgradeRequestMapper).insert(any(MesProcessPoolActiveOrderVersionUpgradeRequestDO.class));
    }

    @Test
    void previewRejectsPendingNonconformanceBeforeVersionUpgrade() {
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder());
        doThrow(new ServiceException(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED))
                .when(nonconformanceReviewService)
                .ensureWorkOrderNotFrozen(WORK_ORDER_ID, "活跃订单版本升级预览");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.preview(LEADER_USER_ID, ACTIVE_ORDER_ID));

        assertEquals(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED.getCode(), ex.getCode());
        verify(routeMapper, never()).selectById(ROUTE_ID);
    }

    @Test
    void submitRechecksPendingNonconformanceAfterSourceOrderLock() {
        stubPreviewSources();
        when(versionUpgradeRequestMapper.selectByIdempotencyKey(ACTIVE_ORDER_ID, "upgrade-ncr-race"))
                .thenReturn(null);
        when(versionUpgradeRequestMapper.selectOngoingBySourceActiveOrderId(ACTIVE_ORDER_ID))
                .thenReturn(null);
        when(activeOrderMapper.selectByIdForUpdate(ACTIVE_ORDER_ID)).thenReturn(activeOrder());
        when(releaseApplicationMapper.selectListByActiveOrderIdsForUpdate(List.of(ACTIVE_ORDER_ID)))
                .thenReturn(List.of());
        doNothing().when(nonconformanceReviewService)
                .ensureWorkOrderNotFrozen(WORK_ORDER_ID, "活跃订单版本升级预览");
        doThrow(new ServiceException(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED))
                .when(nonconformanceReviewService)
                .ensureWorkOrderNotFrozen(WORK_ORDER_ID, "活跃订单版本升级提交");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.submit(
                LEADER_USER_ID,
                new MesTeamLeaderActiveOrderVersionUpgradeSubmitCommand()
                        .setActiveOrderId(ACTIVE_ORDER_ID)
                        .setIdempotencyKey("upgrade-ncr-race")
                        .setUpgradeReason("正式版本升级")
                        .setConfirmRestartFromBeginning(true)));

        assertEquals(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED.getCode(), ex.getCode());
        verify(activeOrderMapper, never()).freezeForVersionUpgrade(any(), any(), any(), any());
        verify(versionUpgradeRequestMapper, never()).insert(
                any(MesProcessPoolActiveOrderVersionUpgradeRequestDO.class));
        InOrder lockOrder = inOrder(activeOrderMapper, nonconformanceReviewService);
        lockOrder.verify(activeOrderMapper).selectByIdForUpdate(ACTIVE_ORDER_ID);
        lockOrder.verify(activeOrderMapper).selectById(ACTIVE_ORDER_ID);
        lockOrder.verify(nonconformanceReviewService)
                .ensureWorkOrderNotFrozen(WORK_ORDER_ID, "活跃订单版本升级预览");
        lockOrder.verify(nonconformanceReviewService)
                .ensureWorkOrderNotFrozen(WORK_ORDER_ID, "活跃订单版本升级提交");
    }

    @Test
    void applyApprovedUpgradeRejectsPendingNonconformanceBeforeRemovingSource() {
        MesProcessPoolActiveOrderVersionUpgradeRequestDO request =
                MesProcessPoolActiveOrderVersionUpgradeRequestDO.builder()
                        .id(7701L)
                        .sourceActiveOrderId(ACTIVE_ORDER_ID)
                        .sourceWorkOrderId(WORK_ORDER_ID)
                        .requestedBy(LEADER_USER_ID)
                        .requestStatus("PENDING_APPROVAL")
                        .approvalStatus("PENDING")
                        .freezeStatus("OLD_ORDER_FROZEN")
                        .build();
        when(versionUpgradeRequestMapper.selectByIdForUpdate(7701L)).thenReturn(request);
        when(activeOrderMapper.selectByIdForUpdate(ACTIVE_ORDER_ID)).thenReturn(activeOrder()
                .setActiveStatus("VERSION_UPGRADE_PENDING"));
        doThrow(new ServiceException(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED))
                .when(nonconformanceReviewService)
                .ensureWorkOrderNotFrozen(WORK_ORDER_ID, "活跃订单版本升级审批应用");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.applyApprovedUpgrade(7701L, 4001L));

        assertEquals(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED.getCode(), ex.getCode());
        verify(batchExecutionMapper, never()).voidForVersionUpgrade(any(), any(), any());
        verify(activeOrderMapper, never()).removePendingVersionUpgradeOrder(any(), any(), any(), any(), any());
        verify(activeOrderService, never()).addActiveOrder(any());
    }

    @Test
    void applyApprovedUpgradeWithoutNonconformanceCompletesExistingPath() {
        MesProcessPoolActiveOrderVersionUpgradeRequestDO request =
                MesProcessPoolActiveOrderVersionUpgradeRequestDO.builder()
                        .id(7702L)
                        .requestCode("AOVU-8101-NORMAL")
                        .sourceActiveOrderId(ACTIVE_ORDER_ID)
                        .sourceWorkOrderId(WORK_ORDER_ID)
                        .requestedBy(LEADER_USER_ID)
                        .requestStatus("PENDING_APPROVAL")
                        .approvalStatus("PENDING")
                        .freezeStatus("OLD_ORDER_FROZEN")
                        .targetSnapshotJson("{\"targetVersions\":["
                                + "{\"objectType\":\"PROCESS_ROUTE\",\"targetVersionId\":448},"
                                + "{\"objectType\":\"QA_INSPECTION_REGULATION\",\"targetVersionId\":9903}]}")
                        .build();
        when(versionUpgradeRequestMapper.selectByIdForUpdate(7702L)).thenReturn(request);
        when(activeOrderMapper.selectByIdForUpdate(ACTIVE_ORDER_ID)).thenReturn(activeOrder()
                .setActiveStatus("VERSION_UPGRADE_PENDING"));
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder());
        when(activeOrderMapper.removePendingVersionUpgradeOrder(any(), any(), any(), any(), any())).thenReturn(1);
        when(pickListBindingMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of());
        when(activeOrderService.addActiveOrder(any())).thenReturn(MesTeamLeaderActiveOrderAddResult.builder()
                .activeOrderId(8202L).build());
        when(versionUpgradeRequestMapper.markApplied(any(), any(), any(), any(), any(), any())).thenReturn(1);

        MesTeamLeaderActiveOrderVersionUpgradeApplyResult result =
                service.applyApprovedUpgrade(7702L, 4001L);

        assertEquals("APPLIED", result.getRequestStatus());
        assertEquals("APPROVED", result.getApprovalStatus());
        assertEquals("APPLIED", result.getFreezeStatus());
        assertEquals(8202L, result.getTargetActiveOrderId());
        verify(nonconformanceReviewService).ensureWorkOrderNotFrozen(
                WORK_ORDER_ID, "活跃订单版本升级审批应用");
        verify(reportAllocationOrderChangeService).invalidateActiveOrder(
                ACTIVE_ORDER_ID, 4001L, "活跃订单版本升级审批通过，旧订单作废");
        verify(activeOrderService).addActiveOrder(any());
    }

    private void stubPreviewSources() {
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder());
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(workOrder());
        when(routeMapper.selectById(ROUTE_ID)).thenReturn(MesProRouteDO.builder()
                .id(ROUTE_ID)
                .name("按压式球囊扩充压力泵")
                .build());
        when(routeVersionMapper.selectById(CURRENT_ROUTE_VERSION_ID)).thenReturn(routeVersion(
                CURRENT_ROUTE_VERSION_ID, "V1"));
        when(routeVersionMapper.selectActiveByRouteId(ROUTE_ID)).thenReturn(routeVersion(
                TARGET_ROUTE_VERSION_ID, "V2"));
        when(regulationMapper.selectById(REGULATION_ID)).thenReturn(MesQaInspectionRegulationDO.builder()
                .id(REGULATION_ID)
                .regulationName("PQC 检验规程")
                .build());
        when(regulationVersionMapper.selectById(CURRENT_QA_VERSION_ID)).thenReturn(qaVersion(
                CURRENT_QA_VERSION_ID, "A/1"));
        when(regulationVersionMapper.selectLatestPublishedByRegulationId(REGULATION_ID)).thenReturn(qaVersion(
                TARGET_QA_VERSION_ID, "A/2"));
    }

    private MesProWorkOrderDO workOrder() {
        return MesProWorkOrderDO.builder()
                .id(WORK_ORDER_ID)
                .code("WO-9001")
                .build();
    }

    private MesProcessPoolActiveOrderDO activeOrder() {
        return MesProcessPoolActiveOrderDO.builder()
                .id(ACTIVE_ORDER_ID)
                .leaderUserId(LEADER_USER_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeVersionId(CURRENT_ROUTE_VERSION_ID)
                .qaRegulationId(REGULATION_ID)
                .qaRegulationVersionId(CURRENT_QA_VERSION_ID)
                .activeStatus("ACTIVE")
                .businessStatus("PRODUCING")
                .version(7)
                .build();
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO releaseApplication() {
        return MesProcessPoolActiveOrderReleaseApplicationDO.builder()
                .id(8601L)
                .activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(WORK_ORDER_ID)
                .applicationStatus("PQC_RELEASE_PENDING")
                .build();
    }

    private MesProRouteVersionDO routeVersion(Long id, String versionNo) {
        return MesProRouteVersionDO.builder()
                .id(id)
                .routeId(ROUTE_ID)
                .versionNo(versionNo)
                .build();
    }

    private MesQaInspectionRegulationVersionDO qaVersion(Long id, String versionNo) {
        return MesQaInspectionRegulationVersionDO.builder()
                .id(id)
                .regulationId(REGULATION_ID)
                .versionNo(versionNo)
                .build();
    }
}
