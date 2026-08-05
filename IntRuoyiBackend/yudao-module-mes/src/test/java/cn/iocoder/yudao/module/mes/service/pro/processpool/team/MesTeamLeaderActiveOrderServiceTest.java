package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
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
    private MesProcessPoolTeamMaintenanceAuditMapper auditMapper;
    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock
    private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    @Mock
    private MesActiveOrderTransferTraceService transferTraceService;
    @Mock
    private MesQaInspectionRegulationMapper inspectionRegulationMapper;
    @Mock
    private MesQaInspectionRegulationVersionMapper inspectionRegulationVersionMapper;
    @Mock
    private MesQaInspectionRegulationItemMapper inspectionRegulationItemMapper;
    @Mock
    private MesPqcInspectionTaskMapper pqcInspectionTaskMapper;

    private MesTeamLeaderActiveOrderService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderActiveOrderServiceImpl(activeOrderMapper, workOrderService, auditMapper,
                scheduleOrderMapper, scheduleOrderProcessMapper, processSnapshotMapper, transferTraceService,
                inspectionRegulationMapper, inspectionRegulationVersionMapper, inspectionRegulationItemMapper,
                pqcInspectionTaskMapper);
        lenient().when(inspectionRegulationMapper.selectPublishedByRouteProcess(any(), any(), any(), any(), any()))
                .thenReturn(publishedRegulation(9902L));
        lenient().when(inspectionRegulationVersionMapper.selectById(9902L))
                .thenReturn(publishedRegulationVersion(true, null));
        lenient().when(inspectionRegulationItemMapper.selectListByVersionId(9902L)).thenReturn(defaultPqcItems());
        lenient().when(pqcInspectionTaskMapper.selectByIdentity(any(), any(), any(), any(), any(), any()))
                .thenReturn(null);
        lenient().when(pqcInspectionTaskMapper.insert(any(MesPqcInspectionTaskDO.class))).thenReturn(1);
    }

    @Test
    void shouldAddWorkOrderToLeaderActivePoolWithJoinTime() {
        when(workOrderService.validateWorkOrderConfirmed(9001L)).thenReturn(MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .productId(1001L)
                .quantity(new BigDecimal("200"))
                .build());
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolActiveOrderDO.class).setId(8101L);
            return 1;
        });
        when(scheduleOrderMapper.selectEffectiveByWorkOrderId(9001L)).thenReturn(MesProScheduleOrderDO.builder()
                .id(7701L)
                .workOrderId(9001L)
                .productId(1001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(7701L)).thenReturn(List.of(
                scheduleProcess(928609L, 6001L, "3.000000", "600.000000"),
                scheduleProcess(928610L, 6002L, "2.000000", "400.000000")));
        when(processSnapshotMapper.insertBatch(any())).thenReturn(Boolean.TRUE);

        Long activeOrderId = service.addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());

        assertEquals(8101L, activeOrderId);
        verify(workOrderService).validateWorkOrderConfirmed(9001L);
        ArgumentCaptor<MesProcessPoolActiveOrderDO> captor =
                ArgumentCaptor.forClass(MesProcessPoolActiveOrderDO.class);
        verify(activeOrderMapper).insert(captor.capture());
        MesProcessPoolActiveOrderDO activeOrder = captor.getValue();
        assertEquals(3001L, activeOrder.getLeaderUserId());
        assertEquals(9001L, activeOrder.getWorkOrderId());
        assertEquals(922119L, activeOrder.getRouteId());
        assertEquals(448L, activeOrder.getRouteVersionId());
        assertEquals(new BigDecimal("200"), activeOrder.getErpFixedQuantitySnapshot());
        assertEquals("ACTIVE", activeOrder.getActiveStatus());
        assertEquals("ACTIVE", activeOrder.getBusinessStatus());
        assertEquals(0, activeOrder.getVersion());
        assertNotNull(activeOrder.getJoinedAt());
        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Collection<MesProcessPoolActiveOrderProcessSnapshotDO>> snapshotCaptor =
                org.mockito.ArgumentCaptor.forClass(Collection.class);
        verify(processSnapshotMapper).insertBatch(snapshotCaptor.capture());
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = List.copyOf(snapshotCaptor.getValue());
        assertEquals(2, snapshots.size());
        assertSnapshot(snapshots.get(0), 8101L, 9001L, 922119L, 448L, 928609L, 6001L, "200", "3.000000", "600.000000");
        assertSnapshot(snapshots.get(1), 8101L, 9001L, 922119L, 448L, 928610L, 6002L, "200", "2.000000", "400.000000");
        verify(auditMapper).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldReturnExistingActiveOrderWhenSameWorkOrderRouteVersionAlreadyActive() {
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(9001L, 922119L, 448L))
                .thenReturn(MesProcessPoolActiveOrderDO.builder()
                        .id(8101L)
                        .leaderUserId(3001L)
                        .workOrderId(9001L)
                        .routeId(922119L)
                        .routeVersionId(448L)
                        .activeStatus("ACTIVE")
                        .businessStatus("ACTIVE")
                        .build());

        Long activeOrderId = service.addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());

        assertEquals(8101L, activeOrderId);
        verify(activeOrderMapper).selectActiveByWorkOrderRouteVersion(9001L, 922119L, 448L);
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(auditMapper, never()).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldReturnExistingActiveOrderWhenConcurrentInsertHitsUniqueKey() {
        MesProcessPoolActiveOrderDO existing = MesProcessPoolActiveOrderDO.builder()
                .id(8102L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .activeStatus("ACTIVE")
                .businessStatus("ACTIVE")
                .build();
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(9001L, 922119L, 448L))
                .thenReturn(null, existing);
        when(workOrderService.validateWorkOrderConfirmed(9001L)).thenReturn(MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .productId(1001L)
                .quantity(new BigDecimal("200"))
                .build());
        when(scheduleOrderMapper.selectEffectiveByWorkOrderId(9001L)).thenReturn(MesProScheduleOrderDO.builder()
                .id(7701L)
                .workOrderId(9001L)
                .productId(1001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class)))
                .thenThrow(new DuplicateKeyException("uk_mes_pp_active_order"));

        Long activeOrderId = service.addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());

        assertEquals(8102L, activeOrderId);
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(auditMapper, never()).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldRejectConflictingRouteBeforeInsertingActiveOrder() {
        when(workOrderService.validateWorkOrderConfirmed(9001L)).thenReturn(MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .productId(1001L)
                .quantity(new BigDecimal("200"))
                .build());
        when(scheduleOrderMapper.selectEffectiveByWorkOrderId(9001L)).thenReturn(MesProScheduleOrderDO.builder()
                .id(7701L)
                .workOrderId(9001L)
                .productId(1001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(
                MesTeamLeaderActiveOrderAddReqBO.builder()
                        .leaderUserId(3001L)
                        .workOrderId(9001L)
                        .routeId(922120L)
                        .routeVersionId(449L)
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED.getCode(), ex.getCode());
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(auditMapper, never()).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldRejectUnconfirmedWorkOrderBeforeAddingActiveOrder() {
        when(workOrderService.validateWorkOrderConfirmed(9001L))
                .thenThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil
                        .exception(ErrorCodeConstants.PRO_WORK_ORDER_NOT_CONFIRMED));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(
                MesTeamLeaderActiveOrderAddReqBO.builder()
                        .leaderUserId(3001L)
                        .workOrderId(9001L)
                        .routeId(922119L)
                        .routeVersionId(448L)
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_WORK_ORDER_NOT_CONFIRMED.getCode(), ex.getCode());
        verify(workOrderService).validateWorkOrderConfirmed(9001L);
        verify(scheduleOrderMapper, never()).selectEffectiveByWorkOrderId(any());
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(auditMapper, never()).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldRemoveActiveOrderWithExplicitVersionGuardWithoutDeletingHistory() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .activeStatus("ACTIVE")
                .businessStatus("ACTIVE")
                .version(7)
                .build());
        when(activeOrderMapper.removeActiveOrder(eq(8101L), eq(7), any(LocalDateTime.class))).thenReturn(1);

        service.removeActiveOrder(MesTeamLeaderActiveOrderRemoveReqBO.builder()
                .leaderUserId(3001L)
                .activeOrderId(8101L)
                .build());

        ArgumentCaptor<LocalDateTime> removedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(activeOrderMapper).removeActiveOrder(eq(8101L), eq(7), removedAtCaptor.capture());
        assertNotNull(removedAtCaptor.getValue());
        verify(activeOrderMapper, never()).updateById(any(MesProcessPoolActiveOrderDO.class));
        verify(auditMapper).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldRecordFormalTransferTraceWhenAddingActiveOrderWithTransferIds() {
        when(workOrderService.validateWorkOrderConfirmed(9001L)).thenReturn(MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .productId(1001L)
                .quantity(new BigDecimal("200"))
                .build());
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolActiveOrderDO.class).setId(8101L);
            return 1;
        });
        when(scheduleOrderMapper.selectEffectiveByWorkOrderId(9001L)).thenReturn(MesProScheduleOrderDO.builder()
                .id(7701L)
                .workOrderId(9001L)
                .productId(1001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(7701L)).thenReturn(List.of(
                scheduleProcess(928609L, 6001L, "3.000000", "600.000000")));
        when(processSnapshotMapper.insertBatch(any())).thenReturn(Boolean.TRUE);

        Long activeOrderId = service.addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .transferIds(List.of(5001L, 5002L))
                .build());

        assertEquals(8101L, activeOrderId);
        ArgumentCaptor<MesProcessPoolActiveOrderDO> activeOrderCaptor =
                ArgumentCaptor.forClass(MesProcessPoolActiveOrderDO.class);
        verify(transferTraceService).recordTransferTracesForActiveOrder(
                activeOrderCaptor.capture(), eq(List.of(5001L, 5002L)));
        assertEquals(8101L, activeOrderCaptor.getValue().getId());
        assertEquals(9001L, activeOrderCaptor.getValue().getWorkOrderId());
        assertEquals(922119L, activeOrderCaptor.getValue().getRouteId());
        assertEquals(448L, activeOrderCaptor.getValue().getRouteVersionId());
    }

    @Test
    void shouldGenerateFormalPqcTasksFromPublishedRegulationWhenAddingActiveOrder() {
        when(workOrderService.validateWorkOrderConfirmed(9001L)).thenReturn(MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .productId(1001L)
                .quantity(new BigDecimal("301"))
                .build());
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolActiveOrderDO.class).setId(8101L);
            return 1;
        });
        when(scheduleOrderMapper.selectEffectiveByWorkOrderId(9001L)).thenReturn(MesProScheduleOrderDO.builder()
                .id(7701L)
                .workOrderId(9001L)
                .productId(1001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(7701L)).thenReturn(List.of(
                scheduleProcess(928609L, 6001L, "1.000000", "301.000000")));
        when(processSnapshotMapper.insertBatch(any())).thenReturn(Boolean.TRUE);

        Long activeOrderId = service.addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());

        assertEquals(8101L, activeOrderId);
        ArgumentCaptor<MesPqcInspectionTaskDO> taskCaptor =
                ArgumentCaptor.forClass(MesPqcInspectionTaskDO.class);
        verify(pqcInspectionTaskMapper, times(4)).insert(taskCaptor.capture());
        List<MesPqcInspectionTaskDO> tasks = taskCaptor.getAllValues();
        assertPqcTask(tasks.get(0), "FIRST", "FIRST", 5);
        assertPqcTask(tasks.get(1), "PATROL", "AM", 16);
        assertPqcTask(tasks.get(2), "PATROL", "PM", 16);
        assertPqcTask(tasks.get(3), "FINAL", "FINAL", 3);
    }

    @Test
    void shouldSkipFinalPqcTaskWhenPublishedRegulationMarksFinalInspectionNotApplicable() {
        when(inspectionRegulationVersionMapper.selectById(9902L))
                .thenReturn(publishedRegulationVersion(false, "该工序后续 OQC 覆盖最终包装确认"));
        when(inspectionRegulationItemMapper.selectListByVersionId(9902L)).thenReturn(List.of(
                pqcItem("FIRST", 5, null),
                pqcItem("PATROL", null, new BigDecimal("0.050000"))));
        when(workOrderService.validateWorkOrderConfirmed(9001L)).thenReturn(MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .productId(1001L)
                .quantity(new BigDecimal("301"))
                .build());
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolActiveOrderDO.class).setId(8101L);
            return 1;
        });
        when(scheduleOrderMapper.selectEffectiveByWorkOrderId(9001L)).thenReturn(MesProScheduleOrderDO.builder()
                .id(7701L)
                .workOrderId(9001L)
                .productId(1001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(7701L)).thenReturn(List.of(
                scheduleProcess(928609L, 6001L, "1.000000", "301.000000")));
        when(processSnapshotMapper.insertBatch(any())).thenReturn(Boolean.TRUE);

        Long activeOrderId = service.addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());

        assertEquals(8101L, activeOrderId);
        ArgumentCaptor<MesPqcInspectionTaskDO> taskCaptor =
                ArgumentCaptor.forClass(MesPqcInspectionTaskDO.class);
        verify(pqcInspectionTaskMapper, times(3)).insert(taskCaptor.capture());
        List<MesPqcInspectionTaskDO> tasks = taskCaptor.getAllValues();
        assertPqcTask(tasks.get(0), "FIRST", "FIRST", 5);
        assertPqcTask(tasks.get(1), "PATROL", "AM", 16);
        assertPqcTask(tasks.get(2), "PATROL", "PM", 16);
    }

    @Test
    void shouldRejectActiveOrderWhenPublishedRegulationMissingFinalApplicability() {
        when(inspectionRegulationVersionMapper.selectById(9902L))
                .thenReturn(publishedRegulationVersion(null, null));
        when(workOrderService.validateWorkOrderConfirmed(9001L)).thenReturn(MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .productId(1001L)
                .quantity(new BigDecimal("301"))
                .build());
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolActiveOrderDO.class).setId(8101L);
            return 1;
        });
        when(scheduleOrderMapper.selectEffectiveByWorkOrderId(9001L)).thenReturn(MesProScheduleOrderDO.builder()
                .id(7701L)
                .workOrderId(9001L)
                .productId(1001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(7701L)).thenReturn(List.of(
                scheduleProcess(928609L, 6001L, "1.000000", "301.000000")));
        when(processSnapshotMapper.insertBatch(any())).thenReturn(Boolean.TRUE);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(
                MesTeamLeaderActiveOrderAddReqBO.builder()
                        .leaderUserId(3001L)
                        .workOrderId(9001L)
                        .routeId(922119L)
                        .routeVersionId(448L)
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED.getCode(), ex.getCode());
        verify(pqcInspectionTaskMapper, never()).insert(any(MesPqcInspectionTaskDO.class));
    }

    @Test
    void shouldRejectActiveOrderWhenPublishedPqcRegulationMissing() {
        when(workOrderService.validateWorkOrderConfirmed(9001L)).thenReturn(MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .productId(1001L)
                .quantity(new BigDecimal("301"))
                .build());
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolActiveOrderDO.class).setId(8101L);
            return 1;
        });
        when(scheduleOrderMapper.selectEffectiveByWorkOrderId(9001L)).thenReturn(MesProScheduleOrderDO.builder()
                .id(7701L)
                .workOrderId(9001L)
                .productId(1001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(7701L)).thenReturn(List.of(
                scheduleProcess(928609L, 6001L, "1.000000", "301.000000")));
        when(processSnapshotMapper.insertBatch(any())).thenReturn(Boolean.TRUE);
        when(inspectionRegulationMapper.selectPublishedByRouteProcess(1001L, 922119L, 448L, 928609L, 6001L))
                .thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(
                MesTeamLeaderActiveOrderAddReqBO.builder()
                        .leaderUserId(3001L)
                        .workOrderId(9001L)
                        .routeId(922119L)
                        .routeVersionId(448L)
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED.getCode(), ex.getCode());
        verify(pqcInspectionTaskMapper, never()).insert(any(MesPqcInspectionTaskDO.class));
    }

    @Test
    void shouldRejectActiveOrderWhenPqcTaskIdentityAlreadyExists() {
        when(workOrderService.validateWorkOrderConfirmed(9001L)).thenReturn(MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .productId(1001L)
                .quantity(new BigDecimal("301"))
                .build());
        when(activeOrderMapper.insert(any(MesProcessPoolActiveOrderDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolActiveOrderDO.class).setId(8101L);
            return 1;
        });
        when(scheduleOrderMapper.selectEffectiveByWorkOrderId(9001L)).thenReturn(MesProScheduleOrderDO.builder()
                .id(7701L)
                .workOrderId(9001L)
                .productId(1001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(7701L)).thenReturn(List.of(
                scheduleProcess(928609L, 6001L, "1.000000", "301.000000")));
        when(processSnapshotMapper.insertBatch(any())).thenReturn(Boolean.TRUE);
        when(pqcInspectionTaskMapper.selectByIdentity(8101L, 928609L, "PATROL",
                LocalDate.of(2026, 8, 5), "PM", 1))
                .thenReturn(MesPqcInspectionTaskDO.builder().id(99001L).build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.addActiveOrder(
                MesTeamLeaderActiveOrderAddReqBO.builder()
                        .leaderUserId(3001L)
                        .workOrderId(9001L)
                        .routeId(922119L)
                        .routeVersionId(448L)
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PQC_INSPECTION_TASK_IDENTITY_CONFLICT.getCode(), ex.getCode());
        verify(pqcInspectionTaskMapper, times(2)).insert(any(MesPqcInspectionTaskDO.class));
    }

    @Test
    void shouldRejectRemoveWhenVersionGuardCannotUpdateActiveOrder() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .activeStatus("ACTIVE")
                .businessStatus("ACTIVE")
                .version(7)
                .build());
        when(activeOrderMapper.removeActiveOrder(eq(8101L), eq(7), any(LocalDateTime.class))).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.removeActiveOrder(
                MesTeamLeaderActiveOrderRemoveReqBO.builder()
                        .leaderUserId(3001L)
                        .activeOrderId(8101L)
                        .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS.getCode(), ex.getCode());
        verify(activeOrderMapper).removeActiveOrder(eq(8101L), eq(7), any(LocalDateTime.class));
        verify(activeOrderMapper, never()).updateById(any(MesProcessPoolActiveOrderDO.class));
        verify(auditMapper, never()).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldReactivateRemovedActiveOrderWhenSameWorkOrderRouteVersionIsJoinedAgain() {
        when(activeOrderMapper.selectActiveByWorkOrderRouteVersion(9001L, 922119L, 448L)).thenReturn(null);
        when(workOrderService.validateWorkOrderConfirmed(9001L)).thenReturn(MesProWorkOrderDO.builder()
                .id(9001L)
                .code("WO-9001")
                .productId(1001L)
                .quantity(new BigDecimal("200"))
                .build());
        when(scheduleOrderMapper.selectEffectiveByWorkOrderId(9001L)).thenReturn(MesProScheduleOrderDO.builder()
                .id(7701L)
                .workOrderId(9001L)
                .productId(1001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());
        when(activeOrderMapper.selectRemovedByWorkOrderRouteVersion(9001L, 922119L, 448L))
                .thenReturn(MesProcessPoolActiveOrderDO.builder()
                        .id(8101L)
                        .leaderUserId(3001L)
                        .workOrderId(9001L)
                        .routeId(922119L)
                        .routeVersionId(448L)
                        .activeStatus("REMOVED")
                        .businessStatus("REMOVED")
                        .version(7)
                        .removedAt(LocalDateTime.of(2026, 8, 4, 10, 30))
                        .build());
        when(activeOrderMapper.reactivateRemovedActiveOrder(any(), any(), any(), any())).thenReturn(1);

        Long activeOrderId = service.addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .build());

        assertEquals(8101L, activeOrderId);
        verify(workOrderService).validateWorkOrderConfirmed(9001L);
        verify(scheduleOrderMapper).selectEffectiveByWorkOrderId(9001L);
        verify(activeOrderMapper).reactivateRemovedActiveOrder(any(), any(), any(), any());
        verify(activeOrderMapper, never()).insert(any(MesProcessPoolActiveOrderDO.class));
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(auditMapper).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldListActiveOrdersOnlyForCurrentLeaderInFifoOrder() {
        List<MesProcessPoolActiveOrderDO> expected = List.of(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .activeStatus("ACTIVE")
                .joinedAt(LocalDateTime.of(2026, 7, 31, 8, 30))
                .build());
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(expected);

        List<MesProcessPoolActiveOrderDO> activeOrders = service.listActiveOrders(3001L);

        assertEquals(expected, activeOrders);
        verify(activeOrderMapper).selectActiveListByLeader(3001L);
        verify(activeOrderMapper, never()).selectActiveList();
    }

    @Test
    void shouldListActiveOrdersWithSingleActiveOrderQueryForDailyClosePerformance() {
        List<MesProcessPoolActiveOrderDO> expected = List.of(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .activeStatus("ACTIVE")
                .joinedAt(LocalDateTime.of(2026, 7, 31, 8, 30))
                .build());
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(expected);

        List<MesProcessPoolActiveOrderDO> activeOrders = service.listActiveOrders(3001L);

        assertEquals(expected, activeOrders);
        verify(activeOrderMapper).selectActiveListByLeader(3001L);
        verify(activeOrderMapper, never()).selectActiveList();
        verify(scheduleOrderProcessMapper, never()).selectListByScheduleOrderId(any());
        verify(processSnapshotMapper, never()).insertBatch(any());
        verify(scheduleOrderMapper, never()).selectEffectiveByWorkOrderId(any());
    }

    private static MesProScheduleOrderProcessDO scheduleProcess(Long routeProcessId, Long processId, String factor,
                                                                String plannedQuantity) {
        return MesProScheduleOrderProcessDO.builder()
                .routeProcessId(routeProcessId)
                .processId(processId)
                .enabled(Boolean.TRUE)
                .planDate(LocalDate.of(2026, 8, 5))
                .productionQuantityFactor(new BigDecimal(factor))
                .plannedQuantity(new BigDecimal(plannedQuantity))
                .build();
    }

    private static MesQaInspectionRegulationDO publishedRegulation(Long versionId) {
        return MesQaInspectionRegulationDO.builder()
                .id(9901L)
                .productId(1001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .routeProcessId(928609L)
                .processId(6001L)
                .lifecycleStatus("PUBLISHED")
                .currentVersionId(versionId)
                .build();
    }

    private static MesQaInspectionRegulationVersionDO publishedRegulationVersion(
            Boolean finalInspectionApplicable, String reason) {
        return MesQaInspectionRegulationVersionDO.builder()
                .id(9902L)
                .regulationId(9901L)
                .versionNo("V21-QA-1")
                .lifecycleStatus("PUBLISHED")
                .finalInspectionApplicable(finalInspectionApplicable)
                .finalInspectionNotApplicableReason(reason)
                .snapshotJson("{}")
                .build();
    }

    private static List<MesQaInspectionRegulationItemDO> defaultPqcItems() {
        return List.of(
                pqcItem("FIRST", 5, null),
                pqcItem("PATROL", null, new BigDecimal("0.050000")),
                pqcItem("FINAL", 3, null));
    }

    private static MesQaInspectionRegulationItemDO pqcItem(String inspectionType, Integer fixedQuantity,
                                                           BigDecimal patrolRatio) {
        return MesQaInspectionRegulationItemDO.builder()
                .regulationVersionId(9902L)
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

    private static void assertPqcTask(MesPqcInspectionTaskDO task, String inspectionType, String shiftCode,
                                      Integer plannedInspectionQuantity) {
        assertEquals(8101L, task.getActiveOrderId());
        assertEquals(9001L, task.getWorkOrderId());
        assertEquals(922119L, task.getRouteId());
        assertEquals(448L, task.getRouteVersionId());
        assertEquals(928609L, task.getRouteProcessId());
        assertEquals(6001L, task.getProcessId());
        assertEquals(9902L, task.getRegulationVersionId());
        assertEquals(inspectionType, task.getInspectionType());
        assertEquals(LocalDate.of(2026, 8, 5), task.getBusinessDate());
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
