package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineProcessPoolContextReqVO;
import cn.iocoder.yudao.module.mes.enums.md.autocode.MesMdAutoCodeRuleCodeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackTypeEnum;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitAuthorizationService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitIdentityCommand;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitIdentityTrace;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventCreateReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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
    private final MesFrontlineDeviceParameterValidator deviceParameterValidator;
    private final MesProFrontlineFeedbackPayloadSplitter payloadSplitter;
    private final MesMdAutoCodeRecordService autoCodeRecordService;
    private final MesProBatchRecordExecutionSignatureService signatureService;

    public MesProFrontlineFeedbackSubmitServiceImpl(MesProFeedbackService feedbackService,
                                                    MesProFrontlineRecordbookEntryService recordbookEntryService,
                                                    MesProcessPoolSubmitEventService processPoolSubmitEventService,
                                                    MesFrontlineSubmitAuthorizationService submitAuthorizationService,
                                                    MesFrontlineLossReasonValidator lossReasonValidator,
                                                    MesFrontlineDeviceParameterValidator deviceParameterValidator,
                                                    MesProFrontlineFeedbackPayloadSplitter payloadSplitter,
                                                    MesMdAutoCodeRecordService autoCodeRecordService,
                                                    MesProBatchRecordExecutionSignatureService signatureService) {
        this.feedbackService = feedbackService;
        this.recordbookEntryService = recordbookEntryService;
        this.processPoolSubmitEventService = processPoolSubmitEventService;
        this.submitAuthorizationService = submitAuthorizationService;
        this.lossReasonValidator = lossReasonValidator;
        this.deviceParameterValidator = deviceParameterValidator;
        this.payloadSplitter = payloadSplitter;
        this.autoCodeRecordService = autoCodeRecordService;
        this.signatureService = signatureService;
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
        MesFrontlineSubmitIdentityTrace identityTrace = submitAuthorizationService.authorize(
                buildSubmitIdentityCommand(reqVO, loginUserId));
        validateSelectedActiveOrderContext(reqVO);
        List<MesFrontlineLossReasonSnapshot> lossReasonSnapshots = lossReasonValidator.requireSnapshotLossReasons(
                identityTrace.sessionSnapshot().content().defectReasons(),
                reqVO.getFeedbackPayload().getLossDetails(),
                reqVO.getFeedbackPayload().getLossQuantity());
        deviceParameterValidator.validateSnapshotDeviceAndParameters(
                identityTrace.sessionSnapshot().content().devices(),
                reqVO.getFeedbackPayload().getSelectedDevice(),
                reqVO.getFeedbackPayload().getDeviceParameterReadings());
        MesFrontlineLossReasonSnapshot lossReasonSnapshot = lossReasonSnapshots == null || lossReasonSnapshots.isEmpty()
                ? null : lossReasonSnapshots.get(0);

        LocalDateTime submittedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        MesProFrontlineFeedbackSplitPayload splitPayload = payloadSplitter.split(reqVO, loginUserId,
                submittedAt, lossReasonSnapshot);
        Optional<cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventResult> existing =
                processPoolSubmitEventService.findExistingSubmitEvent(splitPayload.getProcessPoolEventPayload());
        if (existing.isPresent()) {
            return toSubmitResp(existing.get());
        }

        authorizeSelectedActiveOrder(reqVO, loginUserId);
        applyServerResolvedFeedbackIdentity(reqVO);
        Long signatureId = signatureService.recordProductionSubmitSignature(reqVO.getSignatureEmployeeId(),
                reqVO.getSignaturePassword(), "一线生产报工提交");
        reqVO.setSignatureId(signatureId);
        splitPayload = payloadSplitter.split(reqVO, loginUserId, submittedAt, lossReasonSnapshot);

        Long feedbackId = feedbackService.createFrontlineFeedback(splitPayload.getFeedbackPayload());
        feedbackService.submitFeedback(feedbackId);

        MesProFrontlineRecordbookEntryResult recordbookResult = null;
        if (splitPayload.getRecordbookEntryPayload() != null) {
            MesProFrontlineRecordbookEntryPayload recordbookEntryPayload = splitPayload.getRecordbookEntryPayload()
                    .setFeedbackId(feedbackId);
            recordbookResult = recordbookEntryService.createOriginalEntry(recordbookEntryPayload);
        }

        MesProcessPoolSubmitEventCreateReqBO eventPayload = splitPayload.getProcessPoolEventPayload()
                .setFeedbackId(feedbackId)
                .setRecordbookEntryId(recordbookResult == null ? null : recordbookResult.getRecordbookEntryId())
                .setRecordbookEventId(recordbookResult == null ? null : recordbookResult.getRecordbookEventId());
        Long processPoolEventId = processPoolSubmitEventService.createSubmitEvent(eventPayload);

        return new MesProFrontlineFeedbackSubmitRespVO()
                .setFeedbackId(feedbackId)
                .setRecordbookEntryId(recordbookResult == null ? null : recordbookResult.getRecordbookEntryId())
                .setRecordbookEventId(recordbookResult == null ? null : recordbookResult.getRecordbookEventId())
                .setProcessPoolEventId(processPoolEventId);
    }

    private void validateSelectedActiveOrderContext(MesProFrontlineFeedbackSubmitReqVO reqVO) {
        MesProFrontlineFeedbackPayloadReqVO feedback = reqVO.getFeedbackPayload();
        MesProFrontlineProcessPoolContextReqVO context = reqVO.getProcessPoolContext();
        if (feedback.getWorkOrderId() == null || context.getWorkOrderId() == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "selectedActiveOrder");
        }
        if (!Objects.equals(feedback.getWorkOrderId(), context.getWorkOrderId())
                || !Objects.equals(feedback.getRouteId(), context.getRouteId())
                || !Objects.equals(feedback.getProcessId(), context.getProcessId())) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "selectedActiveOrderContext");
        }
    }

    private void authorizeSelectedActiveOrder(MesProFrontlineFeedbackSubmitReqVO reqVO, Long loginUserId) {
        MesProFrontlineProcessPoolContextReqVO context = reqVO.getProcessPoolContext();
        submitAuthorizationService.authorizeActiveOrder(loginUserId, context.getWorkOrderId(),
                context.getRouteId(), context.getRouteProcessId(), context.getProcessId());
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
        validateLossDetailTotal(reqVO);
        if (reqVO.getProcessPoolContext() == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "processPoolContext");
        }
        if (StrUtil.isBlank(reqVO.getProcessPoolSubmissionIdempotencyKey())) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "processPoolSubmissionIdempotencyKey");
        }
        if (StrUtil.isBlank(reqVO.getFrontlineSessionSnapshotId())) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "frontlineSessionSnapshotId");
        }
        if (StrUtil.isBlank(reqVO.getFrontlineSessionSnapshotHash())) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "frontlineSessionSnapshotHash");
        }
        if (reqVO.getRawPayload() == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "rawPayload");
        }
        if (reqVO.getSignatureId() != null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "signatureId must be server generated");
        }
        if (reqVO.getActualEmployeeId() == null || reqVO.getSignatureEmployeeId() == null
                || StrUtil.isBlank(reqVO.getSignaturePassword())) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "signature");
        }
        if (reqVO.getProcessPoolContext().getDeviceAccountUserId() == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "deviceAccountUserId");
        }
        if (!Objects.equals(reqVO.getActualEmployeeId(), reqVO.getSignatureEmployeeId())) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SIGNATURE_EMPLOYEE_MISMATCH);
        }
    }

    private void applyServerResolvedFeedbackIdentity(MesProFrontlineFeedbackSubmitReqVO reqVO) {
        MesProFrontlineFeedbackPayloadReqVO feedbackPayload = reqVO.getFeedbackPayload();
        if (StrUtil.isBlank(feedbackPayload.getCode())) {
            feedbackPayload.setCode(autoCodeRecordService.generateAutoCode(
                    MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode()));
        }
        if (feedbackPayload.getType() == null) {
            feedbackPayload.setType(MesProFeedbackTypeEnum.SELF.getType());
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

    private void validateLossDetailTotal(MesProFrontlineFeedbackSubmitReqVO reqVO) {
        MesProFrontlineFeedbackPayloadReqVO feedbackPayload = reqVO.getFeedbackPayload();
        List<MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO> lossDetails = resolveLossDetails(reqVO);
        feedbackPayload.setLossDetails(lossDetails);
        BigDecimal detailTotal = lossDetails.stream()
                .map(detail -> detail.getQuantity() == null ? BigDecimal.ZERO : detail.getQuantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (feedbackPayload.getLossQuantity().compareTo(detailTotal) != 0) {
            throw exception(PRO_FRONTLINE_FEEDBACK_QUANTITY_INVALID, "损耗数量必须等于各损耗原因数量之和");
        }
    }

    private List<MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO> resolveLossDetails(
            MesProFrontlineFeedbackSubmitReqVO reqVO) {
        List<MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO> lossDetails =
                reqVO.getFeedbackPayload().getLossDetails();
        if (lossDetails != null) {
            return lossDetails;
        }
        Object rawLossDetails = reqVO.getRawPayload() == null ? null : reqVO.getRawPayload().get("lossDetails");
        if (rawLossDetails == null && reqVO.getRawPayload() != null) {
            rawLossDetails = reqVO.getRawPayload().get("lossReasonDetails");
        }
        if (!(rawLossDetails instanceof List<?> rawItems)) {
            return List.of();
        }
        return rawItems.stream()
                .filter(Map.class::isInstance)
                .map(item -> toLossDetail((Map<?, ?>) item))
                .toList();
    }

    private MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO toLossDetail(Map<?, ?> item) {
        return new MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO()
                .setReasonId(toLong(item.get("reasonId")))
                .setReasonCode(toText(item.get("reasonCode")))
                .setReasonName(toText(item.get("reasonName")))
                .setQuantity(toBigDecimal(item.get("quantity")));
    }

    private static Long toLong(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value).trim());
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value).trim());
    }

    private static String toText(Object value) {
        return value == null ? null : String.valueOf(value);
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
                context.getTemplateType(),
                reqVO.getFrontlineSessionSnapshotId(),
                reqVO.getFrontlineSessionSnapshotHash());
    }

}
