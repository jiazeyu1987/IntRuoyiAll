package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderOrderProcessTargetServiceTest {

    @Mock
    private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;

    private MesTeamLeaderOrderProcessTargetService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderOrderProcessTargetService(processSnapshotMapper);
    }

    @Test
    void shouldResolveMissingProductionFactorAsOneAndDeriveTargetQuantity() {
        when(processSnapshotMapper.selectByActiveOrderAndProcess(35L, 980631L, 922985L))
                .thenReturn(MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                        .activeOrderId(35L)
                        .workOrderId(980022L)
                        .routeProcessId(980631L)
                        .processId(922985L)
                        .erpFixedQuantitySnapshot(new BigDecimal("10.000000"))
                        .productionQuantityFactorSnapshot(null)
                        .plannedQuantitySnapshot(null)
                        .build());

        MesTeamLeaderOrderProcessTarget target = service.requireTarget(
                activeOrder(35L, 980022L), 980631L, 922985L);

        assertEquals(980631L, target.routeProcessId());
        assertEquals(922985L, target.processId());
        assertAmount("10.000000", target.erpFixedQuantity());
        assertAmount("1.000000", target.productionQuantityFactor());
        assertAmount("10.000000", target.plannedQuantity());
    }

    @Test
    void shouldReturnEmptyWhenActiveOrderDoesNotHaveCurrentRouteProcessSnapshot() {
        when(processSnapshotMapper.selectByActiveOrderAndProcess(35L, 980645L, 922985L)).thenReturn(null);

        Optional<MesTeamLeaderOrderProcessTarget> target = service.findTarget(
                activeOrder(35L, 980022L), 980645L, 922985L);

        assertTrue(target.isEmpty());
    }

    @Test
    void shouldStillRejectNonPositiveProductionFactor() {
        when(processSnapshotMapper.selectByActiveOrderAndProcess(35L, 980631L, 922985L))
                .thenReturn(MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                        .activeOrderId(35L)
                        .workOrderId(980022L)
                        .routeProcessId(980631L)
                        .processId(922985L)
                        .erpFixedQuantitySnapshot(new BigDecimal("10.000000"))
                        .productionQuantityFactorSnapshot(BigDecimal.ZERO)
                        .plannedQuantitySnapshot(new BigDecimal("10.000000"))
                        .build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.requireTarget(
                activeOrder(35L, 980022L), 980631L, 922985L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED.getCode(), ex.getCode());
    }

    private static MesProcessPoolActiveOrderDO activeOrder(Long id, Long workOrderId) {
        return MesProcessPoolActiveOrderDO.builder()
                .id(id)
                .workOrderId(workOrderId)
                .erpFixedQuantitySnapshot(new BigDecimal("10.000000"))
                .build();
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
