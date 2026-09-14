package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreateEventReqDTO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreatePqcInspectionReqDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProcessPoolEventFreezeGateTest {

    @Mock private MesProProcessPoolEventMapper eventMapper;
    @Mock private MesProEdhrNonconformanceReviewService nonconformanceReviewService;

    @Test
    void newProductionSubmitIsBlockedWhenNonconformanceReviewIsPending() {
        MesProcessPoolEventServiceImpl service = new MesProcessPoolEventServiceImpl();
        ReflectionTestUtils.setField(service, "processPoolEventMapper", eventMapper);
        ReflectionTestUtils.setField(service, "nonconformanceReviewService", nonconformanceReviewService);
        when(eventMapper.selectSubmitByIdempotency(any())).thenReturn(null);
        doThrow(new ServiceException(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED))
                .when(nonconformanceReviewService).ensureWorkOrderNotFrozen(3001L, "生产报工");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createEvent(validEventReq()));

        assertEquals(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED.getCode(), exception.getCode());
        verify(nonconformanceReviewService).ensureWorkOrderNotFrozen(3001L, "生产报工");
    }

    @Test
    void newPqcSubmitIsBlockedWhenNonconformanceReviewIsPending() {
        MesProcessPoolEventServiceImpl service = new MesProcessPoolEventServiceImpl();
        ReflectionTestUtils.setField(service, "processPoolEventMapper", eventMapper);
        ReflectionTestUtils.setField(service, "nonconformanceReviewService", nonconformanceReviewService);
        when(eventMapper.selectPqcByIdempotency(any())).thenReturn(null);
        doThrow(new ServiceException(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED))
                .when(nonconformanceReviewService).ensureWorkOrderNotFrozen(3001L, "PQC提交");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createPqcInspectionEvent(validPqcReq()));

        assertEquals(PRO_EDHR_NONCONFORMANCE_REVIEW_FROZEN_ACTION_LOCKED.getCode(), exception.getCode());
        verify(nonconformanceReviewService).ensureWorkOrderNotFrozen(3001L, "PQC提交");
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

    private MesProcessPoolCreatePqcInspectionReqDTO validPqcReq() {
        return MesProcessPoolCreatePqcInspectionReqDTO.builder()
                .workOrderId(3001L)
                .productionSubmitEventId(8001L)
                .pqcSubmissionIdempotencyKey("PQC-FREEZE-GATE-2")
                .routeId(4001L)
                .qaProcessId(4301L)
                .actualEmployeeId(5001L)
                .deviceAccountId(5101L)
                .deviceId(5201L)
                .workstationId(5301L)
                .templateType("PQC_SIMPLE")
                .feedbackSourceType("MES_PQC")
                .feedbackSourceId(6002L)
                .recordbookSourceType("MES_RECORDBOOK_ENTRY")
                .recordbookSourceId(6003L)
                .inspectionResult("SUCCESS")
                .rawPayload("{\"inspectionResult\":\"SUCCESS\"}")
                .signatureId(7002L)
                .signatureUserId(5001L)
                .build();
    }
}
