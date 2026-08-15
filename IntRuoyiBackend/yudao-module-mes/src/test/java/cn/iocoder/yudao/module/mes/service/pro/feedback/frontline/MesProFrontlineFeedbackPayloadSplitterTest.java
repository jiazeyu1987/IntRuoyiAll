package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.MesProFeedbackSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineRecordbookPayloadReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventCreateReqBO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class MesProFrontlineFeedbackPayloadSplitterTest {

    @Test
    void shouldSplitFeedbackQuantitiesAndRecordbookRawContent() {
        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        LocalDateTime submittedAt = LocalDateTime.of(2026, 7, 30, 9, 10, 11);
        MesFrontlineLossReasonSnapshot lossReasonSnapshot =
                new MesFrontlineLossReasonSnapshot(8301L, "LOSS-001", "正常损耗");

        MesProFrontlineFeedbackSplitPayload splitPayload =
                new MesProFrontlineFeedbackPayloadSplitter().split(reqVO, 9001L, submittedAt, lossReasonSnapshot);

        MesProFeedbackSaveReqVO feedbackPayload = splitPayload.getFeedbackPayload();
        assertEquals(new BigDecimal("100.500"), feedbackPayload.getFeedbackQuantity());
        assertEquals(new BigDecimal("98.000"), feedbackPayload.getQualifiedQuantity());
        assertEquals(new BigDecimal("2.500"), feedbackPayload.getUnqualifiedQuantity());
        assertEquals(new BigDecimal("1.000"), feedbackPayload.getLaborScrapQuantity());
        assertEquals(new BigDecimal("1.500"), feedbackPayload.getMaterialScrapQuantity());
        assertEquals(new BigDecimal("0.000"), feedbackPayload.getOtherScrapQuantity());
        assertEquals(8301L, feedbackPayload.getLossReasonId());
        assertEquals("LOSS-001", feedbackPayload.getLossReasonCodeSnapshot());
        assertEquals("正常损耗", feedbackPayload.getLossReasonNameSnapshot());
        assertEquals(9001L, feedbackPayload.getFeedbackUserId());
        assertEquals(submittedAt, feedbackPayload.getFeedbackTime());
        assertEquals(7001L, feedbackPayload.getApproveUserId());
        assertEquals("frontline production", feedbackPayload.getRemark());
        assertFalse(feedbackPayload.getRemark().contains("pressure"));

        MesProFrontlineRecordbookEntryPayload recordbookPayload = splitPayload.getRecordbookEntryPayload();
        assertEquals(901L, recordbookPayload.getRecordbookId());
        Map<String, Object> content = recordbookPayload.getEntryContent();
        assertFalse(content.containsKey("previousProcessInputQuantity"));
        assertEquals(reqVO.getRecordbookPayload().getEquipmentParameters(), content.get("equipmentParameters"));
        assertEquals(reqVO.getRawPayload(), content.get("rawPayload"));

        MesProcessPoolSubmitEventCreateReqBO eventPayload = splitPayload.getProcessPoolEventPayload();
        assertEquals(81L, eventPayload.getActiveOrderId());
        assertEquals(41L, eventPayload.getWorkOrderId());
        assertEquals(51L, eventPayload.getTaskId());
        assertEquals(21L, eventPayload.getRouteId());
        assertEquals(71L, eventPayload.getRouteProcessId());
        assertEquals(31L, eventPayload.getProcessId());
        assertEquals(11L, eventPayload.getWorkstationId());
        assertEquals(501L, eventPayload.getDeviceId());
        assertEquals(9001L, eventPayload.getDeviceAccountUserId());
        assertEquals(9001L, eventPayload.getActualEmployeeId());
        assertEquals(9001L, eventPayload.getSignatureEmployeeId());
        assertNull(eventPayload.getSignatureId());
        assertEquals("PRODUCTION_SIMPLE", eventPayload.getTemplateType());
        assertEquals(new BigDecimal("100.500"), eventPayload.getOutputQuantity());
        assertEquals(new BigDecimal("2.500"), eventPayload.getLossQuantity());
        assertEquals(reqVO.getRecordbookPayload().getEquipmentParameters(), eventPayload.getEquipmentParameters());
        Map<String, Object> eventRawPayload = eventPayload.getRawPayload();
        assertEquals(81L, eventRawPayload.get("activeOrderId"));
        assertEquals("PRODUCTION_SIMPLE", eventRawPayload.get("templateType"));
        assertEquals(Map.of("P10", "WAITING"), eventRawPayload.get("routePredecessorStatuses"));
        assertEquals(reqVO.getRecordbookPayload().getEquipmentParameters(), eventRawPayload.get("equipmentParameters"));
        assertEquals(new BigDecimal("100.500"), eventRawPayload.get("outputQuantity"));
        assertEquals(new BigDecimal("2.500"), eventRawPayload.get("lossQuantity"));
        assertEquals(8301L, eventRawPayload.get("lossReasonId"));
        assertEquals("LOSS-001", eventRawPayload.get("lossReasonCodeSnapshot"));
        assertEquals("正常损耗", eventRawPayload.get("lossReasonNameSnapshot"));
        assertFalse(eventRawPayload.containsKey("previousProcessInputQuantity"));
        assertEquals(new BigDecimal("50"), eventRawPayload.get("temperature"));
        assertEquals(new BigDecimal("10"), eventRawPayload.get("pressure"));
        assertEquals(submittedAt, eventPayload.getSubmittedAt());
    }

    @Test
    void shouldFlattenFrontendDeviceParameterMapIntoProcessPoolRawPayload() {
        MesProFrontlineFeedbackSubmitReqVO reqVO = MesProFrontlineFeedbackSubmitTestData.buildSubmitReq();
        Map<String, Object> nestedParameters = new LinkedHashMap<>();
        nestedParameters.put("TLW Device", Map.of("TLW-20260731-PRESSURE", new BigDecimal("15")));
        reqVO.getRecordbookPayload().setEquipmentParameters(nestedParameters);
        reqVO.getRawPayload().clear();
        reqVO.getRawPayload().put("templateType", "PRODUCTION_SIMPLE");
        reqVO.getRawPayload().put("deviceParameters", nestedParameters);

        MesProcessPoolSubmitEventCreateReqBO eventPayload =
                new MesProFrontlineFeedbackPayloadSplitter().split(reqVO, 9001L,
                        LocalDateTime.of(2026, 8, 1, 9, 10, 11),
                        new MesFrontlineLossReasonSnapshot(8301L, "LOSS-001", "正常损耗"))
                        .getProcessPoolEventPayload();

        Map<String, Object> eventRawPayload = eventPayload.getRawPayload();
        assertEquals(nestedParameters, eventRawPayload.get("equipmentParameters"));
        assertEquals(new BigDecimal("15"), eventRawPayload.get("TLW-20260731-PRESSURE"));
    }

    @Test
    void recordbookPayloadContractShouldNotExposePreviousProcessInputQuantity() {
        assertFalse(Arrays.stream(MesProFrontlineRecordbookPayloadReqVO.class.getDeclaredFields())
                .anyMatch(field -> "previousProcessInputQuantity".equals(field.getName())));
    }
}
