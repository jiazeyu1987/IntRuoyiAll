package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreateEventReqDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.function.Consumer;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(MesProcessPoolEventServiceImpl.class)
class MesProcessPoolEventServiceTest extends BaseDbUnitTest {

    @Resource
    private MesProcessPoolEventService processPoolEventService;

    @Resource
    private MesProProcessPoolEventMapper processPoolEventMapper;

    @Test
    void shouldRejectEventWhenRequiredContextMissing() {
        List<Consumer<MesProcessPoolCreateEventReqDTO>> missingContextCases = List.of(
                req -> req.setWorkOrderId(null),
                req -> req.setRouteId(null),
                req -> req.setRouteProcessId(null),
                req -> req.setProcessId(null),
                req -> req.setActualEmployeeId(null),
                req -> req.setDeviceAccountId(null),
                req -> req.setDeviceId(null),
                req -> req.setWorkstationId(null),
                req -> req.setTemplateType(null),
                req -> req.setFeedbackSourceType(null),
                req -> req.setFeedbackSourceId(null),
                req -> req.setRecordbookSourceType(null),
                req -> req.setRecordbookSourceId(null),
                req -> req.setRawPayload(" "),
                req -> req.setSignatureId(null),
                req -> req.setSignatureUserId(null)
        );

        for (Consumer<MesProcessPoolCreateEventReqDTO> missingContext : missingContextCases) {
            MesProcessPoolCreateEventReqDTO req = validEventReq();
            missingContext.accept(req);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> processPoolEventService.createEvent(req));
            assertEquals(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), ex.getCode());
        }
        assertEquals(0L, processPoolEventMapper.selectCount());
    }

    @Test
    void shouldPersistEventWithRequiredContext() {
        MesProcessPoolCreateEventReqDTO req = validEventReq();

        Long eventId = processPoolEventService.createEvent(req);

        MesProProcessPoolEventDO event = processPoolEventMapper.selectById(eventId);
        assertEquals(req.getWorkOrderId(), event.getWorkOrderId());
        assertEquals(req.getRouteId(), event.getRouteId());
        assertEquals(req.getRouteProcessId(), event.getRouteProcessId());
        assertEquals(req.getProcessId(), event.getProcessId());
        assertEquals(req.getActualEmployeeId(), event.getActualEmployeeId());
        assertEquals(req.getDeviceAccountId(), event.getDeviceAccountId());
        assertEquals(req.getDeviceId(), event.getDeviceId());
        assertEquals(req.getWorkstationId(), event.getWorkstationId());
        assertEquals(req.getTemplateType(), event.getTemplateType());
        assertEquals(req.getFeedbackSourceType(), event.getFeedbackSourceType());
        assertEquals(req.getFeedbackSourceId(), event.getFeedbackSourceId());
        assertEquals(req.getRecordbookSourceType(), event.getRecordbookSourceType());
        assertEquals(req.getRecordbookSourceId(), event.getRecordbookSourceId());
        assertEquals(req.getRawPayload(), event.getRawPayload());
        assertEquals(req.getSignatureId(), event.getSignatureId());
        assertEquals(req.getSignatureUserId(), event.getSignatureUserId());
    }

    private static MesProcessPoolCreateEventReqDTO validEventReq() {
        Long actualEmployeeId = randomLongId();
        return MesProcessPoolCreateEventReqDTO.builder()
                .eventType("PRODUCTION_SUBMIT")
                .eventIdempotencyKey("P0-SUBMIT-" + randomLongId())
                .workOrderId(randomLongId())
                .routeId(randomLongId())
                .routeProcessId(randomLongId())
                .processId(randomLongId())
                .actualEmployeeId(actualEmployeeId)
                .deviceAccountId(randomLongId())
                .deviceId(randomLongId())
                .workstationId(randomLongId())
                .templateType("PRODUCTION_SIMPLE")
                .feedbackSourceType("MES_PRO_FEEDBACK")
                .feedbackSourceId(randomLongId())
                .recordbookEntryId(randomLongId())
                .recordbookSourceType("MES_RECORDBOOK_ENTRY")
                .recordbookSourceId(randomLongId())
                .rawPayload("{\"outputQuantity\":10}")
                .signatureId(randomLongId())
                .signatureUserId(actualEmployeeId)
                .build();
    }
}
