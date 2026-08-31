package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
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
import cn.iocoder.yudao.module.mes.service.pro.frontline.ActiveOrderSnapshotResolver;
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
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_SIGNATURE_EMPLOYEE_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED;

@Service
@Validated
public class MesProFrontlineFeedbackSubmitServiceImpl implements MesProFrontlineFeedbackSubmitService {

    private final MesProFeedbackService feedbackService;
    private final MesProFeedbackMaterialService feedbackMaterialService;
    private final MesProcessPoolSubmitEventService processPoolSubmitEventService;
    private final MesFrontlineSubmitAuthorizationService submitAuthorizationService;
    private final MesFrontlineParameterAuditService parameterAuditService;
    private final MesProFrontlineFeedbackMaterialSubmissionValidator materialSubmissionValidator;
    private final MesProFrontlineFeedbackPayloadSplitter payloadSplitter;
    private final MesMdAutoCodeRecordService autoCodeRecordService;
    private final MesProBatchRecordExecutionSignatureService signatureService;
    private final ActiveOrderSnapshotResolver activeOrderSnapshotResolver;

    public MesProFrontlineFeedbackSubmitServiceImpl(MesProFeedbackService feedbackService,
                                                    MesProFeedbackMaterialService feedbackMaterialService,
                                                    MesProcessPoolSubmitEventService processPoolSubmitEventService,
                                                    MesFrontlineSubmitAuthorizationService submitAuthorizationService,
                                                    MesFrontlineParameterAuditService parameterAuditService,
                                                    MesProFrontlineFeedbackMaterialSubmissionValidator materialSubmissionValidator,
                                                    MesProFrontlineFeedbackPayloadSplitter payloadSplitter,
                                                    MesMdAutoCodeRecordService autoCodeRecordService,
                                                    MesProBatchRecordExecutionSignatureService signatureService,
                                                    ActiveOrderSnapshotResolver activeOrderSnapshotResolver) {
        this.feedbackService = feedbackService;
        this.feedbackMaterialService = feedbackMaterialService;
        this.processPoolSubmitEventService = processPoolSubmitEventService;
        this.submitAuthorizationService = submitAuthorizationService;
        this.parameterAuditService = parameterAuditService;
        this.materialSubmissionValidator = materialSubmissionValidator;
        this.payloadSplitter = payloadSplitter;
        this.autoCodeRecordService = autoCodeRecordService;
        this.signatureService = signatureService;
        this.activeOrderSnapshotResolver = activeOrderSnapshotResolver;
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
        MesProFrontlineFeedbackMaterialSubmission materialSubmission = materialSubmissionValidator.validate(
                identityTrace.sessionSnapshot().content().materials(),
                identityTrace.sessionSnapshot().content().defectReasons(), reqVO.getMaterialDetails());
        applyMaterialAggregate(reqVO, materialSubmission);
        MesFrontlineLossReasonSnapshot lossReasonSnapshot = firstLossReason(materialSubmission);

        LocalDateTime submittedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        MesProFrontlineFeedbackSplitPayload splitPayload = payloadSplitter.split(reqVO, loginUserId,
                submittedAt, lossReasonSnapshot);
        applyFormalFeedbackAggregate(splitPayload, materialSubmission);
        Optional<cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventResult> existing =
                processPoolSubmitEventService.findExistingSubmitEvent(splitPayload.getProcessPoolEventPayload());
        if (existing.isPresent()) {
            return toSubmitResp(existing.get());
        }

        authorizeSelectedActiveOrder(reqVO, loginUserId);
        ActiveOrderSnapshotResolver.ActiveOrderSnapshot activeOrderSnapshot =
                requireActiveOrderSnapshot(reqVO);
        MesFrontlineParameterAuditResult parameterAuditResult = parameterAuditService.resolveAndApply(reqVO);
        attachParameterAudit(reqVO, parameterAuditResult);
        applyServerResolvedFeedbackIdentity(reqVO);
        Long signatureId = signatureService.recordProductionSubmitSignature(reqVO.getSignatureEmployeeId(),
                reqVO.getSignaturePassword(), "一线生产报工提交");
        reqVO.setSignatureId(signatureId);
        splitPayload = payloadSplitter.split(reqVO, loginUserId, submittedAt, lossReasonSnapshot);
        applyFormalFeedbackAggregate(splitPayload, materialSubmission);

        Long feedbackId = feedbackService.createFrontlineFeedback(splitPayload.getFeedbackPayload());
        feedbackService.submitFeedback(feedbackId);
        feedbackMaterialService.createMaterials(buildMaterialCreateCommand(
                feedbackId, activeOrderSnapshot, reqVO, materialSubmission));

        // The submit stage stores the recordbook payload only as a source snapshot.
        // Flow 4 owns creation of the formal batch record at the explicit completion command.
        MesProcessPoolSubmitEventCreateReqBO eventPayload = splitPayload.getProcessPoolEventPayload()
                .setFeedbackId(feedbackId)
                .setRecordbookEntryId(null)
                .setRecordbookEventId(null);
        Long processPoolEventId = processPoolSubmitEventService.createSubmitEvent(eventPayload);
        MesProFrontlineProcessPoolContextReqVO context = reqVO.getProcessPoolContext();
        if (materialSubmission.progressQuantity().compareTo(BigDecimal.ZERO) > 0) {
            processPoolSubmitEventService.createInitialAllocation(processPoolEventId,
                    context.getActiveOrderId(), materialSubmission.progressQuantity());
        }

        MesProFrontlineFeedbackSubmitRespVO response = new MesProFrontlineFeedbackSubmitRespVO()
                .setFeedbackId(feedbackId)
                .setRecordbookEntryId(null)
                .setRecordbookEventId(null)
                .setProcessPoolEventId(processPoolEventId);
        return applyParameterAudit(response, parameterAuditResult);
    }

    private void validateSelectedActiveOrderContext(MesProFrontlineFeedbackSubmitReqVO reqVO) {
        MesProFrontlineFeedbackPayloadReqVO feedback = reqVO.getFeedbackPayload();
        MesProFrontlineProcessPoolContextReqVO context = reqVO.getProcessPoolContext();
        if (context.getActiveOrderId() == null || feedback.getWorkOrderId() == null
                || context.getWorkOrderId() == null) {
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
        submitAuthorizationService.authorizeActiveOrder(loginUserId, context.getActiveOrderId(), context.getWorkOrderId(),
                context.getRouteId(), context.getRouteProcessId(), context.getProcessId());
    }

    private MesProFrontlineFeedbackSubmitRespVO toSubmitResp(
            cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventResult result) {
        if (result.getParameterAuditResult() == null) {
            throw new IllegalStateException("Existing production submit event is missing parameter audit payload");
        }
        MesProFrontlineFeedbackSubmitRespVO response = new MesProFrontlineFeedbackSubmitRespVO()
                .setFeedbackId(result.getFeedbackId())
                .setRecordbookEntryId(result.getRecordbookEntryId())
                .setRecordbookEventId(result.getRecordbookEventId())
                .setProcessPoolEventId(result.getProcessPoolEventId());
        return applyParameterAudit(response, result.getParameterAuditResult());
    }

    private void attachParameterAudit(MesProFrontlineFeedbackSubmitReqVO reqVO,
                                      MesFrontlineParameterAuditResult parameterAuditResult) {
        Map<String, Object> rawPayload = new java.util.LinkedHashMap<>(reqVO.getRawPayload());
        rawPayload.put("parameterAudit", parameterAuditResult);
        reqVO.setRawPayload(rawPayload);
    }

    private MesProFrontlineFeedbackSubmitRespVO applyParameterAudit(
            MesProFrontlineFeedbackSubmitRespVO response, MesFrontlineParameterAuditResult auditResult) {
        return response
                .setParameterAuditStatus(auditResult.getParameterAuditStatus())
                .setParameterAuditTotalCount(auditResult.getTotalCount())
                .setParameterAuditResolvedCount(auditResult.getResolvedCount())
                .setParameterAuditUnresolvedCount(auditResult.getUnresolvedCount())
                .setAuditItems(auditResult.getAuditItems().stream()
                        .map(this::toAuditItemResp)
                        .toList());
    }

    private MesProFrontlineFeedbackSubmitRespVO.ParameterAuditItemRespVO toAuditItemResp(
            MesFrontlineParameterAuditItem item) {
        return new MesProFrontlineFeedbackSubmitRespVO.ParameterAuditItemRespVO()
                .setReadingIndex(item.getReadingIndex())
                .setDeviceId(item.getDeviceId())
                .setParameterCode(item.getParameterCode())
                .setParameterName(item.getParameterName())
                .setUnit(item.getUnit())
                .setValue(item.getValue())
                .setTextValue(item.getTextValue())
                .setLowerLimit(item.getLowerLimit())
                .setUpperLimit(item.getUpperLimit())
                .setParameterStatus(item.getParameterStatus())
                .setResolutionStatus(item.getResolutionStatus())
                .setReasonCode(item.getReasonCode())
                .setSnapshotSource(item.getSnapshotSource());
    }

    private void validateSubmitContext(MesProFrontlineFeedbackSubmitReqVO reqVO) {
        if (reqVO == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "request");
        }
        if (reqVO.getFeedbackPayload() == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "feedbackPayload");
        }
        if (reqVO.getMaterialDetails() == null || reqVO.getMaterialDetails().isEmpty()) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "materialDetails");
        }
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

    private void applyMaterialAggregate(MesProFrontlineFeedbackSubmitReqVO reqVO,
                                        MesProFrontlineFeedbackMaterialSubmission submission) {
        MesProFrontlineFeedbackPayloadReqVO feedback = reqVO.getFeedbackPayload();
        List<MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO> lossDetails = submission.materials().stream()
                .flatMap(material -> material.lossDetails().stream())
                .toList();
        feedback.setOutputQuantity(submission.progressQuantity())
                .setLossQuantity(submission.totalLossQuantity())
                .setLossDetails(lossDetails)
                .setLaborScrapQuantity(submission.totalLossQuantity())
                .setMaterialScrapQuantity(BigDecimal.ZERO)
                .setOtherScrapQuantity(BigDecimal.ZERO);
        MesFrontlineLossReasonSnapshot firstLoss = firstLossReason(submission);
        feedback.setLossReasonId(firstLoss == null ? null : firstLoss.reasonId());
        Map<String, Object> rawPayload = new java.util.LinkedHashMap<>(reqVO.getRawPayload());
        rawPayload.put("materialDetails", reqVO.getMaterialDetails());
        rawPayload.put("progressQuantity", submission.progressQuantity());
        reqVO.setRawPayload(rawPayload);
    }

    private void applyFormalFeedbackAggregate(
            MesProFrontlineFeedbackSplitPayload splitPayload,
            MesProFrontlineFeedbackMaterialSubmission submission) {
        BigDecimal progressQuantity = submission.progressQuantity();
        BigDecimal lossQuantity = submission.totalLossQuantity();
        splitPayload.getFeedbackPayload()
                .setFeedbackQuantity(progressQuantity.add(lossQuantity))
                .setQualifiedQuantity(progressQuantity)
                .setUnqualifiedQuantity(lossQuantity);
    }

    private MesFrontlineLossReasonSnapshot firstLossReason(
            MesProFrontlineFeedbackMaterialSubmission submission) {
        return submission.materials().stream()
                .flatMap(material -> material.lossDetails().stream())
                .findFirst()
                .map(detail -> new MesFrontlineLossReasonSnapshot(
                        detail.getReasonId(), detail.getReasonCode(), detail.getReasonName()))
                .orElse(null);
    }

    private ActiveOrderSnapshotResolver.ActiveOrderSnapshot requireActiveOrderSnapshot(
            MesProFrontlineFeedbackSubmitReqVO reqVO) {
        MesProFrontlineProcessPoolContextReqVO context = reqVO.getProcessPoolContext();
        ActiveOrderSnapshotResolver.ActiveOrderSnapshot snapshot =
                activeOrderSnapshotResolver.requireEffective(context.getActiveOrderId());
        if (!Objects.equals(snapshot.workOrderId(), context.getWorkOrderId())
                || !Objects.equals(snapshot.routeId(), context.getRouteId())) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "activeOrderSnapshot");
        }
        return snapshot;
    }

    private MesProFeedbackMaterialCreateCommand buildMaterialCreateCommand(
            Long feedbackId,
            ActiveOrderSnapshotResolver.ActiveOrderSnapshot activeOrder,
            MesProFrontlineFeedbackSubmitReqVO reqVO,
            MesProFrontlineFeedbackMaterialSubmission submission) {
        MesProFrontlineProcessPoolContextReqVO context = reqVO.getProcessPoolContext();
        List<MesProFeedbackMaterialCreateCommand.Entry> entries = submission.materials().stream()
                .map(material -> new MesProFeedbackMaterialCreateCommand.Entry(
                        material.materialId(), material.materialCode(), material.materialName(),
                        material.materialSpecification(), material.bomQuantity(), material.outputQuantity(),
                        material.lossQuantity(), JsonUtils.toJsonString(material.lossDetails()),
                        material.selectedDevice() == null ? null : JsonUtils.toJsonString(material.selectedDevice()),
                        JsonUtils.toJsonString(material.deviceParameterReadings())))
                .toList();
        return new MesProFeedbackMaterialCreateCommand(feedbackId, context.getActiveOrderId(),
                context.getWorkOrderId(), context.getRouteId(), activeOrder.routeVersionId(),
                context.getRouteProcessId(), context.getProcessId(), entries);
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
