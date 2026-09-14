package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
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

    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;

    private MesFrontlineActiveOrderProcessService service;

    @BeforeEach
    void setUp() {
        service = new MesFrontlineActiveOrderProcessServiceImpl(activeOrderMapper, processSnapshotMapper,
                routeVersionMapper);
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
        assertEquals(new BigDecimal("1.500000"), process.productionQuantityFactor());
        assertEquals(new BigDecimal("150.000000"), process.targetQuantity());
        assertEquals(Boolean.FALSE, process.checkFlag());
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
                .productionQuantityFactorSnapshot(new BigDecimal("1.500000"))
                .plannedQuantitySnapshot(new BigDecimal("150.000000"))
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
                .productionQuantityFactorSnapshot(new BigDecimal("1.500000"))
                .plannedQuantitySnapshot(new BigDecimal("150.000000"))
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
