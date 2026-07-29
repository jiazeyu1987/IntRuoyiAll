package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreatePqcInspectionReqDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_PQC_RESULT_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(MesProcessPoolEventServiceImpl.class)
class MesProcessPoolPqcEventTest extends BaseDbUnitTest {

    @Resource
    private MesProcessPoolEventService processPoolEventService;

    @Resource
    private MesProProcessPoolEventMapper processPoolEventMapper;
    @Resource
    private MesProProcessPoolPqcRecordMapper pqcRecordMapper;

    @Test
    void shouldStorePqcInspectionAsPoolEvent() {
        MesProcessPoolCreatePqcInspectionReqDTO req = validPqcReq();

        Long eventId = processPoolEventService.createPqcInspectionEvent(req);

        MesProProcessPoolEventDO event = processPoolEventMapper.selectById(eventId);
        assertEquals("PQC_INSPECTION", event.getEventType());
        assertEquals(req.getTemplateType(), event.getTemplateType());
        assertEquals(req.getActualEmployeeId(), event.getSignatureUserId());
        assertNotNull(event.getServerSubmitTime());

        MesProProcessPoolPqcRecordDO pqcRecord = pqcRecordMapper.selectByEventId(eventId);
        assertNotNull(pqcRecord);
        assertEquals(event.getId(), pqcRecord.getEventId());
        assertEquals(event.getPoolId(), pqcRecord.getPoolId());
        assertEquals(req.getWorkOrderId(), pqcRecord.getWorkOrderId());
        assertEquals(req.getRouteId(), pqcRecord.getRouteId());
        assertEquals(req.getRouteProcessId(), pqcRecord.getRouteProcessId());
        assertEquals(req.getProcessId(), pqcRecord.getProcessId());
        assertEquals(req.getInspectionResult(), pqcRecord.getInspectionResult());
        assertEquals(req.getActualEmployeeId(), pqcRecord.getActualEmployeeId());
        assertEquals(req.getSignatureId(), pqcRecord.getSignatureId());
        assertEquals(event.getServerSubmitTime(), pqcRecord.getServerSubmitTime());
        assertEquals(req.getRawPayload(), pqcRecord.getRawPayload());
    }

    @Test
    void shouldRejectUnsupportedPqcInspectionResult() {
        MesProcessPoolCreatePqcInspectionReqDTO req = validPqcReq();
        req.setInspectionResult("RECHECK");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> processPoolEventService.createPqcInspectionEvent(req));

        assertEquals(PRO_PROCESS_POOL_PQC_RESULT_INVALID.getCode(), ex.getCode());
        assertEquals(0L, processPoolEventMapper.selectCount());
        assertEquals(0L, pqcRecordMapper.selectCount());
    }

    private static MesProcessPoolCreatePqcInspectionReqDTO validPqcReq() {
        Long actualEmployeeId = randomLongId();
        return MesProcessPoolCreatePqcInspectionReqDTO.builder()
                .workOrderId(randomLongId())
                .routeId(randomLongId())
                .routeProcessId(randomLongId())
                .processId(randomLongId())
                .actualEmployeeId(actualEmployeeId)
                .deviceAccountId(randomLongId())
                .deviceId(randomLongId())
                .workstationId(randomLongId())
                .templateType("PQC_SIMPLE")
                .feedbackSourceType("MES_PQC")
                .feedbackSourceId(randomLongId())
                .recordbookSourceType("MES_RECORDBOOK_ENTRY")
                .recordbookSourceId(randomLongId())
                .inspectionResult("SUCCESS")
                .rawPayload("{\"inspectionResult\":\"SUCCESS\"}")
                .signatureId(randomLongId())
                .signatureUserId(actualEmployeeId)
                .clientSubmitTime(LocalDateTime.of(2000, 1, 1, 8, 0))
                .build();
    }
}
