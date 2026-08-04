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
            feedbackPayload.setQualifiedQuantity(feedback.getOutputQuantity().subtract(lossQuantity).max(BigDecimal.ZERO));
        }
        feedbackPayload.setUnqualifiedQuantity(lossQuantity);
        feedbackPayload.setLaborScrapQuantity(feedback.getLaborScrapQuantity());
        feedbackPayload.setMaterialScrapQuantity(feedback.getMaterialScrapQuantity());
        feedbackPayload.setOtherScrapQuantity(feedback.getOtherScrapQuantity());
        feedbackPayload.setFeedbackUserId(reqVO.getActualEmployeeId());
        feedbackPayload.setFeedbackTime(submittedAt);
        feedbackPayload.setApproveUserId(feedback.getApproveUserId());
        feedbackPayload.setRemark(feedback.getRemark());

        Map<String, Object> entryContent = new LinkedHashMap<>(recordbook.getEntryContent());
        entryContent.put("equipmentParameters", recordbook.getEquipmentParameters());
        entryContent.put("rawPayload", reqVO.getRawPayload());
        MesProFrontlineRecordbookEntryPayload recordbookEntryPayload = new MesProFrontlineRecordbookEntryPayload()
                .setRecordbookId(recordbook.getRecordbookId())
                .setEntryTitle(recordbook.getEntryTitle())
                .setEntryContent(entryContent)
                .setTagCodes(recordbook.getTagCodes())
                .setIdempotencyKey(recordbook.getIdempotencyKey())
                .setRemark(recordbook.getRemark());

        Map<String, Object> processPoolRawPayload = buildProcessPoolRawPayload(reqVO, feedback, recordbook);
        MesProcessPoolSubmitEventCreateReqBO eventPayload = new MesProcessPoolSubmitEventCreateReqBO()
                .setProcessPoolSubmissionIdempotencyKey(reqVO.getProcessPoolSubmissionIdempotencyKey())
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
                .setEquipmentParameters(recordbook.getEquipmentParameters())
                .setRawPayload(processPoolRawPayload)
                .setSubmittedAt(submittedAt);

        return new MesProFrontlineFeedbackSplitPayload()
                .setFeedbackPayload(feedbackPayload)
                .setRecordbookEntryPayload(recordbookEntryPayload)
                .setProcessPoolEventPayload(eventPayload);
    }

    private Map<String, Object> buildProcessPoolRawPayload(MesProFrontlineFeedbackSubmitReqVO reqVO,
                                                           MesProFrontlineFeedbackPayloadReqVO feedback,
                                                           MesProFrontlineRecordbookPayloadReqVO recordbook) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (reqVO.getRawPayload() != null) {
        payload.putAll(reqVO.getRawPayload());
        }
        payload.put("outputQuantity", feedback.getOutputQuantity());
        payload.put("lossQuantity", feedback.getLossQuantity());
        payload.put("equipmentParameters", recordbook.getEquipmentParameters());
        if (recordbook.getEquipmentParameters() != null) {
            recordbook.getEquipmentParameters().forEach((code, value) -> {
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

}
