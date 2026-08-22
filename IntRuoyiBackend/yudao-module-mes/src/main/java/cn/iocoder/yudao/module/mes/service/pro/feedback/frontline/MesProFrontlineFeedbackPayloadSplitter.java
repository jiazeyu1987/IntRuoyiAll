package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.MesProFeedbackSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineProcessPoolContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineRecordbookPayloadReqVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventCreateReqBO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MesProFrontlineFeedbackPayloadSplitter {

    public MesProFrontlineFeedbackSplitPayload split(MesProFrontlineFeedbackSubmitReqVO reqVO,
                                                     Long loginUserId,
                                                     LocalDateTime submittedAt) {
        return split(reqVO, loginUserId, submittedAt, null);
    }

    public MesProFrontlineFeedbackSplitPayload split(MesProFrontlineFeedbackSubmitReqVO reqVO,
                                                     Long loginUserId,
                                                     LocalDateTime submittedAt,
                                                     MesFrontlineLossReasonSnapshot lossReasonSnapshot) {
        MesProFrontlineFeedbackPayloadReqVO feedback = reqVO.getFeedbackPayload();
        MesProFrontlineRecordbookPayloadReqVO recordbook = reqVO.getRecordbookPayload();
        MesProFrontlineProcessPoolContextReqVO context = reqVO.getProcessPoolContext();

        MesProFeedbackSaveReqVO feedbackPayload = new MesProFeedbackSaveReqVO();
        feedbackPayload.setCode(feedback.getCode());
        feedbackPayload.setType(feedback.getType());
        feedbackPayload.setWorkstationId(feedback.getWorkstationId());
        feedbackPayload.setRouteId(feedback.getRouteId());
        feedbackPayload.setProcessId(feedback.getProcessId());
        feedbackPayload.setWorkOrderId(feedback.getWorkOrderId());
        feedbackPayload.setTaskId(feedback.getTaskId());
        feedbackPayload.setScheduleOrderId(feedback.getScheduleOrderId());
        feedbackPayload.setScheduleOrderProcessId(feedback.getScheduleOrderProcessId());
        feedbackPayload.setItemId(feedback.getItemId());
        feedbackPayload.setExpireDate(feedback.getExpireDate());
        feedbackPayload.setScheduledQuantity(feedback.getScheduledQuantity());
        feedbackPayload.setFeedbackQuantity(feedback.getOutputQuantity());
        BigDecimal lossQuantity = feedback.getLossQuantity() == null ? BigDecimal.ZERO : feedback.getLossQuantity();
        if (feedback.getOutputQuantity() != null) {
            feedbackPayload.setQualifiedQuantity(feedback.getOutputQuantity().subtract(lossQuantity));
        }
        feedbackPayload.setUnqualifiedQuantity(lossQuantity);
        feedbackPayload.setLaborScrapQuantity(feedback.getLaborScrapQuantity());
        feedbackPayload.setMaterialScrapQuantity(feedback.getMaterialScrapQuantity());
        feedbackPayload.setOtherScrapQuantity(feedback.getOtherScrapQuantity());
        if (lossReasonSnapshot != null) {
            feedbackPayload.setLossReasonId(lossReasonSnapshot.reasonId());
            feedbackPayload.setLossReasonCodeSnapshot(lossReasonSnapshot.reasonCode());
            feedbackPayload.setLossReasonNameSnapshot(lossReasonSnapshot.reasonName());
        }
        feedbackPayload.setFeedbackUserId(reqVO.getActualEmployeeId());
        feedbackPayload.setFeedbackTime(submittedAt);
        feedbackPayload.setApproveUserId(feedback.getApproveUserId());
        feedbackPayload.setRemark(feedback.getRemark());

        Map<String, Object> equipmentParameters = resolveEquipmentParameters(reqVO, recordbook);
        MesProFrontlineRecordbookSourceSnapshot recordbookSourceSnapshot = null;
        if (recordbook != null) {
            Map<String, Object> entryContent = new LinkedHashMap<>(recordbook.getEntryContent());
            entryContent.put("equipmentParameters", equipmentParameters);
            entryContent.put("rawPayload", reqVO.getRawPayload());
            recordbookSourceSnapshot = new MesProFrontlineRecordbookSourceSnapshot()
                    .setRecordbookId(recordbook.getRecordbookId())
                    .setEntryTitle(recordbook.getEntryTitle())
                    .setEntryContent(entryContent)
                    .setTagCodes(recordbook.getTagCodes())
                    .setIdempotencyKey(recordbook.getIdempotencyKey())
                    .setRemark(recordbook.getRemark());
        }

        Map<String, Object> processPoolRawPayload = buildProcessPoolRawPayload(reqVO, feedback, equipmentParameters,
                lossReasonSnapshot);
        MesProcessPoolSubmitEventCreateReqBO eventPayload = new MesProcessPoolSubmitEventCreateReqBO()
                .setProcessPoolSubmissionIdempotencyKey(reqVO.getProcessPoolSubmissionIdempotencyKey())
                .setActiveOrderId(context.getActiveOrderId())
                .setWorkOrderId(context.getWorkOrderId())
                .setTaskId(context.getTaskId())
                .setRouteId(context.getRouteId())
                .setRouteProcessId(context.getRouteProcessId())
                .setProcessId(context.getProcessId())
                .setWorkstationId(context.getWorkstationId())
                .setDeviceId(context.getDeviceId())
                .setDeviceAccountUserId(context.getDeviceAccountUserId())
                .setActualEmployeeId(reqVO.getActualEmployeeId())
                .setSignatureEmployeeId(reqVO.getSignatureEmployeeId())
                .setSignatureId(reqVO.getSignatureId())
                .setTemplateType(context.getTemplateType())
                .setOutputQuantity(feedback.getOutputQuantity())
                .setLossQuantity(feedback.getLossQuantity())
                .setLossReasonId(lossReasonSnapshot == null ? null : lossReasonSnapshot.reasonId())
                .setLossReasonCodeSnapshot(lossReasonSnapshot == null ? null : lossReasonSnapshot.reasonCode())
                .setLossReasonNameSnapshot(lossReasonSnapshot == null ? null : lossReasonSnapshot.reasonName())
                .setLossDetails(feedback.getLossDetails())
                .setSelectedDevice(feedback.getSelectedDevice())
                .setDeviceParameterReadings(feedback.getDeviceParameterReadings())
                .setEquipmentParameters(equipmentParameters)
                .setRawPayload(processPoolRawPayload)
                .setSubmittedAt(submittedAt);

        return new MesProFrontlineFeedbackSplitPayload()
                .setFeedbackPayload(feedbackPayload)
                .setRecordbookSourceSnapshot(recordbookSourceSnapshot)
                .setProcessPoolEventPayload(eventPayload);
    }

    private Map<String, Object> buildProcessPoolRawPayload(MesProFrontlineFeedbackSubmitReqVO reqVO,
                                                           MesProFrontlineFeedbackPayloadReqVO feedback,
                                                           Map<String, Object> equipmentParameters,
                                                           MesFrontlineLossReasonSnapshot lossReasonSnapshot) {
        MesProFrontlineRecordbookPayloadReqVO recordbook = reqVO.getRecordbookPayload();
        Map<String, Object> payload = new LinkedHashMap<>();
        if (reqVO.getRawPayload() != null) {
            payload.putAll(reqVO.getRawPayload());
        }
        payload.put("activeOrderId", reqVO.getProcessPoolContext().getActiveOrderId());
        Map<String, Object> activeOrderProcessSnapshot = new LinkedHashMap<>();
        activeOrderProcessSnapshot.put("activeOrderId", reqVO.getProcessPoolContext().getActiveOrderId());
        activeOrderProcessSnapshot.put("activeOrderProcessSnapshotId",
                feedback.getActiveOrderProcessSnapshotId());
        activeOrderProcessSnapshot.put("routeProcessId", reqVO.getProcessPoolContext().getRouteProcessId());
        activeOrderProcessSnapshot.put("processId", reqVO.getProcessPoolContext().getProcessId());
        payload.put("activeOrderProcess", activeOrderProcessSnapshot);
        payload.put("outputQuantity", feedback.getOutputQuantity());
        payload.put("lossQuantity", feedback.getLossQuantity());
        payload.put("lossDetails", feedback.getLossDetails());
        payload.put("selectedDevice", feedback.getSelectedDevice());
        payload.put("deviceParameterReadings", feedback.getDeviceParameterReadings());
        if (lossReasonSnapshot != null) {
            payload.put("lossReasonId", lossReasonSnapshot.reasonId());
            payload.put("lossReasonCodeSnapshot", lossReasonSnapshot.reasonCode());
            payload.put("lossReasonNameSnapshot", lossReasonSnapshot.reasonName());
        }
        if (recordbook != null) {
            Map<String, Object> recordbookSnapshot = new LinkedHashMap<>();
            recordbookSnapshot.put("recordbookId", recordbook.getRecordbookId());
            recordbookSnapshot.put("entryTitle", recordbook.getEntryTitle());
            recordbookSnapshot.put("entryContent", recordbook.getEntryContent());
            recordbookSnapshot.put("tagCodes", recordbook.getTagCodes());
            recordbookSnapshot.put("idempotencyKey", recordbook.getIdempotencyKey());
            recordbookSnapshot.put("remark", recordbook.getRemark());
            payload.put("recordbookSourceSnapshot", recordbookSnapshot);
        }
        payload.put("equipmentParameters", equipmentParameters);
        if (equipmentParameters != null) {
            equipmentParameters.forEach((code, value) -> {
                if (value instanceof Map<?, ?> nestedParameters) {
                    nestedParameters.forEach((nestedCode, nestedValue) -> {
                        if (nestedCode != null) {
                            payload.put(String.valueOf(nestedCode), nestedValue);
                        }
                    });
                } else if (code != null) {
                    payload.put(code, value);
                }
            });
        }
        return payload;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveEquipmentParameters(MesProFrontlineFeedbackSubmitReqVO reqVO,
                                                           MesProFrontlineRecordbookPayloadReqVO recordbook) {
        if (recordbook != null) {
            return recordbook.getEquipmentParameters();
        }
        Object rawEquipmentParameters = reqVO.getRawPayload() == null ? null : reqVO.getRawPayload()
                .get("equipmentParameters");
        if (rawEquipmentParameters instanceof Map<?, ?> parameters) {
            return (Map<String, Object>) parameters;
        }
        return null;
    }

}
