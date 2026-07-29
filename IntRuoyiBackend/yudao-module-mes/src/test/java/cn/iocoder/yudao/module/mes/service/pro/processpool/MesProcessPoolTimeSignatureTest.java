package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreateEventReqDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SIGNATURE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SIGNATURE_EMPLOYEE_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(MesProcessPoolEventServiceImpl.class)
class MesProcessPoolTimeSignatureTest extends BaseDbUnitTest {

    @Resource
    private MesProcessPoolEventService processPoolEventService;

    @Resource
    private MesProProcessPoolEventMapper processPoolEventMapper;

    @Test
    void shouldUseServerSubmitTimeAndKeepActualEmployeeAsSignatureSubject() {
        LocalDateTime clientTime = LocalDateTime.of(2000, 1, 1, 8, 0);
        MesProcessPoolCreateEventReqDTO req = validEventReq();
        req.setClientSubmitTime(clientTime);
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        Long eventId = processPoolEventService.createEvent(req);

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        MesProProcessPoolEventDO event = processPoolEventMapper.selectById(eventId);
        assertFalse(clientTime.equals(event.getServerSubmitTime()));
        assertTrue(!event.getServerSubmitTime().isBefore(before) && !event.getServerSubmitTime().isAfter(after));
        assertEquals(req.getActualEmployeeId(), event.getSignatureUserId());
    }

    @Test
    void shouldRejectSignatureEmployeeMismatch() {
        MesProcessPoolCreateEventReqDTO req = validEventReq();
        req.setSignatureUserId(randomLongId());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> processPoolEventService.createEvent(req));

        assertEquals(PRO_PROCESS_POOL_SIGNATURE_EMPLOYEE_MISMATCH.getCode(), ex.getCode());
        assertEquals(0L, processPoolEventMapper.selectCount());
    }

    @Test
    void shouldRejectDuplicateSignature() {
        MesProcessPoolCreateEventReqDTO first = validEventReq();
        Long signatureId = randomLongId();
        first.setSignatureId(signatureId);
        MesProcessPoolCreateEventReqDTO second = validEventReq();
        second.setSignatureId(signatureId);

        processPoolEventService.createEvent(first);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> processPoolEventService.createEvent(second));

        assertEquals(PRO_PROCESS_POOL_SIGNATURE_DUPLICATE.getCode(), ex.getCode());
        assertEquals(1L, processPoolEventMapper.selectCount());
    }

    private static MesProcessPoolCreateEventReqDTO validEventReq() {
        Long actualEmployeeId = randomLongId();
        return MesProcessPoolCreateEventReqDTO.builder()
                .eventType("PRODUCTION_SUBMIT")
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
                .recordbookSourceType("MES_RECORDBOOK_ENTRY")
                .recordbookSourceId(randomLongId())
                .rawPayload("{\"outputQuantity\":10}")
                .signatureId(randomLongId())
                .signatureUserId(actualEmployeeId)
                .signatureSnapshot("{\"method\":\"E_SIGN\"}")
                .build();
    }
}
