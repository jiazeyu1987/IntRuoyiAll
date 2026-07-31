package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineProcessPoolContextReqVO;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitAuthorizationService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitIdentityCommand;
import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlinePqcResults;
import cn.iocoder.yudao.module.mes.service.pro.frontline.template.FrontlineTemplateCodes;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventCreateReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolEventService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolCreatePqcInspectionReqDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_DEVICE_ACCOUNT_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_LOGIN_USER_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED;

@Service
@Validated
public class MesProFrontlineFeedbackSubmitServiceImpl implements MesProFrontlineFeedbackSubmitService {

    private static final String FEEDBACK_SOURCE_TYPE = "MES_PRO_FEEDBACK";
    private static final String RECORDBOOK_SOURCE_TYPE = "MES_PRO_EDHR_RECORD_BOOK_EVENT";

    private final MesProFeedbackService feedbackService;
    private final MesProFrontlineRecordbookEntryService recordbookEntryService;
    private final MesProcessPoolEventService processPoolEventService;
    private final MesProcessPoolSubmitEventService processPoolSubmitEventService;
    private final MesFrontlineSubmitAuthorizationService submitAuthorizationService;
    private final MesFrontlineSubmitSignatureService submitSignatureService;
    private final MesProFrontlineFeedbackPayloadSplitter payloadSplitter;

    public MesProFrontlineFeedbackSubmitServiceImpl(MesProFeedbackService feedbackService,
                                                    MesProFrontlineRecordbookEntryService recordbookEntryService,
                                                    MesProcessPoolEventService processPoolEventService,
                                                    MesProcessPoolSubmitEventService processPoolSubmitEventService,
                                                    MesFrontlineSubmitAuthorizationService submitAuthorizationService,
                                                    MesFrontlineSubmitSignatureService submitSignatureService,
                                                    MesProFrontlineFeedbackPayloadSplitter payloadSplitter) {
        this.feedbackService = feedbackService;
        this.recordbookEntryService = recordbookEntryService;
        this.processPoolEventService = processPoolEventService;
        this.processPoolSubmitEventService = processPoolSubmitEventService;
        this.submitAuthorizationService = submitAuthorizationService;
        this.submitSignatureService = submitSignatureService;
        this.payloadSplitter = payloadSplitter;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProFrontlineFeedbackSubmitRespVO submit(MesProFrontlineFeedbackSubmitReqVO reqVO) {
        validateSubmitContext(reqVO);
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_LOGIN_USER_REQUIRED);
        }
        Long deviceAccountUserId = reqVO.getProcessPoolContext().getDeviceAccountUserId();
        if (!Objects.equals(deviceAccountUserId, loginUserId)) {
            throw exception(PRO_FRONTLINE_FEEDBACK_DEVICE_ACCOUNT_MISMATCH, deviceAccountUserId);
        }
        reqVO.setSignatureEmployeeId(reqVO.getActualEmployeeId());
        submitAuthorizationService.authorize(buildSubmitIdentityCommand(reqVO, loginUserId));
        Long signatureId = submitSignatureService.recordSubmitSignature(
                reqVO.getActualEmployeeId(), reqVO.getSignaturePassword(), reqVO.getSignatureComment());
        reqVO.setSignatureId(signatureId);

        LocalDateTime submittedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        MesProFrontlineFeedbackSplitPayload splitPayload = payloadSplitter.split(reqVO, loginUserId, submittedAt);

        Long feedbackId = feedbackService.createFeedback(splitPayload.getFeedbackPayload());
        feedbackService.submitFeedback(feedbackId);

        MesProFrontlineRecordbookEntryPayload recordbookEntryPayload = splitPayload.getRecordbookEntryPayload()
                .setFeedbackId(feedbackId);
        MesProFrontlineRecordbookEntryResult recordbookResult =
                recordbookEntryService.createOriginalEntry(recordbookEntryPayload);

        Long processPoolEventId = createProcessPoolEvent(reqVO, splitPayload, feedbackId, recordbookResult);

        return new MesProFrontlineFeedbackSubmitRespVO()
                .setFeedbackId(feedbackId)
                .setRecordbookEntryId(recordbookResult.getRecordbookEntryId())
                .setRecordbookEventId(recordbookResult.getRecordbookEventId())
                .setProcessPoolEventId(processPoolEventId);
    }

    private Long createProcessPoolEvent(MesProFrontlineFeedbackSubmitReqVO reqVO,
                                        MesProFrontlineFeedbackSplitPayload splitPayload,
                                        Long feedbackId,
                                        MesProFrontlineRecordbookEntryResult recordbookResult) {
        if (FrontlineTemplateCodes.PQC_SIMPLIFIED.equals(reqVO.getProcessPoolContext().getTemplateType())) {
            return processPoolEventService.createPqcInspectionEvent(MesProcessPoolCreatePqcInspectionReqDTO.builder()
                    .workOrderId(reqVO.getProcessPoolContext().getWorkOrderId())
                    .routeId(reqVO.getProcessPoolContext().getRouteId())
                    .routeProcessId(reqVO.getProcessPoolContext().getRouteProcessId())
                    .processId(reqVO.getProcessPoolContext().getProcessId())
                    .actualEmployeeId(reqVO.getActualEmployeeId())
                    .deviceAccountId(reqVO.getProcessPoolContext().getDeviceAccountUserId())
                    .deviceId(reqVO.getProcessPoolContext().getDeviceId())
                    .workstationId(reqVO.getProcessPoolContext().getWorkstationId())
                    .templateType(reqVO.getProcessPoolContext().getTemplateType())
                    .feedbackSourceType(FEEDBACK_SOURCE_TYPE)
                    .feedbackSourceId(feedbackId)
                    .recordbookSourceType(RECORDBOOK_SOURCE_TYPE)
                    .recordbookSourceId(recordbookResult.getRecordbookEventId())
                    .inspectionResult(resolvePqcInspectionResult(reqVO.getRawPayload()))
                    .rawPayload(JsonUtils.toJsonString(reqVO.getRawPayload()))
                    .clientSubmitTime(splitPayload.getProcessPoolEventPayload().getSubmittedAt())
                    .signatureId(reqVO.getSignatureId())
                    .signatureUserId(reqVO.getSignatureEmployeeId())
                    .build());
        }
        MesProcessPoolSubmitEventCreateReqBO eventPayload = splitPayload.getProcessPoolEventPayload()
                .setFeedbackId(feedbackId)
                .setRecordbookEntryId(recordbookResult.getRecordbookEntryId())
                .setRecordbookEventId(recordbookResult.getRecordbookEventId());
        return processPoolSubmitEventService.createSubmitEvent(eventPayload);
    }

    private String resolvePqcInspectionResult(Map<String, Object> rawPayload) {
        Object rawResult = rawPayload == null ? null : rawPayload.get("PQC_RESULT");
        if (Objects.equals(FrontlinePqcResults.DETECTION_SUCCESS, rawResult)) {
            return MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS;
        }
        if (Objects.equals(FrontlinePqcResults.DETECTION_FAILED, rawResult)) {
            return MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE;
        }
        throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "PQC_RESULT");
    }

    private void validateSubmitContext(MesProFrontlineFeedbackSubmitReqVO reqVO) {
        if (reqVO == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "request");
        }
        if (reqVO.getFeedbackPayload() == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "feedbackPayload");
        }
        if (reqVO.getRecordbookPayload() == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "recordbookPayload");
        }
        if (reqVO.getProcessPoolContext() == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "processPoolContext");
        }
        if (reqVO.getRawPayload() == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "rawPayload");
        }
        if (reqVO.getActualEmployeeId() == null || StrUtil.isBlank(reqVO.getSignaturePassword())) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "signature");
        }
        if (reqVO.getProcessPoolContext().getDeviceAccountUserId() == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "deviceAccountUserId");
        }
    }

    private MesFrontlineSubmitIdentityCommand buildSubmitIdentityCommand(MesProFrontlineFeedbackSubmitReqVO reqVO,
                                                                         Long loginUserId) {
        MesProFrontlineProcessPoolContextReqVO context = reqVO.getProcessPoolContext();
        return new MesFrontlineSubmitIdentityCommand(
                loginUserId,
                reqVO.getActualEmployeeId(),
                reqVO.getSignatureEmployeeId(),
                context.getDeviceId(),
                context.getWorkstationId(),
                context.getRouteId(),
                context.getRouteProcessId(),
                context.getProcessId(),
                context.getTemplateType());
    }

}
