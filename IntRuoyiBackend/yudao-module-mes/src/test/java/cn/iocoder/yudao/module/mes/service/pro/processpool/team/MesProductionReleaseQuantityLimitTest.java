package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProductionReleaseQuantityLimitTest {
    @Mock private MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock private MesProProcessPoolEventMapper productionEventMapper;
    @InjectMocks private MesTeamLeaderActiveOrderReleaseGenerationService service;

    @BeforeEach
    void injectEventReader() {
        ReflectionTestUtils.setField(service, "productionEventMapper", productionEventMapper);
    }

    @ParameterizedTest
    @CsvSource({"1.1,1100,5000,false", "1.1,1101,5000,true", "1.25,1250,1050,false", "1.25,1251,5000,true"})
    void releaseUsesLossCeilingIndependentlyOfScheduling(String coefficient, String quantity,
                                                       String plannedQuantity, boolean blocked) {
        var order = MesProcessPoolActiveOrderDO.builder().id(1L).workOrderId(2L).build();
        var snapshot = MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .activeOrderId(1L).workOrderId(2L).routeProcessId(3L).processId(4L)
                .erpFixedQuantitySnapshot(new BigDecimal("1000"))
                .plannedQuantitySnapshot(new BigDecimal(plannedQuantity))
                .productionConfigSnapshotJson("{\"outputMaterialIds\":[],\"lossCoefficient\":" + coefficient + "}")
                .build();
        var allocation = MesProcessPoolReportAllocationDO.builder().id(5L).eventId(6L)
                .activeOrderId(1L).workOrderId(2L).routeProcessId(3L).processId(4L)
                .allocatedQuantity(new BigDecimal(quantity)).build();
        when(allocationMapper.selectListByActiveOrderIds(List.of(1L))).thenReturn(List.of(allocation));
        when(productionEventMapper.selectProductionSubmitsByIdsForUpdate(anyList())).thenReturn(List.of());
        Runnable validate = () -> ReflectionTestUtils.invokeMethod(service,
                "requireNoProductionQuantityConflict", order, List.of(snapshot));
        if (blocked) {
            var failure = assertThrows(MesReleaseFlowBlockerException.class, validate::run);
            assertEquals(MesReleaseFlowBlockerType.PRODUCTION_QUANTITY_CONFLICT,
                    failure.getFailure().getBlockers().get(0).getBlockerType());
        } else {
            assertDoesNotThrow(validate::run);
        }
    }
}
