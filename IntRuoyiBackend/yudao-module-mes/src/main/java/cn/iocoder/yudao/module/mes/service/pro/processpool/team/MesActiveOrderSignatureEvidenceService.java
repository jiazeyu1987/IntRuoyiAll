package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProductionSubmitSignatureContext;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchActiveOrderDetailService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseOrderDetailService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/** Reads unified signature evidence only after the caller's existing business-detail authorization. */
@Service
@RequiredArgsConstructor
public class MesActiveOrderSignatureEvidenceService {
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
                        add(bindings, s, Set.of("TEAM_LEADER_REVIEW"),
                                "PROCESS_POOL_EVENT", submission.getSubmittedEventIds(), submission.getSubmittedEventIds()));
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
                    actions, null, List.of(), List.of()));
        }
        var related = bindings.stream().filter(b -> Objects.equals(b.id(), signatureId)).toList();
        if (related.isEmpty()) throw exception(NOT_RELATED);
        ElectronicSignatureEvidenceDTO evidence = signatureQueryService.getById(signatureId);
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
        for (Binding binding : related) {
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
        return new Result(detail.getActiveOrderId(), related.get(0).signerName(), evidence, verification);
    }

    private static void add(List<Binding> bindings, MesTeamLeaderActiveOrderDetail.SignatureDetail signature,
                            Set<String> actions, String sourceType, List<Long> sourceIds, List<Long> eventIds) {
        if (signature != null && signature.getSignatureId() != null) {
            bindings.add(new Binding(signature.getSignatureId(), signature.getSignerName(), null, actions,
                    sourceType, sourceIds, eventIds));
        }
    }

    private void requireFormalActor(Binding binding, ElectronicSignatureEvidenceDTO evidence,
                                    JSONObject content, String[] subject, Long activeOrderId) {
        if (binding.actions().contains("PRODUCTION_SUBMIT") || binding.actions().contains("PQC_SUBMIT")) {
            boolean matched = false;
            for (Long eventId : binding.eventIds()) {
                var event = eventMapper.selectById(eventId);
                if (event != null && Objects.equals(event.getSignatureId(), evidence.id())) {
                    if (!Objects.equals(event.getSignatureUserId(), evidence.actorId())) throw exception(EVIDENCE_INVALID);
                    if (binding.actions().contains("PRODUCTION_SUBMIT")
                            && MesProductionSubmitSignatureContext.SOURCE_TYPE.equals(subject[9])) {
                        if (!Objects.equals(subject[11], content.getString("reviewSourceName"))) {
                            throw exception(EVIDENCE_INVALID);
                        }
                        JSONObject identity = JSON.parseObject(subject[11]);
                        if (identity == null || !Objects.equals(activeOrderId, identity.getLong("activeOrderId"))
                                || !Objects.equals(activeOrderId, content.getLong("reviewSourceId"))
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
                           Set<String> actions, String sourceType, List<Long> sourceIds, List<Long> eventIds) {}

    public record Result(Long activeOrderId, String signerName, ElectronicSignatureEvidenceDTO evidence,
                         ElectronicSignatureVerificationDTO verification) {}
}
