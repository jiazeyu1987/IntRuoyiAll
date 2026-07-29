package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_DEVICE_ACCOUNT_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_LOGIN_USER_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_SIGNATURE_EMPLOYEE_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED;

@Service
@Validated
public class MesProFrontlineFeedbackSubmitServiceImpl implements MesProFrontlineFeedbackSubmitService {

    private final MesProFeedbackService feedbackService;
    private final MesProFrontlineRecordbookEntryService recordbookEntryService;
    private final MesProcessPoolSubmitEventService processPoolSubmitEventService;
    private final MesFrontlineSubmitAuthorizationService submitAuthorizationService;
    private final MesProFrontlineFeedbackPayloadSplitter payloadSplitter;

    public MesProFrontlineFeedbackSubmitServiceImpl(MesProFeedbackService feedbackService,
                                                    MesProFrontlineRecordbookEntryService recordbookEntryService,
                                                    MesProcessPoolSubmitEventService processPoolSubmitEventService,
                                                    MesFrontlineSubmitAuthorizationService submitAuthorizationService,
                                                    MesProFrontlineFeedbackPayloadSplitter payloadSplitter) {
        this.feedbackService = feedbackService;
        this.recordbookEntryService = recordbookEntryService;
        this.processPoolSubmitEventService = processPoolSubmitEventService;
        this.submitAuthorizationService = submitAuthorizationService;
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

        LocalDateTime submittedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        MesProFrontlineFeedbackSplitPayload splitPayload = payloadSplitter.split(reqVO, loginUserId, submittedAt);

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
