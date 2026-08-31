package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreateEventReqDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_TEMPORARY_FROZEN_OPERATION_FORBIDDEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProcessPoolEventFreezeGateTest {

    @Mock private MesProProcessPoolEventMapper eventMapper;
    @Mock private MesProWorkOrderMapper workOrderMapper;

    @Test
    void newProductionSubmitIsBlockedWhenWorkOrderIsFrozen() {
        MesProcessPoolEventServiceImpl service = new MesProcessPoolEventServiceImpl();
        ReflectionTestUtils.setField(service, "processPoolEventMapper", eventMapper);
        ReflectionTestUtils.setField(service, "workOrderMapper", workOrderMapper);
        when(eventMapper.selectSubmitByIdempotency(any())).thenReturn(null);
        when(workOrderMapper.selectByIdForUpdate(3001L)).thenReturn(
                new MesProWorkOrderDO().setId(3001L).setTemporaryFrozen(true));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createEvent(validEventReq()));

        assertEquals(PRO_WORK_ORDER_TEMPORARY_FROZEN_OPERATION_FORBIDDEN.getCode(), exception.getCode());
    }

    private MesProcessPoolCreateEventReqDTO validEventReq() {
        return MesProcessPoolCreateEventReqDTO.builder()
                .eventType("PRODUCTION_SUBMIT")
                .eventIdempotencyKey("PQC-FREEZE-GATE-1")
                .workOrderId(3001L)
                .routeId(4001L)
                .routeProcessId(4101L)
                .processId(4201L)
                .actualEmployeeId(5001L)
                .deviceAccountId(5101L)
                .deviceId(5201L)
                .workstationId(5301L)
                .templateType("PRODUCTION_SIMPLE")
                .feedbackSourceType("MES_PRO_FEEDBACK")
                .feedbackSourceId(6001L)
                .rawPayload("{\"outputQuantity\":10}")
                .signatureId(7001L)
                .signatureUserId(5001L)
                .build();
    }
}
