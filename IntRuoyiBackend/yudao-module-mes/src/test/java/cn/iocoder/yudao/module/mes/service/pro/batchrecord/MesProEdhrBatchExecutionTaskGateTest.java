package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrBatchExecutionTaskGateTest {

    private final MesProEdhrBatchExecutionServiceImpl service = new MesProEdhrBatchExecutionServiceImpl();

    @Test
    void resolveTaskGate_shouldUnlockSiblingBranchesAfterSharedPredecessorApproved() {
        MesProEdhrBatchExecutionTaskDO processA = task(1L, 101L, null, true,
                MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED);
        MesProEdhrBatchExecutionTaskDO processB = task(2L, 102L, 101L, false,
                MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING);
        MesProEdhrBatchExecutionTaskDO processC = task(3L, 103L, 101L, false,
                MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING);

        assertTrue(available(processB, List.of(processA, processB, processC)));
        assertTrue(available(processC, List.of(processA, processB, processC)));
    }

    @Test
    void resolveTaskGate_shouldWaitOnlyForDirectPredecessor() {
        MesProEdhrBatchExecutionTaskDO processA = task(1L, 101L, null, true,
                MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED);
        MesProEdhrBatchExecutionTaskDO processB = task(2L, 102L, 101L, false,
                MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING);
        MesProEdhrBatchExecutionTaskDO processC = task(3L, 103L, 101L, false,
                MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING);
        MesProEdhrBatchExecutionTaskDO processD = task(4L, 104L, 103L, false,
                MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING);

        assertFalse(available(processD, List.of(processA, processB, processC, processD)));
        processC.setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED);
        assertTrue(available(processD, List.of(processA, processB, processC, processD)));
    }

    @Test
    void resolveTaskGate_shouldEnforceSequentialModeInsideSameProcess() {
        MesProEdhrBatchExecutionTaskDO batchRecord = task(1L, 101L, null, true,
                MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING);
        batchRecord.setExecutionMode("SEQUENTIAL");
        batchRecord.setBatchRecordSort(1);
        MesProEdhrBatchExecutionTaskDO lossRecord = task(2L, 101L, null, true,
                MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING);
        lossRecord.setExecutionMode("SEQUENTIAL");
        lossRecord.setBatchRecordSort(2);

        assertFalse(available(lossRecord, List.of(batchRecord, lossRecord)));
        batchRecord.setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED);
        assertTrue(available(lossRecord, List.of(batchRecord, lossRecord)));
    }

    @Test
    void resolveTaskGate_shouldAllowDynamicCompanionFormBeforeMainApproved() {
        MesProEdhrBatchExecutionTaskDO mainRecord = task(1L, 101L, null, true,
                MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_DRAFT);
        mainRecord.setExecutionMode("SEQUENTIAL");
        mainRecord.setBatchRecordSort(1);
        mainRecord.setFormSlotType("MAIN");
        MesProEdhrBatchExecutionTaskDO lossForm = task(2L, 101L, null, true,
                MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING);
        lossForm.setExecutionMode("SEQUENTIAL");
        lossForm.setBatchRecordSort(2);
        lossForm.setBatchRecordReportId(null);
        lossForm.setFormSlotType("LOSS_REPORT");
        lossForm.setFormBindingKey("FB-LOSS-101");
        lossForm.setFormTemplateId(25L);

        assertTrue(available(lossForm, List.of(mainRecord, lossForm)));
    }

    @Test
    void resolveTaskGate_shouldBlockWhenPredecessorSnapshotDoesNotResolveToTask() {
        MesProEdhrBatchExecutionTaskDO task = task(4L, 104L, 999L, false,
                MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING);

        assertFalse(available(task, List.of(task)));
    }

    @Test
    void resolveTaskGate_shouldUnlockPreRouteSpecialNodeBeforeRouteForms() {
        MesProEdhrBatchExecutionTaskDO incomingInspection = specialTask(10L,
                MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_INCOMING_INSPECTION_REPORT,
                0, MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING);
        MesProEdhrBatchExecutionTaskDO firstRouteForm = task(11L, 101L, null, true,
                MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING)
                .setRouteProcessSort(10);

        assertTrue(available(incomingInspection, List.of(incomingInspection, firstRouteForm)));
    }

    @Test
    void resolveTaskGate_shouldWaitForPriorRouteFormsBeforePostRouteSpecialNode() {
        MesProEdhrBatchExecutionTaskDO firstRouteForm = task(12L, 101L, null, true,
                MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING)
                .setRouteProcessSort(10);
        MesProEdhrBatchExecutionTaskDO sterilization = specialTask(13L,
                MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_STERILIZATION_REPORT,
                9000, MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING);

        assertFalse(available(sterilization, List.of(firstRouteForm, sterilization)));

        firstRouteForm.setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED);

        assertTrue(available(sterilization, List.of(firstRouteForm, sterilization)));
    }

    private boolean available(MesProEdhrBatchExecutionTaskDO task,
                              List<MesProEdhrBatchExecutionTaskDO> allTasks) {
        Object gate = ReflectionTestUtils.invokeMethod(service, "resolveTaskGate", task, allTasks);
        return Boolean.TRUE.equals(ReflectionTestUtils.getField(gate, "available"));
    }

    private MesProEdhrBatchExecutionTaskDO task(Long id,
                                                Long routeProcessId,
                                                Long predecessorRouteProcessId,
                                                boolean root,
                                                int status) {
        return MesProEdhrBatchExecutionTaskDO.builder()
                .id(id)
                .nodeType(MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM)
                .routeProcessId(routeProcessId)
                .predecessorRouteProcessId(predecessorRouteProcessId)
                .rootProcessFlag(root)
                .batchRecordReportId("REPORT-" + id)
                .batchRecordSort(1)
                .executionMode("PARALLEL")
                .requiredFlag(true)
                .status(status)
                .build();
    }

    private MesProEdhrBatchExecutionTaskDO specialTask(Long id,
                                                       String nodeType,
                                                       Integer routeProcessSort,
                                                       int status) {
        return MesProEdhrBatchExecutionTaskDO.builder()
                .id(id)
                .nodeType(nodeType)
                .routeProcessSort(routeProcessSort)
                .requiredFlag(true)
                .status(status)
                .build();
    }
}
