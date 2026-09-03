package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackMaterialReqVO;
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
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterial;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineTeamDeviceOption;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitIdentityCommand;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineSubmitIdentityTrace;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventCreateReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolSubmitEventService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDeviceParameterSnapshotCodec;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_DEVICE_ACCOUNT_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_LOGIN_USER_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_MATERIAL_INVALID;
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
        validateDeviceSelections(reqVO.getFeedbackPayload(),
                identityTrace.sessionSnapshot().content().devices());
        for (var material : reqVO.getMaterialDetails()) {
            if (material == null) {
                throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "materialDetails[]");
            }
            validateDeviceSelections(material.getSelectedDevice(), material.getSelectedDevices(),
                    material.getDeviceParameterReadings(), identityTrace.sessionSnapshot().content().devices());
        }
        List<MesFrontlineProcessMaterial> frozenMaterials = identityTrace.sessionSnapshot().content().materials();
        List<MesFrontlineProcessMaterial> inputMaterials = frozenMaterials.stream()
                .filter(material -> MesFrontlineProcessMaterial.ROLE_INPUT.equals(material.materialRole())).toList();
        List<MesFrontlineProcessMaterial> outputMaterials = frozenMaterials.stream()
                .filter(material -> MesFrontlineProcessMaterial.ROLE_OUTPUT.equals(material.materialRole())).toList();
        requireExactOutputMaterialDetails(outputMaterials, reqVO.getMaterialDetails());
        attachInputMaterialEvidence(reqVO, inputMaterials);
        MesProFrontlineFeedbackMaterialSubmission materialSubmission = outputMaterials.isEmpty() ? null
                : materialSubmissionValidator.validate(outputMaterials,
                        identityTrace.sessionSnapshot().content().defectReasons(), reqVO.getMaterialDetails());
        MesFrontlineLossReasonSnapshot lossReasonSnapshot;
        if (materialSubmission == null) {
            lossReasonSnapshot = validateEmptyMaterialSubmission(reqVO, identityTrace);
        } else {
            applyMaterialAggregate(reqVO, materialSubmission);
            lossReasonSnapshot = firstLossReason(materialSubmission);
        }

        LocalDateTime submittedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        MesProFrontlineFeedbackSplitPayload splitPayload = payloadSplitter.split(reqVO, loginUserId,
                submittedAt, lossReasonSnapshot);
        if (materialSubmission != null) {
            applyFormalFeedbackAggregate(splitPayload, materialSubmission);
        }
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
        if (materialSubmission != null) {
            applyFormalFeedbackAggregate(splitPayload, materialSubmission);
        }

        Long feedbackId = feedbackService.createFrontlineFeedback(splitPayload.getFeedbackPayload());
        feedbackService.submitFeedback(feedbackId);
        if (materialSubmission != null) {
            feedbackMaterialService.createMaterials(buildMaterialCreateCommand(
                    feedbackId, activeOrderSnapshot, reqVO, materialSubmission));
        }

        // The submit stage stores the recordbook payload only as a source snapshot.
        // Flow 4 owns creation of the formal batch record at the explicit completion command.
        MesProcessPoolSubmitEventCreateReqBO eventPayload = splitPayload.getProcessPoolEventPayload()
                .setFeedbackId(feedbackId)
                .setRecordbookEntryId(null)
                .setRecordbookEventId(null);
        Long processPoolEventId = processPoolSubmitEventService.createSubmitEvent(eventPayload);
        MesProFrontlineProcessPoolContextReqVO context = reqVO.getProcessPoolContext();
        BigDecimal allocationQuantity = materialSubmission == null
                ? reqVO.getFeedbackPayload().getOutputQuantity() : materialSubmission.progressQuantity();
        if (allocationQuantity.compareTo(BigDecimal.ZERO) > 0) {
            processPoolSubmitEventService.createInitialAllocation(processPoolEventId,
                    context.getActiveOrderId(), allocationQuantity);
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
        if (reqVO.getMaterialDetails() == null) {
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

    private void attachInputMaterialEvidence(MesProFrontlineFeedbackSubmitReqVO reqVO,
                                             List<MesFrontlineProcessMaterial> inputMaterials) {
        List<Map<String, Object>> evidence = inputMaterials.stream().map(material -> {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("materialId", material.materialId());
            item.put("materialCode", material.materialCode());
            item.put("materialName", material.materialName());
            item.put("batchCodes", material.batchCodes());
            item.put("requestedQuantity", material.requestedQuantity());
            item.put("actualQuantity", material.actualQuantity());
            item.put("baseActualQuantity", material.baseActualQuantity());
            item.put("sourcePickListIds", material.sourcePickListIds());
            item.put("sourcePickListItemIds", material.sourcePickListItemIds());
            item.put("sourceSnapshotHash", material.sourceSnapshotHash());
            return Map.copyOf(item);
        }).toList();
        Map<String, Object> rawPayload = new java.util.LinkedHashMap<>(reqVO.getRawPayload());
        rawPayload.put("inputMaterialDetails", evidence);
        reqVO.setRawPayload(rawPayload);
    }

    private void requireExactOutputMaterialDetails(List<MesFrontlineProcessMaterial> outputMaterials,
                                                   List<MesProFrontlineFeedbackMaterialReqVO> submitted) {
        java.util.Set<Long> expectedIds = outputMaterials.stream()
                .map(MesFrontlineProcessMaterial::materialId)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        java.util.Set<Long> submittedIds = submitted.stream()
                .map(MesProFrontlineFeedbackMaterialReqVO::getMaterialId)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        if (submittedIds.size() != submitted.size() || !expectedIds.equals(submittedIds)) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED,
                    "materialDetails must exactly match frozen output materials");
        }
    }

    private static void validateDeviceSelections(MesProFrontlineFeedbackPayloadReqVO payload,
                                                 List<MesFrontlineTeamDeviceOption> allowedDevices) {
        validateDeviceSelections(payload.getSelectedDevice(), payload.getSelectedDevices(),
                payload.getDeviceParameterReadings(), allowedDevices);
    }

    static void validateDeviceSelections(
            MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO legacySelectedDevice,
            List<MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO> selectedDevices,
            List<MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO> readings,
            List<MesFrontlineTeamDeviceOption> allowedDevices) {
        if (legacySelectedDevice != null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "selectedDevice is no longer supported");
        }
        List<MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO> selected =
                selectedDevices == null ? List.of() : selectedDevices;
        List<MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO> submittedReadings =
                readings == null ? List.of() : readings;
        Map<Long, MesFrontlineTeamDeviceOption> allowedById = (allowedDevices == null
                ? List.<MesFrontlineTeamDeviceOption>of() : allowedDevices).stream()
                .collect(java.util.stream.Collectors.toMap(MesFrontlineTeamDeviceOption::deviceId,
                        device -> device, (left, ignored) -> left));
        Set<Long> selectedIds = new HashSet<>();
        Map<String, Integer> selectedCountsByGroup = new HashMap<>();
        for (var device : selected) {
            Long deviceId = device == null ? null : device.getDeviceId();
            MesFrontlineTeamDeviceOption allowed = deviceId == null ? null : allowedById.get(deviceId);
            if (allowed == null || !selectedIds.add(deviceId)) {
                throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "selectedDevices=" + deviceId);
            }
            String groupKey = allowed.deviceGroupKey();
            if (StrUtil.isBlank(groupKey)
                    || !List.of("SINGLE", "MULTIPLE").contains(allowed.selectionMode())) {
                throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED,
                        "device selection snapshot=" + deviceId);
            }
            int selectedCount = selectedCountsByGroup.merge(groupKey, 1, Integer::sum);
            if ("SINGLE".equals(allowed.selectionMode()) && selectedCount > 1) {
                throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED,
                        "SINGLE deviceGroupKey=" + groupKey);
            }
        }
        Map<Long, Set<String>> submittedCodesByDevice = new HashMap<>();
        for (var reading : submittedReadings) {
            if (reading == null || reading.getDeviceId() == null || !selectedIds.contains(reading.getDeviceId())) {
                throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED, "unselected device parameter");
            }
            submittedCodesByDevice.computeIfAbsent(reading.getDeviceId(), ignored -> new HashSet<>())
                    .add(MesDeviceParameterSnapshotCodec.normalizeCode(reading.getParameterCode()));
        }
        for (Long selectedId : selectedIds) {
            MesFrontlineTeamDeviceOption device = allowedById.get(selectedId);
            Set<String> submittedCodes = submittedCodesByDevice.getOrDefault(selectedId, Set.of());
            for (var parameter : device.parameters()) {
                if (!MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_TEXT_STANDARD.equals(parameter.valueType())
                        && !submittedCodes.contains(MesDeviceParameterSnapshotCodec.normalizeCode(
                        parameter.parameterCode()))) {
                    throw exception(PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED,
                            "missing device parameter=" + parameter.parameterCode());
                }
            }
        }
    }

    private MesFrontlineLossReasonSnapshot validateEmptyMaterialSubmission(
            MesProFrontlineFeedbackSubmitReqVO reqVO,
            MesFrontlineSubmitIdentityTrace identityTrace) {
        if (!reqVO.getMaterialDetails().isEmpty()) {
            throw exception(PRO_FRONTLINE_FEEDBACK_MATERIAL_INVALID,
                    "提交物料集合与冻结工序不一致：当前工序未配置批记录物料");
        }
        return materialSubmissionValidator.validateProcessPayload(
                reqVO.getFeedbackPayload(), identityTrace.sessionSnapshot().content().defectReasons());
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
                        material.selectedDevices().isEmpty() ? null : JsonUtils.toJsonString(material.selectedDevices()),
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
