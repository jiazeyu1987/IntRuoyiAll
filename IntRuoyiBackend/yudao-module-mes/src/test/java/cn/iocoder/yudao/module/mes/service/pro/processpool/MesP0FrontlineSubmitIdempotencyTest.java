package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolQuantityFragmentMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreateEventReqDTO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolQuantityFragmentCreateDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesProductionReportManagementSummaryService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(MesProcessPoolEventServiceImpl.class)
class MesP0FrontlineSubmitIdempotencyTest extends BaseDbUnitTest {

    @Resource
    private MesProcessPoolEventService processPoolEventService;
    @Resource
    private MesProProcessPoolEventMapper processPoolEventMapper;
    @Resource
    private MesProProcessPoolQuantityFragmentMapper quantityFragmentMapper;
    @MockitoBean
    private MesProductionReportManagementSummaryService reportManagementSummaryService;

    @Test
    void shouldReturnSameProductionSubmitEventForDuplicateIdempotencyKey() {
        MesProcessPoolCreateEventReqDTO firstReq = validSubmitReq("P0-SUBMIT-20260803-001");

        Long firstEventId = processPoolEventService.createEvent(firstReq);
        Long duplicateEventId = processPoolEventService.createEvent(validSubmitReq("P0-SUBMIT-20260803-001"));

        assertEquals(firstEventId, duplicateEventId);
        assertEquals(1L, processPoolEventMapper.selectCount());
        assertEquals(1L, quantityFragmentMapper.selectCount());
        MesProProcessPoolEventDO event = processPoolEventMapper.selectById(firstEventId);
        assertEquals("P0-SUBMIT-20260803-001", event.getEventIdempotencyKey());
        assertEquals(firstReq.getFeedbackSourceId(), event.getFeedbackSourceId());
        assertEquals(firstReq.getRecordbookEntryId(), event.getRecordbookEntryId());
        assertEquals(firstReq.getRecordbookSourceId(), event.getRecordbookSourceId());
    }

    @Test
    void shouldFindExistingSubmitEventBeforeFrontlineWritesDuplicates() {
        MesProcessPoolCreateEventReqDTO req = validSubmitReq("P0-SUBMIT-20260803-002");
        Long eventId = processPoolEventService.createEvent(req);

        Optional<MesProcessPoolSubmitEventResult> existing =
                processPoolEventService.findExistingSubmitEvent(validSubmitLookupReq("P0-SUBMIT-20260803-002"));

        assertTrue(existing.isPresent());
        assertEquals(eventId, existing.get().getProcessPoolEventId());
        assertEquals(req.getFeedbackSourceId(), existing.get().getFeedbackId());
        assertEquals(req.getRecordbookEntryId(), existing.get().getRecordbookEntryId());
        assertEquals(req.getRecordbookSourceId(), existing.get().getRecordbookEventId());
    }

    @Test
    void shouldRejectProductionSubmitWithoutIdempotencyKey() {
        MesProcessPoolCreateEventReqDTO req = validSubmitReq(" ");

        ServiceException ex = assertThrows(ServiceException.class, () -> processPoolEventService.createEvent(req));

        assertEquals(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), ex.getCode());
        assertEquals(0L, processPoolEventMapper.selectCount());
    }

    private static MesProcessPoolCreateEventReqDTO validSubmitReq(String idempotencyKey) {
        return validSubmitLookupReq(idempotencyKey)
                .setFeedbackSourceType("MES_PRO_FEEDBACK")
                .setFeedbackSourceId(randomLongId())
                .setRecordbookEntryId(randomLongId())
                .setRecordbookSourceType("MES_PRO_EDHR_RECORD_BOOK_EVENT")
                .setRecordbookSourceId(randomLongId())
                .setRawPayload("{\"outputQuantity\":10}")
                .setSignatureId(randomLongId())
                .setSignatureUserId(3001L)
                .setActualEmployeeId(3001L)
                .setQuantityFragments(List.of(MesProcessPoolQuantityFragmentCreateDTO.builder()
                        .sourceQuantityType("OUTPUT")
                        .qualityStatus("OUTPUT")
                        .totalQuantity(new BigDecimal("10.000"))
                        .rawPayload("{\"sourceQuantityType\":\"OUTPUT\"}")
                        .build()));
    }

    private static MesProcessPoolCreateEventReqDTO validSubmitLookupReq(String idempotencyKey) {
        return MesProcessPoolCreateEventReqDTO.builder()
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .eventIdempotencyKey(idempotencyKey)
                .workOrderId(41001L)
                .routeId(21001L)
                .routeProcessId(71001L)
                .processId(31001L)
                .actualEmployeeId(3001L)
                .deviceAccountId(9001L)
                .deviceId(501L)
                .workstationId(11L)
                .templateType("PRODUCTION_SIMPLE")
                .build();
    }
}
