package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineProcessPoolContextReqVO;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitAuthorizationService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitIdentityCommand;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventCreateReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_DEVICE_ACCOUNT_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_LOGIN_USER_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_QUANTITY_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_SIGNATURE_EMPLOYEE_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED;

@Service
@Validated
public class MesProFrontlineFeedbackSubmitServiceImpl implements MesProFrontlineFeedbackSubmitService {

    private final MesProFeedbackService feedbackService;
    private final MesProFrontlineRecordbookEntryService recordbookEntryService;
    private final MesProcessPoolSubmitEventService processPoolSubmitEventService;
    private final MesFrontlineSubmitAuthorizationService submitAuthorizationService;
    private final MesFrontlineLossReasonValidator lossReasonValidator;
    private final MesProFrontlineFeedbackPayloadSplitter payloadSplitter;

    public MesProFrontlineFeedbackSubmitServiceImpl(MesProFeedbackService feedbackService,
                                                    MesProFrontlineRecordbookEntryService recordbookEntryService,
                                                    MesProcessPoolSubmitEventService processPoolSubmitEventService,
                                                    MesFrontlineSubmitAuthorizationService submitAuthorizationService,
                                                    MesFrontlineLossReasonValidator lossReasonValidator,
                                                    MesProFrontlineFeedbackPayloadSplitter payloadSplitter) {
        this.feedbackService = feedbackService;
        this.recordbookEntryService = recordbookEntryService;
        this.processPoolSubmitEventService = processPoolSubmitEventService;
        this.submitAuthorizationService = submitAuthorizationService;
        this.lossReasonValidator = lossReasonValidator;
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
        submitAuthorizationService.authorize(buildSubmitIdentityCommand(reqVO, loginUserId));
        MesFrontlineLossReasonSnapshot lossReasonSnapshot = lossReasonValidator.requireEnabledLossReason(
                reqVO.getProcessPoolContext().getRouteProcessId(),
                reqVO.getFeedbackPayload().getLossReasonId(),
                reqVO.getFeedbackPayload().getLossQuantity());

        LocalDateTime submittedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        MesProFrontlineFeedbackSplitPayload splitPayload = payloadSplitter.split(reqVO, loginUserId,
                submittedAt, lossReasonSnapshot);
        Optional<cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventResult> existing =
                processPoolSubmitEventService.findExistingSubmitEvent(splitPayload.getProcessPoolEventPayload());
        if (existing.isPresent()) {
            return toSubmitResp(existing.get());
        }

        Long feedbackId = feedbackService.createFeedback(splitPayload.getFeedbackPayload());
        feedbackService.submitFeedback(feedbackId);

        MesProFrontlineRecordbookEntryPayload recordbookEntryPayload = splitPayload.getRecordbookEntryPayload()
                .setFeedbackId(feedbackId);
        MesProFrontlineRecordbookEntryResult recordbookResult =
                recordbookEntryService.createOriginalEntry(recordbookEntryPayload);

        MesProcessPoolSubmitEventCreateReqBO eventPayload = splitPayload.getProcessPoolEventPayload()
                .setFeedbackId(feedbackId)
                .setRecordbookEntryId(recordbookResult.getRecordbookEntryId())
                .setRecordbookEventId(recordbookResult.getRecordbookEventId());
        Long processPoolEventId = processPoolSubmitEventService.createSubmitEvent(eventPayload);

        return new MesProFrontlineFeedbackSubmitRespVO()
                .setFeedbackId(feedbackId)
                .setRecordbookEntryId(recordbookResult.getRecordbookEntryId())
                .setRecordbookEventId(recordbookResult.getRecordbookEventId())
                .setProcessPoolEventId(processPoolEventId);
    }

    private MesProFrontlineFeedbackSubmitRespVO toSubmitResp(
            cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventResult result) {
        return new MesProFrontlineFeedbackSubmitRespVO()
                .setFeedbackId(result.getFeedbackId())
                .setRecordbookEntryId(result.getRecordbookEntryId())
                .setRecordbookEventId(result.getRecordbookEventId())
                .setProcessPoolEventId(result.getProcessPoolEventId());
    }

    private void validateSubmitContext(MesProFrontlineFeedbackSubmitReqVO reqVO) {
        if (reqVO == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "request");
        }
        if (reqVO.getFeedbackPayload() == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "feedbackPayload");
        }
        validateProductionQuantity(reqVO.getFeedbackPayload());
        if (reqVO.getRecordbookPayload() == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "recordbookPayload");
        }
        if (reqVO.getProcessPoolContext() == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "processPoolContext");
        }
        if (cn.hutool.core.util.StrUtil.isBlank(reqVO.getProcessPoolSubmissionIdempotencyKey())) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "processPoolSubmissionIdempotencyKey");
        }
        if (reqVO.getRawPayload() == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "rawPayload");
        }
        if (reqVO.getActualEmployeeId() == null || reqVO.getSignatureEmployeeId() == null
                || reqVO.getSignatureId() == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "signature");
        }
        if (reqVO.getProcessPoolContext().getDeviceAccountUserId() == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "deviceAccountUserId");
        }
        if (!Objects.equals(reqVO.getActualEmployeeId(), reqVO.getSignatureEmployeeId())) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SIGNATURE_EMPLOYEE_MISMATCH);
        }
    }

    private void validateProductionQuantity(MesProFrontlineFeedbackPayloadReqVO feedbackPayload) {
        BigDecimal outputQuantity = feedbackPayload.getOutputQuantity();
        if (outputQuantity == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "outputQuantity");
        }
        BigDecimal lossQuantity = feedbackPayload.getLossQuantity();
        if (lossQuantity == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "lossQuantity");
        }
        if (outputQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_FRONTLINE_FEEDBACK_QUANTITY_INVALID, "输出数量必须大于 0");
        }
        if (lossQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw exception(PRO_FRONTLINE_FEEDBACK_QUANTITY_INVALID, "损耗数量不能小于 0");
        }
        if (lossQuantity.compareTo(outputQuantity) > 0) {
            throw exception(PRO_FRONTLINE_FEEDBACK_QUANTITY_INVALID, "损耗数量不能大于输出数量");
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
