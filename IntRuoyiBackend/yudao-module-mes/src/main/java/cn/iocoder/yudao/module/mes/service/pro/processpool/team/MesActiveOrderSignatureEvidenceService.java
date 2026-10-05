package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
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
                || evidence.actorId() == null || evidence.signedAt() == null
                || evidence.subjectId() == null || evidence.subjectId().isBlank()
                || evidence.canonicalContentJson() == null || evidence.canonicalContentJson().isBlank()) {
            throw exception(EVIDENCE_INVALID);
        }
        JSONObject content = JSON.parseObject(evidence.canonicalContentJson());
        String[] subject = new String(Base64.getUrlDecoder().decode(evidence.subjectId()), StandardCharsets.UTF_8)
                .split("\n", -1);
        if (content == null || subject.length != 17 || evidence.actionCode() == null
                || !Objects.equals(subject[1], evidence.actionCode())
                || !Objects.equals(content.getString("actionType"), evidence.actionCode())
                || !Objects.equals(subject[0], content.getString("executionId"))
                || !Objects.equals(subject[9], content.getString("reviewSourceType"))
                || !Objects.equals(subject[10], content.getString("reviewSourceId"))) {
            throw exception(EVIDENCE_INVALID);
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
        ElectronicSignatureVerificationDTO verification = signatureQueryService.verifyEvidence(signatureId);
        if (verification == null || !Objects.equals(verification.signatureId(), signatureId)
                || !Objects.equals(verification.storedContentHash(), evidence.contentHash())
                || !Objects.equals(verification.storedEvidenceHash(), evidence.evidenceHash())
                || verification.verificationStatus() == null
                || !Set.of("VALID", "MISMATCH").contains(verification.verificationStatus())) {
            throw exception(VERIFICATION_INVALID);
        }
        String calculatedStatus = Objects.equals(verification.storedContentHash(), verification.calculatedContentHash())
                && Objects.equals(verification.storedEvidenceHash(), verification.calculatedEvidenceHash())
                ? "VALID" : "MISMATCH";
        if (!Objects.equals(calculatedStatus, verification.verificationStatus())) throw exception(VERIFICATION_INVALID);
        String signerName;
        if (Set.of("PRODUCTION_SUBMIT", "PQC_SUBMIT").contains(evidence.actionCode())) {
            signerName = MesSubmissionSignatureIdentityReader.frozenName(evidence);
        } else if ("FIELD_CHANGE".equals(evidence.actionCode())) {
            // The correction reader has verified the persisted revision/review signature snapshot and audit association.
            signerName = correctionSignerName;
            if (signerName == null || signerName.isBlank()) throw exception(EVIDENCE_INVALID);
        } else {
            signerName = related.get(0).signerName();
        }
        return new Result(detail.getActiveOrderId(), signerName, evidence, verification);
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
        return new Result(detail.getActiveOrderId(), bindings.get(0).signerName(), evidence,
                signatureQueryService.verifyEvidence(evidence.id()));
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
                || !JsonUtils.parseTree(expected.canonicalContentJson()).equals(JsonUtils.parseTree(evidence.canonicalContentJson()))) {
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
                    var signatureIdentity = content.getJSONObject("signatureIdentity");
                    if (signatureIdentity != null) {
                        var payload = JSON.parseObject(event.getRawPayload());
                        String domain = binding.actions().contains("PQC_SUBMIT") ? "SYSTEM_USER"
                                : payload == null ? null : payload.getString("signatureIdentityDomain");
                        if (!Objects.equals(domain, signatureIdentity.getString("domain"))
                                || !Objects.equals(event.getSignatureUserId(), signatureIdentity.getLong("signerId"))
                                || !Objects.equals(event.getDeviceAccountId(), signatureIdentity.getLong("operatorId"))) {
                            throw exception(EVIDENCE_INVALID);
                        }
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
