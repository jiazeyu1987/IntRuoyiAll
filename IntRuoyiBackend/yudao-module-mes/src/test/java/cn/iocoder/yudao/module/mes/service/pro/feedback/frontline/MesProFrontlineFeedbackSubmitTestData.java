package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackMaterialReqVO;
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
        List<MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO> lossDetails = List.of(
                new MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO()
                        .setReasonId(8301L)
                        .setReasonCode("LOSS-001")
                        .setReasonName("正常损耗")
                        .setQuantity(new BigDecimal("2.500")));
        MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO selectedDevice =
                new MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO()
                        .setDeviceId(501L)
                        .setDeviceCode("PT-A-03")
                        .setDeviceName("压力泵");
        List<MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO> selectedDevices = List.of(selectedDevice);
        List<MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO> deviceParameterReadings = List.of(
                new MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO()
                        .setDeviceId(501L)
                        .setDeviceCode("PT-A-03")
                        .setDeviceName("压力泵")
                        .setParameterCode("pressure")
                        .setParameterName("压力")
                        .setValue(new BigDecimal("50"))
                        .setUnit("MPa")
                        .setLowerLimit(new BigDecimal("20"))
                        .setUpperLimit(new BigDecimal("40"))
                        .setParameterStatus("ABOVE_UPPER"));

        Map<String, Object> entryContent = new LinkedHashMap<>();
        entryContent.put("operatorNote", "frontline original record");

        Map<String, Object> rawPayload = new LinkedHashMap<>();
        rawPayload.put("templateType", "PRODUCTION_SIMPLE");
        rawPayload.put("equipmentParameters", equipmentParameters);
        rawPayload.put("lossDetails", lossDetails);
        rawPayload.put("selectedDevices", selectedDevices);
        rawPayload.put("deviceParameterReadings", deviceParameterReadings);
        rawPayload.put("routePredecessorStatuses", Map.of("P10", "WAITING"));

        return new MesProFrontlineFeedbackSubmitReqVO()
                .setProcessPoolSubmissionIdempotencyKey("P0-SUBMIT-F2-20260730-001")
                .setFrontlineSessionSnapshotId("frontline-session-snapshot-001")
                .setFrontlineSessionSnapshotHash("frontline-session-snapshot-hash-001")
                .setActualEmployeeId(9001L)
                .setSignatureEmployeeId(9001L)
                .setSignaturePassword("sign-123")
                .setMaterialDetails(List.of(
                        new MesProFrontlineFeedbackMaterialReqVO()
                                .setMaterialId(501L)
                                .setOutputQuantity(new BigDecimal("100.500"))
                                .setLossQuantity(new BigDecimal("2.500"))
                                .setLossDetails(lossDetails)
                                .setSelectedDevices(selectedDevices)
                                .setDeviceParameterReadings(deviceParameterReadings),
                        new MesProFrontlineFeedbackMaterialReqVO()
                                .setMaterialId(502L)
                                .setOutputQuantity(new BigDecimal("100.500"))
                                .setLossQuantity(BigDecimal.ZERO)
                                .setLossDetails(List.of())
                                .setSelectedDevices(selectedDevices)
                                .setDeviceParameterReadings(deviceParameterReadings)))
                .setFeedbackPayload(new MesProFrontlineFeedbackPayloadReqVO()
                        .setCode("FB-F2-001")
                        .setType(1)
                        .setWorkstationId(11L)
                        .setRouteId(21L)
                        .setProcessId(31L)
                        .setWorkOrderId(41L)
                        .setTaskId(51L)
                        .setActiveOrderProcessSnapshotId(5101L)
                        .setScheduleOrderId(81L)
                        .setScheduleOrderProcessId(82L)
                        .setItemId(61L)
                        .setExpireDate(LocalDateTime.of(2026, 8, 30, 0, 0))
                        .setScheduledQuantity(new BigDecimal("300.000"))
                        .setOutputQuantity(new BigDecimal("100.500"))
                        .setLossQuantity(new BigDecimal("2.500"))
                        .setLossReasonId(8301L)
                        .setLossDetails(lossDetails)
                        .setSelectedDevices(selectedDevices)
                        .setDeviceParameterReadings(deviceParameterReadings)
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
                        .setActiveOrderId(81L)
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

    static void stubLossReasonValidator(MesFrontlineLossReasonValidator validator) {
        org.mockito.Mockito.lenient().when(validator.requireSnapshotLossReasons(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    List<MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO> details = invocation.getArgument(1);
                    if (details == null) {
                        return List.of();
                    }
                    return details.stream()
                            .filter(detail -> detail != null && detail.getQuantity() != null
                                    && detail.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                            .map(detail -> new MesFrontlineLossReasonSnapshot(
                                    detail.getReasonId(), "LOSS-001", "正常损耗"))
                            .toList();
                });
    }

    static void stubActiveOrderSnapshot(
            cn.iocoder.yudao.module.mes.service.pro.frontline.ActiveOrderSnapshotResolver resolver) {
        org.mockito.Mockito.lenient().when(resolver.requireEffective(81L))
                .thenReturn(new cn.iocoder.yudao.module.mes.service.pro.frontline.ActiveOrderSnapshotResolver
                        .ActiveOrderSnapshot(81L, 41L, 21L, 627L, 71L, 72L, 73L));
    }
}
