package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProductionSubmitSignatureContext;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchActiveOrderDetailService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSignatureResult;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolPqcInspectionCorrectionService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseOrderDetailService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseSignoffService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditEvidence;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditRelation;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/** Reads unified signature evidence only after the caller's existing business-detail authorization. */
@Service
@RequiredArgsConstructor
public class MesActiveOrderSignatureEvidenceService {
    @jakarta.annotation.Resource private cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService verifiedSignatureFacts;
    private cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesSignatureContentProtocol.VerifiedMesSignatureContent verifiedContent(Long id) {
        return cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesSignatureContentProtocol.parseVerifiedContent(verifiedSignatureFacts.requireVerifiedFact(id), cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId());
    }

    @Resource private MesActiveOrderCorrectionEvidenceService correctionEvidence;
    private static final ErrorCode IDENTITY_INVALID = new ErrorCode(1_040_760_450, "签名证据查询身份无效");
    private static final ErrorCode NOT_RELATED = new ErrorCode(1_040_760_451, "签名不属于当前订单正式记录");
    private static final ErrorCode EVIDENCE_INVALID = new ErrorCode(1_040_760_452, "签名证据与正式记录不一致");
    private static final ErrorCode VERIFICATION_INVALID = new ErrorCode(1_040_760_453, "签名完整性核验结果不一致");

    private final MesTeamLeaderActiveOrderDetailService teamDetailService;
    private final MesPqcReleaseOrderDetailService pqcDetailService;
    private final MesProEdhrBatchActiveOrderDetailService batchDetailService;
    private final ElectronicSignatureQueryService signatureQueryService;
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProcessPoolSubmissionReviewMapper reviewMapper;
    private final MesProProcessPoolEventRevisionMapper revisionMapper;
    private final GxpAuditEventMapper auditEventMapper;
    private final MesProductionSubmissionReadBinding productionBinding;
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    private final MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    private final MesProEdhrWorkTaskMapper workTaskMapper;
    private final MesProEdhrBatchExecutionMapper batchMapper;
    private final MesProductionReleaseSignoffService marketSignoff;

    @Transactional(readOnly = true)
    public Result getTeam(Long viewerId, Long activeOrderId, Long signatureId) {
        requirePositive(viewerId); requirePositive(activeOrderId); requirePositive(signatureId);
        return read(teamDetailService.getDetail(viewerId, activeOrderId), signatureId);
    }

    @Transactional(readOnly = true)
    public Result getPqc(Long viewerId, Long applicationId, Long signatureId) {
        requirePositive(viewerId); requirePositive(applicationId); requirePositive(signatureId);
        return read(pqcDetailService.get(viewerId, applicationId).detail(), signatureId);
    }

    @Transactional(readOnly = true)
    public Result getBatch(Long batchExecutionId, Long activeOrderId, Long signatureId) {
        requirePositive(signatureId);
        // Exactly one explicit source identity, just as the formal batch detail entry requires.
        if ((batchExecutionId == null) == (activeOrderId == null)) throw exception(IDENTITY_INVALID);
        if (activeOrderId != null) {
            requirePositive(activeOrderId);
            return read(batchDetailService.getDetailByActiveOrderId(activeOrderId), signatureId);
        }
        requirePositive(batchExecutionId);
        return read(batchDetailService.getDetail(batchExecutionId), signatureId);
    }

    private Result read(MesTeamLeaderActiveOrderDetail detail, Long signatureId) {
        if (detail == null || detail.getActiveOrderId() == null) throw exception(EVIDENCE_INVALID);
        List<Binding> bindings = new ArrayList<>();
        for (var process : detail.getProcesses()) {
            for (var submission : process.getSubmissions()) {
                add(bindings, submission.getSubmitterSignature(), Set.of("PRODUCTION_SUBMIT"), null,
                        List.of(), List.of(submission.getEventId()));
                add(bindings, submission.getReviewerSignature(), Set.of("TEAM_LEADER_REVIEW"),
                        "PROCESS_POOL_EVENT", List.of(submission.getEventId()), List.of(submission.getEventId()));
            }
            for (var submission : process.getPqcSubmissions()) {
                submission.getProductionSubmitterSignatures().forEach(s ->
                        add(bindings, s, Set.of("PRODUCTION_SUBMIT"), null, List.of(), submission.getProductionEventIds()));
                submission.getSubmitterSignatures().forEach(s ->
                        add(bindings, s, Set.of("PQC_SUBMIT"), "MES_PQC_INSPECTION_TASK", submission.getPqcTaskIds(),
                                submission.getSubmittedEventIds()));
                submission.getReviewerSignatures().forEach(s ->
                        addPqcReview(bindings, s, submission.getSubmittedEventIds(), submission.getPqcTaskIds()));
            }
        }
        if (detail.getPqcProductionRelease() != null) {
            add(bindings, detail.getPqcProductionRelease().getSignature(), Set.of("PQC_RELEASE"), null, List.of(), List.of());
        }
        for (var fact : detail.getOperationFacts()) {
            if (fact.getSignatureId() == null) continue;
            // Operation facts are formal persisted foreign-key associations, never inferred from names or dates.
            Set<String> actions = switch (fact.getOperationType()) {
                case "BATCH_RECORD_RELEASE_APPROVED" -> Set.of("MARKET_RELEASE");
                case "PQC_PRODUCTION_RELEASE_APPROVED" -> Set.of("PQC_RELEASE");
                // Other operation facts already provide an exact persisted signature FK and actor.
                // Do not infer a signature action from an audit operation's label.
                default -> Set.of();
            };
            bindings.add(new Binding(fact.getSignatureId(), fact.getActorName(), fact.getActorUserId(),
                    actions, null, List.of(), List.of(), false, List.of()));
        }
        var related = bindings.stream().filter(b -> Objects.equals(b.id(), signatureId)).toList();
        if (related.isEmpty()) {
            List<MesProProcessPoolEventRevisionDO> correctionMatches = new ArrayList<>();
            for (Long eventId : MesActiveOrderCorrectionEvidenceService.eventNames(detail).keySet()) {
                correctionMatches.addAll(revisionMapper.selectListByEventId(eventId).stream()
                        .filter(r -> Objects.equals(signatureId, r.getRevisionSignatureId())).toList());
            }
            if (correctionMatches.size() != 1) throw exception(NOT_RELATED);
            var correction = correctionEvidence.verifyRelated(detail, correctionMatches.get(0));
            return new Result(detail.getActiveOrderId(), correction.signerName(), correction.evidence(), correction.verification());
        }
        ElectronicSignatureEvidenceDTO evidence = signatureQueryService.getById(signatureId);
        if (evidence != null && Objects.equals(evidence.id(), signatureId) && "BPM".equals(evidence.moduleCode())) {
            return readBpmMarketRelease(detail, related, evidence);
        }
        if (evidence == null || !Objects.equals(evidence.id(), signatureId)
                || !Objects.equals(evidence.moduleCode(), MesBatchRecordSignatureSubjectAdapter.MODULE_CODE)
                || !Objects.equals(evidence.subjectType(), MesBatchRecordSignatureSubjectAdapter.SUBJECT_TYPE)
                || evidence.actorId() == null
                || (evidence.signedAt() == null && !"PQC_RELEASE".equals(evidence.actionCode()))
                || evidence.subjectId() == null || evidence.subjectId().isBlank()
                || evidence.canonicalContentJson() == null || evidence.canonicalContentJson().isBlank()) {
            throw exception(EVIDENCE_INVALID);
        }
        var diagnostic = signatureQueryService.verifyEvidence(evidence.id());
        requireVerification(evidence, diagnostic);
        JSONObject content = JSON.parseObject("PQC_RELEASE".equals(evidence.actionCode())
                ? evidence.canonicalContentJson() : diagnosticBusinessContent(evidence, diagnostic));
        String[] subject = new String(Base64.getUrlDecoder().decode(evidence.subjectId()), StandardCharsets.UTF_8)
                .split("\n", -1);
        if (content == null || (subject.length != 17 && subject.length != 18) || evidence.actionCode() == null
                || (subject.length == 18 && !"PQC_RELEASE".equals(evidence.actionCode()))
                || !Objects.equals(subject[1], evidence.actionCode())
                || !Objects.equals(content.getString("actionType"), evidence.actionCode())
                || !Objects.equals(subject[0], content.getString("executionId"))
                || !Objects.equals(subject[9], content.getString("reviewSourceType"))
                || !Objects.equals(subject[10], content.getString("reviewSourceId"))) {
            throw exception(EVIDENCE_INVALID);
        }
        if ("PQC_RELEASE".equals(evidence.actionCode())) {
            var application = applicationMapper.selectOne(new LambdaQueryWrapperX<MesProcessPoolActiveOrderReleaseApplicationDO>()
                    .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getId, Long.valueOf(subject[10]))
                    .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                    .eq(MesProcessPoolActiveOrderReleaseApplicationDO::getDeleted, false));
            if (application == null) throw exception(EVIDENCE_INVALID);
            requirePqcReadBinding(detail, application,
                    readPqcTask(workTaskMapper, application.getPqcReleaseWorkTaskId()),
                    readPqcBatch(batchMapper, application.getBatchExecutionId()), evidence);
        }
        String correctionSignerName = null;
        for (Binding binding : related) {
            if (binding.pqcReviewer() && "FIELD_CHANGE".equals(evidence.actionCode())) {
                correctionSignerName = requirePqcCorrection(binding, evidence, subject, detail.getActiveOrderId());
                continue;
            }
            if ((!binding.actions().isEmpty() && !binding.actions().contains(evidence.actionCode()))
                    || (binding.actorId() != null && !Objects.equals(binding.actorId(), evidence.actorId()))) {
                throw exception(EVIDENCE_INVALID);
            }
            requireFormalActor(binding, evidence, content, subject, detail.getActiveOrderId());
            if (binding.sourceType() != null && (!Objects.equals(content.getString("reviewSourceType"), binding.sourceType())
                    || !binding.sourceIds().contains(content.getLong("reviewSourceId")))) {
                throw exception(EVIDENCE_INVALID);
            }
        }
        ElectronicSignatureVerificationDTO verification = diagnostic;
        requireVerification(evidence, verification);
        String signerName;
        if (Set.of("PRODUCTION_SUBMIT", "PQC_SUBMIT").contains(evidence.actionCode())) {
            signerName = MesSubmissionSignatureIdentityReader.frozenName(evidence);
        } else if ("FIELD_CHANGE".equals(evidence.actionCode())) {
            // The correction reader has verified the persisted revision/review signature snapshot and audit association.
            signerName = correctionSignerName;
            if (signerName == null || signerName.isBlank()) throw exception(EVIDENCE_INVALID);
        } else if ("PQC_RELEASE".equals(evidence.actionCode())) {
            signerName = pqcReadProjection(evidence, verification).getSignerName();
        } else {
            signerName = related.get(0).signerName();
        }
        return new Result(detail.getActiveOrderId(), signerName, evidence, verification);
    }

    private String diagnosticBusinessContent(ElectronicSignatureEvidenceDTO evidence, ElectronicSignatureVerificationDTO verification) {
        var projection = cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesSignatureContentProtocol.readDiagnosticContent(
                evidence, verification, TenantContextHolder.getRequiredTenantId());
        var content = JSON.parseObject(projection.businessContentJson());
        if (projection.identity() != null) content.put("signatureIdentity", JSON.parseObject(evidence.canonicalContentJson()).getJSONObject("signatureIdentity"));
        return content.toJSONString();
    }
    public static MesProEdhrWorkTaskDO readPqcTask(
            MesProEdhrWorkTaskMapper mapper, Long taskId) {
        requirePositive(taskId);
        return mapper.selectOne(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getId, taskId)
                .eq(MesProEdhrWorkTaskDO::getDeleted, false)
                .apply("tenant_id = {0}", TenantContextHolder.getRequiredTenantId()));
    }

    public static MesProEdhrBatchExecutionDO readPqcBatch(
            MesProEdhrBatchExecutionMapper mapper, Long batchId) {
        requirePositive(batchId);
        return mapper.selectOne(new LambdaQueryWrapperX<MesProEdhrBatchExecutionDO>()
                .eq(MesProEdhrBatchExecutionDO::getId, batchId)
                .eq(MesProEdhrBatchExecutionDO::getDeleted, false)
                .apply("tenant_id = {0}", TenantContextHolder.getRequiredTenantId()));
    }

    /** Both formal consumers bind the complete subject to the persisted decision and receipt. */
    static void requirePqcIdentity(MesTeamLeaderActiveOrderDetail detail,
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProEdhrWorkTaskDO task,
            MesProEdhrBatchExecutionDO batch,
            ElectronicSignatureEvidenceDTO evidence) {
        requirePqcReadBinding(detail, application, task, batch, evidence);
        if (!"VALID".equals(evidence.verificationStatus()) || evidence.signedAt() == null
                || evidence.contentHash() == null || evidence.contentHash().isBlank()
                || evidence.evidenceHash() == null || evidence.evidenceHash().isBlank()) throw exception(EVIDENCE_INVALID);
    }

    /** Read association only; this does not authorize a formal action or certify signature validity. */
    public static void requirePqcReadBinding(MesTeamLeaderActiveOrderDetail detail,
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            MesProEdhrWorkTaskDO task,
            MesProEdhrBatchExecutionDO batch,
            ElectronicSignatureEvidenceDTO evidence) {
        if (application == null || task == null || batch == null || evidence == null
                || !positive(application.getId()) || !positive(application.getBatchExecutionId())
                || !positive(application.getWorkOrderId()) || !positive(application.getPqcDecidedBy())
                || !Objects.equals(TenantContextHolder.getRequiredTenantId(), application.getTenantId())
                || Boolean.TRUE.equals(application.getDeleted()) || Boolean.TRUE.equals(task.getDeleted()) || Boolean.TRUE.equals(batch.getDeleted())
                || !Objects.equals(detail.getActiveOrderId(), application.getActiveOrderId())
                || !Objects.equals(detail.getWorkOrderId(), application.getWorkOrderId())
                || !Objects.equals(batch.getId(), application.getBatchExecutionId())
                || !Objects.equals(TenantContextHolder.getRequiredTenantId(), batch.getTenantId())
                || !Objects.equals(batch.getWorkOrderId(), application.getWorkOrderId())
                || !Objects.equals(task.getId(), application.getPqcReleaseWorkTaskId())
                || !Objects.equals(task.getBusinessScopeId(), application.getId())
                || !"RELEASE_APPLICATION".equals(task.getBusinessScopeType())
                || !"PQC_PRODUCTION_RELEASE".equals(task.getTaskType()) || !"DONE".equals(task.getStatus())
                || !"APPROVE".equals(task.getReason()) || !"APPROVE".equals(application.getPqcDecision())
                || application.getApplicationStatus() == null
                || !Set.of("REPORT_UPLOAD_PENDING", "MANAGER_RELEASE_PENDING", "RELEASED", "NONCONFORMANCE_REWORK", "NONCONFORMANCE_VOID")
                        .contains(application.getApplicationStatus())
                || task.getCompletedAt() == null || !Objects.equals(task.getCompletedAt(), application.getPqcDecidedAt())
                || !Objects.equals(task.getBatchExecutionId(), application.getBatchExecutionId())
                || !Objects.equals(task.getWorkOrderId(), application.getWorkOrderId())
                || !Objects.equals(evidence.actorId(), application.getPqcDecidedBy())
                || !"MES".equals(evidence.moduleCode()) || !"MES_BATCH_RECORD".equals(evidence.subjectType())
                || !"PQC_RELEASE".equals(evidence.actionCode())
                || evidence.authenticationMethod() == null
                || !Set.of("SESSION_PLUS_PASSWORD", "SESSION_PLUS_SELECTED_USER_PASSWORD").contains(evidence.authenticationMethod()))
            throw exception(EVIDENCE_INVALID);
        var receipt = JsonUtils.parseTree(application.getDossierSummaryJson());
        if (receipt == null || !receipt.isObject()
                || !exactId(receipt.path("applicationId"), application.getId())
                || !exactId(receipt.path("pqcReleaseWorkTaskId"), task.getId())
                || !exactId(receipt.path("batchExecutionId"), batch.getId())
                || !exactId(receipt.path("decidedBy"), evidence.actorId())
                || !exactId(receipt.path("signatureId"), evidence.id())
                || !"APPROVE".equals(receipt.path("decision").asText())) throw exception(EVIDENCE_INVALID);
        String originalSubject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(batch.getId(), "PQC_RELEASE",
                null, null, null, null, null, null, null, "PQC_RELEASE_APPLICATION", application.getId(),
                "PQC生产放行", "PQC_RELEASE", null, null, null, null);
        if (!Objects.equals(originalSubject, MesBatchRecordSignatureSubjectAdapter.subjectWithoutOpinionSource(evidence.subjectId()))
                || !Objects.equals(MesBatchRecordSignatureSubjectAdapter.subjectVersion(evidence.subjectId()), evidence.subjectVersion()))
            throw exception(EVIDENCE_INVALID);
        requirePqcCanonical(evidence);
    }

    /** Call only after the persisted application/task/receipt association has been checked. */
    public static MesTeamLeaderActiveOrderDetail.SignatureDetail pqcReadProjection(ElectronicSignatureEvidenceDTO evidence,
            cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService.VerifiedSignatureFact fact) {
        var projection = requirePqcCanonical(evidence, fact);
        if ("V1".equals(fact.verification().evidenceFormatVersion())) return pqcReadProjection(evidence, fact.verification());
        return new MesTeamLeaderActiveOrderDetail.SignatureDetail().setSignatureId(fact.id())
                .setSignerName(projection.identity().displayName()).setSignerNameStatus("VERIFIED_SNAPSHOT")
                .setSignedAt(fact.signedAt()).setRole("PQC_RELEASE").setVerification(fact.verification());
    }
    public static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesSignatureContentProtocol.VerifiedMesSignatureContent requirePqcCanonical(
            ElectronicSignatureEvidenceDTO evidence, cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService.VerifiedSignatureFact fact) {
        if (evidence == null || fact == null || !Objects.equals(evidence.id(), fact.id())
                || !Objects.equals(evidence.contentHash(), fact.contentHash()) || !Objects.equals(evidence.evidenceHash(), fact.evidenceHash())
                || !"PQC_RELEASE".equals(fact.actionCode())) throw exception(EVIDENCE_INVALID);
        var projection = cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesSignatureContentProtocol.parseVerifiedContent(fact, TenantContextHolder.getRequiredTenantId());
        if ("V1".equals(fact.verification().evidenceFormatVersion())) { requirePqcCanonical(evidence); return projection; }
        if (projection.identity() == null || !"SYSTEM_USER".equals(projection.identity().domain())) throw exception(EVIDENCE_INVALID);
        return projection;
    }
    public static MesTeamLeaderActiveOrderDetail.SignatureDetail pqcReadProjection(
            ElectronicSignatureEvidenceDTO evidence, ElectronicSignatureVerificationDTO verification) {
        // This diagnostic reader consumes the registered historical V1 PQC producer.
        // Protocol comes from persisted-fact verification, never from business JSON keys.
        if (verification == null || !"V1".equals(verification.evidenceFormatVersion())) throw exception(EVIDENCE_INVALID);
        requirePqcCanonical(evidence);
        requireVerification(evidence, verification);
        boolean snapshot = "SNAPSHOT".equals(evidence.identitySnapshotStatus());
        boolean verified = "VALID".equals(verification.verificationStatus());
        String nameStatus = snapshot ? verified ? "VERIFIED_SNAPSHOT" : "UNVERIFIED_SNAPSHOT" : "NOT_RECORDED";
        String name = !snapshot ? "未记录" : !verified ? "签时姓名未核验"
                : JsonUtils.parseTree(evidence.canonicalContentJson()).path("signatureIdentity").path("displayName").asText();
        return new MesTeamLeaderActiveOrderDetail.SignatureDetail().setSignatureId(evidence.id())
                .setSignerName(name).setSignerNameStatus(nameStatus).setSignedAt(evidence.signedAt())
                .setRole("PQC_RELEASE").setVerification(verification);
    }

    /** Exact kernel V1 SYSTEM_USER snapshot; no directory, audit name or employee-domain substitution. */
    public static void requirePqcCanonical(ElectronicSignatureEvidenceDTO evidence) {
        var expected = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(new SignatureSubjectCommand(
                evidence.actorId(), evidence.moduleCode(), evidence.actionCode(), evidence.subjectType(),
                evidence.subjectId(), evidence.subjectVersion(), evidence.reason()));
        var actual = JsonUtils.parseTree(evidence.canonicalContentJson());
        if (actual == null || !actual.isObject()) throw exception(EVIDENCE_INVALID);
        var expectedContent = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.parseTree(expected.canonicalContentJson());
        var identity = actual.get("signatureIdentity");
        if ("SESSION_PLUS_SELECTED_USER_PASSWORD".equals(evidence.authenticationMethod())) {
            if (identity == null || !identity.isObject() || identity.size() != 5
                    || !"SYSTEM_USER".equals(identity.path("domain").asText())
                    || !numericId(identity.get("signerId"), evidence.actorId())
                    || !numericId(identity.get("tenantId"), TenantContextHolder.getRequiredTenantId())
                    || !positiveJsonId(identity.get("operatorId"))
                    || !identity.path("displayName").isTextual() || identity.path("displayName").asText().isBlank())
                throw exception(EVIDENCE_INVALID);
            expectedContent.set("signatureIdentity", identity);
        } else if (!"SESSION_PLUS_PASSWORD".equals(evidence.authenticationMethod()) || identity != null) {
            throw exception(EVIDENCE_INVALID);
        }
        if (!expectedContent.equals(actual))
            throw exception(EVIDENCE_INVALID);
        MesBatchRecordSignatureSubjectAdapter.verifiedUserOpinion(evidence.subjectId(), evidence.actionCode(), evidence.reason());
    }

    private static boolean positiveJsonId(com.fasterxml.jackson.databind.JsonNode node) {
        return node != null && node.isIntegralNumber() && node.canConvertToLong() && node.longValue() > 0;
    }

    private static boolean numericId(com.fasterxml.jackson.databind.JsonNode node, Long id) {
        return positiveJsonId(node) && Objects.equals(node.longValue(), id);
    }

    private static boolean exactId(com.fasterxml.jackson.databind.JsonNode node, Long id) {
        return positive(id) && (node.isIntegralNumber() || node.isTextual()) && Objects.equals(id.toString(), node.asText());
    }

    public static void requireVerification(ElectronicSignatureEvidenceDTO evidence, ElectronicSignatureVerificationDTO verification) {
        if (verification == null || !Objects.equals(verification.signatureId(), evidence.id())
                || !Objects.equals(verification.storedContentHash(), evidence.contentHash())
                || !Objects.equals(verification.storedEvidenceHash(), evidence.evidenceHash())
                || !Objects.equals(verification.algorithm(), evidence.algorithm()) || !Objects.equals(verification.keyVersion(), evidence.keyVersion())
                || !"LOCAL_DIGEST_TIME_AND_CREATION_AUDIT".equals(verification.verificationScope())
                || !"NOT_BOUND".equals(verification.timeTrustStatus()) || verification.verifiedAt() == null
                || verification.evidenceFormatVersion() == null
                || !Set.of("V1", "V2", "UNSUPPORTED").contains(verification.evidenceFormatVersion())
                || verification.timeSource() == null || !Set.of("SERVER_CLOCK", "UNKNOWN").contains(verification.timeSource()))
            throw exception(VERIFICATION_INVALID);
        List<String> dimensions = Arrays.asList(verification.digestStatus(), verification.timeConsistencyStatus(), verification.auditBindingStatus());
        if (dimensions.stream().anyMatch(s -> s == null || !Set.of("CONSISTENT", "MISSING", "MISMATCH").contains(s)))
            throw exception(VERIFICATION_INVALID);
        boolean hashesMatch = evidence.contentHash() != null && !evidence.contentHash().isBlank()
                && evidence.evidenceHash() != null && !evidence.evidenceHash().isBlank()
                && Objects.equals(evidence.contentHash(), verification.calculatedContentHash())
                && Objects.equals(evidence.evidenceHash(), verification.calculatedEvidenceHash());
        if ("CONSISTENT".equals(verification.digestStatus()) && !hashesMatch) throw exception(VERIFICATION_INVALID);
        if ("UNSUPPORTED".equals(verification.evidenceFormatVersion()) && !"MISMATCH".equals(verification.digestStatus()))
            throw exception(VERIFICATION_INVALID);
        if ("CONSISTENT".equals(verification.timeConsistencyStatus()) && !"SERVER_CLOCK".equals(verification.timeSource()))
            throw exception(VERIFICATION_INVALID);
        String overall = dimensions.contains("MISMATCH") ? "MISMATCH" : dimensions.contains("MISSING") ? "INCOMPLETE" : "VALID";
        if (!Objects.equals(overall, verification.verificationStatus())) throw exception(VERIFICATION_INVALID);
    }

    private Result readBpmMarketRelease(MesTeamLeaderActiveOrderDetail detail, List<Binding> bindings,
                                       ElectronicSignatureEvidenceDTO evidence) {
        if (bindings.stream().anyMatch(b -> !b.actions().equals(Set.of("MARKET_RELEASE"))
                || !Objects.equals(b.actorId(), evidence.actorId()))) throw exception(EVIDENCE_INVALID);
        var owners = applicationMapper.selectListByActiveOrderIds(List.of(detail.getActiveOrderId())).stream()
                .filter(a -> Objects.equals(a.getActiveOrderId(), detail.getActiveOrderId())
                        && "RELEASED".equals(a.getApplicationStatus())).toList();
        if (owners.size() != 1) throw exception(EVIDENCE_INVALID);
        var application = owners.get(0);
        var transaction = releaseTransactionMapper.selectById(application.getReleaseTransactionId());
        var task = workTaskMapper.selectById(application.getReleaseApprovalWorkTaskId());
        var batch = batchMapper.selectById(application.getBatchExecutionId());
        MesProductionReleaseSignoffService.requireBinding(task, application, transaction, batch);
        if (!"RELEASED".equals(transaction.getReleaseStatus()) || !"DONE".equals(task.getStatus())
                || task.getCandidateUserSnapshot() == null
                || Arrays.stream(task.getCandidateUserSnapshot().split(","))
                    .noneMatch(candidate -> candidate.trim().equals(String.valueOf(evidence.actorId())))
                || !Objects.equals(transaction.getApprovalSignatureId(), evidence.id())
                || !Objects.equals(transaction.getApprovedBy(), evidence.actorId())
                || !Objects.equals(transaction.getApprovalSignoffEvidenceHash(), evidence.evidenceHash())
                || !Objects.equals(marketSignoff.findVerifiedSignatureId(task.getId(), transaction.getApprovedBy(),
                        evidence.subjectId(), transaction.getApprovalSignoffEvidenceHash(), transaction.getApprovalOpinion())
                        .orElse(null), evidence.id())) throw exception(EVIDENCE_INVALID);
        var verification = verifiedSignatureFacts.requireVerifiedFact(evidence.id()).verification();
        requireVerification(evidence, verification);
        return new Result(detail.getActiveOrderId(), bindings.get(0).signerName(), evidence, verification);
    }

    private static void add(List<Binding> bindings, MesTeamLeaderActiveOrderDetail.SignatureDetail signature,
                            Set<String> actions, String sourceType, List<Long> sourceIds, List<Long> eventIds) {
        if (signature != null && signature.getSignatureId() != null) {
            bindings.add(new Binding(signature.getSignatureId(), signature.getSignerName(), null, actions,
                    sourceType, sourceIds, eventIds, false, List.of()));
        }
    }

    private static void addPqcReview(List<Binding> bindings, MesTeamLeaderActiveOrderDetail.SignatureDetail signature,
                                     List<Long> eventIds, List<Long> taskIds) {
        if (signature != null && signature.getSignatureId() != null) {
            bindings.add(new Binding(signature.getSignatureId(), signature.getSignerName(), null,
                    Set.of("TEAM_LEADER_REVIEW"), "PROCESS_POOL_EVENT", eventIds, eventIds, true, taskIds));
        }
    }

    private String requirePqcCorrection(Binding binding, ElectronicSignatureEvidenceDTO evidence,
                                      String[] subject, Long activeOrderId) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (!positive(tenantId) || !positive(evidence.actorId())
                || evidence.contentHash() == null || evidence.contentHash().isBlank()
                || evidence.evidenceHash() == null || evidence.evidenceHash().isBlank()) throw exception(EVIDENCE_INVALID);
        List<MesProcessPoolSubmissionReviewDO> reviews = new ArrayList<>();
        for (Long eventId : binding.eventIds()) reviews.addAll(reviewMapper.selectListByEventId(eventId));
        var matches = reviews.stream().filter(r -> Objects.equals(r.getReviewSignatureId(), evidence.id())).toList();
        if (matches.size() != 1) throw exception(EVIDENCE_INVALID);
        var review = matches.get(0);
        JSONObject snapshot = JSON.parseObject(review.getReviewSignatureSnapshotJson());
        if (!approvedPqcReview(review, tenantId) || snapshot == null
                || !Objects.equals(review.getLeaderUserId(), evidence.actorId())
                || !Objects.equals(review.getReviewSignatureUserId(), evidence.actorId())
                || review.getReviewedAt() == null
                || !Objects.equals(review.getReviewRemark(), evidence.reason())
                || evidence.reason() == null || evidence.reason().isBlank()
                || !"PQC_INSPECTION_CORRECTION".equals(snapshot.getString("actionType"))
                || !Objects.equals(snapshot.getLong("processPoolEventId"), review.getEventId())
                || !binding.eventIds().contains(review.getEventId())
                || !Objects.equals(snapshot.getLong("signatureId"), evidence.id())
                || !Objects.equals(snapshot.getLong("actorId"), evidence.actorId())
                || !positive(snapshot.getLong("revisionId")) || !positive(snapshot.getLong("supersededReviewId"))) {
            throw exception(EVIDENCE_INVALID);
        }
        var event = eventMapper.selectById(review.getEventId());
        var revision = revisionMapper.selectById(snapshot.getLong("revisionId"));
        if (event == null || revision == null || !Objects.equals(event.getId(), review.getEventId())
                || !Objects.equals(event.getTenantId(), tenantId) || !Objects.equals(revision.getTenantId(), tenantId)
                || !MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION.equals(event.getEventType())
                || !"MES_PQC_INSPECTION_TASK".equals(event.getFeedbackSourceType())
                || !"MES_PQC_INSPECTION_TASK".equals(event.getRecordbookSourceType())
                || !positive(event.getFeedbackSourceId())
                || !Objects.equals(event.getFeedbackSourceId(), event.getRecordbookSourceId())
                || !binding.pqcTaskIds().contains(event.getFeedbackSourceId())
                || !Objects.equals(revision.getId(), snapshot.getLong("revisionId"))
                || !Objects.equals(revision.getEventId(), event.getId())
                || !positive(event.getPoolId()) || !Objects.equals(revision.getPoolId(), event.getPoolId())
                || !positive(event.getWorkOrderId()) || !Objects.equals(revision.getWorkOrderId(), event.getWorkOrderId())
                || !positive(event.getRouteId()) || !Objects.equals(revision.getRouteId(), event.getRouteId())
                || !positive(event.getQaProcessId())
                || !positive(revision.getRouteProcessId()) || !positive(revision.getProcessId())
                || !MesProProcessPoolEventRevisionDO.STATUS_EFFECTIVE.equals(revision.getRevisionStatus())
                || !Objects.equals(revision.getRevisionSignatureId(), evidence.id())
                || !Objects.equals(revision.getRevisionSignatureUserId(), evidence.actorId())
                || !Objects.equals(revision.getModifiedByUserId(), evidence.actorId())
                || !Objects.equals(revision.getChangeReason(), evidence.reason())
                || revision.getAfterPayload() == null || revision.getAfterPayload().isBlank()
                || revision.getRevisionSignatureSnapshot() == null || revision.getRevisionSignatureSnapshot().isBlank()) {
            throw exception(EVIDENCE_INVALID);
        }
        var previousMatches = reviews.stream().filter(r -> Objects.equals(r.getId(), snapshot.getLong("supersededReviewId"))).toList();
        if (previousMatches.size() != 1 || Objects.equals(review.getId(), snapshot.getLong("supersededReviewId"))
                || !approvedPqcReview(previousMatches.get(0), tenantId)
                || !Objects.equals(previousMatches.get(0).getEventId(), event.getId())) throw exception(EVIDENCE_INVALID);
        JSONObject payload = JSON.parseObject(revision.getAfterPayload());
        JSONObject signed = snapshot.getJSONObject("signature");
        var revisionSignature = JsonUtils.parseObject(revision.getRevisionSignatureSnapshot(),
                MesProBatchRecordExecutionFieldAuditSignatureResult.class);
        if (payload == null || signed == null || revisionSignature == null
                || !Objects.equals(payload.getLong("activeOrderId"), activeOrderId)
                || !Objects.equals(payload.getLong("pqcTaskId"), event.getFeedbackSourceId())
                || !Objects.equals(payload.getLong("supersededReviewId"), snapshot.getLong("supersededReviewId"))
                || !Objects.equals(revisionSignature.getSignatureId(), evidence.id())
                || !Objects.equals(revisionSignature.getActorId(), evidence.actorId())
                || revisionSignature.getSignedAt() == null
                || !Objects.equals(JsonUtils.parseTree(review.getReviewSignatureSnapshotJson()).get("signature"),
                        JsonUtils.parseTree(revision.getRevisionSignatureSnapshot()))) {
            throw exception(EVIDENCE_INVALID);
        }
        String payloadHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(revision.getAfterPayload());
        if (!Objects.equals(payloadHash, snapshot.getString("payloadHash")) || subject[16].isBlank()) {
            throw exception(EVIDENCE_INVALID);
        }
        String expectedSubject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, "FIELD_CHANGE", null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, subject[16]);
        var expected = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(new SignatureSubjectCommand(
                evidence.actorId(), "MES", "FIELD_CHANGE", "MES_BATCH_RECORD", expectedSubject,
                evidence.subjectVersion(), evidence.reason()));
        if (!Objects.equals(expectedSubject, evidence.subjectId())
                || !Objects.equals(MesProBatchRecordExecutionFieldAuditHasher.sha256(expectedSubject), evidence.subjectVersion())
                || !JsonUtils.parseTree(expected.canonicalContentJson()).equals(JsonUtils.parseTree(verifiedContent(evidence.id()).businessContentJson()))) {
            throw exception(EVIDENCE_INVALID);
        }
        requireCorrectionAudit(evidence, event, review, revision, snapshot.getLong("supersededReviewId"),
                activeOrderId, payloadHash, subject[16], tenantId);
        String signerName = revisionSignature.getActorName();
        if (signerName == null || signerName.isBlank()) throw exception(EVIDENCE_INVALID);
        return signerName;
    }

    private void requireCorrectionAudit(ElectronicSignatureEvidenceDTO signature, MesProProcessPoolEventDO event,
                                        MesProcessPoolSubmissionReviewDO review, MesProProcessPoolEventRevisionDO revision,
                                        Long previousReviewId, Long activeOrderId, String payloadHash,
                                        String challenge, Long tenantId) {
        String operation = "mes.pqc-inspection.correct";
        String request = "MES-PQC-CORRECTION:" + revision.getId();
        String subject = "MES_PROCESS_POOL_EVENT:" + event.getId();
        String signatureId = signature.id().toString();
        var audits = auditEventMapper.selectList(new LambdaQueryWrapperX<GxpAuditEventDO>()
                .eq(GxpAuditEventDO::getTenantId, tenantId).eq(GxpAuditEventDO::getOperationId, operation)
                .eq(GxpAuditEventDO::getSubjectId, subject).eq(GxpAuditEventDO::getRequestId, request)
                .eq(GxpAuditEventDO::getSignatureRecordId, signatureId));
        if (audits.size() != 1) throw exception(EVIDENCE_INVALID);
        var audit = audits.get(0);
        if (!positive(audit.getId()) || !Objects.equals(audit.getTenantId(), tenantId)
                || !Objects.equals(audit.getOperationId(), operation) || !Objects.equals(audit.getSubjectId(), subject)
                || !Objects.equals(audit.getRequestId(), request) || !Objects.equals(audit.getSignatureRecordId(), signatureId)
                || !Objects.equals(audit.getSignatureContentHash(), signature.contentHash())
                || !Objects.equals(audit.getActorId(), signature.actorId())
                || !Objects.equals(audit.getReason(), signature.reason())
                || !Objects.equals(audit.getEventSchemaVersion(), 2) || !"SUCCESS".equals(audit.getResultStatus())
                || !"MES_PQC_INSPECTION_CORRECT".equals(audit.getReasonCode()) || !"USER".equals(audit.getReasonSource())
                || !"SERVICE_METHOD".equals(audit.getSourceType())
                || !Objects.equals(audit.getSourceLocator(), MesProcessPoolPqcInspectionCorrectionService.class.getName() + "#correct")
                || audit.getRelationManifestJson() == null || audit.getEvidenceManifestJson() == null) {
            throw exception(EVIDENCE_INVALID);
        }
        Set<GxpAuditRelation> expectedRelations = Set.of(
                new GxpAuditRelation("SUBJECT", "PROCESS_POOL_EVENT", event.getId().toString(), null, null),
                new GxpAuditRelation("SOURCE", "PQC_INSPECTION_TASK", event.getFeedbackSourceId().toString(), null, null),
                new GxpAuditRelation("SOURCE", "ACTIVE_ORDER", activeOrderId.toString(), null, null),
                new GxpAuditRelation("SOURCE", "WORK_ORDER", event.getWorkOrderId().toString(), null, null),
                new GxpAuditRelation("REVISION", "REVISION", revision.getId().toString(), null, null),
                new GxpAuditRelation("SIGNATURE", "SIGNATURE", signatureId, null, null),
                new GxpAuditRelation("PREVIOUS_REVIEW", "PQC_REVIEW", previousReviewId.toString(), null, null),
                new GxpAuditRelation("CORRECTION_REVIEW", "PQC_REVIEW", review.getId().toString(), null, null));
        // This immutable audit bridges the original request challenge to the normalized JSON-column payload.
        // Never recreate the raw-request challenge from revision.afterPayload after a database round trip.
        Set<GxpAuditEvidence> expectedEvidences = Set.of(
                new GxpAuditEvidence("SIGNATURE", signatureId, null, signature.contentHash(), "PQC_INSPECTION_CORRECTION"),
                new GxpAuditEvidence("SIGNATURE_CHALLENGE", signatureId, null, challenge, "SIGNED_CORRECTION_REQUEST"),
                new GxpAuditEvidence("REVISION", revision.getId().toString(), null, payloadHash, "CORRECTED_PAYLOAD"));
        var relations = JsonUtils.parseArray(audit.getRelationManifestJson(), GxpAuditRelation.class);
        var evidences = JsonUtils.parseArray(audit.getEvidenceManifestJson(), GxpAuditEvidence.class);
        if (relations == null || evidences == null || relations.size() != expectedRelations.size()
                || evidences.size() != expectedEvidences.size() || !expectedRelations.equals(new HashSet<>(relations))
                || !expectedEvidences.equals(new HashSet<>(evidences))) throw exception(EVIDENCE_INVALID);
    }

    private static boolean approvedPqcReview(MesProcessPoolSubmissionReviewDO review, Long tenantId) {
        return positive(review.getId()) && Objects.equals(review.getTenantId(), tenantId)
                && MesProcessPoolSubmissionReviewDO.STATUS_APPROVED.equals(review.getReviewStatus())
                && MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC.equals(review.getLeaderType())
                && positive(review.getReviewSignatureId()) && positive(review.getLeaderUserId())
                && Objects.equals(review.getReviewSignatureUserId(), review.getLeaderUserId());
    }

    private static boolean positive(Long id) { return id != null && id > 0; }

    private void requireFormalActor(Binding binding, ElectronicSignatureEvidenceDTO evidence,
                                    JSONObject content, String[] subject, Long activeOrderId) {
        if (binding.actions().contains("PRODUCTION_SUBMIT") || binding.actions().contains("PQC_SUBMIT")) {
            boolean matched = false;
            for (Long eventId : binding.eventIds()) {
                var event = eventMapper.selectById(eventId);
                if (event != null && Objects.equals(event.getSignatureId(), evidence.id())) {
                    if (!Objects.equals(event.getSignatureUserId(), evidence.actorId())) throw exception(EVIDENCE_INVALID);
                    var signatureIdentity = JSON.parseObject(content.getString("signatureIdentity"));
                    if (signatureIdentity != null) {
                        var payload = JSON.parseObject(event.getRawPayload());
                        String domain = binding.actions().contains("PQC_SUBMIT") ? "SYSTEM_USER" : payload == null ? null : payload.getString("signatureIdentityDomain");
                        if (!Objects.equals(domain, signatureIdentity.getString("domain")) || !Objects.equals(event.getTenantId(), signatureIdentity.getLong("tenantId"))
                                || !Objects.equals(event.getSignatureUserId(), signatureIdentity.getLong("signerId"))
                                || !Objects.equals(event.getDeviceAccountId(), signatureIdentity.getLong("operatorId"))) throw exception(EVIDENCE_INVALID);
                    }
                    if (binding.actions().contains("PRODUCTION_SUBMIT")
                            && MesProductionSubmitSignatureContext.SOURCE_TYPE.equals(subject[9])) {
                        var source = productionBinding.require(event, activeOrderId);
                        if (!Objects.equals(subject[11], content.getString("reviewSourceName"))) {
                            throw exception(EVIDENCE_INVALID);
                        }
                        JSONObject identity = JSON.parseObject(subject[11]);
                        if (identity == null || !Objects.equals(source.activeOrderId(), identity.getLong("activeOrderId"))
                                || !Objects.equals(source.activeOrderId(), content.getLong("reviewSourceId"))
                                || !Objects.equals(event.getRouteProcessId(), identity.getLong("routeProcessId"))
                                || !Objects.equals(event.getProcessId(), identity.getLong("processId"))
                                || !Objects.equals(event.getEventIdempotencyKey(),
                                        identity.getString("submissionIdempotencyKey"))) {
                            throw exception(EVIDENCE_INVALID);
                        }
                    }
                    matched = true;
                }
            }
            if (!matched) throw exception(EVIDENCE_INVALID);
        } else if (binding.actions().contains("TEAM_LEADER_REVIEW")) {
            boolean matched = false;
            for (Long eventId : binding.eventIds()) {
                for (var review : reviewMapper.selectListByEventId(eventId)) {
                    if (Objects.equals(review.getReviewSignatureId(), evidence.id())) {
                        if (!Objects.equals(review.getReviewSignatureUserId(), evidence.actorId())) throw exception(EVIDENCE_INVALID);
                        matched = true;
                    }
                }
            }
            if (!matched) throw exception(EVIDENCE_INVALID);
        }
    }

    private static void requirePositive(Long id) {
        if (id == null || id <= 0) throw exception(IDENTITY_INVALID);
    }

    private record Binding(Long id, String signerName, Long actorId,
                           Set<String> actions, String sourceType, List<Long> sourceIds, List<Long> eventIds,
                           boolean pqcReviewer, List<Long> pqcTaskIds) {}

    public record Result(Long activeOrderId, String signerName, ElectronicSignatureEvidenceDTO evidence,
                         ElectronicSignatureVerificationDTO verification) {}
}
