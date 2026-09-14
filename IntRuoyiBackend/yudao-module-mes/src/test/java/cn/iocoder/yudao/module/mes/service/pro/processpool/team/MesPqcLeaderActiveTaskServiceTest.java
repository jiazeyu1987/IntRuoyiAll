package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesPqcLeaderActiveTaskServiceTest {

    private static final long ACTIVE_ORDER_ID = 5001L;
    private static final long WORK_ORDER_ID = 1001L;
    private static final long ROUTE_ID = 2001L;
    private static final long ROUTE_VERSION_ID = 3001L;
    private static final long REGULATION_ID = 4001L;
    private static final long REGULATION_VERSION_ID = 5002L;
    private static final long QA_PROCESS_ID = 6001L;

    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    private MesPqcInspectionTaskMapper taskMapper;
    private MesProWorkOrderMapper workOrderMapper;
    private MesProRouteMapper routeMapper;
    private MesProRouteVersionMapper routeVersionMapper;
    private MesQaInspectionRegulationMapper regulationMapper;
    private MesQaInspectionRegulationVersionMapper regulationVersionMapper;
    private MesQaInspectionRegulationProcessMapper regulationProcessMapper;
    private MesPqcLeaderActiveTaskService service;

    @BeforeEach
    void setUp() {
        activeOrderMapper = mock(MesProcessPoolActiveOrderMapper.class);
        taskMapper = mock(MesPqcInspectionTaskMapper.class);
        workOrderMapper = mock(MesProWorkOrderMapper.class);
        routeMapper = mock(MesProRouteMapper.class);
        routeVersionMapper = mock(MesProRouteVersionMapper.class);
        regulationMapper = mock(MesQaInspectionRegulationMapper.class);
        regulationVersionMapper = mock(MesQaInspectionRegulationVersionMapper.class);
        regulationProcessMapper = mock(MesQaInspectionRegulationProcessMapper.class);
        service = new MesPqcLeaderActiveTaskServiceImpl(activeOrderMapper, taskMapper, workOrderMapper,
                routeMapper, routeVersionMapper, regulationMapper, regulationVersionMapper,
                regulationProcessMapper);
    }

    @Test
    void listActiveTasksUsesActiveOrdersAndNonTerminalStatusesWithFrozenVersions() {
        when(activeOrderMapper.selectActiveList()).thenReturn(List.of(activeOrder()));
        when(taskMapper.selectListByActiveOrderIdsAndStatuses(
                List.of(ACTIVE_ORDER_ID), Set.of("PENDING", "SUBMITTED")))
                .thenReturn(List.of(task(7001L, "PENDING", "PATROL_AM"),
                        task(7002L, "SUBMITTED", "FINAL")));
        stubFormalContext(REGULATION_VERSION_ID);

        List<MesPqcLeaderActiveTaskRow> result = service.listActiveTasks();

        assertEquals(List.of(7001L, 7002L), result.stream().map(MesPqcLeaderActiveTaskRow::getPqcTaskId).toList());
        assertEquals(List.of("PENDING", "SUBMITTED"),
                result.stream().map(MesPqcLeaderActiveTaskRow::getTaskStatus).toList());
        assertEquals("WO-PQC-001", result.get(0).getWorkOrderCode());
        assertEquals("QA-PQC-001", result.get(0).getQaRegulationCode());
        assertEquals("V3", result.get(0).getQaVersionNo());
        assertEquals("ROUTE-PQC", result.get(0).getRouteName());
        assertEquals("V8", result.get(0).getRouteVersionNo());
        assertEquals("清洗工序", result.get(0).getQaProcessName());
        verify(taskMapper).selectListByActiveOrderIdsAndStatuses(
                List.of(ACTIVE_ORDER_ID), Set.of("PENDING", "SUBMITTED"));
    }

    @Test
    void listActiveTasksExcludesStaleTaskWhenQaVersionDiffersFromActiveOrderSnapshot() {
        when(activeOrderMapper.selectActiveList()).thenReturn(List.of(activeOrder()));
        when(taskMapper.selectListByActiveOrderIdsAndStatuses(
                List.of(ACTIVE_ORDER_ID), Set.of("PENDING", "SUBMITTED")))
                .thenReturn(List.of(
                        task(7001L, "PENDING", "FIRST").setRegulationVersionId(9999L),
                        task(7002L, "PENDING", "PATROL_AM")));
        stubFormalContext(REGULATION_VERSION_ID);

        List<MesPqcLeaderActiveTaskRow> result = service.listActiveTasks();

        assertEquals(List.of(7002L), result.stream().map(MesPqcLeaderActiveTaskRow::getPqcTaskId).toList());
    }

    private void stubFormalContext(long regulationVersionId) {
        when(workOrderMapper.selectBatchIds(List.of(WORK_ORDER_ID)))
                .thenReturn(List.of(MesProWorkOrderDO.builder().id(WORK_ORDER_ID)
                        .code("WO-PQC-001").name("当前生产订单").build()));
        when(routeMapper.selectBatchIds(List.of(ROUTE_ID)))
                .thenReturn(List.of(MesProRouteDO.builder().id(ROUTE_ID)
                        .code("ROUTE-001").name("ROUTE-PQC").build()));
        when(routeVersionMapper.selectBatchIds(List.of(ROUTE_VERSION_ID)))
                .thenReturn(List.of(MesProRouteVersionDO.builder().id(ROUTE_VERSION_ID)
                        .routeId(ROUTE_ID).versionNo("V8").build()));
        when(regulationMapper.selectBatchIds(List.of(REGULATION_ID)))
                .thenReturn(List.of(MesQaInspectionRegulationDO.builder().id(REGULATION_ID)
                        .regulationCode("QA-PQC-001").regulationName("PQC检验规程").build()));
        when(regulationVersionMapper.selectBatchIds(List.of(regulationVersionId)))
                .thenReturn(List.of(MesQaInspectionRegulationVersionDO.builder().id(regulationVersionId)
                        .regulationId(REGULATION_ID).versionNo("V3").lifecycleStatus("PUBLISHED").build()));
        when(regulationProcessMapper.selectBatchIds(List.of(QA_PROCESS_ID)))
                .thenReturn(List.of(MesQaInspectionRegulationProcessDO.builder().id(QA_PROCESS_ID)
                        .regulationVersionId(regulationVersionId).processCode("QA-CLEAN")
                        .processName("清洗工序").sort(1).build()));
    }

    private MesProcessPoolActiveOrderDO activeOrder() {
        return MesProcessPoolActiveOrderDO.builder().id(ACTIVE_ORDER_ID)
                .workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID).routeVersionId(ROUTE_VERSION_ID)
                .qaRegulationId(REGULATION_ID).qaRegulationVersionId(REGULATION_VERSION_ID)
                .activeStatus("ACTIVE").businessStatus("ACTIVE").build();
    }

    private MesPqcInspectionTaskDO task(long id, String status, String ruleKey) {
        String inspectionType = ruleKey.startsWith("PATROL") ? "PATROL" : ruleKey;
        return MesPqcInspectionTaskDO.builder().id(id).activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID).routeVersionId(ROUTE_VERSION_ID)
                .qaProcessId(QA_PROCESS_ID).regulationVersionId(REGULATION_VERSION_ID)
                .inspectionRuleKey(ruleKey).inspectionType(inspectionType)
                .businessDate(LocalDate.of(2026, 8, 17)).shiftCode(ruleKey).roundNo(1)
                .plannedInspectionQuantity(4).actualInspectionQuantity(
                        "SUBMITTED".equals(status) ? 4 : null).taskStatus(status).build();
    }
}
