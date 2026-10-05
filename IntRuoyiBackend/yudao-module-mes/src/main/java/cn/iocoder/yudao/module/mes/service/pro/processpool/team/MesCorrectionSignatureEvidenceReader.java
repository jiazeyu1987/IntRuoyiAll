package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDiffDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionDiffMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSignatureResult;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditEvidence;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditRelation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable correction evidence; current task ownership and current payload are not historical evidence. */
@Service
@RequiredArgsConstructor
public class MesCorrectionSignatureEvidenceReader {
    private final MesProProcessPoolEventRevisionDiffMapper diffs;
    private final ElectronicSignatureQueryService signatures;
    private final GxpAuditEventMapper audits;

    public void require(MesProProcessPoolEventDO event, MesProProcessPoolEventRevisionDO revision) {
        read(event, revision);
    }

    public Verified read(MesProProcessPoolEventDO event, MesProProcessPoolEventRevisionDO revision) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        check(event != null && revision != null && positive(event.getId()) && positive(revision.getId())
                && Objects.equals(tenant, event.getTenantId()) && Objects.equals(tenant, revision.getTenantId())
                && !Boolean.TRUE.equals(event.getSimulated()) && !text(event.getSimulationStage()) && !text(event.getSimulationRunId())
                && Objects.equals(event.getId(), revision.getEventId())
                && Objects.equals(event.getPoolId(), revision.getPoolId()) && positive(revision.getPoolId())
                && Objects.equals(event.getWorkOrderId(), revision.getWorkOrderId()) && positive(revision.getWorkOrderId())
                && positive(event.getFeedbackSourceId())
                && Objects.equals(event.getRouteId(), revision.getRouteId()) && positive(revision.getRouteId())
                && positive(revision.getRouteProcessId()) && positive(revision.getProcessId())
                && "EFFECTIVE".equals(revision.getRevisionStatus()) && revision.getServerRevisionTime() != null
                && positive(revision.getRevisionSignatureId()) && positive(revision.getModifiedByUserId())
                && Objects.equals(revision.getModifiedByUserId(), revision.getRevisionSignatureUserId()), "修订正式身份不一致");
        boolean production = "PRODUCTION_SUBMIT".equals(event.getEventType());
        check(production || "PQC_INSPECTION".equals(event.getEventType()), "修订业务类型无效");
        if (production) check(Objects.equals(event.getRouteProcessId(), revision.getRouteProcessId())
                && Objects.equals(event.getProcessId(), revision.getProcessId()), "生产修订工序不一致");
        var before = JsonUtils.parseTree(revision.getBeforePayload());
        var after = JsonUtils.parseTree(revision.getAfterPayload());
        check(before != null && before.isObject() && after != null && after.isObject()
                && !Objects.equals(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(revision.getBeforePayload()),
                    MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(revision.getAfterPayload())), "修订缺少真实正文差异");
        check(text(revision.getChangeReason()), "修订更正原因缺失");
        var changes = diffs.selectListByRevisionIds(List.of(revision.getId()));
        check(changes != null && !changes.isEmpty() && changes.stream().allMatch(d ->
                d != null && positive(d.getId()) && Objects.equals(tenant, d.getTenantId())
                && Objects.equals(event.getId(), d.getEventId()) && Objects.equals(revision.getId(), d.getRevisionId())
                && text(d.getFieldCode()) && text(d.getFieldName()) && !"supersededReviewId".equals(d.getFieldCode())
                && !Objects.equals(d.getBeforeValue(), d.getAfterValue())), "修订正式字段差异缺失或不一致");
        check(changes.stream().map(MesProProcessPoolEventRevisionDiffDO::getId).distinct().count() == changes.size()
                && changes.stream().map(MesProProcessPoolEventRevisionDiffDO::getFieldCode).distinct().count() == changes.size(),
                "修订正式字段差异重复");
        var snapshot = JsonUtils.parseObject(revision.getRevisionSignatureSnapshot(), MesProBatchRecordExecutionFieldAuditSignatureResult.class);
        check(snapshot != null && Objects.equals(snapshot.getSignatureId(), revision.getRevisionSignatureId())
                && Objects.equals(snapshot.getActorId(), revision.getModifiedByUserId())
                && snapshot.getSignedAt() != null && text(snapshot.getActorName()), "修订冻结签名快照不一致");
        var evidence = signatures.getById(revision.getRevisionSignatureId());
        check(evidence != null && Objects.equals(evidence.id(), revision.getRevisionSignatureId())
                && Objects.equals(evidence.actorId(), revision.getModifiedByUserId()) && evidence.signedAt() != null
                && "MES".equals(evidence.moduleCode()) && "FIELD_CHANGE".equals(evidence.actionCode())
                && "MES_BATCH_RECORD".equals(evidence.subjectType())
                && Objects.equals(evidence.reason(), revision.getChangeReason().trim())
                && text(evidence.contentHash()) && text(evidence.evidenceHash()), "修订统一签名不一致");
        var verified = signatures.verifyEvidence(evidence.id());
        check(verified != null && Objects.equals(evidence.id(), verified.signatureId())
                && "VALID".equals(verified.verificationStatus())
                && Objects.equals(evidence.contentHash(), verified.storedContentHash())
                && Objects.equals(evidence.contentHash(), verified.calculatedContentHash())
                && Objects.equals(evidence.evidenceHash(), verified.storedEvidenceHash())
                && Objects.equals(evidence.evidenceHash(), verified.calculatedEvidenceHash()), "修订签名完整性核验失败");
        String operation = production ? "mes.production-report.correct" : "mes.pqc-inspection.correct";
        String request = (production ? "MES-PRODUCTION-CORRECTION:" : "MES-PQC-CORRECTION:") + revision.getId();
        var rows = audits.selectList(new LambdaQueryWrapperX<GxpAuditEventDO>().eq(GxpAuditEventDO::getTenantId, tenant)
                .eq(GxpAuditEventDO::getOperationId, operation).eq(GxpAuditEventDO::getSubjectId, "MES_PROCESS_POOL_EVENT:" + event.getId())
                .eq(GxpAuditEventDO::getRequestId, request).eq(GxpAuditEventDO::getSignatureRecordId, evidence.id().toString()));
        check(rows != null && rows.size() == 1, "修订缺少唯一正式审计关联");
        var audit = rows.get(0);
        check(positive(audit.getId()) && Objects.equals(tenant, audit.getTenantId())
                && Objects.equals(operation, audit.getOperationId()) && Objects.equals(request, audit.getRequestId())
                && Objects.equals("MES_PROCESS_POOL_EVENT:" + event.getId(), audit.getSubjectId())
                && Objects.equals(evidence.id().toString(), audit.getSignatureRecordId())
                && Objects.equals(evidence.actorId(), audit.getActorId()) && Objects.equals(evidence.reason(), audit.getReason())
                && Objects.equals(evidence.contentHash(), audit.getSignatureContentHash())
                && Objects.equals(2, audit.getEventSchemaVersion()) && "SUCCESS".equals(audit.getResultStatus())
                && "USER".equals(audit.getReasonSource()) && "SERVICE_METHOD".equals(audit.getSourceType())
                && Objects.equals(production ? "MES_PRODUCTION_REPORT_CORRECT" : "MES_PQC_INSPECTION_CORRECT", audit.getReasonCode())
                && Objects.equals("cn.iocoder.yudao.module.mes.service.pro.processpool."
                    + (production ? "MesProcessPoolProductionReportCorrectionService" : "MesProcessPoolPqcInspectionCorrectionService") + "#correct", audit.getSourceLocator()),
                "修订审计正式来源不一致");
        var manifest = JsonUtils.parseArray(audit.getEvidenceManifestJson(), GxpAuditEvidence.class);
        check(manifest != null && manifest.size() == 3 && new HashSet<>(manifest).size() == 3, "修订审计证据清单无效");
        var challenges = manifest.stream().filter(e -> "SIGNATURE_CHALLENGE".equals(e.evidenceType())
                && evidence.id().toString().equals(e.sourceId()) && e.sourceVersion() == null
                && "SIGNED_CORRECTION_REQUEST".equals(e.role())).toList();
        check(challenges.size() == 1 && challenges.get(0).sha256() != null
                && challenges.get(0).sha256().matches("[0-9a-f]{64}"), "修订签名挑战缺失");
        String challenge = challenges.get(0).sha256();
        String challengePayload = MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(revision.getAfterPayload());
        check(Objects.equals(challenge, MesProBatchRecordExecutionFieldAuditHasher.sha256(
                event.getId() + "|" + challengePayload + "|" + revision.getChangeReason().trim())),
                "修订签名挑战与正式修订正文不一致");
        Set<GxpAuditEvidence> expectedEvidence = Set.of(challenges.get(0),
                new GxpAuditEvidence("SIGNATURE", evidence.id().toString(), null, evidence.contentHash(),
                    production ? "PRODUCTION_REPORT_CORRECTION" : "PQC_INSPECTION_CORRECTION"),
                new GxpAuditEvidence("REVISION", revision.getId().toString(), null,
                    MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(revision.getAfterPayload()), "CORRECTED_PAYLOAD"));
        check(expectedEvidence.equals(new HashSet<>(manifest)), "修订正文与审计证据摘要不一致");
        var relations = JsonUtils.parseArray(audit.getRelationManifestJson(), GxpAuditRelation.class);
        var core = Set.of(new GxpAuditRelation("SUBJECT", "PROCESS_POOL_EVENT", event.getId().toString(), null, null),
                new GxpAuditRelation("SOURCE", "WORK_ORDER", event.getWorkOrderId().toString(), null, null),
                new GxpAuditRelation("SOURCE", production ? "FEEDBACK" : "PQC_INSPECTION_TASK", event.getFeedbackSourceId().toString(), null, null),
                new GxpAuditRelation("REVISION", "REVISION", revision.getId().toString(), null, null),
                new GxpAuditRelation("SIGNATURE", "SIGNATURE", evidence.id().toString(), null, null));
        check(relations != null && new HashSet<>(relations).size() == relations.size()
                && new HashSet<>(relations).containsAll(core), "修订审计业务关联不一致");
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, "FIELD_CHANGE", null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, challenge);
        check(Objects.equals(subject, evidence.subjectId())
                && Objects.equals(MesProBatchRecordExecutionFieldAuditHasher.sha256(subject), evidence.subjectVersion()), "修订签名属于另一业务挑战");
        var expected = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(new SignatureSubjectCommand(
                evidence.actorId(), "MES", "FIELD_CHANGE", "MES_BATCH_RECORD", subject, evidence.subjectVersion(), evidence.reason()));
        check(JsonUtils.parseTree(expected.canonicalContentJson()).equals(JsonUtils.parseTree(evidence.canonicalContentJson())), "修订规范签名正文不一致");
        return new Verified(evidence, verified, snapshot.getActorName(), List.copyOf(changes));
    }

    private static boolean positive(Long value) { return value != null && value > 0; }
    private static boolean text(String value) { return value != null && !value.isBlank(); }
    private static void check(boolean valid, String reason) {
        if (!valid) throw new IllegalStateException("CORRECTION_EVIDENCE_INVALID: " + reason);
    }
    public record Verified(ElectronicSignatureEvidenceDTO evidence, ElectronicSignatureVerificationDTO verification,
                           String signerName, List<MesProProcessPoolEventRevisionDiffDO> changes) {}
}
