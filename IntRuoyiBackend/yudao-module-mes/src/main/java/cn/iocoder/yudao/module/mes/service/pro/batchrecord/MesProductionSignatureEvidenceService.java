package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditRelation;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditEvidence;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * Production signatures are authenticated from their formal ledger/projection and their original
 * production transaction association. The event snapshot is a checked cache, never an authority.
 */
@Service
@RequiredArgsConstructor
public class MesProductionSignatureEvidenceService {
    public static final ErrorCode EVIDENCE_INVALID = new ErrorCode(1_040_760_455,
            "生产电子签名与正式提交来源或完整性证据不一致");
    private static final String ACTION = MesProBatchRecordExecutionSignatureService.ACTION_PRODUCTION_SUBMIT;
    private static final String PRODUCTION_OPERATION = "mes.production.submit";
    private static final String PROJECTION_OPERATION = "mes.employee-production-signature.create";
    private static final String PRODUCTION_SOURCE =
            "cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackSubmitServiceImpl#submit";

    private final ElectronicSignatureQueryService signatureQueryService;
    private final MesProBatchRecordExecutionSignatureMapper projectionMapper;
    private final GxpAuditEventMapper auditMapper;
    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProProcessPoolEventMapper eventMapper;

    /** Called inside the existing submit transaction, after signing and before event creation. */
    public String snapshotForSubmission(Long actorId, Long signatureId, MesProductionSubmitSignatureContext context) {
        require(positive(actorId) && positive(signatureId) && context != null);
        return JsonUtils.toJsonString(resolve(actorId, signatureId, context, true));
    }

    /** All release readers use this same primary contract, including events with no cached snapshot. */
    public boolean isValidForEvent(MesProProcessPoolEventDO event) {
        if (event == null || !positive(event.getId())
                || !positive(event.getWorkOrderId()) || !positive(event.getRouteId())
                || !positive(event.getRouteProcessId()) || !positive(event.getProcessId())
                || !positive(event.getActualEmployeeId()) || !positive(event.getSignatureId())
                || !Objects.equals(event.getActualEmployeeId(), event.getSignatureUserId())
                || !MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(event.getEventType())
                || !"MES_PRO_FEEDBACK".equals(event.getFeedbackSourceType())
                || !positive(event.getFeedbackSourceId()) || event.getServerSubmitTime() == null
                || StrUtil.hasBlank(event.getEventIdempotencyKey(), event.getRawPayload())) return false;
        JSONObject payload = JSON.parseObject(event.getRawPayload());
        require(payload != null && positive(payload.getLong("activeOrderId")));
        Long activeOrderId = payload.getLong("activeOrderId");
        var active = activeOrderMapper.selectById(activeOrderId);
        require(active != null && Objects.equals(active.getWorkOrderId(), event.getWorkOrderId())
                && Objects.equals(active.getRouteId(), event.getRouteId()));
        var context = new MesProductionSubmitSignatureContext(activeOrderId, event.getRouteProcessId(),
                event.getProcessId(), event.getEventIdempotencyKey());
        Snapshot evidence = resolve(event.getActualEmployeeId(), event.getSignatureId(), context, false);
        var signedIdentity = JSON.parseObject(evidence.canonicalContentJson()).getJSONObject("signatureIdentity");
        if (signedIdentity != null) require(Objects.equals(signedIdentity.getString("domain"), payload.getString("signatureIdentityDomain"))
                && Objects.equals(signedIdentity.getLong("operatorId"), event.getDeviceAccountId())
                && Objects.equals(signedIdentity.getLong("tenantId"), event.getTenantId()));
        require(!LocalDateTime.parse(evidence.signedAt()).isAfter(event.getServerSubmitTime()));
        // Production events are append-only. A signature must identify exactly this event, never a
        // nearest-time or same-process event. This also fixes the formal submission-key association.
        var events = eventMapper.selectList(new LambdaQueryWrapperX<MesProProcessPoolEventDO>()
                .eq(MesProProcessPoolEventDO::getEventType, MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .eq(MesProProcessPoolEventDO::getSignatureId, event.getSignatureId())
                .eq(MesProProcessPoolEventDO::getSignatureUserId, event.getSignatureUserId()));
        require(events != null && events.size() == 1 && sameEvent(events.get(0), event));
        requireProductionAudit(event, activeOrderId, evidence);
        if (StrUtil.isNotBlank(event.getSignatureSnapshot())) {
            require(JsonUtils.parseTree(event.getSignatureSnapshot())
                    .equals(JsonUtils.parseTree(JsonUtils.toJsonString(evidence))));
        }
        return true;
    }

    private Snapshot resolve(Long actorId, Long signatureId, MesProductionSubmitSignatureContext context,
                             boolean requireSignedContext) {
        ElectronicSignatureEvidenceDTO unified = signatureQueryService.getById(signatureId);
        require(unified != null && Objects.equals(unified.actorId(), actorId)
                && ACTION.equals(unified.actionCode()) && "MES".equals(unified.moduleCode()));
        return unifiedSnapshot(unified, actorId, signatureId, context, requireSignedContext);
    }

    private Snapshot unifiedSnapshot(ElectronicSignatureEvidenceDTO evidence, Long actorId, Long signatureId,
                                     MesProductionSubmitSignatureContext context, boolean requireSignedContext) {
        require(Objects.equals(evidence.id(), signatureId) && Objects.equals(evidence.actorId(), actorId)
                && MesBatchRecordSignatureSubjectAdapter.SUBJECT_TYPE.equals(evidence.subjectType())
                && evidence.signedAt() != null && "VALID".equals(evidence.verificationStatus())
                && "SHA-256".equals(evidence.algorithm())
                && Set.of("SESSION_PLUS_PASSWORD", "SESSION_PLUS_SELECTED_USER_PASSWORD",
                    "SESSION_PLUS_EMPLOYEE_PROFILE_PASSWORD").contains(evidence.authenticationMethod())
                && !StrUtil.hasBlank(evidence.subjectId(), evidence.subjectVersion(), evidence.canonicalContentJson(),
                evidence.contentHash(), evidence.evidenceHash(), evidence.keyVersion(), evidence.policyVersion()));
        String[] subject = new String(Base64.getUrlDecoder().decode(evidence.subjectId()), StandardCharsets.UTF_8)
                .split("\n", -1);
        require(subject.length == 17 && "0".equals(subject[0]) && ACTION.equals(subject[1]));
        for (int index : new int[]{2, 3, 4, 5, 6, 7, 8, 12, 13, 14, 15, 16}) require(subject[index].isEmpty());
        var expected = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(new SignatureSubjectCommand(
                actorId, "MES", ACTION, evidence.subjectType(), evidence.subjectId(), evidence.subjectVersion(),
                evidence.reason()));
        var signedContent = JSON.parseObject(evidence.canonicalContentJson());
        var identity = signedContent.getJSONObject("signatureIdentity");
        signedContent.remove("signatureIdentity");
        if (identity != null) require(Objects.equals(identity.getLong("signerId"), actorId)
                && Objects.equals(identity.getLong("tenantId"), TenantContextHolder.getRequiredTenantId())
                && positive(identity.getLong("operatorId"))
                && Set.of("SYSTEM_USER", "MES_EMPLOYEE_PROFILE").contains(identity.getString("domain")));
        require(Objects.equals(DigestUtil.sha256Hex(evidence.subjectId()), evidence.subjectVersion())
                && JsonUtils.parseTree(expected.canonicalContentJson()).equals(JsonUtils.parseTree(signedContent.toJSONString())));
        // Original subject contents are never rewritten. The mandatory audited event association
        // below is the primary business binding for every event, even when the subject is unscoped.
        boolean unscoped = subject[9].isEmpty() && subject[10].isEmpty() && subject[11].isEmpty();
        require(!requireSignedContext || !unscoped);
        if (!unscoped) {
            require(MesProductionSubmitSignatureContext.SOURCE_TYPE.equals(subject[9])
                    && Objects.equals(String.valueOf(context.activeOrderId()), subject[10])
                    && JsonUtils.parseTree(context.sourceName()).equals(JsonUtils.parseTree(subject[11])));
        }
        var verification = signatureQueryService.verifyEvidence(signatureId);
        require(verification != null && Objects.equals(verification.signatureId(), signatureId)
                && "VALID".equals(verification.verificationStatus())
                && Objects.equals(evidence.contentHash(), verification.storedContentHash())
                && Objects.equals(evidence.contentHash(), verification.calculatedContentHash())
                && Objects.equals(evidence.evidenceHash(), verification.storedEvidenceHash())
                && Objects.equals(evidence.evidenceHash(), verification.calculatedEvidenceHash())
                && Objects.equals(evidence.algorithm(), verification.algorithm())
                && Objects.equals(evidence.keyVersion(), verification.keyVersion()));
        require(identity == null ? "SESSION_PLUS_PASSWORD".equals(evidence.authenticationMethod())
                : Objects.equals(evidence.authenticationMethod(), "MES_EMPLOYEE_PROFILE".equals(identity.getString("domain"))
                    ? "SESSION_PLUS_EMPLOYEE_PROFILE_PASSWORD" : "SESSION_PLUS_SELECTED_USER_PASSWORD"));
        var audit = requireUnifiedSignatureAudit(evidence);
        return new Snapshot(identity != null && "MES_EMPLOYEE_PROFILE".equals(identity.getString("domain"))
                ? "UNIFIED_EMPLOYEE_PROFILE" : "UNIFIED_SYSTEM_USER", signatureId, actorId, evidence.signedAt().toString(),
                evidence.contentHash(), evidence.evidenceHash(), evidence.canonicalContentJson(), audit.getTransactionId());
    }

    private GxpAuditEventDO requireUnifiedSignatureAudit(ElectronicSignatureEvidenceDTO evidence) {
        var audits = auditMapper.selectList(new LambdaQueryWrapperX<GxpAuditEventDO>()
                .eq(GxpAuditEventDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(GxpAuditEventDO::getOperationId, "signature.record.create")
                .eq(GxpAuditEventDO::getSignatureRecordId, evidence.id().toString()));
        require(audits != null && audits.size() == 1);
        var audit = audits.get(0);
        requireAuditIntegrity(audit);
        require("signature.record.create".equals(audit.getOperationId())
                && Objects.equals(audit.getSignatureRecordId(), evidence.id().toString())
                && Objects.equals(audit.getSignatureContentHash(), evidence.contentHash())
                && Objects.equals(audit.getSubjectId(), evidence.subjectType() + ":" + evidence.subjectId())
                && Objects.equals(audit.getSubjectVersion(), evidence.subjectVersion())
                && "ELECTRONIC_SIGNATURE_RECORDED".equals(audit.getAfterState())
                && "ElectronicSignatureServiceImpl.sign".equals(audit.getSourceLocator()));
        JSONObject after = JSON.parseObject(audit.getAfterStateJson());
        require(after != null && Objects.equals(after.getLong("signatureId"), evidence.id())
                && Objects.equals(after.getLong("actorId"), evidence.actorId())
                && Objects.equals(after.getString("moduleCode"), evidence.moduleCode())
                && Objects.equals(after.getString("actionCode"), evidence.actionCode())
                && Objects.equals(after.getString("subjectType"), evidence.subjectType())
                && Objects.equals(after.getString("subjectId"), evidence.subjectId())
                && Objects.equals(after.getString("subjectVersion"), evidence.subjectVersion())
                && Objects.equals(after.getString("contentHash"), evidence.contentHash())
                && Objects.equals(after.getString("evidenceHash"), evidence.evidenceHash())
                && Objects.equals(after.getString("verificationStatus"), evidence.verificationStatus()));
        return audit;
    }

    private void requireProductionAudit(MesProProcessPoolEventDO event, Long activeOrderId, Snapshot evidence) {
        var audits = auditMapper.selectList(new LambdaQueryWrapperX<GxpAuditEventDO>()
                .eq(GxpAuditEventDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(GxpAuditEventDO::getOperationId, PRODUCTION_OPERATION)
                .eq(GxpAuditEventDO::getSubjectId, "MES_PROCESS_POOL_EVENT:" + event.getId()));
        require(audits != null && audits.size() == 1);
        var audit = audits.get(0);
        requireAuditIntegrity(audit);
        require(PRODUCTION_OPERATION.equals(audit.getOperationId())
                && Objects.equals(audit.getSubjectId(), "MES_PROCESS_POOL_EVENT:" + event.getId())
                && Objects.equals(audit.getSubjectVersion(), event.getId().toString())
                && Objects.equals(audit.getSignatureRecordId(), event.getSignatureId().toString())
                && Objects.equals(audit.getTransactionId(), evidence.signatureTransactionId())
                && Objects.equals(audit.getIdempotencyKey(), "PRODUCTION_SUBMIT_EVENT:" + event.getId())
                && Objects.equals(audit.getRequestId(), "MES-PRODUCTION-SUBMIT:" + event.getId())
                && "ABSENT".equals(audit.getBeforeState()) && "PRESENT".equals(audit.getAfterState())
                && "MES_PRODUCTION_SUBMIT".equals(audit.getReasonCode()) && "SYSTEM".equals(audit.getReasonSource())
                && "SERVICE_METHOD".equals(audit.getSourceType()) && PRODUCTION_SOURCE.equals(audit.getSourceLocator()));
        JSONObject after = JSON.parseObject(audit.getAfterStateJson());
        require(after != null && "PRODUCTION".equals(after.getString("profile"))
                && Objects.equals(after.getLong("eventId"), event.getId())
                && Objects.equals(after.getLong("feedbackId"), event.getFeedbackSourceId())
                && Objects.equals(after.getLong("activeOrderId"), activeOrderId)
                && Objects.equals(after.getLong("routeProcessId"), event.getRouteProcessId())
                && Objects.equals(after.getLong("processId"), event.getProcessId())
                && Objects.equals(after.getLong("actualEmployeeId"), event.getActualEmployeeId())
                && Objects.equals(after.getLong("signatureEmployeeId"), event.getSignatureUserId())
                && Objects.equals(after.getLong("signatureId"), event.getSignatureId()));
        JSONObject performed = JSON.parseObject(audit.getPerformedByJson());
        require(performed != null && Objects.equals(performed.getLong("actorId"), event.getActualEmployeeId())
                && Objects.equals(performed.getString("actorType"),
                "UNIFIED_SYSTEM_USER".equals(evidence.authority()) ? "SYSTEM_USER" : "MES_EMPLOYEE_PROFILE"));
        JSONObject sourceSnapshot = after.getJSONObject("formalSourceSnapshot");
        require(sourceSnapshot != null && !StrUtil.hasBlank(sourceSnapshot.getString("snapshotId"),
                sourceSnapshot.getString("snapshotHash")) && sourceSnapshot.getJSONObject("content") != null
                && Objects.equals(sourceSnapshot.getString("snapshotHash"), after.getString("formalSourceSnapshotHash")));
        var evidences = JsonUtils.parseArray(audit.getEvidenceManifestJson(), GxpAuditEvidence.class);
        require(evidences != null && evidences.size() == 1 && evidences.get(0).equals(new GxpAuditEvidence(
                "FORMAL_SOURCE_SNAPSHOT", sourceSnapshot.getString("snapshotId"), null,
                sourceSnapshot.getString("snapshotHash"), "PRODUCTION")));
        Set<GxpAuditRelation> expected = Set.of(
                new GxpAuditRelation("SUBJECT", "ACTIVE_ORDER", activeOrderId.toString(), null, null),
                new GxpAuditRelation("SOURCE", "FEEDBACK", event.getFeedbackSourceId().toString(), null, null),
                new GxpAuditRelation("SOURCE", "PROCESS_POOL_EVENT", event.getId().toString(), event.getId().toString(), null),
                new GxpAuditRelation("SIGNATURE", "SIGNATURE", event.getSignatureId().toString(), null, null));
        var relations = JsonUtils.parseArray(audit.getRelationManifestJson(), GxpAuditRelation.class);
        require(relations != null && relations.size() == expected.size() && expected.equals(new HashSet<>(relations)));
    }

    private static void requireAuditIntegrity(GxpAuditEventDO audit) {
        require(audit != null && positive(audit.getId())
                && Objects.equals(audit.getTenantId(), TenantContextHolder.getRequiredTenantId())
                && "SUCCESS".equals(audit.getResultStatus()) && Integer.valueOf(2).equals(audit.getEventSchemaVersion())
                && "SHA-256".equals(audit.getAlgorithm()) && "GXP_CANONICAL_V2".equals(audit.getCanonicalizationVersion())
                && !StrUtil.hasBlank(audit.getTransactionId(), audit.getCanonicalEventJson(), audit.getEventHash(),
                audit.getBeforeStateJson(), audit.getAfterStateJson(), audit.getStatePayloadHash())
                && Objects.equals(DigestUtil.sha256Hex(audit.getCanonicalEventJson()), audit.getEventHash())
                && Objects.equals(DigestUtil.sha256Hex(audit.getBeforeStateJson() + "\u001f" + audit.getAfterStateJson()),
                audit.getStatePayloadHash()));
        var canonical = JsonUtils.parseTree(audit.getCanonicalEventJson());
        var bean = new BeanWrapperImpl(audit);
        for (String field : List.of("tenantId", "operationId", "subjectId", "subjectVersion", "actorId",
                "idempotencyKey", "requestId", "resultStatus", "eventSchemaVersion", "canonicalizationVersion",
                "algorithm", "transactionId", "signatureRecordId", "signatureContentHash", "reasonCode", "reasonSource",
                "sourceType", "sourceLocator", "relationManifestJson", "evidenceManifestJson", "performedByJson",
                "beforeState", "afterState", "afterObjectVersion", "beforeStateJson", "afterStateJson", "statePayloadHash")) {
            Object column = bean.getPropertyValue(field);
            require(column == null ? canonical.get(field) == null || canonical.get(field).isNull()
                    : Objects.equals(canonical.get(field), JsonUtils.parseTree(JsonUtils.toJsonString(column))));
        }
    }

    private static boolean sameEvent(MesProProcessPoolEventDO persisted, MesProProcessPoolEventDO event) {
        return persisted != null && Objects.equals(persisted.getId(), event.getId())
                && Objects.equals(persisted.getEventType(), event.getEventType())
                && Objects.equals(persisted.getWorkOrderId(), event.getWorkOrderId())
                && Objects.equals(persisted.getRouteId(), event.getRouteId())
                && Objects.equals(persisted.getRouteProcessId(), event.getRouteProcessId())
                && Objects.equals(persisted.getProcessId(), event.getProcessId())
                && Objects.equals(persisted.getFeedbackSourceType(), event.getFeedbackSourceType())
                && Objects.equals(persisted.getFeedbackSourceId(), event.getFeedbackSourceId())
                && Objects.equals(persisted.getEventIdempotencyKey(), event.getEventIdempotencyKey())
                && Objects.equals(persisted.getSignatureId(), event.getSignatureId())
                && Objects.equals(persisted.getSignatureUserId(), event.getSignatureUserId())
                && Objects.equals(persisted.getActualEmployeeId(), event.getActualEmployeeId())
                && Objects.equals(persisted.getServerSubmitTime(), event.getServerSubmitTime())
                && Objects.equals(persisted.getRawPayload(), event.getRawPayload());
    }

    private static boolean positive(Long value) { return value != null && value > 0; }
    private static void require(boolean condition) { if (!condition) throw exception(EVIDENCE_INVALID); }

    public record Snapshot(String authority, Long signatureId, Long actorId, String signedAt,
                           String contentHash, String evidenceHash, String canonicalContentJson,
                           String signatureTransactionId) {}
}
