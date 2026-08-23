package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
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

    private MesTeamLeaderActiveOrderCompletionProgressPortImpl port;

    @BeforeEach
    void setUp() {
        port = new MesTeamLeaderActiveOrderCompletionProgressPortImpl(snapshotMapper, allocationMapper, taskMapper);
    }

    @Test
    void readsLockedFormalSourcesAndReturnsDualHundred() {
        MesProcessPoolActiveOrderDO order = order();
        when(snapshotMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(snapshot(101L, 10)));
        when(allocationMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(
                MesProcessPoolReportAllocationDO.builder().id(201L).activeOrderId(10L).workOrderId(30L)
                        .routeProcessId(101L).processId(1L).allocatedQuantity(BigDecimal.TEN).build()));
        when(taskMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder().id(301L).activeOrderId(10L).workOrderId(30L)
                        .routeId(40L).routeVersionId(41L).routeProcessId(101L).processId(1L)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED).build()));

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
        when(snapshotMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(snapshot(101L, 10)));
        when(allocationMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(
                MesProcessPoolReportAllocationDO.builder().id(201L).activeOrderId(10L).workOrderId(30L)
                        .routeProcessId(101L).processId(1L).allocatedQuantity(BigDecimal.ONE).build()));
        when(taskMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder().id(301L).activeOrderId(10L).workOrderId(30L)
                        .routeId(40L).routeVersionId(41L).routeProcessId(101L).processId(1L)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED).build()));

        assertEquals(false, port.read(20L, order).isDoubleComplete());
    }

    @Test
    void pqcTasksMustExactlyMatchFrozenProcessSnapshots() {
        MesProcessPoolActiveOrderDO order = order();
        when(snapshotMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(
                snapshot(101L, 10), snapshot(102L, 10)));
        when(allocationMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(
                MesProcessPoolReportAllocationDO.builder().id(201L).activeOrderId(10L).workOrderId(30L)
                        .routeProcessId(101L).processId(1L).allocatedQuantity(BigDecimal.TEN).build(),
                MesProcessPoolReportAllocationDO.builder().id(202L).activeOrderId(10L).workOrderId(30L)
                        .routeProcessId(102L).processId(1L).allocatedQuantity(BigDecimal.TEN).build()));
        when(taskMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(
                task(301L, 101L), task(302L, 999L)));

        assertThrows(RuntimeException.class, () -> port.read(20L, order));
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

    private MesPqcInspectionTaskDO task(Long id, Long routeProcessId) {
        return MesPqcInspectionTaskDO.builder().id(id).activeOrderId(10L).workOrderId(30L)
                .routeId(40L).routeVersionId(41L).routeProcessId(routeProcessId).processId(1L)
                .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED).build();
    }
}
