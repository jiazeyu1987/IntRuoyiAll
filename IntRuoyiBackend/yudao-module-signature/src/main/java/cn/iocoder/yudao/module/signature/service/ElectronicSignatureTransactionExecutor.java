package cn.iocoder.yudao.module.signature.service;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.api.dto.*;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.system.api.signature.SignatureAuthenticationApi;
import cn.iocoder.yudao.module.system.api.signature.dto.AuthenticatedSigner;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Trusted V2 writer. Caller holds tenant ledger and domain roots before entering this method. */
@Service
public class ElectronicSignatureTransactionExecutor {
    private final ElectronicSignatureRecordMapper mapper;
    private final GxpAuditService audit;
    private final List<SignatureAuthenticationApi> authentication;
    private final Clock clock;
    public ElectronicSignatureTransactionExecutor(ElectronicSignatureRecordMapper mapper, GxpAuditService audit,
            List<SignatureAuthenticationApi> authentication, Clock clock) { this.mapper = mapper; this.audit = audit; this.authentication = authentication; this.clock = clock; }
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    @GxpWriteOperation(operationId = "signature.record.create")
    public ElectronicSignatureResult executeAuthenticated(AuthenticatedSignatureRequest request, AuthenticatedSigner signer) {
        Objects.requireNonNull(request); Objects.requireNonNull(signer);
        audit.assertLedgerLockHeld();
        var command = request.command(); var subject = request.subject(); var action = request.action();
        if (!Objects.equals(TenantContextHolder.getRequiredTenantId(), signer.tenantId()) || !Objects.equals(command.subjectId(), subject.subjectId()) || !Objects.equals(command.expectedSubjectVersion(), subject.subjectVersion()) || !Objects.equals(command.subjectType(), subject.subjectType()) || !Objects.equals(command.actionCode(), action.actionCode()) || !Objects.equals(command.subjectType(), action.subjectType()) || request.displayName() == null || request.displayName().isBlank() || request.stableSubjectKey() == null || request.signatureImage() == null) throw new IllegalArgumentException("SIGNATURE_LOCKED_REQUEST_INVALID");
        String requestHash = ElectronicSignatureV2Protocol.requestHash(request, signer);
        var existing = mapper.selectOne(new LambdaQueryWrapper<ElectronicSignatureRecordDO>().eq(ElectronicSignatureRecordDO::getTenantId, signer.tenantId()).eq(ElectronicSignatureRecordDO::getIdempotencyKey, command.idempotencyKey()));
        if (existing != null) {
            if (!"V2".equals(existing.getCommandHashVersion()) || !requestHash.equals(existing.getCommandHash())) throw new IllegalArgumentException("SIGNATURE_IDEMPOTENCY_CONFLICT");
            throw new IllegalStateException("SIGNATURE_PARTIAL_COMMIT_WITHOUT_CENTER_RECEIPT");
        }
        var providers = authentication.stream().filter(p -> p.identityDomain().equals(signer.domain())).toList(); if (providers.size() != 1) throw new IllegalStateException("SIGNATURE_IDENTITY_PROVIDER_NOT_UNIQUE");
        Instant sampled = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        if (!signer.equals(providers.get(0).lockAndRevalidate(signer, sampled))) throw new IllegalArgumentException("SIGNATURE_CREDENTIAL_VERSION_CHANGED");
        var signedAt = LocalDateTime.ofInstant(sampled, ZoneId.of("Asia/Shanghai"));
        ObjectNode content = (ObjectNode) JsonUtils.parseTree(subject.canonicalContentJson()).deepCopy();
        for (String key : Set.of("evidenceFormatVersion", "commandHashVersion", "signatureIdentity", "signatureTime", "signatureImage", "nodePosition")) if (content.has(key)) throw new IllegalArgumentException("SIGNATURE_RESERVED_CONTENT_KEY: " + key);
        content.put("evidenceFormatVersion", "V2"); content.put("commandHashVersion", "V2");
        var identity = content.putObject("signatureIdentity"); identity.put("domain", signer.domain()); identity.put("signerId", signer.actorId().toString()); identity.put("displayName", request.displayName()); identity.put("operatorId", signer.operatorId().toString()); identity.put("tenantId", signer.tenantId().toString());
        var time = content.putObject("signatureTime"); time.put("localDateTime", signedAt.format(java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss"))); time.put("zoneId", "Asia/Shanghai"); time.put("precision", "SECOND"); time.put("source", "SERVER_CLOCK"); content.set("signatureImage", request.signatureImage().deepCopy());
        var position = content.putObject("nodePosition"); position.put("applicability", subject.taskId() == null ? "NOT_APPLICABLE" : "TASK_CONTEXT"); position.put("taskId", subject.taskId()); position.put("nodeCode", subject.nodeCode()); position.put("processInstanceId", subject.processInstanceId());
        String canonical = ElectronicSignatureV2Protocol.canonical(content);
        var row = ElectronicSignatureRecordDO.builder().moduleCode(command.moduleCode()).actionCode(command.actionCode()).subjectType(subject.subjectType()).subjectId(subject.subjectId()).subjectVersion(subject.subjectVersion()).actorId(signer.actorId()).meaningCode(action.meaningCode()).meaningLabel(action.meaningLabel()).reason(command.reason()).signedAt(signedAt).timeEvidenceId("SERVER_CLOCK:" + time.get("localDateTime").textValue()).authenticationMethod("MES_EMPLOYEE_PROFILE".equals(signer.domain()) ? "SESSION_PLUS_EMPLOYEE_PROFILE_PASSWORD" : signer.actorId().equals(signer.operatorId()) ? "SESSION_PLUS_PASSWORD" : "SESSION_PLUS_SELECTED_USER_PASSWORD").contentHash(DigestUtil.sha256Hex(canonical)).canonicalContentJson(canonical).beforeContentJson(ElectronicSignatureJsonCanonicalizer.canonicalize(subject.beforeContentJson())).afterContentJson(ElectronicSignatureJsonCanonicalizer.canonicalize(subject.afterContentJson())).fieldDiffJson(ElectronicSignatureJsonCanonicalizer.canonicalize(subject.fieldDiffJson())).algorithm("SHA-256").keyVersion("digest-format-v2").policyVersion(action.policyVersion()).verificationStatus("VALID").idempotencyKey(command.idempotencyKey()).commandHash(requestHash).processInstanceId(subject.processInstanceId()).taskId(subject.taskId()).nodeCode(subject.nodeCode()).nodeOrder(subject.nodeOrder()).evidenceFormatVersion("V2").commandHashVersion("V2").build();
        row.setTenantId(signer.tenantId()); row.setBeforeContentHash(row.getBeforeContentJson() == null ? null : DigestUtil.sha256Hex(row.getBeforeContentJson())); row.setAfterContentHash(row.getAfterContentJson() == null ? null : DigestUtil.sha256Hex(row.getAfterContentJson())); row.setEvidenceHash(ElectronicSignatureV2Protocol.hash(ElectronicSignatureV2Protocol.evidence(row, content)));
        if (mapper.insert(row) != 1 || row.getId() == null) throw new IllegalStateException("SIGNATURE_INSERT_FAILED");
        audit.append(GxpAuditCommand.builder().operationId("signature.record.create").subjectId(subject.subjectType() + ":" + subject.subjectId()).subjectVersion(subject.subjectVersion()).reason(command.reason()).beforeState(GxpAuditStateEnvelope.builder().state("NO_SIGNATURE_RECORD").objectVersion(subject.subjectVersion()).canonicalJson("{}").build()).afterState(GxpAuditStateEnvelope.builder().state("ELECTRONIC_SIGNATURE_RECORDED").objectVersion(subject.subjectVersion()).canonicalJson(JsonUtils.toJsonString(afterState(row))).build()).idempotencyKey("SIGNATURE:" + DigestUtil.sha256Hex(command.idempotencyKey())).source("ElectronicSignatureTransactionExecutor.executeAuthenticated").sourceType("SERVICE_METHOD").sourceLocator("ElectronicSignatureTransactionExecutor.executeAuthenticated").signatureRecordId(row.getId().toString()).signatureContentHash(row.getContentHash()).build());
        return new ElectronicSignatureResult(row.getId(), "VALID", signedAt, row.getTimeEvidenceId(), row.getSubjectVersion(), row.getContentHash(), row.getEvidenceHash(), row.getAlgorithm(), row.getKeyVersion());
    }
    public static Map<String, Object> afterState(ElectronicSignatureRecordDO r) {
        var state = new LinkedHashMap<String, Object>();
        state.put("signatureId", r.getId()); state.put("moduleCode", r.getModuleCode()); state.put("actionCode", r.getActionCode()); state.put("subjectType", r.getSubjectType()); state.put("subjectId", r.getSubjectId()); state.put("subjectVersion", r.getSubjectVersion()); state.put("actorId", r.getActorId()); state.put("meaningCode", r.getMeaningCode()); state.put("meaningLabel", r.getMeaningLabel()); state.put("reason", r.getReason()); state.put("signedAt", r.getSignedAt()); state.put("authenticationMethod", r.getAuthenticationMethod()); state.put("contentHash", r.getContentHash()); state.put("beforeContentHash", r.getBeforeContentHash()); state.put("afterContentHash", r.getAfterContentHash()); state.put("evidenceHash", r.getEvidenceHash()); state.put("policyVersion", r.getPolicyVersion()); state.put("verificationStatus", r.getVerificationStatus()); state.put("processInstanceId", r.getProcessInstanceId()); state.put("taskId", r.getTaskId()); state.put("nodeCode", r.getNodeCode()); state.put("nodeOrder", r.getNodeOrder()); state.put("evidenceFormatVersion", r.getEvidenceFormatVersion()); state.put("commandHashVersion", r.getCommandHashVersion()); return state;
    }
}
