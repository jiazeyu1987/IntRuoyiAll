package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesReportAllocationPoolQuantityServiceTest {

    private final MesReportAllocationPoolQuantityService service =
            new MesReportAllocationPoolQuantityService();

    @Test
    void shouldUseFullReportedOutputWithoutRequiringPqcBinding() {
        MesProProcessPoolEventDO event = MesProProcessPoolEventDO.builder()
                .id(176L)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .reportOutputQuantity(new BigDecimal("411111"))
                .build();

        BigDecimal quantity = service.requirePoolQuantity(event);

        assertEquals(0, new BigDecimal("411111").compareTo(quantity));
    }

    @Test
    void shouldInitializeFormalOutputFromSubmittedPayload() {
        MesProProcessPoolEventDO event = MesProProcessPoolEventDO.builder()
                .id(176L)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .rawPayload("{\"outputQuantity\":411111}")
                .build();

        BigDecimal quantity = service.requireSubmittedOutputQuantity(event);

        assertEquals(0, new BigDecimal("411111").compareTo(quantity));
    }

    @Test
    void shouldRejectNonProductionEventAsAllocationRoot() {
        MesProProcessPoolEventDO event = MesProProcessPoolEventDO.builder()
                .id(177L)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                .rawPayload("{\"outputQuantity\":1}")
                .build();

        assertThrows(ServiceException.class, () -> service.requirePoolQuantity(event));
    }
}
