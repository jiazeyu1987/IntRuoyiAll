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

    private MesTeamLeaderActiveOrderVersionUpgradeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderActiveOrderVersionUpgradeServiceImpl(
                activeOrderMapper, versionUpgradeRequestMapper, pickListBindingMapper,
                pickListBindingItemMapper, releaseApplicationMapper, auditMapper, batchExecutionMapper,
                workTaskService, reportAllocationOrderChangeService, activeOrderService, workOrderMapper,
                routeMapper, routeVersionMapper, regulationMapper, regulationVersionMapper,
                approvalOrchestratorProvider);
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

    private void stubPreviewSources() {
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder());
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(MesProWorkOrderDO.builder()
                .id(WORK_ORDER_ID)
                .code("WO-9001")
                .build());
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
