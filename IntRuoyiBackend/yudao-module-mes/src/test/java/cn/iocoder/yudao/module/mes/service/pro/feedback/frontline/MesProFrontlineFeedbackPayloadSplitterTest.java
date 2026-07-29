package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineProcessPoolContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineRecordbookPayloadReqVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class MesProFrontlineFeedbackPayloadSplitterTest {

    private final MesProFrontlineFeedbackPayloadSplitter splitter = new MesProFrontlineFeedbackPayloadSplitter();

    @Test
    void split_routesQuantitiesToFeedbackAndRawFactsToRecordbookAndPoolEvent() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 7, 30, 9, 30);
        MesProFrontlineFeedbackSubmitReqVO reqVO = buildReq();

        MesProFrontlineFeedbackSplitPayload split = splitter.split(reqVO, 9001L, submittedAt);

        assertEquals(new BigDecimal("12.500"), split.getFeedbackPayload().getFeedbackQuantity());
        assertEquals(new BigDecimal("1.250"), split.getFeedbackPayload().getUnqualifiedQuantity());
        assertEquals(new BigDecimal("0.500"), split.getFeedbackPayload().getLaborScrapQuantity());
        assertEquals(new BigDecimal("0.750"), split.getFeedbackPayload().getMaterialScrapQuantity());
        assertEquals(1001L, split.getFeedbackPayload().getFeedbackUserId());
        assertEquals(submittedAt, split.getFeedbackPayload().getFeedbackTime());
        assertFalse(String.valueOf(split.getFeedbackPayload().getRemark()).contains("sterilizerPressure"));

        Map<String, Object> entryContent = split.getRecordbookEntryPayload().getEntryContent();
        assertEquals(new BigDecimal("10.000"), entryContent.get("previousProcessInputQuantity"));
        assertEquals(Map.of("sterilizerPressure", new BigDecimal("50.000")),
                entryContent.get("equipmentParameters"));
        assertEquals(reqVO.getRawPayload(), entryContent.get("rawPayload"));

        assertSame(reqVO.getRawPayload(), split.getProcessPoolEventPayload().getRawPayload());
        assertEquals(new BigDecimal("50.000"),
                split.getProcessPoolEventPayload().getEquipmentParameters().get("sterilizerPressure"));
    }

    static MesProFrontlineFeedbackSubmitReqVO buildReq() {
        MesProFrontlineFeedbackPayloadReqVO feedbackPayload = new MesProFrontlineFeedbackPayloadReqVO()
                .setCode("FB-F2-001")
                .setType(1)
                .setWorkstationId(21L)
                .setRouteId(31L)
                .setProcessId(41L)
                .setWorkOrderId(51L)
                .setTaskId(61L)
                .setItemId(71L)
                .setOutputQuantity(new BigDecimal("12.500"))
                .setLossQuantity(new BigDecimal("1.250"))
                .setLaborScrapQuantity(new BigDecimal("0.500"))
                .setMaterialScrapQuantity(new BigDecimal("0.750"))
                .setOtherScrapQuantity(BigDecimal.ZERO)
                .setApproveUserId(2001L)
                .setRemark("frontline submit");
        MesProFrontlineRecordbookPayloadReqVO recordbookPayload = new MesProFrontlineRecordbookPayloadReqVO()
                .setRecordbookId(81L)
                .setEntryTitle("frontline original")
                .setEntryContent(new LinkedHashMap<>(Map.of("shift", "A")))
                .setPreviousProcessInputQuantity(new BigDecimal("10.000"))
                .setEquipmentParameters(Map.of("sterilizerPressure", new BigDecimal("50.000")))
                .setTagCodes(List.of("FRONTLINE"))
                .setIdempotencyKey("F2-ENTRY-001");
        MesProFrontlineProcessPoolContextReqVO poolContext = new MesProFrontlineProcessPoolContextReqVO()
                .setWorkOrderId(51L)
                .setTaskId(61L)
                .setRouteId(31L)
                .setRouteProcessId(91L)
                .setProcessId(41L)
                .setWorkstationId(21L)
                .setDeviceId(101L)
                .setDeviceAccountUserId(9001L)
                .setTemplateType("PRODUCTION_SIMPLE");
        return new MesProFrontlineFeedbackSubmitReqVO()
                .setFeedbackPayload(feedbackPayload)
                .setRecordbookPayload(recordbookPayload)
                .setProcessPoolContext(poolContext)
                .setActualEmployeeId(1001L)
                .setSignatureId(1101L)
                .setSignatureEmployeeId(1001L)
                .setRawPayload(new LinkedHashMap<>(Map.of("fieldPressure", new BigDecimal("50.000"))));
    }
}
