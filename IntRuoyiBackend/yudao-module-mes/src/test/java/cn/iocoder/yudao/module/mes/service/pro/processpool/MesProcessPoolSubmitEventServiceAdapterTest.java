package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreateEventReqDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProcessPoolSubmitEventServiceAdapterTest {

    @Mock
    private MesProcessPoolEventService eventService;

    private MesProcessPoolSubmitEventService submitEventService;

    @BeforeEach
    void setUp() {
        submitEventService = new MesProcessPoolSubmitEventServiceImpl(eventService);
    }

    @Test
    void shouldMapFrontlineSubmitEventToFormalProcessPoolEvent() {
        when(eventService.createEvent(org.mockito.ArgumentMatchers.any())).thenReturn(801L);
        Map<String, Object> rawPayload = new LinkedHashMap<>();
        rawPayload.put("templateType", "PRODUCTION_SIMPLE");
        rawPayload.put("fieldPressure", new BigDecimal("50.000"));

        Long eventId = submitEventService.createSubmitEvent(new MesProcessPoolSubmitEventCreateReqBO()
                .setFeedbackId(501L)
                .setProcessPoolSubmissionIdempotencyKey("P0-SUBMIT-F2-20260730-001")
                .setRecordbookEntryId(701L)
                .setRecordbookEventId(702L)
                .setWorkOrderId(41L)
                .setTaskId(51L)
                .setRouteId(21L)
                .setRouteProcessId(71L)
                .setProcessId(31L)
                .setWorkstationId(11L)
                .setDeviceId(601L)
                .setDeviceAccountUserId(9001L)
                .setActualEmployeeId(3001L)
                .setSignatureEmployeeId(3001L)
                .setSignatureId(4001L)
                .setTemplateType("PRODUCTION_SIMPLE")
                .setOutputQuantity(new BigDecimal("100.500"))
                .setLossQuantity(new BigDecimal("2.500"))
                .setEquipmentParameters(Map.of("pressure", new BigDecimal("50.000")))
                .setRawPayload(rawPayload)
                .setSubmittedAt(LocalDateTime.of(2026, 7, 30, 9, 0)));

        assertEquals(801L, eventId);
        ArgumentCaptor<MesProcessPoolCreateEventReqDTO> captor =
                ArgumentCaptor.forClass(MesProcessPoolCreateEventReqDTO.class);
        verify(eventService).createEvent(captor.capture());
        MesProcessPoolCreateEventReqDTO dto = captor.getValue();
        assertEquals(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT, dto.getEventType());
        assertEquals("P0-SUBMIT-F2-20260730-001", dto.getEventIdempotencyKey());
        assertEquals(41L, dto.getWorkOrderId());
        assertEquals(21L, dto.getRouteId());
        assertEquals(71L, dto.getRouteProcessId());
        assertEquals(31L, dto.getProcessId());
        assertEquals(9001L, dto.getDeviceAccountId());
        assertEquals(3001L, dto.getActualEmployeeId());
        assertEquals(3001L, dto.getSignatureUserId());
        assertEquals(4001L, dto.getSignatureId());
        assertEquals("MES_PRO_FEEDBACK", dto.getFeedbackSourceType());
        assertEquals(501L, dto.getFeedbackSourceId());
        assertEquals(701L, dto.getRecordbookEntryId());
        assertEquals("MES_PRO_EDHR_RECORD_BOOK_EVENT", dto.getRecordbookSourceType());
        assertEquals(702L, dto.getRecordbookSourceId());
        assertTrue(dto.getRawPayload().contains("\"fieldPressure\""));
        assertNotNull(dto.getQuantityFragments());
        assertEquals(2, dto.getQuantityFragments().size());
        assertEquals("OUTPUT", dto.getQuantityFragments().get(0).getSourceQuantityType());
        assertEquals(0, new BigDecimal("100.500")
                .compareTo(dto.getQuantityFragments().get(0).getTotalQuantity()));
        assertFalse(dto.getQuantityFragments().get(0).getRawPayload().contains("previousProcessInputQuantity"));
        assertEquals("LOSS", dto.getQuantityFragments().get(1).getSourceQuantityType());
        assertEquals(0, new BigDecimal("2.500")
                .compareTo(dto.getQuantityFragments().get(1).getTotalQuantity()));
        assertFalse(dto.getQuantityFragments().get(1).getRawPayload().contains("previousProcessInputQuantity"));
        assertFalse(Arrays.stream(MesProcessPoolSubmitEventCreateReqBO.class.getDeclaredFields())
                .anyMatch(field -> "previousProcessInputQuantity".equals(field.getName())));
    }
}
