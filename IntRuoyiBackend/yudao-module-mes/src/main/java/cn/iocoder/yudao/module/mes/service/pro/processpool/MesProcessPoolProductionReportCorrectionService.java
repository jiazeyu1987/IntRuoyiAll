package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDiffDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMaterialMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionDiffMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolQuantityFragmentMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSignatureCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSignatureResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesFrontlineLossReasonSnapshot;
import cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesFrontlineLossReasonValidator;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderScopeService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesProductionReportManagementSummaryService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditEvidence;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditRelation;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_CHANGE_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_DIFF_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SIGNATURE_EMPLOYEE_MISMATCH;

@Service
public class MesProcessPoolProductionReportCorrectionService {

    private static final String FIELD_OUTPUT_QUANTITY = "OUTPUT_QUANTITY";
    private static final String FIELD_SCRAP_QUANTITY = "SCRAP_QUANTITY";
    private static final String FEEDBACK_SOURCE_TYPE = "MES_PRO_FEEDBACK";

    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProProcessPoolQuantityFragmentMapper fragmentMapper;
    private final MesProFeedbackMapper feedbackMapper;
    private final MesProFeedbackMaterialMapper feedbackMaterialMapper;
    private final MesProcessPoolEventRevisionService revisionService;
    private final MesProBatchRecordExecutionSignatureService signatureService;
    private final MesFrontlineLossReasonValidator lossReasonValidator;
    private final MesTeamLeaderScopeService scopeService;
    private final MesProductionReportManagementSummaryService reportManagementSummaryService;
    @Resource
    private GxpAuditService gxpAuditService;
    @Resource
    private ElectronicSignatureQueryService electronicSignatureQueryService;
    @Resource
    private MesProProcessPoolEventRevisionMapper revisionMapper;
    @Resource
    private MesProProcessPoolEventRevisionDiffMapper revisionDiffMapper;
    @Resource
    private MesFrontlineReturnCorrectionService ownReturnService;

    public MesProcessPoolProductionReportCorrectionService(
            MesProProcessPoolEventMapper eventMapper,
            MesProProcessPoolQuantityFragmentMapper fragmentMapper,
            MesProFeedbackMapper feedbackMapper,
            MesProFeedbackMaterialMapper feedbackMaterialMapper,
            MesProcessPoolEventRevisionService revisionService,
            MesProBatchRecordExecutionSignatureService signatureService,
            MesFrontlineLossReasonValidator lossReasonValidator,
            MesTeamLeaderScopeService scopeService,
            MesProductionReportManagementSummaryService reportManagementSummaryService) {
        this.eventMapper = eventMapper;
        this.fragmentMapper = fragmentMapper;
        this.feedbackMapper = feedbackMapper;
        this.feedbackMaterialMapper = feedbackMaterialMapper;
        this.revisionService = revisionService;
        this.signatureService = signatureService;
        this.lossReasonValidator = lossReasonValidator;
        this.scopeService = scopeService;
        this.reportManagementSummaryService = reportManagementSummaryService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long correct(MesProcessPoolProductionReportCorrectionCommand command) {
        return correctInternal(command, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public MesFrontlineReturnCorrectionService.CorrectionResult correctOwnReturned(MesProcessPoolProductionReportCorrectionCommand command,
                                   Long activeOrderId, Long rejectedReviewId, Long expectedRevisionId) {
        validateCommand(command);
        gxpAuditService.acquireLedgerLock();
        var context = ownReturnService.requireOwnReturned(command.getEventId(), activeOrderId,
                rejectedReviewId, expectedRevisionId, command.getActorUserId(), "PRODUCTION");
        Long revisionId = correctInternal(command, context);
        return ownReturnService.complete(context, revisionId, command.getActorUserId());
    }

    private Long correctInternal(MesProcessPoolProductionReportCorrectionCommand command,
                                 MesFrontlineReturnCorrectionService.ReturnContext ownReturn) {
        validateCommand(command);
        gxpAuditService.acquireLedgerLock();
        MesProProcessPoolEventDO event = eventMapper.selectByIdForUpdate(command.getEventId());
        if (event == null) {
            throw exception(PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS, command.getEventId());
        }
        if (!MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(event.getEventType())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "productionSubmitEvent");
        }
        if (ownReturn == null) {
            scopeService.assertCanAccessEmployee(command.getActorUserId(), "PRODUCTION", event.getActualEmployeeId());
        } else if (!Objects.equals(ownReturn.event().getId(), event.getId())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "ownReturn.eventChanged");
        }

        ObjectNode afterPayload = requireObject(event.getRawPayload(), "rawPayload").deepCopy();
        if (ownReturn != null) {
            afterPayload.put("supersededReviewId", ownReturn.review().getId());
        }
        ObjectNode fieldValues = requireObject(afterPayload.get("fieldValues"), "rawPayload.fieldValues");
        BigDecimal beforeOutput = requireDecimal(afterPayload.get("outputQuantity"), "rawPayload.outputQuantity");
        BigDecimal beforeLoss = requireDecimal(afterPayload.get("lossQuantity"), "rawPayload.lossQuantity");
        MesProProcessPoolQuantityFragmentDO outputFragment = requireOutputFragment(event.getId());
        List<MesProcessPoolEventRevisionFieldChangeBO> changes = new ArrayList<>();

        applyOutputQuantity(command.getOutputQuantity(), beforeOutput, afterPayload, fieldValues,
                outputFragment, changes);
        applyLossDetails(command.getLossDetails(), beforeLoss, event.getRouteProcessId(),
                afterPayload, fieldValues, changes);
        applyMaterialDetails(command.getMaterialDetails(), afterPayload, changes);
        applyDeviceParameterReadings(command.getDeviceParameterReadings(), afterPayload, fieldValues, changes);

        if (changes.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_REVISION_DIFF_REQUIRED);
        }

        // Freeze current persisted facts before signature/revision and in-place summary mutation.
        CorrectionAuditState before = correctionAuditState(event, outputFragment, null);
        String afterPayloadJson = JsonUtils.toJsonString(afterPayload);
        String challengeHash = MesProBatchRecordExecutionFieldAuditHasher.sha256(
                event.getId() + "|" + MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(afterPayloadJson)
                        + "|" + command.getChangeReason().trim());
        MesProBatchRecordExecutionFieldAuditSignatureResult signature =
                signatureService.recordFieldChangeSignature(
                        new MesProBatchRecordExecutionFieldAuditSignatureCommand()
                                .setExecutionId(0L)
                                .setPassword(command.getSignaturePassword())
                                .setReasonCategory("PRODUCTION_REPORT_CORRECTION")
                                .setReasonText(command.getChangeReason().trim())
                                .setSignatureChallengeHash(challengeHash));
        if (signature == null || signature.getSignatureId() == null || signature.getSignatureId() <= 0
                || signature.getSignedAt() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "correctionSignature");
        }
        if (!Objects.equals(signature.getActorId(), command.getActorUserId())) {
            throw exception(PRO_PROCESS_POOL_SIGNATURE_EMPLOYEE_MISMATCH);
        }
        String signatureContentHash = verifyCorrectionSignature(command, event, challengeHash, signature);

        Long revisionId = revisionService.updateProductionReportRecord(MesProcessPoolEventRevisionUpdateReqBO.builder()
                .eventId(event.getId())
                .afterPayload(afterPayloadJson)
                .changeReason(command.getChangeReason().trim())
                .revisionSignatureId(signature.getSignatureId())
                .revisionSignatureUserId(signature.getActorId())
                .revisionSignatureSnapshot(JsonUtils.toJsonString(signature))
                .modifiedByUserId(signature.getActorId())
                .changedFields(changes)
                .build());

        syncFormalFeedbackSource(event, afterPayload);
        event.setRawPayload(afterPayloadJson)
                .setReportOutputQuantity(command.getOutputQuantity());
        reportManagementSummaryService.refreshProductionEvent(event);

        if (beforeOutput.compareTo(command.getOutputQuantity()) != 0) {
            updateOutputFragment(outputFragment, command.getOutputQuantity());
        }
        if (revisionId == null || revisionId <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "productionCorrection.revisionId");
        }
        MesProProcessPoolEventDO persisted = eventMapper.selectByIdForUpdate(event.getId());
        if (persisted == null || !requireObject(afterPayloadJson, "productionCorrection.persistedPayload")
                .equals(requireObject(persisted.getRawPayload(), "productionCorrection.persistedPayload"))) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "productionCorrection.persistedPayload");
        }
        CorrectionAuditState after = correctionAuditState(persisted, requireOutputFragment(event.getId()), revisionId);
        appendCorrectionAudit(command, persisted, revisionId, signature, signatureContentHash, challengeHash, before, after);
        return revisionId;
    }

    private String verifyCorrectionSignature(MesProcessPoolProductionReportCorrectionCommand command,
                                             MesProProcessPoolEventDO event, String challenge,
                                             MesProBatchRecordExecutionFieldAuditSignatureResult signature) {
        if (!Objects.equals(event.getTenantId(), TenantContextHolder.getRequiredTenantId())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "correctionSignature.tenant");
        }
        var evidence = electronicSignatureQueryService.getById(signature.getSignatureId());
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L,
                MesProBatchRecordExecutionSignatureService.ACTION_FIELD_CHANGE,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, challenge);
        if (evidence == null || !Objects.equals(signature.getSignatureId(), evidence.id())
                || !Objects.equals(command.getActorUserId(), evidence.actorId())
                || !"MES".equals(evidence.moduleCode()) || !"FIELD_CHANGE".equals(evidence.actionCode())
                || !"MES_BATCH_RECORD".equals(evidence.subjectType()) || !subject.equals(evidence.subjectId())
                || !MesProBatchRecordExecutionFieldAuditHasher.sha256(subject).equals(evidence.subjectVersion())
                || !command.getChangeReason().trim().equals(evidence.reason())
                || StrUtil.isBlank(evidence.contentHash()) || StrUtil.isBlank(evidence.evidenceHash())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "correctionSignature.binding");
        }
        var verified = electronicSignatureQueryService.verifyEvidence(signature.getSignatureId());
        var expected = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(new SignatureSubjectCommand(
                command.getActorUserId(), "MES", "FIELD_CHANGE", "MES_BATCH_RECORD", subject,
                evidence.subjectVersion(), command.getChangeReason().trim()));
        if (verified == null || !Objects.equals(signature.getSignatureId(), verified.signatureId())
                || !"VALID".equals(verified.verificationStatus())
                || !evidence.contentHash().equals(verified.storedContentHash())
                || !evidence.contentHash().equals(verified.calculatedContentHash())
                || !evidence.evidenceHash().equals(verified.storedEvidenceHash())
                || !evidence.evidenceHash().equals(verified.calculatedEvidenceHash())
                || !JsonUtils.parseTree(expected.canonicalContentJson()).equals(JsonUtils.parseTree(evidence.canonicalContentJson()))) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "correctionSignature.content");
        }
        return evidence.contentHash();
    }

    private CorrectionAuditState correctionAuditState(MesProProcessPoolEventDO event,
                                                      MesProProcessPoolQuantityFragmentDO fragment, Long revisionId) {
        if (event.getWorkOrderId() == null || !FEEDBACK_SOURCE_TYPE.equals(event.getFeedbackSourceType())
                || event.getFeedbackSourceId() == null || !Objects.equals(fragment.getEventId(), event.getId())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "productionCorrection.auditIdentity");
        }
        List<MesProFeedbackDO> feedbackRows = feedbackMapper.selectListByIdsForUpdate(List.of(event.getFeedbackSourceId()));
        if (feedbackRows == null || feedbackRows.size() != 1 || feedbackRows.get(0) == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "formalFeedback");
        }
        MesProFeedbackDO feedback = feedbackRows.get(0);
        if (!Objects.equals(feedback.getId(), event.getFeedbackSourceId())
                || !Objects.equals(feedback.getWorkOrderId(), event.getWorkOrderId())
                || !Objects.equals(feedback.getRouteId(), event.getRouteId())
                || !Objects.equals(feedback.getProcessId(), event.getProcessId())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "formalFeedback.identity");
        }
        List<MesProFeedbackMaterialDO> materials = feedbackMaterialMapper.selectListByFeedbackIdForUpdate(feedback.getId());
        if (materials == null || materials.stream().anyMatch(row -> row == null || row.getId() == null
                || !Objects.equals(row.getFeedbackId(), feedback.getId()))) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "productionCorrection.materialFacts");
        }
        ObjectNode data = JsonUtils.getObjectMapper().createObjectNode();
        data.set("event", JsonUtils.getObjectMapper().valueToTree(event));
        data.set("feedback", JsonUtils.getObjectMapper().valueToTree(feedback));
        data.set("materials", JsonUtils.getObjectMapper().valueToTree(materials.stream()
                .sorted(java.util.Comparator.comparing(MesProFeedbackMaterialDO::getId)).toList()));
        data.set("outputFragment", JsonUtils.getObjectMapper().valueToTree(fragment));
        data.putNull("revision");
        data.putArray("revisionDiffs");
        if (revisionId != null) {
            MesProProcessPoolEventRevisionDO revision = revisionMapper.selectOne(
                    new LambdaQueryWrapperX<MesProProcessPoolEventRevisionDO>()
                            .eq(MesProProcessPoolEventRevisionDO::getId, revisionId).last("FOR UPDATE"));
            List<MesProProcessPoolEventRevisionDiffDO> diffs = revisionDiffMapper.selectList(
                    new LambdaQueryWrapperX<MesProProcessPoolEventRevisionDiffDO>()
                            .eq(MesProProcessPoolEventRevisionDiffDO::getRevisionId, revisionId)
                            .orderByAsc(MesProProcessPoolEventRevisionDiffDO::getId).last("FOR UPDATE"));
            if (revision == null || !Objects.equals(revision.getEventId(), event.getId())
                    || !requireObject(revision.getAfterPayload(), "productionCorrection.persistedRevision")
                        .equals(requireObject(event.getRawPayload(), "productionCorrection.persistedRevision"))
                    || diffs == null || diffs.isEmpty() || diffs.stream().anyMatch(diff -> diff == null
                    || !Objects.equals(diff.getRevisionId(), revisionId) || !Objects.equals(diff.getEventId(), event.getId()))) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "productionCorrection.persistedRevision");
            }
            data.set("revision", JsonUtils.getObjectMapper().valueToTree(revision));
            data.set("revisionDiffs", JsonUtils.getObjectMapper().valueToTree(diffs));
        }
        String json = JsonUtils.toJsonString(normalizeAudit(data));
        return new CorrectionAuditState(GxpAuditStateEnvelope.builder().state("PRESENT").canonicalJson(json)
                .objectVersion("sha256:" + MesProBatchRecordExecutionFieldAuditHasher.sha256(json)).build(),
                materials.stream().map(MesProFeedbackMaterialDO::getActiveOrderId).filter(Objects::nonNull)
                        .distinct().sorted().toList());
    }

    private void appendCorrectionAudit(MesProcessPoolProductionReportCorrectionCommand command,
                                       MesProProcessPoolEventDO event, Long revisionId,
                                       MesProBatchRecordExecutionFieldAuditSignatureResult signature,
                                       String signatureContentHash, String challenge,
                                       CorrectionAuditState before, CorrectionAuditState after) {
        String operation = "mes.production-report.correct";
        List<GxpAuditRelation> links = new ArrayList<>();
        links.add(new GxpAuditRelation("SUBJECT", "PROCESS_POOL_EVENT", event.getId().toString(), null, null));
        links.add(new GxpAuditRelation("SOURCE", "WORK_ORDER", event.getWorkOrderId().toString(), null, null));
        links.add(new GxpAuditRelation("SOURCE", "FEEDBACK", event.getFeedbackSourceId().toString(), null, null));
        links.add(new GxpAuditRelation("REVISION", "REVISION", revisionId.toString(), null, null));
        links.add(new GxpAuditRelation("SIGNATURE", "SIGNATURE", signature.getSignatureId().toString(), null, null));
        java.util.stream.Stream.concat(before.activeOrderIds().stream(), after.activeOrderIds().stream())
                .distinct().sorted().forEach(id -> links.add(new GxpAuditRelation("SOURCE", "ACTIVE_ORDER", id.toString(), null, null)));
        String identity = JsonUtils.toJsonString(List.of(event.getTenantId().toString(), operation,
                event.getId().toString(), revisionId.toString()));
        gxpAuditService.append(GxpAuditCommand.builder().eventSchemaVersion(2).operationId(operation)
                .subjectId("MES_PROCESS_POOL_EVENT:" + event.getId()).subjectVersion(after.envelope().getObjectVersion())
                .beforeState(before.envelope()).afterState(after.envelope()).reasonSource("USER")
                .reasonCode("MES_PRODUCTION_REPORT_CORRECT").reason(command.getChangeReason().trim())
                .idempotencyKey("GXP2:" + MesProBatchRecordExecutionFieldAuditHasher.sha256(identity))
                .requestId("MES-PRODUCTION-CORRECTION:" + revisionId).resultStatus("SUCCESS")
                .sourceType("SERVICE_METHOD").sourceLocator(MesProcessPoolProductionReportCorrectionService.class.getName() + "#correct")
                .signatureRecordId(signature.getSignatureId().toString()).signatureContentHash(signatureContentHash)
                .links(links).evidences(List.of(
                        new GxpAuditEvidence("SIGNATURE", signature.getSignatureId().toString(), null,
                                signatureContentHash, "PRODUCTION_REPORT_CORRECTION"),
                        new GxpAuditEvidence("SIGNATURE_CHALLENGE", signature.getSignatureId().toString(), null,
                                challenge, "SIGNED_CORRECTION_REQUEST"),
                        new GxpAuditEvidence("REVISION", revisionId.toString(), null,
                                MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload()), "CORRECTED_PAYLOAD")))
                .build());
    }

    private static JsonNode normalizeAudit(JsonNode node) {
        if (node.isObject()) {
            ObjectNode sorted = JsonUtils.getObjectMapper().createObjectNode();
            java.util.TreeSet<String> keys = new java.util.TreeSet<>();
            node.fieldNames().forEachRemaining(keys::add);
            keys.forEach(key -> sorted.set(key, normalizeAudit(node.get(key))));
            return sorted;
        }
        if (node.isArray()) {
            ArrayNode array = JsonUtils.getObjectMapper().createArrayNode();
            node.forEach(value -> array.add(normalizeAudit(value)));
            return array;
        }
        if (node.isNumber()) return com.fasterxml.jackson.databind.node.TextNode.valueOf(
                node.decimalValue().stripTrailingZeros().toPlainString());
        return node;
    }

    private record CorrectionAuditState(GxpAuditStateEnvelope envelope, List<Long> activeOrderIds) { }

    private void validateCommand(MesProcessPoolProductionReportCorrectionCommand command) {
        if (command == null || command.getEventId() == null || command.getEventId() <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "eventId");
        }
        if (command.getActorUserId() == null || command.getActorUserId() <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "actorUserId");
        }
        if (command.getOutputQuantity() == null
                || command.getOutputQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "outputQuantity");
        }
        if (command.getLossDetails() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "lossDetails");
        }
        if (command.getLossDetails().stream().anyMatch(item -> item == null
                || item.getReasonId() == null || item.getReasonId() <= 0
                || item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "lossDetails");
        }
        if (command.getMaterialDetails() != null
                && command.getMaterialDetails().stream().anyMatch(item -> item == null
                || item.getMaterialId() == null || item.getMaterialId() <= 0
                || item.getOutputQuantity() == null || item.getOutputQuantity().compareTo(BigDecimal.ZERO) < 0
                || item.getLossQuantity() == null || item.getLossQuantity().compareTo(BigDecimal.ZERO) < 0)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "materialDetails");
        }
        if (StrUtil.isBlank(command.getChangeReason())) {
            throw exception(PRO_PROCESS_POOL_REVISION_CHANGE_REASON_REQUIRED);
        }
        if (StrUtil.isBlank(command.getSignaturePassword())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "signaturePassword");
        }
    }

    private MesProProcessPoolQuantityFragmentDO requireOutputFragment(Long eventId) {
        List<MesProProcessPoolQuantityFragmentDO> fragments = fragmentMapper.selectListByEventIdForUpdate(eventId)
                .stream()
                .filter(item -> MesProProcessPoolQuantityFragmentDO.SOURCE_QUANTITY_TYPE_OUTPUT
                        .equals(item.getSourceQuantityType()))
                .toList();
        if (fragments.size() != 1) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "outputQuantityFragment");
        }
        return fragments.get(0);
    }

    private void applyOutputQuantity(BigDecimal after, BigDecimal before, ObjectNode payload,
                                     ObjectNode fieldValues, MesProProcessPoolQuantityFragmentDO fragment,
                                     List<MesProcessPoolEventRevisionFieldChangeBO> changes) {
        requireDecimal(fieldValues.get(FIELD_OUTPUT_QUANTITY), "rawPayload.fieldValues.OUTPUT_QUANTITY");
        payload.put("outputQuantity", after);
        fieldValues.put(FIELD_OUTPUT_QUANTITY, after);
        if (before.compareTo(after) == 0) {
            return;
        }
        changes.add(fieldChange(FIELD_OUTPUT_QUANTITY, "完成数量", before, after,
                true, fragment.getId(), MesProcessPoolFragmentOriginalField.OUTPUT_QUANTITY));
    }

    private void applyLossDetails(
            List<MesProcessPoolProductionReportCorrectionCommand.LossDetailCommand> requested,
            BigDecimal beforeLoss,
            Long routeProcessId,
            ObjectNode payload,
            ObjectNode fieldValues,
            List<MesProcessPoolEventRevisionFieldChangeBO> changes) {
        requireDecimal(fieldValues.get(FIELD_SCRAP_QUANTITY), "rawPayload.fieldValues.SCRAP_QUANTITY");
        requireArray(payload.get("lossDetails"), "rawPayload.lossDetails");
        Map<Long, JsonNode> originalReasons = originalLossReasons(payload);
        Set<Long> duplicateCheck = requested.stream()
                .map(MesProcessPoolProductionReportCorrectionCommand.LossDetailCommand::getReasonId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (duplicateCheck.size() != requested.size()) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "lossDetails.reasonId");
        }

        ArrayNode canonicalDetails = JsonUtils.getObjectMapper().createArrayNode();
        Map<Long, JsonNode> canonicalReasons = new LinkedHashMap<>();
        BigDecimal afterLoss = BigDecimal.ZERO;
        for (MesProcessPoolProductionReportCorrectionCommand.LossDetailCommand detail : requested) {
            if (detail == null || detail.getReasonId() == null || detail.getQuantity() == null
                    || detail.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "lossDetails");
            }
            JsonNode original = originalReasons.get(detail.getReasonId());
            MesFrontlineLossReasonSnapshot snapshot = original == null
                    ? lossReasonValidator.requireEnabledLossReason(
                            routeProcessId, detail.getReasonId(), detail.getQuantity())
                    : new MesFrontlineLossReasonSnapshot(
                            detail.getReasonId(), text(original, "reasonCode"), text(original, "reasonName"));
            ObjectNode canonical = JsonUtils.getObjectMapper().createObjectNode();
            canonical.put("reasonId", snapshot.reasonId());
            putNullable(canonical, "reasonCode", snapshot.reasonCode());
            putNullable(canonical, "reasonName", snapshot.reasonName());
            canonical.put("quantity", detail.getQuantity());
            canonicalDetails.add(canonical);
            canonicalReasons.put(detail.getReasonId(), canonical);
            afterLoss = afterLoss.add(detail.getQuantity());
        }

        payload.set("lossDetails", canonicalDetails);
        payload.set("lossReasonDetails", canonicalDetails.deepCopy());
        payload.put("lossQuantity", afterLoss);
        fieldValues.put(FIELD_SCRAP_QUANTITY, afterLoss);
        boolean hasActualLoss = afterLoss.signum() > 0;
        payload.put("hasActualLoss", hasActualLoss);
        payload.put("zeroLossConfirmed", !hasActualLoss);
        payload.put("lossDecision", hasActualLoss ? "REQUIRED" : "NO_LOSS");
        if (beforeLoss.compareTo(afterLoss) != 0) {
            changes.add(fieldChange(FIELD_SCRAP_QUANTITY, "损耗数量", beforeLoss, afterLoss,
                    false, null, MesProcessPoolFragmentOriginalField.LOSS_QUANTITY));
        }
        addLossReasonChanges(originalReasons, canonicalReasons, changes);
    }

    private void applyMaterialDetails(
            List<MesProcessPoolProductionReportCorrectionCommand.MaterialDetailCommand> requested,
        ObjectNode payload,
        List<MesProcessPoolEventRevisionFieldChangeBO> changes) {
        ArrayNode original = optionalArray(payload.get("materialDetails"));
        if (requested == null || requested.isEmpty()) {
            return;
        }
        if (original == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "materialDetails");
        }
        Map<Long, JsonNode> canonicalReasons = originalLossReasons(payload);
        Map<Long, MesProcessPoolProductionReportCorrectionCommand.MaterialDetailCommand> byMaterialId =
                requested.stream().collect(Collectors.toMap(
                        MesProcessPoolProductionReportCorrectionCommand.MaterialDetailCommand::getMaterialId,
                        item -> item,
                        (left, right) -> right,
                        LinkedHashMap::new));
        ArrayNode updated = original.deepCopy();
        for (JsonNode node : updated) {
            if (!(node instanceof ObjectNode material)) {
                continue;
            }
            Long materialId = longOrNull(material.get("materialId"));
            if (materialId == null || materialId <= 0) {
                continue;
            }
            MesProcessPoolProductionReportCorrectionCommand.MaterialDetailCommand change =
                    byMaterialId.remove(materialId);
            if (change == null) {
                continue;
            }
            BigDecimal beforeOutput = decimalOrNull(material.get("outputQuantity"));
            BigDecimal beforeLoss = decimalOrNull(material.get("lossQuantity"));
            if (beforeOutput != null && beforeOutput.compareTo(change.getOutputQuantity()) != 0) {
                material.put("outputQuantity", change.getOutputQuantity());
                changes.add(fieldChange("MATERIAL_OUTPUT." + materialId,
                        materialFieldName(material, "完成数量"),
                        beforeOutput, change.getOutputQuantity(), false, null,
                        MesProcessPoolFragmentOriginalField.OUTPUT_QUANTITY));
            }
            if (beforeLoss != null && beforeLoss.compareTo(change.getLossQuantity()) != 0) {
                material.put("lossQuantity", change.getLossQuantity());
                changes.add(fieldChange("MATERIAL_LOSS." + materialId,
                        materialFieldName(material, "损耗数量"),
                        beforeLoss, change.getLossQuantity(), false, null,
                        MesProcessPoolFragmentOriginalField.LOSS_QUANTITY));
            }
            if (change.getLossDetails() != null) {
                ArrayNode materialReasons = JsonUtils.getObjectMapper().createArrayNode();
                for (MesProcessPoolProductionReportCorrectionCommand.LossDetailCommand detail : change.getLossDetails()) {
                    JsonNode canonical = detail == null ? null : canonicalReasons.get(detail.getReasonId());
                    if (canonical == null || detail.getQuantity() == null
                            || detail.getQuantity().signum() <= 0) {
                        throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "materialDetails.lossDetails");
                    }
                    ObjectNode materialReason = ((ObjectNode) canonical).deepCopy();
                    materialReason.put("quantity", detail.getQuantity());
                    materialReasons.add(materialReason);
                }
                material.set("lossDetails", materialReasons);
            }
            if (change.getSelectedDevice() == null) {
                material.putNull("selectedDevice");
            } else {
                material.set("selectedDevice", JsonUtils.getObjectMapper().valueToTree(change.getSelectedDevice()));
            }
            if (change.getDeviceParameterReadings() != null) {
                material.set("deviceParameterReadings",
                        JsonUtils.getObjectMapper().valueToTree(change.getDeviceParameterReadings()));
            }
        }
        if (!byMaterialId.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "materialDetails.materialId");
        }
        payload.set("materialDetails", updated);
    }

    private void syncFormalFeedbackSource(MesProProcessPoolEventDO event, ObjectNode afterPayload) {
        if (!FEEDBACK_SOURCE_TYPE.equals(event.getFeedbackSourceType())
                || event.getFeedbackSourceId() == null || event.getFeedbackSourceId() <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "feedbackSource");
        }
        List<MesProFeedbackDO> formalFeedback =
                feedbackMapper.selectListByIdsForUpdate(List.of(event.getFeedbackSourceId()));
        if (formalFeedback == null || formalFeedback.size() != 1 || formalFeedback.get(0) == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "formalFeedback");
        }
        MesProFeedbackDO feedback = formalFeedback.get(0);
        if (!Objects.equals(feedback.getId(), event.getFeedbackSourceId())
                || !Objects.equals(feedback.getWorkOrderId(), event.getWorkOrderId())
                || !Objects.equals(feedback.getRouteId(), event.getRouteId())
                || !Objects.equals(feedback.getProcessId(), event.getProcessId())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "formalFeedback.identity");
        }

        BigDecimal outputQuantity = requireDecimal(afterPayload.get("outputQuantity"), "rawPayload.outputQuantity");
        BigDecimal lossQuantity = requireDecimal(afterPayload.get("lossQuantity"), "rawPayload.lossQuantity");
        if (outputQuantity.compareTo(BigDecimal.ZERO) <= 0 || lossQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "formalFeedback.quantity");
        }
        BigDecimal feedbackQuantity = outputQuantity.add(lossQuantity);
        BigDecimal qualifiedQuantity = outputQuantity;
        LossReasonSync reason = lossQuantity.signum() == 0 ? null : firstLossReason(afterPayload);

        int updated = feedbackMapper.updateCorrectedProductionReport(
                feedback.getId(), feedbackQuantity, qualifiedQuantity, lossQuantity,
                reason == null ? null : reason.reasonId(),
                reason == null ? null : reason.reasonCode(),
                reason == null ? null : reason.reasonName());
        if (updated != 1) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "formalFeedback.update");
        }
        syncFormalMaterialFacts(feedback.getId(), afterPayload);
    }

    private void syncFormalMaterialFacts(Long feedbackId, ObjectNode afterPayload) {
        ArrayNode materialDetails = optionalArray(afterPayload.get("materialDetails"));
        if (materialDetails == null || materialDetails.isEmpty()) {
            return;
        }
        List<MesProFeedbackMaterialDO> rows = feedbackMaterialMapper.selectListByFeedbackIdForUpdate(feedbackId);
        Map<Long, MesProFeedbackMaterialDO> materialById = rows == null ? Map.of() : rows.stream()
                .filter(Objects::nonNull)
                .filter(row -> row.getMaterialId() != null)
                .collect(Collectors.toMap(MesProFeedbackMaterialDO::getMaterialId,
                        row -> row, (left, right) -> left, LinkedHashMap::new));
        Set<Long> missingFormalMaterialIds = new LinkedHashSet<>();
        for (JsonNode node : materialDetails) {
            ObjectNode material = requireObject(node, "materialDetails[]");
            Long materialId = requireLong(material.get("materialId"), "materialDetails.materialId");
            MesProFeedbackMaterialDO row = materialById.remove(materialId);
            if (row == null) {
                missingFormalMaterialIds.add(materialId);
                continue;
            }
            BigDecimal outputQuantity = requireDecimal(material.get("outputQuantity"),
                    "materialDetails.outputQuantity");
            BigDecimal lossQuantity = requireDecimal(material.get("lossQuantity"), "materialDetails.lossQuantity");
            if (outputQuantity.compareTo(BigDecimal.ZERO) < 0 || lossQuantity.compareTo(BigDecimal.ZERO) < 0
                    || lossQuantity.compareTo(outputQuantity) > 0) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "materialDetails.quantity");
            }
            int updated = feedbackMaterialMapper.updateCorrectedMaterialFact(
                    row.getId(), outputQuantity, lossQuantity, materialLossDetailsJson(material, lossQuantity),
                    jsonStringOrNull(material.get("selectedDevice")),
                    jsonStringOrEmptyArray(material.get("deviceParameterReadings")));
            if (updated != 1) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "formalMaterial.update");
            }
        }
        if (!missingFormalMaterialIds.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED,
                    "missingFormalMaterialIds=" + missingFormalMaterialIds);
        }
    }

    private boolean hasMaterialFacts(ObjectNode payload) {
        ArrayNode materialDetails = optionalArray(payload.get("materialDetails"));
        return materialDetails != null && !materialDetails.isEmpty();
    }

    private LossReasonSync firstLossReason(ObjectNode payload) {
        ArrayNode details = requireArray(payload.get("lossDetails"), "rawPayload.lossDetails");
        if (details.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "lossDetails");
        }
        ObjectNode detail = requireObject(details.get(0), "lossDetails[]");
        return new LossReasonSync(
                requireLong(detail.get("reasonId"), "lossDetails.reasonId"),
                requiredText(detail, "reasonCode", "lossDetails.reasonCode"),
                requiredText(detail, "reasonName", "lossDetails.reasonName"));
    }

    private String materialLossDetailsJson(ObjectNode material, BigDecimal lossQuantity) {
        JsonNode details = material.get("lossDetails");
        if (details == null || details.isNull()) {
            if (lossQuantity.signum() == 0) {
                return "[]";
            }
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "materialDetails.lossDetails");
        }
        ArrayNode array = requireArray(details, "materialDetails.lossDetails");
        if (lossQuantity.signum() > 0 && array.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "materialDetails.lossDetails");
        }
        return JsonUtils.toJsonString(array);
    }

    private String jsonStringOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : JsonUtils.toJsonString(node);
    }

    private String jsonStringOrEmptyArray(JsonNode node) {
        return node == null || node.isNull() ? "[]" : JsonUtils.toJsonString(node);
    }

    private String requiredText(ObjectNode node, String fieldName, String errorFieldName) {
        String value = text(node, fieldName);
        if (StrUtil.isBlank(value)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, errorFieldName);
        }
        return value;
    }

    private record LossReasonSync(Long reasonId, String reasonCode, String reasonName) {
    }

    private String materialFieldName(ObjectNode material, String suffix) {
        String materialName = StrUtil.blankToDefault(text(material, "materialName"), text(material, "materialCode"));
        if (StrUtil.isBlank(materialName)) {
            materialName = String.valueOf(requireLong(material.get("materialId"), "materialDetails.materialId"));
        }
        return "物料：" + materialName + " " + suffix;
    }

    private void applyDeviceParameterReadings(
            List<MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand> requested,
            ObjectNode payload,
            ObjectNode fieldValues,
            List<MesProcessPoolEventRevisionFieldChangeBO> changes) {
        ArrayNode original = optionalArray(payload.get("deviceParameterReadings"));
        Map<String, MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand> byKey =
                (requested == null ? List.<MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand>of()
                        : requested).stream()
                        .filter(item -> item != null
                                && item.getDeviceId() != null && item.getDeviceId() > 0
                                && StrUtil.isNotBlank(item.getParameterCode())
                                && (item.getValue() != null || StrUtil.isNotBlank(item.getTextValue())))
                        .collect(Collectors.toMap(
                        item -> parameterKey(item.getDeviceId(), item.getParameterCode()),
                        item -> item,
                        (left, right) -> {
                            if (!sameParameterValue(requestedParameterValue(left), requestedParameterValue(right))) {
                                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED,
                                        "deviceParameterReadings.requestValue");
                            }
                            return left;
                        },
                        LinkedHashMap::new));
        if (byKey.isEmpty() || original == null) {
            return;
        }

        Map<String, Object> originalValues = new LinkedHashMap<>();
        for (JsonNode node : original) {
            if (!(node instanceof ObjectNode reading)) {
                continue;
            }
            Long deviceId = longOrNull(reading.get("deviceId"));
            String parameterCode = text(reading, "parameterCode");
            if (deviceId == null || deviceId <= 0 || StrUtil.isBlank(parameterCode)) {
                continue;
            }
            String key = parameterKey(deviceId, parameterCode);
            var change = byKey.get(key);
            if (change == null) {
                continue;
            }
            Object before = StrUtil.isNotBlank(change.getTextValue())
                    ? StrUtil.blankToDefault(text(reading, "textValue"), text(reading, "value"))
                    : decimalOrNull(reading.get("value"));
            if (originalValues.containsKey(key) && !sameParameterValue(originalValues.get(key), before)) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "deviceParameterReadings.originalValue");
            }
            originalValues.put(key, before);
        }

        ArrayNode updated = original.deepCopy();
        Set<String> changedKeys = new LinkedHashSet<>();
        for (JsonNode node : updated) {
            if (!(node instanceof ObjectNode reading)) {
                continue;
            }
            Long deviceId = longOrNull(reading.get("deviceId"));
            String parameterCode = text(reading, "parameterCode");
            if (deviceId == null || deviceId <= 0 || StrUtil.isBlank(parameterCode)) {
                continue;
            }
            String key = parameterKey(deviceId, parameterCode);
            MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand change = byKey.get(key);
            if (change == null) {
                continue;
            }
            if (StrUtil.isNotBlank(change.getTextValue())) {
                String before = StrUtil.blankToDefault(text(reading, "textValue"), text(reading, "value"));
                String after = change.getTextValue().trim();
                if (Objects.equals(before, after)) {
                    continue;
                }
                reading.put("textValue", after);
                reading.put("value", after);
                reading.put("parameterStatus", "NORMAL");
                updateParameterCopies(payload, fieldValues, reading, parameterCode, after);
                String parameterName = StrUtil.blankToDefault(text(reading, "parameterName"), parameterCode);
                String unit = text(reading, "unit");
                String displayName = StrUtil.isBlank(unit) ? parameterName : parameterName + "（" + unit + "）";
                if (changedKeys.add(key)) {
                    changes.add(fieldChange("DEVICE_PARAMETERS." + parameterCode, displayName,
                            before, after, false, null,
                            MesProcessPoolFragmentOriginalField.DEVICE_PARAMETERS));
                }
            } else {
                BigDecimal before = decimalOrNull(reading.get("value"));
                if (before != null && before.compareTo(change.getValue()) == 0) {
                    continue;
                }
                reading.put("value", change.getValue());
                reading.put("parameterStatus", resolveParameterStatus(
                        change.getValue(), decimalOrNull(reading.get("lowerLimit")),
                        decimalOrNull(reading.get("upperLimit"))));
                updateParameterCopies(payload, fieldValues, reading, parameterCode, change.getValue());
                String parameterName = StrUtil.blankToDefault(text(reading, "parameterName"), parameterCode);
                String unit = text(reading, "unit");
                String displayName = StrUtil.isBlank(unit) ? parameterName : parameterName + "（" + unit + "）";
                if (changedKeys.add(key)) {
                    changes.add(fieldChange("DEVICE_PARAMETERS." + parameterCode, displayName,
                            before, change.getValue(), false, null,
                            MesProcessPoolFragmentOriginalField.DEVICE_PARAMETERS));
                }
            }
        }
        payload.set("deviceParameterReadings", updated);
    }

    private Object requestedParameterValue(
            MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand requested) {
        return StrUtil.isNotBlank(requested.getTextValue()) ? requested.getTextValue().trim() : requested.getValue();
    }

    private boolean sameParameterValue(Object left, Object right) {
        if (left instanceof BigDecimal first && right instanceof BigDecimal second) {
            return first.compareTo(second) == 0;
        }
        return Objects.equals(left, right);
    }

    private void updateParameterCopies(ObjectNode payload, ObjectNode fieldValues, ObjectNode reading,
                                       String parameterCode, Object value) {
        String deviceName = text(reading, "deviceName");
        if (StrUtil.isBlank(deviceName)) {
            return;
        }
        ObjectNode equipmentParameters = objectChildWhenMissing(payload, "equipmentParameters");
        if (equipmentParameters != null) {
            ObjectNode deviceParameters = objectChildWhenMissing(equipmentParameters, deviceName);
            if (deviceParameters != null) {
                putParameterCopyValue(deviceParameters, parameterCode, value);
            }
        }
        ObjectNode fieldDeviceParameters = objectChildWhenMissing(fieldValues, "DEVICE_PARAMETERS");
        if (fieldDeviceParameters != null) {
            ObjectNode fieldDevice = objectChildWhenMissing(fieldDeviceParameters, deviceName);
            if (fieldDevice != null) {
                putParameterCopyValue(fieldDevice, parameterCode, value);
            }
        }
    }

    private void putParameterCopyValue(ObjectNode target, String parameterCode, Object value) {
        if (value instanceof BigDecimal number) {
            target.put(parameterCode, number);
            return;
        }
        target.put(parameterCode, String.valueOf(value));
    }

    private ObjectNode objectChildWhenMissing(ObjectNode parent, String fieldName) {
        JsonNode existing = parent.get(fieldName);
        if (existing instanceof ObjectNode object) {
            return object;
        }
        if (existing != null && !existing.isNull()) {
            return null;
        }
        ObjectNode created = JsonUtils.getObjectMapper().createObjectNode();
        parent.set(fieldName, created);
        return created;
    }

    private void updateOutputFragment(MesProProcessPoolQuantityFragmentDO fragment, BigDecimal outputQuantity) {
        BigDecimal allocated = fragment.getAllocatedQuantity() == null
                ? BigDecimal.ZERO : fragment.getAllocatedQuantity();
        BigDecimal available = outputQuantity.subtract(allocated);
        if (available.compareTo(BigDecimal.ZERO) < 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "outputQuantityFragment.availableQuantity");
        }
        int updated = fragmentMapper.updateById(new MesProProcessPoolQuantityFragmentDO()
                .setId(fragment.getId())
                .setTotalQuantity(outputQuantity)
                .setAvailableQuantity(available));
        if (updated <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "outputQuantityFragment.update");
        }
    }

    private Map<Long, JsonNode> originalLossReasons(ObjectNode payload) {
        ArrayNode details = requireArray(payload.get("lossDetails"), "rawPayload.lossDetails");
        Map<Long, JsonNode> result = new LinkedHashMap<>();
        for (JsonNode detail : details) {
            Long reasonId = requireLong(detail.get("reasonId"), "lossDetails.reasonId");
            BigDecimal quantity = requireDecimal(detail.get("quantity"), "lossDetails.quantity");
            JsonNode previous = result.get(reasonId);
            if (previous == null) {
                result.put(reasonId, detail.deepCopy());
            } else {
                if (!Objects.equals(text(previous, "reasonCode"), text(detail, "reasonCode"))
                        || !Objects.equals(text(previous, "reasonName"), text(detail, "reasonName"))) {
                    throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "lossDetails.reasonMetadata");
                }
                // Multiple material facts can carry the same reason; aggregate only the audit comparison copy.
                ((ObjectNode) previous).put("quantity",
                        requireDecimal(previous.get("quantity"), "lossDetails.quantity").add(quantity));
            }
        }
        return result;
    }

    private void addLossReasonChanges(
            Map<Long, JsonNode> originalReasons,
            Map<Long, JsonNode> canonicalReasons,
            List<MesProcessPoolEventRevisionFieldChangeBO> changes) {
        Set<Long> reasonIds = new LinkedHashSet<>(originalReasons.keySet());
        reasonIds.addAll(canonicalReasons.keySet());
        for (Long reasonId : reasonIds) {
            JsonNode beforeReason = originalReasons.get(reasonId);
            JsonNode afterReason = canonicalReasons.get(reasonId);
            BigDecimal before = beforeReason == null
                    ? BigDecimal.ZERO : requireDecimal(beforeReason.get("quantity"), "lossDetails.quantity");
            BigDecimal after = afterReason == null
                    ? BigDecimal.ZERO : requireDecimal(afterReason.get("quantity"), "lossDetails.quantity");
            if (before.compareTo(after) == 0) {
                continue;
            }
            JsonNode reason = afterReason == null ? beforeReason : afterReason;
            String reasonName = StrUtil.blankToDefault(text(reason, "reasonName"), text(reason, "reasonCode"));
            if (StrUtil.isBlank(reasonName)) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "lossDetails.reasonName");
            }
            changes.add(fieldChange("LOSS_REASON." + reasonId, "损耗原因：" + reasonName,
                    before, after, false, null, MesProcessPoolFragmentOriginalField.LOSS_QUANTITY));
        }
    }

    private MesProcessPoolEventRevisionFieldChangeBO fieldChange(
            String code, String name, Object before, Object after, boolean affectsFragment,
            Long fragmentId, MesProcessPoolFragmentOriginalField originalField) {
        return MesProcessPoolEventRevisionFieldChangeBO.builder()
                .fieldCode(code)
                .fieldName(name)
                .beforeValue(formatValue(before))
                .afterValue(formatValue(after))
                .affectsQuantityFragment(affectsFragment)
                .sourceQuantityFragmentId(fragmentId)
                .originalField(originalField)
                .build();
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "--";
        }
        if (value instanceof BigDecimal number) {
            BigDecimal normalized = number.stripTrailingZeros();
            return normalized.compareTo(BigDecimal.ZERO) == 0 ? "0" : normalized.toPlainString();
        }
        return String.valueOf(value);
    }

    private String resolveParameterStatus(BigDecimal value, BigDecimal lower, BigDecimal upper) {
        if (lower != null && value.compareTo(lower) < 0) {
            return "BELOW_LOWER";
        }
        if (upper != null && value.compareTo(upper) > 0) {
            return "ABOVE_UPPER";
        }
        return "NORMAL";
    }

    private String parameterKey(Long deviceId, String parameterCode) {
        if (deviceId == null || deviceId <= 0 || StrUtil.isBlank(parameterCode)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "deviceParameterReadings");
        }
        return deviceId + ":" + parameterCode.trim();
    }

    private ObjectNode requireObject(String json, String fieldName) {
        if (StrUtil.isBlank(json)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, fieldName);
        }
        try {
            return requireObject(JsonUtils.parseTree(json), fieldName);
        } catch (RuntimeException ex) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, fieldName);
        }
    }

    private ObjectNode requireObject(JsonNode node, String fieldName) {
        if (!(node instanceof ObjectNode object)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, fieldName);
        }
        return object;
    }

    private ArrayNode requireArray(JsonNode node, String fieldName) {
        if (!(node instanceof ArrayNode array)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, fieldName);
        }
        return array;
    }

    private ArrayNode optionalArray(JsonNode node) {
        return node instanceof ArrayNode array ? array : null;
    }

    private BigDecimal requireDecimal(JsonNode node, String fieldName) {
        BigDecimal value = decimalOrNull(node);
        if (value == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, fieldName);
        }
        return value;
    }

    private BigDecimal decimalOrNull(JsonNode node) {
        return node != null && node.isNumber() ? node.decimalValue() : null;
    }

    private Long requireLong(JsonNode node, String fieldName) {
        if (node == null || !node.canConvertToLong()) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, fieldName);
        }
        return node.longValue();
    }

    private Long longOrNull(JsonNode node) {
        return node != null && node.canConvertToLong() ? node.longValue() : null;
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || node.get(fieldName) == null || node.get(fieldName).isNull()) {
            return null;
        }
        return node.get(fieldName).asText();
    }

    private void putNullable(ObjectNode node, String fieldName, String value) {
        if (value == null) {
            node.putNull(fieldName);
        } else {
            node.put(fieldName, value);
        }
    }

}
