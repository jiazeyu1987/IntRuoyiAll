package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderDetailReadMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesTeamLeaderActiveOrderEventPartyReadDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesTeamLeaderActiveOrderDetailReadDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterial;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterialService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.lenient;
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
    @Mock
    private MesQaInspectionRegulationProcessMapper qaProcessMapper;
    @Mock
    private ErpKingdeeProductionReplenishmentListItemMapper replenishmentListItemMapper;
    @Mock
    private ErpKingdeeProductionReplenishmentListMapper replenishmentListMapper;
    @Mock
    private MesMdItemMapper itemMapper;
    @InjectMocks
    private MesTeamLeaderActiveOrderDetailServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(replenishmentListItemMapper.selectListByProductionOrderNo("881MO090889"))
                .thenReturn(List.of());
    }

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
                        .qaItemCode("WIDTH")
                        .inspectionRuleKey("FIRST")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)
                        .actualInspectionQuantity(2)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(7101L)
                        .build()));
        when(qaProcessMapper.selectBatchIds(List.of(5101L))).thenReturn(List.of(
                MesQaInspectionRegulationProcessDO.builder()
                        .id(5101L)
                        .regulationVersionId(5201L)
                        .processCode("QA-粗洗")
                        .processName("粗洗检验")
                        .build()));
        when(detailReadMapper.selectEventPartiesByEventIds(List.of(7101L))).thenReturn(List.of(
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(7101L)
                        .setSubmitterName("PQC王五")
                        .setReviewerName("PQC主管甲")));
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
        assertEquals("PQC王五", pqcSubmission.getSubmitterName());
        assertEquals("PQC主管甲", pqcSubmission.getReviewerName());
        assertEquals(1, pqcSubmission.getItems().size());
        assertEquals("WIDTH", pqcSubmission.getItems().get(0).getItemCode());
        assertEquals("EQ-001", pqcSubmission.getItems().get(0).getSelectedEquipmentNumber());
    }

    @Test
    void shouldExposeMaterialDeviceMeteringValidityFromOriginalPayload() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .activeStatus("ACTIVE")
                .build());
        MesTeamLeaderActiveOrderDetailReadDO productionRow = row(9101L, 5001L, 6001L, "粗洗",
                "10.000000", 7001L, "10", "张三", "生产组长甲", "2026-08-13T08:10:00");
        productionRow.setOriginalPayloadJson("""
                {
                  "materialDetails": [
                    {
                      "materialId": 3102,
                      "materialCode": "OUT-A",
                      "materialName": "产出A",
                      "outputQuantity": 10,
                      "lossQuantity": 0,
                      "clearanceConfirmations": [
                        { "key": "workplace", "label": "清场", "confirmed": true },
                        { "key": "material", "label": "物料", "confirmed": true },
                        { "key": "cleaning", "label": "清洁", "confirmed": true }
                      ],
                      "selectedDevices": [
                        {
                          "deviceId": 980009,
                          "deviceCode": "B09393",
                          "deviceName": "超声波清洗机",
                          "inMeteringValidityPeriod": false
                        }
                      ],
                      "deviceParameterReadings": [
                        {
                          "deviceId": 980009,
                          "deviceCode": "B09393",
                          "deviceName": "超声波清洗机",
                          "parameterCode": "TEMP",
                          "parameterName": "清洗温度",
                          "unit": "℃",
                          "value": 45,
                          "textValue": "45",
                          "lowerLimit": 20,
                          "upperLimit": 30,
                          "parameterStatus": "ABOVE_UPPER"
                        }
                      ]
                    }
                  ]
                }
                """);
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(productionRow));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of());
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of());

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        MesTeamLeaderActiveOrderDetail.SubmissionMaterialDetail material =
                detail.getProcesses().get(0).getSubmissions().get(0).getMaterials().get(0);
        assertEquals("OUT-A", material.getMaterialCode());
        assertEquals(1, material.getDevices().size());
        assertEquals("B09393", material.getDevices().get(0).getDeviceCode());
        assertEquals(Boolean.FALSE, material.getDevices().get(0).getInMeteringValidityPeriod());
        assertEquals(3, material.getClearanceConfirmations().size());
        assertEquals("workplace", material.getClearanceConfirmations().get(0).getKey());
        assertEquals(Boolean.TRUE, material.getClearanceConfirmations().get(0).getConfirmed());
        assertEquals(1, material.getDeviceParameters().size());
        assertEquals("TEMP", material.getDeviceParameters().get(0).getParameterCode());
        assertEquals(new BigDecimal("20"), material.getDeviceParameters().get(0).getLowerLimit());
        assertEquals(new BigDecimal("30"), material.getDeviceParameters().get(0).getUpperLimit());
        assertEquals("ABOVE_UPPER", material.getDeviceParameters().get(0).getParameterStatus());
    }

    @Test
    void shouldKeepSameProcessFinalInspectionTasksSeparatedByQaItemCode() {
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
                        .qaItemCode("APPEARANCE")
                        .inspectionRuleKey("FINAL")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)
                        .actualInspectionQuantity(3)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(8869L)
                        .build(),
                MesPqcInspectionTaskDO.builder()
                        .id(4102L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .inspectionType("FINAL")
                        .qaItemCode("CLEAN")
                        .inspectionRuleKey("FINAL")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)
                        .actualInspectionQuantity(3)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(8872L)
                        .build()));
        when(qaProcessMapper.selectBatchIds(List.of(5101L))).thenReturn(List.of(
                MesQaInspectionRegulationProcessDO.builder()
                        .id(5101L)
                        .regulationVersionId(5201L)
                        .processCode("QA-粗洗")
                        .processName("粗洗检验")
                        .build()));
        when(detailReadMapper.selectEventPartiesByEventIds(List.of(8869L, 8872L))).thenReturn(List.of(
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(8869L)
                        .setSubmitterName("PQC王五")
                        .setReviewerName("PQC主管甲"),
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(8872L)
                        .setSubmitterName("PQC赵六")
                        .setReviewerName("PQC主管甲")));
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
        assertEquals(2, process.getPqcSubmissions().size());
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail appearance = process.getPqcSubmissions().get(0);
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail clean = process.getPqcSubmissions().get(1);
        assertEquals("FINAL", appearance.getInspectionType());
        assertEquals("FINAL", appearance.getInspectionRuleKey());
        assertEquals("APPEARANCE", appearance.getQaItemCode());
        assertEquals(List.of(8869L), appearance.getSubmittedEventIds());
        assertEquals("PQC王五", appearance.getSubmitterName());
        assertEquals(1, appearance.getItems().size());
        assertEquals("APPEARANCE", appearance.getItems().get(0).getItemCode());
        assertEquals("FINAL", clean.getInspectionType());
        assertEquals("FINAL", clean.getInspectionRuleKey());
        assertEquals("CLEAN", clean.getQaItemCode());
        assertEquals(List.of(8872L), clean.getSubmittedEventIds());
        assertEquals("PQC赵六", clean.getSubmitterName());
        assertEquals(1, clean.getItems().size());
        assertEquals("CLEAN", clean.getItems().get(0).getItemCode());
    }

    @Test
    void shouldKeepPatrolAmAndPmAsSeparatePqcSubmissionBlocks() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "清洗", "100.000000", 7001L, "100", "张三", "生产组长甲",
                        "2026-08-13T08:10:00")));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder()
                        .id(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .inspectionType("PATROL")
                        .qaItemCode("APPEARANCE")
                        .inspectionRuleKey("PATROL_AM")
                        .shiftCode("AM")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED)
                        .actualInspectionQuantity(1)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(10287L)
                        .build(),
                MesPqcInspectionTaskDO.builder()
                        .id(4102L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .inspectionType("PATROL")
                        .qaItemCode("APPEARANCE")
                        .inspectionRuleKey("PATROL_PM")
                        .shiftCode("PM")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED)
                        .actualInspectionQuantity(1)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(10288L)
                        .build()));
        when(qaProcessMapper.selectBatchIds(List.of(5101L))).thenReturn(List.of(
                MesQaInspectionRegulationProcessDO.builder()
                        .id(5101L)
                        .regulationVersionId(5201L)
                        .processCode("PQC-清洗")
                        .processName("清洗")
                        .build()));
        when(detailReadMapper.selectEventPartiesByEventIds(List.of(10287L, 10288L))).thenReturn(List.of(
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(10287L)
                        .setSubmitterName("PQC管理员")
                        .setReviewerName("PQC管理员"),
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(10288L)
                        .setSubmitterName("PQC管理员")
                        .setReviewerName("PQC管理员")));
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
                        .itemCode("APPEARANCE")
                        .itemName("外观")
                        .judgement("PASS")
                        .build()));

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        MesTeamLeaderActiveOrderDetail.ProcessDetail process = detail.getProcesses().get(0);
        assertEquals(2, process.getPqcSubmissions().size());
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail am = process.getPqcSubmissions().get(0);
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail pm = process.getPqcSubmissions().get(1);
        assertEquals("PATROL_AM", am.getInspectionRuleKey());
        assertEquals(List.of(10287L), am.getSubmittedEventIds());
        assertEquals("PATROL_PM", pm.getInspectionRuleKey());
        assertEquals(List.of(10288L), pm.getSubmittedEventIds());
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
