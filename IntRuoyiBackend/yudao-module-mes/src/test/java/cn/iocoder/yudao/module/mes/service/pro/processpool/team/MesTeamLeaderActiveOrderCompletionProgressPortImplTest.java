package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderCompletionProgressPortImplTest {

    @Mock
    private MesProcessPoolActiveOrderProcessSnapshotMapper snapshotMapper;
    @Mock
    private MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock
    private MesPqcInspectionTaskMapper taskMapper;
    @Mock
    private MesProProcessPoolEventMapper eventMapper;

    private MesTeamLeaderActiveOrderCompletionProgressPortImpl port;

    @BeforeEach
    void setUp() {
        port = new MesTeamLeaderActiveOrderCompletionProgressPortImpl(snapshotMapper, allocationMapper, taskMapper,
                eventMapper);
    }

    @Test
    void readsLockedFormalSourcesAndReturnsDualHundred() {
        MesProcessPoolActiveOrderDO order = order();
        when(snapshotMapper.selectListByActiveOrderIdForUpdate(10L))
                .thenReturn(List.of(snapshotWithOutputMaterials(101L, 10, 501L)));
        when(allocationMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(
                MesProcessPoolReportAllocationDO.builder().id(201L).activeOrderId(10L).workOrderId(30L)
                        .routeProcessId(101L).processId(1L).eventId(401L).allocatedQuantity(BigDecimal.TEN).build()));
        when(taskMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder().id(301L).activeOrderId(10L).workOrderId(30L)
                        .routeId(40L).routeVersionId(41L).routeProcessId(101L).processId(1L)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED).build()));
        when(eventMapper.selectProductionSubmitsByIdsForUpdate(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of(
                productionSubmit(401L, 101L, "{\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":10}]}")));

        MesTeamLeaderActiveOrderCompletionProgress progress = port.read(20L, order);

        assertEquals(BigDecimal.valueOf(100).setScale(6), progress.getProductionProgressPercent());
        assertEquals(BigDecimal.valueOf(100).setScale(6), progress.getInspectionProgressPercent());
        org.mockito.Mockito.verify(snapshotMapper).selectListByActiveOrderIdForUpdate(10L);
        org.mockito.Mockito.verify(allocationMapper).selectListByActiveOrderIdForUpdate(10L);
        org.mockito.Mockito.verify(taskMapper).selectListByActiveOrderIdForUpdate(10L);
    }

    @Test
    void incompleteProductionCannotPassGate() {
        MesProcessPoolActiveOrderDO order = order();
        when(snapshotMapper.selectListByActiveOrderIdForUpdate(10L))
                .thenReturn(List.of(snapshotWithOutputMaterials(101L, 10, 501L)));
        when(allocationMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(
                MesProcessPoolReportAllocationDO.builder().id(201L).activeOrderId(10L).workOrderId(30L)
                        .routeProcessId(101L).processId(1L).eventId(401L).allocatedQuantity(BigDecimal.ONE).build()));
        when(taskMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder().id(301L).activeOrderId(10L).workOrderId(30L)
                        .routeId(40L).routeVersionId(41L).routeProcessId(101L).processId(1L)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED).build()));
        when(eventMapper.selectProductionSubmitsByIdsForUpdate(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of(
                productionSubmit(401L, 101L, "{\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":1}]}")));

        assertEquals(false, port.read(20L, order).isDoubleComplete());
    }

    @Test
    void splitOutputMaterialsDoNotAccumulateAsProcessProgress() {
        MesProcessPoolActiveOrderDO order = order();
        when(snapshotMapper.selectListByActiveOrderIdForUpdate(10L))
                .thenReturn(List.of(snapshotWithOutputMaterials(101L, 100, 501L, 502L)));
        when(allocationMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(
                MesProcessPoolReportAllocationDO.builder().id(201L).activeOrderId(10L).workOrderId(30L)
                        .routeProcessId(101L).processId(1L).eventId(401L)
                        .allocatedQuantity(BigDecimal.valueOf(50)).build(),
                MesProcessPoolReportAllocationDO.builder().id(202L).activeOrderId(10L).workOrderId(30L)
                        .routeProcessId(101L).processId(1L).eventId(402L)
                        .allocatedQuantity(BigDecimal.valueOf(50)).build()));
        when(taskMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(task(301L, 101L)));
        when(eventMapper.selectProductionSubmitsByIdsForUpdate(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of(
                productionSubmit(401L, 101L, "{\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":50}]}"),
                productionSubmit(402L, 101L, "{\"materialDetails\":[{\"materialId\":502,\"outputQuantity\":50}]}")));

        MesTeamLeaderActiveOrderCompletionProgress progress = port.read(20L, order);

        assertEquals(BigDecimal.ZERO.setScale(6), progress.getProductionProgressPercent());
        assertEquals(BigDecimal.valueOf(100).setScale(6), progress.getInspectionProgressPercent());
    }

    @Test
    void allOutputMaterialsMustReachTargetBeforeProcessIsComplete() {
        MesProcessPoolActiveOrderDO order = order();
        when(snapshotMapper.selectListByActiveOrderIdForUpdate(10L))
                .thenReturn(List.of(snapshotWithOutputMaterials(101L, 100, 501L, 502L)));
        when(allocationMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(
                MesProcessPoolReportAllocationDO.builder().id(201L).activeOrderId(10L).workOrderId(30L)
                        .routeProcessId(101L).processId(1L).eventId(401L)
                        .allocatedQuantity(BigDecimal.valueOf(60)).build(),
                MesProcessPoolReportAllocationDO.builder().id(202L).activeOrderId(10L).workOrderId(30L)
                        .routeProcessId(101L).processId(1L).eventId(402L)
                        .allocatedQuantity(BigDecimal.valueOf(100)).build()), List.of(
                MesProcessPoolReportAllocationDO.builder().id(203L).activeOrderId(10L).workOrderId(30L)
                        .routeProcessId(101L).processId(1L).eventId(403L)
                        .allocatedQuantity(BigDecimal.valueOf(100)).build(),
                MesProcessPoolReportAllocationDO.builder().id(204L).activeOrderId(10L).workOrderId(30L)
                        .routeProcessId(101L).processId(1L).eventId(404L)
                        .allocatedQuantity(BigDecimal.valueOf(100)).build()));
        when(taskMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(task(301L, 101L)));
        when(eventMapper.selectProductionSubmitsByIdsForUpdate(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of(
                productionSubmit(401L, 101L, "{\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":60}]}"),
                productionSubmit(402L, 101L, "{\"materialDetails\":[{\"materialId\":502,\"outputQuantity\":100}]}")));

        assertEquals(BigDecimal.ZERO.setScale(6), port.read(20L, order).getProductionProgressPercent());

        when(eventMapper.selectProductionSubmitsByIdsForUpdate(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of(
                productionSubmit(403L, 101L, "{\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":100}]}"),
                productionSubmit(404L, 101L, "{\"materialDetails\":[{\"materialId\":502,\"outputQuantity\":100}]}")));

        assertEquals(BigDecimal.valueOf(100).setScale(6), port.read(20L, order).getProductionProgressPercent());
    }

    @Test
    void pqcTasksMustExactlyMatchFrozenProcessSnapshots() {
        MesProcessPoolActiveOrderDO order = order();
        when(snapshotMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(
                snapshotWithOutputMaterials(101L, 10, 501L), snapshotWithOutputMaterials(102L, 10, 501L)));
        when(allocationMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(
                MesProcessPoolReportAllocationDO.builder().id(201L).activeOrderId(10L).workOrderId(30L)
                        .routeProcessId(101L).processId(1L).eventId(401L).allocatedQuantity(BigDecimal.TEN).build(),
                MesProcessPoolReportAllocationDO.builder().id(202L).activeOrderId(10L).workOrderId(30L)
                        .routeProcessId(102L).processId(1L).eventId(402L).allocatedQuantity(BigDecimal.TEN).build()));
        when(taskMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(
                task(301L, 101L), task(302L, 999L)));
        when(eventMapper.selectProductionSubmitsByIdsForUpdate(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of(
                productionSubmit(401L, 101L, "{\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":10}]}"),
                productionSubmit(402L, 102L, "{\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":10}]}")));

        assertThrows(RuntimeException.class, () -> port.read(20L, order));
    }

    @Test
    void allocatedSourceOrderEventCanCompleteTheTargetOrder() {
        when(snapshotMapper.selectListByActiveOrderIdForUpdate(10L))
                .thenReturn(List.of(snapshotWithOutputMaterials(101L, 60, 501L)));
        when(allocationMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(
                MesProcessPoolReportAllocationDO.builder().id(201L).activeOrderId(10L).workOrderId(30L)
                        .routeProcessId(101L).processId(1L).eventId(401L)
                        .allocatedQuantity(BigDecimal.valueOf(60)).build()));
        when(taskMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(task(301L, 101L)));
        var source = productionSubmit(401L, 101L,
                "{\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":100}]}").setWorkOrderId(29L);
        org.mockito.Mockito.lenient().when(eventMapper.selectProductionSubmitsByIdsForUpdate(List.of(401L)))
                .thenReturn(List.of(source));

        assertEquals(BigDecimal.valueOf(100).setScale(6), port.read(20L, order()).getProductionProgressPercent());
        assertEquals(29L, source.getWorkOrderId());
    }

    @Test
    void missingFormalSnapshotFailsFast() {
        when(snapshotMapper.selectListByActiveOrderIdForUpdate(anyLong())).thenReturn(List.of());
        assertThrows(RuntimeException.class, () -> port.read(20L, order()));
    }

    private MesProcessPoolActiveOrderDO order() {
        return MesProcessPoolActiveOrderDO.builder().id(10L).leaderUserId(20L).workOrderId(30L)
                .routeId(40L).routeVersionId(41L).activeStatus("ACTIVE").version(2).build();
    }

    private MesProcessPoolActiveOrderProcessSnapshotDO snapshot(Long routeProcessId, int target) {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder().id(101L).activeOrderId(10L).workOrderId(30L)
                .routeId(40L).routeVersionId(41L).routeProcessId(routeProcessId).processId(1L)
                .plannedQuantitySnapshot(BigDecimal.valueOf(target)).build();
    }

    private MesProcessPoolActiveOrderProcessSnapshotDO snapshotWithOutputMaterials(
            Long routeProcessId, int target, Long... materialIds) {
        String outputMaterialIds = java.util.Arrays.stream(materialIds)
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
        return snapshot(routeProcessId, target)
                .setProductionConfigSnapshotJson("{\"outputMaterialIds\":[" + outputMaterialIds + "]}");
    }

    private MesPqcInspectionTaskDO task(Long id, Long routeProcessId) {
        return MesPqcInspectionTaskDO.builder().id(id).activeOrderId(10L).workOrderId(30L)
                .routeId(40L).routeVersionId(41L).routeProcessId(routeProcessId).processId(1L)
                .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED).build();
    }

    private MesProProcessPoolEventDO productionSubmit(Long id, Long routeProcessId, String rawPayload) {
        return MesProProcessPoolEventDO.builder()
                .id(id)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .workOrderId(30L)
                .routeId(40L)
                .routeProcessId(routeProcessId)
                .processId(1L)
                .rawPayload(rawPayload)
                .build();
    }
}
