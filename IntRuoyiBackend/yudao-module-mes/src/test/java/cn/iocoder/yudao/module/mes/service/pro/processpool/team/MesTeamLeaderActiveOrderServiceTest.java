package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolWorkOrderAbnormalDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderServiceTest {

    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesProWorkOrderService workOrderService;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesMdItemMapper itemMapper;
    @Mock
    private MesProcessPoolTeamMaintenanceAuditMapper auditMapper;
    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesRouteDccProjectBindingMapper routeDccProjectBindingMapper;
    @Mock
    private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    @Mock
    private MesProcessPoolReportAllocationMapper reportAllocationMapper;
    @Mock
    private MesQaInspectionRegulationMapper inspectionRegulationMapper;
    @Mock
    private MesQaInspectionRegulationVersionMapper inspectionRegulationVersionMapper;
    @Mock
    private MesQaInspectionRegulationProcessMapper inspectionRegulationProcessMapper;
    @Mock
    private MesQaInspectionRegulationItemMapper inspectionRegulationItemMapper;
    @Mock
    private MesPqcInspectionTaskMapper pqcInspectionTaskMapper;
    @Mock
    private MesWorkOrderAbnormalStateService abnormalStateService;
    @Mock
    private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    @Mock
    private DccProjectCodeMapper dccProjectCodeMapper;
    @Mock
    private MesReportAllocationOrderChangeService reportAllocationOrderChangeService;

    private MesTeamLeaderActiveOrderService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderActiveOrderServiceImpl(activeOrderMapper, workOrderService, workOrderMapper,
                itemMapper, auditMapper, scheduleOrderMapper, scheduleOrderProcessMapper, routeProductMapper, routeMapper,
                routeVersionMapper, routeDccProjectBindingMapper, processSnapshotMapper, reportAllocationMapper,
                inspectionRegulationMapper, inspectionRegulationVersionMapper, inspectionRegulationProcessMapper,
                inspectionRegulationItemMapper, pqcInspectionTaskMapper, abnormalStateService,
                releaseApplicationMapper, dccProjectCodeMapper, reportAllocationOrderChangeService);
        lenient().when(itemMapper.selectListByCodeOrNameLike(any(), eq(20))).thenReturn(List.of());
        lenient().when(inspectionRegulationMapper.selectListByDccProjectCodeIds(any()))
                .thenReturn(List.of(publishedRegulation(9902L)));
        lenient().when(inspectionRegulationVersionMapper.selectById(9902L))
                .thenReturn(publishedRegulationVersion(true, null));
        lenient().when(inspectionRegulationItemMapper.selectListByVersionId(9902L)).thenReturn(defaultPqcItems());
        lenient().when(pqcInspectionTaskMapper.selectByQaIdentity(any(), any(), any(), any(), any(), any()))
                .thenReturn(null);
        lenient().when(pqcInspectionTaskMapper.insert(any(MesPqcInspectionTaskDO.class))).thenReturn(1);
        lenient().when(abnormalStateService.findLatestOpenByWorkOrderIds(any())).thenReturn(Map.of());
        lenient().when(releaseApplicationMapper.selectLatestByActiveOrderIds(any())).thenReturn(List.of());
        lenient().when(reportAllocationMapper.selectListByActiveOrderIds(any())).thenReturn(List.of());
    }

    @Test
    void shouldReturnUnreleasedAllocationsBeforeRemovingActiveOrder() {
        MesProcessPoolActiveOrderDO activeOrder = existingActiveOrder(8101L, "ACTIVE", 7);
        when(activeOrderMapper.selectByIdForUpdate(8101L)).thenReturn(activeOrder);
        when(activeOrderMapper.removeActiveOrder(eq(8101L), eq(7), any(LocalDateTime.class))).thenReturn(1);

        service.removeActiveOrder(MesTeamLeaderActiveOrderRemoveReqBO.builder()
                .leaderUserId(3001L).activeOrderId(8101L).build());

        InOrder order = inOrder(reportAllocationOrderChangeService, activeOrderMapper);
        order.verify(reportAllocationOrderChangeService)
                .invalidateActiveOrder(8101L, 3001L, "活跃订单移除");
        order.verify(activeOrderMapper).removeActiveOrder(eq(8101L), eq(7), any(LocalDateTime.class));
    }

    @Test
    void shouldSearchWorkOrderCandidatesByCodeWhenProductionRouteExists() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder()));
        stubCandidatePqcPrerequisites(publishedRegulation(9902L));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertEquals(9001L, candidates.get(0).getWorkOrderId());
        assertEquals("WO-9001", candidates.get(0).getWorkOrderCode());
        assertTrue(candidates.get(0).isEligible());
        assertEquals(null, candidates.get(0).getIneligibleReason());
        verify(workOrderMapper).selectCandidatesByKeyword("WO-9", List.of());
        verify(scheduleOrderMapper, never()).selectEffectiveListByWorkOrderIds(any());
        verify(routeProductMapper).selectListByItemIds(List.of(1001L));
        verify(routeVersionMapper).selectListByRouteIds(List.of(922119L));
    }

    @Test
    void shouldSearchWorkOrderCandidatesByProductKeywordWhenProductionRouteExists() {
        when(itemMapper.selectListByCodeOrNameLike("球囊", 20)).thenReturn(List.of(
                MesMdItemDO.builder().id(1001L).code("AW.107.02.01.2010").name("球囊扩张压力泵").build()));
        when(workOrderMapper.selectCandidatesByKeyword("球囊", List.of(1001L)))
                .thenReturn(List.of(confirmedWorkOrder()));
        stubCandidatePqcPrerequisites(publishedRegulation(9902L));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("球囊");

        assertEquals(1, candidates.size());
        assertEquals(9001L, candidates.get(0).getWorkOrderId());
        assertEquals("WO-9001", candidates.get(0).getWorkOrderCode());
        assertTrue(candidates.get(0).isEligible());
        verify(itemMapper).selectListByCodeOrNameLike("球囊", 20);
        verify(workOrderMapper).selectCandidatesByKeyword("球囊", List.of(1001L));
    }

    @Test
    void shouldResolveProductionRouteWhenWorkOrderProductDiffers() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder(9001L, "WO-9001", new BigDecimal("200"), 1002L)));
        MesQaInspectionRegulationDO routeRegulation = publishedRegulation(
                9902L, 1001L, 922119L, 448L, 928609L, 6001L);
        stubFormalRouteQaContext(1002L, 448L, routeRegulation);

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertTrue(candidates.get(0).isEligible());
        assertEquals(null, candidates.get(0).getIneligibleReason());
        verify(routeProductMapper).selectListByItemIds(List.of(1002L));
        verify(routeVersionMapper).selectListByRouteIds(List.of(922119L));
        verify(routeDccProjectBindingMapper, never()).selectCurrentByRouteId(any());
        verify(dccProjectCodeMapper, never()).selectById(any());
        verify(inspectionRegulationMapper, never()).selectListByDccProjectCodeIds(any());
    }

    @Test
    void shouldIgnoreQaRouteVersionFieldsWhenResolvingProductionRoute() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder()));
        MesQaInspectionRegulationDO legacyRouteFields = publishedRegulation(
                9902L, 1001L, 922119L, 448L, 928608L, 6000L);
        stubFormalRouteQaContext(1001L, 627L, legacyRouteFields);

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertTrue(candidates.get(0).isEligible());
        assertEquals(null, candidates.get(0).getIneligibleReason());
    }

    @Test
    void shouldIgnoreDeletedRouteBindingWhenOneFormalRouteStillExists() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrder()));
        stubFormalRouteQaContext(1001L, 448L, publishedRegulation(9902L));
        doReturn(List.of(
                MesProRouteProductDO.builder().id(7001L).routeId(922119L).itemId(1001L).build(),
                MesProRouteProductDO.builder().id(7002L).routeId(999999L).itemId(1001L).build()))
                .when(routeProductMapper).selectListByItemIds(any());
        when(routeMapper.selectBatchIds(any())).thenReturn(List.of(
                MesProRouteDO.builder().id(922119L).code("ROUTE-922119").build()));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertTrue(candidates.get(0).isEligible());
    }

    @Test
    void shouldBlockCancelledWorkOrderBeforeProductionRouteResolution() {
        MesProWorkOrderDO cancelled = confirmedWorkOrder();
        cancelled.setStatus(MesProWorkOrderStatusEnum.CANCELED.getStatus());
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of())).thenReturn(List.of(cancelled));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertFalse(candidates.get(0).isEligible());
        assertEquals("生产工单已取消", candidates.get(0).getIneligibleReason());
        verify(inspectionRegulationMapper, never()).selectListByDccProjectCodeIds(any());
    }

    @Test
    void shouldRequireFormalProductRouteBindingForCandidate() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrderWithPlannedStart()));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertFalse(candidates.get(0).isEligible());
        assertEquals("缺少产品工艺路线绑定", candidates.get(0).getIneligibleReason());
        verify(scheduleOrderMapper, never()).selectEffectiveListByWorkOrderIds(any());
        verify(routeProductMapper).selectListByItemIds(List.of(1001L));
    }

    @Test
    void shouldAllowCandidateWhenProductionRouteIsReadyButQaIsMissing() {
        when(workOrderMapper.selectCandidatesByKeyword("WO-9", List.of()))
                .thenReturn(List.of(confirmedWorkOrderWithPlannedStart()));
        stubFormalRouteQaContext(1001L, 448L);

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO-9");

        assertEquals(1, candidates.size());
        assertTrue(candidates.get(0).isEligible());
        assertEquals(null, candidates.get(0).getIneligibleReason());
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldSortEligibleActiveOrderCandidatesBeforeBlockedCandidates() {
        when(workOrderMapper.selectCandidatesByKeyword("WO", List.of())).thenReturn(List.of(
                confirmedWorkOrder(9002L, "WO-9002", new BigDecimal("200"), 1002L),
                confirmedWorkOrder(9001L, "WO-9001", new BigDecimal("200"), 1001L)));
        stubCandidatePqcPrerequisites(publishedRegulation(9902L, 928609L, 6001L));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("WO");

        assertEquals(List.of(9001L, 9002L), candidates.stream()
                .map(MesTeamLeaderActiveOrderCandidateBO::getWorkOrderId)
                .toList());
        assertTrue(candidates.get(0).isEligible());
        assertEquals(null, candidates.get(0).getIneligibleReason());
        assertFalse(candidates.get(1).isEligible());
        assertEquals("缺少产品工艺路线绑定", candidates.get(1).getIneligibleReason());
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(pqcInspectionTaskMapper, never()).insert(any(MesPqcInspectionTaskDO.class));
    }

    @Test
    void shouldNotLoadQaDependenciesForRemoteDropdown() {
        when(workOrderMapper.selectCandidatesByKeyword("88", List.of())).thenReturn(List.of(
                confirmedWorkOrder(9001L, "881MO093613", new BigDecimal("200"), 1001L),
                confirmedWorkOrder(9002L, "881MO093615", new BigDecimal("200"), 1001L)));
        stubCandidatePqcPrerequisites(publishedRegulation(9902L, 928609L, 6001L));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("88");

        assertEquals(List.of(9001L, 9002L), candidates.stream()
                .map(MesTeamLeaderActiveOrderCandidateBO::getWorkOrderId)
                .toList());
        assertTrue(candidates.stream().allMatch(MesTeamLeaderActiveOrderCandidateBO::isEligible));
        verify(scheduleOrderMapper, never()).selectEffectiveListByWorkOrderIds(any());
        verify(scheduleOrderProcessMapper, never()).selectListByScheduleOrderIds(any());
        verify(scheduleOrderProcessMapper, never()).selectListByScheduleOrderId(any());
        verify(inspectionRegulationMapper, never()).selectListByDccProjectCodeIds(any());
        verify(inspectionRegulationVersionMapper, never()).selectBatchIds(any());
        verify(inspectionRegulationVersionMapper, never()).selectById(any());
        verify(inspectionRegulationItemMapper, never()).selectListByVersionIds(any());
        verify(inspectionRegulationItemMapper, never()).selectListByVersionId(any());
    }

    @Test
    void shouldApplyCandidateLimitAfterEligibilityEvaluationForBroadKeyword() {
        List<MesProWorkOrderDO> matches = new ArrayList<>();
        for (int index = 1; index <= 20; index++) {
            MesProWorkOrderDO cancelled = confirmedWorkOrder(9100L + index,
                    "883MO-CANCELLED-" + index, new BigDecimal("200"), 2000L + index);
            cancelled.setStatus(MesProWorkOrderStatusEnum.CANCELED.getStatus());
            matches.add(cancelled);
        }
        matches.add(confirmedWorkOrder(9001L, "881MO090935", new BigDecimal("517"), 1001L));
        matches.add(confirmedWorkOrder(9002L, "881MO090972", new BigDecimal("5223"), 1001L));
        matches.add(confirmedWorkOrder(9003L, "881MO090973", new BigDecimal("4223"), 1001L));
        matches.add(confirmedWorkOrder(9004L, "881MO090974", new BigDecimal("8543"), 1001L));
        when(workOrderMapper.selectCandidatesByKeyword("88", List.of())).thenReturn(matches);
        stubCandidatePqcPrerequisites(publishedRegulation(9902L, 928609L, 6001L));

        List<MesTeamLeaderActiveOrderCandidateBO> candidates = service.searchActiveOrderCandidates("88");

        assertEquals(20, candidates.size());
        assertEquals(List.of("881MO090935", "881MO090972", "881MO090973", "881MO090974"),
                candidates.subList(0, 4).stream()
                        .map(MesTeamLeaderActiveOrderCandidateBO::getWorkOrderCode)
                        .toList());
        assertTrue(candidates.subList(0, 4).stream().allMatch(MesTeamLeaderActiveOrderCandidateBO::isEligible));
        assertTrue(candidates.subList(4, candidates.size()).stream()
                .allMatch(candidate -> !candidate.isEligible()
                        && "生产工单已取消".equals(candidate.getIneligibleReason())));
    }

    @Test
    void shouldAddWorkOrderToLeaderActivePoolWithServerResolvedProductionRoute() {
        stubWorkOrderExists(confirmedWorkOrder());
        stubFormalRouteQaContext(1001L, 448L, activeRouteSnapshotJson(2),
                publishedRegulation(9902L, 928609L, 6001L));
        when(activeOrderMapper.selectLastByLeaderForUpdate(3001L))
                .thenReturn(MesProcessPoolActiveOrderDO.builder().id(8000L).sortOrder(40L).build());
        stubSuccessfulActiveOrderInsert();

        Long activeOrderId = service.addActiveOrder(activeOrderReq());

        assertEquals(8101L, activeOrderId);
        verify(workOrderService).validateWorkOrderExists(9001L);
        verify(workOrderService, never()).validateWorkOrderConfirmed(any());
        verify(scheduleOrderMapper, never()).selectEffectiveListByWorkOrderIds(any());
        verify(routeProductMapper).selectListByItemIds(List.of(1001L));
        verify(routeVersionMapper).selectListByRouteIds(List.of(922119L));
        ArgumentCaptor<MesProcessPoolActiveOrderDO> captor =
                ArgumentCaptor.forClass(MesProcessPoolActiveOrderDO.class);
        verify(activeOrderMapper).insert(captor.capture());
        MesProcessPoolActiveOrderDO activeOrder = captor.getValue();
        assertEquals(3001L, activeOrder.getLeaderUserId());
        assertEquals(9001L, activeOrder.getWorkOrderId());
        assertEquals(922119L, activeOrder.getRouteId());
        assertEquals(448L, activeOrder.getRouteVersionId());
        assertEquals(147L, activeOrder.getDccProjectCodeId());
        assertEquals(9901L, activeOrder.getQaRegulationId());
        assertEquals(9902L, activeOrder.getQaRegulationVersionId());
        assertEquals(new BigDecimal("200"), activeOrder.getErpFixedQuantitySnapshot());
        assertEquals("ACTIVE", activeOrder.getActiveStatus());
        assertEquals("ACTIVE", activeOrder.getBusinessStatus());
        assertEquals(41L, activeOrder.getSortOrder());
        assertEquals(0, activeOrder.getVersion());
        assertNotNull(activeOrder.getJoinedAt());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MesProcessPoolActiveOrderProcessSnapshotDO>> snapshotCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(processSnapshotMapper).insertBatch(snapshotCaptor.capture());
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = List.copyOf(snapshotCaptor.getValue());
        assertEquals(2, snapshots.size());
        assertSnapshot(snapshots.get(0), 8101L, 9001L, 922119L, 448L, 928601L, 6001L,
                "200", "1.000000", "200.000000");
        assertSnapshot(snapshots.get(1), 8101L, 9001L, 922119L, 448L, 928602L, 6002L,
                "200", "1.000000", "200.000000");
        ArgumentCaptor<MesPqcInspectionTaskDO> taskCaptor =
                ArgumentCaptor.forClass(MesPqcInspectionTaskDO.class);
        verify(pqcInspectionTaskMapper, times(4)).insert(taskCaptor.capture());
        List<MesPqcInspectionTaskDO> tasks = taskCaptor.getAllValues();
        LocalDate businessDate = activeOrder.getJoinedAt().toLocalDate();
        assertPqcTask(tasks.get(0), "FIRST", "FIRST", "FIRST", 5, businessDate);
        assertPqcTask(tasks.get(1), "PATROL", "PATROL_AM", "AM", 10, businessDate);
        assertPqcTask(tasks.get(2), "PATROL", "PATROL_PM", "PM", 10, businessDate);
        assertPqcTask(tasks.get(3), "FINAL", "FINAL", "FINAL", 3, businessDate);
        verify(auditMapper).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldSnapshotAllRouteProcessesAndCreateQaOwnedPqcTasks() {
        stubWorkOrderExists(confirmedWorkOrder(new BigDecimal("10")));
        stubFormalRouteQaContext(1001L, 448L, activeRouteSnapshotJson(10),
                publishedRegulation(9902L, 928601L, 6001L));
        stubSuccessfulActiveOrderInsert();

        Long activeOrderId = service.addActiveOrder(activeOrderReq());

        assertEquals(8101L, activeOrderId);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MesProcessPoolActiveOrderProcessSnapshotDO>> snapshotCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(processSnapshotMapper).insertBatch(snapshotCaptor.capture());
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = List.copyOf(snapshotCaptor.getValue());
        assertEquals(10, snapshots.size());
        assertSnapshot(snapshots.get(0), 8101L, 9001L, 922119L, 448L, 928601L, 6001L,
                "10", "1.000000", "10.000000");
        assertSnapshot(snapshots.get(9), 8101L, 9001L, 922119L, 448L, 928610L, 6010L,
                "10", "1.000000", "10.000000");
        verify(pqcInspectionTaskMapper, times(4)).insert(any(MesPqcInspectionTaskDO.class));
    }

    @Test
    void shouldCreateIndependentFirstAndPatrolTasksForItemsInSameQaProcess() {
        stubWorkOrderExists(confirmedWorkOrder());
        stubFormalRouteQaContext(1001L, 448L, activeRouteSnapshotJson(2),
                publishedRegulation(9902L, 928609L, 6001L));
        when(inspectionRegulationItemMapper.selectListByVersionId(9902L)).thenReturn(List.of(
                pqcItem("APPEARANCE", "外观", "FIRST", 13, null),
                pqcItem("PRESSURE_RELEASE", "撤压", "FIRST", 5, null),
                pqcItem("APPEARANCE", "外观", "PATROL", null, new BigDecimal("0.400000")),
                pqcItem("PRESSURE_RELEASE", "撤压", "PATROL", null, new BigDecimal("1.000000")),
                pqcItem("APPEARANCE", "外观", "FINAL", 3, null),
                pqcItem("PRESSURE_RELEASE", "撤压", "FINAL", 3, null)));
        stubSuccessfulActiveOrderInsert();

        service.addActiveOrder(activeOrderReq());

        ArgumentCaptor<MesPqcInspectionTaskDO> taskCaptor =
                ArgumentCaptor.forClass(MesPqcInspectionTaskDO.class);
        verify(pqcInspectionTaskMapper, times(7)).insert(taskCaptor.capture());
        List<MesPqcInspectionTaskDO> tasks = taskCaptor.getAllValues();
        assertEquals(List.of(5, 13), tasks.stream()
                .filter(task -> "FIRST".equals(task.getInspectionRuleKey()))
                .map(MesPqcInspectionTaskDO::getPlannedInspectionQuantity)
                .sorted()
                .toList());
        assertEquals(List.of("APPEARANCE:13", "PRESSURE_RELEASE:5"), tasks.stream()
                .filter(task -> "FIRST".equals(task.getInspectionRuleKey()))
                .map(task -> task.getQaItemCode() + ":" + task.getPlannedInspectionQuantity())
                .sorted()
                .toList());
        assertEquals(List.of(1, 1, 2, 2), tasks.stream()
                .filter(task -> task.getInspectionRuleKey().startsWith("PATROL_"))
                .map(MesPqcInspectionTaskDO::getPlannedInspectionQuantity)
                .sorted()
                .toList());
        assertEquals(List.of("APPEARANCE:1", "APPEARANCE:1",
                        "PRESSURE_RELEASE:2", "PRESSURE_RELEASE:2"), tasks.stream()
                .filter(task -> task.getInspectionRuleKey().startsWith("PATROL_"))
                .map(task -> task.getQaItemCode() + ":" + task.getPlannedInspectionQuantity())
                .sorted()
                .toList());
        assertEquals(1L, tasks.stream()
                .filter(task -> "FINAL".equals(task.getInspectionRuleKey()))
                .count());
        assertEquals("", tasks.stream()
                .filter(task -> "FINAL".equals(task.getInspectionRuleKey()))
                .findFirst()
                .orElseThrow()
                .getQaItemCode());
    }

    @Test
    void shouldAddUsingProductionRouteWhenWorkOrderProductDiffers() {
        stubWorkOrderExists(confirmedWorkOrder(9001L, "WO-9001", new BigDecimal("200"), 1002L));
        stubFormalRouteQaContext(1002L, 448L,
                publishedRegulation(9902L, 1001L, 922119L, 448L, 928609L, 6001L));
        stubSuccessfulActiveOrderInsert();

        Long activeOrderId = service.addActiveOrder(activeOrderReq());

        assertEquals(8101L, activeOrderId);
        verify(routeProductMapper).selectListByItemIds(List.of(1002L));
        verify(routeDccProjectBindingMapper).selectCurrentByRouteId(922119L);
        verify(inspectionRegulationMapper).selectListByDccProjectCodeIds(List.of(147L));
        ArgumentCaptor<MesProcessPoolActiveOrderDO> captor =
                ArgumentCaptor.forClass(MesProcessPoolActiveOrderDO.class);
        verify(activeOrderMapper).insert(captor.capture());
        assertEquals(922119L, captor.getValue().getRouteId());
        assertEquals(448L, captor.getValue().getRouteVersionId());
        assertEquals(147L, captor.getValue().getDccProjectCodeId());
        assertEquals(9901L, captor.getValue().getQaRegulationId());
        assertEquals(9902L, captor.getValue().getQaRegulationVersionId());
    }

    @Test
    void shouldRejectAddWithoutFormalProductRouteBinding() {
        stubWorkOrderExists(confirmedWorkOrderWithPlannedStart());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(activeOrderReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("缺少产品工艺路线绑定"));
        verify(workOrderService).validateWorkOrderExists(9001L);
        verify(workOrderService, never()).validateWorkOrderConfirmed(any());
        verify(scheduleOrderMapper, never()).selectEffectiveListByWorkOrderIds(any());
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldAllowUnconfirmedWorkOrderWhenProductionRouteExists() {
        MesProWorkOrderDO draftWorkOrder = confirmedWorkOrderWithPlannedStart();
        draftWorkOrder.setStatus(0);
        stubWorkOrderExists(draftWorkOrder);
        stubCandidatePqcPrerequisites(publishedRegulation(9902L));
        stubSuccessfulActiveOrderInsert();

        Long activeOrderId = service.addActiveOrder(activeOrderReq());

        assertEquals(8101L, activeOrderId);
        verify(workOrderService, never()).validateWorkOrderConfirmed(any());
    }

    @Test
    void shouldRejectAddWhenQaIsMissing() {
        stubWorkOrderExists(confirmedWorkOrderWithPlannedStart());
        stubFormalRouteQaContext(1001L, 448L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.addActiveOrder(activeOrderReq()));

        assertEquals(ErrorCodeConstants.PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("DCC项目代码缺少唯一QA规程"));
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldRejectAddWhenRouteHasNoDccProjectBinding() {
        stubWorkOrderExists(confirmedWorkOrderWithPlannedStart());
        stubFormalRouteQaContext(1001L, 448L, publishedRegulation(9902L));
        when(routeDccProjectBindingMapper.selectCurrentByRouteId(922119L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.addActiveOrder(activeOrderReq()));

        assertEquals(ErrorCodeConstants.PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("缺少正式DCC项目代码关系"));
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldRejectAddWhenRouteDccProjectIsDisabled() {
        stubWorkOrderExists(confirmedWorkOrderWithPlannedStart());
        stubFormalRouteQaContext(1001L, 448L, publishedRegulation(9902L));
        when(dccProjectCodeMapper.selectById(147L)).thenReturn(DccProjectCodeDO.builder()
                .id(147L).projectCode("ID").status("DISABLE").build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.addActiveOrder(activeOrderReq()));

        assertEquals(ErrorCodeConstants.PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("DCC项目代码不存在或已停用"));
        verifyNoActiveOrderWrites();
    }

    @Test
    void shouldReturnExistingActiveOrderWhenSameWorkOrderRouteVersionAlreadyActive() {
        stubWorkOrderExists(confirmedWorkOrder());
        stubCandidatePqcPrerequisites(publishedRegulation(9902L));
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(9001L, 922119L, 448L))
                .thenReturn(existingActiveOrder(8101L, "ACTIVE", 0));

        Long activeOrderId = service.addActiveOrder(activeOrderReq());

        assertEquals(8101L, activeOrderId);
        verify(activeOrderMapper).selectActiveByWorkOrderRouteVersion(9001L, 922119L, 448L);
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(processSnapshotMapper, never()).insertBatch(any());
    }

    @Test
    void shouldReturnExistingActiveOrderWhenConcurrentInsertHitsUniqueKey() {
        stubWorkOrderExists(confirmedWorkOrder());
        stubCandidatePqcPrerequisites(publishedRegulation(9902L));
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(9001L, 922119L, 448L))
                .thenReturn(null, existingActiveOrder(8102L, "ACTIVE", 0));
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class)))
                .thenThrow(new DuplicateKeyException("uk_mes_pp_active_order"));

        Long activeOrderId = service.addActiveOrder(activeOrderReq());

        assertEquals(8102L, activeOrderId);
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(auditMapper, never()).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldReactivateRemovedActiveOrderWhenSameWorkOrderRouteVersionIsJoinedAgain() {
        stubWorkOrderExists(confirmedWorkOrder());
        stubCandidatePqcPrerequisites(publishedRegulation(9902L));
        when(activeOrderMapper.selectRemovedByWorkOrderRouteVersion(9001L, 922119L, 448L))
                .thenReturn(existingActiveOrder(8101L, "REMOVED", 7));
        when(inspectionRegulationMapper.selectById(9901L)).thenReturn(publishedRegulation(9902L));
        when(pqcInspectionTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder()
                        .id(8801L)
                        .activeOrderId(8101L)
                        .qaProcessId(19902L)
                        .regulationVersionId(9902L)
                        .inspectionType("FIRST")
                        .inspectionRuleKey("FIRST")
                        .businessDate(LocalDate.of(2026, 8, 12))
                        .shiftCode("FIRST")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)
                        .build()));
        when(activeOrderMapper.reactivateRemovedActiveOrder(any(), any(), any(), any(), any())).thenReturn(1);

        Long activeOrderId = service.addActiveOrder(activeOrderReq());

        assertEquals(8101L, activeOrderId);
        verify(activeOrderMapper).reactivateRemovedActiveOrder(
                eq(8101L), eq(3001L), eq(7), any(LocalDateTime.class), eq(1L));
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(routeDccProjectBindingMapper, never()).selectCurrentByRouteId(any());
        verify(dccProjectCodeMapper, never()).selectById(any());
        verify(inspectionRegulationMapper, never()).selectListByDccProjectCodeIds(any());
        verify(pqcInspectionTaskMapper, never()).insert(any(MesPqcInspectionTaskDO.class));
        verify(auditMapper).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldListActiveOrdersWithFormalRouteDisplayFieldsUsingBatchQueries() {
        List<MesProcessPoolActiveOrderDO> activeOrderRows = List.of(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .activeStatus("ACTIVE")
                .joinedAt(LocalDateTime.of(2026, 7, 31, 8, 30))
                .build());
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(activeOrderRows);
        when(routeMapper.selectBatchIds(List.of(922119L))).thenReturn(List.of(MesProRouteDO.builder()
                .id(922119L)
                .name("按压式球囊扩充压力泵工艺路线")
                .build()));
        when(routeVersionMapper.selectBatchIds(List.of(448L))).thenReturn(List.of(MesProRouteVersionDO.builder()
                .id(448L)
                .routeId(922119L)
                .versionNo("V1")
                .build()));
        when(workOrderMapper.selectBatchIds(List.of(9001L))).thenReturn(List.of(confirmedWorkOrder()));
        when(itemMapper.selectBatchIds(List.of(1001L))).thenReturn(List.of(MesMdItemDO.builder()
                .id(1001L)
                .code("AW.107.02.01.2010")
                .name("球囊扩张压力泵")
                .build()));
        when(abnormalStateService.findLatestOpenByWorkOrderIds(List.of(9001L))).thenReturn(Map.of(9001L,
                MesProcessPoolWorkOrderAbnormalDO.builder()
                        .id(8801L)
                        .workOrderId(9001L)
                        .abnormalDescription("设备停机")
                        .reportStatus(MesProcessPoolWorkOrderAbnormalDO.REPORT_STATUS_REPORTED)
                        .reportedAt(LocalDateTime.of(2026, 8, 7, 11, 0))
                        .build()));
        when(processSnapshotMapper.selectListByActiveOrderIds(List.of(8101L)))
                .thenReturn(processSnapshots(8101L, 9001L, 10));
        when(pqcInspectionTaskMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of());
        when(releaseApplicationMapper.selectLatestByActiveOrderIds(List.of(8101L))).thenReturn(List.of(
                MesProcessPoolActiveOrderReleaseApplicationDO.builder()
                        .id(7001L)
                        .activeOrderId(8101L)
                        .pqcReleaseWorkTaskId(8001L)
                        .applicationStatus("PQC_RELEASE_PENDING")
                        .sourceSnapshotHash("source-hash")
                        .version(1)
                        .build()));

        List<MesTeamLeaderActiveOrderRow> activeOrders = service.listActiveOrders(3001L);

        assertEquals(1, activeOrders.size());
        assertEquals(8101L, activeOrders.get(0).getId());
        assertEquals("WO-9001", activeOrders.get(0).getWorkOrderCode());
        assertEquals("BATCH-9001", activeOrders.get(0).getBatchCode());
        assertEquals("球囊扩张压力泵", activeOrders.get(0).getProductName());
        assertEquals("AW.107.02.01.2010", activeOrders.get(0).getProductCode());
        assertEquals(new BigDecimal("200"), activeOrders.get(0).getQuantity());
        assertEquals("按压式球囊扩充压力泵工艺路线", activeOrders.get(0).getRouteName());
        assertEquals("V1", activeOrders.get(0).getRouteVersionNo());
        assertEquals(new BigDecimal("0.000000"), activeOrders.get(0).getProductionProgressPercent());
        assertEquals(new BigDecimal("0.000000"), activeOrders.get(0).getInspectionProgressPercent());
        assertTrue(activeOrders.get(0).getAbnormal());
        assertEquals("设备停机", activeOrders.get(0).getAbnormalReason());
        assertEquals(LocalDateTime.of(2026, 8, 7, 11, 0), activeOrders.get(0).getAbnormalReportedAt());
        assertEquals(7001L, activeOrders.get(0).getReleaseApplicationId());
        assertEquals(8001L, activeOrders.get(0).getPqcReleaseWorkTaskId());
        assertEquals("PQC_RELEASE_PENDING", activeOrders.get(0).getReleaseApplicationStatus());
        assertEquals("source-hash", activeOrders.get(0).getReleaseSourceSnapshotHash());
        assertEquals(1, activeOrders.get(0).getReleaseApplicationVersion());
        verify(activeOrderMapper).selectActiveListByLeader(3001L);
        verify(routeMapper).selectBatchIds(List.of(922119L));
        verify(routeVersionMapper).selectBatchIds(List.of(448L));
        verify(workOrderMapper).selectBatchIds(List.of(9001L));
        verify(itemMapper).selectBatchIds(List.of(1001L));
        verify(activeOrderMapper, never()).selectActiveList();
        verify(scheduleOrderProcessMapper, never()).selectListByScheduleOrderId(any());
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(scheduleOrderMapper, never()).selectEffectiveListByWorkOrderIds(any());
    }

    @Test
    void shouldListActiveOrdersWithProductionAndInspectionProgressByFormalProcessCount() {
        List<MesProcessPoolActiveOrderDO> activeOrderRows = List.of(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .activeStatus("ACTIVE")
                .joinedAt(LocalDateTime.of(2026, 7, 31, 8, 30))
                .build());
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(activeOrderRows);
        when(routeMapper.selectBatchIds(List.of(922119L))).thenReturn(List.of(MesProRouteDO.builder()
                .id(922119L)
                .name("按压式球囊扩充压力泵工艺路线")
                .build()));
        when(routeVersionMapper.selectBatchIds(List.of(448L))).thenReturn(List.of(MesProRouteVersionDO.builder()
                .id(448L)
                .routeId(922119L)
                .versionNo("V1")
                .build()));
        when(workOrderMapper.selectBatchIds(List.of(9001L))).thenReturn(List.of(confirmedWorkOrder()));
        when(itemMapper.selectBatchIds(List.of(1001L))).thenReturn(List.of(MesMdItemDO.builder()
                .id(1001L)
                .code("AW.107.02.01.2010")
                .name("球囊扩张压力泵")
                .build()));
        when(processSnapshotMapper.selectListByActiveOrderIds(List.of(8101L)))
                .thenReturn(processSnapshots(8101L, 9001L, 10));
        when(reportAllocationMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of(
                allocation(8101L, 9001L, 5001L, 6001L, "200"),
                allocation(8101L, 9001L, 5002L, 6002L, "199"),
                allocation(8101L, 9001L, 5099L, 6099L, "200")));
        when(pqcInspectionTaskMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of(
                pqcTask(8101L, 5003L, 6003L, MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED),
                pqcTask(8101L, 5004L, 6004L, MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED),
                pqcTask(8101L, 5005L, 6005L, MesPqcInspectionTaskDO.TASK_STATUS_PENDING)));

        List<MesTeamLeaderActiveOrderRow> activeOrders = service.listActiveOrders(3001L);

        assertEquals(1, activeOrders.size());
        assertEquals(new BigDecimal("10.000000"), activeOrders.get(0).getProductionProgressPercent());
        assertEquals(new BigDecimal("10.000000"), activeOrders.get(0).getInspectionProgressPercent());
        verify(processSnapshotMapper).selectListByActiveOrderIds(List.of(8101L));
        verify(reportAllocationMapper).selectListByActiveOrderIds(List.of(8101L));
        verify(pqcInspectionTaskMapper).selectListByActiveOrderIds(List.of(8101L));
    }

    @Test
    void shouldListActiveOrdersWithPerProcessRemainingQuantitiesAfterCurrentAllocations() {
        List<MesProcessPoolActiveOrderDO> activeOrderRows = List.of(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .activeStatus("ACTIVE")
                .joinedAt(LocalDateTime.of(2026, 7, 31, 8, 30))
                .build());
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(activeOrderRows);
        when(routeMapper.selectBatchIds(List.of(922119L))).thenReturn(List.of(MesProRouteDO.builder()
                .id(922119L)
                .name("按压式球囊扩充压力泵工艺路线")
                .build()));
        when(routeVersionMapper.selectBatchIds(List.of(448L))).thenReturn(List.of(MesProRouteVersionDO.builder()
                .id(448L)
                .routeId(922119L)
                .versionNo("V1")
                .build()));
        when(workOrderMapper.selectBatchIds(List.of(9001L))).thenReturn(List.of(confirmedWorkOrder(new BigDecimal("100"))));
        when(itemMapper.selectBatchIds(List.of(1001L))).thenReturn(List.of(MesMdItemDO.builder()
                .id(1001L)
                .code("AW.107.02.01.2010")
                .name("球囊扩张压力泵")
                .build()));
        when(processSnapshotMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of(
                processSnapshot(8101L, 9001L, 5001L, 6001L, "100.000000"),
                processSnapshot(8101L, 9001L, 5002L, 6002L, "100.000000")));
        when(reportAllocationMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of(
                allocation(8101L, 9001L, 5001L, 6001L, "70"),
                allocation(8101L, 9001L, 5002L, 6002L, "100")));
        when(pqcInspectionTaskMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of());

        List<MesTeamLeaderActiveOrderRow> activeOrders = service.listActiveOrders(3001L);

        assertEquals(1, activeOrders.size());
        assertEquals(2, activeOrders.get(0).getProcessRemainingQuantities().size());
        assertEquals(5001L, activeOrders.get(0).getProcessRemainingQuantities().get(0).getRouteProcessId());
        assertEquals(6001L, activeOrders.get(0).getProcessRemainingQuantities().get(0).getProcessId());
        assertEquals(new BigDecimal("100.000000"),
                activeOrders.get(0).getProcessRemainingQuantities().get(0).getPlannedQuantity());
        assertEquals(new BigDecimal("70"),
                activeOrders.get(0).getProcessRemainingQuantities().get(0).getAllocatedQuantity());
        assertEquals(new BigDecimal("30.000000"),
                activeOrders.get(0).getProcessRemainingQuantities().get(0).getRemainingQuantity());
        assertEquals(new BigDecimal("0.000000"),
                activeOrders.get(0).getProcessRemainingQuantities().get(1).getRemainingQuantity());
    }

    @Test
    void shouldRecalculateProductionProgressFromCurrentAllocationAfterQuantityReduction() {
        List<MesProcessPoolActiveOrderDO> activeOrderRows = List.of(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .erpFixedQuantitySnapshot(new BigDecimal("200"))
                .activeStatus("ACTIVE")
                .joinedAt(LocalDateTime.of(2026, 7, 31, 8, 30))
                .build());
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(activeOrderRows);
        when(routeMapper.selectBatchIds(List.of(922119L))).thenReturn(List.of(MesProRouteDO.builder()
                .id(922119L)
                .name("按压式球囊扩充压力泵工艺路线")
                .build()));
        when(routeVersionMapper.selectBatchIds(List.of(448L))).thenReturn(List.of(MesProRouteVersionDO.builder()
                .id(448L)
                .routeId(922119L)
                .versionNo("V1")
                .routeSnapshotJson(activeRouteSnapshotJson(10))
                .build()));
        when(workOrderMapper.selectBatchIds(List.of(9001L))).thenReturn(List.of(confirmedWorkOrder()));
        when(itemMapper.selectBatchIds(List.of(1001L))).thenReturn(List.of(MesMdItemDO.builder()
                .id(1001L)
                .code("AW.107.02.01.2010")
                .name("球囊扩张压力泵")
                .build()));
        when(processSnapshotMapper.selectListByActiveOrderIds(List.of(8101L)))
                .thenReturn(formalRouteProcessSnapshots(8101L, 9001L, 10));
        when(reportAllocationMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of(
                allocation(8101L, 9001L, 928601L, 6001L, "199"),
                allocation(8101L, 9001L, 928602L, 6002L, "200")));
        when(pqcInspectionTaskMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of());

        List<MesTeamLeaderActiveOrderRow> activeOrders = service.listActiveOrders(3001L);

        assertEquals(1, activeOrders.size());
        assertEquals(new BigDecimal("10.000000"), activeOrders.get(0).getProductionProgressPercent());
        verify(reportAllocationMapper).selectListByActiveOrderIds(List.of(8101L));
    }

    @Test
    void shouldCalculateProductionProgressFromFormalRouteWhenActiveOrderSnapshotIsIncomplete() {
        List<MesProcessPoolActiveOrderDO> activeOrderRows = List.of(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .erpFixedQuantitySnapshot(new BigDecimal("200"))
                .activeStatus("ACTIVE")
                .joinedAt(LocalDateTime.of(2026, 7, 31, 8, 30))
                .build());
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(activeOrderRows);
        when(routeMapper.selectBatchIds(List.of(922119L))).thenReturn(List.of(MesProRouteDO.builder()
                .id(922119L)
                .name("按压式球囊扩充压力泵工艺路线")
                .build()));
        when(routeVersionMapper.selectBatchIds(List.of(448L))).thenReturn(List.of(MesProRouteVersionDO.builder()
                .id(448L)
                .routeId(922119L)
                .versionNo("V1")
                .routeSnapshotJson(activeRouteSnapshotJson(10))
                .build()));
        when(workOrderMapper.selectBatchIds(List.of(9001L))).thenReturn(List.of(confirmedWorkOrder()));
        when(itemMapper.selectBatchIds(List.of(1001L))).thenReturn(List.of(MesMdItemDO.builder()
                .id(1001L)
                .code("AW.107.02.01.2010")
                .name("球囊扩张压力泵")
                .build()));
        when(processSnapshotMapper.selectListByActiveOrderIds(List.of(8101L)))
                .thenReturn(formalRouteProcessSnapshots(8101L, 9001L, 1));
        when(reportAllocationMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of(
                allocation(8101L, 9001L, 928601L, 6001L, "200")));
        when(pqcInspectionTaskMapper.selectListByActiveOrderIds(List.of(8101L))).thenReturn(List.of());

        List<MesTeamLeaderActiveOrderRow> activeOrders = service.listActiveOrders(3001L);

        assertEquals(1, activeOrders.size());
        assertEquals(new BigDecimal("10.000000"), activeOrders.get(0).getProductionProgressPercent());
        assertEquals(new BigDecimal("0.000000"), activeOrders.get(0).getInspectionProgressPercent());
    }

    @Test
    void shouldFailActiveOrderListWhenFormalRouteIsMissing() {
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(List.of(
                MesProcessPoolActiveOrderDO.builder()
                        .id(8101L)
                        .routeId(922119L)
                        .routeVersionId(448L)
                        .build()));
        when(routeMapper.selectBatchIds(List.of(922119L))).thenReturn(List.of());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.listActiveOrders(3001L));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_NOT_EXISTS.getCode(), ex.getCode());
        verify(routeVersionMapper, never()).selectBatchIds(any());
    }

    @Test
    void shouldFailActiveOrderListWhenVersionDoesNotBelongToRoute() {
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(List.of(
                MesProcessPoolActiveOrderDO.builder()
                        .id(8101L)
                        .routeId(922119L)
                        .routeVersionId(448L)
                        .build()));
        when(routeMapper.selectBatchIds(List.of(922119L))).thenReturn(List.of(MesProRouteDO.builder()
                .id(922119L)
                .name("按压式球囊扩充压力泵工艺路线")
                .build()));
        when(routeVersionMapper.selectBatchIds(List.of(448L))).thenReturn(List.of(MesProRouteVersionDO.builder()
                .id(448L)
                .routeId(922120L)
                .versionNo("V1")
                .build()));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.listActiveOrders(3001L));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_NOT_EXISTS.getCode(), ex.getCode());
    }

    private static MesTeamLeaderActiveOrderAddReqBO activeOrderReq() {
        return MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .build();
    }

    private void stubWorkOrderExists(MesProWorkOrderDO workOrder) {
        when(workOrderService.validateWorkOrderExists(9001L)).thenReturn(workOrder);
    }

    private static MesProWorkOrderDO confirmedWorkOrder() {
        return confirmedWorkOrder(new BigDecimal("200"));
    }

    private static MesProWorkOrderDO confirmedWorkOrder(BigDecimal quantity) {
        return confirmedWorkOrder(9001L, "WO-9001", quantity, 1001L);
    }

    private static MesProWorkOrderDO confirmedWorkOrderWithPlannedStart() {
        return MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .productId(1001L)
                .quantity(new BigDecimal("200"))
                .plannedStartTime(LocalDateTime.of(2026, 8, 5, 8, 0))
                .build();
    }

    private static MesProWorkOrderDO confirmedWorkOrder(Long id, String code, BigDecimal quantity, Long productId) {
        return MesProWorkOrderDO.builder()
                .id(id)
                .code(code)
                .batchCode("BATCH-" + id)
                .productId(productId)
                .quantity(quantity)
                .build();
    }

    private void stubEffectiveSchedules(MesProScheduleOrderDO... schedules) {
        stubEffectiveSchedulesFor(9001L, schedules);
    }

    private static List<MesProcessPoolActiveOrderProcessSnapshotDO> processSnapshots(Long activeOrderId,
                                                                                     Long workOrderId,
                                                                                     int processCount) {
        return java.util.stream.IntStream.rangeClosed(1, processCount)
                .mapToObj(index -> MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                        .activeOrderId(activeOrderId)
                        .workOrderId(workOrderId)
                        .routeId(922119L)
                        .routeVersionId(448L)
                        .routeProcessId(5000L + index)
                        .processId(6000L + index)
                        .plannedQuantitySnapshot(new BigDecimal("200.000000"))
                        .build())
                .toList();
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO processSnapshot(Long activeOrderId, Long workOrderId,
                                                                               Long routeProcessId, Long processId,
                                                                               String plannedQuantity) {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .activeOrderId(activeOrderId)
                .workOrderId(workOrderId)
                .routeId(922119L)
                .routeVersionId(448L)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .plannedQuantitySnapshot(new BigDecimal(plannedQuantity))
                .build();
    }

    private static List<MesProcessPoolActiveOrderProcessSnapshotDO> formalRouteProcessSnapshots(Long activeOrderId,
                                                                                                 Long workOrderId,
                                                                                                 int processCount) {
        return java.util.stream.IntStream.rangeClosed(1, processCount)
                .mapToObj(index -> MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                        .activeOrderId(activeOrderId)
                        .workOrderId(workOrderId)
                        .routeId(922119L)
                        .routeVersionId(448L)
                        .routeProcessId(928600L + index)
                        .processId(6000L + index)
                        .plannedQuantitySnapshot(new BigDecimal("200.000000"))
                        .build())
                .toList();
    }

    private static MesProcessPoolReportAllocationDO allocation(Long activeOrderId, Long workOrderId,
                                                               Long routeProcessId, Long processId,
                                                               String quantity) {
        return MesProcessPoolReportAllocationDO.builder()
                .activeOrderId(activeOrderId)
                .workOrderId(workOrderId)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .allocatedQuantity(new BigDecimal(quantity))
                .lifecycleStatus(MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT)
                .build();
    }

    private static MesPqcInspectionTaskDO pqcTask(Long activeOrderId, Long routeProcessId, Long processId,
                                                   String status) {
        return MesPqcInspectionTaskDO.builder()
                .activeOrderId(activeOrderId)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .taskStatus(status)
                .build();
    }

    private void stubEffectiveSchedulesFor(Long workOrderId, MesProScheduleOrderDO... schedules) {
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(List.of(workOrderId))).thenReturn(List.of(schedules));
    }

    private void stubSuccessfulInsertAndProcesses(List<MesProScheduleOrderProcessDO> processes) {
        stubSuccessfulActiveOrderInsert();
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(7701L)).thenReturn(processes);
    }

    private void stubSuccessfulActiveOrderInsert() {
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolActiveOrderDO.class).setId(8101L);
            return 1;
        });
        when(processSnapshotMapper.insertBatch(any())).thenReturn(Boolean.TRUE);
    }

    private void stubUnscheduledActiveRoute() {
        stubUnscheduledActiveRoute(activeRouteSnapshotJson());
    }

    private void stubUnscheduledActiveRoute(String routeSnapshotJson) {
        when(routeProductMapper.selectListByItemIds(List.of(1001L))).thenReturn(List.of(
                MesProRouteProductDO.builder().id(7001L).itemId(1001L).routeId(922119L).build()));
        when(routeVersionMapper.selectListByRouteIds(List.of(922119L))).thenReturn(List.of(
                activeRouteVersion(routeSnapshotJson)));
    }

    private static MesProRouteVersionDO activeRouteVersion(String routeSnapshotJson) {
        return MesProRouteVersionDO.builder()
                .id(448L)
                .routeId(922119L)
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .routeSnapshotJson(routeSnapshotJson)
                .build();
    }

    private static String activeRouteSnapshotJson() {
        return """
                {
                  "configSnapshots": {
                    "flowGraph": {
                      "nodes": [
                        {"routeProcessId": 928609, "processId": 6001, "sort": 10}
                      ]
                    },
                    "scheduleUseConfigs": [
                      {
                        "routeId": 922119,
                        "routeProcessId": 928609,
                        "useType": "SCHEDULE",
                        "enabled": true,
                        "productionQuantityFactor": 1.000000
                      }
                    ]
                  }
                }
                """;
    }

    private static String activeRouteSnapshotJsonWithoutProductionQuantityFactor() {
        return """
                {
                  "configSnapshots": {
                    "flowGraph": {
                      "nodes": [
                        {"routeProcessId": 928609, "processId": 6001, "sort": 10}
                      ]
                    },
                    "scheduleUseConfigs": [
                      {
                        "routeId": 922119,
                        "routeProcessId": 928609,
                        "useType": "SCHEDULE",
                        "enabled": true
                      }
                    ]
                  }
                }
                """;
    }

    private static MesProScheduleOrderDO effectiveSchedule(Long id, Long routeId, Long routeVersionId) {
        return effectiveSchedule(id, 9001L, routeId, routeVersionId);
    }

    private static MesProScheduleOrderDO effectiveSchedule(Long id, Long workOrderId, Long routeId,
                                                           Long routeVersionId) {
        return MesProScheduleOrderDO.builder()
                .id(id)
                .workOrderId(workOrderId)
                .productId(1001L)
                .routeId(routeId)
                .routeVersionId(routeVersionId)
                .build();
    }

    private static MesProcessPoolActiveOrderDO existingActiveOrder(Long id, String status, Integer version) {
        return MesProcessPoolActiveOrderDO.builder()
                .id(id)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .activeStatus(status)
                .businessStatus(status)
                .dccProjectCodeId(147L)
                .qaRegulationId(9901L)
                .qaRegulationVersionId(9902L)
                .version(version)
                .removedAt("REMOVED".equals(status) ? LocalDateTime.of(2026, 8, 4, 10, 30) : null)
                .build();
    }

    private void verifyNoActiveOrderWrites() {
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(activeOrderMapper, never()).reactivateRemovedActiveOrder(any(), any(), any(), any(), any());
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(pqcInspectionTaskMapper, never()).insert(any(MesPqcInspectionTaskDO.class));
        verify(auditMapper, never()).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    private static MesProScheduleOrderProcessDO scheduleProcess(Long routeProcessId, Long processId, String factor,
                                                                String plannedQuantity) {
        return scheduleProcess(7701L, routeProcessId, processId, factor, plannedQuantity);
    }

    private static MesProScheduleOrderProcessDO scheduleProcess(Long scheduleOrderId, Long routeProcessId,
                                                                Long processId, String factor,
                                                                String plannedQuantity) {
        return scheduleProcess(scheduleOrderId, routeProcessId, processId, factor, plannedQuantity,
                LocalDate.of(2026, 8, 5));
    }

    private static MesProScheduleOrderProcessDO scheduleProcessWithoutPlanDate(Long routeProcessId, Long processId,
                                                                               String factor,
                                                                               String plannedQuantity) {
        return scheduleProcess(7701L, routeProcessId, processId, factor, plannedQuantity, null);
    }

    private static MesProScheduleOrderProcessDO scheduleProcess(Long scheduleOrderId, Long routeProcessId,
                                                                Long processId, String factor,
                                                                String plannedQuantity, LocalDate planDate) {
        return MesProScheduleOrderProcessDO.builder()
                .scheduleOrderId(scheduleOrderId)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .enabled(Boolean.TRUE)
                .planDate(planDate)
                .productionQuantityFactor(new BigDecimal(factor))
                .plannedQuantity(new BigDecimal(plannedQuantity))
                .build();
    }

    private static MesQaInspectionRegulationDO publishedRegulation(Long versionId) {
        return publishedRegulation(versionId, 928609L, 6001L);
    }

    private static MesQaInspectionRegulationDO publishedRegulation(Long versionId, Long routeProcessId,
                                                                   Long processId) {
        return publishedRegulation(versionId, 1001L, 922119L, 448L, routeProcessId, processId);
    }

    private static MesQaInspectionRegulationDO publishedRegulation(Long versionId, Long productId, Long routeId,
                                                                   Long routeVersionId, Long routeProcessId,
                                                                   Long processId) {
        return MesQaInspectionRegulationDO.builder()
                .id(9901L)
                .dccProjectCodeId(147L)
                .productId(productId)
                .routeId(routeId)
                .routeVersionId(routeVersionId)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .ownerModule(MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA)
                .lifecycleStatus("PUBLISHED")
                .currentVersionId(versionId)
                .build();
    }

    private void stubCandidatePqcPrerequisites(MesQaInspectionRegulationDO... regulations) {
        stubFormalRouteQaContext(1001L, 448L, regulations);
    }

    private void stubFormalRouteQaContext(Long workOrderProductId, Long activeRouteVersionId,
                                          MesQaInspectionRegulationDO... regulations) {
        stubFormalRouteQaContext(workOrderProductId, activeRouteVersionId, activeRouteSnapshotJson(List.of(regulations)),
                regulations);
    }

    private void stubFormalRouteQaContext(Long workOrderProductId, Long activeRouteVersionId, String routeSnapshotJson,
                                          MesQaInspectionRegulationDO... regulations) {
        List<MesQaInspectionRegulationDO> regulationList = List.of(regulations);
        LinkedHashSet<Long> routeItemIds = new LinkedHashSet<>();
        routeItemIds.add(workOrderProductId);
        regulationList.stream().map(MesQaInspectionRegulationDO::getProductId).forEach(routeItemIds::add);
        routeItemIds.add(924005L);
        List<MesProRouteProductDO> routeProducts = routeItemIds.stream()
                .map(itemId -> MesProRouteProductDO.builder()
                        .id(7000L + itemId)
                        .routeId(922119L)
                        .itemId(itemId)
                        .build())
                .toList();
        lenient().when(routeProductMapper.selectListByItemIds(any())).thenAnswer(invocation -> {
            Collection<Long> requestedItemIds = invocation.getArgument(0);
            return requestedItemIds.contains(workOrderProductId)
                    ? List.of(MesProRouteProductDO.builder()
                    .id(7001L)
                    .routeId(922119L)
                    .itemId(workOrderProductId)
                    .build())
                    : List.of();
        });
        lenient().when(routeProductMapper.selectListByRouteIds(any())).thenReturn(routeProducts);
        lenient().when(routeMapper.selectBatchIds(any())).thenReturn(List.of(MesProRouteDO.builder()
                .id(922119L)
                .code("ROUTE-922119")
                .build()));
        when(routeVersionMapper.selectListByRouteIds(any())).thenReturn(List.of(MesProRouteVersionDO.builder()
                .id(activeRouteVersionId)
                .routeId(922119L)
                .versionNo("V-ACTIVE")
                .active(Boolean.TRUE)
                .lifecycleStatus("ACTIVE")
                .routeSnapshotJson(routeSnapshotJson)
                .build()));
        lenient().when(itemMapper.selectBatchIds(any())).thenReturn(List.of(MesMdItemDO.builder()
                .id(924005L)
                .code("ID")
                .name("球囊扩张压力泵")
                .build()));
        lenient().when(routeDccProjectBindingMapper.selectCurrentByRouteId(922119L)).thenReturn(MesRouteDccProjectBindingDO.builder()
                .id(61001L)
                .routeId(922119L)
                .dccProjectCodeId(147L)
                .version(1L)
                .build());
        lenient().when(dccProjectCodeMapper.selectById(147L)).thenReturn(DccProjectCodeDO.builder()
                .id(147L)
                .productMasterId(11L)
                .projectCode("ID")
                .projectName("球囊扩张压力泵")
                .status("ENABLE")
                .build());
        lenient().when(inspectionRegulationMapper.selectListByDccProjectCodeIds(any())).thenReturn(regulationList);
        if (!regulationList.isEmpty()) {
            lenient().when(inspectionRegulationVersionMapper.selectBatchIds(any())).thenAnswer(invocation ->
                    regulationList.stream()
                            .map(MesQaInspectionRegulationDO::getCurrentVersionId)
                            .map(versionId -> publishedRegulationVersion(versionId, true, null))
                            .toList());
            lenient().when(inspectionRegulationItemMapper.selectListByVersionIds(any())).thenAnswer(invocation ->
                    regulationList.stream()
                            .map(MesQaInspectionRegulationDO::getCurrentVersionId)
                            .flatMap(versionId -> defaultPqcItems(versionId).stream())
                            .toList());
            lenient().when(inspectionRegulationProcessMapper.selectListByVersionIds(any())).thenAnswer(invocation ->
                    regulationList.stream()
                            .map(MesQaInspectionRegulationDO::getCurrentVersionId)
                            .map(MesTeamLeaderActiveOrderServiceTest::qaProcess)
                            .toList());
        }
    }

    private static MesQaInspectionRegulationVersionDO publishedRegulationVersion(
            Boolean finalInspectionApplicable, String reason) {
        return publishedRegulationVersion(9902L, finalInspectionApplicable, reason);
    }

    private static MesQaInspectionRegulationVersionDO publishedRegulationVersion(
            Long id, Boolean finalInspectionApplicable, String reason) {
        return MesQaInspectionRegulationVersionDO.builder()
                .id(id)
                .regulationId(9901L)
                .versionNo("V21-QA-1")
                .lifecycleStatus("PUBLISHED")
                .finalInspectionApplicable(finalInspectionApplicable)
                .finalInspectionNotApplicableReason(reason)
                .inspectionTypeRulesJson("""
                        [
                          {"key":"FIRST","inspectionType":"FIRST","label":"首检","required":true,"fixedQuantity":5},
                          {"key":"PATROL_AM","inspectionType":"PATROL","label":"上午巡检","required":true},
                          {"key":"PATROL_PM","inspectionType":"PATROL","label":"下午巡检","required":true},
                          {"key":"FINAL","inspectionType":"FINAL","label":"末检","required":%s,"fixedQuantity":3}
                        ]
                        """.formatted(Boolean.TRUE.equals(finalInspectionApplicable)))
                .snapshotJson("{}")
                .build();
    }

    private static List<MesQaInspectionRegulationItemDO> defaultPqcItems() {
        return defaultPqcItems(9902L);
    }

    private static String activeRouteSnapshotJson(List<MesQaInspectionRegulationDO> regulations) {
        List<MesQaInspectionRegulationDO> source = regulations == null || regulations.isEmpty()
                ? List.of(publishedRegulation(9902L)) : regulations;
        String nodes = source.stream()
                .map(regulation -> "{\"routeProcessId\":" + regulation.getRouteProcessId()
                        + ",\"processId\":" + regulation.getProcessId() + ",\"sort\":10}")
                .collect(java.util.stream.Collectors.joining(","));
        String configs = source.stream()
                .map(regulation -> "{\"routeId\":922119,\"routeProcessId\":" + regulation.getRouteProcessId()
                        + ",\"useType\":\"SCHEDULE\",\"enabled\":true,\"productionQuantityFactor\":1.000000}")
                .collect(java.util.stream.Collectors.joining(","));
        return """
                {
                  "configSnapshots": {
                    "flowGraph": {
                      "nodes": [%s]
                    },
                    "scheduleUseConfigs": [%s]
                  }
                }
                """.formatted(nodes, configs);
    }

    private static String activeRouteSnapshotJson(int processCount) {
        String nodes = java.util.stream.IntStream.rangeClosed(1, processCount)
                .mapToObj(index -> "{\"routeProcessId\":" + (928600L + index)
                        + ",\"processId\":" + (6000L + index) + ",\"sort\":" + (index * 10) + "}")
                .collect(java.util.stream.Collectors.joining(","));
        String configs = java.util.stream.IntStream.rangeClosed(1, processCount)
                .mapToObj(index -> "{\"routeId\":922119,\"routeProcessId\":" + (928600L + index)
                        + ",\"useType\":\"SCHEDULE\",\"enabled\":true,\"productionQuantityFactor\":1.000000}")
                .collect(java.util.stream.Collectors.joining(","));
        return """
                {
                  "configSnapshots": {
                    "flowGraph": {
                      "nodes": [%s]
                    },
                    "scheduleUseConfigs": [%s]
                  }
                }
                """.formatted(nodes, configs);
    }

    private static List<MesQaInspectionRegulationItemDO> defaultPqcItems(Long regulationVersionId) {
        return List.of(
                pqcItem(regulationVersionId, "FIRST", 5, null),
                pqcItem(regulationVersionId, "PATROL", null, new BigDecimal("5.000000")),
                pqcItem(regulationVersionId, "FINAL", 3, null));
    }

    private static MesQaInspectionRegulationProcessDO qaProcess(Long regulationVersionId) {
        return MesQaInspectionRegulationProcessDO.builder()
                .id(regulationVersionId + 10000L)
                .regulationVersionId(regulationVersionId)
                .processCode("ID-QA-001")
                .processName("清洗")
                .sort(1)
                .build();
    }

    private static MesQaInspectionRegulationItemDO pqcItem(String inspectionType, Integer fixedQuantity,
                                                           BigDecimal patrolRatio) {
        return pqcItem(9902L, inspectionType, fixedQuantity, patrolRatio);
    }

    private static MesQaInspectionRegulationItemDO pqcItem(Long regulationVersionId, String inspectionType,
                                                           Integer fixedQuantity, BigDecimal patrolRatio) {
        return MesQaInspectionRegulationItemDO.builder()
                .regulationVersionId(regulationVersionId)
                .qaProcessId(regulationVersionId + 10000L)
                .inspectionType(inspectionType)
                .itemCode(inspectionType + "-001")
                .itemName(inspectionType + " 检验项目")
                .inspectionMethod("目视")
                .standardText("符合规程")
                .resultType("BOOLEAN")
                .firstInspectionQuantity(fixedQuantity)
                .patrolInspectionRatio(patrolRatio)
                .build();
    }

    private static MesQaInspectionRegulationItemDO pqcItem(String itemCode, String itemName,
                                                           String inspectionType, Integer fixedQuantity,
                                                           BigDecimal patrolRatio) {
        return MesQaInspectionRegulationItemDO.builder()
                .regulationVersionId(9902L)
                .qaProcessId(19902L)
                .inspectionType(inspectionType)
                .itemCode(itemCode)
                .itemName(itemName)
                .inspectionMethod("目视")
                .standardText("符合规程")
                .resultType("BOOLEAN")
                .firstInspectionQuantity(fixedQuantity)
                .patrolInspectionRatio(patrolRatio)
                .build();
    }

    private static void assertPqcTask(MesPqcInspectionTaskDO task, String inspectionType, String inspectionRuleKey,
                                      String shiftCode, Integer plannedInspectionQuantity, LocalDate businessDate) {
        assertEquals(8101L, task.getActiveOrderId());
        assertEquals(9001L, task.getWorkOrderId());
        assertEquals(922119L, task.getRouteId());
        assertEquals(448L, task.getRouteVersionId());
        assertEquals(null, task.getRouteProcessId());
        assertEquals(null, task.getProcessId());
        assertEquals(19902L, task.getQaProcessId());
        assertEquals("FINAL".equals(inspectionType) ? "" : inspectionType + "-001", task.getQaItemCode());
        assertEquals(9902L, task.getRegulationVersionId());
        assertEquals(inspectionType, task.getInspectionType());
        assertEquals(inspectionRuleKey, task.getInspectionRuleKey());
        assertEquals(businessDate, task.getBusinessDate());
        assertEquals(shiftCode, task.getShiftCode());
        assertEquals(1, task.getRoundNo());
        assertEquals(plannedInspectionQuantity, task.getPlannedInspectionQuantity());
        assertEquals(0, task.getActualInspectionQuantity());
        assertEquals("PENDING", task.getTaskStatus());
    }

    private static void assertSnapshot(MesProcessPoolActiveOrderProcessSnapshotDO snapshot, Long activeOrderId,
                                       Long workOrderId, Long routeId, Long routeVersionId, Long routeProcessId,
                                       Long processId, String erpQuantity, String factor, String plannedQuantity) {
        assertEquals(activeOrderId, snapshot.getActiveOrderId());
        assertEquals(workOrderId, snapshot.getWorkOrderId());
        assertEquals(routeId, snapshot.getRouteId());
        assertEquals(routeVersionId, snapshot.getRouteVersionId());
        assertEquals(routeProcessId, snapshot.getRouteProcessId());
        assertEquals(processId, snapshot.getProcessId());
        assertAmount(erpQuantity, snapshot.getErpFixedQuantitySnapshot());
        assertAmount(factor, snapshot.getProductionQuantityFactorSnapshot());
        assertAmount(plannedQuantity, snapshot.getPlannedQuantitySnapshot());
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
