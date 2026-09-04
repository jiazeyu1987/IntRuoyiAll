package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderDetailReadMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesTeamLeaderActiveOrderDetailReadDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterial;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterialService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderDetailServiceImplTest {

    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesProcessPoolActiveOrderDetailReadMapper detailReadMapper;
    @Mock
    private MesFrontlineProcessMaterialService processMaterialService;
    @Mock
    private MesPqcInspectionTaskMapper pqcTaskMapper;
    @Mock
    private MesPqcProcessInspectionAggregateDetailMapper pqcAggregateDetailMapper;
    @InjectMocks
    private MesTeamLeaderActiveOrderDetailServiceImpl service;

    @Test
    void shouldGroupMultipleEmployeesAndSubmissionsByFormalProcessSnapshot() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100.000000", 7001L, "30", "张三", "生产组长甲",
                        "2026-08-13T08:10:00"),
                row(9101L, 5001L, 6001L, "粗洗", "100.000000", 7002L, "40", "李四", null,
                        "2026-08-13T09:20:00"),
                row(9102L, 5002L, 6002L, "精洗", "100.000000", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5002L, 6002L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of());
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of());

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        assertEquals(8101L, detail.getActiveOrderId());
        assertEquals("881MO090889", detail.getWorkOrderCode());
        assertEquals("球囊扩张压力泵工艺路线", detail.getRouteName());
        assertEquals(2, detail.getProcesses().size());
        MesTeamLeaderActiveOrderDetail.ProcessDetail roughWash = detail.getProcesses().get(0);
        assertEquals(new BigDecimal("100.000000"), roughWash.getRequiredQuantity());
        assertEquals(new BigDecimal("70"), roughWash.getSubmittedQuantity());
        assertEquals(2, roughWash.getSubmissionCount());
        assertEquals("张三", roughWash.getSubmissions().get(0).getSubmitterName());
        assertEquals("生产组长甲", roughWash.getSubmissions().get(0).getReviewerName());
        assertEquals("李四", roughWash.getSubmissions().get(1).getSubmitterName());
        assertNull(roughWash.getSubmissions().get(1).getReviewerName());
        assertEquals(0, detail.getProcesses().get(1).getSubmissionCount());
        assertEquals(List.of(), detail.getProcesses().get(1).getSubmissions());
    }

    @Test
    void shouldExposeInputMaterialBatchesAndPqcSubmissionDetailsByProcess() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .routeVersionId(9301L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100.000000", 7001L, "30", "张三", "生产组长甲",
                        "2026-08-13T08:10:00")));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of(
                new MesFrontlineProcessMaterial(3101L, "MAT-A", "物料A", "S1",
                        MesFrontlineProcessMaterial.ROLE_INPUT, null, List.of("LOT-01", "LOT-02"),
                        new BigDecimal("10"), new BigDecimal("9"), new BigDecimal("9"),
                        List.of(2101L, 2102L), List.of("SIM-SOUT-001", "SIM-SOUT-002"),
                        List.of(2201L, 2202L), "hash-pick-list"),
                new MesFrontlineProcessMaterial(3102L, "OUT-A", "产出A", "S2",
                        MesFrontlineProcessMaterial.ROLE_OUTPUT, null, List.of(), null,
                        null, null, List.of(), List.of(), List.of(), null)));
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder()
                        .id(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .inspectionType("FIRST")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)
                        .actualInspectionQuantity(2)
                        .submittedEventId(7101L)
                        .build()));
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4201L)
                        .pqcTaskId(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .sampleNo(1)
                        .itemCode("WIDTH")
                        .itemName("宽度")
                        .measuredValue("12.3")
                        .itemResult("12.3")
                        .judgement("PASS")
                        .selectedEquipmentNumber("EQ-001")
                        .standardText("10-15")
                        .build()));

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        MesTeamLeaderActiveOrderDetail.ProcessDetail process = detail.getProcesses().get(0);
        assertEquals(1, process.getInputMaterials().size());
        assertEquals("MAT-A", process.getInputMaterials().get(0).getMaterialCode());
        assertEquals(List.of("LOT-01", "LOT-02"), process.getInputMaterials().get(0).getBatchCodes());
        assertEquals(List.of(2101L, 2102L), process.getInputMaterials().get(0).getSourcePickListIds());
        assertEquals(List.of("SIM-SOUT-001", "SIM-SOUT-002"),
                process.getInputMaterials().get(0).getSourcePickListNos());
        assertEquals(1, process.getPqcSubmissions().size());
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail pqcSubmission = process.getPqcSubmissions().get(0);
        assertEquals(4101L, pqcSubmission.getPqcTaskId());
        assertEquals(7101L, pqcSubmission.getSubmittedEventId());
        assertEquals(1, pqcSubmission.getItems().size());
        assertEquals("WIDTH", pqcSubmission.getItems().get(0).getItemCode());
        assertEquals("EQ-001", pqcSubmission.getItems().get(0).getSelectedEquipmentNumber());
    }

    @Test
    void shouldMergeSameProcessFinalInspectionTasksIntoOnePqcSubmissionBlock() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100.000000", 7001L, "100", "张三", "生产组长甲",
                        "2026-08-13T08:10:00")));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder()
                        .id(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .inspectionType("FINAL")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)
                        .actualInspectionQuantity(3)
                        .submittedEventId(8869L)
                        .build(),
                MesPqcInspectionTaskDO.builder()
                        .id(4102L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .inspectionType("FINAL")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)
                        .actualInspectionQuantity(3)
                        .submittedEventId(8872L)
                        .build()));
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4201L)
                        .pqcTaskId(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .sampleNo(1)
                        .itemCode("APPEARANCE")
                        .itemName("外观")
                        .judgement("PASS")
                        .build(),
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4202L)
                        .pqcTaskId(4102L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .sampleNo(1)
                        .itemCode("CLEAN")
                        .itemName("清洁度")
                        .judgement("PASS")
                        .build()));

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        MesTeamLeaderActiveOrderDetail.ProcessDetail process = detail.getProcesses().get(0);
        assertEquals(1, process.getPqcSubmissions().size());
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail finalInspection = process.getPqcSubmissions().get(0);
        assertEquals("FINAL", finalInspection.getInspectionType());
        assertEquals(1, finalInspection.getRoundNo());
        assertEquals(3, finalInspection.getActualInspectionQuantity());
        assertEquals(2, finalInspection.getItems().size());
        assertEquals("APPEARANCE", finalInspection.getItems().get(0).getItemCode());
        assertEquals("CLEAN", finalInspection.getItems().get(1).getItemCode());
    }

    @Test
    void shouldMarkAllSubmissionsOfOverrunProcessAsQuantityConflict() throws Exception {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "1500.000000", 7001L, "1000", "张三", "生产组长甲",
                        "2026-08-19T16:52:08"),
                row(9101L, 5001L, 6001L, "粗洗", "1500.000000", 7002L, "2000", "李四", "生产组长甲",
                        "2026-08-19T16:53:03")));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of());
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of());

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        MesTeamLeaderActiveOrderDetail.ProcessDetail process = detail.getProcesses().get(0);
        assertEquals(Boolean.TRUE, invokeBoolean(process, "getQuantityConflict"));
        assertEquals(0, new BigDecimal("1500").compareTo(invokeBigDecimal(process, "getOverageQuantity")));
        for (MesTeamLeaderActiveOrderDetail.SubmissionDetail submission : process.getSubmissions()) {
            assertEquals(Boolean.TRUE, invokeBoolean(submission, "getQuantityConflict"));
        }
    }

    @Test
    void shouldRejectActiveOrderOutsideCurrentLeaderOrRemovedOrder() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3002L)
                .activeStatus("ACTIVE")
                .build());

        ServiceException error = assertThrows(ServiceException.class, () -> service.getDetail(3001L, 8101L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS.getCode(), error.getCode());
    }

    @Test
    void shouldFailWhenFormalProcessSnapshotsAreMissing() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of());

        ServiceException error = assertThrows(ServiceException.class, () -> service.getDetail(3001L, 8101L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED.getCode(), error.getCode());
    }

    private static MesTeamLeaderActiveOrderDetailReadDO row(Long snapshotId,
                                                              Long routeProcessId,
                                                              Long processId,
                                                             String processName,
                                                             String requiredQuantity,
                                                             Long eventId,
                                                             String submittedQuantity,
                                                             String submitterName,
                                                             String reviewerName,
                                                             String submittedAt) {
        return new MesTeamLeaderActiveOrderDetailReadDO()
                .setSnapshotId(snapshotId)
                .setActiveOrderId(8101L)
                .setWorkOrderId(9001L)
                .setWorkOrderCode("881MO090889")
                .setRouteName("球囊扩张压力泵工艺路线")
                .setRouteProcessId(routeProcessId)
                .setProcessId(processId)
                .setProcessCode("P-" + processId)
                .setProcessName(processName)
                .setRequiredQuantity(new BigDecimal(requiredQuantity))
                .setEventId(eventId)
                .setSubmittedQuantity(submittedQuantity == null ? null : new BigDecimal(submittedQuantity))
                .setSubmitterName(submitterName)
                .setReviewerName(reviewerName)
                .setSubmittedAt(submittedAt == null ? null : LocalDateTime.parse(submittedAt));
    }

    private static Boolean invokeBoolean(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return (Boolean) method.invoke(target);
    }

    private static BigDecimal invokeBigDecimal(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return (BigDecimal) method.invoke(target);
    }
}
