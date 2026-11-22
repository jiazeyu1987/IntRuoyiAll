package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineActiveOrderProcessServiceTest {

    private static final Long LOGIN_LEADER_ID = 9001L;
    private static final Long ACTIVE_ORDER_ID = 48L;
    private static final Long ROUTE_ID = 101L;
    private static final Long ROUTE_VERSION_ID = 501L;
    private static final Long FROZEN_ROUTE_PROCESS_ID = 980645L;
    private static final Long PROCESS_ID = 201L;
    private static final Long PRODUCTION_EVENT_ID = 8801L;
    private static final Long OUTPUT_MATERIAL_ID = 9001L;

    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProcessPoolReportAllocationMapper reportAllocationMapper;
    @Mock
    private MesProProcessPoolEventMapper processPoolEventMapper;

    private MesFrontlineActiveOrderProcessService service;

    @BeforeEach
    void setUp() {
        service = new MesFrontlineActiveOrderProcessServiceImpl(activeOrderMapper, processSnapshotMapper,
                routeVersionMapper, reportAllocationMapper, processPoolEventMapper);
        { org.springframework.test.util.ReflectionTestUtils.setField(service, "packagingAccess", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesPackagingLeaderAccess.class)); }
    }

    @Test
    void listProcesses_returnsFrozenVersionIdentityQuantityAndWorkstation() {
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder());
        when(routeVersionMapper.selectById(ROUTE_VERSION_ID)).thenReturn(routeVersion());
        when(processSnapshotMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                processSnapshot()));

        List<MesFrontlineActiveOrderProcess> processes = service.listProcesses(
                LOGIN_LEADER_ID, ACTIVE_ORDER_ID);

        assertEquals(1, processes.size());
        MesFrontlineActiveOrderProcess process = processes.get(0);
        assertEquals(ACTIVE_ORDER_ID, process.activeOrderId());
        assertEquals(ROUTE_VERSION_ID, process.routeVersionId());
        assertEquals(FROZEN_ROUTE_PROCESS_ID, process.routeProcessId());
        assertEquals(PROCESS_ID, process.processId());
        assertEquals("P-OLD", process.processCode());
        assertEquals("旧版精洗", process.processName());
        assertEquals(301L, process.workstationId());
        assertEquals("WS-OLD", process.workstationCode());
        assertEquals(new BigDecimal("1.1"), process.lossCoefficient());
        assertEquals(new BigDecimal("100.000000"), process.targetQuantity());
        assertEquals(new BigDecimal("0.000000"), process.submittedQuantity());
        assertEquals(Boolean.FALSE, process.checkFlag());
    }

    @Test
    void listProcesses_returnsSubmittedQuantityFromCurrentActiveOrderProcessProgress() {
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder());
        when(routeVersionMapper.selectById(ROUTE_VERSION_ID)).thenReturn(routeVersion());
        when(processSnapshotMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                processSnapshotWithOutputMaterialConfig()));
        when(reportAllocationMapper.selectListByActiveOrderIds(List.of(ACTIVE_ORDER_ID))).thenReturn(List.of(
                reportAllocation()));
        when(processPoolEventMapper.selectBatchIds(List.of(PRODUCTION_EVENT_ID))).thenReturn(List.of(
                productionSubmitEvent()));

        List<MesFrontlineActiveOrderProcess> processes = service.listProcesses(LOGIN_LEADER_ID, ACTIVE_ORDER_ID);

        assertEquals(1, processes.size());
        assertEquals(new BigDecimal("40.000000"), processes.get(0).submittedQuantity());
        assertEquals(0, new BigDecimal("40.000000").compareTo(processes.get(0).outputSubmittedQuantities().get(OUTPUT_MATERIAL_ID)));
    }

    @Test
    void listProcesses_distinguishesMaterialTotalsFromProcessMinimumAndRefreshesAfterRemainingMaterial() {
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder());
        when(routeVersionMapper.selectById(ROUTE_VERSION_ID)).thenReturn(routeVersion());
        var snapshot = processSnapshotWithOutputMaterialConfig()
                .setProductionConfigSnapshotJson("{\"outputMaterialIds\": [9001, 9002]}");
        when(processSnapshotMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(snapshot));
        var allocation = reportAllocation().setAllocatedQuantity(new BigDecimal("5000"));
        when(reportAllocationMapper.selectListByActiveOrderIds(List.of(ACTIVE_ORDER_ID))).thenReturn(List.of(allocation));
        var event = productionSubmitEvent().setReportOutputQuantity(new BigDecimal("5000"))
                .setRawPayload("{\"materialDetails\": [{\"materialId\":9001,\"outputQuantity\":5000}]}");
        when(processPoolEventMapper.selectBatchIds(List.of(PRODUCTION_EVENT_ID))).thenReturn(List.of(event));
        var first = service.listProcesses(LOGIN_LEADER_ID, ACTIVE_ORDER_ID).get(0);
        assertEquals(new BigDecimal("0.000000"), first.submittedQuantity());
        assertEquals(0, new BigDecimal("5000.000000").compareTo(first.outputSubmittedQuantities().get(9001L)));
        assertEquals(0, new BigDecimal("0.000000").compareTo(first.outputSubmittedQuantities().get(9002L)));
        event.setRawPayload("{\"materialDetails\": [{\"materialId\":9001,\"outputQuantity\":5000},{\"materialId\":9002,\"outputQuantity\":5000}]}");
        var completed = service.listProcesses(LOGIN_LEADER_ID, ACTIVE_ORDER_ID).get(0);
        assertEquals(new BigDecimal("5000.000000"), completed.submittedQuantity());
        assertEquals(0, new BigDecimal("5000.000000").compareTo(completed.outputSubmittedQuantities().get(9002L)));
    }

    @Test
    void requireProcess_rejectsCurrentRouteProcessThatIsNotFrozenByOrder() {
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder());
        when(routeVersionMapper.selectById(ROUTE_VERSION_ID)).thenReturn(routeVersion());
        when(processSnapshotMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                processSnapshot()));

        assertThrows(ServiceException.class, () -> service.requireProcess(LOGIN_LEADER_ID, ACTIVE_ORDER_ID,
                ROUTE_ID, 9908090160L, PROCESS_ID));
    }

    @Test
    void listProcesses_usesFrozenProcessSnapshotWhenRouteNodeLabelsAreMissing() {
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder());
        when(routeVersionMapper.selectById(ROUTE_VERSION_ID)).thenReturn(routeVersionWithoutProcessLabels());
        when(processSnapshotMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                processSnapshotWithLabels()));

        List<MesFrontlineActiveOrderProcess> processes = service.listProcesses(LOGIN_LEADER_ID, ACTIVE_ORDER_ID);

        assertEquals(1, processes.size());
        assertEquals("ER0C9BD936FFAE", processes.get(0).processCode());
        assertEquals("粗洗工序", processes.get(0).processName());
    }

    @Test
    void listProcesses_keepsFrozenRouteProcessCheckFlagFromLockedVersionSnapshot() {
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder());
        when(routeVersionMapper.selectById(ROUTE_VERSION_ID)).thenReturn(routeVersionWithCheckProcess());
        when(processSnapshotMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                processSnapshot()));

        List<MesFrontlineActiveOrderProcess> processes = service.listProcesses(LOGIN_LEADER_ID, ACTIVE_ORDER_ID);

        assertEquals(1, processes.size());
        assertEquals(Boolean.TRUE, processes.get(0).checkFlag());
        assertEquals(Boolean.TRUE, processes.get(0).toRouteProcessCandidate().checkFlag());
        assertEquals(FROZEN_ROUTE_PROCESS_ID, processes.get(0).toRouteProcessCandidate().routeProcessId());
    }

    @Test
    void listProcesses_rejectsFrozenNodeAndProcessSnapshotWithoutLabels() {
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder());
        when(routeVersionMapper.selectById(ROUTE_VERSION_ID)).thenReturn(routeVersionWithoutProcessLabels());
        when(processSnapshotMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(
                processSnapshot()));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.listProcesses(LOGIN_LEADER_ID, ACTIVE_ORDER_ID));
        assertTrue(exception.getMessage().contains("activeOrderId=" + ACTIVE_ORDER_ID));
        assertTrue(exception.getMessage().contains("冻结编码或名称"));
    }

    @Test
    void getFrozenSopFileUrl_readsSupersededOrderVersionWithoutConsultingNewActiveVersion() {
        stubFrozenSop("\"sopFileUrl\":\"/files/sop-v1.pdf\"");

        assertEquals("/files/sop-v1.pdf", frozenSop());
        org.mockito.Mockito.verify(routeVersionMapper, org.mockito.Mockito.never()).selectActiveByRouteId(ROUTE_ID);
    }

    @Test
    void getFrozenSopFileUrl_returnsExplicitUnboundSnapshot() {
        stubFrozenSop("\"sopFileUrl\":null");

        org.junit.jupiter.api.Assertions.assertNull(frozenSop());
    }

    @Test
    void getFrozenSopFileUrl_rejectsAbsentFieldInsteadOfReadingCurrentMaster() {
        stubFrozenSop("\"legacyField\":null");

        ServiceException error = assertThrows(ServiceException.class, this::frozenSop);
        assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants
                .PRO_FRONTLINE_ACTIVE_ORDER_PROCESS_SNAPSHOT_INVALID.getCode(), error.getCode());
        assertTrue(error.getMessage().contains("缺少 SOP 文件绑定字段"));
    }

    @Test
    void getFrozenSopFileUrl_rejectsMalformedFrozenBinding() {
        stubFrozenSop("\"sopFileUrl\":123");

        ServiceException error = assertThrows(ServiceException.class, this::frozenSop);
        assertTrue(error.getMessage().contains("SOP 文件绑定字段格式无效"));
    }

    @Test
    void getFrozenSopFileUrl_rejectsForeignProcess() {
        stubFrozenSop("\"sopFileUrl\":\"/files/sop-v1.pdf\"");

        ServiceException error = assertThrows(ServiceException.class, () -> service.getFrozenSopFileUrl(
                LOGIN_LEADER_ID, ACTIVE_ORDER_ID, ROUTE_ID, FROZEN_ROUTE_PROCESS_ID + 1, PROCESS_ID));
        assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants
                .PRO_FRONTLINE_ACTIVE_ORDER_PROCESS_MISMATCH.getCode(), error.getCode());
    }

    @Test
    void getFrozenSopFileUrl_rejectsUnauthorizedLeaderBeforeReadingSnapshot() {
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder());

        assertThrows(ServiceException.class, () -> service.getFrozenSopFileUrl(
                LOGIN_LEADER_ID + 1, ACTIVE_ORDER_ID, ROUTE_ID, FROZEN_ROUTE_PROCESS_ID, PROCESS_ID));
        org.mockito.Mockito.verifyNoInteractions(routeVersionMapper, processSnapshotMapper);
    }

    @Test
    void getFrozenSopFileUrl_rejectsOrderVersionChangeDuringRead() {
        stubFrozenSop("\"sopFileUrl\":\"/files/sop-v1.pdf\"");
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder(),
                activeOrder().setRouteVersionId(ROUTE_VERSION_ID + 1));

        ServiceException error = assertThrows(ServiceException.class, this::frozenSop);
        assertTrue(error.getMessage().contains("锁定工艺路线或版本发生变化"));
        org.mockito.Mockito.verify(routeVersionMapper, org.mockito.Mockito.never())
                .selectById(ROUTE_VERSION_ID + 1);
    }

    private String frozenSop() {
        return service.getFrozenSopFileUrl(LOGIN_LEADER_ID, ACTIVE_ORDER_ID, ROUTE_ID,
                FROZEN_ROUTE_PROCESS_ID, PROCESS_ID);
    }

    private void stubFrozenSop(String field) {
        when(activeOrderMapper.selectById(ACTIVE_ORDER_ID)).thenReturn(activeOrder());
        var version = routeVersion();
        version.setRouteSnapshotJson(version.getRouteSnapshotJson().replace("\"sort\": 10", field + ",\"sort\": 10"));
        when(routeVersionMapper.selectById(ROUTE_VERSION_ID)).thenReturn(version);
        when(processSnapshotMapper.selectListByActiveOrderId(ACTIVE_ORDER_ID)).thenReturn(List.of(processSnapshot()));
    }

    private static MesProcessPoolActiveOrderDO activeOrder() {
        return MesProcessPoolActiveOrderDO.builder()
                .id(ACTIVE_ORDER_ID)
                .leaderUserId(LOGIN_LEADER_ID)
                .workOrderId(1001L)
                .routeId(ROUTE_ID)
                .routeVersionId(ROUTE_VERSION_ID)
                .activeStatus("ACTIVE")
                .businessStatus("ACTIVE")
                .build();
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO processSnapshot() {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .id(7001L)
                .activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(1001L)
                .routeId(ROUTE_ID)
                .routeVersionId(ROUTE_VERSION_ID)
                .routeProcessId(FROZEN_ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .erpFixedQuantitySnapshot(new BigDecimal("100.000000"))
                .productionQuantityFactorSnapshot(new BigDecimal("1.500000"))
                .plannedQuantitySnapshot(new BigDecimal("150.000000"))
                .productionConfigSnapshotJson("{\"outputMaterialIds\": []}")
                .build();
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO processSnapshotWithLabels() {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .id(7001L)
                .activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(1001L)
                .routeId(ROUTE_ID)
                .routeVersionId(ROUTE_VERSION_ID)
                .routeProcessId(FROZEN_ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .processCodeSnapshot("ER0C9BD936FFAE")
                .processNameSnapshot("粗洗工序")
                .erpFixedQuantitySnapshot(new BigDecimal("100.000000"))
                .productionQuantityFactorSnapshot(new BigDecimal("1.500000"))
                .plannedQuantitySnapshot(new BigDecimal("150.000000"))
                .productionConfigSnapshotJson("{\"outputMaterialIds\": []}")
                .build();
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO processSnapshotWithOutputMaterialConfig() {
        return processSnapshot()
                .setProductionConfigSnapshotJson("""
                        {
                          "outputMaterialIds": [9001]
                        }
                        """);
    }

    private static MesProcessPoolReportAllocationDO reportAllocation() {
        return MesProcessPoolReportAllocationDO.builder()
                .id(9101L)
                .eventId(PRODUCTION_EVENT_ID)
                .activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(1001L)
                .routeProcessId(FROZEN_ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .allocatedQuantity(new BigDecimal("40.000000"))
                .lifecycleStatus(MesProcessPoolReportAllocationDO.LIFECYCLE_CURRENT)
                .build();
    }

    private static MesProProcessPoolEventDO productionSubmitEvent() {
        return MesProProcessPoolEventDO.builder()
                .id(PRODUCTION_EVENT_ID)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .reportOutputQuantity(new BigDecimal("40.000000"))
                .workOrderId(1001L)
                .routeId(ROUTE_ID)
                .routeProcessId(FROZEN_ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .rawPayload("""
                        {
                          "materialDetails": [
                            {
                              "materialId": 9001,
                              "outputQuantity": 40
                            }
                          ]
                        }
                        """)
                .build();
    }

    private static MesProRouteVersionDO routeVersion() {
        return MesProRouteVersionDO.builder()
                .id(ROUTE_VERSION_ID)
                .routeId(ROUTE_ID)
                .versionNo("V1")
                .active(Boolean.FALSE)
                .lifecycleStatus("SUPERSEDED")
                .routeSnapshotJson("""
                        {
                          "routeId": 101,
                          "routeCode": "R-OLD",
                          "routeName": "旧版路线",
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {
                                  "routeProcessId": 980645,
                                  "processId": 201,
                                  "processCode": "P-OLD",
                                  "processName": "旧版精洗",
                                  "sort": 10,
                                  "routeProcessWorkstationId": 301,
                                  "workstationCode": "WS-OLD",
                                  "workstationName": "旧版精洗工位",
                                  "checkFlag": false
                                }
                              ]
                            }
                          }
                        }
                        """)
                .build();
    }

    private static MesProRouteVersionDO routeVersionWithCheckProcess() {
        return MesProRouteVersionDO.builder()
                .id(ROUTE_VERSION_ID)
                .routeId(ROUTE_ID)
                .versionNo("V1")
                .active(Boolean.FALSE)
                .lifecycleStatus("SUPERSEDED")
                .routeSnapshotJson("""
                        {
                          "routeId": 101,
                          "routeCode": "R-OLD",
                          "routeName": "旧版路线",
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {
                                  "routeProcessId": 980645,
                                  "processId": 201,
                                  "processCode": "P-OLD",
                                  "processName": "旧版精洗",
                                  "sort": 10,
                                  "routeProcessWorkstationId": 301,
                                  "workstationCode": "WS-OLD",
                                  "workstationName": "旧版精洗工位",
                                  "checkFlag": true
                                }
                              ]
                            }
                          }
                        }
                        """)
                .build();
    }

    private static MesProRouteVersionDO routeVersionWithoutProcessLabels() {
        return MesProRouteVersionDO.builder()
                .id(ROUTE_VERSION_ID)
                .routeId(ROUTE_ID)
                .versionNo("V1")
                .active(Boolean.FALSE)
                .lifecycleStatus("SUPERSEDED")
                .routeSnapshotJson("""
                        {
                          "routeId": 101,
                          "routeCode": "R-OLD",
                          "routeName": "旧版路线",
                          "configSnapshots": {
                            "flowGraph": {
                              "nodes": [
                                {
                                  "routeProcessId": 980645,
                                  "processId": 201,
                                  "sort": 10,
                                  "routeProcessWorkstationId": 301,
                                  "workstationCode": "WS-OLD",
                                  "workstationName": "旧版精洗工位"
                                }
                              ]
                            }
                          }
                        }
                        """)
                .build();
    }

}
