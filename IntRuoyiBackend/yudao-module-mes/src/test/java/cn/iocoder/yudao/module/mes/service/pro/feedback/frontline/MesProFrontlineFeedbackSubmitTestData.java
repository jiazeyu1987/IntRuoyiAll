package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineProcessPoolContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineRecordbookPayloadReqVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MesProFrontlineFeedbackSubmitTestData {

    private MesProFrontlineFeedbackSubmitTestData() {
    }

    static MesProFrontlineFeedbackSubmitReqVO buildSubmitReq() {
        Map<String, Object> equipmentParameters = new LinkedHashMap<>();
        equipmentParameters.put("temperature", new BigDecimal("50"));
        equipmentParameters.put("pressure", new BigDecimal("10"));

        Map<String, Object> entryContent = new LinkedHashMap<>();
        entryContent.put("operatorNote", "frontline original record");

        Map<String, Object> rawPayload = new LinkedHashMap<>();
        rawPayload.put("templateType", "PRODUCTION_SIMPLE");
        rawPayload.put("equipmentParameters", equipmentParameters);
        rawPayload.put("routePredecessorStatuses", Map.of("P10", "WAITING"));

        return new MesProFrontlineFeedbackSubmitReqVO()
                .setActualEmployeeId(3001L)
                .setSignatureId(4001L)
                .setSignatureEmployeeId(3001L)
                .setFeedbackPayload(new MesProFrontlineFeedbackPayloadReqVO()
                        .setCode("FB-F2-001")
                        .setType(1)
                        .setWorkstationId(11L)
                        .setRouteId(21L)
                        .setProcessId(31L)
                        .setWorkOrderId(41L)
                        .setTaskId(51L)
                        .setScheduleOrderId(81L)
                        .setScheduleOrderProcessId(82L)
                        .setItemId(61L)
                        .setExpireDate(LocalDateTime.of(2026, 8, 30, 0, 0))
                        .setScheduledQuantity(new BigDecimal("300.000"))
                        .setOutputQuantity(new BigDecimal("100.500"))
                        .setLossQuantity(new BigDecimal("2.500"))
                        .setLaborScrapQuantity(new BigDecimal("1.000"))
                        .setMaterialScrapQuantity(new BigDecimal("1.500"))
                        .setOtherScrapQuantity(new BigDecimal("0.000"))
                        .setApproveUserId(7001L)
                        .setRemark("frontline production"))
                .setRecordbookPayload(new MesProFrontlineRecordbookPayloadReqVO()
                        .setRecordbookId(901L)
                        .setEntryTitle("F2 production original")
                        .setEntryContent(entryContent)
                        .setEquipmentParameters(equipmentParameters)
                        .setTagCodes(List.of("F2-RAW"))
                        .setIdempotencyKey("F2-20260730-001")
                        .setRemark("recordbook original"))
                .setProcessPoolContext(new MesProFrontlineProcessPoolContextReqVO()
                        .setWorkOrderId(41L)
                        .setTaskId(51L)
                        .setRouteId(21L)
                        .setRouteProcessId(71L)
                        .setProcessId(31L)
                        .setWorkstationId(11L)
                        .setDeviceId(501L)
                        .setDeviceAccountUserId(9001L)
                        .setTemplateType("PRODUCTION_SIMPLE"))
                .setRawPayload(rawPayload);
    }
}
